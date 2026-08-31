# Java Secure Coding Quick Reference

Quick reference guide for common Java security vulnerabilities and their solutions.

---

## Input Validation

### SQL Injection (CWE-89)
```java
// ❌ VULNERABLE
String query = "SELECT * FROM users WHERE id = " + userId;

// ✅ SECURE - PreparedStatement
PreparedStatement pstmt = conn.prepareStatement(
    "SELECT * FROM users WHERE id = ?"
);
pstmt.setInt(1, userId);
```

### XSS (CWE-79)
```java
// ❌ VULNERABLE
out.println("<div>" + userInput + "</div>");

// ✅ SECURE - OWASP Java Encoder
import org.owasp.encoder.Encode;
out.println("<div>" + Encode.forHtml(userInput) + "</div>");
```

### Command Injection (CWE-78)
```java
// ❌ VULNERABLE
Runtime.getRuntime().exec("ls " + userInput);

// ✅ SECURE - ProcessBuilder with arguments
ProcessBuilder pb = new ProcessBuilder("ls", userInput);
Process p = pb.start();
```

### Path Traversal (CWE-22)
```java
// ❌ VULNERABLE
File file = new File("/uploads/" + filename);

// ✅ SECURE - Path normalization
Path basePath = Paths.get("/uploads").toRealPath();
Path filePath = basePath.resolve(filename).normalize();
if (!filePath.startsWith(basePath)) {
    throw new SecurityException("Path traversal attempt");
}
```

### Insecure Deserialization (CWE-502)
```java
// ❌ VULNERABLE
ObjectInputStream ois = new ObjectInputStream(input);
Object obj = ois.readObject();

// ✅ SECURE - Use JSON instead
ObjectMapper mapper = new ObjectMapper();
MyClass obj = mapper.readValue(input, MyClass.class);

// ✅ OR - Use ObjectInputFilter (Java 9+)
ois.setObjectInputFilter(info -> {
    if (ALLOWED_CLASSES.contains(info.serialClass().getName())) {
        return Status.ALLOWED;
    }
    return Status.REJECTED;
});
```

---

## Cryptography

### Weak Algorithms (CWE-327)
```java
// ❌ VULNERABLE
MessageDigest md = MessageDigest.getInstance("MD5");

// ✅ SECURE
MessageDigest md = MessageDigest.getInstance("SHA-256");
```

### Hardcoded Passwords (CWE-259)
```java
// ❌ VULNERABLE
String password = "admin123";

// ✅ SECURE - Environment variables
String password = System.getenv("DB_PASSWORD");
```

### Hash Without Salt (CWE-760)
```java
// ❌ VULNERABLE
String hash = DigestUtils.sha256Hex(password);

// ✅ SECURE - BCrypt with salt
String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
boolean valid = BCrypt.checkpw(password, hash);
```

### Insufficient Random (CWE-330)
```java
// ❌ VULNERABLE
Random rand = new Random();
int token = rand.nextInt();

// ✅ SECURE
SecureRandom secureRand = new SecureRandom();
byte[] token = new byte[32];
secureRand.nextBytes(token);
```

### Weak Key Length (CWE-326)
```java
// ❌ VULNERABLE - RSA 1024
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
keyGen.initialize(1024);

// ✅ SECURE - RSA 2048+
keyGen.initialize(2048);
```

---

## Authentication & Authorization

### Missing Authentication (CWE-306)
```java
// ❌ VULNERABLE
@GetMapping("/admin")
public String adminPanel() {
    return "admin";
}

// ✅ SECURE - Spring Security
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public String adminPanel() {
    return "admin";
}
```

### Improper Authorization (CWE-285)
```java
// ❌ VULNERABLE - No ownership check
public void deleteDocument(int docId) {
    documentRepo.delete(docId);
}

// ✅ SECURE - Verify ownership
public void deleteDocument(int docId, String userId) {
    Document doc = documentRepo.findById(docId);
    if (!doc.getOwnerId().equals(userId)) {
        throw new AccessDeniedException("Not your document");
    }
    documentRepo.delete(docId);
}
```

### Weak Password Requirements (CWE-521)
```java
// ❌ VULNERABLE
if (password.length() >= 6) { /* OK */ }

// ✅ SECURE
Pattern pattern = Pattern.compile(
    "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$"
);
if (!pattern.matcher(password).matches()) {
    throw new IllegalArgumentException(
        "Password must be 12+ chars with uppercase, lowercase, digit, special"
    );
}
```

---

## Session Management

### Session Fixation
```java
// ❌ VULNERABLE
// Reusing session ID after login

// ✅ SECURE - Regenerate session
HttpSession oldSession = request.getSession(false);
if (oldSession != null) {
    oldSession.invalidate();
}
HttpSession newSession = request.getSession(true);
```

### Cookie Security (CWE-539)
```java
// ❌ VULNERABLE
Cookie cookie = new Cookie("sessionId", sessionId);

// ✅ SECURE
Cookie cookie = new Cookie("sessionId", sessionId);
cookie.setHttpOnly(true);  // Prevent XSS access
cookie.setSecure(true);    // HTTPS only
cookie.setSameSite("Strict"); // CSRF protection
cookie.setMaxAge(3600);    // 1 hour expiration
```

---

## Error Handling

### Information Disclosure (CWE-209)
```java
// ❌ VULNERABLE
catch (Exception e) {
    response.getWriter().println("Error: " + e.getMessage());
    e.printStackTrace();
}

// ✅ SECURE
catch (Exception e) {
    logger.error("Database error", e);
    response.getWriter().println("An error occurred. Please try again.");
}
```

