#### Version Number
${version-number}

#### New Features
- 397426: Format of logging has been changed to include the UTC Date alongside UTC Time
- 359648: Added support for a new leaner message format  
  The Worker Framework previously defined the task-specific part of its message (which is the vast majority of the message) as a byte array. As messages are normally passed in JSON, this was serialized as a base64 encoded string, which added~33% to the size of the task data. Given that the task-specific part of the message is itself normally JSON, the Worker Framework has now been updated to embed this directly and avoid the overhead. This significantly reduces the message size.

#### Known Issues
- None
