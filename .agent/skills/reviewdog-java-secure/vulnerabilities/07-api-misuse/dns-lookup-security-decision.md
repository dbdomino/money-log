# DNS Lookup in Security Decision

## Metadata
- **CWE ID**: CWE-350, CWE-247
- **Severity**: High
- **OWASP Category**: A07:2021 - Identification and Authentication Failures
- **Detection Difficulty**: Medium

## Description

Using DNS lookups (reverse DNS resolution or hostname resolution) to make security decisions is fundamentally flawed because DNS data can be easily spoofed or manipulated by attackers. When an application trusts hostname information obtained through DNS queries (like `getCanonicalHostName()`, `gethostbyaddr()`, or `Dns.GetHostByAddress()`) to determine whether to grant access or trust a connection, attackers can manipulate DNS records to bypass these security checks.

The vulnerability occurs because:
- DNS responses can be spoofed or poisoned
- Attackers can control DNS records for domains they own
- Reverse DNS lookups can return attacker-controlled hostnames
- DNS data is not authenticated by default (without DNSSEC)
- The mapping between IP addresses and hostnames is not trustworthy
- DNS cache poisoning can affect local DNS resolvers

Security decisions should be based on:
- IP address whitelists (not hostnames)
- Cryptographic authentication (certificates, tokens)
- Pre-shared secrets or keys
- Mutual TLS with certificate validation

## Vulnerable Code Examples

### Vulnerable Java (Reverse DNS Lookup)
```java
import java.net.InetAddress;
import java.net.UnknownHostException;

public class TrustManager {
    private static final String TRUSTED_DOMAIN = "trustme.com";

    // VULNERABLE: Makes security decision based on DNS lookup
    public boolean isTrustedHost(String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);

            // VULNERABLE: getCanonicalHostName() performs reverse DNS lookup
            String hostname = addr.getCanonicalHostName();

            System.out.println("Resolved IP " + ipAddress + " to hostname: " + hostname);

            // VULNERABLE: Trust decision based on DNS data
            if (hostname.endsWith(TRUSTED_DOMAIN)) {
                System.out.println("Trusted host detected: " + hostname);
                return true;
            }

            return false;

        } catch (UnknownHostException e) {
            System.err.println("Failed to resolve IP: " + ipAddress);
            return false;
        }
    }

    public void grantAccess(String clientIP) {
        // VULNERABLE: Security decision based on DNS
        if (isTrustedHost(clientIP)) {
            // Grant privileged access
            performPrivilegedOperation();
        } else {
            System.out.println("Access denied for " + clientIP);
        }
    }

    private void performPrivilegedOperation() {
        System.out.println("Granting privileged access...");
        // Execute sensitive operations
    }
}

// Attacker can exploit this:
// 1. Set up malicious server at IP 10.0.0.1
// 2. Configure reverse DNS: 10.0.0.1 -> evil.trustme.com
// 3. Application resolves 10.0.0.1 to evil.trustme.com
// 4. Hostname ends with "trustme.com" - access granted!
```

### Vulnerable C# (DNS-based Authentication)
```csharp
using System;
using System.Net;
using System.Net.Sockets;

public class SecurityValidator {
    private const string TRUSTED_DOMAIN = "trust.com";

    // VULNERABLE: Uses DNS lookup for security decision
    public bool ValidateClient(string hostIPAddress) {
        try {
            IPAddress ipAddress = IPAddress.Parse(hostIPAddress);

            // VULNERABLE: Reverse DNS lookup
            IPHostEntry hostInfo = Dns.GetHostEntry(ipAddress);
            string hostName = hostInfo.HostName;

            Console.WriteLine($"Resolved {hostIPAddress} to {hostName}");

            // VULNERABLE: Trust based on hostname suffix
            if (hostName.EndsWith(TRUSTED_DOMAIN, StringComparison.OrdinalIgnoreCase)) {
                Console.WriteLine($"Trusted client: {hostName}");
                return true;
            }

            return false;

        } catch (SocketException ex) {
            Console.WriteLine($"DNS lookup failed: {ex.Message}");
            return false;
        }
    }

    public void ProcessRequest(string clientIP, string request) {
        // VULNERABLE: Security gate based on DNS
        if (ValidateClient(clientIP)) {
            ExecutePrivilegedCommand(request);
        } else {
            Console.WriteLine("Unauthorized access attempt");
        }
    }

    private void ExecutePrivilegedCommand(string command) {
        Console.WriteLine($"Executing: {command}");
        // Execute privileged operations
    }
}
```