### Improper Exception Handling (CWE-755)
```java
// ❌ VULNERABLE
try {
    riskyOperation();
} catch (Exception e) {
    // Empty catch block
}

// ✅ SECURE
try {
    riskyOperation();
} catch (SpecificException e) {
    logger.error("Operation failed", e);
    throw new BusinessException("Could not complete operation", e);
}
```

---

## Resource Management

### Resource Leak (CWE-404)
```java
// ❌ VULNERABLE
FileInputStream fis = new FileInputStream("file.txt");
// If exception occurs, stream not closed

// ✅ SECURE - try-with-resources
try (FileInputStream fis = new FileInputStream("file.txt")) {
    // Use stream
} // Automatically closed
```

### Null Pointer (CWE-476)
```java
// ❌ VULNERABLE
public void processUser(User user) {
    String name = user.getName().toUpperCase();
}

// ✅ SECURE
public void processUser(User user) {
    if (user == null || user.getName() == null) {
        throw new IllegalArgumentException("User and name required");
    }
    String name = user.getName().toUpperCase();
}

// ✅ BETTER - Java 8+ Optional
public void processUser(Optional<User> userOpt) {
    String name = userOpt
        .map(User::getName)
        .orElse("Unknown")
        .toUpperCase();
}
```

---

## Encapsulation

### Private Array Returned (CWE-495)
```java
// ❌ VULNERABLE
private String[] roles;
public String[] getRoles() {
    return roles;  // Caller can modify internal array
}

// ✅ SECURE - Defensive copy
public String[] getRoles() {
    return roles.clone();
}

// ✅ BETTER - Immutable collection
public List<String> getRoles() {
    return Collections.unmodifiableList(Arrays.asList(roles));
}
```

### Public Data Assigned (CWE-496)
```java
// ❌ VULNERABLE
private String[] roles;
public void setRoles(String[] roles) {
    this.roles = roles;  // Caller retains reference
}

// ✅ SECURE - Defensive copy
public void setRoles(String[] roles) {
    this.roles = roles.clone();
}
```

---

## Concurrency

### Race Condition (CWE-362)
```java
// ❌ VULNERABLE
private int counter = 0;
public void increment() {
    counter++;  // Not thread-safe
}

// ✅ SECURE - Synchronized
private int counter = 0;
public synchronized void increment() {
    counter++;
}

// ✅ BETTER - AtomicInteger
private AtomicInteger counter = new AtomicInteger(0);
public void increment() {
    counter.incrementAndGet();
}
```

---

## Common Security Headers

### Spring Security Configuration
```java
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .headers()
                .contentSecurityPolicy("default-src 'self'")
                .and()
                .xssProtection()
                .and()
                .contentTypeOptions()
                .and()
                .frameOptions().deny()
            .and()
            .csrf().csrfTokenRepository(
                CookieCsrfTokenRepository.withHttpOnlyFalse()
            );
    }
}
```

---

## Dependency Security

### Check for Vulnerabilities
```bash
# Maven
mvn org.owasp:dependency-check-maven:check

# Gradle
gradle dependencyCheckAnalyze

# Snyk
snyk test
```

### Keep Dependencies Updated
```xml
<!-- Use Maven Versions Plugin -->
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>versions-maven-plugin</artifactId>
    <version>2.14.2</version>
</plugin>
```

```bash
mvn versions:display-dependency-updates
```

---

## Static Analysis Tools

```bash
# SpotBugs (FindBugs successor)
mvn spotbugs:check

# PMD
mvn pmd:check

# Checkstyle
mvn checkstyle:check

# SonarQube
mvn sonar:sonar
```

---

## Security Testing

### OWASP ZAP
```bash
# Dynamic Application Security Testing (DAST)
zap-cli quick-scan http://localhost:8080
```

### Burp Suite
- Intercept HTTP requests
- Test for injection vulnerabilities
- Scan for common vulnerabilities

---

## Secure Development Checklist

### Input Validation
- [ ] All user input validated
- [ ] PreparedStatement for SQL
- [ ] Output encoding for XSS
- [ ] Path traversal prevention
- [ ] Command injection prevention

### Authentication
- [ ] Strong password policy
- [ ] Account lockout after failed attempts
- [ ] Session regeneration after login
- [ ] Secure cookie attributes

### Authorization
- [ ] Role-based access control
- [ ] Resource ownership verification
- [ ] Principle of least privilege

### Cryptography
- [ ] Strong algorithms (SHA-256+, AES-256)
- [ ] Secure random (SecureRandom)
- [ ] Password hashing with salt (BCrypt, Argon2)
- [ ] HTTPS everywhere

### Error Handling
- [ ] Generic error messages to users
- [ ] Detailed errors in logs only
- [ ] Proper exception handling
- [ ] No stack traces to users

### Dependencies
- [ ] Regular updates
- [ ] Vulnerability scanning
- [ ] OWASP Dependency-Check

---

## Spring Security Quick Start

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

## References

- [OWASP Top 10](https://owasp.org/Top10/)
- [CWE Database](https://cwe.mitre.org/)
- [CERT Java Coding Standard](https://wiki.sei.cmu.edu/confluence/display/java)
- [Spring Security](https://spring.io/projects/spring-security)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)

---

**Last Updated**: 2026-02-05
**Version**: 1.0
