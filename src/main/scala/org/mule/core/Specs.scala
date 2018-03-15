package org.mule.core

import java.io.File

import amf.core.parser.YMapOps
import org.mule.util.FileUtils
import org.mulesoft.common.io.Fs
import org.yaml.model.{YDocument, YMap}
import org.yaml.parser.YamlParser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Specs {

  final val TCKUTOR_SUFFIX: String = "-TCKutor"
  final val DELIMITATOR_CHAR: String = "_"
  final val RAML10_IDENTIFIER: String = "#%RAML 1.0"
  final val RAML08_IDENTIFIER: String = "#%RAML 0.8"
  final val OAS20_IDENTIFIER: String = "\"swagger\": \"2.0\""
  final val OAS30_IDENTIFIER: String = "openapi: 3.0.0"
  final val JSON_LD_IDENTIFIER: String = "@id"
  final val KEEP_GENERATED_FILES: String = "keepGeneratedFiles"
  final val NOT_TCK_REPO: String = "notTckRepo"
  final val JAVA_PARSER: String = "javaParser"
  final val OAS_PARSER: String = "oasParser"
  final val RUN_OAS_CHECK: String = "oasCheck"
  final val RUN_CONVERT_AMF: String = "runConvertAMF"
  final val RUN_REG_EXPS: String = "runRegExps"

  sealed abstract class APIType(
      val label: String,
      val extension: String,
      val identifier: String
  )

  case object RAML extends APIType("RAML", ".raml", "#%RAML")
  case object RAML08 extends APIType("RAML", ".raml", "#%RAML 0.8")
  case object RAML10 extends APIType("RAML", ".raml", "#%RAML 1.0")
  case object OAS20 extends APIType("OAS", ".json", "\"swagger\": \"2.0\"")
  case object OAS30 extends APIType("OAS", ".json", "openapi: 3.0.0")
  case object JSON_LD extends APIType("JSON_LD", ".json", "@id")

  def getApiKind(file: File): APIType = {
    val filename: String = file.getName
    filename match {
      case f if f.endsWith(RAML.extension) =>
        //TODO refactor when raml vendor is at document level
        if (FileUtils.readFileContent(file).contains(RAML08_IDENTIFIER)) return RAML08
        if (FileUtils.readFileContent(file).contains(RAML10_IDENTIFIER)) return RAML10
        throw new Exception("could not identify ApiType for file = " + file.getAbsolutePath)
      case f if f.endsWith(OAS20.extension) => //the extension is the same for JSON-LD
        if (existsKey(file, OAS20_IDENTIFIER)) return OAS20
        if (existsKey(file, JSON_LD_IDENTIFIER)) return JSON_LD
        throw new Exception("could not identify ApiType for file = " + file.getAbsolutePath)
      case f if f.endsWith(OAS30.extension) =>
        if (FileUtils.readFileContent(file).contains(OAS30_IDENTIFIER)) return OAS30
        throw new Exception("could not identify ApiType for file = " + file.getAbsolutePath)
      case _ => throw new Exception("could not identify ApiType for file = " + file.getAbsolutePath)
    }
  }

  private def existsKey(file: File, key: String): Boolean = {
    try {
      val futureParse = Future {
        val value: CharSequence = Fs.syncFile(file.getAbsolutePath).read()
        YamlParser(value).parse()
      }
      val parseResult = Await.result(futureParse, 10 seconds)
      val document: Option[YDocument] = parseResult.collect({ case d: YDocument => d }).toList.headOption
      document match {
        case Some(d) =>
          d.node.to[YMap] match {
            case Right(map) => map.key(key).isDefined
            case Left(_) => false
          }
        case None => false
      }
    } catch {
      case e: Exception =>
        throw new Exception("Error trying to identify ApiType for file = " + file.getAbsolutePath)
    }
  }

}