### Vulnerable C (Legacy Code)
```c
#include <stdio.h>
#include <string.h>
#include <netdb.h>
#include <arpa/inet.h>

#define TRUSTED_HOST "trustme.example.com"

// VULNERABLE: Uses gethostbyaddr for security decision
int is_trusted_host(struct sockaddr_in *client_addr) {
    struct hostent *hp;
    char *trusted_host = TRUSTED_HOST;

    // VULNERABLE: Reverse DNS lookup
    hp = gethostbyaddr(
        (char *)&client_addr->sin_addr,
        sizeof(client_addr->sin_addr),
        AF_INET
    );

    if (hp == NULL) {
        fprintf(stderr, "DNS lookup failed\n");
        return 0;
    }

    printf("Resolved to hostname: %s\n", hp->h_name);

    // VULNERABLE: Security decision based on hostname
    if (hp && !strncmp(hp->h_name, trusted_host, strlen(trusted_host))) {
        printf("Trusted host: %s\n", hp->h_name);
        return 1;  // Trusted
    }

    return 0;  // Not trusted
}

void handle_connection(struct sockaddr_in *client_addr) {
    // VULNERABLE: Grant access based on DNS
    if (is_trusted_host(client_addr)) {
        printf("Granting privileged access\n");
        execute_privileged_operation();
    } else {
        printf("Access denied\n");
    }
}

void execute_privileged_operation() {
    // Sensitive operations
    printf("Executing privileged operation...\n");
}
```

### Vulnerable Java (Servlet Example)
```java
import javax.servlet.http.*;
import java.io.*;
import java.net.*;

public class AdminServlet extends HttpServlet {
    private static final String TRUSTED_DOMAIN = "corp.example.com";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String remoteAddr = request.getRemoteAddr();

        // VULNERABLE: DNS-based access control
        if (isTrustedClient(remoteAddr)) {
            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            // Expose sensitive admin functionality
            out.println("<html><body>");
            out.println("<h1>Admin Panel</h1>");
            out.println("<p>Welcome, trusted user!</p>");
            out.println("<a href='?action=delete_all'>Delete All Users</a>");
            out.println("</body></html>");
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
        }
    }

    // VULNERABLE: DNS lookup for authentication
    private boolean isTrustedClient(String ipAddress) {
        try {
            InetAddress addr = InetAddress.getByName(ipAddress);
            String hostname = addr.getCanonicalHostName();

            return hostname.endsWith(TRUSTED_DOMAIN);

        } catch (UnknownHostException e) {
            return false;
        }
    }
}
```

## Secure Code Examples

### Secure Java (IP Whitelist)
```java
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class SecureTrustManager {
    // SECURE: Use IP address whitelist
    private static final Set<String> TRUSTED_IPS = new HashSet<>(Arrays.asList(
        "192.168.1.100",
        "192.168.1.101",
        "10.0.0.50"
    ));

    // Alternative: Use IP ranges (CIDR notation)
    private static final List<String> TRUSTED_NETWORKS = Arrays.asList(
        "192.168.1.0/24",
        "10.0.0.0/8"
    );

    // SECURE: Trust based on IP address, not DNS
    public boolean isTrustedHost(String ipAddress) {
        // Validate IP format first
        if (!isValidIP(ipAddress)) {
            return false;
        }

        // Check against whitelist
        if (TRUSTED_IPS.contains(ipAddress)) {
            System.out.println("Trusted IP: " + ipAddress);
            return true;
        }

        // Check against network ranges
        if (isInTrustedNetwork(ipAddress)) {
            System.out.println("IP in trusted network: " + ipAddress);
            return true;
        }

        return false;
    }

    public void grantAccess(String clientIP) {
        if (isTrustedHost(clientIP)) {
            performPrivilegedOperation();
        } else {
            System.out.println("Access denied for " + clientIP);
        }
    }

    private boolean isValidIP(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return ip.equals(addr.getHostAddress());
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isInTrustedNetwork(String ipAddress) {
        for (String network : TRUSTED_NETWORKS) {
            if (ipMatchesCIDR(ipAddress, network)) {
                return true;
            }
        }
        return false;
    }

    private boolean ipMatchesCIDR(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String networkIP = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);

            InetAddress addr = InetAddress.getByName(ip);
            InetAddress network = InetAddress.getByName(networkIP);

            byte[] addrBytes = addr.getAddress();
            byte[] networkBytes = network.getAddress();

            int mask = -1 << (32 - prefixLength);

            int addrInt = ((addrBytes[0] & 0xFF) << 24) |
                         ((addrBytes[1] & 0xFF) << 16) |
                         ((addrBytes[2] & 0xFF) << 8) |
                         (addrBytes[3] & 0xFF);

            int networkInt = ((networkBytes[0] & 0xFF) << 24) |
                            ((networkBytes[1] & 0xFF) << 16) |
                            ((networkBytes[2] & 0xFF) << 8) |
                            (networkBytes[3] & 0xFF);

            return (addrInt & mask) == (networkInt & mask);

        } catch (Exception e) {
            return false;
        }
    }

    private void performPrivilegedOperation() {
        System.out.println("Executing privileged operation");
    }
}
```

