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
