# Public Data Assigned to Private Array

## Metadata
- **CWE ID**: CWE-496
- **Severity**: Medium to High
- **OWASP Category**: A04:2021 - Insecure Design
- **Detection Difficulty**: Easy

## Description

Assigning a reference from a public parameter directly to a private array field breaks encapsulation by allowing external code to retain a reference to the object's internal state. When a setter method stores a parameter array reference directly in a private field, the caller can continue to modify the array after the assignment, bypassing validation and creating inconsistent object states. This violates the principle of defensive copying and can lead to security vulnerabilities.

The vulnerability occurs because:
- Arrays are mutable objects passed by reference in Java
- Storing a parameter reference directly allows external modification
- The caller retains the ability to modify "private" data
- This bypasses setter validation and business logic
- The object cannot guarantee invariants about its internal state

This is particularly dangerous when:
- The array contains security-sensitive data (roles, permissions, keys)
- The class maintains invariants that depend on array contents
- The array is used in security decisions
- Multiple threads access the shared reference

## Vulnerable Code Examples

### Vulnerable Java (Direct Reference Assignment)
```java
public class UserAccount {
    private String username;
    private String[] userRoles;  // Security-sensitive data
    private int[] accessLevels;

    public UserAccount(String username) {
        this.username = username;
    }

    // VULNERABLE: Stores reference to caller's array
    public void setUserRoles(String[] userRoles) {
        this.userRoles = userRoles;  // Caller retains reference
    }

    // VULNERABLE: Direct assignment without validation
    public void setAccessLevels(int[] levels) {
        this.accessLevels = levels;  // No defensive copy
    }

    public String[] getUserRoles() {
        return userRoles;
    }

    public boolean hasRole(String role) {
        if (userRoles == null) return false;
        for (String r : userRoles) {
            if (r.equals(role)) return true;
        }
        return false;
    }
}

// Exploitation example
public class Attacker {
    public static void main(String[] args) {
        UserAccount user = new UserAccount("john");

        // Create array with basic permissions
        String[] roles = new String[]{"USER", "READER"};

        // Set roles
        user.setUserRoles(roles);

        System.out.println("Initial roles: " + Arrays.toString(user.getUserRoles()));
        // Output: [USER, READER]

        System.out.println("Is admin? " + user.hasRole("ADMIN"));
        // Output: false

        // EXPLOIT: Modify array after assignment
        roles[0] = "ADMIN";  // Privilege escalation!

        System.out.println("Modified roles: " + Arrays.toString(user.getUserRoles()));
        // Output: [ADMIN, READER]

        System.out.println("Is admin? " + user.hasRole("ADMIN"));
        // Output: true - Security bypass!
    }
}
```

### Vulnerable Java (Security Configuration)
```java
public class SecurityPolicy {
    private UserRole[] allowedRoles;
    private String[] trustedDomains;

    public SecurityPolicy() {
        // Default secure configuration
        this.allowedRoles = new UserRole[0];
        this.trustedDomains = new String[0];
    }

    // VULNERABLE: Direct assignment allows external modification
    public void setAllowedRoles(UserRole[] roles) {
        // Missing validation and defensive copy
        this.allowedRoles = roles;
    }

    public void setTrustedDomains(String[] domains) {
        this.trustedDomains = domains;
    }

    public boolean isRoleAllowed(UserRole role) {
        for (UserRole allowed : allowedRoles) {
            if (allowed.equals(role)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDomainTrusted(String domain) {
        for (String trusted : trustedDomains) {
            if (trusted.equals(domain)) {
                return true;
            }
        }
        return false;
    }
}

class UserRole implements Cloneable {
    private String name;
    private int level;

    public UserRole(String name, int level) {
        this.name = name;
        this.level = level;
    }

    public String getName() { return name; }
    public int getLevel() { return level; }

    // VULNERABLE: Mutable after creation
    public void setLevel(int level) { this.level = level; }

    @Override
    public UserRole clone() {
        return new UserRole(this.name, this.level);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserRole)) return false;
        UserRole other = (UserRole) obj;
        return this.name.equals(other.name) && this.level == other.level;
    }
}

// Exploitation
public class PolicyBypass {
    public static void main(String[] args) {
        SecurityPolicy policy = new SecurityPolicy();

        // Create roles with limited access
        UserRole[] roles = new UserRole[]{
            new UserRole("viewer", 1),
            new UserRole("editor", 2)
        };

        policy.setAllowedRoles(roles);

        UserRole admin = new UserRole("admin", 10);
        System.out.println("Admin allowed? " + policy.isRoleAllowed(admin));  // false

        // EXPLOIT 1: Modify array elements
        roles[0] = new UserRole("admin", 10);
        System.out.println("Admin allowed? " + policy.isRoleAllowed(admin));  // true

        // EXPLOIT 2: Modify mutable objects in array
        roles[1].setLevel(10);  // Escalate privilege
    }
}
```

