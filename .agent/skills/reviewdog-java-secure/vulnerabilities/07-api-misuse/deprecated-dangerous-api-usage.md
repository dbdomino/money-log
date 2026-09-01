# Deprecated/Dangerous API Usage

## Metadata
- **CWE ID**: CWE-676, CWE-242, CWE-246, CWE-382
- **Severity**: Medium to High
- **OWASP Category**: A06:2021 - Vulnerable and Outdated Components
- **Detection Difficulty**: Easy

## Description

Using deprecated, inherently dangerous, or platform-inappropriate APIs introduces security vulnerabilities and reliability issues. These APIs were deprecated because they have fundamental flaws that cannot be safely mitigated, such as buffer overflows, lack of bounds checking, or inappropriate behavior in managed environments. In enterprise contexts (J2EE, .NET), using low-level APIs that bypass framework controls (direct sockets, System.exit(), Application.Exit()) violates platform constraints and can cause system instability.

Common dangerous APIs include:
- **C/C++**: `gets()`, `strcpy()`, `strcat()`, `sprintf()` - no bounds checking, buffer overflows
- **C**: `getenv()` - environment variable manipulation risks
- **Java J2EE**: Direct socket usage (`new Socket()`) - bypasses container connection pooling
- **Java J2EE**: `System.exit()` - terminates entire JVM, not just application
- **C#**: `Application.Exit()` in exception handlers - abrupt termination

These APIs should be replaced with:
- Bounds-safe alternatives (`gets_s()`, `strncpy()`, `snprintf()`)
- Framework-provided mechanisms (J2EE connection pooling, proper resource cleanup)
- Graceful cleanup methods (`this.Close()`, proper exception handling)

## Vulnerable Code Examples

### Vulnerable C (Buffer Overflow APIs)
```c
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

void vulnerable_input_handling() {
    char str[100];

    // VULNERABLE: gets() has no bounds checking
    // Buffer overflow if input exceeds 100 bytes
    printf("Enter your name: ");
    gets(str);  // DANGEROUS: No buffer size parameter
    printf("Hello, %s\n", str);
}

void vulnerable_string_copy(char *user_input) {
    char buffer[50];

    // VULNERABLE: strcpy() doesn't check buffer size
    strcpy(buffer, user_input);  // Buffer overflow if input > 50 bytes

    // VULNERABLE: strcat() can overflow
    char dest[20] = "Hello, ";
    strcat(dest, user_input);  // Overflow if combined length > 20
}

void vulnerable_formatting(char *username, int age) {
    char output[100];

    // VULNERABLE: sprintf() doesn't check bounds
    sprintf(output, "User: %s, Age: %d, Status: Active", username, age);
    // Overflow if username is very long

    printf("%s\n", output);
}

void vulnerable_env_var() {
    // VULNERABLE: Environment variables can be manipulated by attackers
    char *path = getenv("PATH");

    if (path != NULL) {
        char command[256];
        // VULNERABLE: Using environment variable in system command
        sprintf(command, "%s/important_program", path);
        system(command);  // Can execute attacker-controlled programs
    }
}
```

### Vulnerable Java (J2EE Direct Socket Usage)
```java
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class DataFetchServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String externalHost = request.getParameter("host");

        // VULNERABLE: Direct socket creation in J2EE servlet
        // Bypasses container-managed connection pooling
        // No connection timeout, resource management, or pooling
        Socket socket = null;

        try {
            // VULNERABLE: Creates socket directly
            socket = new Socket(externalHost, 8080);

            InputStream in = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }

        } catch (IOException e) {
            throw new ServletException("Connection failed", e);
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
}
```

### Vulnerable Java (System.exit in J2EE)
```java
import javax.servlet.http.*;
import java.io.*;

public class ShutdownServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try {
            if ("shutdown".equals(action)) {
                // Perform cleanup
                cleanup();

                // VULNERABLE: System.exit() in J2EE environment
                // Terminates entire JVM, affecting ALL applications in container
                // Violates J2EE specification
                System.exit(0);  // DANGEROUS: Kills entire application server
            }

        } catch (Exception e) {
            // VULNERABLE: Exit on exception
            System.err.println("Error: " + e.getMessage());
            System.exit(1);  // DANGEROUS: Terminates JVM on error
        }
    }

    private void cleanup() {
        // Cleanup logic
        System.out.println("Performing cleanup...");
    }
}
```

