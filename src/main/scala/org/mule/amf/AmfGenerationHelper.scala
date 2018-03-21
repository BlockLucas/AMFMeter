package org.mule.amf

import java.io.File

import amf.model.document.BaseUnit
import org.mule.core.Specs.APIType

object AmfGenerationHelper {

  def handleGen(targetFile: File, targetKind: APIType, baseUnit: BaseUnit): Either[Throwable, Unit] = {
    try {
        generate(targetFile, targetKind, baseUnit)
        Right()
    } catch {
      case s: StackOverflowError => Left(s)
      case e: Exception => Left(e)
    }

  }

  private def generate(target: File, kind: APIType, baseUnit: BaseUnit): Unit = {
    val generator = AmfObjectsHandler.createGenerator(kind)
    generator.generateFile(baseUnit, target).get()
  }

}
