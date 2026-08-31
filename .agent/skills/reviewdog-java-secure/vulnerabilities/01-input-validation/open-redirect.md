# Open Redirect (CWE-601)

**Severity**: 🟠 HIGH
**Category**: Input Validation & Representation
**OWASP Top 10**: A01:2021 – Broken Access Control

---

## Overview

### Attack Description

Unvalidated redirects occur when an application accepts untrusted URL input and redirects users to that location without proper validation. A server program that uses user input values as the address of an external site to automatically redirect to can be exposed to phishing attacks. Attackers can exploit this to redirect users to malicious sites (phishing, malware) while making it appear to come from a legitimate trusted domain.

### Impact

Although it may seem safe because the client transmits the URL address for the redirect, an attacker can modify the request to redirect users to a dangerous URL.

**Potential consequences:**
- Phishing attacks (fake login pages)
- Malware distribution
- Credential theft
- User trust exploitation
- Session hijacking
- Social engineering attacks

---

## Security Measures

### Key Principles

Manage the URLs and domains of external sites for automatic redirect using a whitelist. When using user input values as the address for automatic redirect, verify that the input value exists in the whitelist.

**Primary Defenses:**

1. **Whitelist URLs/Domains**
   - Maintain whitelist of allowed redirect URLs
   - Validate redirect URL against whitelist
   - Only allow relative URLs (no external redirects)
   - Reject any URL not in whitelist

2. **Use Indirect References**
   - Map user input to internal ID/token
   - Lookup actual URL from safe mapping
   - Never use user input directly as URL

