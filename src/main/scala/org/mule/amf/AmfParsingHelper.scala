package org.mule.amf

import java.io.File
import amf.client.model.document.BaseUnit
import org.mule.core.Specs.APIType

object AmfParsingHelper{

  def handleParse(file: File, kind: APIType): Either[Exception,  BaseUnit] =  {
    try {
        val baseUnit = parse(file, kind)
        Right(baseUnit)
    } catch {
      case e: Exception => Left(e)
    }
  }

  private def parse(file: File, kind: APIType): BaseUnit = {
    val parser = AmfObjectsHandler.createParser(kind)
    parser.parseFileAsync("file://" + file.getPath).get()
  }
}
