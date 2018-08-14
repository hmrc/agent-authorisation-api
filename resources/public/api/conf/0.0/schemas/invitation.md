
#  Schema

| Abstract | Extensible | Status | Identifiable | Custom Properties | Additional Properties | Defined In |
|----------|------------|--------|--------------|-------------------|-----------------------|------------|
| Can be instantiated | No | Experimental | No | Forbidden | Permitted | [invitation.json](invitation.json) |

#  Properties

| Property | Type | Required | Defined by |
|----------|------|----------|------------|
| [_links](#_links) | `object` | **Required** |  (this schema) |
| [arn](#arn) | `string` | **Required** |  (this schema) |
| [clientActionUrl](#clientactionurl) | `string` | Optional |  (this schema) |
| [created](#created) | `string` | **Required** |  (this schema) |
| [expiresOn](#expireson) | `string` | Optional |  (this schema) |
| [service](#service) | reference | **Required** |  (this schema) |
| [status](#status) | `string` | **Required** |  (this schema) |
| [updated](#updated) | `string` | Optional |  (this schema) |
| `*` | any | Additional | this schema *allows* additional properties |

## _links


`_links`
* is **required**
* type: `object`
* defined in this schema

### _links Type


`object` with following properties:


| Property | Type | Required |
|----------|------|----------|
| `self`| object | **Required** |



#### self

A link to the current resource

`self`
* is **required**
* type: `object`

##### self Type

Unknown type `object`.

```json
{
  "type": "object",
  "description": "A link to the current resource",
  "properties": {
    "href": {
      "type": "string"
    }
  },
  "required": [
    "href"
  ],
  "simpletype": "`object`"
}
```










## arn

The Agent Registration Number for the calling agency

`arn`
* is **required**
* type: `string`
* defined in this schema

### arn Type


`string`






## clientActionUrl

Link for the client to authorise/reject this agent's invitation.

`clientActionUrl`
* is optional
* type: `string`
* defined in this schema

### clientActionUrl Type


`string`






## created

Creation time of the request (RFC3339 / ISO8601 format)

`created`
* is **required**
* type: `string`
* defined in this schema

### created Type


`string`






## expiresOn

Expiration time of the request (RFC3339 / ISO8601 format)

`expiresOn`
* is optional
* type: `string`
* defined in this schema

### expiresOn Type


`string`






## service


`service`
* is **required**
* type: reference
* defined in this schema

### service Type


* [service.json](service.md) – `service.json`





## status

The current status of the invitation

`status`
* is **required**
* type: `string`
* defined in this schema

### status Type


`string`






## updated

Update time of the request (RFC3339 / ISO8601 format)

`updated`
* is optional
* type: `string`
* defined in this schema

### updated Type


`string`





