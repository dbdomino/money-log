# Cross-Site Request Forgery (CSRF) (CWE-352)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A01:2021 – Broken Access Control

---

## Overview

### Attack Description

Cross-Site Request Forgery (CSRF) is an attack that forces authenticated users to submit requests to a web application for which they are currently authenticated, without the user's knowledge or intent. If a web application does not verify whether requests from users were actually composed and sent as intended, this vulnerability can occur. Attackers trick victims into executing unwanted actions such as changing email, transferring money, or changing passwords without their knowledge.

### Impact

The attacker exploits the user's authenticated session to perform specific actions, and the application cannot distinguish between normal and forged requests. If the data structure is fixed and transmitted via GET method, an attacker can easily discover this and send crafted requests to trigger dangerous operations.

**Potential consequences:**
- Unauthorized fund transfers
- Password/email changes
- Account takeover
- Data modification/deletion
- Privilege escalation
- Execution of administrative functions

---

## Security Measures

### Key Principles

Use POST method instead of GET when creating input forms. Use tokens between input forms and the programs that process them, and ensure that direct URL usage by attackers does not work. For critical functions, enforce re-authentication in addition to session validation.

**Primary Defenses:**

1. **CSRF Tokens (Synchronizer Token Pattern)**
   - Generate unique, unpredictable token per session/request
   - Embed token in forms as hidden field
   - Validate token on server-side before processing
   - Token must be tied to user session

2. **SameSite Cookie Attribute**
   - Set `SameSite=Strict` or `SameSite=Lax` on cookies
   - Prevents cookies from being sent in cross-site requests
   - Modern browser support required

3. **Double Submit Cookie**
   - Send random value in both cookie and request parameter
   - Server verifies both values match
   - Doesn't require server-side session storage

4. **Custom Request Headers**
   - Use custom headers (e.g., `X-Requested-With: XMLHttpRequest`)
   - CORS prevents adding custom headers in cross-origin requests

5. **Re-authentication for Sensitive Actions**
   - Require password re-entry for critical operations
   - Use CAPTCHA for sensitive actions
   - Send confirmation emails/SMS

---

## Code Examples

### Attack Scenario

When requests from clients are processed without verifying whether they are legitimate, the application can be vulnerable to CSRF attacks.

**Attack Flow:**
1. User logs into `bank.com`
2. Attacker sends user email with malicious link/image
3. User clicks link while still logged into bank
4. Browser automatically sends authentication cookies
5. Bank processes forged request as legitimate
6. Money transferred to attacker's account

**Example Malicious HTML:**
```html
<!-- Hidden form that auto-submits -->
<form action="https://bank.com/transfer" method="POST" id="csrf">
    <input type="hidden" name="to" value="attacker_account">
    <input type="hidden" name="amount" value="10000">
</form>
<script>
    document.getElementById('csrf').submit();
</script>

<!-- Or as an image tag -->
<img src="https://bank.com/transfer?to=attacker&amount=10000">
```

---

### ❌ Vulnerable Code

#### Java - No CSRF Protection

```html
<!-- This type of request is fundamentally vulnerable to CSRF -->
```

**Vulnerable Form:**
```html
<form action="/transfer" method="POST">
    <input type="text" name="to" placeholder="Recipient">
    <input type="number" name="amount" placeholder="Amount">
    <input type="submit" value="Transfer">
</form>
```

**Vulnerable Servlet:**
```java
@WebServlet("/transfer")
public class TransferServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        // No CSRF token validation!
        String to = request.getParameter("to");
        String amount = request.getParameter("amount");

        // Process transfer
        bankService.transfer(getCurrentUser(), to, amount);

        response.sendRedirect("/success");
    }
}
```

**Problems:**
1. No CSRF token in form
2. No token validation on server
3. Accepts any POST request with valid session
4. Vulnerable to forged requests

---

### ✅ Secure Code

#### Java - CSRF Token with Session

```java
// When the input screen is requested, generate a random token and store it in the session
session.setAttribute("SESSION_CSRF_TOKEN", UUID.randomUUID().toString());
// Set the random token as a HIDDEN field value in the input screen to be sent along
<input type="hidden" name="param_csrf_token" value="${SESSION_CSRF_TOKEN}"/>

// Compare the request parameter with the token stored in the session; process only if they match
String pToken = request.getParameter("param_csrf_token");
String sToken = (String)session.getAttribute("SESSION_CSRF_TOKEN");
if (pToken != null && pToken.equals(sToken)) {
    // Matching token exists -> normal processing
    ......
} else {
    // Token missing or values don't match -> display error message
    ......
}
```

**Security Features:**
1. Generate random CSRF token per session
2. Store token in session
3. Include token in hidden form field
4. Validate token before processing request
5. Reject mismatched or missing tokens

---

#### ✅ Complete CSRF Protection Example

