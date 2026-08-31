# Information Exposure Through Persistent Cookies

**CWE-539: Information Exposure Through Persistent Cookies**

## Overview

Information Exposure Through Persistent Cookies is a security vulnerability that occurs when web applications set excessively long expiration times for cookies containing sensitive information or store them as persistent cookies. When cookies are stored on the client's hard disk for extended periods, they can be stolen through physical access or malware, enabling attacks such as session hijacking and exposure of personal information.

## Severity
- **CVSS v3.1 Score**: 5.3 (Medium)
- **Risk Level**: Medium
- **Impact**: Confidentiality Impact

## Vulnerability Impact

### Attack Scenarios

#### Scenario 1: Long-Lived Session Cookie Theft
```
1. Web application stores an authentication token as a cookie with a 1-year expiration
2. User logs in on a public computer and only closes the browser
3. Attacker accesses the same computer and obtains the cookie file
4. Attacker uses the stolen cookie to access the user's account
5. Result: Account takeover and personal information leak
```

#### Scenario 2: Cookie Collection via Malware
```
1. Web application stores user personal information in persistent cookies
2. User's system becomes infected with malware
3. Malware scans browser cookie storage and collects information
4. Collected cookie data is transmitted to the attacker's server
5. Result: Mass leak of user information and session data
```

#### Scenario 3: Remember Me Feature Abuse
```
1. Website creates persistent cookies via a "Keep me logged in" feature
2. User logs in at a public location such as a cafe or library
3. User finishes without logging out
4. Next user accesses the same site with the same browser
5. Result: Automatic login grants access to the previous user's account
```

## Vulnerable Code Examples

### Java - Vulnerable Code (Excessive Cookie Expiration)

```java
// Vulnerable example: Authentication cookie with 1-year expiration
import javax.servlet.http.*;
import java.io.IOException;

public class VulnerableLoginServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // User authentication (simplified)
        if (authenticateUser(username, password)) {
            // Vulnerability: Authentication cookie persisted for 1 year
            Cookie loginCookie = new Cookie("authToken", generateAuthToken(username));
            loginCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year = 31,536,000 seconds
            loginCookie.setPath("/");
            response.addCookie(loginCookie);

            // Vulnerability: Sensitive user info stored in cookie
            Cookie userInfoCookie = new Cookie("userInfo", username + ":" + getUserEmail(username));
            userInfoCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
            response.addCookie(userInfoCookie);

            response.sendRedirect("/dashboard");
        } else {
            response.sendRedirect("/login?error=1");
        }
    }

    // Vulnerable Remember Me feature
    protected void handleRememberMe(HttpServletRequest request, HttpServletResponse response) {
        String rememberMe = request.getParameter("rememberMe");

        if ("true".equals(rememberMe)) {
            String username = request.getParameter("username");
            // Vulnerability: Username and password stored in cookie
            Cookie rememberCookie = new Cookie("remembered", username + ":" + request.getParameter("password"));
            rememberCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
            response.addCookie(rememberCookie);
        }
    }

    private boolean authenticateUser(String username, String password) {
        // Authentication logic
        return true;
    }

    private String generateAuthToken(String username) {
        return "TOKEN_" + username + "_" + System.currentTimeMillis();
    }

    private String getUserEmail(String username) {
        return username + "@example.com";
    }
}
```

### Java - Vulnerable Code (Persistent Session Information)

```java
// Vulnerable example: Storing session information as persistent cookies
import javax.servlet.http.*;
import java.util.UUID;

public class VulnerableSessionManager {

    public void createUserSession(HttpServletRequest request, HttpServletResponse response,
                                  String username, String role) {
        // Vulnerability: Session ID stored as persistent cookie
        String sessionId = UUID.randomUUID().toString();
        Cookie sessionCookie = new Cookie("JSESSIONID", sessionId);
        sessionCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        sessionCookie.setPath("/");
        response.addCookie(sessionCookie);

        // Vulnerability: User role information stored as persistent cookie
        Cookie roleCookie = new Cookie("userRole", role);
        roleCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        response.addCookie(roleCookie);

        // Vulnerability: Permission information stored in cookie
        Cookie permissionCookie = new Cookie("permissions", "admin,write,delete");
        permissionCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        response.addCookie(permissionCookie);
    }

    // Vulnerability: HttpOnly and Secure flags not set
    public void setPreferences(HttpServletResponse response, String preferences) {
        Cookie prefCookie = new Cookie("userPrefs", preferences);
        prefCookie.setMaxAge(60 * 60 * 24 * 365); // 1 year
        // No HttpOnly flag - vulnerable to XSS attacks
        // No Secure flag - exposed over HTTP connections
        response.addCookie(prefCookie);
    }
}
```

