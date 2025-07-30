# UuidUtils

Utility class for generating various types of UUIDs with different characteristics and use cases.

## Overview

The `UuidUtils` class provides static methods for generating different types of UUIDs, including secure random UUIDs, time-prefixed UUIDs for sortability, and deterministic UUIDs.

## Key Methods

### `generateSecureUuid()`
Generates a completely random UUID suitable for authentication tokens and security-sensitive applications.

```java
UUID secureToken = UuidUtils.generateSecureUuid();
```

### `generateTimePrefixedUuid()`
Generates a timestamp-prefixed UUID that is time-sortable (similar to UUIDv7). Not suitable for authentication tokens.

```java
UUID timeSortableId = UuidUtils.generateTimePrefixedUuid();
```

### `generateTimePrefixedUuid(long timestamp)`
Generates a timestamp-prefixed UUID with a specific timestamp.

```java
long customTimestamp = System.currentTimeMillis();
UUID timeSortableId = UuidUtils.generateTimePrefixedUuid(customTimestamp);
```

### `generateDeterministicTimePrefixedUuid(byte[] data)`
Generates a deterministic timestamp-prefixed UUID based on provided data. Useful for consistent UUID generation.

```java
byte[] data = "some-unique-data".getBytes();
UUID deterministicId = UuidUtils.generateDeterministicTimePrefixedUuid(data);
```

### `generateDeterministicTimePrefixedUuid(long timestamp, byte[] data)`
Generates a deterministic timestamp-prefixed UUID with a specific timestamp and data.

```java
long timestamp = System.currentTimeMillis();
byte[] data = "unique-identifier".getBytes();
UUID deterministicId = UuidUtils.generateDeterministicTimePrefixedUuid(timestamp, data);
```

## Deprecated Methods

### `generatePrefixCombUuid()` (Deprecated)
Generates a random UUID with 38-bit prefix based on current milliseconds. Use `generateTimePrefixedUuid()` instead.

### `generatePrefixCombUuid(long timestamp, UUID uuid)` (Deprecated)
Generates a UUID with 38-bit prefix based on provided timestamp and UUID. Use `generateDeterministicTimePrefixedUuid()` instead.

## Usage Examples

### Security Tokens
```java
// For authentication tokens, session IDs, etc.
UUID sessionToken = UuidUtils.generateSecureUuid();
UUID apiKey = UuidUtils.generateSecureUuid();
```

### Time-Sortable Identifiers
```java
// For database records that need time-based sorting
UUID orderId = UuidUtils.generateTimePrefixedUuid();
UUID eventId = UuidUtils.generateTimePrefixedUuid();

// These UUIDs will sort chronologically
List<UUID> ids = Arrays.asList(
    UuidUtils.generateTimePrefixedUuid(),
    UuidUtils.generateTimePrefixedUuid(),
    UuidUtils.generateTimePrefixedUuid()
);
// ids are naturally sorted by creation time
```

### Deterministic UUIDs
```java
// For consistent UUID generation across different runs
String userId = "user123";
UUID consistentId = UuidUtils.generateDeterministicTimePrefixedUuid(userId.getBytes());

// Same input will always generate the same UUID (for the same timestamp)
UUID sameId = UuidUtils.generateDeterministicTimePrefixedUuid(userId.getBytes());
// consistentId.equals(sameId) will be true if generated in the same millisecond
```

### Custom Timestamp UUIDs
```java
// For backdating or specific timestamp requirements
long specificTime = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli();
UUID backdatedId = UuidUtils.generateTimePrefixedUuid(specificTime);
```

## Important Notes

- **Security**: Only use `generateSecureUuid()` for security-sensitive applications like authentication tokens
- **Time-sortable UUIDs**: These are not suitable for security tokens but are perfect for database records that need chronological ordering
- **Deterministic UUIDs**: Useful for idempotent operations and consistent UUID generation
- **Future compatibility**: Time-prefixed methods may be deprecated once JDK UUIDv7 implementation is available

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.UuidUtils`
