# Staging Service Operations

### GET /status : 
Returns status information about the staging service. A client should use this to check that the service is functional before uploading a large batch.
#### curl -X GET "http://curly-slv04.swinfra.net:31506/status" -H "accept: application/json"

### PUT /batches/{batchId} : 
Add documents to a batch. The batch will be automatically created if it doesn't already exist.
#### curl -X PUT "http://curly-slv01.swinfra.net:31401/batches/abc4444" -i -v --form "uploadData=@A_Christmas_Carol1.txt;type=text/plain" --form "uploadData=@A_Christmas_Carol2.txt;type=text/plain" --form "uploadData=@batch1.json;type=application/document+json" -H "Content-Type: multipart/mixed;"

### GET /batches : 
Retrieve the current list of batches in alphabetical order.
##### Query Parameters
- **startsWith**: (string) Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.
- **from**: (string) Specifies the identifier to fetch batches that follow it alphabetically.
- **limit**: (integer) Specifies the number of results to return (defaults to 25 if not specified).

#### curl -X GET "http://curly-slv04.swinfra.net:31506/batches?limit=25&startsWith=abc&from=a" -H "accept: application/json"

### DELETE /batches/{batchId} : 
Delete specified batch.
#### curl -X DELETE "http://curly-slv04.swinfra.net:31506/batches/abc" -H "accept: application/json"
