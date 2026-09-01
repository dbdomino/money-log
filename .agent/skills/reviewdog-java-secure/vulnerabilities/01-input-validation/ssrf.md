# Server-Side Request Forgery (SSRF) (CWE-918)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A10:2021 – Server-Side Request Forgery (SSRF)

---

## Overview

### Attack Description

Server-Side Request Forgery (SSRF) is a security vulnerability that occurs when user input values that have not undergone proper validation are used in server-to-server requests, enabling malicious actions. Attackers exploit this to access internal services, scan internal networks, bypass access controls, or exfiltrate sensitive data.

### Impact

When a web server exposed externally has vulnerable parameters, an attacker can forge URIs or requests to access restricted internal services or internal networks. Even within the same internal network, requests can be made by exploiting device authentication and verification mechanisms.

**Potential consequences:**
- Access to internal services (databases, admin panels)
- Port scanning of internal network
- Reading local files (`file://`)
- Bypassing firewall/ACL protections
- Cloud metadata service exploitation (AWS, Azure, GCP)
- Denial of Service
- Remote Code Execution (in some cases)

---

## Security Measures

### Key Principles

When using user input values for service calls to other systems within a trusted scope, filter the user input values using a whitelist approach.

If arbitrary URLs from users must be accepted, blacklist internal URLs for filtering. Additionally, even within the same internal network, ensure that requests are made only after verifying device authentication and authorization.

**Primary Defenses:**

1. **Whitelist Allowed Domains/IPs**
   - Maintain whitelist of allowed external domains
   - Only allow known, trusted destinations
   - Validate against whitelist before making requests

2. **Blacklist Internal Addresses**
   - Block private IP ranges: `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`
   - Block localhost: `127.0.0.1`, `::1`, `localhost`
   - Block link-local: `169.254.0.0/16`
   - Block cloud metadata: `169.254.169.254`

3. **URL Validation**
   - Parse and validate URL structure
   - Check protocol (only allow `http`/`https`)
   - Validate domain/IP against whitelist
   - Resolve DNS and check resolved IP

4. **Network Segmentation**
   - Isolate application servers from internal resources
   - Use firewalls to restrict outbound connections
   - Separate networks for different trust levels

5. **Use Indirect References**
   - Map user input to internal IDs
   - Lookup actual URL from safe mapping
   - Never use user input directly as URL

---

## Code Examples

### Attack Scenario

The following example is implemented to connect to web resources based on user input values without validation. In this case, an attacker can manipulate the URL to access internal servers and acquire data.

**Attack Examples:**

| Attack Type | Example Code |
|-------------|--------------|
| Acquiring sensitive internal network information | `http://site_example.com/connect?url=http://192.168.0.45/member/list.json` |
| Accessing externally blocked admin page | `http://site_example.com/connect?url=http://192.168.0.45/admin` |
| Bypassing domain restrictions to acquire sensitive information | `http://site_example.com/connect?url=http://site_example.com:x@192.168.0.45/member/list.json` |
| Bypassing filters using shortened URLs | `http://site_example.com/connect?url=http://bit.ly/sdk3kjhkl3` |
| Setting domain to internal IP to acquire sensitive information | `http://site_example.com/connect?url=http://internal.site.com/member/list.json` |
| Viewing files on the server | `http://site_example.com/connect?url=http://attack/fileview.html` |

---

### ❌ Vulnerable Code

#### Java - No URL Validation

```java
protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws
IOException {
    // Using user input value (url) without validation is unsafe
    URL url = new URL(req.getParameter("url"));
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
}
```

**Problems:**
1. User input (`url`) used directly without validation
2. No whitelist of allowed domains
3. No blocking of internal IP addresses
4. Can access any URL including internal resources

**Attack Examples:**
```java
// Access internal database
?url=http://localhost:3306/

// Access cloud metadata (AWS)
?url=http://169.254.169.254/latest/meta-data/

// Access internal admin panel
?url=http://192.168.1.100/admin

// Read local files (if supported)
?url=file:///etc/passwd
```

---

### ✅ Secure Code

#### Java - URL Whitelist Validation

```java
public class Connect {
    // Create a URL list in key-value format
    private Map<String, URL> urlMap;

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws
IOException {
        // Receive a key from the user for urlMap and look up the URL value from urlMap
        URL url = urlMap.get(req.getParameter("url"));
        // Create a connection using the value referenced from urlMap
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    }
}
```

**Security Features:**
1. Whitelist of allowed URLs stored in `urlMap`
2. User provides key/ID, not actual URL
3. Lookup URL from safe mapping
4. No direct user control over destination

