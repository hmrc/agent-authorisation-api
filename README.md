# Agent Authorisation API documentation version 0.0

### Overview
This API allows tax agents to request authorisation to act on a client's behalf for a specific Making Tax Digital (MTD) tax service and have the option to cancel the authorisation request.

The API also allows the agent to check the status of authorisations already requested and query active or inactive relationships.

This API has no effect on the existing XML API.

### Why use this API?
Agents often use software to perform services for their clients. 
The API will benefit these agents since it will allow them to be able to request the invitation link directly through software, to send to their client so that they can authorise the agent for a service. 
This will save an agent time since currently they must separately log into Agent Services UI to request this link. 
This also aligns with the API first strategy for Agent Services.

### Usage scenario
The aim is for the API to mirror the current process that happens through the Agent Services user interface:
* An gent uses a third-party application or software to request a new authorisation
* An agent identifier - the Agent Reference Number (ARN) - is passed to the API
* The agent enters the service they are requesting access to, for example, sending Income Tax updates through software (MTD-IT) or sending VAT Returns through software (MTD-VAT)
* The agent enters the identifier for the client they are requesting authorisation from, for example:
    * National Insurance number (NINO)
    * Company registration number (CRN)
    * VAT registration number (VRN)
* If required by the service the agent enters an additional identifer for the client, for example, the client's postcode or VAT registration date
* The API returns a link for the client to follow to authorise the agent and the date when the authorisation request will expire
* The agent sends the link to the client they wish to act on behalf of
* If the agent changes their mind, they can cancel the authorisation request as long as the client has not responded to it
* The agent accesses the link and signs in using their a Government Gateway login details to accept the agent's request
* The agent can check if they have been authorised by a client.

### Upcoming Feature
The following feature is currently not available but it is expected to be available in a future release.

Request Body:

Create a new invitation (via CRN and UTR)

/agents/:arn/invitations: 

```json
{
  "service": ["MTD-VAT"],
  "clientIdType": "crn",
  "clientId": "AA123456",
  "knownFact": "1234567890"
}
```

Response Header:

Location : /agents/AARN9999999/invitations/CS5AK7O8FPC43

Error Responses:

Http Error Code: 400
```json
{
  "code": "CT_UTR_FORMAT_INVALID",
  "message": "Corporation Tax Unique Taxpayer Reference must be in the correct format. Check the API documentation to find the correct format."
}
```

Http Error Code: 403
```json
{
  "code": "CT_UTR_DOES_NOT_MATCH",
  "message": " The submitted CT UTR did not match HMRC record for the client."
}
```

### Versioning
Specific versions are requested by providing an Accept header. When
backwards-incompatible API changes are made, a new version will be released.
Backwards-compatible changes are released in the current version without the
need to change your Accept header.  See our [reference guide](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning) for more on
versioning.

### Errors
We use standard [HTTP status codes](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#http-status-codes) to show whether an API request succeeded or not. They're usually:
* in the 200 to 299 range if it succeeded; including code 202 if it was accepted by an API that needs to wait for further action
* in the 400 to 499 range if it didn't succeed because of a client error by your application
* in the 500 to 599 range if it didn't succeed because of an error on our server

Errors specific to each API are shown in its own Resources section, under Response. 
See our [reference guide](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#errors) for more on errors.

---

### /agents/{arn}/invitations

* **arn**: The MTD platform Agent Reference Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

#### application/json (application/json) 
Create a new authorisation request.

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
  "message": "The account used to sign in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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
| Accept | string | Specifies the version of the API that you want to call. See [versioning](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

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
  "message": "The account used to sign in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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

---
#### **DELETE** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

### Response code: 204
The invitation has been successfully cancelled

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
  "message": "The account used to sign in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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

---

### /agents/{arn}/relationships

* **arn**: The MTD platform Agent Registration Number.
    * Type: string
    
    * Required: true

#### **POST** *(secured)*:

###### Headers

| Name | Type | Description | Required | Examples |
|:-----|:----:|:------------|:--------:|---------:|
| Accept | string | Specifies the version of the API that you want to call. See [versioning](https://www.tax.service.gov.uk/api-documentation/docs/reference-guide#versioning). | true | ``` application/vnd.hmrc.1.0+json ```  |

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
  "message": "The account used to sign in cannot access this authorisation request. Their details do not match the agent business that created the authorisation request."
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

---