### C# - Vulnerable Code

```csharp
// Vulnerable example: Excessive cookie expiration in ASP.NET
using System;
using System.Web;

public class VulnerableLoginHandler : IHttpHandler
{
    public void ProcessRequest(HttpContext context)
    {
        string username = context.Request.Form["username"];
        string password = context.Request.Form["password"];

        if (AuthenticateUser(username, password))
        {
            // Vulnerability: Authentication cookie persisted for 1 year
            HttpCookie authCookie = new HttpCookie("AuthToken");
            authCookie.Value = GenerateAuthToken(username);
            authCookie.Expires = DateTime.Now.AddMinutes(60.0 * 24.0 * 365.0); // 1 year
            context.Response.Cookies.Add(authCookie);

            // Vulnerability: Sensitive information stored in persistent cookie
            HttpCookie userInfoCookie = new HttpCookie("UserInfo");
            userInfoCookie.Values["Username"] = username;
            userInfoCookie.Values["Email"] = GetUserEmail(username);
            userInfoCookie.Values["Role"] = GetUserRole(username);
            userInfoCookie.Expires = DateTime.Now.AddYears(1); // 1 year
            context.Response.Cookies.Add(userInfoCookie);

            context.Response.Redirect("/Dashboard.aspx");
        }
    }

    // Vulnerable Remember Me implementation
    private void SetRememberMeCookie(HttpContext context, string username, string password)
    {
        // Vulnerability: Password stored in cookie
        HttpCookie rememberCookie = new HttpCookie("RememberMe");
        rememberCookie.Value = username + ":" + password; // Plaintext password stored!
        rememberCookie.Expires = DateTime.Now.AddDays(365);
        // HttpOnly and Secure flags not set
        context.Response.Cookies.Add(rememberCookie);
    }

    public bool IsReusable => false;

    private bool AuthenticateUser(string username, string password) => true;
    private string GenerateAuthToken(string username) => "TOKEN_" + username;
    private string GetUserEmail(string username) => username + "@example.com";
    private string GetUserRole(string username) => "User";
}
```

### C - Vulnerable Code (CGI Environment)

```c
// Vulnerable example: Excessive cookie expiration in C CGI
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define ONE_YEAR_SECONDS (60 * 60 * 24 * 365)

// Vulnerable function: Setting persistent authentication cookie
void set_persistent_auth_cookie(const char* username, const char* token) {
    time_t now = time(NULL);
    time_t expires = now + ONE_YEAR_SECONDS; // 1 year later
    struct tm* expire_time = gmtime(&expires);
    char expire_str[100];

    strftime(expire_str, sizeof(expire_str), "%a, %d %b %Y %H:%M:%S GMT", expire_time);

    // Vulnerability: Authentication cookie with 1-year expiration
    printf("Set-Cookie: authToken=%s; Expires=%s; Path=/\r\n", token, expire_str);

    // Vulnerability: No HttpOnly or Secure flags
    printf("Set-Cookie: username=%s; Expires=%s; Path=/\r\n", username, expire_str);
}

// Vulnerable Remember Me feature
void handle_remember_me(const char* username, const char* password) {
    time_t now = time(NULL);
    time_t expires = now + (60 * 60 * 24 * 365); // 1 year
    struct tm* expire_time = gmtime(&expires);
    char expire_str[100];
    char cookie_value[256];

    strftime(expire_str, sizeof(expire_str), "%a, %d %b %Y %H:%M:%S GMT", expire_time);

    // Vulnerability: Username and password stored in cookie
    sprintf(cookie_value, "%s:%s", username, password);
    printf("Set-Cookie: remembered=%s; Expires=%s; Path=/\r\n", cookie_value, expire_str);
}

// Vulnerable session management
void create_session_cookie(const char* session_id, const char* user_role) {
    time_t now = time(NULL);
    time_t expires = now + (60 * 60 * 24 * 30); // 30 days
    struct tm* expire_time = gmtime(&expires);
    char expire_str[100];

    strftime(expire_str, sizeof(expire_str), "%a, %d %b %Y %H:%M:%S GMT", expire_time);

    // Vulnerability: Session cookie persisted for 30 days
    printf("Set-Cookie: SESSIONID=%s; Expires=%s; Path=/\r\n", session_id, expire_str);

    // Vulnerability: Permission information stored as persistent cookie
    printf("Set-Cookie: role=%s; Expires=%s; Path=/\r\n", user_role, expire_str);
}

int main(void) {
    printf("Content-Type: text/html\r\n");

    // Vulnerable cookie settings
    set_persistent_auth_cookie("admin", "TOKEN123456");
    handle_remember_me("admin", "password123");
    create_session_cookie("SESSION_ABC123", "administrator");

    printf("\r\n<html><body>Login successful</body></html>\n");
    return 0;
}
```

