#### Version Number
${version-number}

#### New Features
- US616155: This worker now supports message prioritization.  

  Messages intended for this worker can be redirected to one or more staging queues instead of the worker's target queue.  This feature can be enabled by setting the `CAF_WORKER_ENABLE_DIVERTED_TASK_CHECKING` environment variable to `false` on **this worker**, and setting the `CAF_WMP_ENABLED` environment variable to `true` on the **component that routes messages to this worker**.    

- US585006: Added a new GET `batchStatus` endpoint for getting the upload status of a specified batch.

#### Bug Fixes
- US656030: Fixed ClassCastExceptions in document validator.

#### Known Issues
- None
