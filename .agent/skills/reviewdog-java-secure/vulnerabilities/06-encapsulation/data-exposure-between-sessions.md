# Data Exposure Between Sessions

## Metadata
- **CWE ID**: CWE-488, CWE-543
- **Severity**: High
- **OWASP Category**: A01:2021 - Broken Access Control
- **Detection Difficulty**: Medium

## Description

Data exposure between sessions occurs when singleton patterns or shared class-level variables are used inappropriately in multi-threaded environments, particularly in servlet containers. When member variables are declared at the servlet class level (using `<%!` in JSP or as instance variables in servlets), they are shared across all concurrent requests from different users, leading to sensitive data leakage between user sessions.

This vulnerability is particularly dangerous because:
- User A's data can be accessed by User B
- Session isolation is completely broken
- Authentication and authorization mechanisms are bypassed
- The application appears to work correctly in single-user testing

## Vulnerable Code Examples

### Vulnerable JSP (Servlet Member Variables)
```jsp
<%@ page contentType="text/html; charset=UTF-8" %>
<%!
    // VULNERABLE: These are servlet member variables shared across all requests
    String name;
    String email;
    String sessionID;
%>
<%
    // Data from current user's request
    name = request.getParameter("name");
    email = request.getParameter("email");
    sessionID = session.getId();

    // When multiple users access simultaneously:
    // User A's data can be overwritten by User B's data
    // User A might see User B's name and email
%>
<html>
<body>
    <h2>User Information</h2>
    <p>Name: <%= name %></p>
    <p>Email: <%= email %></p>
    <p>Session: <%= sessionID %></p>
</body>
</html>
```

### Vulnerable Servlet (Instance Variables)
```java
import javax.servlet.http.*;
import java.io.*;

public class UserInfoServlet extends HttpServlet {
    // VULNERABLE: Instance variables shared across all requests
    private String userName;
    private String userEmail;
    private String userRole;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Multiple threads can access and modify these variables simultaneously
        userName = request.getParameter("userName");
        userEmail = request.getParameter("userEmail");
        userRole = request.getParameter("userRole");

        // Race condition: values might change between setting and using
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        out.println("<html><body>");
        out.println("<h2>Welcome, " + userName + "</h2>");
        out.println("<p>Email: " + userEmail + "</p>");
        out.println("<p>Role: " + userRole + "</p>");
        out.println("</body></html>");
    }
}
```

### Vulnerable Singleton in Multi-threaded Environment
```java
public class UserManager {
    // VULNERABLE: Singleton without proper synchronization
    private static UserManager instance;
    private String currentUser;
    private String currentSession;

    private UserManager() {}

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public void setCurrentUser(String user) {
        this.currentUser = user;
        this.currentSession = generateSession();
    }

    public String getCurrentUser() {
        return currentUser;  // Returns data from last request, not current user
    }

    private String generateSession() {
        return "SESSION-" + System.currentTimeMillis();
    }
}

// Usage in Servlet
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");

        // VULNERABLE: Shared singleton causes data leakage
        UserManager manager = UserManager.getInstance();
        manager.setCurrentUser(username);

        // User A might see User B's username
        response.getWriter().println("Logged in as: " + manager.getCurrentUser());
    }
}
```

## Secure Code Examples

### Secure JSP (Local Variables)
```jsp
<%@ page contentType="text/html; charset=UTF-8" %>
<%
    // SECURE: Local variables in scriptlet scope - each request has its own copy
    String name = request.getParameter("name");
    String email = request.getParameter("email");
    String sessionID = session.getId();

    // Each request is properly isolated
%>
<html>
<body>
    <h2>User Information</h2>
    <p>Name: <%= name %></p>
    <p>Email: <%= email %></p>
    <p>Session: <%= sessionID %></p>
</body>
</html>
```

### Secure Servlet (Local Variables)
```java
import javax.servlet.http.*;
import java.io.*;

public class UserInfoServlet extends HttpServlet {
    // SECURE: No instance variables for request-specific data

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // SECURE: Local variables - each thread has its own copy
        String userName = request.getParameter("userName");
        String userEmail = request.getParameter("userEmail");
        String userRole = request.getParameter("userRole");

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            out.println("<html><body>");
            out.println("<h2>Welcome, " + escapeHtml(userName) + "</h2>");
            out.println("<p>Email: " + escapeHtml(userEmail) + "</p>");
            out.println("<p>Role: " + escapeHtml(userRole) + "</p>");
            out.println("</body></html>");
        } finally {
            out.close();
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

### Secure Controller Pattern (MVC)
```java
// Model
public class UserInfo {
    private final String userName;
    private final String userEmail;
    private final String userRole;

    public UserInfo(String userName, String userEmail, String userRole) {
        this.userName = userName;
        this.userEmail = userEmail;
        this.userRole = userRole;
    }

    // Getters only - immutable object
    public String getUserName() { return userName; }
    public String getUserEmail() { return userEmail; }
    public String getUserRole() { return userRole; }
}

// Controller
import javax.servlet.http.*;
import java.io.*;

public class UserInfoController extends HttpServlet {
    // SECURE: Only constants or thread-safe objects as instance variables
    private static final String VIEW_PATH = "/WEB-INF/views/userInfo.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // SECURE: Request-scoped data using request attributes
        String userName = request.getParameter("userName");
        String userEmail = request.getParameter("userEmail");
        String userRole = request.getParameter("userRole");

