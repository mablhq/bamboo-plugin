# Changelog

All notable changes to the Bamboo mabl plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.6] - August 25, 2020

## Added
* [MABL-2468](https://mabl.atlassian.net/browse/MABL-2468) Added support for capturing test case IDs and for writing out JUnit test report and allow Bamboo to capture the file as a build artifact
* Added support for counting and reporting skipped tests

## Fixed
* Fixed issue with reporting test duration

## Changed    
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

## Fixed
* Respect JVM proxy settings

[Unreleased]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.6...head
[0.1.6]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.5...bamboo-plugin-0.1.6
[0.1.5]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.4...bamboo-plugin-0.1.5
[0.1.4]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.1.3...bamboo-plugin-0.1.4
[0.1.3]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.0.10...bamboo-plugin-0.1.3
[0.0.10]: https://github.com/mablhq/bamboo-plugin/compare/bamboo-plugin-0.0.8...bamboo-plugin-0.0.10
