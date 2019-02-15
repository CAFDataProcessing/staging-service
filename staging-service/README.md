# Staging Service Operations

### GET /status : Returns status information about the staging service. A client should use this to check that the service is functional before uploading a large batch.
#### curl -X GET "http://curly-slv04.swinfra.net:31506/status" -H "accept: application/json"

### PUT /batches/{batchId} : Add documents to a batch. The batch will be automatically created if it doesn't already exist.
#### curl -X PUT "http://curly-slv04.swinfra.net:31506/batches/abc" -i -v -d @batches-put-payload.txt -H "Content-Type: multipart/mixed; boundary=--------------------------7f9655a614526b44"

`Contents of batches-put-payload.txt`
```
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="A_Christmas_Carol.txt"
Content-Type: text/plain

Marley was dead: to begin with. There is no doubt whatever about that. The register of his burial was signed by the clergyman, the clerk, the undertaker, and the chief mourner. Scrooge signed it: and Scrooge’s name was good upon ’Change, for anything he chose to put his hand to. Old Marley was as dead as a door-nail.
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="Front_Cover.jpg"
Content-Type: image/jpeg
Content-Transfer-Encoding: base64

QSBDaHJpc3RtYXMgQ2Fyb2wgQm9vayBDb3Zlcg==
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="family1"
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
           "data": "A_Christmas_Carol.txt",
           "encoding": "local_ref"
          },
        "COVER_PIC": {
            "data": "Front_Cover.jpg",
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
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="family2"
Content-Type: application/document+json

{
  "document": {
    "reference": "least-fav-book.msg",
    "fields": {
      "FROM": "Mark Roberts",
      "TO": "Gene Simmons",
      "SUBJECT": "Least Favourite book",
      "CONTENT": "Category Theory for Programmers - never read it"
    }
  }
}
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="family3"
Content-Type: application/document+json

{
  "document": {
    "reference": "ingestion-data-flow.md",
    "fields": {
      "STATUS": "UPDATED",
      "TITLE": "Ingestion Data Flow",
      "COMPANY": "Micro Focus International plc",
      ...
    }
  }
}
--------------------------7f9655a614526b44
Content-Disposition: form-data; name="family4"
Content-Type: application/document+json

{
  "document": {
    "reference": "happy-birthday.msg",
    "fields": {
      "STATUS": "DELETED"
    }
  }
}
--------------------------7f9655a614526b44

```

### GET /batches : Retrieve the current list of batches in alphabetical order.
##### Query Parameters
- **startsWith**: (string) Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.
- **from**: (string) Specifies the identifier to fetch batches that follow it alphabetically.
- **limit**: (integer) Specifies the number of results to return (defaults to 25 if not specified).

#### curl -X GET "http://curly-slv04.swinfra.net:31506/batches?limit=25&startsWith=abc&from=a" -H "accept: application/json"

### DELETE /batches/{batchId} : Delete specified batch.
#### curl -X DELETE "http://curly-slv04.swinfra.net:31506/batches/abc" -H "accept: application/json"
