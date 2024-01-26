# Staging Service Internal Client

This project is a Java library used internally for testing the [Staging Service](../staging-service). It allows callers to upload batches, get batches and delete batches.

## Usage

This project builds a Java library that can be used to make calls to the Staging Service.  It is used by the [acceptance tests](../staging-service-acceptance-tests) project.

#### getStatus(String tenantId) returns StagingStatusResponse
Returns status information about the staging service. Checks that the service is functional before uploading a large batch.  
- **tenantId**: Tenant identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)

#### createOrReplaceBatch(String tenantId, String batchId, Stream<MultiPart> uploadData)
Upload documents to a batch. The batch will be automatically created if it doesn't already exist.

- **tenantId**: Tenant identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)
- **batchId**: Batch identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)
- **uploadData**: Stream of multiple document families and associated files

#### getBatches(String tenantId, String startsWith, String from, Integer limit) returns StagingBatchList
Retrieve the current list of batches in alphabetical order.

- **tenantId**: Tenant identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)
- **startsWith**: Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.
- **from**: Specifies the identifier to fetch batches that follow it alphabetically.
- **limit**: Specifies the number of results to return (defaults to 25).

#### deleteBatch(String tenantId, String batchId)
Delete specified batch.

- **tenantId**: Tenant identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)
- **batchId**: Batch identifier (should match the following regex: `^[a-z0-9,.()\-+_!]{0,127}[a-z0-9,()\-+_!]$`)

#### Sample code snippet
```
final String stagingServiceURI = "http://localhost:8080";
final ApiClient apiClient = new ApiClient();
apiClient.setBasePath(stagingServiceURI);
stagingApi = new StagingApi();
stagingApi.setApiClient(apiClient);

final String tenantId = "acme-com";

// To get the status of the staging service
final StagingStatusResponse status = stagingApi.getStatus(tenantId);
System.out.println("Staging service status : " + status.getMessage());

// To upload a batch
final String batchId = "test-batch";
final String[] contentFiles = new String[]{"A_Christmas_Carol1.txt", "A_Christmas_Carol2.txt"};
final String[] documentFiles = new String[]{"batch1.json", "batch2.json", "batch3.json",
                               "batch4.json", "batch5.json", "batch6.json"};
final List<MultiPart> uploadData = new ArrayList<>();
for (final String file : contentFiles) {
    uploadData.add(new MultiPartContent(file, getClass().getResource("/" + file)));
}
for (final String file : documentFiles) {
    uploadData.add(new MultiPartDocument(getClass().getResource("/" + file)));
}
try {
    stagingApi.createOrReplaceBatch(tenantId, batchId, uploadData.stream());
} catch (final ApiException ex) {
    System.out.println("Upload batch failed : " + ex.getMessage()
            + " response code : " + ex.getCode()
            + " response body : " + ex.getResponseBody());
}
// To list batches
final StagingBatchList batches = stagingApi.getBatches(tenantId, "test", testBat, 10);
System.out.println("Batch list : " + batches.getEntries());

// To delete a batch
stagingApi.deleteBatch(tenantId, batchId);
        
```