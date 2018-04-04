package org.mule.amf.graph

import amf.client.AMF
import amf.client.model.document.BaseUnit
import amf.core.unsafe.PlatformSecrets

trait AMFSerializerExtension extends PlatformSecrets { this: KnowledgeBase =>

  protected def buildBaseUnit(documentUrl: String): BaseUnit = {
    AMF.init().get()

    val json = JSONExpander.expand(documentUrl, tbox)

    //hack, we pick the model to parse based on the first type in the list, this parsing changes the order, we cannot build from #Shape
    val fixedJson = json.replaceAll("#Shape", "#ShapeFoo")

    AMF.amfGraphParser().parseStringAsync(fixedJson).get()

  }

  def exportDocument(vendor: String, documentUrl: String): String = {
    val model = buildBaseUnit(documentUrl)

    vendor match {
      case "RAML 1.0" => AMF.raml10Generator().generateString(model).get()// new AMFSerializer(model, "application/yaml", "RAML 1.0", GenerationOptions()).dumpToString
      case "OAS 2.0"  => AMF.oas20Generator().generateString(model).get() // new AMFSerializer(model, "application/json", "OAS 2.0", GenerationOptions()).dumpToString
      case _          => throw new Exception(s"Unknown document $vendor")
    }
  }

}
