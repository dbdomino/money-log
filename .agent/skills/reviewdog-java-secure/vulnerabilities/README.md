# Java Secure Coding Vulnerabilities

Based on Korean Government Secure Coding Guide (KISA 2021.12.29)

---

## Overview

This directory contains **44 comprehensive vulnerability documentation files** for Java secure coding, organized by category. Each file includes vulnerable code examples, secure solutions, and references to industry standards (CWE, OWASP, CERT).

---

## Directory Structure

```
vulnerabilities/
├── 01-input-validation/        # Input Validation (15 files)
├── 02-security-features/       # Security Features (16 files)
├── 03-time-and-state/          # Time and State (2 files)
├── 04-error-handling/          # Error Handling (3 files)
├── 05-code-quality/            # Code Quality (2 files)
├── 06-encapsulation/           # Encapsulation (4 files)
└── 07-api-misuse/              # API Misuse (2 files)
```

---

## Vulnerability Categories

### 01. Input Validation - 15 files

Vulnerabilities related to improper validation and handling of input data.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `sql-injection.md` | CWE-89 | CRITICAL | SQL Injection |
| `xss.md` | CWE-79 | HIGH | Cross-Site Scripting |
| `command-injection.md` | CWE-78 | CRITICAL | OS Command Injection |
| `path-traversal.md` | CWE-22 | HIGH | Path Traversal and Resource Injection |
| `code-injection.md` | CWE-94 | CRITICAL | Code Injection |
| `ldap-injection.md` | CWE-90 | HIGH | LDAP Injection |
| `xpath-injection.md` | CWE-643 | HIGH | XPath Injection |
| `xxe.md` | CWE-611 | HIGH | XML External Entity Reference |
| `csrf.md` | CWE-352 | MEDIUM | Cross-Site Request Forgery |
| `ssrf.md` | CWE-918 | HIGH | Server-Side Request Forgery |
| `open-redirect.md` | CWE-601 | MEDIUM | Untrusted URL Redirect |
| `http-response-splitting.md` | CWE-113 | MEDIUM | HTTP Response Splitting |
| `file-upload.md` | CWE-434 | HIGH | Dangerous File Upload |
| `integer-overflow.md` | CWE-190 | MEDIUM | Integer Overflow |
| `insecure-deserialization.md` | CWE-502 | CRITICAL | Insecure Deserialization |

---

### 02. Security Features - 16 files

Vulnerabilities in cryptography, authentication, authorization, and other security mechanisms.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `hardcoded-password.md` | CWE-259 | HIGH | Hardcoded Password |
| `weak-cryptographic-algorithm.md` | CWE-327 | HIGH | Weak Cryptographic Algorithm |
| `insufficient-random-values.md` | CWE-330 | MEDIUM | Insufficient Random Values |
| `hash-without-salt.md` | CWE-760 | HIGH | Hash Without Salt |
| `insufficient-key-length.md` | CWE-326 | HIGH | Insufficient Key Length |
| `missing-authentication.md` | CWE-306 | HIGH | Missing Authentication |
| `improper-authorization.md` | CWE-285 | HIGH | Improper Authorization |
| `weak-password-requirements.md` | CWE-521 | MEDIUM | Weak Password Requirements |
| `excessive-authentication-attempts.md` | CWE-307 | MEDIUM | Unlimited Authentication Attempts |
| `improper-certificate-validation.md` | CWE-295 | HIGH | Improper Certificate Validation |
| `improper-signature-verification.md` | CWE-347 | HIGH | Improper Signature Verification |
| `code-download-without-integrity-check.md` | CWE-494 | MEDIUM | Missing Integrity Check |
| `unencrypted-sensitive-information.md` | CWE-312 | HIGH | Unencrypted Sensitive Information |
| `information-exposure-through-comments.md` | CWE-615 | LOW | Information Exposure Through Comments |
| `persistent-cookies-information-exposure.md` | CWE-539 | MEDIUM | Missing Cookie Security Attributes |
| `incorrect-permission-assignment.md` | CWE-732 | MEDIUM | Incorrect Permission Assignment |

---

### 03. Time and State - 2 files

Race conditions and timing-related vulnerabilities.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `toctou-race-condition.md` | CWE-367 | HIGH | Time-of-Check Time-of-Use (TOCTOU) |
| `uncontrolled-recursion.md` | CWE-674 | MEDIUM | Non-terminating Loop/Recursion |

---

### 04. Error Handling - 3 files

Information disclosure through error messages and improper error handling.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `error-message-information-disclosure.md` | CWE-209 | MEDIUM | Error Message Information Disclosure |
| `improper-error-handling.md` | CWE-390 | MEDIUM | Improper Error Handling |
| `improper-exception-handling.md` | CWE-755 | MEDIUM | Improper Exception Handling |

---

### 05. Code Quality - 2 files

Null pointer dereferences and resource management issues.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `null-pointer-dereference.md` | CWE-476 | MEDIUM | Null Pointer Dereference |
| `improper-resource-release.md` | CWE-404 | MEDIUM | Improper Resource Release |

