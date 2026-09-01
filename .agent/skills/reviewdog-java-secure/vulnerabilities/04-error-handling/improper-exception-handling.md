# Improper Exception Handling (CWE-754)

**Severity**: MEDIUM
**Category**: Error Handling
**OWASP Top 10**: A04:2021 - Insecure Design

---

## Overview

### Attack Description

Improper exception handling occurs when code catches generic exceptions (like `Exception` or `Throwable`) instead of specific exception types, making it impossible to respond appropriately to different error conditions. Each exception type represents a different error scenario requiring different handling strategies. Simply printing error messages without proper logging or recovery undermines system stability and security.

### Impact

**Potential consequences:**
- Inability to distinguish between error types
- Inappropriate error recovery
- Hidden bugs and issues
- Security vulnerabilities masked
- Difficulty in debugging and troubleshooting
- System instability
- Resource leaks
- Inconsistent application state
- Poor user experience

---

## Security Measures

### Key Principles

Each exception type must be handled with specific processing. Instead of catching the generic `Exception`, catch specific exception types (`IOException`, `SQLException`, etc.) and respond appropriately to each.

**Primary Defenses:**

1. **Catch Specific Exceptions**
   - Catch most specific exception types first
   - Avoid catching generic `Exception` or `Throwable`
   - Handle each exception type appropriately
   - Order catch blocks from specific to general

2. **Multiple Catch Blocks**
   - Use separate catch blocks for different exceptions
   - Implement specific recovery logic for each
   - Log with appropriate context
   - Provide meaningful error messages

3. **Proper Exception Hierarchy**
   - Understand exception inheritance
   - Catch child exceptions before parent
   - Don't catch `Error` or `Throwable` unless necessary
   - Use multi-catch for similar handling (Java 7+)

4. **Appropriate Recovery Actions**
   - Retry for transient errors (network, timeout)
   - Rollback for transaction errors
   - Fail-fast for configuration errors
   - Degrade gracefully for non-critical errors

5. **Logging and Monitoring**
   - Log different exceptions with different levels
   - Include context information
   - Use correlation IDs
   - Monitor exception patterns

---

## Code Examples

### Attack Scenario

The following is a vulnerable example where all exceptions are caught generically as `Exception`, preventing appropriate handling for each error scenario.

**Vulnerable catch:**
```java
try {
    // Multiple operations that can fail differently
} catch (Exception e) {
    System.err.println("Exception : " + e.getMessage());
    // Cannot distinguish between network error, parsing error, or file error
}
```

Each exception has different causes and resolutions, but they are all handled identically, making proper recovery impossible.

---

### ❌ Vulnerable Code

#### Java - Generic Exception Catching

```java
public void processURL(String urlString) {
    try {
        // Operations that can throw various types of exceptions
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream in = connection.getInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        // JSON parsing
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(response.toString());

        // Save data
        saveToDatabase(json);

    } catch (Exception e) {
        // VULNERABLE: Catches all exceptions generically
        // MalformedURLException, IOException, ParseException, SQLException
        // are all handled the same way
        System.err.println("Exception : " + e.getMessage());
    }
}
```

**Problems:**
1. Cannot distinguish between different error types
2. No specific recovery for each error
3. MalformedURLException (invalid URL) handled same as IOException (network error)
4. ParseException (bad JSON) handled same as SQLException (database error)
5. Using `System.err.println` instead of logger
6. No proper error recovery
7. Resources may not be closed properly

---

#### Java - Catching Throwable

```java
public void criticalOperation() {
    try {
        // Perform critical operations
        initializeSystem();
        processData();
        saveResults();

    } catch (Throwable t) {
        // VULNERABLE: Catching Throwable also catches Error,
        // not just Exception.
        // Severe errors like OutOfMemoryError and StackOverflowError are also caught
        System.out.println("Error occurred: " + t.getMessage());

        // Error indicates an unrecoverable JVM state,
        // so catching it and continuing execution is dangerous
    }
}
```

**Problems:**
1. Catches `Error` which should not be caught
2. Cannot recover from `OutOfMemoryError` or `StackOverflowError`
3. JVM may be in unstable state
4. Masks serious system problems
5. No proper logging

---

#### Java - Ignoring Specific Exception Types

```java
public String fetchUserData(String userId) {
    String userData = null;

    try {
        // HTTP request
        URL url = new URL("http://api.example.com/user/" + userId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);

        InputStream is = conn.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        userData = br.readLine();

    } catch (Exception e) {
        // VULNERABLE: All exceptions handled identically
        // - MalformedURLException: URL format error -> no retry needed
        // - SocketTimeoutException: Timeout -> retry needed
        // - IOException: Network error -> retry needed
        // But all return null the same way
        System.err.println("Failed to fetch user data: " + e.getMessage());
    }

    return userData;
}
```

