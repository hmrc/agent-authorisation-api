# Agent Authorisation API documentation version 0.0

### Overview
This API allows agents to request authorisation to act on a client's behalf for the different MTD tax services. The API also allows the Agent to check the status of authorisations already requested. Please note this API has no effect on the existing XML API. 

## Motivation
Agents often use software to perform services for their clients. 
The API will benefit these agents since it will allow them to be able to request the invitation link directly through software, to send to their client so that they can authorise the agent for a service. 
This will save an agent time since currently they must separately log into Agent Services UI to request this link. 
This also aligns with the API first strategy for Agent Services.

## Usage scenario
The aim is for the API to mirror the current process that happens through the Agent Services user interface
* Agent uses 3rd party application/software to request a new authorisation
* Agent identifier is passed to the API (ARN)
* Agent enters service they are requesting access to eg. MTD-IT, MTD-VAT
* Agent enters the identifier for the client they are requesting access for, e.g. NINO, CRN, VAT registration number
* If required by the service the agent enters a known fact check for the client, e.g. postcode, VAT registration date
* Link for the client to follow to authorise the agent is returned by the API. The expiration date of the link is also returned by the API
* Agent sends the link to the client
* Client clicks the link and authorises agent (requires sign on through Government Gateway)

### Versioning
Can not resolve https://developer.service.hmrc.gov.uk/api-documentation/assets/common/docs/versioning.md

### Errors
Can not resolve https://developer.service.hmrc.gov.uk/api-documentation/assets/common/docs/errors.md

---

## /agents/{arn}

#### Available endpoints

* [/agents/{arn}/invitations](#agentsarninvitations)* [/agents/{arn}/invitations/{invitationId}](#agentsarninvitationsinvitationid)

* [/agents/{arn}/relationships](#agentsarnrelationships)

### /agents/{arn}/invitations

* **arn**: The MTD platform Agent Registration Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

#### application/json (application/json) 
Create a new invitation.

```
{
  "service": ["MTD-IT"],
  "clientIdType": "ni",
  "clientId": "AA999999A",
  "knownFact": "AA11 1AA"
}
```
```
{
  "service": ["MTD-VAT"],
  "clientIdType": "vrn",
  "clientId": "101747696",
  "knownFact": "2007-05-18"
}
```
```
{
  "service": ["MTD-VAT"],
  "clientIdType": "crn",
  "clientId": "AA123456",
  "knownFact": "1234567890"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 204
The invitation was successfully created.

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Location | string | Location of the invitation that was created. | true | ``` /agents/AARN9999999/invitations/CS5AK7O8FPC43 ```  |

### Response code: 400

#### application/json (application/json) 

```
{
  "code": "SERVICE_NOT_SUPPORTED"
}
```
```
{
  "code": "CLIENT_ID_FORMAT_INVALID"
}
```
```
{
  "code": "POSTCODE_FORMAT_INVALID"
}
```
```
{
  "code": "VAT_REG_DATE_FORMAT_INVALID"
}
```
```
{
  "code": "CT_UTR_FORMAT_INVALID"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 401

#### application/json (application/json) 

```
{
  "code": "INVALID_CREDENTIALS"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 403

#### application/json (application/json) 

```
{
  "code": "CLIENT_REGISTRATION_NOT_FOUND"
}
```
```
{
  "code": "POSTCODE_DOES_NOT_MATCH"
}
```
```
{
  "code": "VAT_REG_DATE_DOES_NOT_MATCH"
}
```
```
{
  "code": "CT_UTR_DOES_NOT_MATCH"
}
```
```
{
  "code": "NOT_AN_AGENT"
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED"
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

---

### /agents/{arn}/invitations/{invitationId}

* **invitationId**: A unique invitation id
    * Type: string
    
    * Required: true

#### **GET** *(secured)*:

### Response code: 200

#### application/json (application/json) 
Returns the invitation.

```
{
  "_links": {
    "self": {
      "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
    }
  },
  "created": "2017-01-25T15:20:14.917Z",
  "expiresOn": "2017-02-04T00:00:00:000Z",
  "arn": "AARN9999999",
  "service": ["MTD-IT"],
  "status": "Pending",
  "clientActionUrl": "https://www.tax.service.gov.uk/invitations/CS5AK7O8FPC43"
}
```
```
{
  "_links": {
    "self": {
      "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
    }
  },
  "created": "2017-01-25T15:20:14.917Z",
  "updated": "2017-01-25T15:20:14.917Z",
  "arn": "AARN9999999",
  "service": ["MTD-VAT"],
  "status": "Accepted"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 401

#### application/json (application/json) 

```
{
  "code": "INVALID_CREDENTIALS"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 403

#### application/json (application/json) 

```
{
  "code": "NOT_AN_AGENT"
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED"
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 404

#### application/json (application/json) 

```
{
  "code": "INVITATION_NOT_FOUND"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

---
#### **DELETE** *(secured)*:

### Response code: 202
The invitation has been successfully cancelled

### Response code: 401

#### application/json (application/json) 

```
{
  "code": "INVALID_CREDENTIALS"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 403

#### application/json (application/json) 

```
{
  "code": "INVALID_INVITATION_STATUS"
}
```
```
{
  "code": "NOT_AN_AGENT"
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED"
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 404

#### application/json (application/json) 

```
{
  "code": "INVITATION_NOT_FOUND"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

---

### /agents/{arn}/relationships

* **arn**: The MTD platform Agent Registration Number.
    * Type: string
    
    * Required: true

#### **GET** *(secured)*:

#### application/json (application/json) 
Check Relationship based on details received.

```
{
  "service": ["MTD-IT"],
  "clientIdType": "ni",
  "clientId": "AA999999A",
  "knownFact": "AA11 1AA"
}
```
```
{
  "service": ["MTD-VAT"],
  "clientIdType": "vrn",
  "clientId": "101747696",
  "knownFact": "2007-05-18"
}
```
```
{
  "service": ["MTD-VAT"],
  "clientIdType": "crn",
  "clientId": "AA123456",
  "knownFact": "1234567890"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 204
Relationship is active. Agent has delegated authorisation for the client.

### Response code: 401

#### application/json (application/json) 

```
{
  "code": "INVALID_CREDENTIALS"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 403

#### application/json (application/json) 

```
{
  "code": "NOT_AN_AGENT"
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED"
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 404
Relationship is inactive. Agent does not have delegated authorisation for the client.

---

