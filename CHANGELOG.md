# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.14.0] - 2025-07-10

### Changed
- Added support for generating [UUIDv7](https://uuid7.com/)s (`generateTimePrefixedUuid`).
- Added support for generating UUIDv8s. This implementation (`generateDeterministicTimePrefixedUuid`) is a combination of UUIDv7's time-prefixing and UUIDv3's seeding.
- Deprecated prefix combined UUIDv4s in favour of UUIDv7s or UUIDv8s (as appropriate).

## [1.13.4] - 2025-06-17

### Changed
- Added support for spring boot 3.5
- Updated the version for spring boot 3.4.0 to 3.4.6

## [1.13.3] - 2025-04-03

### Changed
- Added deterministic prefix Combined Uuid which is constructed based on provided timestamp and uuid

## [1.13.2] - 2024-12-05

### Changed
- Added support for spring boot 3.4

## [1.13.1] - 2024-10-15

### Fixed

* Memory visibility issue when taking thread dumps.

## [1.13.0] - 2024-10-11

### Added

* Base91 encoder.
* RoaringBitmap based LargeBitmap with better 64 bits support.
* ThreadUtils to take safe-point free thread dumps. Those thread dumps will be inconsistent, but still suitable in many cases.

### Changed

* Java 17+ is now required.

## [1.12.5] - 2024-07-16

### Changed

* Added support for Spring Boot 3.3.

## [1.12.4] - 2024-02-22

### Changed

* Added support for Spring Boot 3.2.

## [1.12.3] - 2024-01-11

### Fixed

* Eliminating shutdown noise from interruption in schedule tasks executor.

## [1.12.2] - 2024-01-05

### Fixed

* Stopping a scheduled task does not stop other tasks with the same execution time.

## [1.12.1] - 2023-10-29

### Added

* UuidUtils.add method.

## [1.12.1] - 2023-10-29

### Added

* UuidUtils.add method.

## [1.12.0] - 2023-10-03

### Changed

* Improved memory allocations in meter cache by avoiding frequent linkToTargetMethod allocations by lambda executions.

## [1.11.1] - 2023-08-17

### Fixed

* Fixes issue of DefaultJsonConverter being incorrectly set as a @Component.

## [1.11.0] - 2023-08-16

### Added

* Ported `JsonConverter` interface and `DefaultJsonConverter` implementation from old common lib.

## [1.10.3] - 2023-08-08

### Added

* Ported `JavaTimeModuleFactory` from old common lib so that the lib can be removed in the future. It provides consistent millisecond serialisation of
  `Instant` and `ZonedDateTime` objects.

## [1.10.2] - 2023-07-28

### Added

* Support for Spring Boot 3.1

### Bumped

* Build against Spring Boot 3.0.6 --> 3.0.7
* Build against Spring Boot 2.7.11 --> 2.7.13
* Build against Spring Boot 2.6.14 --> 2.6.15

## [1.10.1] - 2023-05-08

### Added

* Two Java Validation annotations used in many of our libraries.

## [1.10.0] - 2023-05-08

### Added

* Support for Spring Boot 3.0

### Removed

* Support for Spring Boot 2.5
* `@PostConstruct` and `@PreDestroy` annotations.

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