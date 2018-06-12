Agent Client Authorisation API
==============================

An API allowing MTD-enabled Agents to request authorisation to a service for a client, instead of filling the 64-8 paper form.

## Motivation
Agents often use software to perform services for their clients. 
The API will benefit these agents since it will allow them to be able to request the invitation link to authorise an agent for a service directly through software. 
This will save an agent time since currently an agent must separately log into Agent Services to request this link. 
This also aligns with the API first strategy for Agent Services.

### API docs
Refer to [RAML documentation](https://github.com/hmrc/agent-client-authorisation-api/blob/master/resources/public/api/conf/0.1/application.raml) for further details on each API.
   

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
|Report income or expenses through software|HMRC-MTD-IT|
|View PAYE income record|PERSONAL-INCOME-RECORD|
|Report VAT returns through software|HMRC-MTD-VAT|


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
Validates the service, clientIdentifier, type and optionally postcode (only applicable for Self-Assessment) and creates an invitation.

```
POST  /agencies/:arn/invitations
```

Request:
```
http://localhost:9432/agent-client-authorisation/agenices/TARN0000001/invitations

```
Example Body of ITSA:
```json
{
  "service": "HMRC-MTD-IT",
  "clientIdType": "ni",
  "clientId": "AB123456A",
  "clientPostcode": "DHJ4EJ"
}
```

Example Body of VAT:
```json
{
  "service": "HMRC-MTD-VAT",
  "clientIdType": "vrn",
  "clientId": "101747696",
  "clientPostcode": null
}
```

Example Body of IRV:
```json
{
  "service": "PERSONAL-INCOME-RECORD",
  "clientIdType": "ni",
  "clientId": "AE123456C",
  "clientPostcode": null
}
```

|Response|Description|
|--------|---------|
|204|Successfully created invitation. (In Headers) Location → "/agencies/:arn/invitations/:invitationdId"|
|400|Received Valid Json but incorrect data|
|400|Received Invalid Json|
|403|Client Registration Not Found|
|403|(HMRC-MTD-IT Only) Post Code does not match|
|501|Unsupported Service|

Note: The link returned from a successful create invitation response is "GET a Specific Agent's Sent Invitation"


#### GET a Specific Agent's Sent Invitation <a name="agentSpecificInvitation"></a>
Retrieves a specific invitation by its InvitationId
```
GET   /agencies/:arn/invitations/:invitationId
```

Request:
```
http://localhost:9432/agent-client-authorisation/agenices/TARN0000001/invitations/CS5AK7O8FPC43
```

|Response|Description|
|--------|---------|
|200|Returns an invitation in json|
|404|The specified invitation was not found.|

Example Response: 200 with Body:
```json
{
   "arn" : "TARN0000001",
   "service" : "HMRC-MTD-VAT",
   "lastUpdated" : "2018-05-04T13:51:35.278Z",
   "created" : "2018-04-16T15:05:54.029Z",
   "clientIdType" : "vrn",
   "clientId" : "101747641",
   "expiryDate" : "2018-04-26",
   "suppliedClientIdType" : "vrn",
   "suppliedClientId" : "101747641",
   "_links" : {
      "self" : {
         "href" : "/agent-client-authorisation/agencies/TARN0000001/invitations/CS5AK7O8FPC43"
      }
   },
   "status" : "Expired",
   "postcode" : null
}
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
