# CWE Mapping - Java Secure Coding

**Complete mapping of all 47 vulnerabilities to CWE identifiers, OWASP Top 10, and severity levels**

**Based on KISA Secure Coding Guide 2021.12.29**

---

## Complete CWE Mapping Table

| # | English Title | CWE | OWASP Top 10 2021 | Severity | Category |
|---|---|---|---|---|---|
| 1 | SQL Injection | CWE-89 | A03:2021 Injection | CRITICAL | Input Validation |
| 2 | Code Injection | CWE-94 | A03:2021 Injection | CRITICAL | Input Validation |
| 3 | Path Traversal | CWE-22 | A01:2021 Broken Access | CRITICAL | Input Validation |
| 4 | Cross-Site Scripting (XSS) | CWE-79 | A03:2021 Injection | CRITICAL | Input Validation |
| 5 | OS Command Injection | CWE-78 | A03:2021 Injection | CRITICAL | Input Validation |
| 6 | Dangerous File Upload | CWE-434 | A04:2021 Insecure Design | HIGH | Input Validation |
| 7 | Open Redirect | CWE-601 | A01:2021 Broken Access | HIGH | Input Validation |
| 8 | XML External Entity (XXE) | CWE-611 | A05:2021 Security Misconfig | CRITICAL | Input Validation |
| 9 | XML Injection | CWE-91 | A03:2021 Injection | HIGH | Input Validation |
| 10 | LDAP Injection | CWE-90 | A03:2021 Injection | HIGH | Input Validation |
| 11 | Cross-Site Request Forgery (CSRF) | CWE-352 | A01:2021 Broken Access | CRITICAL | Input Validation |
| 12 | Server-Side Request Forgery (SSRF) | CWE-918 | A10:2021 SSRF | CRITICAL | Input Validation |
| 13 | HTTP Response Splitting | CWE-113 | A03:2021 Injection | MEDIUM | Input Validation |
| 14 | Integer Overflow | CWE-190 | A04:2021 Insecure Design | MEDIUM | Input Validation |
| 15 | Security Decision Based on Input | CWE-807 | A04:2021 Insecure Design | HIGH | Input Validation |
| 16 | Buffer Overflow | CWE-119 | A04:2021 Insecure Design | CRITICAL | Input Validation |
| 17 | Format String Injection | CWE-134 | A03:2021 Injection | HIGH | Input Validation |
| 18 | Missing Authentication | CWE-306 | A07:2021 Auth Failures | CRITICAL | Security Features |
| 19 | Improper Authorization | CWE-285 | A01:2021 Broken Access | CRITICAL | Security Features |
| 20 | Incorrect Permission Assignment | CWE-732 | A01:2021 Broken Access | HIGH | Security Features |
| 21 | Use of Weak Cryptography | CWE-327 | A02:2021 Crypto Failures | CRITICAL | Security Features |
| 22 | Unencrypted Sensitive Data | CWE-311 | A02:2021 Crypto Failures | CRITICAL | Security Features |
| 23 | Hardcoded Credentials | CWE-798 | A02:2021 Crypto Failures | CRITICAL | Security Features |
| 24 | Insufficient Key Length | CWE-326 | A02:2021 Crypto Failures | HIGH | Security Features |
| 25 | Weak Random Number | CWE-330 | A02:2021 Crypto Failures | HIGH | Security Features |
| 26 | Weak Password Requirements | CWE-521 | A07:2021 Auth Failures | HIGH | Security Features |
| 27 | Insufficient Verification | CWE-287 | A07:2021 Auth Failures | HIGH | Security Features |
| 28 | Insufficient Session Validation | CWE-613 | A07:2021 Auth Failures | HIGH | Security Features |
| 29 | Sensitive Cookie Exposure | CWE-539 | A02:2021 Crypto Failures | MEDIUM | Security Features |
| 30 | Sensitive Comment Information | CWE-615 | A05:2021 Security Misconfig | MEDIUM | Security Features |
| 31 | Hash Without Salt | CWE-759 | A02:2021 Crypto Failures | HIGH | Security Features |
| 32 | Download Without Integrity Check | CWE-494 | A08:2021 Integrity Failures | HIGH | Security Features |
| 33 | No Rate Limiting on Auth | CWE-307 | A07:2021 Auth Failures | HIGH | Security Features |
| 34 | Module Usage Restrictions | - | A04:2021 Insecure Design | MEDIUM | Security Features |
| 35 | Time-of-Check-Time-of-Use | CWE-362 | A04:2021 Insecure Design | HIGH | Time & State |
| 36 | Infinite Loop/Recursion | CWE-835 | A06:2021 Vuln Components | MEDIUM | Time & State |
| 37 | Error Message Information Leak | CWE-209 | A05:2021 Security Misconfig | HIGH | Error Handling |
| 38 | Missing Error Handling | CWE-391 | A04:2021 Insecure Design | MEDIUM | Error Handling |
| 39 | Improper Exception Handling | CWE-396 | A04:2021 Insecure Design | MEDIUM | Error Handling |
| 40 | Null Pointer Dereference | CWE-476 | A06:2021 Vuln Components | MEDIUM | Code Quality |
| 41 | Improper Resource Shutdown | CWE-404 | A06:2021 Vuln Components | MEDIUM | Code Quality |
| 42 | Session Data Exposure | CWE-488 | A01:2021 Broken Access | MEDIUM | Encapsulation |
| 43 | Leftover Debug Code | CWE-489 | A05:2021 Security Misconfig | MEDIUM | Encapsulation |
| 44 | Private Array Return | CWE-495 | A04:2021 Insecure Design | LOW | Encapsulation |
| 45 | Public Data Assignment | CWE-496 | A04:2021 Insecure Design | LOW | Encapsulation |
| 46 | DNS Lookup for Security Decision | CWE-350 | A05:2021 Security Misconfig | MEDIUM | API Misuse |
| 47 | Insecure API Usage | - | A05:2021 Security Misconfig | MEDIUM | API Misuse |

