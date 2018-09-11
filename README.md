# Agent Authorisation API documentation version 0.0

### Overview
This API allows agents to request authorisation to act on a client's behalf for the different MTD tax services and have the option to cancel the authorisation. The API also allows the Agent to check the status of authorisations already requested and query active or inactive relationships. Please note this API has no effect on the existing XML API. 

### APIs
* [/agent/:arn/invitations](#agentsarninvitations)
    * [POST](#post-secured) Create a new invitation.
    * [/:invitationId](#agentsarninvitationsinvitationid)
        * [GET](#get-secured) Returns the invitation object
        * [DELETE](#delete-secured) Cancels the invitation.
* [/agent/:arn/relationships](#agentsarnrelationships)
    * [GET](#get-secured-1) Check Status of Relationship   

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
* If the Agent decides to change their mind, they have the option to cancel the invitation as long as it has not been responded by the client.
* Client clicks the link and authorises agent (requires sign on through Government Gateway)
* The Agent can check if they have an active relationship for delegated authorisation to act on behalf of a client.

### Versioning
Specific versions are requested by providing an Accept header. When
backwards-incompatible API changes are made, a new version will be released.
Backwards-compatible changes are released in the current version without the
need to change your Accept header.  See our [reference guide](/api-documentation/docs/reference-guide#versioning) for more on
versioning.

### Errors
We use standard [HTTP status codes](/api-documentation/docs/reference-guide#http-status-codes) to show whether an API request succeeded or not. They're usually:
* in the 200 to 299 range if it succeeded; including code 202 if it was accepted by an API that needs to wait for further action
* in the 400 to 499 range if it didn't succeed because of a client error by your application
* in the 500 to 599 range if it didn't succeed because of an error on our server

Errors specific to each API are shown in its own Resources section, under Response. 
See our [reference guide](/api-documentation/docs/reference-guide#errors) for more on errors.

---

### /agents/{arn}/invitations

* **arn**: The MTD platform Agent Registration Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

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

#### errorResponse (application/json) 

```
{
  "code": "SERVICE_NOT_SUPPORTED",
  "message": "The specified service is not supported."
}
```
```
{
  "code": "CLIENT_ID_FORMAT_INVALID",
  "message": "The submitted clientId does not match the expected format."
}
```
```
{
  "code": "POSTCODE_FORMAT_INVALID",
  "message": "The submitted postcode does not match the expected format."
}
```
```
{
  "code": "VAT_REG_DATE_FORMAT_INVALID",
  "message": "The submitted VAT registration date does not match the expected format."
}
```
```
{
  "code": "CT_UTR_FORMAT_INVALID",
  "message": "The submitted CT UTR does not match the expected format."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 401

#### errorResponse (application/json) 

```
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid Authentication information provided."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 403

#### errorResponse (application/json) 

```
{
  "code": "CLIENT_REGISTRATION_NOT_FOUND",
  "message": "The Client's registration was not found."
}
```
```
{
  "code": "POSTCODE_DOES_NOT_MATCH",
  "message": "The submitted postcode did not match the client's postcode as held by HMRC."
}
```
```
{
  "code": "VAT_REG_DATE_DOES_NOT_MATCH",
  "message": "The submitted VAT registration date did not match HMRC record for the client."
}
```
```
{
  "code": "CT_UTR_DOES_NOT_MATCH",
  "message": "The submitted CT UTR did not match HMRC record for the client."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "The logged in user is not an agent."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "The Agent is not subscribed to Agent Services."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The logged in user is not permitted to access invitations for the specified agency."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---

### /agents/{arn}/invitations/{invitationId}

* **invitationId**: A unique invitation id
    * Type: string
    
    * Required: true

#### **GET** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

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

#### errorResponse (application/json) 

```
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid Authentication information provided."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 403

#### errorResponse (application/json) 

```
{
  "code": "NOT_AN_AGENT",
  "message": "The logged in user is not an agent."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "The Agent is not subscribed to Agent Services."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The logged in user is not permitted to access invitations for the specified agency."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 404

#### errorResponse (application/json) 

```
{
  "code": "INVITATION_NOT_FOUND",
  "message": "The specified invitation was not found."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---
#### **DELETE** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

### Response code: 202
The invitation has been successfully cancelled

### Response code: 401

#### errorResponse (application/json) 

```
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid Authentication information provided."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 403

#### errorResponse (application/json) 

```
{
  "code": "INVALID_INVITATION_STATUS",
  "message": "The requested state transition is not permitted given the invitation's current status."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "The logged in user is not an agent."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "The Agent is not subscribed to Agent Services."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The logged in user is not permitted to access invitations for the specified agency."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 404

#### errorResponse (application/json) 

```
{
  "code": "INVITATION_NOT_FOUND",
  "message": "The specified invitation was not found."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---

### /agents/{arn}/relationships

* **arn**: The MTD platform Agent Registration Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

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

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "SERVICE_NOT_SUPPORTED",
  "message": "The specified service is not supported."
}
```
```
{
  "code": "CLIENT_ID_FORMAT_INVALID",
  "message": "The submitted clientId does not match the expected format."
}
```
```
{
  "code": "POSTCODE_FORMAT_INVALID",
  "message": "The submitted postcode does not match the expected format."
}
```
```
{
  "code": "VAT_REG_DATE_FORMAT_INVALID",
  "message": "The submitted VAT registration date does not match the expected format."
}
```
```
{
  "code": "CT_UTR_FORMAT_INVALID",
  "message": "The submitted CT UTR does not match the expected format."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 401

#### errorResponse (application/json) 

```
{
  "code": "INVALID_CREDENTIALS",
  "message": "Invalid Authentication information provided."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 403

#### errorResponse (application/json) 

```
{
  "code": "CLIENT_REGISTRATION_NOT_FOUND",
  "message": "The Client's registration was not found."
}
```
```
{
  "code": "POSTCODE_DOES_NOT_MATCH",
  "message": "The submitted postcode did not match the client's postcode as held by HMRC."
}
```
```
{
  "code": "VAT_REG_DATE_DOES_NOT_MATCH",
  "message": "The submitted VAT registration date did not match HMRC record for the client."
}
```
```
{
  "code": "CT_UTR_DOES_NOT_MATCH",
  "message": "The submitted CT UTR did not match HMRC record for the client."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "The logged in user is not an agent."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "The Agent is not subscribed to Agent Services."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The logged in user is not permitted to access invitations for the specified agency."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 404
Relationship is inactive. Agent does not have delegated authorisation for the client.

---