---

#### ✅ Best Practice - Complete SSRF Protection

```java
public class SecureURLFetcher {

    // Whitelist of allowed domains
    private static final Set<String> ALLOWED_DOMAINS = Set.of(
        "api.trusted-partner.com",
        "cdn.example.com",
        "public-api.example.org"
    );

    // Blacklist of internal IP ranges
    private static final List<String> BLOCKED_IP_PATTERNS = Arrays.asList(
        "^10\\..*",              // 10.0.0.0/8
        "^172\\.(1[6-9]|2[0-9]|3[01])\\..*",  // 172.16.0.0/12
        "^192\\.168\\..*",       // 192.168.0.0/16
        "^127\\..*",             // 127.0.0.0/8 (localhost)
        "^169\\.254\\..*",       // 169.254.0.0/16 (link-local)
        "^0\\.0\\.0\\.0$",       // 0.0.0.0
        "^::1$",                 // IPv6 localhost
        "^fc00:.*",              // IPv6 private
        "^fe80:.*"               // IPv6 link-local
    );

    public String fetchURL(String userInput) throws IOException {
        // 1. Validate input not empty
        if (userInput == null || userInput.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        // 2. Parse URL
        URL url;
        try {
            url = new URL(userInput);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        // 3. Check protocol (only http/https)
        String protocol = url.getProtocol().toLowerCase();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new SecurityException(
                "Only HTTP and HTTPS protocols allowed");
        }

        // 4. Check domain against whitelist
        String host = url.getHost().toLowerCase();
        if (!ALLOWED_DOMAINS.contains(host)) {
            throw new SecurityException(
                "Domain not in whitelist: " + host);
        }

        // 5. Resolve DNS and check IP address
        InetAddress address = InetAddress.getByName(host);
        String ip = address.getHostAddress();

        // 6. Check if resolved IP is internal/private
        if (isInternalIP(ip)) {
            throw new SecurityException(
                "Access to internal IP address blocked: " + ip);
        }

        // 7. Make request with timeout
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);  // 5 second timeout
        conn.setReadTimeout(5000);
        conn.setInstanceFollowRedirects(false);  // Don't follow redirects

        // 8. Read response
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } finally {
            conn.disconnect();
        }
    }

    private boolean isInternalIP(String ip) {
        // Check against blacklist patterns
        for (String pattern : BLOCKED_IP_PATTERNS) {
            if (ip.matches(pattern)) {
                return true;
            }
        }

        // Additional checks for special cases
        try {
            InetAddress addr = InetAddress.getByName(ip);

            // Check if loopback
            if (addr.isLoopbackAddress()) {
                return true;
            }

            // Check if link-local
            if (addr.isLinkLocalAddress()) {
                return true;
            }

            // Check if site-local (private)
            if (addr.isSiteLocalAddress()) {
                return true;
            }

        } catch (UnknownHostException e) {
            return true;  // If can't resolve, block it
        }

        return false;
    }
}
```

**Security Features:**
1. Whitelist of allowed domains
2. Protocol validation (only HTTP/HTTPS)
3. DNS resolution and IP validation
4. Blacklist of internal IP ranges
5. Loopback/link-local/site-local detection
6. Connection timeouts
7. Disable automatic redirects
8. Comprehensive error handling

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-918: Server-Side Request Forgery (SSRF)**
   MITRE, https://cwe.mitre.org/data/definitions/918.html

### OWASP

② **Server Side Request Forgery**
   OWASP, https://owasp.org/www-community/attacks/Server_Side_Request_Forgery

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find URL connection with user input
grep -r "new URL.*request\\.getParameter" .
grep -r "HttpURLConnection.*request" .
grep -r "openConnection.*user" .

# Find HTTP client usage
grep -r "HttpClient.*execute" .
grep -r "RestTemplate.*exchange" .
grep -r "WebClient.*retrieve" .

# Find file access
grep -r "URL.*file://" .
```

---

## ✅ Security Checklist

- [ ] Whitelist of allowed external domains enforced
- [ ] Internal IP addresses blocked (10.x, 172.16.x, 192.168.x, 127.x)
- [ ] Cloud metadata endpoint blocked (`169.254.169.254`)
- [ ] Only HTTP/HTTPS protocols allowed
- [ ] DNS resolution performed and IP validated
- [ ] No automatic redirect following
- [ ] Connection timeouts configured
- [ ] User input never used directly as URL
- [ ] SSRF testing completed
- [ ] Network segmentation in place

---

## 🎯 Common SSRF Attack Targets

### Internal Services

```
# Database
http://localhost:3306/
http://localhost:5432/
http://localhost:27017/

