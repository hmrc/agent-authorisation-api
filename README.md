Agent Authorisation API
==============================

This API allows agents to request authorisation to act on a client's behalf for the different MTD tax services. The API also allows the Agent to check the status of authorisation request. Please note this API has no effect on the existing XML API. 

## Motivation
Agents often use software to perform services for their clients. 
The API will benefit these agents since it will allow them to be able to request the invitation link directly through software, to send to their client so that they can authorise the agent for a service. 
This will save an agent time since currently they must separately log into Agent Services UI to request this link. 
This also aligns with the API first strategy for Agent Services.

### API docs
Refer to [RAML documentation](https://github.com/hmrc/agent-authorisation-api/blob/master/resources/public/api/conf/0.0/application.raml) for further details on each API.
   

## Table of Contents
*   [Supported Regimes / Services](#supportedRegimes)
*   [Invitation Status](#invitationStatus)
*   [Agent APIs](#agentApis)
    *   [Create Invitation](#createInvitation)
    *   [GET a Specific Agent's Sent Invitation](#agentSpecificInvitation)

### Supported Regimes / Services <a name="supportedRegimes"></a>
This supports MTD-enabled Agent and Client authorisation processes for the following tax services for agents:

|Tax service|Service Id|
|--------|--------|
|Report income or expenses through software|MTD-IT|
|Report VAT returns through software|MTD-VAT|


### Invitation Status <a name="invitationStatus"></a>
Invitations can have one of the following status:

|Invitation Status|Description|
|--------|---------|
|Pending|Default status when an invitation has been created|
|Accepted|Allows Agent to be authorised to act on behalf of a client|
|Rejected|Prevents Agent being authorised to act on a client's behalf|
|Expired|Client did not respond to the Agent's Invitation within 10 days|
|Cancelled|Agent cancels the invitation they sent out, preventing a client from responding|

Note: Invitations with "Pending" status is the only editable status.
  

## Agent APIs <a name="agentApis"></a>
The following APIs require agent authentication. 

Any unauthorised access could receive one of the following responses:

|Response|Description|
|--------|---------|
|401|Unauthorised. Not logged In|
|403|The Agent is not subscribed to Agent Services.|
|403|The logged in user is not permitted to access invitations for the specified agency.|


#### Create Invitation <a name="createInvitation"></a>
Validates the service, clientIdentifier, clientIdentifierType and creates an invitation.

```
POST  /agents/:arn/invitations
```

Request:
```
https://api.service.hmrc.gov.uk/agents/TARN0000001/invitations
```

Example Body with ITSA registered postcode:
```json
{
  "service": "HMRC-MTD-IT",
  "clientIdType": "ni",
  "clientId": "AB123456A",
  "knownFact": "AA11 1A"
}
```

Example Body of VAT registration date:
```json
{
  "service": "HMRC-MTD-VAT",
  "clientIdType": "vrn",
  "clientId": "101747696",
  "knownFact": "2007-01-07"
}
```

|Response|Description|
|--------|---------|
|204|Successfully created invitation. (In Headers) Location → "/agencies/:arn/invitations/:invitationId"|
|400|Received Invalid Json|
|400|Received Valid Json but incorrect data|
|403|Client Registration Not Found|
|403|Supplied known fact does not match registration data|
|501|Unsupported Service|

Note: The link returned from a successful create invitation response is "GET a Specific Agent's Sent Invitation"


#### GET a Specific Agent's Sent Invitation <a name="agentSpecificInvitation"></a>
Retrieves a specific invitation by its InvitationId
```
GET   /agents/:arn/invitations/:invitationId
```

Request:
```
https://api.service.hmrc.gov.uk/agents/TARN0000001/invitations/CS5AK7O8FPC43
```

|Response|Description|
|--------|---------|
|200|Returns an invitation in json|
|404|The specified invitation was not found.|

Example response: 200 with `Pending` body:
```json
{
   "arn" : "TARN0000001",
   "service" : "HMRC-MTD-VAT",
   "created" : "2018-04-16T15:05:54.029Z",
   "expiresOn" : "2018-05-04T00:00:00:000Z",
   "status" : "Pending",
   "clientActionUrl": "https://www.tax.service.gov.uk/invitations/CS5AK7O8FPC43",
   "_links" : {
         "self" : {
            "href" : "/agents/TARN0000001/invitations/CS5AK7O8FPC43"
         }
      }
}
```

Example response: 200 with `Accepted` body:
```json
{
   "arn" : "TARN0000001",
   "service" : "HMRC-MTD-VAT",
   "created" : "2018-04-16T15:05:54.029Z",
   "updated" : "2018-05-04T01:16:21:786Z",
   "status" : "Accepted",
   "_links" : {
         "self" : {
            "href" : "/agents/TARN0000001/invitations/CS5AK7O8FPC43"
         }
      }
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