### Vulnerable C# (Application.Exit)
```csharp
using System;
using System.Windows.Forms;

public class DataProcessor : Form {

    public void ProcessData(string filePath) {
        try {
            // Process file
            string data = System.IO.File.ReadAllText(filePath);
            PerformProcessing(data);

        } catch (Exception ex) {
            // VULNERABLE: Application.Exit() in exception handler
            // Abruptly terminates application without proper cleanup
            MessageBox.Show("Error: " + ex.Message);
            Application.Exit();  // DANGEROUS: No graceful cleanup
        }
    }

    private void PerformProcessing(string data) {
        if (data == null || data.Length == 0) {
            // VULNERABLE: Exit on validation failure
            MessageBox.Show("Invalid data");
            Application.Exit();  // DANGEROUS
        }

        // Process data
        Console.WriteLine("Processing: " + data);
    }
}
```

### Vulnerable C (Multiple Dangerous APIs)
```c
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void process_user_data(char *username, char *password) {
    char user_buffer[64];
    char pass_buffer[64];
    char combined[128];

    // VULNERABLE: strcpy with no bounds checking
    strcpy(user_buffer, username);
    strcpy(pass_buffer, password);

    // VULNERABLE: strcat can overflow
    strcpy(combined, "User: ");
    strcat(combined, user_buffer);
    strcat(combined, ", Pass: ");
    strcat(combined, pass_buffer);

    // VULNERABLE: gets() for additional input
    char comments[100];
    printf("Comments: ");
    gets(comments);

    // VULNERABLE: sprintf without bounds
    char log[200];
    sprintf(log, "Login attempt - %s - %s", combined, comments);

    printf("%s\n", log);
}
```

## Secure Code Examples

### Secure C (Bounds-Safe Alternatives)
```c
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

// SECURE: Use gets_s() instead of gets()
void secure_input_handling() {
    char str[100];

    printf("Enter your name: ");

    // SECURE: gets_s() requires buffer size
    if (gets_s(str, sizeof(str)) != NULL) {
        printf("Hello, %s\n", str);
    } else {
        fprintf(stderr, "Input error or buffer overflow prevented\n");
    }

    // Alternative: Use fgets()
    if (fgets(str, sizeof(str), stdin) != NULL) {
        // Remove newline if present
        size_t len = strlen(str);
        if (len > 0 && str[len-1] == '\n') {
            str[len-1] = '\0';
        }
        printf("Hello, %s\n", str);
    }
}

// SECURE: Use strncpy() instead of strcpy()
void secure_string_copy(const char *user_input) {
    char buffer[50];

    // SECURE: strncpy() with size limit
    strncpy(buffer, user_input, sizeof(buffer) - 1);
    buffer[sizeof(buffer) - 1] = '\0';  // Ensure null termination

    // SECURE: strncat() with size limit
    char dest[20] = "Hello, ";
    size_t remaining = sizeof(dest) - strlen(dest) - 1;
    strncat(dest, user_input, remaining);
    dest[sizeof(dest) - 1] = '\0';  // Ensure null termination
}

// SECURE: Use snprintf() instead of sprintf()
void secure_formatting(const char *username, int age) {
    char output[100];

    // SECURE: snprintf() with buffer size
    int written = snprintf(output, sizeof(output),
                          "User: %s, Age: %d, Status: Active",
                          username, age);

    if (written < 0 || written >= sizeof(output)) {
        fprintf(stderr, "Format error or truncation occurred\n");
        return;
    }

    printf("%s\n", output);
}

// SECURE: Validate and sanitize environment variables
void secure_env_var() {
    const char *path = getenv("PATH");

    if (path == NULL) {
        fprintf(stderr, "PATH not set\n");
        return;
    }

    // SECURE: Use absolute path, don't trust PATH
    const char *trusted_program = "/usr/local/bin/important_program";

    // Validate the program exists and has correct permissions
    if (access(trusted_program, X_OK) == 0) {
        system(trusted_program);
    } else {
        fprintf(stderr, "Trusted program not found or not executable\n");
    }
}

// SECURE: Complete example with all bounds checking
void secure_process_user_data(const char *username, const char *password) {
    char user_buffer[64];
    char pass_buffer[64];
    char combined[128];

    // SECURE: strncpy with null termination
    strncpy(user_buffer, username, sizeof(user_buffer) - 1);
    user_buffer[sizeof(user_buffer) - 1] = '\0';

    strncpy(pass_buffer, password, sizeof(pass_buffer) - 1);
    pass_buffer[sizeof(pass_buffer) - 1] = '\0';

    // SECURE: snprintf for combining strings
    snprintf(combined, sizeof(combined), "User: %s, Pass: %s",
             user_buffer, pass_buffer);

    // SECURE: fgets instead of gets
    char comments[100];
    printf("Comments: ");
    if (fgets(comments, sizeof(comments), stdin) != NULL) {
        // Remove newline
        size_t len = strlen(comments);
        if (len > 0 && comments[len-1] == '\n') {
            comments[len-1] = '\0';
        }
    }

    // SECURE: snprintf with size
    char log[200];
    snprintf(log, sizeof(log), "Login attempt - %s - %s", combined, comments);

    printf("%s\n", log);
}
```

