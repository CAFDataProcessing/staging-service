# Staging Service Client

This project is a Java library to communicate with the [Staging Service](../staging-service). It allows callers to upload batches, get batches and delete batches. 

## Usage

This project builds a Java library that can be used to make calls to the Staging Service. The library should take a dependency on `staging-service-client` using the following Maven coordinates:

    <dependency>
        <groupId>com.github.cafdataprocessing</groupId>
        <artifactId>staging-service-client</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>

The [integration tests](../staging-service-acceptance-tests) show how the staging-service-client can be used.

#### getStatus() returns StagingStatusResponse
Returns status information about the staging service. A client should use this to check that the service is functional before uploading a large batch.

#### createOrReplaceBatch(String batchId, Stream<MultiPart> uploadData)
Upload documents to a batch. The batch will be automatically created if it doesn't already exist.

- **batchId**: Batch identifier (should match the following regex : "^[^\\\\/:*?\"|]+$")
- **uploadData**: Stream of multiple document families and associated files

#### getBatches(String startsWith, String from, Integer limit) returns StagingBatchList
Retrieve the current list of batches in alphabetical order.

- **startsWith**: Specifies the prefix for batch identifier to fetch batches whose identifiers start with the specified value.
- **from**: Specifies the identifier to fetch batches that follow it alphabetically.
- **limit**: Specifies the number of results to return (defaults to 25).

#### deleteBatch(String batchId)
Delete specified batch.

- **batchId**: Batch identifier (should match the following regex : "^[^\\\\/:*?\"|]+$")

#### Sample code snippet
```
final String stagingServiceURI = "http://localhost:8080";
final ApiClient apiClient = new ApiClient();
apiClient.setBasePath(stagingServiceURI);
stagingApi = new StagingApi();
stagingApi.setApiClient(apiClient);

// To get the status of the staging service
final StagingStatusResponse status = stagingApi.getStatus();
System.out.println("Staging service status : " + status.getMessage());

// To upload a batch
final String batchId = "testBatch";
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
    stagingApi.createOrReplaceBatch(batchId, uploadData.stream());
} catch (final ApiException ex) {
    System.out.println("Upload batch failed : " + ex.getMessage()
            + " response code : " + ex.getCode()
            + " response body : " + ex.getResponseBody());
}
// To list batches
final StagingBatchList batches = stagingApi.getBatches("test", testBat, 10);
System.out.println("Batch list : " + batches.getEntries());

// To delete a batch
stagingApi.deleteBatch(batchId);
        
```