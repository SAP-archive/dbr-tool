# Document Service Backup Restore Tool

The tool delivered to you to improve your experience with the
<a href="https://market.yaas.io/beta/all/Persistence%2520(Beta)/9b174e06-9283-4c47-8d16-6eded2ac840a">
Persistence package</a> components. The Document service backup and restore tool, simply `dbr`, allows to backup data along with the indexes from the
<a href="https://devportal.yaas.io/services/beta/document/latest/">Document service </a>and restore the data to
the Document service.

## Table of contents


* [Installation](#installation)
* [Usage](#usage)
* [Development](#development)
* [License](#license)
* [Defects and Feedback](#defects-and-feedback)
* [Contribution](#contribution)
* [Credits](#credits)



## Installation

To build a distribution zip use command:

```
$ sbt clean test dist
```

The distribution zip is created in `artifact` directory.

## Usage

Check [USER_GUIDE](USER_GUIDE.md) for the details.

## Development

- `sbt test` - run tests
- `sbt scalastyle` - run scalastyle
- `sbt dependencyUpdatesReport` - create dependency report

## Licence

Copyright (c) 2014 [SAP SE](http://www.sap.com) or an SAP affiliate company. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License here: [LICENSE](LICENSE)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


## Defects and Feedback

Use [the Github issue tracker](../../issues) for now.

## Contribution

Read the [CONTRIBUTING](CONTRIBUTING.md) so you know exactly how to contribute to this project.

## Credits

<p align="center">

[![YaaS](https://github.com/YaaS/sample-yaas-repository/blob/master/YaaS.png)](https://yaas.io)

<p align="center">
:heart: from the GitHub team @ YaaS
