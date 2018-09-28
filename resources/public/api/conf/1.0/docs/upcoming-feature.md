The following feature is currently not available but it may be available in a future release.

Request Body:

Create a new authorisation (via CRN and UTR)

/agents/:arn/invitations: 

<pre class="code--block">
{
  "service": ["MTD-VAT"],
  "clientIdType": "crn",
  "clientId": "AA123456",
  "knownFact": "1234567890"
}
</pre>

Response Header:

Location : /agents/AARN9999999/invitations/CS5AK7O8FPC43

Error Responses:

Http Error Code: 400

<pre class="code--block">
{
  "code": "CT_UTR_FORMAT_INVALID",
  "message": "Corporation Tax Unique Taxpayer Reference must be in the correct format. Check the API documentation to find the correct format."
}
</pre>

Http Error Code: 403

<pre class="code--block">
{
  "code": "CT_UTR_DOES_NOT_MATCH",
  "message": " The submitted CT UTR did not match HMRC record for the client."
}
</pre>
