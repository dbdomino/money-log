# Error Message Information Disclosure (CWE-209, CWE-497)

**Severity**: HIGH
**Category**: Error Handling
**OWASP Top 10**: A04:2021 - Insecure Design

---

## Overview

### Attack Description

Error messages that reveal internal system information (stack traces, database structure, file paths, source code fragments) can help attackers identify vulnerabilities and plan further attacks. While detailed error messages are useful during development for debugging, production systems should display generic error messages to users and log detailed information securely.

### Impact

**Potential consequences:**
- Exposure of system architecture and configuration
- Database schema and query structure disclosure
- File system structure and paths revealed
- Technology stack and version information leaked
- Facilitation of targeted attacks
- Credential or sensitive data exposure
- Compliance violations (PCI-DSS, GDPR)

---

## Security Measures

### Key Principles

Display only generic error messages to users, and securely log detailed error information on the server side. Do not directly expose stack traces or internal system information to users.

**Primary Defenses:**

1. **Generic Error Messages for Users**
   - Show user-friendly, generic messages
   - Avoid technical details in user-facing errors
   - Use error codes instead of detailed messages
   - Implement custom error pages

2. **Secure Server-Side Logging**
   - Log detailed errors server-side only
   - Use logging frameworks (Log4j, SLF4J, Logback)
   - Store logs in secure locations
   - Implement log rotation and retention policies

3. **Separate Development and Production Error Handling**
   - Detailed errors in development environment
   - Generic errors in production environment
   - Use configuration to control error verbosity
   - Never use printStackTrace() in production

4. **Custom Exception Handling**
   - Implement custom exception handlers
   - Map exceptions to user-friendly messages
   - Log exceptions with context information
   - Use correlation IDs for error tracking

5. **Security Headers and Error Pages**
   - Configure custom error pages (404, 500)
   - Disable server version in headers
   - Remove framework signatures from errors
   - Implement proper HTTP status codes

---

## Code Examples

### Attack Scenario

The following is a vulnerable example where a stack trace is printed directly when an exception occurs, exposing system information.

**Vulnerable output to user:**
```
java.sql.SQLException: Table 'users' doesn't exist
    at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1073)
    at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:3593)
    at com.example.UserDAO.getUser(UserDAO.java:45)
    at com.example.LoginServlet.doPost(LoginServlet.java:23)
    ...
```

This reveals: Database type (MySQL), table names, file paths, class structure, line numbers.

---

### Vulnerable Code

#### Java - printStackTrace() to User

```java
public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    try {
        String filename = request.getParameter("filename");
        FileInputStream fis = new FileInputStream(filename);
        // ... file processing ...

    } catch (FileNotFoundException e) {
        // VULNERABLE: Prints stack trace directly to the user
        // File paths and system structure are exposed
        e.printStackTrace();
        out.println("Error: " + e.getMessage());
    } catch (IOException e) {
        // VULNERABLE: Outputs system error directly
        System.err.println(e.getMessage());
        out.println("File processing error: " + e.toString());
    }
}
```

**Problems:**
1. `printStackTrace()` outputs to console and potentially to user
2. `e.getMessage()` reveals file paths
3. `e.toString()` shows exception class names
4. Exposes internal system structure
5. No proper logging mechanism

---

#### Java - Exposing Database Errors

```java
public User getUserById(String userId) {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
        conn = getConnection();
        String sql = "SELECT * FROM users WHERE user_id = ?";
        pstmt = conn.prepareStatement(sql);
        pstmt.setString(1, userId);
        rs = pstmt.executeQuery();

        if (rs.next()) {
            return new User(rs.getString("username"), rs.getString("email"));
        }

    } catch (SQLException e) {
        // VULNERABLE: Exposes database error directly
        // Table structure, column names, and query information are exposed
        System.err.println("SQL Error: " + e.getMessage());
        throw new RuntimeException("Database error: " + e.getSQLState()
            + " - " + e.getMessage());
    }

    return null;
}
```

**Problems:**
1. SQL error messages exposed to caller
2. Database structure revealed
3. SQL state codes exposed
4. No generic error message
5. Stack trace propagated to user

---

#### C# - Exception Details to User

