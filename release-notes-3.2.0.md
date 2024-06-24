!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US919158: The `healthcheck` endpoint now supports the `/liveness` and `/readiness` paths to check if the service is alive/ready.
  - The `healthcheck/liveness` endpoint checks: livenessState,diskAccess
  - The `healthcheck/readiness` endpoint checks: livenessState,diskAccess,readinessState,ping

#### Known Issues