```java
public class CSRFProtectionFilter implements Filter {

    private static final String CSRF_TOKEN_NAME = "CSRF_TOKEN";
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        // Only check POST, PUT, DELETE requests
        String method = req.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method) ||
            "OPTIONS".equals(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Generate token if doesn't exist
        if (session == null ||
            session.getAttribute(CSRF_TOKEN_NAME) == null) {
            session = req.getSession(true);
            String token = UUID.randomUUID().toString();
            session.setAttribute(CSRF_TOKEN_NAME, token);
        }

        // Get token from session
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_NAME);

        // Get token from request (form or header)
        String requestToken = req.getParameter(CSRF_TOKEN_NAME);
        if (requestToken == null) {
            requestToken = req.getHeader(CSRF_HEADER_NAME);
        }

        // Validate token
        if (requestToken == null || !requestToken.equals(sessionToken)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                "CSRF token validation failed");
            return;
        }

        // Token valid, proceed
        chain.doFilter(request, response);
    }
}
```

**Register Filter in web.xml:**
```xml
<filter>
    <filter-name>CSRFProtectionFilter</filter-name>
    <filter-class>com.example.CSRFProtectionFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>CSRFProtectionFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**JSP Form with Token:**
```jsp
<%
    String csrfToken = (String) session.getAttribute("CSRF_TOKEN");
%>
<form action="/transfer" method="POST">
    <input type="hidden" name="CSRF_TOKEN" value="<%= csrfToken %>">
    <input type="text" name="to" placeholder="Recipient">
    <input type="number" name="amount" placeholder="Amount">
    <input type="submit" value="Transfer">
</form>
```

---

#### ✅ Spring Security - Automatic CSRF Protection

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .and()
            .authorizeRequests()
                .anyRequest().authenticated();
    }
}
```

**Thymeleaf Form (Auto-includes CSRF):**
```html
<form th:action="@{/transfer}" method="post">
    <!-- CSRF token automatically added by Spring Security -->
    <input type="text" name="to" placeholder="Recipient">
    <input type="number" name="amount" placeholder="Amount">
    <button type="submit">Transfer</button>
</form>
```

**JavaScript with CSRF Token:**
```javascript
// Get CSRF token from meta tag
var token = $("meta[name='_csrf']").attr("content");
var header = $("meta[name='_csrf_header']").attr("content");

// Include in AJAX requests
$.ajax({
    url: "/transfer",
    type: "POST",
    data: { to: "recipient", amount: 1000 },
    beforeSend: function(xhr) {
        xhr.setRequestHeader(header, token);
    }
});
```

---

### C# / ASP.NET Example

#### ✅ Secure C# with AntiForgeryToken

```csharp
@using (Html.BeginForm("PostTest","Home",FormMethod.Post,null))
{
    // Use AntiForgeryToken() to prevent Cross-Site Request Forgery
    @Html.AntiForgeryToken()
    <input type="submit" value="Html PsBk Click" />
}
```

**Controller with Validation:**
```csharp
[HttpPost]
[ValidateAntiForgeryToken]
public ActionResult Transfer(TransferModel model)
{
    // AntiForgeryToken automatically validated by attribute
    // Process transfer
    return View("Success");
}
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-352: Cross-Site Request Forgery (CSRF)**
   MITRE, http://cwe.mitre.org/data/definitions/352.html

### OWASP

② **"Security Corner: Cross-Site Request Forgeries"**
   Chris Shiflett, http://shiflett.org/articles/cross-site-request-forgeries

③ **Cross-Site_Request_Forgery_(CSRF)**
   OWASP, https://www.owasp.org/index.php/Cross-Site_Request_Forgery_(CSRF)

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find forms without CSRF tokens
grep -r "<form" . | grep -v "csrf"
grep -r "doPost" . | grep -v "csrf"

# Find state-changing GET requests
grep -r "@GetMapping.*delete" .
grep -r "@GetMapping.*update" .

# Check for CSRF protection
grep -r "csrf" .
grep -r "CSRF_TOKEN" .
grep -r "AntiForgeryToken" .
```

---

## ✅ Security Checklist

- [ ] All state-changing operations use POST/PUT/DELETE (not GET)
- [ ] CSRF tokens generated for all forms
- [ ] CSRF tokens validated on server-side
- [ ] Tokens tied to user session
- [ ] Tokens unpredictable (cryptographically random)
- [ ] SameSite cookie attribute set (`Strict` or `Lax`)
- [ ] Custom headers used for AJAX requests
- [ ] Re-authentication required for sensitive actions
- [ ] Framework CSRF protection enabled (Spring Security, etc.)
- [ ] CSRF testing completed

---

## 🎯 CSRF Protection Patterns

### 1. Synchronizer Token Pattern (Most Common)

