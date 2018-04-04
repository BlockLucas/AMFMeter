package org.mule.amf.graph

import java.io.{File, FileInputStream, PrintWriter}
import java.nio.charset.Charset
import java.util

import amf.client.AMF
import amf.core.unsafe.PlatformSecrets
import amf.core.vocabulary.Namespace
import amf.client.model.document.BaseUnit
import org.apache.commons.io.IOUtils
import org.apache.jena.query._
import org.apache.jena.rdf.model.{Model, ModelFactory, RDFNode, Resource, Statement, Literal => JLiteral}
import org.apache.jena.reasoner.ReasonerRegistry
import org.apache.jena.update.{UpdateAction, UpdateFactory, UpdateRequest}
import org.mulesoft.common.io.Fs
import org.topbraid.spin.util.JenaUtil

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class Value(val value: String)

case class Uri(override val value: String) extends Value(value) {
  override def toString: String = {
    if (value != null) {
      val compacted = Namespace.compact(value)
      if (value == compacted) {
        s"<$value>"
      } else {
        compacted
      }
    } else ""
  }

}

case class Literal(override val value: String, datatype: Option[String] = None)
  extends Value(value) {
  override def toString: String = datatype match {
    case Some(_) => "\"" + value + "\"^^<$d>"
    case None => "\"" + value + "\""
  }
}

case class Variable(override val value: String) extends Value(value) {
  override def toString: String = s"?$value"
}

case class Triple(subj: Value, pred: Value, obj: Value) {
  override def toString: String = s"$subj $pred $obj ."
}

object U {
  def apply(value: String) = Uri(Namespace.expand(value).iri())
}

object L {
  def apply(value: Any): Literal = value match {
    case i: Integer => Literal(s"$i", Some((Namespace.Xsd + "integer").iri()))
    case f: Float => Literal(s"$f", Some((Namespace.Xsd + "float").iri()))
    case b: Boolean => Literal(s"$b", Some((Namespace.Xsd + "boolean").iri()))
    case l => Literal(s"$l")
  }

  def apply(value: String, datatype: Option[String]) = Literal(value, datatype)
}

object ? {
  def apply(variable: String) = Variable(variable)
}

object T {
  def apply(s: Value, p: Value, o: Value) = Triple(s, p, o)
}

class ConstructQuery(
                      constructClause: String,
                      cb: (ConstructQuery) => Seq[util.HashMap[String, Value]]) {
  var whereClause = ""

  def where(whereClause: String): Seq[util.HashMap[String, Value]] = {
    this.whereClause = whereClause
    cb(this)
  }

  def build: String =
    s"""CONSTRUCT {
       |$constructClause
       |} WHERE {
       |$whereClause
       |}
    """.stripMargin
}

class SelectQuery(projection: String,
                  cb: (SelectQuery) => Seq[util.HashMap[String, Value]]) {
  var whereClause = ""

  def where(whereClause: String): Seq[util.HashMap[String, Value]] = {
    this.whereClause = whereClause
    cb(this)
  }

  def build: String =
    s"""SELECT $projection WHERE {
       |$whereClause
       |}
    """.stripMargin
}

class DeleteQuery(deleteClause: String, cb: (DeleteQuery) => Boolean) {
  var whereClause = ""

  def where(whereClause: String = "*"): Boolean = {
    this.whereClause = whereClause
    cb(this)
  }

  def build: String =
    if (whereClause == "*") {
      s"""
         |DELETE {
         |  $deleteClause
         |}
    """.stripMargin
    } else {
      s"""
         |DELETE {
         |  $deleteClause
         |} WHERE {
         |  $whereClause
         |}
    """.stripMargin
    }
}

class InsertQuery(inserClause: String, cb: (InsertQuery) => Boolean) {
  var whereClause = ""

  def where(whereClause: String = "*"): Boolean = {
    this.whereClause = whereClause
    cb(this)
  }

  def build: String =
    if (whereClause == "*") {
      s"""
         |INSERT {
         |  $inserClause
         |}
    """.stripMargin
    } else {
      s"""
         |INSERT {
         |  $inserClause
         |} WHERE {
         |  $whereClause
         |}
    """.stripMargin
    }
}

class KnowledgeBase(val base: String) extends AMFSerializerExtension {

