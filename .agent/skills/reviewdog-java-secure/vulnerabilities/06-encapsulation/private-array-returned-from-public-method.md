# Private Array Returned from Public Method

## Metadata
- **CWE ID**: CWE-495
- **Severity**: Medium to High
- **OWASP Category**: A04:2021 - Insecure Design
- **Detection Difficulty**: Easy

## Description

Returning a reference to a private mutable array from a public method breaks encapsulation by allowing external code to modify the internal state of an object. When a method returns a direct reference to a private array, callers can modify the array's contents, bypassing any validation or business logic that the class might enforce. This violates the principle of information hiding and can lead to inconsistent object states, security bypasses, and unexpected behavior.

The vulnerability occurs because:
- Arrays in Java are mutable objects passed by reference
- Returning a private array reference exposes internal state
- Callers can modify the array contents directly
- This bypasses setter methods and validation logic
- The object loses control over its internal data

This is particularly dangerous for:
- Security-sensitive data (roles, permissions, keys)
- Business-critical state that must remain consistent
- Collections that have invariants to maintain

## Vulnerable Code Examples

### Vulnerable Java (Direct Array Return)
```java
public class UserAccount {
    private String username;
    private String[] userRoles;  // Security-sensitive data
    private Color[] preferences; // Mutable objects

    public UserAccount(String username) {
        this.username = username;
        this.userRoles = new String[]{"USER"};
        this.preferences = new Color[]{Color.BLUE, Color.GREEN};
    }

    // VULNERABLE: Returns direct reference to private array
    public String[] getUserRoles() {
        return userRoles;  // Caller can modify this array
    }

    // VULNERABLE: Returns direct reference to array of mutable objects
    public Color[] getUserPreferences(Color[] userColors) {
        return preferences;  // Caller can modify Color objects
    }
}

// Exploitation example
public class Attacker {
    public static void main(String[] args) {
        UserAccount user = new UserAccount("john");

        // Get reference to private array
        String[] roles = user.getUserRoles();

        // EXPLOIT: Modify internal state without validation
        roles[0] = "ADMIN";  // Privilege escalation!

        // The user object now has ADMIN role without proper authorization
        System.out.println("User roles: " + Arrays.toString(user.getUserRoles()));
        // Output: User roles: [ADMIN]

        // Get reference to mutable objects
        Color[] colors = user.getUserPreferences(null);

        // EXPLOIT: Modify mutable objects
        colors[0] = Color.RED;  // Changes internal state
    }
}
```

### Vulnerable Java (Security Bypass)
```java
public class AccessControl {
    private String[] allowedIPs;
    private String[] adminUsers;

    public AccessControl() {
        this.allowedIPs = new String[]{"192.168.1.100", "192.168.1.101"};
        this.adminUsers = new String[]{"admin", "superuser"};
    }

    // VULNERABLE: Exposes internal security configuration
    public String[] getAllowedIPs() {
        return allowedIPs;
    }

    public String[] getAdminUsers() {
        return adminUsers;
    }

    public boolean isAllowed(String ip) {
        for (String allowedIP : allowedIPs) {
            if (allowedIP.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAdmin(String username) {
        for (String admin : adminUsers) {
            if (admin.equals(username)) {
                return true;
            }
        }
        return false;
    }
}

// Exploitation: Bypass access control
public class SecurityBypass {
    public static void main(String[] args) {
        AccessControl ac = new AccessControl();

        // Attacker IP initially blocked
        System.out.println("Attacker allowed: " + ac.isAllowed("10.0.0.1"));  // false

        // EXPLOIT: Modify allowed IPs
        String[] ips = ac.getAllowedIPs();
        ips[0] = "10.0.0.1";  // Add attacker IP

        // Now attacker is allowed
        System.out.println("Attacker allowed: " + ac.isAllowed("10.0.0.1"));  // true

        // EXPLOIT: Grant admin privileges
        String[] admins = ac.getAdminUsers();
        admins[0] = "attacker";

        System.out.println("Attacker is admin: " + ac.isAdmin("attacker"));  // true
    }
}
```

## Secure Code Examples

