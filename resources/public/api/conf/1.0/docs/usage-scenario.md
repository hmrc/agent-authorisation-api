The aim is for the API to mirror the current process that happens through the Agent Services user interface:

* An agent uses a third-party application or software to request a new authorisation
* An agent identifier - the Agent Reference Number (ARN) - is passed to the API
* The agent enters the service they are requesting access to, for example, sending Income Tax updates through software (MTD-IT) or sending VAT Returns through software (MTD-VAT)
* The agent enters the identifier for the client they are requesting authorisation from, for example:
    * National Insurance number (NINO)
    * Company registration number (CRN)
    * VAT registration number (VRN)
* If required by the service the agent enters an additional identifier for the client, for example, the client's postcode or VAT registration date
* The API returns a link for the client to follow to authorise the agent and the date when the authorisation request will expire
* The agent sends the link to the client they wish to act on behalf of
* If the agent changes their mind, they can cancel the authorisation request as long as the client has not responded to it
* The agent accesses the link and signs in using their a Government Gateway login details to accept the agent's request
* The agent can check if they have been authorised by a client.

The detailed guide how to create required client data in the External Tests (Sandbox) environment 
can be found at <https://test-www.tax.service.gov.uk/agents-external-stubs/help/agent-authorisation-api>