3. **Validate URL Format**
   - Check URL starts with allowed protocol (https://)
   - Validate domain matches allowed list
   - Use `URI` class for proper parsing
   - Reject malformed URLs

4. **User Warning**
   - Show warning before external redirects
   - Display destination URL for user confirmation
   - Make external links visually distinct

---

## Code Examples

### Attack Scenario

If the following code exists on the server, an attacker can use a link like the one below to redirect victims to phishing sites.

**Attack Example:**
```html
<a href="http://bank.example.com/redirect?url=http://attacker.example.net">Click</a>
```

User sees: `bank.example.com` (trusted domain)
Actually redirects to: `attacker.example.net` (malicious site)

**Real Attack Flow:**
1. Attacker sends phishing email with link to legitimate site
2. Link contains redirect parameter to malicious site
3. User clicks link, sees trusted domain in browser
4. Application redirects to attacker's fake login page
5. User enters credentials on fake page
6. Attacker steals credentials

---

### ❌ Vulnerable Code

#### Java - Direct Redirect with User Input

```java
String id = (String)session.getValue("id");
String bn = request.getParameter("gubun");
// URL received from external input is used for redirect without validation - unsafe
String rd = request.getParameter("redirect");

if (id.length() > 0) {
    String sql = "select level from customer where customer_id = ?";
    conn = db.getConnection();
    pstmt = conn.prepareStatement(sql);
    pstmt.setString(1, id);
    rs = pstmt.executeQuery();
    rs.next();

    if ("0".equals(rs.getString(1)) && "01AD".equals(bn)) {
        response.sendRedirect(rd);
        return;
    }
}
```

**Problems:**
1. `rd` parameter from user input used directly
2. No validation of redirect URL
3. No whitelist checking
4. Can redirect to any arbitrary site
5. Enables phishing attacks

**Attack Examples:**
```
/page?redirect=http://evil.com/fake-login
/page?redirect=//evil.com/malware
/page?redirect=javascript:alert(document.cookie)
```

---

### ❌ Vulnerable C#

```csharp
// External input URL is used for redirect without validation
string url = Request["dest"];
Response.Redirect(url);
```

---

### ✅ Secure Code

#### Java - Whitelist Validation

```java
// Restrict the range of URLs that can be redirected to, preventing redirection to phishing sites
String allowedUrl[] = {"/main.do", "/login.jsp", "list.do"};
......
String rd = request.getParameter("redirect");

try {
    rd = allowedUrl[Integer.parseInt(rd)];
} catch(NumberFormatException e) {
    return "Invalid access.";
} catch(ArrayIndexOutOfBoundsException e) {
    return "Invalid access.";
}

if (id.length() > 0) {
    ......
    if ("0".equals(rs.getString(1)) && "01AD".equals(bn)) {
        response.sendRedirect(rd);
        return;
    }
}
```

**Security Features:**
1. Whitelist of allowed URLs
2. User provides index, not URL
3. Array bounds checking
4. Exception handling for invalid input
5. No direct user control over destination

---

#### ✅ Java - Best Practice with URL Validation

```java
public class SecureRedirect {
    // Whitelist of allowed redirect URLs
    private static final List<String> ALLOWED_REDIRECTS = Arrays.asList(
        "/main.do",
        "/login.jsp",
        "/dashboard.do",
        "/profile.do"
    );

    // Whitelist of allowed external domains
    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
        "example.com",
        "trusted-partner.com"
    );

    public void safeRedirect(HttpServletRequest request,
                             HttpServletResponse response)
            throws IOException {
        String redirectUrl = request.getParameter("redirect");

        // Validate and sanitize
        if (redirectUrl == null || redirectUrl.isEmpty()) {
            response.sendRedirect("/default.jsp");
            return;
        }

        // Check if it's a relative URL (safest)
        if (redirectUrl.startsWith("/")) {
            // Validate against whitelist
            if (ALLOWED_REDIRECTS.contains(redirectUrl)) {
                response.sendRedirect(redirectUrl);
                return;
            }
        }

        // Check if it's an absolute URL
        try {
            URI uri = new URI(redirectUrl);

            // Only allow https
            if (!"https".equals(uri.getScheme())) {
                throw new SecurityException("Only HTTPS allowed");
            }

            // Validate domain against whitelist
            String host = uri.getHost();
            boolean allowed = ALLOWED_DOMAINS.stream()
                .anyMatch(domain -> host.equals(domain) ||
                          host.endsWith("." + domain));

            if (allowed) {
                response.sendRedirect(redirectUrl);
                return;
            }

        } catch (URISyntaxException e) {
            // Invalid URL format
        }

        // Default: reject and go to safe page
        response.sendRedirect("/error.jsp?msg=Invalid+redirect");
    }
}
```

**Security Features:**
1. Whitelist for internal URLs
2. Whitelist for external domains
3. URI parsing for validation
4. HTTPS-only for external redirects
5. Relative URL preferred (safest)
6. Default safe fallback

---

#### ✅ C# - Domain Validation

```csharp
public void AttackOpenRedirect()
{
    String url = Request["dest"];
    // Verify that the external input value is a local URL. When using MVC 3+ framework,
    // you can directly use Url.IsLocalUrl defined in System.Web.Mvc.
    if(isLocalUrl(url)) Response.Redirect(url);
}

private bool IsLocalUrl(string url)
{
    if(string.IsNullOrEmpty(url))
    {
        return false;
    }

    Uri absoluteUri;
    if(Uri.TryCreate(url, UriKind.Absolute, out absoluteUri))
    {
        return String.Equals(this.Request.Url.Host, absoluteUri.Host,
            StringComparison.OrdinalIgnoreCase);
    }
    else
    {
        bool isLocal = !url.StartsWith("http:",
            StringComparison.OrdinalIgnoreCase)
        && !url.StartsWith("https:",
            StringComparison.OrdinalIgnoreCase)
        && Uri.IsWellFormedUriString(url, UriKind.Relative);
        return isLocal;
    }
}
```

**Security Features:**
1. MVC 3+ provides `Url.IsLocalUrl()`
2. Validates URL is local (same domain)
3. Checks for absolute vs relative URLs
4. Rejects external redirects

---

#### ✅ Spring MVC - Best Practice

```java
@Controller
public class RedirectController {

    private static final String DEFAULT_REDIRECT = "/home";
    private static final Set<String> ALLOWED_PATHS = Set.of(
        "/dashboard", "/profile", "/settings"
    );

    @GetMapping("/redirect")
    public String handleRedirect(
            @RequestParam(required = false) String url,
            RedirectAttributes redirectAttributes) {

        // Null/empty check
        if (url == null || url.trim().isEmpty()) {
            return "redirect:" + DEFAULT_REDIRECT;
        }

        // Only allow relative URLs
        if (!url.startsWith("/")) {
            redirectAttributes.addFlashAttribute("error",
                "External redirects not allowed");
            return "redirect:" + DEFAULT_REDIRECT;
        }

        // Whitelist check
        if (ALLOWED_PATHS.contains(url)) {
            return "redirect:" + url;
        }

        // Default fallback
        redirectAttributes.addFlashAttribute("error",
            "Invalid redirect destination");
        return "redirect:" + DEFAULT_REDIRECT;
    }
}
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-601: URL Redirection to Untrusted Site**
   MITRE, http://cwe.mitre.org/data/definitions/601.html

### OWASP

② **Unvalidated Redirects and Forwards Cheat Sheet**
   OWASP, https://www.owasp.org/index.php/Unvalidated_Redirects_and_Forwards_Cheat_Sheet

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find redirect operations
grep -r "sendRedirect.*request" .
grep -r "sendRedirect.*getParameter" .
grep -r "Response.Redirect.*Request" .

# Find forward operations
grep -r "forward.*request\\.getParameter" .

# Find Location header manipulation
grep -r "setHeader.*Location.*request" .
```

---

## ✅ Security Checklist

- [ ] All redirect URLs validated against whitelist
- [ ] No user input used directly in redirects
- [ ] Only relative URLs allowed (or strict domain whitelist)
- [ ] External redirects require HTTPS
- [ ] URL format validated with URI/URL parsing
- [ ] User confirmation shown for external redirects
- [ ] Default safe redirect page configured
- [ ] Open redirect testing completed
- [ ] Logging of all redirect attempts
- [ ] Framework redirect validation enabled (Spring Security, etc.)

---

## 🎯 Open Redirect Attack Variations

### Attack Techniques

```bash
# Standard redirect
?redirect=http://evil.com

# Protocol-relative URL
?redirect=//evil.com

# URL encoding
?redirect=http%3A%2F%2Fevil.com

# Multiple slashes
?redirect=///evil.com

# Whitespace bypass
?redirect=http://evil.com%20

# JavaScript protocol
?redirect=javascript:alert(1)

# Data URI
?redirect=data:text/html,<script>...</script>

# Mixed case
?redirect=hTTp://evil.com

# @ symbol trick
?redirect=http://trusted.com@evil.com

# Subdomain confusion
?redirect=http://evil.com.attacker.com
```

### Defense Against All Variations

```java
public boolean isValidRedirect(String url) {
    if (url == null || url.trim().isEmpty()) {
        return false;
    }

    // Only allow relative URLs (safest)
    if (url.startsWith("/") && !url.startsWith("//")) {
        // Check whitelist
        return ALLOWED_PATHS.contains(url);
    }

    // If absolute URL needed, validate strictly
    try {
        URI uri = new URI(url);

        // Must have scheme
        if (uri.getScheme() == null) {
            return false;
        }

        // Only HTTPS
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        // Check domain whitelist
        String host = uri.getHost();
        if (host == null) {
            return false;
        }

        return ALLOWED_DOMAINS.contains(host.toLowerCase());

    } catch (URISyntaxException e) {
        return false;
    }
}
```

---

## 💡 Framework-Specific Solutions

### Spring Security

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .logout()
                .logoutSuccessUrl("/login?logout")
                // Spring validates redirect URLs automatically
            .and()
            .oauth2Login()
                .defaultSuccessUrl("/dashboard", true)
                // Forces absolute URL, prevents open redirect
            ;
    }
}
```

### ASP.NET MVC

```csharp
// Use built-in validation
if (Url.IsLocalUrl(returnUrl))
{
    return Redirect(returnUrl);
}
return RedirectToAction("Index", "Home");
```

---

## 🚨 Common Mistakes

1. **Blacklist Approach**
   ```java
   // DON'T: Try to block bad URLs
   if (!url.contains("evil.com")) {
       redirect(url); // Still vulnerable!
   }

   // DO: Only allow good URLs
   if (WHITELIST.contains(url)) {
       redirect(url);
   }
   ```

2. **Incomplete Validation**
   ```java
   // DON'T: Only check start of URL
   if (url.startsWith("http://trusted.com")) {
       redirect(url); // Vulnerable to: http://trusted.com@evil.com
   }

   // DO: Parse and validate properly
   URI uri = new URI(url);
   if ("trusted.com".equals(uri.getHost())) {
       redirect(url);
   }
   ```

3. **Client-Side Validation Only**
   ```javascript
   // DON'T: Trust client-side validation
   // Attacker can bypass JavaScript validation
   ```

---

## 💡 Best Practices Summary

1. **Prefer relative URLs** - `/page` instead of `http://domain.com/page`
2. **Use whitelist** - Never blacklist, always whitelist
3. **Validate on server** - Never trust client input
4. **Use indirect references** - Map IDs to URLs
5. **Show warnings** - Alert users to external redirects
6. **Log redirects** - Monitor for attack attempts
7. **Framework features** - Use built-in redirect validation

---

**Always validate redirects with whitelist - Use relative URLs when possible!**