  def render(baseDir: String, uri: String): PrintWriter = {
    val model = models.getNamedModel(uri)
    val json = JSONExpander.expand(uri, model)

    //hack, we pick the model to parse based on the first type in the list, this parsing changes the order, we cannot build from #Shape
    val fixedJson = json.replaceAll("#Shape", "#ShapeFoo")
    val unit = AMF.amfGraphParser().parseStringAsync(fixedJson).get()

    val str = uri.substring(uri.lastIndexOf(".")) match {
      case ".raml" => AMF.raml10Generator().generateString(unit).get()
      case ".json" => AMF.oas20Generator().generateString(unit).get()
      case _ => throw new Exception(s"Unknown document $uri")
    }

    val out = new File(baseDir + File.separator + new File(uri).getName)
    out.getParentFile.mkdirs()
    if (!out.exists()) out.createNewFile()

    new PrintWriter(out) {
      write(str); close()
    }
  }

  def renderInto(baseDir: String, graph: Option[Uri] = None): Future[Unit] = {
    graph match {
      case Some(uri) =>
        render(baseDir, uri.value)
      case None =>
        models.listNames().forEachRemaining {
          uri => render(baseDir, uri)
        }
    }

    Future.unit
  }


  Namespace.registerNamespace("", base)

  private val models: Dataset = DatasetFactory.create()

  def registerNamespace(alias: String, prefix: String): Option[Namespace] =
    Namespace.registerNamespace(alias, prefix)

  def withInference[T](b: => T): T = {
    inference = true
    try {
      b
    } finally {
      inference = false
    }
  }

  // load the model as a new named graph
  def load(model: BaseUnit, namedGraph: Option[Uri] = None): KnowledgeBase = {
    namedGraph match {
      case Some(uri) =>
        val store = ModelFactory.createDefaultModel()
        store.read(
          IOUtils.toInputStream(jsonld(model), Charset.defaultCharset),
          base,
          "JSON-LD")
        models.addNamedModel(uri.value, store)
      case None =>
        val store = models.getDefaultModel
        store.read(
          IOUtils.toInputStream(jsonld(model), Charset.defaultCharset),
          base,
          "JSON-LD")
    }
    this
  }

  // read in named graph or in the union graph
  def findModelRead(namedGraph: Option[Uri]): Model = {
    namedGraph match {
      case Some(uri) => models.getNamedModel(uri.value)
      case None => models.getUnionModel
    }
  }

  // write in named graph or in the default graph
  def findModelWrite(namedGraph: Option[Uri]): Model = {
    namedGraph match {
      case Some(uri) => models.getNamedModel(uri.value)
      case None => models.getDefaultModel
    }
  }

  def facts(namedGraph: Option[Uri]): Seq[Triple] = {
    val model = findModelRead(namedGraph)
    val iterator = model.listStatements()
    val acc = ListBuffer[Triple]()
    while (iterator.hasNext) {
      try {
        acc += statementToTriple(iterator.nextStatement())
      } catch {
        case _: Throwable => // blank node
      }
    }
    acc
  }

  def assert(triple: Triple, namedGraph: Option[Uri]): KnowledgeBase = {
    val target = findModelWrite(namedGraph)
    val s = target.createResource(Namespace.expand(triple.subj.value).iri())
    val p = target.createProperty(Namespace.expand(triple.pred.value).iri())
    val o = triple.obj match {
      case Uri(value) => target.createResource(Namespace.expand(value).iri())
      case Literal(value, Some(dt)) => target.createTypedLiteral(value, dt)
      case Literal(value, None) => target.createLiteral(value)
    }
    target.add(target.createStatement(s, p, o))
    this
  }

  def assert(triples: Seq[Triple], namedGraph: Option[Uri]): KnowledgeBase = {
    triples.foreach(assert(_, namedGraph))
    this
  }

  def assert(triples: String, namedGraph: Option[Uri]): KnowledgeBase = {
    val text = prefixes("N3") + triples
    val store = findModelWrite(namedGraph)
    store.read(IOUtils.toInputStream(text, Charset.defaultCharset), base, "N3")
    this
  }

