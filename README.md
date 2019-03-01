# Agent Authorisation API documentation version 1.0

### Overview
This API allows tax agents to request authorisation to act on a client's behalf for a specific Making Tax Digital (MTD) tax service and have the option to cancel the authorisation request.

The API also allows the agent to check the status of authorisations already requested and query active or inactive relationships.

This API has no effect on the existing XML API.

### Change Log
You can find the changelog in the [agent-authorisation-api](https://github.com/hmrc/agent-authorisation-api/blob/master/CHANGE_LOG.md) GitHub repository.

### Why use this API?
Agents often use software to perform services for their clients. The API will benefit these agents as it allows them to create a request for authorisation to act on a client's behalf for a specific Making Tax Digital service directly through software.

This process involves the agent providing their own personal identifier and some information about the client that they wish to act on behalf of.
This generates a link that the agent can send to their client. The client can then follow this link to authorise the agent  for a service.

This API will save an agent time as they currently need to use an Agent Services user interface to create an authorisation request using their agent services account.

### Usage scenario
The aim is for the API to mirror the current process that happens through the Agent Services user interface:

* An agent uses a third-party application or software to request a new authorisation
* An agent identifier - the Agent Reference Number (ARN) - is passed to the API
* The agent enters the service they are requesting access to, for example, sending Income Tax updates through software (MTD-IT) or sending VAT Returns through software (MTD-VAT)
* The agent enters the identifier for the client they are requesting authorisation from, for example:
    * National Insurance number (NINO)
    * VAT registration number (VRN)
* If required by the service the agent enters an additional identifier for the client, for example, the client's postcode or VAT registration date
* The API returns a link for the client to follow to authorise the agent and the date when the authorisation request will expire
* The agent sends the link to the client they wish to act on behalf of
* If the agent changes their mind, they can cancel the authorisation request as long as the client has not responded to it
* The client accesses the link and signs in using their a Government Gateway login details to accept the agent's request
* The agent can check if they have been authorised by a client.

The detailed guide how to create required client data in the External Tests (Sandbox) environment 
can be found at <https://test-www.tax.service.gov.uk/agents-external-stubs/help/agent-authorisation-api>

### Versioning
When an API changes in a way that is backwards-incompatible, we increase the version number of the API. 
See our [reference guide](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) for more on
versioning.

### Errors
We use standard [HTTP status codes](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#http-status-codes) to show whether an API request succeeded or not. They are usually in the range:
* 200 to 299 if it succeeded, including code 202 if it was accepted by an API that needs to wait for further action
* 400 to 499 if it failed because of a client error by your application
* 500 to 599 if it failed because of an error on our server

Errors specific to each API are shown in the Endpoints section, under Response. 
See our [reference guide](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#errors) for more on errors.

---

### /agents/{arn}/invitations

* **arn**: The Making Tax Digital (MTD) platform Agent Reference Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the response format and the [version](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) of the API to be used. | true | ``` application/vnd.hmrc.1.0+json ```  |

#### application/json (application/json) 
Create a new authorisation request. The request will expire after 14 days.

```
{
  "service": ["MTD-IT"],
  "clientType":"personal",
  "clientIdType": "ni",
  "clientId": "AA999999A",
  "knownFact": "AA11 1AA"
}
```
```
{
  "service": ["MTD-VAT"],
  "clientType":"business",
  "clientIdType": "vrn",
  "clientId": "101747696",
  "knownFact": "2007-05-18"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 204
The authorisation request was created successfully.

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Location | string | Location of the authorisation request that was created which will expire after 14 days. | true | ``` /agents/AARN9999999/invitations/CS5AK7O8FPC43 ```  |

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "SERVICE_NOT_SUPPORTED",
  "message": "The service requested is not supported. Check the API documentation to find which services are supported."
}
```
```
{
  "code": "CLIENT_TYPE_NOT_SUPPORTED",
  "message": "The client type requested is not supported. Check the API documentation to find which client types are supported."
}
```
```
{
  "code": "CLIENT_ID_FORMAT_INVALID",
  "message": "Client identifier must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "CLIENT_ID_DOES_NOT_MATCH_SERVICE",
  "message": "The type of client Identifier provided cannot be used with the requested service. Check the API documentation for details of the correct client identifiers to use."
}
```
```
{
  "code": "POSTCODE_FORMAT_INVALID",
  "message": "Postcode must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "VAT_REG_DATE_FORMAT_INVALID",
  "message": "VAT registration date must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported version number"
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported content-type."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Bad Request"
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
  "code": "UNAUTHORIZED",
  "message": "Bearer token is missing or not authorized."
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
  "message": "The details provided for this client do not match HMRC's records."
}
```
```
{
  "code": "POSTCODE_DOES_NOT_MATCH",
  "message": "The postcode provided does not match HMRC's record for the client."
}
```
```
{
  "code": "VAT_REG_DATE_DOES_NOT_MATCH",
  "message": "The VAT registration date provided does not match HMRC's record for the client."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "This agent needs to create an agent services account before they can use this service."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 406

#### errorResponse (application/json) 

```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Missing 'Accept' header."
}
```
```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Invalid 'Accept' header"
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 500

#### errorResponse (application/json) 

```
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Internal server error."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---
#### **GET** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the response format and the [version](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) of the API to be used. | true | ``` application/vnd.hmrc.1.0+json ```  |

### Response code: 200

#### application/json (application/json) 
Returns all authorisation requests for the last 30 days.

```
[{
  "_links": {
    "self": {
      "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
    }
  },
  "created": "2019-01-21T14:27:44.620Z",
  "arn": "AARN9999999",
  "service": ["MTD-IT"],
  "status": "Pending",
  "expiresOn": "2019-02-04T00:00:00:000Z",
  "clientActionUrl": "https://www.tax.service.gov.uk/invitations/personal/12345678/agent-1"
},
  {
    "_links": {
      "self": {
        "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
      }
    },
    "created": "2019-01-21T14:27:44.620Z",
    "arn": "AARN9999999",
    "service": ["MTD-IT"],
    "status": "Accepted",
    "updated": "2019-01-21T14:27:44.620Z"
  }
]
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 204
The agent has no authorisation requests for the last 30 days.

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported version number"
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported content-type."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Bad Request"
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
  "code": "UNAUTHORIZED",
  "message": "Bearer token is missing or not authorized."
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
  "message": "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "This agent needs to create an agent services account before they can use this service."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 406

#### errorResponse (application/json) 

```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Missing 'Accept' header."
}
```
```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Invalid 'Accept' header"
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 500

#### errorResponse (application/json) 

```
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Internal server error."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---

### /agents/{arn}/invitations/{invitationId}

* **invitationId**: A unique authorisation request ID
    * Type: string
    
    * Required: true

#### **GET** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the response format and the [version](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) of the API to be used. | true | ``` application/vnd.hmrc.1.0+json ```  |

### Response code: 200

#### application/json (application/json) 
Returns the authorisation request.

```
{
  "_links": {
    "self": {
      "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
    }
  },
  "created": "2019-01-21T14:27:44.620Z",
  "expiresOn": "2019-02-04T00:00:00.00",
  "arn": "AARN9999999",
  "service": ["MTD-IT"],
  "status": "Pending",
  "clientActionUrl": "https://www.tax.service.gov.uk/invitations/personal/12345678/agent-1"
}
```
```
{
  "_links": {
    "self": {
      "href": "/agents/AARN9999999/invitations/CS5AK7O8FPC43"
    }
  },
  "created": "2019-01-21T14:27:44.620Z",
  "updated": "2019-01-21T14:27:44.620Z",
  "arn": "AARN9999999",
  "service": ["MTD-VAT"],
  "status": "Accepted"
}
```

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported version number"
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported content-type."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Bad Request"
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
  "code": "UNAUTHORIZED",
  "message": "Bearer token is missing or not authorized."
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
  "message": "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "This agent needs to create an agent services account before they can use this service."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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
  "message": "The authorisation request cannot be found."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 406

#### errorResponse (application/json) 

```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Missing 'Accept' header."
}
```
```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Invalid 'Accept' header"
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 500

#### errorResponse (application/json) 

```
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Internal server error."
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
| Accept | string | Specifies the response format and the [version](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) of the API to be used. | true | ``` application/vnd.hmrc.1.0+json ```  |

### Response code: 204
The authorisation request has been cancelled successfully.

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported version number"
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported content-type."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Bad Request"
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
  "code": "UNAUTHORIZED",
  "message": "Bearer token is missing or not authorized."
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
  "message": "This authorisation request cannot be cancelled as the client has already responded to the request, or the request has expired."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "This agent needs to create an agent services account before they can use this service."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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
  "message": "The authorisation request cannot be found."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 406

#### errorResponse (application/json) 

```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Missing 'Accept' header."
}
```
```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Invalid 'Accept' header"
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 500

#### errorResponse (application/json) 

```
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Internal server error."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---

### /agents/{arn}/relationships

* **arn**: The Making Tax Digital (MTD) platform Agent Reference Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the response format and the [version](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) of the API to be used. | true | ``` application/vnd.hmrc.1.0+json ```  |

#### application/json (application/json) 
Check Relationship based on the details received.

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

##### *application/json*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|

### Response code: 204
Relationship is active. Agent is authorised to act for the client.

### Response code: 400

#### errorResponse (application/json) 

```
{
  "code": "SERVICE_NOT_SUPPORTED",
  "message": "The service requested is not supported. Check the API documentation to find which services are supported."
}
```
```
{
  "code": "CLIENT_ID_FORMAT_INVALID",
  "message": "Client identifier must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "CLIENT_ID_DOES_NOT_MATCH_SERVICE",
  "message": "The type of client Identifier provided cannot be used with the requested service. Check the API documentation for details of the correct client identifiers to use."
}
```
```
{
  "code": "POSTCODE_FORMAT_INVALID",
  "message": "Postcode must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "VAT_REG_DATE_FORMAT_INVALID",
  "message": "VAT registration date must be in the correct format. Check the API documentation to find the correct format."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported version number"
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Missing or unsupported content-type."
}
```
```
{
  "code": "BAD_REQUEST",
  "message": "Bad Request"
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
  "code": "UNAUTHORIZED",
  "message": "Bearer token is missing or not authorized."
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
  "message": "The details provided for this client do not match HMRC's records."
}
```
```
{
  "code": "POSTCODE_DOES_NOT_MATCH",
  "message": "The postcode provided does not match HMRC's record for the client."
}
```
```
{
  "code": "VAT_REG_DATE_DOES_NOT_MATCH",
  "message": "The VAT registration date provided does not match HMRC's record for the client."
}
```
```
{
  "code": "NOT_AN_AGENT",
  "message": "This user does not have a Government Gateway agent account. They need to create an Government Gateway agent account before they can use this service."
}
```
```
{
  "code": "AGENT_NOT_SUBSCRIBED",
  "message": "This agent needs to create an agent services account before they can use this service."
}
```
```
{
  "code": "NO_PERMISSION_ON_AGENCY",
  "message": "The user that is signed in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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
  "code": "RELATIONSHIP_NOT_FOUND",
  "message": "Relationship is inactive. Agent is not authorised to act for this client."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 406

#### errorResponse (application/json) 

```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Missing 'Accept' header."
}
```
```
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "Invalid 'Accept' header"
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

### Response code: 500

#### errorResponse (application/json) 

```
{
  "code": "INTERNAL_SERVER_ERROR",
  "message": "Internal server error."
}
```

##### *errorResponse*:
| Name | Type | Description | Required | Pattern |
|:-----|:----:|:------------|:--------:|--------:|
| code |  string |  | true |  |

---