### Secure Java (Array Copy - Defensive Copy)
```java
public class UserAccount {
    private String username;
    private String[] userRoles;
    private Color[] preferences;

    public UserAccount(String username) {
        this.username = username;
        this.userRoles = new String[]{"USER"};
        this.preferences = new Color[]{Color.BLUE, Color.GREEN};
    }

    // SECURE: Returns a copy of the array
    public String[] getUserRoles() {
        // For String arrays, simple array copy is sufficient (Strings are immutable)
        return Arrays.copyOf(userRoles, userRoles.length);

        // Alternative: manual copy
        // String[] copy = new String[userRoles.length];
        // System.arraycopy(userRoles, 0, copy, 0, userRoles.length);
        // return copy;
    }

    // SECURE: Returns deep copy for arrays of mutable objects
    public Color[] getUserPreferences() {
        Color[] copy = new Color[preferences.length];
        for (int i = 0; i < preferences.length; i++) {
            // Clone each Color object
            copy[i] = new Color(preferences[i].getRGB());
        }
        return copy;
    }

    // Alternative secure pattern: Return unmodifiable list
    public List<String> getUserRolesList() {
        return Collections.unmodifiableList(Arrays.asList(userRoles));
    }
}

// Safe usage
public class SafeClient {
    public static void main(String[] args) {
        UserAccount user = new UserAccount("john");

        // Get copy of array
        String[] roles = user.getUserRoles();

        // Modifications only affect the copy, not internal state
        roles[0] = "ADMIN";

        // Internal state unchanged
        System.out.println("User roles: " + Arrays.toString(user.getUserRoles()));
        // Output: User roles: [USER]
    }
}
```

### Secure Java (Immutable Return for String Arrays)
```java
public class Configuration {
    private String[] allowedDomains;
    private String[] trustedCertificates;

    public Configuration() {
        this.allowedDomains = new String[]{"example.com", "trusted.org"};
        this.trustedCertificates = new String[]{"cert1", "cert2"};
    }

    // SECURE: Strings are immutable, so array copy is sufficient
    public String[] getAllowedDomains() {
        return Arrays.copyOf(allowedDomains, allowedDomains.length);
    }

    // SECURE: Alternative using Collections
    public List<String> getAllowedDomainsList() {
        return Collections.unmodifiableList(Arrays.asList(allowedDomains));
    }

    // SECURE: Stream API for modern Java
    public Stream<String> getAllowedDomainsStream() {
        return Arrays.stream(allowedDomains);
    }
}
```

### Secure C# (List Clone with ICloneable)
```csharp
using System;
using System.Collections.Generic;

public class UserPreferences {
    private List<ICloneable> colors;

    public UserPreferences() {
        colors = new List<ICloneable>();
        colors.Add(new ColorPreference("Blue"));
        colors.Add(new ColorPreference("Green"));
    }

    // SECURE: Returns deep copy of list
    public List<ICloneable> GetColors() {
        List<ICloneable> newList = new List<ICloneable>(colors.Count);

        colors.ForEach((item) => {
            newList.Add((ICloneable)item.Clone());
        });

        return newList;
    }

    // Alternative: Return read-only collection
    public IReadOnlyList<ICloneable> GetColorsReadOnly() {
        return colors.AsReadOnly();
    }
}

public class ColorPreference : ICloneable {
    private string colorName;

    public ColorPreference(string name) {
        colorName = name;
    }

    public object Clone() {
        return new ColorPreference(colorName);
    }

    public string GetColorName() {
        return colorName;
    }
}
```

### Secure Java (Complete Example with Validation)
```java
import java.util.*;

public class AccessControl {
    private final List<String> allowedIPs;
    private final List<String> adminUsers;

    public AccessControl() {
        // Use modifiable lists internally
        this.allowedIPs = new ArrayList<>(Arrays.asList("192.168.1.100", "192.168.1.101"));
        this.adminUsers = new ArrayList<>(Arrays.asList("admin", "superuser"));
    }

    // SECURE: Return unmodifiable view
    public List<String> getAllowedIPs() {
        return Collections.unmodifiableList(new ArrayList<>(allowedIPs));
    }

    public List<String> getAdminUsers() {
        return Collections.unmodifiableList(new ArrayList<>(adminUsers));
    }

    // SECURE: Controlled mutation through validated methods
    public boolean addAllowedIP(String ip) {
        if (ip == null || !isValidIP(ip)) {
            throw new IllegalArgumentException("Invalid IP address");
        }

        if (!allowedIPs.contains(ip)) {
            allowedIPs.add(ip);
            return true;
        }
        return false;
    }

    public boolean addAdminUser(String username, String authorizedBy) {
        // Validation and authorization logic
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid username");
        }

        if (!isAdmin(authorizedBy)) {
            throw new SecurityException("Not authorized to add admin users");
        }

        if (!adminUsers.contains(username)) {
            adminUsers.add(username);
            logSecurityEvent("Admin added: " + username + " by " + authorizedBy);
            return true;
        }
        return false;
    }

    public boolean isAllowed(String ip) {
        return allowedIPs.contains(ip);
    }

    public boolean isAdmin(String username) {
        return adminUsers.contains(username);
    }

    private boolean isValidIP(String ip) {
        // IP validation logic
        return ip.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    }

    private void logSecurityEvent(String event) {
        // Security logging
        System.out.println("[SECURITY] " + event);
    }
}
```

