# Missing Authentication for Critical Function (CWE-306)

**Severity**: 🔴 CRITICAL
**Category**: Security Features
**OWASP Top 10**: A07:2021 – Identification and Authentication Failures

---

## Overview

### Attack Description

Access to sensitive information or critical functions must be restricted to verified users only. Missing authentication for critical functions occurs when an application does not verify user identity before granting access to sensitive operations or resources. If user authentication is missing or insufficient, attackers can access sensitive information or functions without being authenticated, allowing them to bypass authentication and access restricted functionality without proper credentials.

### Impact

**Potential consequences:**
- Unauthorized access to sensitive data
- Privilege escalation
- Data manipulation or deletion
- Account takeover
- Business logic bypass
- Compliance violations (GDPR, PCI-DSS)
- Financial loss

---

## Security Measures

### Key Principles

Access to sensitive information must only be allowed after completing proper authentication procedures.

**Primary Defenses:**

1. **Mandatory Authentication**
   - Require authentication for all sensitive operations
   - Use session management to verify user identity
   - Redirect unauthenticated users to login page
   - Never rely on client-side checks alone

2. **Session Validation**
   - Check session validity on every request
   - Validate session tokens server-side
   - Use framework session management
   - Implement session timeout

3. **Role-Based Access Control (RBAC)**
   - Define user roles and permissions
   - Check both authentication AND authorization
   - Implement least privilege principle
   - Use framework security features

4. **Secure Session Management**
   - Use secure session IDs (random, unpredictable)
   - Set HttpOnly and Secure flags on cookies
   - Implement CSRF protection
   - Invalidate sessions on logout

5. **Multi-Factor Authentication (MFA)**
   - Implement MFA for critical operations
   - Use time-based OTP (TOTP)
   - Support hardware tokens or biometrics
   - Require re-authentication for sensitive actions

---

## Code Examples

### Attack Scenario

The following example is unsafe because it allows access to critical pages (e.g., admin pages) without verifying user authentication.

**Attack:**
```
# Direct URL access without authentication
http://example.com/admin/deleteUser.jsp?userId=123

# Attacker can access admin functions without login
```

---

### ❌ Vulnerable Code

#### Java JSP - No Authentication Check

```jsp
<!-- Allows access to critical page without verifying user authentication -->
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.*" %>
<html>
<head>
    <title>Delete User</title>
</head>
<body>
<%
    // Performs deletion logic directly without authentication check
    String userId = request.getParameter("userId");

    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    PreparedStatement pstmt = conn.prepareStatement(
        "DELETE FROM users WHERE user_id = ?"
    );
    pstmt.setString(1, userId);
    pstmt.executeUpdate();

    out.println("User has been deleted.");
%>
</body>
</html>
```

**Problems:**
1. No session check
2. No authentication verification
3. No authorization check (admin role)
4. Direct access via URL possible
5. Vulnerable to direct object reference

**Attack Example:**
```bash
# Anyone can delete users by accessing the URL directly
curl "http://example.com/admin/deleteUser.jsp?userId=123"
```

---

#### Java Servlet - Missing Session Validation

```java
@WebServlet("/admin/deleteUser")
public class DeleteUserServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Processes directly without session check
        String userId = request.getParameter("userId");

        try {
            UserDAO dao = new UserDAO();
            dao.deleteUser(userId);

            response.getWriter().println("User deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Problems:**
1. No session existence check
2. No user authentication verification
3. No role/permission check
4. Can be accessed by anyone

---

### ✅ Secure Code

#### Java JSP - Session-Based Authentication

```jsp
<!-- Verifies user authentication through session -->
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.*" %>
<html>
<head>
    <title>Delete User</title>
</head>
<body>
<%
    // 1. Check user information from session
    HttpSession session = request.getSession(false);

    if (session == null || session.getAttribute("userId") == null) {
        // Redirect to login page if not authenticated
        response.sendRedirect("/login.jsp");
        return;
    }

    // 2. Verify admin privileges
    String userRole = (String) session.getAttribute("userRole");
    if (!"ADMIN".equals(userRole)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
            "Admin privileges required.");
        return;
    }

    // 3. Perform deletion logic after authentication is confirmed
    String userId = request.getParameter("userId");

    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    PreparedStatement pstmt = conn.prepareStatement(
        "DELETE FROM users WHERE user_id = ?"
    );
    pstmt.setString(1, userId);
    pstmt.executeUpdate();

    out.println("User has been deleted.");
