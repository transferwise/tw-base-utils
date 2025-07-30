# Bitmap Utils

Utilities for working with bitmap operations and bit manipulation.

## Overview

The bitmap utilities package provides classes for efficient bitmap operations, bit manipulation, and working with bit sets for various use cases like feature flags, permissions, and compact data storage.

## Key Features

- Efficient bit manipulation operations
- Bitmap data structures for compact storage
- Utilities for working with bit sets
- Performance-optimized bitmap operations

## Usage Examples

### Basic Bitmap Operations
```java
// Example usage (actual API may vary based on implementation)
BitmapUtils bitmapUtils = new BitmapUtils();

// Set bits
bitmapUtils.setBit(bitmap, 5);
bitmapUtils.setBit(bitmap, 10);

// Check bits
boolean isSet = bitmapUtils.isBitSet(bitmap, 5); // true
boolean isNotSet = bitmapUtils.isBitSet(bitmap, 3); // false

// Clear bits
bitmapUtils.clearBit(bitmap, 5);
```

### Feature Flags Implementation
```java
@Component
public class FeatureFlagManager {
    
    private final BitmapUtils bitmapUtils;
    private final Map<String, Integer> featurePositions;
    
    public FeatureFlagManager(BitmapUtils bitmapUtils) {
        this.bitmapUtils = bitmapUtils;
        this.featurePositions = initializeFeaturePositions();
    }
    
    public boolean isFeatureEnabled(long userFlags, String featureName) {
        Integer position = featurePositions.get(featureName);
        if (position == null) {
            return false;
        }
        return bitmapUtils.isBitSet(userFlags, position);
    }
    
    public long enableFeature(long userFlags, String featureName) {
        Integer position = featurePositions.get(featureName);
        if (position != null) {
            return bitmapUtils.setBit(userFlags, position);
        }
        return userFlags;
    }
    
    private Map<String, Integer> initializeFeaturePositions() {
        Map<String, Integer> positions = new HashMap<>();
        positions.put("PREMIUM_FEATURES", 0);
        positions.put("BETA_ACCESS", 1);
        positions.put("ADVANCED_ANALYTICS", 2);
        return positions;
    }
}
```

### Permission System
```java
@Service
public class PermissionService {
    
    private final BitmapUtils bitmapUtils;
    
    public enum Permission {
        READ(0), WRITE(1), DELETE(2), ADMIN(3);
        
        private final int bitPosition;
        
        Permission(int bitPosition) {
            this.bitPosition = bitPosition;
        }
        
        public int getBitPosition() {
            return bitPosition;
        }
    }
    
    public boolean hasPermission(long userPermissions, Permission permission) {
        return bitmapUtils.isBitSet(userPermissions, permission.getBitPosition());
    }
    
    public long grantPermission(long userPermissions, Permission permission) {
        return bitmapUtils.setBit(userPermissions, permission.getBitPosition());
    }
    
    public long revokePermission(long userPermissions, Permission permission) {
        return bitmapUtils.clearBit(userPermissions, permission.getBitPosition());
    }
    
    public boolean hasAllPermissions(long userPermissions, Permission... permissions) {
        for (Permission permission : permissions) {
            if (!hasPermission(userPermissions, permission)) {
                return false;
            }
        }
        return true;
    }
}
```

### Compact Data Storage
```java
@Entity
public class UserSettings {
    
    @Id
    private Long id;
    
    // Store multiple boolean settings in a single long field
    @Column(name = "settings_bitmap")
    private long settingsBitmap;
    
    // Settings positions
    private static final int EMAIL_NOTIFICATIONS = 0;
    private static final int SMS_NOTIFICATIONS = 1;
    private static final int PUSH_NOTIFICATIONS = 2;
    private static final int MARKETING_EMAILS = 3;
    private static final int DARK_MODE = 4;
    
    @Autowired
    @Transient
    private BitmapUtils bitmapUtils;
    
    public boolean isEmailNotificationsEnabled() {
        return bitmapUtils.isBitSet(settingsBitmap, EMAIL_NOTIFICATIONS);
    }
    
    public void setEmailNotificationsEnabled(boolean enabled) {
        if (enabled) {
            settingsBitmap = bitmapUtils.setBit(settingsBitmap, EMAIL_NOTIFICATIONS);
        } else {
            settingsBitmap = bitmapUtils.clearBit(settingsBitmap, EMAIL_NOTIFICATIONS);
        }
    }
    
    public boolean isDarkModeEnabled() {
        return bitmapUtils.isBitSet(settingsBitmap, DARK_MODE);
    }
    
    public void setDarkModeEnabled(boolean enabled) {
        if (enabled) {
            settingsBitmap = bitmapUtils.setBit(settingsBitmap, DARK_MODE);
        } else {
            settingsBitmap = bitmapUtils.clearBit(settingsBitmap, DARK_MODE);
        }
    }
}
```

## Common Use Cases

1. **Feature Flags**: Store multiple feature flags in a compact format
2. **Permissions**: Manage user permissions efficiently
3. **Settings**: Store boolean settings in a single field
4. **State Management**: Track multiple boolean states compactly
5. **Filtering**: Use bitmaps for efficient filtering operations

## Best Practices

1. **Document Bit Positions**: Always document what each bit position represents
2. **Use Enums**: Define bit positions using enums for better maintainability
3. **Validation**: Validate bit positions to avoid out-of-bounds errors
4. **Migration Strategy**: Plan for adding new bits without breaking existing data
5. **Performance**: Use bitmap operations for high-performance scenarios

## Maven Dependency

```xml
<dependency>
    <groupId>com.transferwise.common</groupId>
    <artifactId>tw-base-utils</artifactId>
    <version>${tw-base-utils.version}</version>
</dependency>
```

## Package

`com.transferwise.common.baseutils.bitmap`

## Note

The specific API methods may vary based on the actual implementation. Please refer to the source code for the exact method signatures and available operations.