### Secure Java (J2EE URL Connection)
```java
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class SecureDataFetchServlet extends HttpServlet {

    private static final int TIMEOUT = 5000;  // 5 seconds
    private static final int MAX_CONTENT_LENGTH = 1048576;  // 1MB

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String urlParam = request.getParameter("url");

        // Validate URL
        if (urlParam == null || urlParam.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL required");
            return;
        }

        try {
            // SECURE: Use URLConnection instead of direct Socket
            URL url = new URL(urlParam);

            // Validate URL scheme (whitelist)
            if (!url.getProtocol().equals("http") && !url.getProtocol().equals("https")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid protocol");
                return;
            }

            // SECURE: Framework-managed connection with timeout
            URLConnection urlConn = url.openConnection();
            urlConn.setConnectTimeout(TIMEOUT);
            urlConn.setReadTimeout(TIMEOUT);

            // Check content length
            int contentLength = urlConn.getContentLength();
            if (contentLength > MAX_CONTENT_LENGTH) {
                response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,
                                 "Content too large");
                return;
            }

            // Read response
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(urlConn.getInputStream()))) {

                String line;
                int totalBytes = 0;

                while ((line = reader.readLine()) != null) {
                    totalBytes += line.length();

                    if (totalBytes > MAX_CONTENT_LENGTH) {
                        throw new IOException("Content length exceeded limit");
                    }

                    out.println(escapeHtml(line));
                }
            }

        } catch (MalformedURLException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid URL");
        } catch (IOException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                             "Failed to fetch data");
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
```

### Secure Java (Proper J2EE Shutdown)
```java
import javax.servlet.http.*;
import java.io.*;
import java.util.logging.*;

public class SecureShutdownServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SecureShutdownServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String action = request.getParameter("action");

        try {
            if ("shutdown".equals(action)) {
                // SECURE: Perform cleanup
                cleanup();

                // SECURE: Close servlet resources, not entire JVM
                logger.info("Servlet shutdown requested");

                // Close this servlet's resources
                destroy();

                // Return success response
                response.setContentType("application/json");
                response.getWriter().write("{\"status\": \"shutdown_initiated\"}");

                // Let container manage lifecycle
                // Do NOT call System.exit()
            }

        } catch (Exception e) {
            // SECURE: Log error and return error response
            logger.log(Level.SEVERE, "Shutdown error", e);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\": \"error\", \"message\": \"" +
                                      e.getMessage() + "\"}");

            // Do NOT call System.exit()
        }
    }

    private void cleanup() {
        logger.info("Performing cleanup...");
        // Cleanup resources, close connections, etc.
    }

    @Override
    public void destroy() {
        // SECURE: Container-managed lifecycle
        logger.info("Servlet destroying - cleaning up resources");
        cleanup();
        super.destroy();
    }
}
```

### Secure C# (Graceful Error Handling)
```csharp
using System;
using System.IO;
using System.Windows.Forms;

public class SecureDataProcessor : Form {
    private static readonly log4net.ILog log =
        log4net.LogManager.GetLogger(typeof(SecureDataProcessor));

    public void ProcessData(string filePath) {
        try {
            // Validate input
            if (string.IsNullOrWhiteSpace(filePath)) {
                ShowError("File path is required");
                return;
            }

            if (!File.Exists(filePath)) {
                ShowError("File not found: " + filePath);
                return;
            }

            // Process file
            string data = File.ReadAllText(filePath);
            PerformProcessing(data);

        } catch (IOException ioEx) {
            // SECURE: Log and handle gracefully
            log.Error("File I/O error", ioEx);
            ShowError("Failed to read file: " + ioEx.Message);

            // SECURE: Use this.Close() instead of Application.Exit()
            this.Close();

        } catch (Exception ex) {
            // SECURE: Log error and close gracefully
            log.Error("Processing error", ex);
            ShowError("Error processing data: " + ex.Message);

            // SECURE: Close form, not entire application
            this.Close();
        }
    }

    private void PerformProcessing(string data) {
        if (string.IsNullOrEmpty(data)) {
            // SECURE: Throw exception for validation error
            throw new ArgumentException("Data cannot be empty");
        }

        // Process data
        Console.WriteLine("Processing: " + data.Substring(0, Math.Min(50, data.Length)));
    }

    private void ShowError(string message) {
        MessageBox.Show(message, "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
    }

    protected override void OnFormClosing(FormClosingEventArgs e) {
        // SECURE: Cleanup before closing
        try {
            // Release resources
            log.Info("Form closing - cleaning up resources");
            // Cleanup code here
        } catch (Exception ex) {
            log.Error("Cleanup error", ex);
        }

        base.OnFormClosing(e);
    }
}
```