```csharp
protected void Page_Load(object sender, EventArgs e)
{
    try
    {
        string userId = Request.QueryString["id"];
        LoadUserData(userId);
    }
    catch (Exception e)
    {
        // VULNERABLE: Displays exception information directly to the user
        // System paths, method names, line numbers are exposed
        Console.WriteLine(e);
        Response.Write("Error occurred: " + e.ToString());
        Response.Write("<br/>Stack Trace: " + e.StackTrace);
    }
}
```

**Problems:**
1. Full exception details shown to user
2. Stack trace exposed
3. File paths and line numbers revealed
4. Method names and class structure disclosed
5. No error logging

---

### Secure Code

#### Java - Proper Error Logging

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureFileHandler extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(SecureFileHandler.class);

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            String filename = request.getParameter("filename");

            // Validate input first
            if (filename == null || filename.trim().isEmpty()) {
                out.println("ERROR-01: Invalid file request");
                return;
            }

            FileInputStream fis = new FileInputStream(filename);
            // ... file processing ...

        } catch (FileNotFoundException e) {
            // 1. Log detailed information only to the server log
            logger.error("ERROR-01: File not found - {}",
                e.getMessage(), e);

            // 2. Show only a generic message to the user
            out.println("ERROR-01: File open error");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        } catch (IOException e) {
            // Detailed error to log, simple message to user
            logger.error("ERROR-02: File processing error", e);
            out.println("ERROR-02: An error occurred while processing the file");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
```

**Security Features:**
1. Uses SLF4J logger for detailed error logging
2. Generic error codes (ERROR-01, ERROR-02) for users
3. Full exception logged server-side with `logger.error()`
4. User sees only error code and generic message
5. Proper HTTP status codes
6. Input validation before processing

---

#### Java - Database Error Handling

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureUserDAO {

    private static final Logger logger = LoggerFactory.getLogger(SecureUserDAO.class);

    public User getUserById(String userId) throws DataAccessException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            String sql = "SELECT * FROM users WHERE user_id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return new User(
                    rs.getString("username"),
                    rs.getString("email")
                );
            }

            return null;

        } catch (SQLException e) {
            // 1. Log detailed database errors only to the log
            logger.error("ERROR-DB-001: Database error while fetching user. " +
                "UserId: {}, SQLState: {}, ErrorCode: {}",
                userId, e.getSQLState(), e.getErrorCode(), e);

            // 2. Pass only a generic exception to the user/caller
            throw new DataAccessException(
                "ERROR-DB-001: An error occurred while retrieving data.");

        } finally {
            closeResources(rs, pstmt, conn);
        }
    }

    private void closeResources(AutoCloseable... resources) {
        for (AutoCloseable resource : resources) {
            if (resource != null) {
                try {
                    resource.close();
                } catch (Exception e) {
                    logger.warn("Resource cleanup error", e);
                }
            }
        }
    }
}

// Custom exception - no technical details exposed
public class DataAccessException extends Exception {
    public DataAccessException(String message) {
        super(message);
    }
}
```

**Security Features:**
1. Detailed SQL errors logged with context
2. Generic exception thrown to caller
3. Error codes for tracking (ERROR-DB-001)
4. No database structure exposed
5. Proper resource cleanup
6. Correlation with user context

---

#### Java - Spring Boot Global Exception Handler

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {

        // Generate correlation ID for error tracking
        String correlationId = UUID.randomUUID().toString();

        // Log detailed error with correlation ID
        logger.error("ERROR [{}]: DataAccessException - {}",
            correlationId, ex.getMessage(), ex);

        // Return generic error to user with correlation ID
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "An error occurred while processing data");
        errorResponse.put("errorCode", "ERROR-DB-001");
        errorResponse.put("correlationId", correlationId);
        errorResponse.put("timestamp", System.currentTimeMillis());

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        logger.warn("ERROR [{}]: Invalid input - {}",
            correlationId, ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid request");
        errorResponse.put("errorCode", "ERROR-VAL-001");
        errorResponse.put("correlationId", correlationId);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Log unexpected errors with full stack trace
        logger.error("ERROR [{}]: Unexpected error occurred",
            correlationId, ex);

        // Generic error response - no technical details
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "An error occurred while processing the request");
        errorResponse.put("errorCode", "ERROR-SYS-001");
        errorResponse.put("correlationId", correlationId);
        errorResponse.put("message", "Please contact the system administrator");

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

**Security Features:**
1. Centralized exception handling
2. Correlation IDs for error tracking
3. Detailed server-side logging
4. Generic user-facing messages
5. Different handling for different exception types
6. No stack traces exposed
7. Proper HTTP status codes
8. Structured error responses

---

#### C# - Secure Error Handling with Logging

```csharp
using System;
using System.Web;
using log4net;

public partial class SecurePage : System.Web.UI.Page
{
    private static readonly ILog _log = LogManager.GetLogger(typeof(SecurePage));

    protected void Page_Load(object sender, EventArgs e)
    {
        try
        {
            string userId = Request.QueryString["id"];

            if (string.IsNullOrEmpty(userId))
            {
                Response.Write("ERROR-01: Invalid request");
                return;
            }

            LoadUserData(userId);
        }
        catch (System.Data.SqlException ex)
        {
            // 1. Log detailed database errors only to the log
            _log.Error("ERROR-DB-001: Database error occurred", ex);
            _log.Debug($"ERROR-DB-001: SQLState={ex.Number}, Message={ex.Message}");

            // 2. Show only a generic message to the user
            Response.Write("ERROR-DB-001: An error occurred while processing data");
            Response.StatusCode = 500;
        }
        catch (UnauthorizedAccessException ex)
        {
            // Authorization error logging
            _log.Warn($"ERROR-AUTH-001: Unauthorized access attempt - {ex.Message}");

            Response.Write("ERROR-AUTH-001: Access denied");
            Response.StatusCode = 403;
        }
        catch (Exception ex)
        {
            // Log unexpected errors in detail
            _log.Error("ERROR-SYS-001: Unexpected error", ex);
            _log.Debug($"ERROR-SYS-001: error information - Type={ex.GetType().Name}");

            // Show only a generic error message
            Response.Write("ERROR-SYS-001: An error occurred while processing the request");
            Response.StatusCode = 500;
        }
    }

    private void LoadUserData(string userId)
    {
        // Implementation
    }
}
```

**Security Features:**
1. Log4net for structured logging
2. Detailed errors logged with `_log.Error()` and `_log.Debug()`
3. Generic error codes shown to users
4. Different log levels (Error, Warn, Debug)
5. No exception details in user response
6. Proper HTTP status codes
7. Context information in logs

---

#### application.properties - Production Configuration

```properties
# Logging configuration for production
logging.level.root=WARN
logging.level.com.example=INFO
logging.level.org.springframework.web=INFO

# Disable detailed error messages in production
server.error.include-message=never
server.error.include-binding-errors=never
server.error.include-stacktrace=never
server.error.include-exception=false

# Custom error page
server.error.whitelabel.enabled=false
server.error.path=/error

# Log file configuration
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

---

#### web.xml - Custom Error Pages

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee
         http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd"
         version="3.1">

    <!-- Custom error pages -->
    <error-page>
        <error-code>404</error-code>
        <location>/errors/404.html</location>
    </error-page>

    <error-page>
        <error-code>403</error-code>
        <location>/errors/403.html</location>
    </error-page>

    <error-page>
        <error-code>500</error-code>
        <location>/errors/500.html</location>
    </error-page>

    <error-page>
        <exception-type>java.lang.Exception</exception-type>
        <location>/errors/general-error.html</location>
    </error-page>

    <!-- Disable detailed error messages -->
    <context-param>
        <param-name>showServerInfo</param-name>
        <param-value>false</param-value>
    </context-param>
</web-app>
```

---

## References

### CWE (Common Weakness Enumeration)

1. **CWE-209: Generation of Error Message Containing Sensitive Information**
   MITRE, https://cwe.mitre.org/data/definitions/209.html

2. **CWE-497: Exposure of Sensitive System Information to an Unauthorized Control Sphere**
   MITRE, https://cwe.mitre.org/data/definitions/497.html

3. **CWE-200: Exposure of Sensitive Information to an Unauthorized Actor**
   MITRE, https://cwe.mitre.org/data/definitions/200.html

### OWASP

4. **A04:2021 - Insecure Design**
   OWASP Top 10, https://owasp.org/Top10/A04_2021-Insecure_Design/

5. **Error Handling Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html

6. **Logging Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html

### CERT

7. **ERR01-J: Do not allow exceptions to expose sensitive information**
   CERT Oracle Secure Coding Standard for Java
   https://wiki.sei.cmu.edu/confluence/display/java/ERR01-J

---

## Detection Patterns (Grep/Search)

Use these patterns to detect error message information disclosure vulnerabilities:

```bash
# Find printStackTrace() calls
grep -r "printStackTrace()" --include="*.java" .
grep -r "\.printStackTrace" --include="*.java" .

# Find System.err usage
grep -r "System\.err\.println" --include="*.java" .
grep -r "System\.out\.println.*Exception" --include="*.java" .

# Find exposed exception messages
grep -r "e\.getMessage()" --include="*.java" . | grep -i "response\|out\.print"
grep -r "e\.toString()" --include="*.java" . | grep -i "response\|out\.print"

# Find C# exception exposure
grep -r "Console\.WriteLine.*Exception" --include="*.cs" .
grep -r "Response\.Write.*\.ToString()" --include="*.cs" .
grep -r "\.StackTrace" --include="*.cs" . | grep -i "response"

# Find missing logging
grep -r "catch.*Exception" --include="*.java" . | grep -v "logger\|log\|LOG"

# Check Spring Boot error configuration
grep -r "server.error.include-stacktrace" --include="*.properties" .
grep -r "server.error.include-message" --include="*.properties" .
```

---

## Security Checklist

- [ ] No `printStackTrace()` in production code
- [ ] No `System.err.println()` or `System.out.println()` for errors
- [ ] Logging framework implemented (SLF4J, Log4j, Logback)
- [ ] Generic error messages for users
- [ ] Detailed errors logged server-side only
- [ ] Error codes used instead of technical messages
- [ ] Custom error pages configured (404, 500, etc.)
- [ ] Stack traces disabled in production
- [ ] Server version information hidden
- [ ] Exception details not exposed in API responses
- [ ] Correlation IDs used for error tracking
- [ ] Different error handling for dev/prod environments
- [ ] Logs stored securely with proper access controls
- [ ] Log rotation and retention policies implemented
- [ ] No database errors exposed to users
- [ ] No file paths or system info in error messages

---

## Framework-Specific Best Practices

### Spring Boot

```java
// application.properties (Production)
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
logging.level.root=WARN
```

### Java EE

```xml
<!-- web.xml -->
<error-page>
    <exception-type>java.lang.Exception</exception-type>
    <location>/error.jsp</location>
</error-page>
```

### ASP.NET

```xml
<!-- Web.config -->
<system.web>
    <customErrors mode="On" defaultRedirect="/Error.html">
        <error statusCode="404" redirect="/Error404.html" />
        <error statusCode="500" redirect="/Error500.html" />
    </customErrors>
</system.web>
```

---

## Logging Best Practices

### 1. Use Appropriate Log Levels

```java
logger.trace("Entering method with param: {}", param);  // Detailed trace
logger.debug("Processing user data: {}", userId);       // Debug info
logger.info("User logged in: {}", username);            // Normal operation
logger.warn("Invalid input detected: {}", input);       // Warning
logger.error("Database connection failed", exception);  // Error
```

### 2. Include Context Information

```java
logger.error("ERROR-DB-001: Failed to fetch user. " +
    "UserId={}, Timestamp={}, RequestId={}",
    userId, System.currentTimeMillis(), requestId, exception);
```

### 3. Use Correlation IDs

```java
String correlationId = UUID.randomUUID().toString();
MDC.put("correlationId", correlationId);
logger.error("Error processing request", exception);
MDC.clear();
```

---

## Common Mistakes

1. **Logging Sensitive Data**
   ```java
   // DON'T: Log passwords or sensitive data
   logger.info("User login: username={}, password={}", username, password);

   // DO: Log only non-sensitive information
   logger.info("User login attempt: username={}", username);
   ```

2. **Exposing Stack Traces in API**
   ```java
   // DON'T: Return exception in JSON
   return ResponseEntity.status(500)
       .body(Map.of("error", exception.getMessage()));

   // DO: Return generic error
   return ResponseEntity.status(500)
       .body(Map.of("error", "Internal server error", "code", "ERR-500"));
   ```

3. **Development Settings in Production**
   ```java
   // DON'T: Enable debug mode in production
   spring.profiles.active=dev
   logging.level.root=DEBUG

   // DO: Use production profile
   spring.profiles.active=prod
   logging.level.root=WARN
   ```

---

**Show generic error messages to users and log detailed information securely!**
