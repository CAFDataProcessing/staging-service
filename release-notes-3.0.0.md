#### Version Number
${version-number}

#### New Features
- None

#### Breaking Changes
- **D854021:** Worker Framework V4 Format message support dropped  
  The Ingestion Worker has been updated to use a new version of the worker framework which no longer supports the V4 format message.

- **US361030:** Auto-generated REST Client library dropped  
  The existing client library was using OkHttp which is EOL.  Changing the underlying library, even to OkHttp3, would be a breaking change.  Consuming services should move to using the `staging-service-contract` module to auto-generate their own client library, so that they can control the underlying library choices.

#### Known Issues
- None