**Problems:**
1. Cannot implement retry logic for transient errors
2. Configuration errors (bad URL) treated same as network errors
3. No differentiation between recoverable and non-recoverable errors
4. Poor user experience
5. No proper logging

---

#### C# - Generic Exception Handling

```csharp
public void ProcessFile(string filePath)
{
    try
    {
        // Read file
        string content = File.ReadAllText(filePath);

        // Parse XML
        XmlDocument doc = new XmlDocument();
        doc.LoadXml(content);

        // Save to database
        SaveToDatabase(doc);
    }
    catch (Exception ex)
    {
        // VULNERABLE: All exceptions handled identically
        // FileNotFoundException, XmlException, SqlException, etc.
        // All receive only the same message output
        Console.WriteLine("Error: " + ex.Message);
    }
}
```

**Problems:**
1. File not found vs. XML parsing error handled identically
2. No specific recovery for each error type
3. Database errors not rolled back
4. No proper logging
5. Resources may leak

---

### ✅ Secure Code

#### Java - Specific Exception Handling

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureURLProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SecureURLProcessor.class);
    private static final int MAX_RETRIES = 3;

    public void processURL(String urlString) {
        BufferedReader reader = null;

        try {
            // 1. Create URL and connect
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            InputStream in = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            // 2. Read response
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // 3. Parse JSON
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(response.toString());

            // 4. Save data
            saveToDatabase(json);

            logger.info("Successfully processed URL: {}", urlString);

        } catch (MalformedURLException e) {
            // URL format error - no retry needed, input problem
            logger.error("Invalid URL format: {}", urlString, e);
            throw new IllegalArgumentException(
                "Invalid URL format: " + urlString, e);

        } catch (SocketTimeoutException e) {
            // Timeout - retryable error
            logger.warn("Connection timeout for URL: {}", urlString, e);
            retryWithBackoff(urlString);

        } catch (IOException e) {
            // Network error - retryable
            logger.error("Network error processing URL: {}", urlString, e);
            retryWithBackoff(urlString);

        } catch (ParseException e) {
            // JSON parsing error - data format problem
            logger.error("JSON parsing error for URL: {}", urlString, e);
            notifyDataTeam("Invalid JSON response from: " + urlString, e);
            throw new DataFormatException(
                "Response data format is invalid", e);

        } catch (SQLException e) {
            // Database error - requires separate handling
            logger.error("Database error while saving data from URL: {}",
                urlString, e);
            notifyDatabaseTeam("Database error in processURL", e);
            throw new DataAccessException(
                "An error occurred while saving data", e);

        } finally {
            // Resource cleanup
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing reader", e);
                }
            }
        }
    }

    private void retryWithBackoff(String urlString) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try {
                Thread.sleep((long) Math.pow(2, i) * 1000);  // Exponential backoff
                processURL(urlString);
                return;  // Success
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Retry interrupted", e);
                break;
            } catch (Exception e) {
                logger.warn("Retry {} failed for URL: {}", i + 1, urlString);
                if (i == MAX_RETRIES - 1) {
                    throw new NetworkException(
                        "Maximum retry count exceeded", e);
                }
            }
        }
    }

    private void notifyDataTeam(String message, Exception e) {
        logger.error("DATA TEAM ALERT: {}", message, e);
        // Send notification to data team
    }

    private void notifyDatabaseTeam(String message, Exception e) {
        logger.error("DBA TEAM ALERT: {}", message, e);
        // Send notification to database team
    }
}
```

**Security Features:**
1. Specific exception handling for each error type
2. Different recovery strategies (retry vs. fail-fast)
3. Proper logging with context
4. Team notifications for specific errors
5. Exponential backoff for retries
6. Resource cleanup in finally
7. Custom exceptions for clarity
8. Timeout configuration

---

#### Java - Try-with-Resources and Multi-Catch

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureDataProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SecureDataProcessor.class);

    public String fetchUserData(String userId) throws UserDataException {

        // Input validation
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }

        String apiUrl = buildApiUrl(userId);
        int retryCount = 0;

        while (retryCount < 3) {
            // Try-with-resources for automatic resource management
            try (InputStream is = fetchFromAPI(apiUrl);
                 InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader br = new BufferedReader(isr)) {

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    result.append(line);
                }

                logger.info("Successfully fetched data for user: {}", userId);
                return result.toString();

            } catch (MalformedURLException e) {
                // URL format error - no retry needed
                logger.error("Invalid API URL for user {}: {}", userId, apiUrl, e);
                throw new UserDataException(
                    "System configuration error. Please contact administrator.", e);

            } catch (SocketTimeoutException | ConnectException e) {
                // Network timeout or connection error - retry
                retryCount++;
                logger.warn("Network timeout for user {} (attempt {}/3): {}",
                    userId, retryCount, e.getMessage());

                if (retryCount >= 3) {
                    throw new UserDataException(
                        "Failed to connect to server. Please try again later.", e);
                }

                // Wait before retry (exponential backoff)
                try {
                    Thread.sleep(1000L * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new UserDataException("Request was interrupted.", ie);
                }

            } catch (FileNotFoundException e) {
                // 404 - User not found
                logger.warn("User not found: {}", userId, e);
                throw new UserDataException(
                    "User information not found.", e);

            } catch (IOException e) {
                // Other IO errors
                retryCount++;
                logger.error("IO error fetching data for user {} (attempt {}/3)",
                    userId, retryCount, e);

                if (retryCount >= 3) {
                    throw new UserDataException(
                        "An error occurred while fetching data.", e);
                }
            }
            // Try-with-resources automatically closes all resources
        }

        throw new UserDataException("Maximum retry count exceeded.");
    }

    private String buildApiUrl(String userId) {
        return String.format("https://api.example.com/users/%s", userId);
    }

    private InputStream fetchFromAPI(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            if (responseCode == 404) {
                throw new FileNotFoundException("User not found: " + apiUrl);
            }
            throw new IOException("HTTP error code: " + responseCode);
        }

        return conn.getInputStream();
    }
}

public class UserDataException extends Exception {
    public UserDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserDataException(String message) {
        super(message);
    }
}
```