## Detection Methods

### Static Analysis
```bash
# Find methods returning arrays
grep -r "public.*\[\].*get" --include="*.java" .

# Find private array fields
grep -r "private.*\[\]" --include="*.java" .

# Look for direct array returns (simple pattern)
grep -r "return.*\[\];" --include="*.java" .
```

### SpotBugs/FindBugs Detector
```java
// Custom SpotBugs detector for CWE-495
public class ReturnPrivateArrayDetector implements Detector {

    public void visitMethod(Method method) {
        if (!method.isPublic()) return;

        // Check if return type is array
        if (method.getReturnType().isArray()) {
            // Check if returned field is private
            for (Field field : method.getReferencedFields()) {
                if (field.isPrivate() && field.getType().isArray()) {
                    reportBug("EI_EXPOSE_REP: Returning reference to mutable private array");
                }
            }
        }
    }
}
```

### Code Review Checklist
- [ ] Public methods don't return references to private arrays
- [ ] Arrays of immutable objects use defensive copying
- [ ] Arrays of mutable objects use deep copying
- [ ] Consider using Collections.unmodifiableList() instead
- [ ] Validate that cloning is implemented correctly for mutable objects
- [ ] Check that returned arrays can't be used to bypass security checks

### IntelliJ IDEA Inspection
```xml
<!-- .idea/inspectionProfiles/Project_Default.xml -->
<inspection_tool class="ReturnOfCollectionOrArrayField"
                  enabled="true"
                  level="WARNING"
                  enabled_by_default="true">
    <option name="ignorePrivateMethods" value="false" />
</inspection_tool>
```

## References

### CWE
- [CWE-495: Private Data Structure Returned From Public Method](https://cwe.mitre.org/data/definitions/495.html)
- [CWE-374: Passing Mutable Objects to an Untrusted Method](https://cwe.mitre.org/data/definitions/374.html)

### CERT Coding Standards
- [OBJ05-J: Do not return references to private mutable class members](https://wiki.sei.cmu.edu/confluence/display/java/OBJ05-J)
- [OBJ06-J: Defensively copy mutable inputs and mutable internal components](https://wiki.sei.cmu.edu/confluence/display/java/OBJ06-J)

### OWASP
- [OWASP Top 10 2021 A04:2021 - Insecure Design](https://owasp.org/Top10/A04_2021-Insecure_Design/)

### Additional Resources
- Effective Java (3rd Edition) by Joshua Bloch - Item 50: Make defensive copies when needed
- [Oracle Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)

## Security Checklist

### For Developers
- [ ] Never return direct references to private arrays
- [ ] Use Arrays.copyOf() for arrays of immutable objects
- [ ] Implement deep copying for arrays of mutable objects
- [ ] Consider returning Collections.unmodifiableList() instead
- [ ] Provide controlled mutation methods with validation
- [ ] Document whether returned arrays/collections are safe to modify
- [ ] Use immutable data structures when possible (Guava ImmutableList, etc.)

### For Code Reviewers
- [ ] Verify all public methods returning arrays use defensive copying
- [ ] Check that deep copying is used for mutable object arrays
- [ ] Confirm proper implementation of clone() methods
- [ ] Validate that unmodifiable collections are properly used
- [ ] Review mutation methods for proper validation
- [ ] Check for consistent encapsulation across the class

### For Security Auditors
- [ ] Identify all public getter methods returning arrays
- [ ] Verify defensive copying for security-sensitive data
- [ ] Test that modifications to returned arrays don't affect internal state
- [ ] Check for privilege escalation possibilities
- [ ] Validate that access control can't be bypassed through array modification
- [ ] Review for consistent application of defensive copying pattern