---

## Statistics by Category

### Input Validation
- **Total**: 17 vulnerabilities
- **Critical**: 7 (SQL Injection, XSS, Command Injection, XXE, CSRF, SSRF, Buffer Overflow)
- **High**: 6
- **Medium**: 4

### Security Features
- **Total**: 17 vulnerabilities
- **Critical**: 4 (Missing Auth, Improper Authz, Weak Crypto, Hardcoded Credentials)
- **High**: 9
- **Medium**: 4

### Time & State
- **Total**: 2 vulnerabilities
- **High**: 1 (TOCTOU)
- **Medium**: 1

### Error Handling
- **Total**: 3 vulnerabilities
- **High**: 1
- **Medium**: 2

### Code Quality
- **Total**: 2 vulnerabilities
- **Medium**: 2

### Encapsulation
- **Total**: 4 vulnerabilities
- **Medium**: 2
- **Low**: 2

### API Misuse
- **Total**: 2 vulnerabilities
- **Medium**: 2

---

## OWASP Top 10 2021 Coverage

### A01:2021 - Broken Access Control
- Path Traversal (CWE-22)
- Open Redirect (CWE-601)
- CSRF (CWE-352)
- Improper Authorization (CWE-285)
- Incorrect Permission (CWE-732)
- Session Data Exposure (CWE-488)

### A02:2021 - Cryptographic Failures
- Weak Cryptography (CWE-327)
- Unencrypted Data (CWE-311)
- Hardcoded Credentials (CWE-798)
- Insufficient Key Length (CWE-326)
- Weak Random (CWE-330)
- Cookie Exposure (CWE-539)
- Hash Without Salt (CWE-759)

### A03:2021 - Injection
- SQL Injection (CWE-89) - Top priority
- Code Injection (CWE-94)
- XSS (CWE-79) - Top priority
- Command Injection (CWE-78) - Top priority
- XML Injection (CWE-91)
- LDAP Injection (CWE-90)
- HTTP Response Splitting (CWE-113)
- Format String (CWE-134)

### A04:2021 - Insecure Design
- Dangerous File Upload (CWE-434)
- Integer Overflow (CWE-190)
- Security Decision Input (CWE-807)
- Buffer Overflow (CWE-119)
- TOCTOU (CWE-362)
- Missing Error Handling (CWE-391)
- Improper Exception Handling (CWE-396)
- Module Restrictions
- Private Array Return (CWE-495)
- Public Data Assignment (CWE-496)

### A05:2021 - Security Misconfiguration
- XXE (CWE-611)
- Comment Exposure (CWE-615)
- Error Message Leak (CWE-209)
- Debug Code (CWE-489)
- DNS Lookup (CWE-350)
- Insecure API Usage

### A06:2021 - Vulnerable and Outdated Components
- Infinite Loop (CWE-835)
- Null Pointer (CWE-476)
- Resource Leak (CWE-404)

### A07:2021 - Identification and Authentication Failures
- Missing Authentication (CWE-306) - Top priority
- Weak Password (CWE-521)
- Insufficient Verification (CWE-287)
- Session Validation (CWE-613)
- No Rate Limiting (CWE-307)

### A08:2021 - Software and Data Integrity Failures
- Download Without Integrity Check (CWE-494)

