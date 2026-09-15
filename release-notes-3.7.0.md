!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- OpenTelemetry support is added by using the OTel configured `oraclelinux-jre25-otel` base image and conditionally enabling
  Java auto-instrumentation at startup when `OTEL_JAVAAGENT_ENABLED=true`.

#### Bug Fixes
- D1217166: Fixed insecure TLS 1.2 cipher suites exposed by the Bouncy Castle providers.

#### Known Issues
