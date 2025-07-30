# Tracing Utils

Utilities for distributed tracing and request correlation, particularly for managing X-Request-ID headers.

## Overview

The tracing utilities package provides classes for managing request correlation IDs and distributed tracing context, making it easier to track requests across multiple services.

## Key Classes

### MdcXRequestIdHolder
Manages X-Request-ID values using SLF4J's MDC (Mapped Diagnostic Context).

```java
@Autowired
private MdcXRequestIdHolder requestIdHolder;

// Set request ID in MDC
requestIdHolder.set("req-12345");

// Get current request ID
String requestId = requestIdHolder.get();

// Remove request ID from MDC
requestIdHolder.remove();
```

### IWithXRequestId (Deprecated)
Interface for objects that can carry X-Request-ID values. This interface is deprecated.

```java
@Deprecated
public class MyRequest implements IWithXRequestId {
    private String xRequestId;
    
    @Override
    public void setXRequestId(String xRequestId) {
        this.xRequestId = xRequestId;
    }
    
    @Override
    public String getXRequestId() {
        return xRequestId;
    }
}
```

## Usage Examples

### Basic Request ID Management
```java
@Component
public class RequestIdManager {
    
    private final MdcXRequestIdHolder requestIdHolder;
    
    public RequestIdManager(MdcXRequestIdHolder requestIdHolder) {
        this.requestIdHolder = requestIdHolder;
    }
    
    public void setRequestId(String requestId) {
        requestIdHolder.set(requestId);
    }
    
    public String getCurrentRequestId() {
        return requestIdHolder.get();
    }
    
    public void clearRequestId() {
        requestIdHolder.remove();
    }
}
```

### Web Filter for Request Correlation
```java
@Component
public class RequestIdFilter implements Filter {
    
    private static final String X_REQUEST_ID_HEADER = "X-Request-ID";
    private final MdcXRequestIdHolder requestIdHolder;
    
    public RequestIdFilter(MdcXRequestIdHolder requestIdHolder) {
        this.requestIdHolder = requestIdHolder;
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get or generate request ID
        String requestId = httpRequest.getHeader(X_REQUEST_ID_HEADER);
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        
        try {
            // Set request ID in MDC
            requestIdHolder.set(requestId);
            
            // Add to response headers
            httpResponse.setHeader(X_REQUEST_ID_HEADER, requestId);
            
            // Continue with the request
            chain.doFilter(request, response);
            
        } finally {
            // Clean up MDC
            requestIdHolder.remove();
        }
    }
}
```

### Service-to-Service Communication
```java
@Service
public class ExternalServiceClient {
    
    private final RestTemplate restTemplate;
    private final MdcXRequestIdHolder requestIdHolder;
    
    public ExternalServiceClient(RestTemplate restTemplate, 
                               MdcXRequestIdHolder requestIdHolder) {
        this.restTemplate = restTemplate;
        this.requestIdHolder = requestIdHolder;
    }
    
    public ApiResponse callExternalService(ApiRequest request) {
        // Create headers with current request ID
        HttpHeaders headers = new HttpHeaders();
        String currentRequestId = requestIdHolder.get();
        if (currentRequestId != null) {
            headers.set("X-Request-ID", currentRequestId);
        }
        
        HttpEntity<ApiRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(
            "/external/api/endpoint", 
            entity, 
            ApiResponse.class
        );
    }
}
```

### Async Processing with Request Context
```java
@Service
public class AsyncProcessor {
    
    private final MdcXRequestIdHolder requestIdHolder;
    private final ExecutorService executorService;
    
    public AsyncProcessor(MdcXRequestIdHolder requestIdHolder) {
        this.requestIdHolder = requestIdHolder;
        this.executorService = Executors.newFixedThreadPool(5);
    }
    
    public CompletableFuture<ProcessingResult> processAsync(ProcessingRequest request) {
        // Capture current request ID
        String currentRequestId = requestIdHolder.get();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Set request ID in the async thread
                requestIdHolder.set(currentRequestId);
                
                // Process the request
                return performProcessing(request);
                
            } finally {
                // Clean up MDC in async thread
                requestIdHolder.remove();
            }
        }, executorService);
    }
    
    private ProcessingResult performProcessing(ProcessingRequest request) {
        // Processing logic - logs will include request ID
        log.info("Processing request: {}", request.getId());
        return new ProcessingResult();
    }
}
```

### Custom Request ID Generation
```java
@Component
public class RequestIdGenerator {
    
    private final MdcXRequestIdHolder requestIdHolder;
    
    public RequestIdGenerator(MdcXRequestIdHolder requestIdHolder) {
        this.requestIdHolder = requestIdHolder;
    }
    
    public String generateAndSetRequestId() {
        String requestId = generateCustomRequestId();
        requestIdHolder.set(requestId);
        return requestId;
    }
    
    private String generateCustomRequestId() {
        // Custom format: timestamp + random
        long timestamp = System.currentTimeMillis();
        String random = UUID.randomUUID().toString().substring(0, 8);
        return String.format("req-%d-%s", timestamp, random);
    }
}
```

### Logging Configuration
To make the most of request ID tracing, configure your logging pattern to include the MDC value:

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [%X{X-Request-ID:-NO-REQUEST-ID}] %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### Integration with Spring Boot
```java
@Configuration
public class TracingConfiguration {
    
    @Bean
    public MdcXRequestIdHolder mdcXRequestIdHolder() {
        return new MdcXRequestIdHolder("X-Request-ID");
    }
    
    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter(
            MdcXRequestIdHolder requestIdHolder) {
        
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestIdFilter(requestIdHolder));
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // High priority
        return registration;
    }
}
```

## Best Practices

1. **Use Consistent Header Names**: Stick to standard header names like "X-Request-ID" or "X-Correlation-ID".

2. **Propagate Across Services**: Always forward request IDs when making service-to-service calls.

3. **Clean Up MDC**: Always remove request IDs from MDC when done, especially in async contexts.

4. **Generate IDs Early**: Generate request IDs as early as possible in the request lifecycle.

5. **Include in Logs**: Configure logging to include request IDs for better traceability.

6. **Handle Missing IDs**: Gracefully handle cases where request IDs are missing.

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.tracing`