  def assertGraph(triple: Triple, target: Model): KnowledgeBase = {
    val s = target.createResource(Namespace.expand(triple.subj.value).iri())
    val p = target.createProperty(Namespace.expand(triple.pred.value).iri())
    val o = triple.obj match {
      case Uri(value) => target.createResource(Namespace.expand(value).iri())
      case Literal(value, Some(dt)) => target.createTypedLiteral(value, dt)
      case Literal(value, None) => target.createLiteral(value)
    }
    target.add(target.createStatement(s, p, o))
    this
  }

  def assert(file: java.io.File, namedGraph: Option[Uri]): KnowledgeBase = {
    val store = findModelWrite(namedGraph)
    store.read(new FileInputStream(file), base, "N3")
    this
  }

  def retract(triple: Triple, namedGraph: Option[Uri]): KnowledgeBase = {
    val store = findModelWrite(namedGraph)
    val s = store.createResource(Namespace.expand(triple.subj.value).iri())
    val p = store.createProperty(Namespace.expand(triple.pred.value).iri())
    val o = triple.obj match {
      case Uri(value) => store.createResource(Namespace.expand(value).iri())
      case Literal(value, Some(dt)) => store.createTypedLiteral(value, dt)
      case Literal(value, None) => store.createLiteral(value)
    }

    store.remove(store.createStatement(s, p, o))
    this
  }

  def retract(triples: String, namedGraph: Option[Uri]): KnowledgeBase = {
    val toRemove = JenaUtil.createMemoryModel
    val text = prefixes("N3") + triples
    toRemove.read(IOUtils.toInputStream(text, Charset.defaultCharset),
      base,
      "N3")
    val store = findModelWrite(namedGraph)
    store.remove(toRemove)
    this
  }

  def retract(file: java.io.File, namedGraph: Option[Uri]): KnowledgeBase = {
    val toRemove = JenaUtil.createMemoryModel
    toRemove.read(new FileInputStream(file), base, "N3")
    val store = findModelWrite(namedGraph)
    store.remove(toRemove)
    store.remove(toRemove)
    this
  }

  def statementToTriple(s: Statement): Triple = {
    Triple(
      resourceToValue(s.getSubject).asInstanceOf[Uri],
      resourceToValue(s.getPredicate).asInstanceOf[Uri],
      resourceToValue(s.getObject)
    )
  }

  def query(text: String): Seq[util.HashMap[String, Value]] = {
    val query: Query = QueryFactory.create(prefixes() + "\n" + text)
    val execution: QueryExecution = effectiveStore() match {
      case d: Dataset => QueryExecutionFactory.create(query, d)
      case m: Model => QueryExecutionFactory.create(query, m)
    }

    val results = ListBuffer[QuerySolution]()
    try {
      execution.execSelect().forEachRemaining(r => results += r)
    } finally {
      execution.close()
    }
    results.map { solution =>
      val result = new util.HashMap[String, Value]()
      solution.varNames().forEachRemaining { v =>
        try {
          result.put(v, resourceToValue(solution.get(v)))
        } catch {
          case _: Throwable => //blank node
        }
      }
      result
    }
  }

  def update(text: String): Boolean = {
    val query: UpdateRequest = UpdateFactory.create(prefixes() + "\n" + text)
    try {
      effectiveStore() match {
        case d: Dataset => UpdateAction.execute(query, d)
        case m: Model => UpdateAction.execute(query, m)
      }
      true
    } finally {
      false
    }
  }

  def construct(constructQuery: String): ConstructQuery =
    new ConstructQuery(constructQuery, { construct =>
      query(construct.build)
    })

  def select(projection: String = "*"): SelectQuery =
    new SelectQuery(projection, { select =>
      query(select.build)
    })

  def delete(delete: String = "*"): DeleteQuery =
    new DeleteQuery(delete, { delete =>
      try {
        update(delete.build)
        true
      } catch {
        case _: Throwable => false
      }
    })

  def insert(insert: String = "*"): InsertQuery =
    new InsertQuery(insert, { insert =>
      try {
        update(insert.build)
        true
      } catch {
        case _: Throwable => false
      }
    })

  def declareClass(classUri: Uri): KnowledgeBase = {
    assertGraph(T(classUri, U("rdf:type"), U("owl:class")), tbox)
    this
  }

  def declareObjectProperty(propertyUri: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdf:type"), U("owl:ObjectProperty")), tbox)
    this
  }

