# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.10.0] - 2023-05-08

### Added

* Support for Spring Boot 3.0

### Removed

* Support for Spring Boot 2.5
* `@PostConstruct` annotations.

## [1.9.0] - 2023-02-14

### Added

* `ParentAwareConnectionProxy`, `ParentAwareDataSourceProxy` and `DataSourceProxy` interfaces.

* `setTargetConnection` method to `ConnectionProxy` interface.

* Spring Boot matrix tests

### Removed

* assertj-core, it makes test compilation slow.

### Changed

* Upgraded some libraries

## [1.8.1] - 2022-11-02

### Fixed

* `ThreadNamingExecutorServiceWrapper` is now setting thread name for `execute` method calls as well.

## [1.8.0] - 2022-09-12

### Changed

* If the Callable supplied to TransactionHelper#call throws an exception, there is an attempt to rollback. Thus far, if the rollback also threw an
  exception, it got thrown and the exception from Callable was lost. Starting in this version, any exception from the rollback is caught and logged,
  and the exception from Callable gets thrown.

## [1.7.1] - 2021-12-08

### Changed

* Build and Publish using GitHub Actions

## [1.7.0] - 2021-11-04

### Changed

* Added TestClock#plus(String) method.

## [1.6.0] - 2021-06-11

### Changed

* Added ConnectionProxy interface

## [1.5.0] - 2021-05-27

### Changed

* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.

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