**Security Features:**
1. Try-with-resources for automatic cleanup
2. Multi-catch for similar exceptions (Java 7+)
3. Retry logic for transient errors
4. Fail-fast for configuration errors
5. Exponential backoff
6. Different handling for different HTTP codes
7. Input validation
8. Proper logging

---

#### Java - Spring MVC Exception Handling

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

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Input validation error - WARN level
        logger.warn("ERROR [{}]: Invalid input - {}", correlationId, ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Bad request");
        body.put("message", ex.getMessage());
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.BAD_REQUEST.value());

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Resource not found - INFO level (expected error)
        logger.info("ERROR [{}]: User not found - {}", correlationId, ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("error", "User not found");
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccessException(
            DataAccessException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Database error - ERROR level, admin notification
        logger.error("ERROR [{}]: Database error - {}", correlationId, ex.getMessage(), ex);

        // Notify DBA team
        notifyDatabaseTeam("Database error", ex, correlationId);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "An error occurred while processing data");
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(NetworkException.class)
    public ResponseEntity<Map<String, Object>> handleNetworkException(
            NetworkException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Network error - WARN level (potentially transient)
        logger.warn("ERROR [{}]: Network error - {}", correlationId, ex.getMessage(), ex);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "An error occurred while communicating with external service");
        body.put("message", "Please try again later");
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());

        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(
            SecurityException ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Security error - ERROR level, security team notification
        logger.error("SECURITY [{}]: Security violation - {}",
            correlationId, ex.getMessage(), ex);

        // Notify security team
        notifySecurityTeam("Security violation detected", ex, correlationId);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "Access denied");
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.FORBIDDEN.value());

        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {

        String correlationId = UUID.randomUUID().toString();

        // Unexpected error - ERROR level, dev team notification
        logger.error("ERROR [{}]: Unexpected error - {}",
            correlationId, ex.getMessage(), ex);

        // Notify development team
        notifyDevelopmentTeam("Unexpected exception", ex, correlationId);

        Map<String, Object> body = new HashMap<>();
        body.put("error", "An error occurred while processing the request");
        body.put("correlationId", correlationId);
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void notifyDatabaseTeam(String message, Exception e, String correlationId) {
        logger.error("DBA ALERT [{}]: {}", correlationId, message, e);
        // Send email or notification
    }

    private void notifySecurityTeam(String message, Exception e, String correlationId) {
        logger.error("SECURITY ALERT [{}]: {}", correlationId, message, e);
        // Send urgent notification
    }

    private void notifyDevelopmentTeam(String message, Exception e, String correlationId) {
        logger.error("DEV ALERT [{}]: {}", correlationId, message, e);
        // Send notification
    }
}
```

**Security Features:**
1. Specific handlers for each exception type
2. Different log levels based on severity
3. Correlation IDs for tracking
4. Team notifications for critical errors
5. Appropriate HTTP status codes
6. Generic messages to users
7. Detailed logging server-side
8. Centralized exception handling

---

#### C# - Specific Exception Handling with Logging

```csharp
using System;
using System.IO;
using System.Xml;
using System.Data.SqlClient;
using log4net;