## Secure Code Examples

### Java - Secure Code

```java
// Secure example: Proper cookie management
import javax.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class SecureLoginServlet extends HttpServlet {

    // Session store (use Redis or similar in production)
    private static final ConcurrentHashMap<String, SessionData> sessionStore = new ConcurrentHashMap<>();
    private static final SecureRandom secureRandom = new SecureRandom();

    // Security settings: Short cookie expiration times
    private static final int SESSION_TIMEOUT_SECONDS = 60 * 60; // 1 hour
    private static final int REMEMBER_ME_TIMEOUT_DAYS = 7; // Maximum 7 days

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean rememberMe = "true".equals(request.getParameter("rememberMe"));

        if (authenticateUser(username, password)) {
            // Generate secure session token
            String sessionToken = generateSecureToken();

            // Store session data on the server side
            SessionData sessionData = new SessionData(username, getUserRole(username));
            sessionStore.put(sessionToken, sessionData);

            // Set session cookie (short expiration time)
            Cookie sessionCookie = new Cookie("sessionToken", sessionToken);

            if (rememberMe) {
                // Even for Remember Me, limit to a maximum of 7 days
                sessionCookie.setMaxAge(60 * 60 * 24 * REMEMBER_ME_TIMEOUT_DAYS);
            } else {
                // Normal login: browser session cookie (no MaxAge set)
                // or short duration (1 hour)
                sessionCookie.setMaxAge(SESSION_TIMEOUT_SECONDS);
            }

            // Set security flags
            sessionCookie.setHttpOnly(true);  // Prevent XSS attacks
            sessionCookie.setSecure(true);    // HTTPS only
            sessionCookie.setPath("/");

            // Set SameSite attribute (CSRF prevention)
            response.setHeader("Set-Cookie",
                String.format("%s=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=%d",
                    sessionCookie.getName(),
                    sessionCookie.getValue(),
                    sessionCookie.getMaxAge()));

            response.sendRedirect("/dashboard");
        } else {
            response.sendRedirect("/login?error=1");
        }
    }

    // Secure token generation
    private String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    // Session validation
    protected SessionData validateSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sessionToken".equals(cookie.getName())) {
                    return sessionStore.get(cookie.getValue());
                }
            }
        }
        return null;
    }

    // Remove session on logout
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("sessionToken".equals(cookie.getName())) {
                    // Remove server-side session
                    sessionStore.remove(cookie.getValue());

                    // Expire cookie
                    Cookie expiredCookie = new Cookie("sessionToken", "");
                    expiredCookie.setMaxAge(0);
                    expiredCookie.setHttpOnly(true);
                    expiredCookie.setSecure(true);
                    expiredCookie.setPath("/");
                    response.addCookie(expiredCookie);
                }
            }
        }
    }

    private boolean authenticateUser(String username, String password) {
        // Actual authentication logic
        return true;
    }

    private String getUserRole(String username) {
        return "USER";
    }

    // Session data class
    private static class SessionData {
        private final String username;
        private final String role;
        private final long createdAt;

        public SessionData(String username, String role) {
            this.username = username;
            this.role = role;
            this.createdAt = System.currentTimeMillis();
        }

        public boolean isExpired(int timeoutSeconds) {
            return (System.currentTimeMillis() - createdAt) > (timeoutSeconds * 1000L);
        }
    }
}
```

### Java - Secure Remember Me Implementation

