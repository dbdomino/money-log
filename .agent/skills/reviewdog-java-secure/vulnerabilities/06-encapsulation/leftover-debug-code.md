# Leftover Debug Code

## Metadata
- **CWE ID**: CWE-489
- **Severity**: Medium to High
- **OWASP Category**: A05:2021 - Security Misconfiguration
- **Detection Difficulty**: Easy to Medium

## Description

Leftover debug code refers to diagnostic code, logging statements, test methods, or development utilities that remain in production builds. This includes debug print statements, test main() methods, verbose logging, internal state dumping, and development-only features. Debug code often exposes sensitive information such as passwords, internal system paths, database queries, user data, and business logic details that attackers can exploit.

Common forms of debug code include:
- `main()` methods in production classes used for testing
- Debug logging with sensitive data (passwords, tokens, PII)
- Commented-out code that reveals system internals
- Development endpoints or features not disabled in production
- Verbose error messages with stack traces and internal details

## Vulnerable Code Examples

### Vulnerable Java (Debug main() Method)
```java
public class UserAuthenticator {
    private String username;
    private String password;

    public boolean authenticate(String user, String pass) {
        // Production authentication logic
        this.username = user;
        this.password = pass;
        return checkCredentials();
    }

    private boolean checkCredentials() {
        // Database validation
        return true;
    }

    // VULNERABLE: Debug main method left in production code
    public static void main(String[] args) {
        UserAuthenticator auth = new UserAuthenticator();

        // Exposes test credentials and system paths
        System.out.println("Testing authentication system...");
        System.out.println("Test user: admin");
        System.out.println("Test password: P@ssw0rd123");
        System.out.println("Database path: /var/db/users.db");

        boolean result = auth.authenticate("admin", "P@ssw0rd123");
        System.out.println("Authentication result: " + result);

        // Reveals internal state
        System.out.println("Current user: " + auth.username);
        System.out.println("Current password: " + auth.password);
    }
}
```

### Vulnerable Java (Debug Logging)
```java
public class PaymentProcessor {
    private static final Logger logger = Logger.getLogger(PaymentProcessor.class.getName());
    private boolean debug = true;  // VULNERABLE: Debug flag left enabled

    public void processPayment(String cardNumber, String cvv, double amount) {
        // VULNERABLE: Logs sensitive payment information
        if (debug) {
            logger.info("Processing payment:");
            logger.info("Card Number: " + cardNumber);
            logger.info("CVV: " + cvv);
            logger.info("Amount: " + amount);
            logger.info("Timestamp: " + System.currentTimeMillis());
        }

        // VULNERABLE: Debug array iteration
        String[] transactionSteps = {"validate", "authorize", "capture", "settle"};
        if (debug) {
            for (int i = 0; i < transactionSteps.length; i++) {
                System.out.printf("Step %d: %s\n", i, transactionSteps[i]);
            }
        }

        // Process payment
        performTransaction(cardNumber, cvv, amount);
    }

    private void performTransaction(String cardNumber, String cvv, double amount) {
        // Transaction logic
    }
}
```

### Vulnerable C# (Console Debug Output)
```csharp
using System;
using System.Data.SqlClient;

public class DatabaseManager {
    private string connectionString;

    public void Connect(string server, string database, string username, string password) {
        connectionString = $"Server={server};Database={database};User Id={username};Password={password};";

        // VULNERABLE: Exposes connection string with credentials
        Console.WriteLine("Connecting to database...");
        Console.WriteLine("Connection string: " + connectionString);
        Console.WriteLine("Server: " + server);
        Console.WriteLine("Username: " + username);
        Console.WriteLine("Password: " + password);  // Critical: password exposure

        using (SqlConnection connection = new SqlConnection(connectionString)) {
            try {
                connection.Open();
                Console.WriteLine("Connection successful!");

                // VULNERABLE: Dumps connection state
                Console.WriteLine("Connection state: " + connection.State);
                Console.WriteLine("Database: " + connection.Database);
            } catch (Exception ex) {
                // VULNERABLE: Exposes full exception details
                Console.WriteLine("Connection failed: " + ex.ToString());
                Console.WriteLine("Stack trace: " + ex.StackTrace);
            }
        }
    }
}
```