public class SecureFileProcessor
{
    private static readonly ILog _log = LogManager.GetLogger(typeof(SecureFileProcessor));

    public void ProcessFile(string filePath)
    {
        SqlConnection conn = null;
        SqlTransaction transaction = null;

        try
        {
            // 1. Read file
            if (string.IsNullOrEmpty(filePath))
            {
                throw new ArgumentException("File path cannot be null or empty");
            }

            string content = File.ReadAllText(filePath);
            _log.Info($"Successfully read file: {filePath}");

            // 2. Parse XML
            XmlDocument doc = new XmlDocument();
            doc.LoadXml(content);
            _log.Debug("XML parsed successfully");

            // 3. Save to database
            conn = new SqlConnection(connectionString);
            conn.Open();
            transaction = conn.BeginTransaction();

            SaveToDatabase(doc, conn, transaction);

            transaction.Commit();
            _log.Info($"Successfully processed file: {filePath}");
        }
        catch (ArgumentException ex)
        {
            // Input validation error - no retry needed
            _log.Warn($"Invalid input: {ex.Message}");
            throw new ProcessingException("Invalid input", ex);
        }
        catch (FileNotFoundException ex)
        {
            // File not found - check file path
            _log.Error($"File not found: {filePath}", ex);
            throw new ProcessingException($"File not found: {filePath}", ex);
        }
        catch (UnauthorizedAccessException ex)
        {
            // No file access permission - check permissions
            _log.Error($"Access denied to file: {filePath}", ex);
            throw new ProcessingException("No permission to access file", ex);
        }
        catch (XmlException ex)
        {
            // XML parsing error - file format problem
            _log.Error($"XML parsing error in file: {filePath}", ex);
            _log.Debug($"XML error detail: Line {ex.LineNumber}, Position {ex.LinePosition}");

            // Notify data team
            NotifyDataTeam($"Invalid XML in {filePath}", ex);

            throw new ProcessingException("XML format is invalid", ex);
        }
        catch (SqlException ex)
        {
            // Database error - rollback transaction
            _log.Error($"Database error processing file: {filePath}", ex);
            _log.Debug($"SQL Error: Number={ex.Number}, State={ex.State}");

            if (transaction != null)
            {
                try
                {
                    transaction.Rollback();
                    _log.Info("Transaction rolled back");
                }
                catch (Exception rollbackEx)
                {
                    _log.Error("Error rolling back transaction", rollbackEx);
                }
            }

            // Notify DBA team
            NotifyDatabaseTeam("Database error in ProcessFile", ex);

            throw new ProcessingException("An error occurred while saving to database", ex);
        }
        catch (IOException ex)
        {
            // File IO error
            _log.Error($"IO error reading file: {filePath}", ex);
            throw new ProcessingException("An error occurred while reading file", ex);
        }
        finally
        {
            // Resource cleanup
            if (transaction != null)
            {
                transaction.Dispose();
            }

            if (conn != null)
            {
                conn.Close();
                conn.Dispose();
            }
        }
    }

    private void NotifyDataTeam(string message, Exception ex)
    {
        _log.Error($"DATA TEAM ALERT: {message}", ex);
        // Send notification
    }

    private void NotifyDatabaseTeam(string message, Exception ex)
    {
        _log.Error($"DBA TEAM ALERT: {message}", ex);
        // Send notification
    }
}

public class ProcessingException : Exception
{
    public ProcessingException(string message, Exception innerException)
        : base(message, innerException)
    {
    }
}
```

**Security Features:**
1. Specific exception handling for each type
2. Different recovery strategies
3. Transaction rollback on database error
4. Team notifications for specific errors
5. Detailed logging with context
6. Resource cleanup in finally
7. Custom exceptions
8. Input validation

---

## References

### CWE (Common Weakness Enumeration)

1. **CWE-754: Improper Check for Unusual or Exceptional Conditions**
   MITRE, https://cwe.mitre.org/data/definitions/754.html

2. **CWE-396: Declaration of Catch for Generic Exception**
   MITRE, https://cwe.mitre.org/data/definitions/396.html

3. **CWE-397: Declaration of Throws for Generic Exception**
   MITRE, https://cwe.mitre.org/data/definitions/397.html

### CERT

4. **ERR07-J: Do not throw RuntimeException, Exception, or Throwable**
   CERT Oracle Secure Coding Standard for Java
   https://wiki.sei.cmu.edu/confluence/display/java/ERR07-J

5. **ERR08-J: Do not catch NullPointerException or any of its ancestors**
   CERT Oracle Secure Coding Standard for Java
   https://wiki.sei.cmu.edu/confluence/display/java/ERR08-J

### OWASP

6. **Error Handling Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Error_Handling_Cheat_Sheet.html

### Spring Framework

7. **Exception Handling in Spring MVC**
   Spring Documentation, https://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc

---

## Detection Patterns (Grep/Search)

Use these patterns to detect improper exception handling:

```bash
# Find generic Exception catching
grep -r "catch\s*(Exception" --include="*.java" .
grep -r "catch\s*(Throwable" --include="*.java" .