### Vulnerable C# (Direct Assignment)
```csharp
public class Configuration {
    private String[] serverList;
    private int[] portList;

    // VULNERABLE: Direct assignment of array reference
    public void SetServerList(String[] servers) {
        this.serverList = servers;  // Caller can modify after assignment
    }

    public void SetPortList(int[] ports) {
        this.portList = ports;
    }

    public String[] GetServerList() {
        return serverList;
    }
}

// Exploitation
class Program {
    static void Main() {
        Configuration config = new Configuration();

        String[] servers = new String[] { "server1.com", "server2.com" };
        config.SetServerList(servers);

        Console.WriteLine("Servers: " + String.Join(", ", config.GetServerList()));

        // EXPLOIT: Modify configuration after assignment
        servers[0] = "malicious.com";

        Console.WriteLine("Servers: " + String.Join(", ", config.GetServerList()));
        // Output shows malicious.com - security bypass!
    }
}
```

## Secure Code Examples

### Secure Java (Defensive Copy for Immutable Elements)
```java
public class UserAccount {
    private String username;
    private String[] userRoles;  // Strings are immutable

    public UserAccount(String username) {
        this.username = username;
        this.userRoles = new String[0];
    }

    // SECURE: Creates defensive copy of array
    public void setUserRoles(String[] userRoles) {
        if (userRoles == null) {
            this.userRoles = new String[0];
            return;
        }

        // Validate before copying
        for (String role : userRoles) {
            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid role: null or empty");
            }
            if (!isValidRole(role)) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }

        // Create defensive copy (String is immutable, so shallow copy is safe)
        this.userRoles = Arrays.copyOf(userRoles, userRoles.length);

        // Alternative: Manual copy
        // this.userRoles = new String[userRoles.length];
        // System.arraycopy(userRoles, 0, this.userRoles, 0, userRoles.length);
    }

    private boolean isValidRole(String role) {
        // Validate against whitelist
        return role.matches("^(USER|ADMIN|READER|EDITOR)$");
    }

    // Secure getter with defensive copy
    public String[] getUserRoles() {
        return Arrays.copyOf(userRoles, userRoles.length);
    }
}
```

### Secure Java (Deep Copy for Mutable Elements)
```java
public class SecurityPolicy {
    private UserRole[] allowedRoles;

    public SecurityPolicy() {
        this.allowedRoles = new UserRole[0];
    }

    // SECURE: Deep copy with validation
    public void setAllowedRoles(UserRole[] roles) {
        if (roles == null) {
            this.allowedRoles = new UserRole[0];
            return;
        }

        // Validate all roles first
        for (UserRole role : roles) {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null");
            }
            validateRole(role);
        }

        // Create deep copy (clone each mutable object)
        this.allowedRoles = new UserRole[roles.length];
        for (int i = 0; i < roles.length; i++) {
            this.allowedRoles[i] = roles[i].clone();
        }
    }

    private void validateRole(UserRole role) {
        if (role.getLevel() < 0 || role.getLevel() > 10) {
            throw new IllegalArgumentException("Invalid role level: " + role.getLevel());
        }
        if (role.getName() == null || role.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid role name");
        }
    }

    // Secure getter with deep copy
    public UserRole[] getAllowedRoles() {
        UserRole[] copy = new UserRole[allowedRoles.length];
        for (int i = 0; i < allowedRoles.length; i++) {
            copy[i] = allowedRoles[i].clone();
        }
        return copy;
    }
}

// Immutable UserRole (preferred approach)
final class UserRole {
    private final String name;
    private final int level;

    public UserRole(String name, int level) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid role name");
        }
        if (level < 0 || level > 10) {
            throw new IllegalArgumentException("Invalid level: " + level);
        }

        this.name = name;
        this.level = level;
    }

    public String getName() { return name; }
    public int getLevel() { return level; }

    // No setters - immutable

    @Override
    public UserRole clone() {
        // Safe to return this for immutable objects
        return this;
        // Or create new instance: return new UserRole(this.name, this.level);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserRole)) return false;
        UserRole other = (UserRole) obj;
        return this.name.equals(other.name) && this.level == other.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, level);
    }
}
```

