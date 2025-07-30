# Validation Utils

Utilities for data validation, particularly for ensuring configuration values are properly resolved.

## Overview

The validation utilities package provides custom validation annotations and validators to ensure that configuration values and other data are properly resolved and don't contain placeholder expressions.

## Key Classes

### @ResolvedValue
A validation annotation that ensures string values don't contain unresolved placeholder expressions (like `${...}`).

```java
public class ConfigurationData {
    @ResolvedValue
    private String databaseUrl;
    
    @ResolvedValue
    private String apiKey;
    
    @ResolvedValue
    private Map<String, String> properties;
}
```

### @LegacyResolvedValue
A legacy version of the resolved value validator for backward compatibility.

```java
public class LegacyConfig {
    @LegacyResolvedValue
    private String configValue;
}
```

### ResolvedValueValidator
The validator implementation that checks for unresolved placeholder expressions.

### LegacyResolvedValueValidator
The legacy validator implementation.

## Usage Examples

### Basic Configuration Validation
```java
@Component
@Validated
public class DatabaseConfiguration {
    
    @ResolvedValue
    @NotNull
    private String jdbcUrl;
    
    @ResolvedValue
    @NotNull
    private String username;
    
    @ResolvedValue
    private String password;
    
    // Getters and setters
    public String getJdbcUrl() { return jdbcUrl; }
    public void setJdbcUrl(String jdbcUrl) { this.jdbcUrl = jdbcUrl; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

### Map and Collection Validation
```java
@Component
public class ApplicationProperties {
    
    @ResolvedValue
    private Map<String, String> environmentVariables;
    
    @ResolvedValue
    private List<String> configPaths;
    
    @ResolvedValue
    private Set<String> enabledFeatures;
    
    // The validator will check all string values in collections and maps
}
```

### REST Controller Validation
```java
@RestController
@Validated
public class ConfigurationController {
    
    @PostMapping("/config")
    public ResponseEntity<String> updateConfiguration(
            @RequestBody @Valid ConfigurationRequest request) {
        
        // Configuration will be validated before this method is called
        configurationService.updateConfiguration(request);
        return ResponseEntity.ok("Configuration updated");
    }
}

public class ConfigurationRequest {
    @ResolvedValue
    @NotBlank
    private String serviceName;
    
    @ResolvedValue
    private Map<String, String> settings;
    
    // Getters and setters
}
```

### Service Layer Validation
```java
@Service
@Validated
public class ConfigurationService {
    
    public void processConfiguration(@Valid @ResolvedValue String configValue) {
        // Method parameter validation
        processConfigurationInternal(configValue);
    }
    
    public void bulkUpdateSettings(@Valid List<@ResolvedValue String> settings) {
        // Validates each string in the list
        settings.forEach(this::applySetting);
    }
}
```

### Custom Validation Groups
```java
public interface BasicValidation {}
public interface AdvancedValidation extends BasicValidation {}

public class ConfigurationData {
    @ResolvedValue(groups = BasicValidation.class)
    private String basicConfig;
    
    @ResolvedValue(groups = AdvancedValidation.class)
    private String advancedConfig;
}

// Validate only basic configuration
validator.validate(configData, BasicValidation.class);

// Validate all configuration
validator.validate(configData, AdvancedValidation.class);
```

## What Gets Validated

The `@ResolvedValue` annotation validates:

1. **String values**: Checks that they don't contain `${...}` placeholder expressions
2. **Map entries**: Validates both keys and values if they are strings
3. **Collection elements**: Validates each element if it's a string
4. **Null values**: Null values are considered valid (use `@NotNull` for null checks)

## Common Use Cases

### Configuration Properties
```java
@ConfigurationProperties(prefix = "app")
@Component
@Validated
public class AppProperties {
    
    @ResolvedValue
    private String databaseUrl;
    
    @ResolvedValue
    private String redisHost;
    
    @ResolvedValue
    private Map<String, String> externalServices;
}
```

### Environment-Specific Settings
```java
@Profile("production")
@Configuration
@Validated
public class ProductionConfig {
    
    @Value("${app.secret.key}")
    @ResolvedValue
    private String secretKey;
    
    @Value("${app.external.api.url}")
    @ResolvedValue
    private String externalApiUrl;
}
```

### API Request Validation
```java
public class ApiConfiguration {
    @ResolvedValue
    @Pattern(regexp = "https?://.*")
    private String webhookUrl;
    
    @ResolvedValue
    @Size(min = 10, max = 100)
    private String apiToken;
}
```

## Error Handling

When validation fails, a `ConstraintViolationException` is thrown:

```java
try {
    validator.validate(configObject);
} catch (ConstraintViolationException e) {
    e.getConstraintViolations().forEach(violation -> {
        log.error("Validation error: {} - {}", 
            violation.getPropertyPath(), 
            violation.getMessage());
    });
}
```

## Best Practices

1. **Combine with Other Validations**: Use `@ResolvedValue` alongside other validation annotations like `@NotNull`, `@NotBlank`, etc.

2. **Validate Early**: Apply validation at the configuration loading stage to catch issues early.

3. **Use in Configuration Classes**: Particularly useful for `@ConfigurationProperties` classes.

4. **Handle Validation Errors**: Provide meaningful error messages when validation fails.

5. **Test Your Validations**: Write tests to ensure your validation rules work as expected.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.validation`