# Admin panels
http://localhost:8080/admin
http://192.168.1.1/admin

# Internal APIs
http://internal-api.company.local/
```

### Cloud Metadata Services

```
# AWS
http://169.254.169.254/latest/meta-data/
http://169.254.169.254/latest/user-data/

# Azure
http://169.254.169.254/metadata/instance?api-version=2021-02-01

# Google Cloud
http://metadata.google.internal/computeMetadata/v1/
```

### Local Files

```
file:///etc/passwd
file:///c:/windows/win.ini
file:///proc/self/environ
```

---

## 🚨 Bypass Techniques

### IP Address Obfuscation

```
# Decimal format
http://2130706433/  (127.0.0.1)

# Hex format
http://0x7f000001/  (127.0.0.1)

# Octal format
http://0177.0.0.1/  (127.0.0.1)

# Mixed formats
http://0x7f.0.0.1/
```

### DNS Rebinding

```
# attacker.com resolves to:
# First request: 1.2.3.4 (public IP)
# Second request: 192.168.1.1 (internal IP)
```

### URL Redirects

```
# Open redirect on trusted domain
https://trusted.com/redirect?url=http://169.254.169.254/
```

### URL Parsing Confusion

```
# @ symbol tricks
http://trusted.com@evil.com/
http://evil.com#@trusted.com/

# Encoding tricks
http://127.0.0.1%2F%40trusted.com/
```

---

## 💡 Defense in Depth

### 1. Application Layer

```java
// Validate URL structure
if (!url.startsWith("http://") && !url.startsWith("https://")) {
    throw new SecurityException("Invalid protocol");
}

// Check whitelist
if (!ALLOWED_DOMAINS.contains(domain)) {
    throw new SecurityException("Domain not allowed");
}
```

### 2. Network Layer

```
# Firewall rules
- Block outbound connections to private IP ranges
- Allow only specific external destinations
- Use egress filtering
```

### 3. Infrastructure Layer

```
# AWS Security Group
- Only allow outbound to specific IPs/ports
- Block metadata endpoint from application servers
- Use VPC endpoints for AWS services
```

---

## 💡 Framework-Specific Solutions

### Spring RestTemplate

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory =
        new SimpleClientHttpRequestFactory();

    // Set timeouts
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(5000);

    RestTemplate restTemplate = new RestTemplate(factory);

    // Add interceptor for URL validation
    restTemplate.setInterceptors(Arrays.asList(
        new URLValidationInterceptor()
    ));

    return restTemplate;
}

public class URLValidationInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        URI uri = request.getURI();

        // Validate URL
        if (!isAllowedURL(uri)) {
            throw new SecurityException("URL not allowed: " + uri);
        }

        return execution.execute(request, body);
    }
}
```

---

## 🚨 Common Mistakes

1. **Only Checking Domain, Not IP**
   ```java
   // DON'T: Only check domain
   if (url.getHost().contains("internal")) {
       block();
   }
   // Attacker uses IP address instead: http://192.168.1.100

   // DO: Resolve and check IP
   InetAddress addr = InetAddress.getByName(url.getHost());
   if (isInternalIP(addr.getHostAddress())) {
       block();
   }
   ```

2. **Allowing Redirects**
   ```java
   // DON'T: Follow redirects automatically
   HttpURLConnection conn = (HttpURLConnection) url.openConnection();
   // Default: setInstanceFollowRedirects(true)

   // DO: Disable redirects
   conn.setInstanceFollowRedirects(false);
   ```

3. **Incomplete IP Blacklist**
   ```java
   // DON'T: Only block 127.0.0.1
   if (ip.equals("127.0.0.1")) block();

   // DO: Block all loopback and private ranges
   if (addr.isLoopbackAddress() ||
       addr.isLinkLocalAddress() ||
       addr.isSiteLocalAddress()) block();
   ```

---

## 💡 Best Practices Summary

1. **Whitelist approach** - Only allow known good domains
2. **Block internal IPs** - All private/loopback/link-local ranges
3. **Resolve DNS** - Check IP after domain resolution
4. **Disable redirects** - Prevent bypass via redirects
5. **Set timeouts** - Prevent DoS via slow connections
6. **Network segmentation** - Firewall outbound connections
7. **Monitor & log** - Track all external requests

---

**Always validate external URLs with whitelist - Block internal IPs!**