### Secure Java (HttpClient for External Connections)
```java
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;
import org.apache.http.util.*;
import java.io.*;

public class SecureHttpClient {
    private static final int TIMEOUT = 5000;
    private static final int MAX_CONTENT_LENGTH = 1048576;

    public String fetchData(String url) throws IOException {
        // SECURE: Use HttpClient with connection pooling
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet(url);

            // Set timeouts
            RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .build();
            httpGet.setConfig(config);

            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new IOException("HTTP error: " + statusCode);
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new IOException("No response entity");
                }

                // Check content length
                long contentLength = entity.getContentLength();
                if (contentLength > MAX_CONTENT_LENGTH) {
                    throw new IOException("Content too large");
                }

                // Read response
                return EntityUtils.toString(entity);
            }
        }
    }
}
```

## Detection Methods

### Static Analysis
```bash
# Find dangerous C functions
grep -r "gets(\|strcpy(\|strcat(\|sprintf(\|getenv(" \
  --include="*.c" --include="*.cpp" .

# Find J2EE bad practices
grep -r "new Socket(\|System\.exit(" --include="*.java" .

# Find Application.Exit in C#
grep -r "Application\.Exit(" --include="*.cs" .

# Find deprecated APIs
grep -r "@Deprecated\|[Obsolete]" --include="*.java" --include="*.cs" .
```

### Compiler Warnings
```bash
# GCC warnings for dangerous functions
gcc -Wall -Wextra -Wformat-security -Wdeprecated-declarations source.c

# Java deprecation warnings
javac -Xlint:deprecation *.java

# C# warnings
csc /warn:4 /warnaserror *.cs
```

### SonarQube Rules
```xml
<!-- Dangerous C functions -->
<rule>
    <key>c:S5827</key>
    <name>gets() should not be used</name>
    <severity>CRITICAL</severity>
</rule>

<!-- J2EE bad practices -->
<rule>
    <key>java:S2151</key>
    <name>System.exit() should not be called in J2EE</name>
    <severity>BLOCKER</severity>
</rule>

<rule>
    <key>java:S2112</key>
    <name>Direct socket creation should not be used in J2EE</name>
    <severity>MAJOR</severity>
</rule>
```

## References

### CWE
- [CWE-676: Use of Potentially Dangerous Function](https://cwe.mitre.org/data/definitions/676.html)
- [CWE-242: Use of Inherently Dangerous Function](https://cwe.mitre.org/data/definitions/242.html)
- [CWE-246: J2EE Bad Practices: Direct Use of Sockets](https://cwe.mitre.org/data/definitions/246.html)
- [CWE-382: J2EE Bad Practices: Use of System.exit()](https://cwe.mitre.org/data/definitions/382.html)

### CERT Coding Standards
- [MSC24-C: Do not use deprecated or obsolescent functions](https://wiki.sei.cmu.edu/confluence/display/c/MSC24-C)
- [STR07-C: Use the bounds-checking interfaces for string manipulation](https://wiki.sei.cmu.edu/confluence/display/c/STR07-C)

### OWASP
- [OWASP Top 10 2021 A06:2021 - Vulnerable and Outdated Components](https://owasp.org/Top10/A06_2021-Vulnerable_and_Outdated_Components/)

### Additional Resources
- ISO/IEC TS 17961: C Secure Coding Rules
- [Microsoft Security Development Lifecycle (SDL) Banned Function Calls](https://docs.microsoft.com/en-us/previous-versions/bb288454(v=msdn.10))
- Java EE Specification - Platform Constraints

## Security Checklist

### For Developers
- [ ] Replace gets() with gets_s() or fgets()
- [ ] Replace strcpy() with strncpy()
- [ ] Replace strcat() with strncat()
- [ ] Replace sprintf() with snprintf()
- [ ] Avoid System.exit() in J2EE applications
- [ ] Use URLConnection instead of direct Socket in J2EE
- [ ] Replace Application.Exit() with this.Close() in C#
- [ ] Enable and fix all deprecation warnings
- [ ] Use static analysis tools to detect dangerous APIs

### For Code Reviewers
- [ ] Verify no banned functions in C/C++ code
- [ ] Check for proper buffer size parameters
- [ ] Confirm no System.exit() in servlets
- [ ] Validate proper use of framework-provided APIs
- [ ] Review exception handlers for Application.Exit()
- [ ] Check that deprecated APIs are not used
- [ ] Verify proper resource cleanup mechanisms

### For Security Auditors
- [ ] Scan codebase for deprecated/dangerous APIs
- [ ] Test buffer handling for overflow vulnerabilities
- [ ] Verify J2EE applications follow platform constraints
- [ ] Check for proper error handling without abrupt termination
- [ ] Validate connection management in enterprise applications
- [ ] Review third-party libraries for deprecated API usage
- [ ] Test application stability under error conditions