### Secure C# (IP-based Validation)
```csharp
using System;
using System.Net;
using System.Collections.Generic;
using System.Linq;

public class SecureValidator {
    // SECURE: IP address whitelist
    private static readonly HashSet<string> TrustedIPs = new HashSet<string> {
        "192.168.1.100",
        "192.168.1.101",
        "10.0.0.50"
    };

    // SECURE: Validate based on IP address
    public bool ValidateClient(string clientIP) {
        // Validate IP format
        if (!IPAddress.TryParse(clientIP, out IPAddress ipAddress)) {
            Console.WriteLine($"Invalid IP address format: {clientIP}");
            return false;
        }

        // Check against whitelist
        if (TrustedIPs.Contains(clientIP)) {
            Console.WriteLine($"Trusted IP: {clientIP}");
            return true;
        }

        Console.WriteLine($"Untrusted IP: {clientIP}");
        return false;
    }

    public void ProcessRequest(string clientIP, string request) {
        if (ValidateClient(clientIP)) {
            ExecutePrivilegedCommand(request);
        } else {
            Console.WriteLine("Access denied");
        }
    }

    private void ExecutePrivilegedCommand(string command) {
        Console.WriteLine($"Executing: {command}");
    }
}

// Alternative: IP range checking
public class IPRangeValidator {
    public bool IsInTrustedRange(string clientIP) {
        if (!IPAddress.TryParse(clientIP, out IPAddress ip)) {
            return false;
        }

        // Check if IP is in trusted subnet (e.g., 192.168.1.0/24)
        byte[] ipBytes = ip.GetAddressBytes();

        return ipBytes[0] == 192 &&
               ipBytes[1] == 168 &&
               ipBytes[2] == 1;
    }
}
```

### Secure C (IP Comparison)
```c
#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>

// SECURE: IP address whitelist
#define TRUSTED_IP "192.168.1.100"

// SECURE: Compare IP addresses, not hostnames
int is_trusted_host(struct sockaddr_in *client_addr) {
    char client_ip[INET_ADDRSTRLEN];
    char *trusted_ip = TRUSTED_IP;

    // Convert IP address to string
    inet_ntop(AF_INET, &(client_addr->sin_addr), client_ip, INET_ADDRSTRLEN);

    printf("Client IP: %s\n", client_ip);

    // SECURE: Compare IP addresses directly
    if (strcmp(client_ip, trusted_ip) == 0) {
        printf("Trusted IP address: %s\n", client_ip);
        return 1;  // Trusted
    }

    printf("Untrusted IP address: %s\n", client_ip);
    return 0;  // Not trusted
}

void handle_connection(struct sockaddr_in *client_addr) {
    if (is_trusted_host(client_addr)) {
        execute_privileged_operation();
    } else {
        printf("Access denied\n");
    }
}

void execute_privileged_operation() {
    printf("Executing privileged operation...\n");
}
```

### Secure Java (Certificate-based Authentication)
```java
import javax.net.ssl.*;
import java.security.cert.*;
import java.util.*;

public class CertificateBasedAuth {
    private static final Set<String> TRUSTED_CERT_FINGERPRINTS = new HashSet<>(Arrays.asList(
        "SHA256:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
    ));

    // SECURE: Cryptographic authentication via client certificates
    public boolean authenticateClient(SSLSession session) {
        try {
            // Get client certificates
            Certificate[] certs = session.getPeerCertificates();

            if (certs == null || certs.length == 0) {
                System.out.println("No client certificate provided");
                return false;
            }

            X509Certificate clientCert = (X509Certificate) certs[0];

            // Verify certificate is not expired
            clientCert.checkValidity();

            // Check certificate fingerprint
            String fingerprint = getCertificateFingerprint(clientCert);

            if (TRUSTED_CERT_FINGERPRINTS.contains(fingerprint)) {
                System.out.println("Valid client certificate");
                return true;
            }

            System.out.println("Unknown client certificate");
            return false;

        } catch (SSLPeerUnverifiedException e) {
            System.err.println("Peer not verified: " + e.getMessage());
            return false;
        } catch (CertificateException e) {
            System.err.println("Certificate invalid: " + e.getMessage());
            return false;
        }
    }

    private String getCertificateFingerprint(X509Certificate cert) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getEncoded();
            md.update(der);
            byte[] digest = md.digest();

            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02X:", b));
            }

            return "SHA256:" + hexString.substring(0, hexString.length() - 1);

        } catch (Exception e) {
            return null;
        }
    }
}
```