### Vulnerable Java (Development Endpoints)
```java
import javax.servlet.http.*;
import java.io.*;

public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        // VULNERABLE: Debug endpoint left enabled in production
        if ("debug".equals(action)) {
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();

            out.println("=== DEBUG INFORMATION ===");
            out.println("Java Version: " + System.getProperty("java.version"));
            out.println("OS: " + System.getProperty("os.name"));
            out.println("User Dir: " + System.getProperty("user.dir"));
            out.println("Classpath: " + System.getProperty("java.class.path"));

            // Exposes environment variables
            out.println("\n=== ENVIRONMENT VARIABLES ===");
            System.getenv().forEach((key, value) ->
                out.println(key + "=" + value));

            // Exposes session data
            out.println("\n=== SESSION DATA ===");
            HttpSession session = request.getSession();
            session.getAttributeNames().asIterator().forEachRemaining(attr ->
                out.println(attr + "=" + session.getAttribute(attr)));
        }
    }
}
```

## Secure Code Examples

### Secure Java (Remove Debug Code)
```java
public class UserAuthenticator {
    private static final Logger logger = Logger.getLogger(UserAuthenticator.class.getName());

    private String username;
    private String password;

    public boolean authenticate(String user, String pass) {
        this.username = user;
        this.password = pass;

        boolean result = checkCredentials();

        // SECURE: Log only non-sensitive information
        if (result) {
            logger.info("User authenticated successfully: " + user);
        } else {
            logger.warning("Authentication failed for user: " + user);
        }

        return result;
    }

    private boolean checkCredentials() {
        // Database validation
        return true;
    }

    // SECURE: No main() method in production code
    // Move tests to separate test classes in src/test
}
```

### Secure Java (Conditional Compilation with Build Flags)
```java
public class PaymentProcessor {
    private static final Logger logger = Logger.getLogger(PaymentProcessor.class.getName());

    // SECURE: Debug flag controlled by system property
    private static final boolean DEBUG = Boolean.getBoolean("payment.debug");

    public void processPayment(String cardNumber, String cvv, double amount) {
        // SECURE: Log only masked data, even in debug mode
        if (DEBUG) {
            logger.fine("Processing payment for masked card: " + maskCardNumber(cardNumber));
            logger.fine("Amount: " + amount);
        }

        // SECURE: Production logging without sensitive data
        logger.info("Payment processing initiated for amount: " + amount);

        performTransaction(cardNumber, cvv, amount);
    }

    private void performTransaction(String cardNumber, String cvv, double amount) {
        // Transaction logic
        logger.info("Transaction completed successfully");
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }
}
```

### Secure C# (Proper Logging)
```csharp
using System;
using System.Data.SqlClient;
using Microsoft.Extensions.Logging;

public class DatabaseManager {
    private readonly ILogger<DatabaseManager> _logger;
    private string connectionString;

    public DatabaseManager(ILogger<DatabaseManager> logger) {
        _logger = logger;
    }

    public void Connect(string server, string database, string username, string password) {
        connectionString = $"Server={server};Database={database};User Id={username};Password={password};";

        // SECURE: Log only non-sensitive information
        _logger.LogInformation("Attempting database connection to server: {Server}, database: {Database}",
            server, database);

        using (SqlConnection connection = new SqlConnection(connectionString)) {
            try {
                connection.Open();
                _logger.LogInformation("Database connection established successfully");
            } catch (SqlException ex) {
                // SECURE: Log error without exposing sensitive details
                _logger.LogError("Database connection failed: {ErrorCode}", ex.Number);
                throw new ApplicationException("Unable to connect to database", ex);
            }
        }
    }
}
```

### Secure Java (Production-Ready Servlet)
```java
import javax.servlet.http.*;
import java.io.*;
import java.util.logging.*;

public class AdminServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(AdminServlet.class.getName());

    // SECURE: Debug mode controlled by configuration
    private static final boolean DEBUG_MODE =
        "true".equals(System.getenv("APP_DEBUG_MODE"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // SECURE: Verify admin authorization
        if (!isAuthorized(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            logger.warning("Unauthorized access attempt to admin servlet");
            return;
        }

        String action = request.getParameter("action");

        // SECURE: Debug endpoint disabled in production
        if (DEBUG_MODE && "debug".equals(action)) {
            // Only expose minimal, non-sensitive debug info
            response.setContentType("text/plain");
            PrintWriter out = response.getWriter();
            out.println("Application Version: 1.0.0");
            out.println("Server Time: " + new java.util.Date());
            logger.info("Debug endpoint accessed by authorized user");
        } else if ("debug".equals(action)) {
            // In production, log attempt and deny
            logger.warning("Debug endpoint access attempted in production mode");
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean isAuthorized(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;

        Object userRole = session.getAttribute("userRole");
        return "ADMIN".equals(userRole);
    }
}
```

