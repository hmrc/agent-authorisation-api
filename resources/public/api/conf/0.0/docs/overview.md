An API allowing MTD-enabled Agents to request authorisation to a service for a client, instead of filling the 64-8 paper form.

##Motivation
Agents often use software to perform services for their clients. 
The API will benefit these agents since it will allow them to be able to request the invitation link to authorise an agent for a service directly through software. 
This will save an agent time since currently an agent must separately log into Agent Services to request this link. 
This also aligns with the API first strategy for Agent Services.

##Usage scenario
  The aim is for the API to mirror the current process that happens through the Agent Services user interface
  * Agent uses 3rd party application/software to request a new authorisation
  * Agent identifier is passed to the API (ARN)
  * Agent enters service they are requesting access to eg. ITSA, VAT, IRV
  * Agent enters the identifier for the client they are requesting access for eg. NINO, VAT registration number
  * If required by the service the agent enters a known fact check for the client eg. postcode, DOB, VAT registration date
  * Link for client to authorise the agent is returned by the API. The expiration date of the link is also returned by the API
  * Agent sends the link to the client
  * Client clicks the link and authorises agent
