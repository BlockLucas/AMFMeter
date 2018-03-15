package org.mule.amf

import amf.core.client.Resolver
import amf.model.document.BaseUnit
import org.mule.core.Specs.APIType

object AmfResolutionHelper {

  def handleResolution(kind: APIType, baseUnit: BaseUnit): Either[Throwable, BaseUnit] = {
    println("about to resolve")
    try {
        val b = resolve(baseUnit, kind)
        println("resolved")
        Right(b)
    } catch {
      case s: StackOverflowError => Left(s)
      case e: Exception => Left(e)
    }
  }

  private def resolve(baseUnit: BaseUnit, kind: APIType): BaseUnit = {
    val resolver: Resolver = AmfObjectsHandler.createResolver(kind)
    resolver.resolve(baseUnit)
  }

}
