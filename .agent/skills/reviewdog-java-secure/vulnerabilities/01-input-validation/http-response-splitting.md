# HTTP Response Splitting (CWE-113)

**Severity**: 🟠 HIGH
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

HTTP Response Splitting occurs when parameters included in HTTP requests are sent back to the user in HTTP response headers (e.g., Set-Cookie), and the input values contain newline characters such as CR (Carriage Return) or LF (Line Feed), causing the HTTP response to be split into two or more responses. In this case, an attacker can use newline characters to terminate the first response and inject malicious code into the second response, enabling XSS and cache poisoning attacks.

### Impact

**Potential consequences:**
- Cross-Site Scripting (XSS) attacks
- Cache poisoning (Web cache poisoning)
- Session hijacking
- Phishing attacks
- Credential theft
- Bypassing security controls

---

## Security Measures

### Key Principles

When including request parameter values in HTTP response headers (e.g., Set-Cookie), remove newline characters such as CR and LF.

**Primary Defenses:**

1. **Remove CRLF Characters**
   - Filter out `\r` (Carriage Return - 0x0D)
   - Filter out `\n` (Line Feed - 0x0A)
   - Remove before adding to response headers

2. **Input Validation**
   - Whitelist allowed characters for header values
   - Reject inputs containing newlines
   - Validate header name and value format

