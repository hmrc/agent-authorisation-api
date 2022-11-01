## [0.68.0](https://github.com/hmrc/agent-authorisation-api/releases/tag/v0.68.0) 18 April 2019

* Moved the APIs from Private Beta to Public Beta 

## [0.49.0](https://github.com/hmrc/agent-authorisation-api/releases/tag/v0.48.0) 31 Janurary 2019

* Added Mandatory parameter field, Client Type for  POST /agents/{arn}/invitations
    * For Schema [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/schemas/create-invitation.json)
    * Example Create Invitation Json Body for:
         *  MTD-IT [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/examples/post-agency-invitations-example.json)
         *  MTD-VAT [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/examples/post-agency-invitations-vat-example.json)

* Added CLIENT_TYPE_NOT_SUPPORTED Error Response [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/application.raml#L104) 
            
* Updated Client Action Url to the Agent Specific Link for: 
    * GET /agents/{arn}/invitations/invitationId
    * GET /agents/{arn}/invitations  
    
    [See here](https://github.com/hmrc/agent-authorisation-api/commit/5815acb81321889b7bcc638be6714265da2555ca#diff-04c6e90faac2675aa89e2176d2eec7d8R310) for comparison
  
* Updated Documentation for POST /agents/{arn}/relationships [See here](https://github.com/hmrc/agent-authorisation-api/blob/v0.49.0/resources/public/api/conf/1.0/application.raml#L240)
    * For Schema [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/schemas/check-relationship.json)
    * Example Check Relationship Json Body for 
        * MTD-IT [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/examples/post-agency-check-relationship-itsa-example.json)
        * MTD-VAT [See here](https://github.com/hmrc/agent-authorisation-api/blob/main/resources/public/api/conf/1.0/examples/post-agency-check-relationship-vat-example.json)
        