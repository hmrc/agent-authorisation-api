package uk.gov.hmrc.agentauthorisation.models
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.agentauthorisation.models


case class CheckRelationshipPayload(
                                    service: List[String],
                                    clientIdType: String,
                                    clientId: String,
                                    knownFact: String)

object CheckRelationshipPayload {

  implicit val reads: Reads[CheckRelationshipPayload] = {
    ((JsPath \ "service").read[List[String]] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientIdType, clientId, knownFact) =>
      CheckRelationshipPayload(service, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[CheckRelationshipPayload] = new Writes[CheckRelationshipPayload] {
    override def writes(o: CheckRelationshipPayload): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}

case class RelationshipRequest(service: String,
                          clientIdType: String,
                          clientId: String,
                          knownFact: String) extends

object RelationshipRequest {
  implicit val reads: Reads[RelationshipRequest] = {
    ((JsPath \ "service").read[String] and
      (JsPath \ "clientIdType").read[String] and
      (JsPath \ "clientId").read[String].map(_.replaceAll(" ", "")) and
      (JsPath \ "knownFact").read[String])((service, clientIdType, clientId, knownFact) =>
      models.RelationshipRequest(service, clientIdType, clientId, knownFact))
  }

  implicit val writes: Writes[RelationshipRequest] = new Writes[RelationshipRequest] {
    override def writes(o: RelationshipRequest): JsValue =
      Json.obj(
        "service"      -> o.service,
        "clientIdType" -> o.clientIdType,
        "clientId"     -> o.clientId.replaceAll(" ", ""),
        "knownFact"    -> o.knownFact)
  }
}
