//package org.mule.amf.graph
//
//import java.io.File
//
//import amf.client.AMF
//import amf.client.parse.Raml10Parser
//
//object AmfGraphHelper{
//
//  def handleLoad(file: File): Either[Exception,  KnowledgeBase] =  {
//    try {
//        val kb = load(file)
//        Right(kb)
//    } catch {
//      case e: Exception => Left(e)
//    }
//  }
//
//  private def load(file: File): KnowledgeBase = {
//    val parser = new Raml10Parser()
//    val baseUnit = parser.parseFileAsync("file://" + file.getAbsolutePath).get()
//    AMF.registerNamespace("tckutor", "http://mulesoft.com/vocabularies/tckutor#")
//    val kb = new KnowledgeBase("file://vocabularies/src/test/resources/")
//    kb.load(baseUnit, Some(U(baseUnit.location)))
//  }
//}