3. **Use Safe APIs**
   - Modern frameworks auto-encode headers
   - Use framework methods (don't set headers manually)
   - HTTP/2 prevents response splitting (binary protocol)

4. **Output Encoding**
   - URL-encode header values
   - HTML-encode when headers displayed
   - Use proper encoding for context

---

## Code Examples

### Attack Scenario

External input values are used to set the returned cookie value. If an attacker sets the lastLogin value to `Wiley Hacker\r\nHTTP/1.1 200 OK\r\n`, the response is split, and the attacker can freely modify the body of the split response.

**Normal Cookie Setting:**
```
Set-Cookie: lastLogin=2024-01-15
```

**Attack Input:**
```
lastLogin=Wiley%20Hacker%0d%0aHTTP/1.1%20200%20OK%0d%0a
```

**Resulting Response (Split):**
```
HTTP/1.1 200 OK
Set-Cookie: lastLogin=Wiley Hacker
HTTP/1.1 200 OK
...
```

The second HTTP response is completely controlled by the attacker.

---

### ❌ Vulnerable Code

#### Java - No CRLF Filtering

```java
// Using externally received values without validation is unsafe
String lastLogin = request.getParameter("last_login");
if (lastLogin == null || "".equals(lastLogin)) {
    return;
}

// Cookies are delivered via the Set-Cookie response header, so newline character validation is required
Cookie c = new Cookie("LASTLOGIN", lastLogin);
c.setMaxAge(1000);
c.setSecure(true);
response.addCookie(c);
response.setContentType("text/html");
```

**Problems:**
1. User input (`lastLogin`) used directly in cookie
2. No filtering of CRLF characters
3. No validation of input format
4. Vulnerable to HTTP response splitting

**Attack Example:**
```
?last_login=value%0d%0aSet-Cookie: admin=true

# Results in:
Set-Cookie: LASTLOGIN=value
Set-Cookie: admin=true
```

---

### ✅ Secure Code

#### Java - CRLF Filtering

```java
String lastLogin = request.getParameter("last_login");
if (lastLogin == null || "".equals(lastLogin)) {
    return;
}

// Validate externally received values through filtering or use them carefully
lastLogin = lastLogin.replaceAll("\r", "").replaceAll("\n", "");

Cookie c = new Cookie("LASTLOGIN", lastLogin);
c.setMaxAge(1000);
c.setSecure(true);
response.addCookie(c);
response.setContentType("text/html");
```

**Security Features:**
1. Remove `\r` (Carriage Return)
2. Remove `\n` (Line Feed)
3. Filter applied before setting cookie
4. Prevents response splitting

---

#### ✅ Better Practice - Complete Validation

```java
public class SecureHeaderHandler {

    // Whitelist pattern: only allow safe characters
    private static final Pattern SAFE_HEADER_VALUE =
        Pattern.compile("^[a-zA-Z0-9\\-_=.]+$");

    public void setSecureCookie(HttpServletRequest request,
                                HttpServletResponse response) {

        String lastLogin = request.getParameter("last_login");

        // 1. Null/empty check
        if (lastLogin == null || lastLogin.trim().isEmpty()) {
            return;
        }

        // 2. Length validation
        if (lastLogin.length() > 100) {
            throw new IllegalArgumentException("Value too long");
        }

        // 3. Remove CRLF characters
        String sanitized = sanitizeCRLF(lastLogin);

        // 4. Whitelist validation
        if (!SAFE_HEADER_VALUE.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Invalid characters in value");
        }

        // 5. URL encode for extra safety
        String encoded = URLEncoder.encode(sanitized, StandardCharsets.UTF_8);

        // 6. Set cookie with safe value
        Cookie cookie = new Cookie("LASTLOGIN", encoded);
        cookie.setMaxAge(1000);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");

        response.addCookie(cookie);
    }

    private String sanitizeCRLF(String input) {
        if (input == null) {
            return null;
        }

        // Remove all forms of line breaks
        return input
            .replaceAll("\\r", "")     // Carriage Return
            .replaceAll("\\n", "")     // Line Feed
            .replaceAll("\\r\\n", "")  // CRLF
            .replaceAll("%0d", "")     // URL-encoded CR
            .replaceAll("%0a", "")     // URL-encoded LF
            .replaceAll("%0D", "")     // URL-encoded CR (uppercase)
            .replaceAll("%0A", "");    // URL-encoded LF (uppercase)
    }

    // Safe method to add custom headers
    public void addSafeHeader(HttpServletResponse response,
                              String headerName, String headerValue) {

        // Validate header name
        if (!headerName.matches("^[a-zA-Z0-9-]+$")) {
            throw new IllegalArgumentException("Invalid header name");
        }

        // Sanitize header value
        String safeValue = sanitizeCRLF(headerValue);

        // Additional validation
        if (safeValue.contains(":") || safeValue.contains(" ")) {
            throw new IllegalArgumentException("Invalid characters in header value");
        }

        response.setHeader(headerName, safeValue);
    }
}
```

**Security Features:**
1. Null and length validation
2. CRLF removal (multiple formats)
3. Whitelist character validation
4. URL encoding
5. HttpOnly and Secure flags set
6. Separate validation for header names

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-113: Improper Neutralization of CRLF Sequences in HTTP Headers ('HTTP Response Splitting')**
   MITRE, https://cwe.mitre.org/data/definitions/113.html

### OWASP

② **HTTP Response Splitting**
   OWASP, https://owasp.org/www-community/attacks/HTTP_Response_Splitting

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find response header manipulation
grep -r "response.setHeader.*request" .
grep -r "response.addHeader.*getParameter" .
grep -r "addCookie.*request" .

# Find redirect operations
grep -r "sendRedirect.*request" .

# Check for CRLF filtering
grep -r "replaceAll.*\\\\r" .
grep -r "replaceAll.*\\\\n" .
```

---

## ✅ Security Checklist

- [ ] All user input sanitized before adding to headers
- [ ] CRLF characters removed (`\r`, `\n`)
- [ ] URL-encoded CRLF removed (`%0d`, `%0a`)
- [ ] Whitelist validation for header values
- [ ] Use framework methods for setting headers
- [ ] HTTP/2 enabled (prevents response splitting)
- [ ] HttpOnly flag set on cookies
- [ ] Secure flag set on cookies
- [ ] Input length limits enforced
- [ ] HTTP response splitting testing completed

---

## 🎯 Attack Techniques

### Basic Response Splitting

```
# Normal input
lastLogin=2024-01-15

# Attack input
lastLogin=test%0d%0aSet-Cookie: admin=true

# Result
Set-Cookie: lastLogin=test
Set-Cookie: admin=true
```

### Complete Response Injection

```
# Attack input
lastLogin=test%0d%0aHTTP/1.1 200 OK%0d%0aContent-Type: text/html%0d%0a%0d%0a<html><script>alert('XSS')</script></html>

# Result (split into two responses)
Response 1:
Set-Cookie: lastLogin=test

Response 2:
HTTP/1.1 200 OK
Content-Type: text/html

<html><script>alert('XSS')</script></html>
```

### Cache Poisoning

```
# Attack input
param=value%0d%0aContent-Length: 0%0d%0a%0d%0aHTTP/1.1 200 OK%0d%0aContent-Type: text/html%0d%0aContent-Length: 25%0d%0a%0d%0a<script>alert('XSS')</script>

# Poison cache with malicious content
```

---

## 🚨 CRLF Variations to Filter

```java
// All CRLF representations to remove:
\r          // Carriage Return (0x0D)
\n          // Line Feed (0x0A)
\r\n        // CRLF sequence
%0d         // URL-encoded CR
%0a         // URL-encoded LF
%0D         // URL-encoded CR (uppercase)
%0A         // URL-encoded LF (uppercase)
%0d%0a      // URL-encoded CRLF
\u000d      // Unicode CR
\u000a      // Unicode LF
```

**Comprehensive Filter:**
```java
public String removeCRLF(String input) {
    if (input == null) return null;

    return input
        .replaceAll("\\r\\n|\\r|\\n", "")     // Regular CRLF
        .replaceAll("%0[dD]%0[aA]", "")       // URL-encoded CRLF
        .replaceAll("%0[dD]|%0[aA]", "")      // URL-encoded CR/LF
        .replaceAll("\\u000[dD]|\\u000[aA]", "");  // Unicode
}
```

---

## 💡 Modern Protections

### HTTP/2

HTTP/2 uses binary framing, which prevents response splitting:
```java
// Enable HTTP/2 in server configuration
// Response splitting not possible with binary protocol
```

### Framework Protection

Most modern frameworks automatically filter CRLF:

**Spring Framework:**
```java
// Spring automatically encodes header values
response.setHeader("X-Custom", userInput);
// CRLF characters are automatically removed/encoded
```

**Servlet 3.0+:**
```java
// IllegalArgumentException thrown if CRLF detected
response.setHeader("X-Custom", userInput);
```

---

## 🚨 Common Mistakes

1. **Only Filtering `\n`**
   ```java
   // DON'T: Only filter LF
   value = value.replaceAll("\n", "");
   // Still vulnerable to \r

   // DO: Filter both CR and LF
   value = value.replaceAll("\r", "").replaceAll("\n", "");
   ```

2. **Missing URL-Encoded CRLF**
   ```java
   // DON'T: Only filter literal CRLF
   value = value.replaceAll("\r\n", "");
   // Attacker uses: %0d%0a

   // DO: Filter encoded forms too
   value = value.replaceAll("%0[dD]%0[aA]", "");
   ```

3. **Setting Headers Manually**
   ```java
   // DON'T: Manual header construction
   String header = "Set-Cookie: session=" + userInput;
   // Vulnerable to splitting

   // DO: Use framework methods
   Cookie cookie = new Cookie("session", sanitize(userInput));
   response.addCookie(cookie);
   ```

---

## 💡 Best Practices Summary

1. **Remove CRLF** - Filter `\r`, `\n`, and encoded variants
2. **Whitelist validation** - Only allow safe characters
3. **Use frameworks** - Leverage built-in protections
4. **HTTP/2** - Enable HTTP/2 for binary framing
5. **Never construct headers manually** - Use framework methods
6. **Set security flags** - HttpOnly, Secure on cookies
7. **Test thoroughly** - Include response splitting in security tests

---

**Always remove CRLF characters from HTTP header values!**
