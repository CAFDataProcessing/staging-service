# Staging Service Operations

### GET /status : 
Returns status information about the staging service. A client should use this to check that the service is functional before uploading a large batch.
#### curl -X GET "http://localhost:8080/status" -H "accept: application/json"

### PUT /batches/{batchId} : 
Upload documents to a batch. The batch will be automatically created if it doesn't already exist.
#### curl -X PUT -i -v  http://localhost:8080/batches/abcBatch --data-binary @batches-put-payload.txt -H "Content-Type: multipart/mixed; boundary=efb8369b-607b-4dcf-9f92-e6cd8244db1e"

##### Contents of batches-put-payload.txt (should have Windows line-endings)
```
--efb8369b-607b-4dcf-9f92-e6cd8244db1e
Content-Disposition: form-data; name="A_Christmas_Carol1.txt"
Content-Type: application/octet-stream

Marley was dead: to begin with. There is no doubt whatever about that. The register of his burial was signed by the clergyman, the clerk, the undertaker, and the chief mourner. Scrooge signed it: and Scrooge s name was good upon  Change, for anything he chose to put his hand to. Old Marley was as dead as a door-nail.
--efb8369b-607b-4dcf-9f92-e6cd8244db1e
Content-Disposition: form-data; name="A_Christmas_Carol2.txt"
Content-Type: application/octet-stream

About Christmas
--efb8369b-607b-4dcf-9f92-e6cd8244db1e
Content-Disposition: form-data; name="batch1.json"
Content-Type: application/document+json

{
  "document": {
    "reference": "fav-book.msg",
    "fields": {
      "FROM": "Mark Roberts",
      "TO": "Gene Simmons",
      "SUBJECT": "Favourite book",
      "CONTENT": "This is the book that popularised the use of the phrase \"Merry Christmas\"."
    },
    "subdocuments": [{
      "reference": "xmas-carol.doc",
      "fields": {
        "BINARY_FILE": {
           "data": "http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc",
           "encoding": "storage_ref"
          },
        "TITLE": "A Christmas Carol",
        "AUTHOR": "Charles Dickens",
        "PUB_DATE": "December 19, 1843",
        "CONTENT": {
           "data": "A_Christmas_Carol1.txt",
           "encoding": "local_ref"
          },
        "SUMMARY": {
            "data": "A_Christmas_Carol2.txt",
            "encoding": "local_ref"
          },
        "PUBLISHER": [
          "Chapman and Hall",
          "Elliot Stock"
        ]
      }
    }]
  }
}
--efb8369b-607b-4dcf-9f92-e6cd8244db1e
Content-Disposition: form-data; name="batch2.json"
Content-Type: application/document+json

{
  "document": {
    "reference": "batch2.msg",
    "fields": {
      "FROM": "Mark Roberts",
      "TO": "Gene Simmons",
      "SUBJECT": "Favourite book",
      "CONTENT": "This is the book that popularised the use of the phrase \"Merry Christmas\"."
    }
  }
}
--efb8369b-607b-4dcf-9f92-e6cd8244db1e
Content-Disposition: form-data; name="batch3.json"
Content-Type: application/document+json

{
  "document": {
    "reference": "batch3.msg",
    "fields": {
      "FROM": "Mark Roberts",
      "TO": "Gene Simmons",
      "SUBJECT": "Favourite book",
      "CONTENT": "This is the book that popularised the use of the phrase \"Merry Christmas\"."
    },
    "subdocuments": [{
      "reference": "xmas-carol.doc",
      "fields": {
        "BINARY_FILE": {
           "data": "http://www.lang.nagoya-u.ac.jp/~matsuoka/misc/urban/cd-carol.doc",
           "encoding": "storage_ref"
          },
        "TITLE": "A Christmas Carol",
        "AUTHOR": "Charles Dickens",
        "PUB_DATE": "December 19, 1843",
        "CONTENT": {
           "data": "A_Christmas_Carol1.txt",
           "encoding": "local_ref"
          },
        "SUMMARY": {
            "data": "A_Christmas_Carol2.txt",
            "encoding": "local_ref"
          },
        "PUBLISHER": [
          "Chapman and Hall",
          "Elliot Stock"
        ]
      }
    }]
  }
}
--efb8369b-607b-4dcf-9f92-e6cd8244db1e--

```

### GET /batches : 
Retrieve the current list of batches in alphabetical order.
##### Query Parameters
- **startsWith**: (string) Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.
- **from**: (string) Specifies the identifier to fetch batches that follow it alphabetically.
- **limit**: (integer) Specifies the number of results to return (defaults to 25 if not specified).

#### curl -X GET "http://curly:8080/batches?limit=25&startsWith=abc&from=a" -H "accept: application/json"

### DELETE /batches/{batchId} : 
Delete specified batch.
#### curl -X DELETE "http://curly:8080/batches/abc" -H "accept: application/json"