# Find catch blocks that only print
grep -A 3 "catch.*Exception" --include="*.java" . | grep "System.err.println\|System.out.println"

# Find catch without specific exception types
grep -r "catch\s*(\s*Exception\s*e\s*)" --include="*.java" .

# Find C# generic exception catching
grep -r "catch\s*(Exception" --include="*.cs" .
grep -r "catch\s*{" --include="*.cs" .

# Find missing specific exception handlers
grep -B 5 "catch.*Exception" --include="*.java" . | grep -v "IOException\|SQLException\|ParseException"

# Find Throwable catches (very dangerous)
grep -r "catch.*Throwable" --include="*.java" .
```

---

## Security Checklist

- [ ] Catch specific exception types, not generic `Exception`
- [ ] Never catch `Throwable` or `Error`
- [ ] Multiple catch blocks for different error types
- [ ] Specific recovery logic for each exception
- [ ] Proper logging with appropriate levels
- [ ] Retry logic for transient errors
- [ ] Fail-fast for configuration errors
- [ ] Transaction rollback on database errors
- [ ] Resource cleanup in finally or try-with-resources
- [ ] Team notifications for critical errors
- [ ] Correlation IDs for error tracking
- [ ] Different handling for different error severities
- [ ] Custom exceptions for business logic
- [ ] Multi-catch for similar exception handling (Java 7+)
- [ ] No `System.err.println` or `System.out.println`

---

## Exception Handling Best Practices

### 1. Exception Hierarchy (Order Matters)

```java
try {
    // Operations
} catch (FileNotFoundException e) {
    // Most specific first
} catch (IOException e) {
    // More general
} catch (Exception e) {
    // Most general last (if needed)
}
```

### 2. Multi-Catch for Similar Handling

```java
// Java 7+ multi-catch
try {
    // Operations
} catch (IOException | SQLException e) {
    logger.error("Data access error", e);
    throw new DataException("Data operation failed", e);
}
```

### 3. Try-with-Resources

```java
// Automatic resource management
try (Connection conn = getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    // Use resources
} catch (SQLException e) {
    logger.error("Database error", e);
}
// Resources automatically closed
```

### 4. Different Log Levels

```java
catch (IllegalArgumentException e) {
    logger.warn("Invalid input: {}", e.getMessage());  // WARN for client errors
}
catch (SQLException e) {
    logger.error("Database error", e);  // ERROR for server errors
}
catch (SecurityException e) {
    logger.error("SECURITY: Access violation", e);  // ERROR for security
}
```

---

## Common Mistakes

1. **Catching Generic Exception**
   ```java
   // DON'T
   try {
       complexOperation();
   } catch (Exception e) {
       System.err.println(e.getMessage());
   }

   // DO
   try {
       complexOperation();
   } catch (IOException e) {
       logger.error("IO error", e);
       retry();
   } catch (SQLException e) {
       logger.error("DB error", e);
       rollback();
   }
   ```

2. **Catching Throwable**
   ```java
   // DON'T: Catches Error too
   try {
       operation();
   } catch (Throwable t) {
       // Catches OutOfMemoryError, StackOverflowError
   }

   // DO: Catch specific exceptions
   try {
       operation();
   } catch (IOException | SQLException e) {
       logger.error("Operation failed", e);
   }
   ```

3. **Wrong Exception Order**
   ```java
   // DON'T: Won't compile (unreachable)
   try {
       operation();
   } catch (Exception e) {
       // Catches all
   } catch (IOException e) {
       // Unreachable code
   }

   // DO: Specific first
   try {
       operation();
   } catch (IOException e) {
       // Specific handling
   } catch (Exception e) {
       // Generic handling
   }
   ```

---

**Handle each exception type specifically - avoid generic Exception catching!**