### Secure Java (Using Collections)
```java
import java.util.*;

public class UserAccount {
    private final List<String> userRoles;

    public UserAccount(String username) {
        this.userRoles = new ArrayList<>();
    }

    // SECURE: Accept collection and create defensive copy
    public void setUserRoles(Collection<String> roles) {
        if (roles == null) {
            this.userRoles.clear();
            return;
        }

        // Validate all roles
        for (String role : roles) {
            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid role");
            }
        }

        // Clear and add all (defensive copy)
        this.userRoles.clear();
        this.userRoles.addAll(roles);
    }

    // Overload for array parameter
    public void setUserRoles(String[] roles) {
        setUserRoles(roles != null ? Arrays.asList(roles) : null);
    }

    // Return unmodifiable view
    public List<String> getUserRoles() {
        return Collections.unmodifiableList(new ArrayList<>(userRoles));
    }

    // Add individual role with validation
    public boolean addRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid role");
        }
        if (!userRoles.contains(role)) {
            userRoles.add(role);
            return true;
        }
        return false;
    }
}
```

### Secure C# (Array Copy)
```csharp
using System;
using System.Linq;

public class Configuration {
    private String[] serverList;
    private int[] portList;

    // SECURE: Creates defensive copy
    public void SetServerList(String[] servers) {
        if (servers == null) {
            this.serverList = new String[0];
            return;
        }

        // Validate
        foreach (String server in servers) {
            if (String.IsNullOrWhiteSpace(server)) {
                throw new ArgumentException("Invalid server name");
            }
        }

        // Create defensive copy
        int length = servers.Length;
        this.serverList = new String[length];
        for (int i = 0; i < length; i++) {
            this.serverList[i] = servers[i];
        }

        // Alternative: Array.Copy
        // this.serverList = new String[servers.Length];
        // Array.Copy(servers, this.serverList, servers.Length);
    }

    public void SetPortList(int[] ports) {
        if (ports == null) {
            this.portList = new int[0];
            return;
        }

        // Validate
        foreach (int port in ports) {
            if (port < 1 || port > 65535) {
                throw new ArgumentException($"Invalid port: {port}");
            }
        }

        // Create defensive copy
        this.portList = (int[])ports.Clone();
    }

    // Return defensive copy
    public String[] GetServerList() {
        return (String[])serverList?.Clone() ?? new String[0];
    }

    public int[] GetPortList() {
        return (int[])portList?.Clone() ?? new int[0];
    }
}
```

### Secure Java (Complete Example with Immutability)
```java
import java.util.*;

public final class SecureConfiguration {
    private final List<String> trustedDomains;
    private final Map<String, Integer> accessLevels;

    private SecureConfiguration(Builder builder) {
        // Create immutable copies
        this.trustedDomains = Collections.unmodifiableList(
            new ArrayList<>(builder.trustedDomains));
        this.accessLevels = Collections.unmodifiableMap(
            new HashMap<>(builder.accessLevels));
    }

    public List<String> getTrustedDomains() {
        // Already unmodifiable
        return trustedDomains;
    }

    public Map<String, Integer> getAccessLevels() {
        // Already unmodifiable
        return accessLevels;
    }

    public boolean isDomainTrusted(String domain) {
        return trustedDomains.contains(domain);
    }

    // Builder pattern for safe construction
    public static class Builder {
        private List<String> trustedDomains = new ArrayList<>();
        private Map<String, Integer> accessLevels = new HashMap<>();

        public Builder addTrustedDomain(String domain) {
            if (domain == null || domain.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid domain");
            }
            trustedDomains.add(domain);
            return this;
        }

        public Builder setTrustedDomains(String[] domains) {
            if (domains == null) return this;

            for (String domain : domains) {
                addTrustedDomain(domain);
            }
            return this;
        }

        public Builder setAccessLevel(String user, int level) {
            if (user == null || level < 0) {
                throw new IllegalArgumentException("Invalid access level");
            }
            accessLevels.put(user, level);
            return this;
        }

        public SecureConfiguration build() {
            return new SecureConfiguration(this);
        }
    }
}

// Usage
public class Example {
    public static void main(String[] args) {
        String[] domains = {"example.com", "trusted.org"};

        SecureConfiguration config = new SecureConfiguration.Builder()
            .setTrustedDomains(domains)
            .setAccessLevel("admin", 10)
            .setAccessLevel("user", 1)
            .build();

        // Modifications to original array don't affect configuration
        domains[0] = "malicious.com";

        System.out.println("Trusted: " + config.isDomainTrusted("example.com"));  // true
        System.out.println("Trusted: " + config.isDomainTrusted("malicious.com"));  // false
    }
}
```