---

### 06. Encapsulation - 4 files

Information leakage through broken encapsulation.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `data-exposure-between-sessions.md` | CWE-488 | MEDIUM | Data Exposure Between Sessions |
| `leftover-debug-code.md` | CWE-489 | LOW | Leftover Debug Code |
| `private-array-returned-from-public-method.md` | CWE-495 | MEDIUM | Private Array Returned from Public Method |
| `public-data-assigned-to-private-array.md` | CWE-496 | MEDIUM | Public Data Assigned to Private Array |

---

### 07. API Misuse - 2 files

Improper use of APIs and deprecated functions.

| File | CWE | Severity | Description |
|------|-----|----------|-------------|
| `dns-lookup-security-decision.md` | CWE-350 | MEDIUM | DNS Lookup in Security Decision |
| `deprecated-dangerous-api-usage.md` | CWE-477 | LOW | Dangerous API Usage |

---

## File Structure

Each vulnerability file follows this structure:

```markdown
# Title

## Metadata
- CWE, Category, Severity, Language, OWASP Top 10

## Overview
- Description
- Attack scenarios
- Impact analysis

## Security Measures
- Prevention strategies
- Best practices

## Code Examples
- Vulnerable code
- Secure code
- Multiple framework examples (Spring, Jakarta EE, etc.)

## References
- CWE links
- OWASP resources
- CERT Java Coding Standard

## Detection Methods
- Static analysis patterns
- Grep patterns
- Code review checklist

## Security Verification Checklist
- Design phase
- Implementation phase
- Testing phase
- Deployment phase
```

---

## Usage

### Search by Vulnerability Name
```bash
# Find SQL injection documentation
cat 01-input-validation/sql-injection.md

# Find XSS prevention
cat 01-input-validation/xss.md
```

### Search by CWE Number
```bash
# Find CWE-89 (SQL Injection)
grep -r "CWE-89" .

# Find all CRITICAL vulnerabilities
grep -r "Severity: CRITICAL" .
```

### Search by OWASP Category
```bash
# Find A01 (Broken Access Control)
grep -r "A01:2021" .
```

---

## Security Standards Covered

### CWE (Common Weakness Enumeration)
- All files mapped to specific CWE numbers
- Links to MITRE CWE database

### OWASP Top 10 (2021)
- A01: Broken Access Control
- A02: Cryptographic Failures
- A03: Injection
- A04: Insecure Design
- A05: Security Misconfiguration
- A06: Vulnerable and Outdated Components
- A07: Identification and Authentication Failures
- A08: Software and Data Integrity Failures
- A09: Security Logging and Monitoring Failures
- A10: Server-Side Request Forgery (SSRF)

### CERT Java Coding Standard
- References to specific CERT rules
- Links to SEI CERT documentation

---

## Frameworks Covered

- **Spring Framework** (Spring Boot, Spring Security)
- **Jakarta EE** (formerly Java EE)
- **JDBC** (raw SQL)
- **JPA / Hibernate** (ORM)
- **MyBatis** (SQL Mapper)
- **JAX-RS** (REST APIs)
- **Servlet API** (web applications)

---

## Static Analysis Tools

Recommended tools for detecting these vulnerabilities:

- **SpotBugs** (formerly FindBugs)
- **SonarQube**
- **Checkmarx**
- **Fortify Static Code Analyzer**
- **Veracode**
- **OWASP Dependency-Check**
- **Snyk**

---

## Contributing

To add or update vulnerability documentation:

1. Follow the template structure in existing files
2. Provide vulnerable and secure code examples
3. Add references to CWE, OWASP, and CERT
4. Test all code examples

---

## Additional Resources

### Official Documentation
- [KISA Secure Coding Guide](https://www.kisa.or.kr/)
- [OWASP Top 10](https://owasp.org/Top10/)
- [CWE Database](https://cwe.mitre.org/)
- [CERT Java Coding Standard](https://wiki.sei.cmu.edu/confluence/display/java)

### Training Resources
- [OWASP WebGoat](https://owasp.org/www-project-webgoat/)
- [Secure Code Warrior](https://www.securecodewarrior.com/)
- [HackerOne CTF](https://ctf.hacker101.com/)

---

## Statistics

- **Total Files**: 44
- **Critical Severity**: 5 files
- **High Severity**: 17 files
- **Medium Severity**: 19 files
- **Low Severity**: 3 files

**Most Common Categories:**
1. Security Features (16 files) - 36%
2. Input Validation (15 files) - 34%
3. Encapsulation (4 files) - 9%
4. Error Handling (3 files) - 7%
5. Others (6 files) - 14%

---

## Version History

- **v1.0** (2026-02-05): Initial release with 44 vulnerability files
- Based on KISA Secure Coding Guide 2021.12.29
- Comprehensive code examples and references

---

## License

Based on Korean Government Secure Coding Guide (Public Domain)

---

**For questions or updates, refer to the main SKILL.md file.**
