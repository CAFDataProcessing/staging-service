#### Version Number
${version-number}

#### Breaking Changes
- US593021: Nashorn script engine is no longer supported.

#### New Features
- US593021: Updated to run on Java 17.

### Bug Fixes
- US607098: Fixed a potential issue in the 'Stale batches cleanup' scheduler, due to a change in the behavior
   of Instant.now() method between JDK8 and later versions causing a change in how the directories are named.

#### Patch Fixes Included
- US572082: Gson version upgraded to [2.9.1](https://github.com/google/gson/releases/tag/gson-parent-2.9.1)
- US572083: Snakeyaml version upgraded to [1.32](https://bitbucket.org/snakeyaml/snakeyaml/wiki/Changes)

#### Known Issues
- None