### Secure Build Configuration (Maven)
```xml
<!-- pom.xml - Separate debug and production builds -->
<project>
    <profiles>
        <profile>
            <id>development</id>
            <properties>
                <maven.compiler.debuglevel>lines,vars,source</maven.compiler.debuglevel>
                <logging.level>DEBUG</logging.level>
            </properties>
        </profile>

        <profile>
            <id>production</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <maven.compiler.debuglevel>none</maven.compiler.debuglevel>
                <logging.level>INFO</logging.level>
            </properties>
            <build>
                <plugins>
                    <!-- Remove debug code in production -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <configuration>
                            <excludes>
                                <exclude>**/*Test.java</exclude>
                                <exclude>**/Debug*.java</exclude>
                            </excludes>
                        </configuration>
                    </plugin>
                </plugins>
            </build>
        </profile>
    </profiles>
</project>
```

## Detection Methods

### Static Analysis
```bash
# Find main() methods in production source code
find src/main/java -name "*.java" -exec grep -l "public static void main" {} \;

# Find debug print statements
grep -r "System.out.println\|System.err.println" src/main/java/

# Find Console.WriteLine in C# code
grep -r "Console.WriteLine" --include="*.cs" .

# Find debug flags
grep -r "debug\s*=\s*true\|DEBUG\s*=\s*true" --include="*.java" --include="*.cs" .

# Find potential password logging
grep -r "password.*log\|log.*password" --include="*.java" -i .
```

### Code Review Checklist
- [ ] No `main()` methods in production classes
- [ ] No `System.out.println()` or `Console.WriteLine()` in production code
- [ ] Debug flags are false or controlled by environment
- [ ] No sensitive data in log statements
- [ ] No commented-out code revealing system internals
- [ ] Development endpoints disabled in production
- [ ] Exception messages don't expose internal details
- [ ] Build process strips debug symbols in production

### SonarQube Rules
```xml
<!-- Custom SonarQube rule for debug code detection -->
<rule>
    <key>leftover-debug-code</key>
    <name>Leftover Debug Code</name>
    <severity>MAJOR</severity>
    <description>
        Detect debug code patterns:
        - main() methods in production code
        - System.out/Console.WriteLine
        - Debug logging with sensitive data
    </description>
</rule>
```

## References

### CWE
- [CWE-489: Leftover Debug Code](https://cwe.mitre.org/data/definitions/489.html)

### CERT Coding Standards
- [MSC11-J: Do not let session information leak within a servlet](https://wiki.sei.cmu.edu/confluence/display/java/MSC11-J)
- [ENV06-J: Production code must not contain debugging entry points](https://wiki.sei.cmu.edu/confluence/display/java/ENV06-J)

### OWASP
- [OWASP Top 10 2021 A05:2021 - Security Misconfiguration](https://owasp.org/Top10/A05_2021-Security_Misconfiguration/)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)

### Additional Resources
- [OWASP Code Review Guide - Reviewing Code for Information Leakage](https://owasp.org/www-project-code-review-guide/)

## Security Checklist

### For Developers
- [ ] Remove all `main()` test methods from production classes
- [ ] Replace `System.out.println()` with proper logging framework
- [ ] Disable or remove debug flags before release
- [ ] Review all logging statements for sensitive data
- [ ] Remove commented-out code
- [ ] Disable development-only features in production builds
- [ ] Use logging levels appropriately (DEBUG, INFO, WARN, ERROR)
- [ ] Configure build process to strip debug code

### For Code Reviewers
- [ ] Verify no debug output statements in code
- [ ] Check for test methods in production classes
- [ ] Confirm debug flags are disabled or environment-controlled
- [ ] Review exception handling for information disclosure
- [ ] Validate logging statements don't expose credentials
- [ ] Check for development endpoints or backdoors
- [ ] Ensure proper logging framework usage

### For DevOps/Release Engineers
- [ ] Verify production build excludes test code
- [ ] Confirm debug symbols stripped from binaries
- [ ] Validate logging configuration for production
- [ ] Check environment variables don't enable debug mode
- [ ] Review deployment scripts for debug flags
- [ ] Test production build doesn't expose debug endpoints
- [ ] Audit log files for sensitive data exposure