## Detection Methods

### Static Analysis
```bash
# Find setter methods with array parameters
grep -r "public void set.*\[\]" --include="*.java" .

# Find direct array assignments in setters
grep -r "this\\..*=.*\[\];" --include="*.java" .

# Look for assignments without Arrays.copyOf or clone
grep -r "this\\..*=\s*[^A].*\[\];" --include="*.java" .
```

### SpotBugs/FindBugs Rules
```java
// Custom detector for CWE-496
public class StorePrivateArrayDetector implements Detector {

    public void visitMethod(Method method) {
        // Check setter methods
        if (method.getName().startsWith("set") &&
            method.getParameterTypes().length == 1) {

            Type paramType = method.getParameterTypes()[0];
            if (paramType.isArray()) {
                // Check for direct assignment to field
                checkForDirectAssignment(method);
            }
        }
    }

    private void checkForDirectAssignment(Method method) {
        // Analyze bytecode for PUTFIELD without AASTORE (array copy)
        // Report if direct assignment detected
    }
}
```

### Code Review Checklist
- [ ] All array setter methods use defensive copying
- [ ] Deep copying used for arrays of mutable objects
- [ ] Validation performed before copying
- [ ] No direct assignment of parameter arrays to fields
- [ ] Consider using Collections with unmodifiable wrappers
- [ ] Immutable objects preferred over mutable ones

## References

### CWE
- [CWE-496: Public Data Element Assigned to Private Element](https://cwe.mitre.org/data/definitions/496.html)
- [CWE-375: Returning a Mutable Object to an Untrusted Caller](https://cwe.mitre.org/data/definitions/375.html)

### CERT Coding Standards
- [OBJ06-J: Defensively copy mutable inputs and mutable internal components](https://wiki.sei.cmu.edu/confluence/display/java/OBJ06-J)
- [OBJ05-J: Do not return references to private mutable class members](https://wiki.sei.cmu.edu/confluence/display/java/OBJ05-J)

### Additional Resources
- Effective Java (3rd Edition) by Joshua Bloch - Item 50: Make defensive copies when needed
- [Oracle Java Secure Coding Guidelines](https://www.oracle.com/java/technologies/javase/seccodeguide.html)

## Security Checklist

### For Developers
- [ ] Use Arrays.copyOf() for defensive copying
- [ ] Implement deep copying for mutable object arrays
- [ ] Validate input arrays before copying
- [ ] Consider using Collections instead of arrays
- [ ] Prefer immutable objects (final fields, no setters)
- [ ] Use builder pattern for complex object construction
- [ ] Document whether methods perform defensive copying

### For Code Reviewers
- [ ] Verify all array setters use defensive copying
- [ ] Check that validation occurs before copying
- [ ] Confirm deep copying for mutable object arrays
- [ ] Validate proper use of clone() method
- [ ] Review for consistent application of defensive copying
- [ ] Check that getters also use defensive copying

### For Security Auditors
- [ ] Identify all setter methods accepting arrays
- [ ] Verify defensive copying for security-sensitive fields
- [ ] Test that external modifications don't affect internal state
- [ ] Check for privilege escalation through array modification
- [ ] Validate access control can't be bypassed
- [ ] Review thread-safety of array operations
