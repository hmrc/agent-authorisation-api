This API allows agents to request authorisation to act on a client's behalf for the different MTD tax services. The API also allows the Agent to check the status of authorisations already requested. Please note this API has no effect on the existing XML API. 

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