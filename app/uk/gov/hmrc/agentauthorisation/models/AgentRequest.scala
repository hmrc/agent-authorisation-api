package uk.gov.hmrc.agentauthorisation.models

trait AgentRequest {

  val service: String
  val clientType: String
  val clientIdType: String
  val clientId: String
  val knownFact: String

}
