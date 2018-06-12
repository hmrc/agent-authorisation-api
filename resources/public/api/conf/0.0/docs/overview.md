An API allowing MTD-enabled Agents to request authorisation to a service for a client, instead of filling the 64-8 paper form.

##API usage scenario
  The aim is for the API to mirror the current process that happens through the Agent Services user interface
  * Agent uses 3rd party application/software to request a new authorisation
  * Agent identifier is passed to the API (ARN)
  * Agent enters service they are requesting access to eg. ITSA, VAT, IRV
  * Agent enters the identifier for the client they are requesting access for eg. NINO, VAT registration number
  * If required by the service the agent enters a known fact check for the client eg. postcode, DOB
  * Link for client to authorise the agent is returned by the API. The expiration date of the link is also returned by the API
  * Agent sends the link to the client
  * Client clicks the link and authorises agent


##Possible alternative
   Individuals or businesses can use 64-8 paper form to authorise agents to deal with HMRC on their behalf. Clients are able to authorise Agents to deal with HMRC on their behalf for any or more of SA, Tax Credits, Corporation Tax, PAYE and VAT
   Agents can also request authorisation to a service for a client through an Agent Services user interface. This process asks for the service the want authorisation for (eg. VAT, ITSA), the client they want authorisation for (eg. NINO, VAT registration number) and sometimes a known fact check (eg. DOB, postcode) to make sure the agent knows the client and is not just spamming people. After this request has been successfully submitted the agent is given a link to send to their client which the client must follow to then grant the access requested by the agent