%>
</body>
</html>
```

**Security Features:**
1. Session existence check (`getSession(false)`)
2. User ID verification from session
3. Role-based authorization check
4. Redirect to login if not authenticated
5. Forbidden error if not authorized

---

#### ✅ Better Practice - Servlet with Complete Authentication

```java
@WebServlet("/admin/deleteUser")
public class SecureDeleteUserServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Session validation
        HttpSession session = request.getSession(false);

        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Authentication required");
            return;
        }

        // 2. User authentication check
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("User not authenticated");
            return;
        }

        // 3. Authorization check (admin role)
        String userRole = (String) session.getAttribute("userRole");
        if (!"ADMIN".equals(userRole)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().println("Admin privileges required");
            return;
        }

        // 4. CSRF token validation
        String sessionToken = (String) session.getAttribute("csrfToken");
        String requestToken = request.getParameter("csrfToken");

        if (sessionToken == null || !sessionToken.equals(requestToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().println("Invalid CSRF token");
            return;
        }

        // 5. Perform deletion with validated user
        String targetUserId = request.getParameter("userId");

        try {
            UserDAO dao = new UserDAO();

            // Log admin action
            AuditLogger.log(userId, "DELETE_USER", targetUserId);

            dao.deleteUser(targetUserId);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("User deleted successfully");

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error deleting user");
            // Log error securely (don't expose to user)
            logger.error("Failed to delete user", e);
        }
    }
}
```

**Security Features:**
1. Session null check with `getSession(false)`
2. User ID validation from session
3. Role-based authorization (ADMIN check)
4. CSRF token validation
5. Audit logging for accountability
6. Proper error handling
7. HTTP status codes (401, 403, 500)

---

#### ✅ Best Practice - Spring Security Filter

```java
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authentication filter for critical admin operations
 */
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get authentication from SecurityContext
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();

        // 2. Check if user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Authentication required\"}"
            );
            return;
        }

        // 3. Check if user has ADMIN role
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Admin privileges required\"}"
            );
            return;
        }

        // 4. Check session validity
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Invalid session\"}"
            );
            return;
        }

        // 5. Check session timeout
        long lastAccessTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        long maxInactiveInterval = session.getMaxInactiveInterval() * 1000;

        if (currentTime - lastAccessTime > maxInactiveInterval) {
            session.invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\": \"Session expired\"}"
            );
            return;
        }

        // 6. Authentication successful - continue
        filterChain.doFilter(request, response);
    }
}
```

**Spring Security Configuration:**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // Public endpoints
                .antMatchers("/", "/login", "/register").permitAll()

                // Admin endpoints require ADMIN role
                .antMatchers("/admin/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard")
                .failureUrl("/login?error=true")
            .and()
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?expired=true")
            .and()
            .and()
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .headers()
                .frameOptions().deny()
                .xssProtection().block(true)
                .contentSecurityPolicy("default-src 'self'");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Controller with Authentication:**
```java
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @PostMapping("/deleteUser")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(
            @RequestParam String userId,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            // Current user already authenticated by Spring Security
            String adminUsername = currentUser.getUsername();

            // Log the action
            auditLog.info("Admin {} deleting user {}",
                adminUsername, userId);

            // Perform deletion
            userService.deleteUser(userId);

            return ResponseEntity.ok()
                .body(Map.of("message", "User deleted successfully"));

        } catch (Exception e) {
            logger.error("Error deleting user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete user"));
        }
    }
}
```

**Security Features:**
1. Spring Security manages authentication
2. `@PreAuthorize` for method-level security
3. Role-based access control (RBAC)
4. Session management with timeout
5. CSRF protection enabled
6. Security headers configured
7. Audit logging with authenticated user
8. `@AuthenticationPrincipal` injects authenticated user
9. Password encryption with BCrypt
10. Session invalidation on logout

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-306: Missing Authentication for Critical Function**
   MITRE, https://cwe.mitre.org/data/definitions/306.html

② **CWE-862: Missing Authorization**
   MITRE, https://cwe.mitre.org/data/definitions/862.html

### OWASP

③ **A07:2021 – Identification and Authentication Failures**
   OWASP Top 10, https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/

④ **Authentication Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html

⑤ **Session Management Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find missing authentication checks
grep -r "doGet\|doPost" --include="*.java" . | grep -v "session"
grep -r "request.getParameter" --include="*.jsp" . | grep -v "session"

# Find direct database operations without auth
grep -r "DELETE FROM\|UPDATE.*SET\|INSERT INTO" --include="*.jsp" .

# Find admin pages without protection
find . -path "*/admin/*" -name "*.jsp" -exec grep -L "session" {} \;

# Find servlets without @PreAuthorize
grep -r "@WebServlet" --include="*.java" . | grep -v "@PreAuthorize"
```

