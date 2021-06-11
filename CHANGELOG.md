# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.0] - 2021-05-27
### Changed
* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.

## [1.6.0] - 2021-06-11
### Changed
* Added ConnectionProxy interface

## [1.5.0] - 2021-06-06
### Changed
* JDK 11+ is now requirement

## [1.4.1] - 2021-03-14
### Changed
* Allowing clearing only specific metrics from meter cache.
* Minor optimization in transactions helper.

## [1.4.0] - 2021-03-01
### Added
* MeterCache to avoid Micrometer's MeterRegistry's inefficiencies.

## [1.3.2] - 2020-09-18
### Changed
* Our UUIDs have version 4 identifier now.

## [1.3.1] - 2020-09-17
### Added
* `generateSecureUuid` was added to `UuidUtils`.

## [1.3.0] - 2020-09-14
### Added
* UuidUtils was added. It is expected to be used to generate all UUIDs used in Transferwise.
