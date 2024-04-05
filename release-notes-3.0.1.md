#### Version Number
${version-number}

#### New Features
- None

#### Bug Fixes
 - I897075: Low disk space will no longer cause this service to go unhealthy.  
   - A failing healthcheck because of low disk space was causing this service to go unhealthy, which was preventing the deletion of
     batches (which would free up disk space).
   - Disk space is still checked in the `status` endpoint, but is no longer checked in the `healthcheck` endpoint.

#### Known Issues
- None