### A09:2021 - Security Logging and Monitoring Failures
- (Covered in error handling section)

### A10:2021 - Server-Side Request Forgery (SSRF)
- SSRF (CWE-918) - Top priority

---

## CWE Top 25 (2023) Coverage

The guide covers **18 out of 25** from CWE Top 25 Most Dangerous Software Weaknesses:

| Rank | CWE | Name | Covered |
|------|-----|------|---------|
| 1 | CWE-787 | Out-of-bounds Write | Yes (as Buffer Overflow) |
| 2 | CWE-79 | Cross-site Scripting | Yes |
| 3 | CWE-89 | SQL Injection | Yes |
| 4 | CWE-20 | Improper Input Validation | Yes (multiple) |
| 5 | CWE-78 | OS Command Injection | Yes |
| 6 | CWE-352 | CSRF | Yes |
| 7 | CWE-22 | Path Traversal | Yes |
| 8 | CWE-434 | Unrestricted File Upload | Yes |
| 9 | CWE-862 | Missing Authorization | Yes (as CWE-285) |
| 10 | CWE-798 | Hardcoded Credentials | Yes |
| 13 | CWE-190 | Integer Overflow | Yes |
| 14 | CWE-119 | Buffer Overflow | Yes |
| 16 | CWE-476 | Null Pointer Dereference | Yes |
| 19 | CWE-362 | Race Condition | Yes (TOCTOU) |
| 21 | CWE-611 | XXE | Yes |
| 22 | CWE-918 | SSRF | Yes |
| 23 | CWE-276 | Incorrect Permissions | Yes (as CWE-732) |
| 25 | CWE-287 | Improper Authentication | Yes |

---

## Remediation Priority Matrix

### Priority 1 (CRITICAL - Fix Immediately)
**11 vulnerabilities** - High exploitability, severe impact

1. SQL Injection (CWE-89)
2. XSS (CWE-79)
3. OS Command Injection (CWE-78)
4. XXE (CWE-611)
5. CSRF (CWE-352)
6. SSRF (CWE-918)
7. Buffer Overflow (CWE-119)
8. Code Injection (CWE-94)
9. Path Traversal (CWE-22)
10. Missing Authentication (CWE-306)
11. Improper Authorization (CWE-285)
12. Weak Cryptography (CWE-327)
13. Unencrypted Data (CWE-311)
14. Hardcoded Credentials (CWE-798)

### Priority 2 (HIGH - Fix Soon)
**18 vulnerabilities** - Moderate exploitability, significant impact

### Priority 3 (MEDIUM - Schedule Fix)
**16 vulnerabilities** - Lower exploitability or impact

### Priority 4 (LOW - Fix When Convenient)
**2 vulnerabilities** - Minimal security impact

---

## External References

### CWE Database
- **CWE List**: https://cwe.mitre.org/data/index.html
- **CWE Top 25**: https://cwe.mitre.org/top25/
- **CWE SANS Top 25**: https://www.sans.org/top25-software-errors/

### OWASP Resources
- **OWASP Top 10 2021**: https://owasp.org/www-project-top-ten/
- **OWASP Cheat Sheets**: https://cheatsheetseries.owasp.org/
- **OWASP ASVS**: https://owasp.org/www-project-application-security-verification-standard/

### CERT Secure Coding
- **CERT Oracle Coding Standard for Java**: https://wiki.sei.cmu.edu/confluence/display/java/
- **SEI CERT C Coding Standard**: https://wiki.sei.cmu.edu/confluence/display/c/

### NIST
- **NVD (National Vulnerability Database)**: https://nvd.nist.gov/
- **NIST SP 800-53**: Security and Privacy Controls

---

## Quick Lookup

### By Severity

**CRITICAL (14)**:
CWE-22, 78, 79, 89, 94, 119, 285, 306, 311, 327, 352, 611, 798, 918

**HIGH (18)**:
CWE-90, 91, 134, 307, 326, 327, 330, 362, 434, 494, 521, 601, 732, 759, 807

**MEDIUM (13)**:
CWE-113, 190, 209, 391, 396, 404, 476, 488, 489, 539, 615, 835

**LOW (2)**:
CWE-495, 496

### By OWASP Category

**A03 (Injection)**: 8 vulnerabilities
**A02 (Crypto)**: 7 vulnerabilities
**A07 (Auth)**: 5 vulnerabilities
**A01 (Access)**: 6 vulnerabilities
**A04 (Design)**: 11 vulnerabilities

---

**Version**: 2021.12.29 (Based on KISA Secure Coding Guide)
**Last Updated**: 2026-02-05
