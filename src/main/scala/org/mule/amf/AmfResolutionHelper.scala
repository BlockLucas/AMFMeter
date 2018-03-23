package org.mule.amf

import amf.client.model.document.BaseUnit
import amf.client.resolve.Resolver
import org.mule.core.Specs.APIType

object AmfResolutionHelper {

  def handleResolution(kind: APIType, baseUnit: BaseUnit): Either[Throwable, BaseUnit] = {
    try {
        val b = resolve(baseUnit, kind)
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