```java
// Secure Remember Me feature
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class SecureRememberMeService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final byte[] SECRET_KEY = generateSecretKey(); // Server secret key
    private static final int TOKEN_VALIDITY_DAYS = 7;

    // Secure Remember Me token generation
    public String createRememberMeToken(String username) {
        try {
            long expiryTime = System.currentTimeMillis() + (TOKEN_VALIDITY_DAYS * 24L * 60 * 60 * 1000);

            // Token structure: username:expiryTime:signature
            String tokenData = username + ":" + expiryTime;
            String signature = generateSignature(tokenData);
            String token = tokenData + ":" + signature;

            return Base64.getUrlEncoder().encodeToString(token.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }

    // Remember Me token validation
    public String validateRememberMeToken(String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token));
            String[] parts = decoded.split(":");

            if (parts.length != 3) {
                return null;
            }

            String username = parts[0];
            long expiryTime = Long.parseLong(parts[1]);
            String signature = parts[2];

            // Check expiration time
            if (System.currentTimeMillis() > expiryTime) {
                return null;
            }

            // Verify signature
            String expectedSignature = generateSignature(username + ":" + expiryTime);
            if (!signature.equals(expectedSignature)) {
                return null;
            }

            return username;
        } catch (Exception e) {
            return null;
        }
    }

    // HMAC signature generation
    private String generateSignature(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(SECRET_KEY, HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] signature = mac.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(signature);
    }

    // Secret key generation (in practice, load from a secure store)
    private static byte[] generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32];
        random.nextBytes(key);
        return key;
    }
}
```

### C# - Secure Code

```csharp
// Secure example: Proper cookie management in ASP.NET
using System;
using System.Security.Cryptography;
using System.Text;
using System.Web;

public class SecureLoginHandler : IHttpHandler
{
    private const int SESSION_TIMEOUT_MINUTES = 60; // 1 hour
    private const int REMEMBER_ME_TIMEOUT_DAYS = 7; // 7 days

    public void ProcessRequest(HttpContext context)
    {
        string username = context.Request.Form["username"];
        string password = context.Request.Form["password"];
        bool rememberMe = context.Request.Form["rememberMe"] == "true";

        if (AuthenticateUser(username, password))
        {
            // Generate secure session token
            string sessionToken = GenerateSecureToken();

            // Store session data on the server side
            context.Session["Username"] = username;
            context.Session["Role"] = GetUserRole(username);
            context.Session.Timeout = SESSION_TIMEOUT_MINUTES;

            // Set session cookie
            HttpCookie sessionCookie = new HttpCookie("SessionToken", sessionToken);

            if (rememberMe)
            {
                // Maximum 7 days for Remember Me
                sessionCookie.Expires = DateTime.Now.AddDays(REMEMBER_ME_TIMEOUT_DAYS);
            }
            else
            {
                // Browser session cookie (no Expires set)
                // or short duration
                sessionCookie.Expires = DateTime.Now.AddMinutes(SESSION_TIMEOUT_MINUTES);
            }

            // Set security flags
            sessionCookie.HttpOnly = true;  // Prevent XSS
            sessionCookie.Secure = true;    // HTTPS only
            sessionCookie.Path = "/";
            sessionCookie.SameSite = SameSiteMode.Strict; // Prevent CSRF

            context.Response.Cookies.Add(sessionCookie);
            context.Response.Redirect("/Dashboard.aspx");
        }
    }

    // Secure token generation
    private string GenerateSecureToken()
    {
        byte[] randomBytes = new byte[32];
        using (var rng = new RNGCryptoServiceProvider())
        {
            rng.GetBytes(randomBytes);
        }
        return Convert.ToBase64String(randomBytes);
    }

    // Secure Remember Me token generation
    private string CreateRememberMeToken(string username)
    {
        long expiryTime = DateTimeOffset.Now.AddDays(REMEMBER_ME_TIMEOUT_DAYS).ToUnixTimeSeconds();
        string tokenData = $"{username}:{expiryTime}";
        string signature = GenerateHMAC(tokenData);
        string token = $"{tokenData}:{signature}";
        return Convert.ToBase64String(Encoding.UTF8.GetBytes(token));
    }

    // HMAC signature generation
    private string GenerateHMAC(string data)
    {
        byte[] secretKey = GetSecretKey(); // Server secret key
        using (var hmac = new HMACSHA256(secretKey))
        {
            byte[] hash = hmac.ComputeHash(Encoding.UTF8.GetBytes(data));
            return Convert.ToBase64String(hash);
        }
    }

    // Logout handling
    private void HandleLogout(HttpContext context)
    {
        // Invalidate session
        context.Session.Clear();
        context.Session.Abandon();

        // Expire cookie
        if (context.Request.Cookies["SessionToken"] != null)
        {
            HttpCookie cookie = new HttpCookie("SessionToken");
            cookie.Expires = DateTime.Now.AddDays(-1);
            cookie.Value = "";
            context.Response.Cookies.Add(cookie);
        }
    }

    public bool IsReusable => false;

    private bool AuthenticateUser(string username, string password) => true;
    private string GetUserRole(string username) => "User";
    private byte[] GetSecretKey() => new byte[32]; // In practice, load from a secure store
}
```