  def declareDatatypeProperty(propertyUri: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdf:type"), U("owl:DatatypeProperty")), tbox)
    this
  }

  def declarePropertyRange(propertyUri: Uri, range: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdfs:range"), range), tbox)
    this
  }

  def declarePropertyDomain(propertyUri: Uri, domain: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdfs:domain"), domain), tbox)
    this
  }

  def declareSubProperty(subProperty: Uri, superProperty: Uri): KnowledgeBase = {
    assertGraph(T(subProperty, U("rdfs:subPropertyOf"), superProperty), tbox)
    this
  }

  def declareInverseProperty(propertyUri: Uri, inverseOf: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("owl:inverseOf"), inverseOf), tbox)
    this
  }

  def declareFunctionalProperty(propertyUri: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdf:type"), U("owl:FunctionalProperty")),
      tbox)
    this
  }

  def declareSymmetricProperty(propertyUri: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdf:type"), U("owl:SymmetricProperty")),
      tbox)
    this
  }

  def declareTransitiveProperty(propertyUri: Uri): KnowledgeBase = {
    assertGraph(T(propertyUri, U("rdf:type"), U("owl:TransitiveProperty")),
      tbox)
    this
  }

  def declareSameAs(individual1: Uri, individual2: Uri): KnowledgeBase = {
    assertGraph(T(individual1, U("owl:sameAs"), individual2), tbox)
    this
  }

  def declareDifferent(individual1: Uri, individual2: Uri): KnowledgeBase = {
    assertGraph(T(individual1, U("owl:differentFrom"), individual2), tbox)
    this
  }

  def declareUnionClass(unionClass: Uri, unionMembers: Seq[Uri]): KnowledgeBase = {
    val s = tbox.createResource(Namespace.expand(unionClass.value).iri())
    val p = tbox.createProperty(Namespace.expand("owl:unionOf").iri())
    val members: Array[RDFNode] = unionMembers
      .map(m => tbox.createResource(Namespace.expand(m.value).iri()))
      .toArray[RDFNode]
    val o = tbox.createList(members)
    tbox.add(tbox.createStatement(s, p, o))
    this
  }

  protected def resourceToValue(n: RDFNode): Value = {
    n match {
      case r: Resource => Uri(r.getURI)
      case l: JLiteral => Literal(l.getString, Some(l.getDatatypeURI))
      case _ => throw new Exception("Blank nodes are not allowed")
    }
  }

  protected def prefixes(style: String = "SPARQL"): String = {
    val baseText = style match {
      case "SPARQL" => s"BASE <$base>\n"
      case "N3" => s"@prefix : <$base> .\n"
    }
    baseText + Namespace.ns
      .map {
        case (p, v) =>
          style match {
            case "SPARQL" => s"PREFIX $p: <${v.base}>"
            case "N3" => s"@prefix $p: <${v.base}> ."
          }

      }
      .mkString("\n")
  }

  protected val tbox: Model = JenaUtil.createMemoryModel

  var inference: Boolean = false

  def effectiveStore(): Object = {
    if (inference) {
      val reasoner = ReasonerRegistry.getOWLReasoner.bindSchema(tbox)
      ModelFactory.createInfModel(reasoner, models.getUnionModel)
    } else {
      models
    }
  }

  protected def jsonld(model: BaseUnit): String = AMF.amfGraphGenerator().generateString(model).get()
}

object KnowledgeBase extends PlatformSecrets {

  def directory(d: String): Future[KnowledgeBase] = {
    val kb = new KnowledgeBase(d)
    val units: Seq[Future[KnowledgeBase]] = Fs.syncFile(d).list.toSeq.map {
      case raml if raml.endsWith(".raml") =>
        load(kb, raml, "application/yaml", "RAML 1.0")
      case oas if oas.endsWith(".json") =>
        load(kb, oas, "application/json", "OAS 2.0")
      case _ => Future.successful(kb)
    }
    Future.sequence(units).map(_ => kb)
  }

  private def load(kb: KnowledgeBase,
                   file: String,
                   mediaType: String,
                   vendor: String): Future[KnowledgeBase] = {
    val f = s"file://${kb.base}/$file"
    null
//    RuntimeCompiler(f, platform, Some(mediaType), vendor) map {
//      model => kb.load(model, Some(U(f)))
//    }
  }
}