### Secure Java (Servlet with IP Whitelist)
```java
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

public class SecureAdminServlet extends HttpServlet {
    // SECURE: IP whitelist
    private static final Set<String> ADMIN_IPS = new HashSet<>(Arrays.asList(
        "192.168.1.100",
        "10.0.0.50"
    ));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String remoteAddr = request.getRemoteAddr();

        // SECURE: IP-based access control
        if (!isAdminIP(remoteAddr)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            logUnauthorizedAccess(remoteAddr);
            return;
        }

        // Additional authentication check
        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Not authenticated");
            return;
        }

        // Render admin panel
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>Admin Panel</h1>");
        out.println("<p>Welcome, administrator!</p>");
        out.println("</body></html>");
    }

    private boolean isAdminIP(String ipAddress) {
        return ADMIN_IPS.contains(ipAddress);
    }

    private void logUnauthorizedAccess(String ip) {
        System.err.println("[SECURITY] Unauthorized admin access attempt from: " + ip);
    }
}
```

## Detection Methods

### Static Analysis
```bash
# Find DNS lookup methods
grep -r "getCanonicalHostName\|gethostbyaddr\|GetHostByAddress\|GetHostEntry" \
  --include="*.java" --include="*.cs" --include="*.c" .

# Find security decisions based on hostnames
grep -r "endsWith.*\.com\|contains.*\.org" --include="*.java" .

# Find DNS-related imports
grep -r "import.*InetAddress\|using System.Net.Dns" .
```

### Code Review Checklist
- [ ] No security decisions based on DNS lookups
- [ ] IP whitelists used instead of hostname checks
- [ ] Certificate-based authentication for sensitive operations
- [ ] No reverse DNS resolution for access control
- [ ] Proper validation of IP addresses
- [ ] Security logging for access attempts

### Runtime Detection
```java
// Monitor for DNS-based security decisions
public aspect DNSSecurityMonitor {
    pointcut dnsLookup() :
        call(* InetAddress.getCanonicalHostName()) ||
        call(* InetAddress.getHostName());

    pointcut securityDecision() :
        execution(* *.isTrusted*(..)) ||
        execution(* *.validate*(..)) ||
        execution(* *.authenticate*(..));

    after() returning : dnsLookup() && cflow(securityDecision()) {
        System.err.println("[WARNING] DNS lookup in security decision detected!");
        Thread.dumpStack();
    }
}
```

## References

### CWE
- [CWE-350: Reliance on Reverse DNS Resolution for a Security-Critical Action](https://cwe.mitre.org/data/definitions/350.html)
- [CWE-247: Reliance on DNS Lookups in a Security Decision](https://cwe.mitre.org/data/definitions/247.html)

### OWASP
- [OWASP Top 10 2021 A07:2021 - Identification and Authentication Failures](https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

### Additional Resources
- [CERT Oracle Secure Coding Standard for Java - MSC61-J](https://wiki.sei.cmu.edu/confluence/display/java/MSC61-J)
- RFC 1912: Common DNS Operational and Configuration Errors
- DNS Security Extensions (DNSSEC)

## Security Checklist

### For Developers
- [ ] Use IP address whitelists for access control
- [ ] Never trust DNS lookups for security decisions
- [ ] Implement certificate-based authentication
- [ ] Use mutual TLS for service-to-service communication
- [ ] Validate and sanitize IP addresses
- [ ] Log all access control decisions
- [ ] Consider using API keys or tokens
- [ ] Implement defense in depth (multiple authentication layers)

### For Code Reviewers
- [ ] Verify no DNS lookups in security-critical code
- [ ] Check that IP whitelists are properly implemented
- [ ] Confirm certificate validation is correct
- [ ] Review access control logic for DNS dependencies
- [ ] Validate that hostnames are not used for trust decisions
- [ ] Check for proper error handling in authentication

### For Security Auditors
- [ ] Identify all authentication and authorization mechanisms
- [ ] Test for DNS spoofing vulnerabilities
- [ ] Verify IP-based controls can't be bypassed
- [ ] Check for DNS cache poisoning risks
- [ ] Validate certificate pinning implementation
- [ ] Review logs for suspicious access patterns
- [ ] Test with DNS manipulation tools