### C - Secure Code

```c
// Secure example: Proper cookie management in C CGI
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <openssl/rand.h>
#include <openssl/hmac.h>

#define SESSION_TIMEOUT_SECONDS (60 * 60)        // 1 hour
#define REMEMBER_ME_TIMEOUT_SECONDS (60 * 60 * 24 * 7) // 7 days
#define TOKEN_SIZE 32

// Secure session cookie setup
void set_secure_session_cookie(const char* session_token, int remember_me) {
    time_t now = time(NULL);
    time_t expires;
    struct tm* expire_time;
    char expire_str[100];

    if (remember_me) {
        expires = now + REMEMBER_ME_TIMEOUT_SECONDS; // 7 days
    } else {
        expires = now + SESSION_TIMEOUT_SECONDS; // 1 hour
    }

    expire_time = gmtime(&expires);
    strftime(expire_str, sizeof(expire_str), "%a, %d %b %Y %H:%M:%S GMT", expire_time);

    // Cookie with security flags
    printf("Set-Cookie: sessionToken=%s; Expires=%s; Path=/; HttpOnly; Secure; SameSite=Strict\r\n",
           session_token, expire_str);
}

// Secure random token generation
int generate_secure_token(char* token_out, size_t token_size) {
    unsigned char random_bytes[TOKEN_SIZE];

    // Secure random generation using OpenSSL
    if (RAND_bytes(random_bytes, TOKEN_SIZE) != 1) {
        return -1;
    }

    // Base64 encoding (simplified)
    for (int i = 0; i < TOKEN_SIZE && i * 2 < token_size - 1; i++) {
        sprintf(token_out + (i * 2), "%02x", random_bytes[i]);
    }

    return 0;
}

// Secure Remember Me token generation
void create_remember_me_token(const char* username, char* token_out, size_t token_size) {
    time_t expiry = time(NULL) + REMEMBER_ME_TIMEOUT_SECONDS;
    char token_data[256];
    unsigned char signature[EVP_MAX_MD_SIZE];
    unsigned int sig_len;

    // Token data structure: username:expiryTime
    snprintf(token_data, sizeof(token_data), "%s:%ld", username, expiry);

    // HMAC signature generation
    unsigned char secret_key[32]; // In practice, load from a secure store
    RAND_bytes(secret_key, sizeof(secret_key));

    HMAC(EVP_sha256(), secret_key, sizeof(secret_key),
         (unsigned char*)token_data, strlen(token_data),
         signature, &sig_len);

    // Token structure: tokenData:signature (simplified)
    snprintf(token_out, token_size, "%s:%02x%02x%02x%02x",
             token_data, signature[0], signature[1], signature[2], signature[3]);
}

// Session-only cookie (deleted when browser closes)
void set_session_only_cookie(const char* session_token) {
    // No Expires attribute set -> browser session cookie
    printf("Set-Cookie: sessionToken=%s; Path=/; HttpOnly; Secure; SameSite=Strict\r\n",
           session_token);
}

// Cookie expiration handling (logout)
void expire_session_cookie(void) {
    time_t past = time(NULL) - 3600; // 1 hour in the past
    struct tm* past_time = gmtime(&past);
    char expire_str[100];

    strftime(expire_str, sizeof(expire_str), "%a, %d %b %Y %H:%M:%S GMT", past_time);

    printf("Set-Cookie: sessionToken=; Expires=%s; Path=/; HttpOnly; Secure\r\n",
           expire_str);
}

int main(void) {
    char session_token[TOKEN_SIZE * 2 + 1];
    char remember_token[512];

    printf("Content-Type: text/html\r\n");

    // Secure session token generation
    if (generate_secure_token(session_token, sizeof(session_token)) == 0) {
        // If Remember Me is not checked: browser session cookie
        set_session_only_cookie(session_token);

        // Or if Remember Me is checked: cookie with limited duration
        // set_secure_session_cookie(session_token, 1);
    }

    printf("\r\n<html><body>Login successful</body></html>\n");
    return 0;
}
```