        // Create immutable object for this request
        UserInfo userInfo = new UserInfo(userName, userEmail, userRole);

        // Store in request scope (not shared between requests)
        request.setAttribute("userInfo", userInfo);

        // Forward to view
        request.getRequestDispatcher(VIEW_PATH).forward(request, response);
    }
}
```

### Secure Thread-Safe Singleton
```java
public class UserSessionManager {
    // SECURE: Thread-safe singleton using enum
    private static final UserSessionManager INSTANCE = new UserSessionManager();

    // Thread-local storage for per-request data
    private final ThreadLocal<String> currentUser = new ThreadLocal<>();
    private final ThreadLocal<String> currentSession = new ThreadLocal<>();

    private UserSessionManager() {}

    public static UserSessionManager getInstance() {
        return INSTANCE;
    }

    public void setCurrentUser(String user) {
        currentUser.set(user);
        currentSession.set(generateSession());
    }

    public String getCurrentUser() {
        return currentUser.get();
    }

    public String getCurrentSession() {
        return currentSession.get();
    }

    public void clearCurrentUser() {
        currentUser.remove();
        currentSession.remove();
    }

    private String generateSession() {
        return "SESSION-" + Thread.currentThread().getId() + "-" + System.currentTimeMillis();
    }
}

// Usage in Servlet
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");

        UserSessionManager manager = UserSessionManager.getInstance();
        try {
            manager.setCurrentUser(username);
            response.getWriter().println("Logged in as: " + manager.getCurrentUser());
        } finally {
            // Always clean up ThreadLocal to prevent memory leaks
            manager.clearCurrentUser();
        }
    }
}
```

## Detection Methods

### Static Analysis
```bash
# Find JSP files with servlet member variable declarations
grep -r "<%!" --include="*.jsp" .

# Find servlet instance variables that might be shared
grep -r "private String\|private int\|private Object" --include="*Servlet.java" .

# Find singleton patterns without ThreadLocal
grep -r "private static.*getInstance" --include="*.java" .
```

### Code Review Checklist
- [ ] No instance variables in servlets for request-specific data
- [ ] JSP files use `<%` (scriptlet) not `<%!` (declaration) for request data
- [ ] Singleton patterns use ThreadLocal for thread-specific data
- [ ] Request attributes used instead of session/application scope when appropriate
- [ ] Thread-safe collections used for shared data structures
- [ ] Proper synchronization for genuinely shared mutable state

### Runtime Detection
```java
// Test for data leakage in concurrent requests
import java.util.concurrent.*;
import java.net.*;

public class SessionLeakageTest {
    private static final String SERVLET_URL = "http://localhost:8080/userInfo";
    private static final int THREAD_COUNT = 10;

    public static void testConcurrentRequests() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    String response = sendRequest("User" + userId, "user" + userId + "@test.com");

                    // Verify response contains correct user data
                    if (!response.contains("User" + userId)) {
                        System.err.println("DATA LEAKAGE DETECTED for User" + userId);
                        System.err.println("Response: " + response);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
    }

    private static String sendRequest(String name, String email) throws Exception {
        URL url = new URL(SERVLET_URL + "?userName=" + name + "&userEmail=" + email);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
```

## References

### CWE
- [CWE-488: Exposure of Data Element to Wrong Session](https://cwe.mitre.org/data/definitions/488.html)
- [CWE-543: Use of Singleton Pattern Without Synchronization in a Multithreaded Context](https://cwe.mitre.org/data/definitions/543.html)

### OWASP
- [OWASP Top 10 2021 A01:2021 - Broken Access Control](https://owasp.org/Top10/A01_2021-Broken_Access_Control/)
- [OWASP Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html)

### Additional Resources
- Java Servlet Specification - Thread Safety
- [Oracle: Web Tier Security Guidelines](https://docs.oracle.com/javaee/7/tutorial/security-webtier.htm)
- [CERT Oracle Secure Coding Standard for Java](https://wiki.sei.cmu.edu/confluence/display/java/SEI+CERT+Oracle+Coding+Standard+for+Java)

## Security Checklist

### For Developers
- [ ] Avoid instance variables in servlets for request-specific data
- [ ] Use local variables within doGet/doPost methods
- [ ] Store request-specific data in request attributes, not instance variables
- [ ] Use ThreadLocal for thread-specific data in singletons
- [ ] Clean up ThreadLocal variables to prevent memory leaks
- [ ] Prefer stateless design for servlet components
- [ ] Use immutable objects for shared data
- [ ] Test with concurrent requests to verify isolation

### For Code Reviewers
- [ ] Verify no `<%!` declarations in JSP files for request data
- [ ] Check servlets have no mutable instance variables for request data
- [ ] Confirm singleton patterns are thread-safe
- [ ] Validate proper use of request/session/application scopes
- [ ] Ensure ThreadLocal cleanup in finally blocks
- [ ] Review synchronization blocks for correctness
- [ ] Verify thread-safety of shared collections

### For Testers
- [ ] Perform concurrent user testing
- [ ] Verify user data isolation between sessions
- [ ] Test with load testing tools (JMeter, Gatling)
- [ ] Monitor for data leakage in logs
- [ ] Check session isolation under high load
- [ ] Validate proper session timeout behavior