```java
// Generate token
String token = UUID.randomUUID().toString();
session.setAttribute("CSRF_TOKEN", token);

// Include in form
<input type="hidden" name="csrf_token" value="${CSRF_TOKEN}">

// Validate on server
String sessionToken = (String) session.getAttribute("CSRF_TOKEN");
String requestToken = request.getParameter("csrf_token");
if (!sessionToken.equals(requestToken)) {
    throw new SecurityException("CSRF validation failed");
}
```

### 2. Double Submit Cookie

```java
// Set cookie with random value
String token = UUID.randomUUID().toString();
Cookie cookie = new Cookie("CSRF_TOKEN", token);
cookie.setHttpOnly(false); // JavaScript needs to read it
cookie.setSecure(true);
cookie.setPath("/");
response.addCookie(cookie);

// JavaScript includes token in request
fetch('/api/transfer', {
    method: 'POST',
    headers: {
        'X-CSRF-TOKEN': getCookie('CSRF_TOKEN')
    },
    body: JSON.stringify(data)
});

// Server validates cookie matches header
String cookieToken = getCookieValue(request, "CSRF_TOKEN");
String headerToken = request.getHeader("X-CSRF-TOKEN");
if (!cookieToken.equals(headerToken)) {
    throw new SecurityException("CSRF validation failed");
}
```

### 3. SameSite Cookie

```java
// Set SameSite attribute on session cookie
Cookie sessionCookie = new Cookie("JSESSIONID", sessionId);
sessionCookie.setHttpOnly(true);
sessionCookie.setSecure(true);
sessionCookie.setPath("/");

// Java EE 8+ or Servlet 4.0+
sessionCookie.setAttribute("SameSite", "Strict"); // or "Lax"

response.addCookie(sessionCookie);
```

**web.xml configuration:**
```xml
<session-config>
    <cookie-config>
        <http-only>true</http-only>
        <secure>true</secure>
        <same-site>Strict</same-site>
    </cookie-config>
</session-config>
```

---

## 🚨 Common Mistakes

1. **Using GET for State Changes**
   ```java
   // DON'T: State-changing operation with GET
   @GetMapping("/delete")
   public String deleteUser(@RequestParam Long id) {
       userService.delete(id); // Vulnerable to CSRF
       return "redirect:/users";
   }

   // DO: Use POST/DELETE
   @PostMapping("/delete")
   @CSRFProtected
   public String deleteUser(@RequestParam Long id) {
       userService.delete(id);
       return "redirect:/users";
   }
   ```

2. **Token in URL**
   ```html
   <!-- DON'T: Token in URL (can leak in Referer header) -->
   <a href="/transfer?csrf_token=abc123&amount=1000">Transfer</a>

   <!-- DO: Token in form body or header -->
   <form method="POST">
       <input type="hidden" name="csrf_token" value="abc123">
   </form>
   ```

3. **Weak Token Generation**
   ```java
   // DON'T: Predictable token
   String token = username + timestamp;

   // DO: Cryptographically random token
   String token = UUID.randomUUID().toString();
   // Or better:
   SecureRandom random = new SecureRandom();
   byte[] bytes = new byte[32];
   random.nextBytes(bytes);
   String token = Base64.getEncoder().encodeToString(bytes);
   ```

4. **Not Validating on Server**
   ```java
   // DON'T: Only check if token exists
   if (request.getParameter("csrf_token") != null) {
       // Process request - INSECURE!
   }

   // DO: Validate token matches session
   String sessionToken = (String) session.getAttribute("CSRF_TOKEN");
   String requestToken = request.getParameter("csrf_token");
   if (sessionToken != null && sessionToken.equals(requestToken)) {
       // Process request - SECURE
   }
   ```

---

## 💡 Framework-Specific Solutions

### Spring Security

```java
// Auto-enabled by default in Spring Security 4+
http.csrf(); // Enabled

// Access token in Thymeleaf
<input type="hidden" th:name="${_csrf.parameterName}"
       th:value="${_csrf.token}"/>

// Access token in JSP
<input type="hidden" name="${_csrf.parameterName}"
       value="${_csrf.token}"/>
```

### ASP.NET MVC

```csharp
// In form
@Html.AntiForgeryToken()

// In controller
[HttpPost]
[ValidateAntiForgeryToken]
public ActionResult Action(Model model) { }
```

### Django

```python
# In template
<form method="post">
    {% csrf_token %}
</form>

# Automatically validated by middleware
```

---

## 💡 Best Practices Summary

1. **Use CSRF tokens** - Synchronizer token pattern
2. **POST for state changes** - Never use GET for modifications
3. **SameSite cookies** - Set `SameSite=Strict` or `Lax`
4. **Framework protection** - Enable built-in CSRF protection
5. **Re-authenticate** - For sensitive operations
6. **Custom headers** - Use `X-Requested-With` for AJAX
7. **Validate server-side** - Never trust client-side validation

---

**Always use CSRF tokens for state-changing requests!**