## Security Best Practices

### 1. Cookie Expiration Management
```
- Session cookies: No MaxAge set (deleted when browser closes)
- Authentication tokens: Maximum 1 hour to 1 day
- Remember Me: Maximum 7 to 14 days
- Sensitive information: Never store in persistent cookies
```

### 2. Security Flag Settings
```
- HttpOnly: Block JavaScript access (prevent XSS)
- Secure: Allow only HTTPS communication
- SameSite: Prevent CSRF attacks
  - Strict: Most restrictive (same site only)
  - Lax: Allow safe HTTP methods
  - None: Must be used with Secure flag
```

### 3. Sensitive Information Handling
```
- Passwords: Never store in cookies
- Personal information: Store in server sessions, cookies hold tokens only
- Permission information: Manage on server side, prevent cookie manipulation
- Credit card information: Never store in cookies
```

### 4. Secure Remember Me Implementation
```java
// Recommendations
- Generate unique tokens per user
- Prevent tampering with HMAC signatures
- Include and verify expiration time
- Token reuse detection mechanism
- Invalidate tokens on password change
```

## Detection and Prevention

### Static Analysis Tools

```bash
# FindBugs/SpotBugs rules
- COOKIE_PERSISTENT_DATA: Sensitive info stored in persistent cookies
- INSECURE_COOKIE: HttpOnly/Secure flags not set

# SonarQube rules
- javasecurity:S2255: Cookie should be "secure"
- javasecurity:S3330: Cookie should use "HttpOnly"

# Run static analysis
./gradlew spotbugsMain
sonar-scanner
```

### Code Review Checklist

```
[ ] Is the cookie expiration time appropriate? (1 year or more is prohibited)
[ ] Is the HttpOnly flag set?
[ ] Is the Secure flag set? (in HTTPS environments)
[ ] Is the SameSite attribute set?
[ ] Is sensitive information (passwords, personal data) not stored in cookies?
[ ] Are session tokens generated securely? (SecureRandom)
[ ] Is the Remember Me feature implemented securely?
[ ] Are cookies properly expired on logout?
```

### Runtime Monitoring

```java
// Cookie security validation filter
public class CookieSecurityFilter implements Filter {

    private static final int MAX_COOKIE_AGE = 60 * 60 * 24 * 7; // 7 days

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Validate cookies with response wrapper
        CookieValidationResponseWrapper wrapper = new CookieValidationResponseWrapper(httpResponse);
        chain.doFilter(request, wrapper);
    }

    private class CookieValidationResponseWrapper extends HttpServletResponseWrapper {

        public CookieValidationResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addCookie(Cookie cookie) {
            // Cookie security validation
            if (cookie.getMaxAge() > MAX_COOKIE_AGE) {
                logSecurityWarning("Cookie age too long: " + cookie.getName());
            }

            if (!cookie.isHttpOnly()) {
                logSecurityWarning("Cookie missing HttpOnly: " + cookie.getName());
            }

            if (!cookie.getSecure()) {
                logSecurityWarning("Cookie missing Secure flag: " + cookie.getName());
            }

            super.addCookie(cookie);
        }

        private void logSecurityWarning(String message) {
            System.err.println("SECURITY WARNING: " + message);
        }
    }
}
```

## Testing Methods

