#### Version Number
${version-number}

#### New Features
- US616155: This worker now supports message prioritization.  

  Messages intended for this worker can be redirected to one or more staging queues instead of the worker's target queue.  This feature can be enabled by setting the `CAF_WORKER_ENABLE_DIVERTED_TASK_CHECKING` environment variable to `false` on **this worker**, and setting the `CAF_WMP_ENABLED` environment variable to `true` on the **component that routes messages to this worker**.    

- US585006: Added a new GET `batchStatus` endpoint for getting the upload status of a specified batch.

- US618160: Validation feature added to the Ingestion Worker.

  - The `CAF_VALIDATION_BATCH_WORKER_VALIDATION_FILE` environment variable can be used to provide a file that contains validation rules.
  - Unexpected fields are ignored and result in a failure being added to the document.

#### Bug Fixes
- US656030: Updated to latest version of the Worker Document Framework.

  - This version includes a fix for a concurrency issue in the DocumentValidator where ClassCastException was being thrown when creating a com.worldturner.medeia.schema.validation.SchemaValidator.

#### Known Issues
- None
