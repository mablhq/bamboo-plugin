# Changelog

All notable changes to the Bamboo mabl plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

To be filled out.

## [0.1.13]

### Fixed

* [MABL-8395](https://mabl.atlassian.net/browse/MABL-8395) Fix logging error if there are skipped test runs

## [0.1.12]

### Fixed

* [MABL-8542](https://mabl.atlassian.net/browse/MABL-8542) Fix multiple test runs triggered by DataTables being listed as a single test run

## [0.1.11] - May 3, 2022

### Changed

* Update mabl icon

### Fixed

* [MABL-6912](https://mabl.atlassian.net/browse/MABL-6912) Bump the API request timeout to 30 minutes
* [MABL-5731](https://mabl.atlassian.net/browse/MABL-5731) Skip logging test status if status is null

## [0.1.10] - July 12, 2021

### Added

* [MABL-5052](https://mabl.atlassian.net/browse/MABL-5052) Share the deployment ID in mabl as a variable mabl.deployment.id available to subsequent 
  tasks in the pipeline
  
### Changed

* Update spotbugs-maven-plugin, wiremock-jre8-standalone dependencies
* Suppress spotbugs warning related to injected Bamboo object instances

## [0.1.9] - June 11, 2021

### Changed

* [IST-271](https://mabl.atlassian.net/browse/IST-271) Address Apache Httpclient vulnerability
* Update junit, jacoco-maven-plugin, spotbugs-maven-plugin, wiremock-jre8-standalone dependencies
* [MABL-3120](https://mabl.atlassian.net/browse/MABL-3120) Update JUnit dependency to address [GHSA-269g-pwp5-87pp](https://github.com/advisories/GHSA-269g-pwp5-87pp) advisory

## [0.1.8] - October 13, 2020

### Added

* [MABL-3059](https://mabl.atlassian.net/browse/MABL-3059) Fix an issue that caused multiple test case IDs to be included in the requirements section of the JUnit report

### Fixed

* [MABL-3060](https://mabl.atlassian.net/browse/MABL-3060) Add support for mabl branch cconfiguration option
* Add a missing namespace declaration to the generated JUnit report XML

### Changed

* Remove PowerMockito dependency
* Update dependencies: Wiremock, Spotbugs, JaCoCo
* Add description to fields on the configuration page

## [0.1.7] - September 09, 2020

### Added

* [MABL-2329](https://mabl.atlassian.net/browse/MABL-2329) Add proxy configuration and basic auth support to bamboo-plugin

## [0.1.6] - August 25, 2020

### Added

* [MABL-2468](https://mabl.atlassian.net/browse/MABL-2468) Added support for capturing test case IDs and for writing out JUnit test report and allow Bamboo to capture the file as a build artifact
* Added support for counting and reporting skipped tests

### Fixed

* Fixed issue with reporting test duration

### Changed    

* Retried plans that completed successfully used to cause test failure
* Updated user-agent string to include JVM and Bamboo version
* Updated test output to include URL to deployment event
* Updated components (Powermock, Wiremock, JUnit, Apache HTTP Client)

## [0.1.5] - May 4, 2020

### Fixed

* [MABL-2097](https://mabl.atlassian.net/browse/MABL-2097) Bamboo plugin: bug on Bamboo 6

## [0.1.4] - April 23, 2020

### Fixed

* [MABL-1800](https://mabl.atlassian.net/browse/MABL-1800) Bamboo: plugin usage on remote agent

## [0.1.3] - March 26, 2020

### Changed

* Removed snapshot tag (internal release)

## [0.0.10] - April 2, 2019

### Fixed

* Respect JVM proxy settings

[Unreleased]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.10...head
[0.1.10]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.9...bamboo-plugin-0.1.10
[0.1.9]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.8...bamboo-plugin-0.1.9
[0.1.8]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.7...bamboo-plugin-0.1.8
[0.1.7]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.6...bamboo-plugin-0.1.7
[0.1.6]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.5...bamboo-plugin-0.1.6
[0.1.5]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.4...bamboo-plugin-0.1.5
[0.1.4]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.3...bamboo-plugin-0.1.4
[0.1.3]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.0.10...bamboo-plugin-0.1.3
[0.0.10]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.0.8...bamboo-plugin-0.0.10
