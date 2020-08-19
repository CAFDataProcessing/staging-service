
#### Version Number
${version-number}

#### New Features for staging-service
- **SCMOD-9780**: Updated images to use Java 11
- **SCMOD:10129**: Introduced configurable healthcheck timeout failure for NFS services.

#### New Features for worker-batch-ingestion
- **SCMOD-9780**: Updated images to use Java 11
- **SCMOD-10198**: Ingestion Worker Batch updated to [3.3.0](https://github.com/JobService/worker-batch/releases/tag/v3.3.0) which inhertied the features from worker framework [3.4.0](https://github.com/WorkerFramework/worker-framework/releases/tag/v3.4.0)
    * **SCMOD-8463**: Confirm health checks before starting
    * **SCMOD-9102**: New filesystem health check
    * **SCMOD-4887**: Poison message recording

#### Known Issues
- None

#### Bug Fixes
- **SCMOD-9626**: Security hardening by upgarding the jackson version.