### Unit Tests

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CookieSecurityTest {

    @Test
    void testSessionCookieMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecureLoginServlet servlet = new SecureLoginServlet();

        servlet.createSessionCookie(response, "testUser", false);

        Cookie cookie = response.getCookie("sessionToken");
        assertNotNull(cookie);

        // Session cookie should be 1 hour or less
        assertTrue(cookie.getMaxAge() <= 3600,
            "Session cookie max age should be <= 1 hour");
    }

    @Test
    void testCookieSecurityFlags() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecureLoginServlet servlet = new SecureLoginServlet();

        servlet.createSessionCookie(response, "testUser", false);

        Cookie cookie = response.getCookie("sessionToken");

        assertTrue(cookie.isHttpOnly(), "Cookie must have HttpOnly flag");
        assertTrue(cookie.getSecure(), "Cookie must have Secure flag");
        assertEquals("/", cookie.getPath(), "Cookie path should be /");
    }

    @Test
    void testRememberMeMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecureLoginServlet servlet = new SecureLoginServlet();

        servlet.createSessionCookie(response, "testUser", true);

        Cookie cookie = response.getCookie("sessionToken");

        // Remember Me should also be 7 days or less
        int sevenDays = 60 * 60 * 24 * 7;
        assertTrue(cookie.getMaxAge() <= sevenDays,
            "Remember Me cookie should be <= 7 days");
    }
}
```

### Integration Tests

```java
@WebMvcTest
class CookieSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLoginSetsCookieWithSecurityFlags() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                .param("username", "testuser")
                .param("password", "testpass"))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        Cookie cookie = result.getResponse().getCookie("sessionToken");
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());

        // Verify Set-Cookie header
        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("Secure"));
        assertTrue(setCookieHeader.contains("SameSite=Strict"));
    }

    @Test
    void testLogoutExpiresCookie() throws Exception {
        // Login first
        Cookie sessionCookie = new Cookie("sessionToken", "valid-token");

        // Logout
        MvcResult result = mockMvc.perform(post("/logout")
                .cookie(sessionCookie))
            .andExpect(status().is3xxRedirection())
            .andReturn();

        Cookie expiredCookie = result.getResponse().getCookie("sessionToken");
        assertNotNull(expiredCookie);
        assertEquals(0, expiredCookie.getMaxAge(), "Cookie should be expired");
    }
}
```

### Verification via Browser Developer Tools

```
1. Using Chrome DevTools:
   - F12 > Application > Cookies
   - Items to verify:
     - Expires/Max-Age: Appropriate expiration time
     - HttpOnly: checked
     - Secure: checked (in HTTPS environment)
     - SameSite: Strict or Lax

2. Verify cookie contents:
   - Check for sensitive information (passwords, personal data)
   - Confirm tokens are unpredictable random values

3. Cookie lifetime testing:
   - Verify automatic expiration after the configured time
   - Verify session cookies are deleted after browser restart
```

## Related Vulnerabilities

- **CWE-315**: Cleartext Storage of Sensitive Information in a Cookie
- **CWE-614**: Sensitive Cookie in HTTPS Session Without 'Secure' Attribute
- **CWE-1004**: Sensitive Cookie Without 'HttpOnly' Flag
- **CWE-352**: Cross-Site Request Forgery (CSRF)

## References

### Standards and Guides
- OWASP Top 10 2021: A01:2021 - Broken Access Control
- OWASP Session Management Cheat Sheet
- OWASP Testing Guide: Testing for Cookies Attributes
- RFC 6265: HTTP State Management Mechanism

### Tools
- OWASP ZAP: Cookie security scanner
- Burp Suite: Cookie manipulation and analysis
- Chrome DevTools: Cookie inspection

### Additional Resources
- CWE-539: https://cwe.mitre.org/data/definitions/539.html
- OWASP Cookie Security: https://owasp.org/www-community/controls/SecureCookieAttribute

## Checklist

### Development Phase
- [ ] Minimize cookie expiration time according to business requirements
- [ ] Set HttpOnly flag on all cookies
- [ ] Set Secure flag in HTTPS environments
- [ ] Set SameSite attribute (Strict or Lax)
- [ ] Store sensitive information in server sessions
- [ ] Use secure random number generator (SecureRandom)
- [ ] Implement Remember Me feature securely

### Testing Phase
- [ ] Automated cookie security flag verification tests
- [ ] Cookie expiration time verification
- [ ] Verify cookie expiration on logout
- [ ] Inspect cookies with browser developer tools

### Deployment Phase
- [ ] Verify HTTPS is applied
- [ ] Verify proxy/load balancer cookie settings
- [ ] Document cookie security policies
- [ ] Configure monitoring and logging

---

**Last updated**: 2025-02-05
