# Document Service Backup Restore Tool

## Overview

Document service backup and restore tool, simply `dbr`, allows to backup data from Document service 
and restore data to Document service.

## Tests and reports

- `sbt test` - run tests
- `sbt scalastyle` - runs scalastyle
- `sbt dependencyUpdatesReport` - creates dependency report

## Build distribution

To build a distribution zip use command:

```
sbt dist
```

The Distribution zip will be created in `artifact` folder. 

## Usage

Check USER_GUIDE.md for details