---

## ✅ Security Checklist

- [ ] All critical functions require authentication
- [ ] Session validation on every request
- [ ] Role-based authorization implemented
- [ ] CSRF tokens validated for state-changing operations
- [ ] Session timeout configured (15-30 minutes)
- [ ] HttpOnly and Secure flags set on session cookies
- [ ] Failed authentication attempts logged
- [ ] Account lockout after repeated failures
- [ ] MFA enabled for admin accounts
- [ ] Audit logging for all critical operations
- [ ] Session invalidation on logout
- [ ] Re-authentication required for sensitive actions
- [ ] Direct URL access tested and blocked

---

## 🎯 Authentication Best Practices

### 1. Session Management

```java
// Create session only after successful authentication
HttpSession session = request.getSession(true);
session.setAttribute("userId", user.getId());
session.setAttribute("userRole", user.getRole());
session.setAttribute("loginTime", System.currentTimeMillis());

// Set session timeout (30 minutes)
session.setMaxInactiveInterval(1800);

// Generate CSRF token
String csrfToken = UUID.randomUUID().toString();
session.setAttribute("csrfToken", csrfToken);
```

### 2. Session Validation

```java
public boolean isAuthenticated(HttpServletRequest request) {
    HttpSession session = request.getSession(false);

    if (session == null) {
        return false;
    }

    String userId = (String) session.getAttribute("userId");
    return userId != null && !userId.trim().isEmpty();
}

public boolean hasRole(HttpServletRequest request, String requiredRole) {
    HttpSession session = request.getSession(false);

    if (session == null) {
        return false;
    }

    String userRole = (String) session.getAttribute("userRole");
    return requiredRole.equals(userRole);
}
```

### 3. Logout Implementation

```java
@PostMapping("/logout")
public String logout(HttpServletRequest request,
                    HttpServletResponse response) {
    // Invalidate session
    HttpSession session = request.getSession(false);
    if (session != null) {
        session.invalidate();
    }

    // Delete cookies
    Cookie cookie = new Cookie("JSESSIONID", "");
    cookie.setMaxAge(0);
    cookie.setPath("/");
    response.addCookie(cookie);

    return "redirect:/login?logout=true";
}
```

---

## 🚨 Common Mistakes

1. **Client-Side Only Checks**
   ```javascript
   // DON'T: JavaScript authentication
   if (localStorage.getItem('isAdmin') === 'true') {
       // Show admin panel
   }
   // Attacker can modify localStorage
   ```

2. **Cookie-Based Authentication Without Validation**
   ```java
   // DON'T: Trust cookie values
   String role = getCookie("userRole");
   if ("ADMIN".equals(role)) {
       // Allow access
   }
   // Attacker can modify cookies
   ```

3. **Missing Session Timeout**
   ```java
   // DON'T: No timeout
   HttpSession session = request.getSession(true);
   // Session never expires - security risk

   // DO: Set timeout
   session.setMaxInactiveInterval(1800); // 30 minutes
   ```

---

## 💡 Best Practices Summary

1. **Always authenticate** - Verify user identity before granting access
2. **Use framework security** - Spring Security, Java EE Security
3. **Session-based auth** - Server-side session management
4. **Role-based access** - RBAC with least privilege
5. **CSRF protection** - Validate tokens for state changes
6. **Audit logging** - Log all authentication events
7. **Secure sessions** - HttpOnly, Secure, SameSite flags
8. **Session timeout** - Auto-logout after inactivity
9. **MFA for admin** - Multi-factor authentication for critical accounts
10. **Test thoroughly** - Verify access controls with security tests

---

**Always require authentication before allowing access to critical functions!**
