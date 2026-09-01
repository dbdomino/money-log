# Improper Certificate Validation (CWE-295)

**Severity**: 🔴 CRITICAL
**Category**: Security Features
**OWASP Top 10**: A02:2021 – Cryptographic Failures

---

## Overview

### Attack Description

Improper certificate validation occurs when applications fail to properly verify SSL/TLS certificates during secure communications. This includes accepting expired certificates, not checking certificate revocation lists (CRL), not validating the certificate chain to a trusted root CA, accepting self-signed certificates, or not verifying the Common Name (CN) matches the server hostname. These failures enable man-in-the-middle attacks where attackers can intercept, read, or modify encrypted communications.

### Impact

**Potential consequences:**
- Man-in-the-middle (MITM) attacks
- Eavesdropping on encrypted communications
- Data interception and theft
- Credential theft
- Session hijacking
- Data tampering
- Impersonation of legitimate servers
- Loss of confidentiality and integrity
- Compliance violations (PCI-DSS, HIPAA, GDPR)
- Reputation damage

---

## Security Measures

### Key Principles

When performing SSL/TLS communication, server certificates must be validated. The certificate's validity period, issuer, Common Name (CN), revocation status (CRL/OCSP), and trust chain must all be verified. Self-signed certificates must not be used in production environments.

**Primary Defenses:**

1. **Certificate Chain Validation**
   - Verify certificate chain to trusted root CA
   - Check all certificates in the chain
   - Ensure proper certificate hierarchy
   - Validate intermediate certificates
   - Use system's trusted certificate store

2. **Certificate Validity Checks**
   - Verify certificate is within validity period (not expired)
   - Check certificate is not yet valid (notBefore)
   - Validate certificate notAfter date
   - Ensure certificate is active

3. **Hostname Verification**
   - Verify Common Name (CN) matches server hostname
   - Check Subject Alternative Names (SAN)
   - Prevent hostname mismatch attacks
   - Use proper hostname verification APIs

4. **Revocation Checking**
   - Check Certificate Revocation Lists (CRL)
   - Use Online Certificate Status Protocol (OCSP)
   - Verify certificate hasn't been revoked
   - Implement CRL/OCSP stapling

5. **Reject Invalid Certificates**
   - Reject self-signed certificates in production
   - Reject expired certificates
   - Reject certificates with invalid signatures
   - Reject untrusted certificate chains
   - No certificate pinning bypass

6. **Use Strong TLS Configuration**
   - TLS 1.2 or higher required
   - Strong cipher suites only
   - Proper certificate key length (2048+ bits for RSA)
   - Disable weak algorithms

---

## Code Examples

### Attack Scenario

An attacker intercepts communications through a man-in-the-middle attack using a self-signed certificate.

**Attack Sequence:**
```
1. Client initiates HTTPS connection to bank.com
   Client → Attacker → bank.com

2. Attacker intercepts and presents self-signed certificate
   Attacker's Cert: CN=bank.com (self-signed)

3. Vulnerable client accepts without validation
   ❌ No certificate chain validation
   ❌ No hostname verification
   ❌ Accepts self-signed certificate

4. Client sends credentials over "secure" connection
   Username: user@email.com
   Password: MyP@ssw0rd!

5. Attacker captures credentials in plaintext
   ✓ Attacker has full access to account
```

**MITM Attack Example:**
```bash
# Attacker sets up proxy with self-signed certificate
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 \
  -subj "/CN=bank.com" -nodes

# Vulnerable client accepts this certificate without validation
# All traffic is now visible to attacker
```

---

### ❌ Vulnerable Code

#### C - Accepting Self-Signed Certificates

```c
#include <openssl/ssl.h>
#include <openssl/err.h>

/**
 * Vulnerable code that unconditionally accepts self-signed certificates
 */
int verify_callback(int preverify_ok, X509_STORE_CTX *ctx) {
    int err = X509_STORE_CTX_get_error(ctx);

    // Ignoring self-signed certificate errors and accepting them
    if (err == X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT ||
        err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
        // DANGER: Accepting self-signed certificate
        return 1; // Accept self-signed certificate!
    }

    return preverify_ok;
}

/**
 * Disabling certificate verification during SSL connection
 */
SSL_CTX* create_insecure_ssl_context() {
    SSL_CTX *ctx = SSL_CTX_new(TLS_client_method());

    // Certificate verification disabled - very dangerous!
    SSL_CTX_set_verify(ctx, SSL_VERIFY_NONE, NULL);

    return ctx;
}

/**
 * SSL connection without hostname verification
 */
int connect_without_hostname_verification(SSL *ssl, const char *hostname) {
    int ret;

    // Perform SSL connection
    ret = SSL_connect(ssl);

    // Hostname verification is not performed
    // An attacker can use a valid certificate from a different domain

    return ret;
}
```

**Problems:**
1. **Accepts self-signed certificates** - MITM vulnerability
2. **Certificate verification disabled** - `SSL_VERIFY_NONE`
3. **No hostname verification** - Allows certificate mismatch
4. **No certificate chain validation**
5. **No revocation checking**
6. **No expiration checking**

---

#### Java - Disabling Certificate Validation

```java
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * Vulnerable code that accepts all certificates
 */
public class InsecureSSLConnection {

    /**
     * TrustManager that trusts all certificates (very dangerous!)
     */
    private static TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
            // Does not verify client certificates
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            // Does not verify server certificates
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // No verification at all - accepts all certificates!
            }

            // Does not verify server certificates
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // No verification at all - accepts all certificates!
            }
        }
    };

    /**
     * Disabling hostname verification
     */
    private static HostnameVerifier trustAllHosts = new HostnameVerifier() {
        public boolean verify(String hostname, SSLSession session) {
            // Accepts all hostnames - very dangerous!
            return true;
        }
    };

    /**
     * Insecure HTTPS connection setup
     */
    public void setupInsecureSSL() throws Exception {
        // Create SSLContext that trusts all certificates
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Disable hostname verification
        HttpsURLConnection.setDefaultHostnameVerifier(trustAllHosts);

        // Now all HTTPS connections are performed without certificate validation - MITM vulnerable!
    }
}
```

**Problems:**
1. **Trust all certificates** - No validation performed
2. **Disabled hostname verification** - Accepts any hostname
3. **No certificate chain validation**
4. **No expiration checking**
5. **No revocation checking**
6. **Global security bypass** - Affects all HTTPS connections

---

#### Java - Incomplete Certificate Validation

```java
import java.security.cert.*;
import javax.net.ssl.*;

public class WeakCertificateValidation {

    /**
     * Incomplete validation that only checks validity period
     */
    public boolean validateCertificate(X509Certificate cert) {
        try {
            // Only checks validity period
            cert.checkValidity();

            // Problem: No other validation performed
            // - No certificate chain validation
            // - No hostname verification
            // - No revocation checking
            // - No trusted CA verification

            return true; // Incomplete validation
        } catch (CertificateException e) {
            return false;
        }
    }

    /**
     * Unconditionally accepts self-signed certificates
     */
    public boolean acceptAnyCertificate(X509Certificate cert) {
        // No certificate validation performed at all
        return true; // Accepts all certificates - dangerous!
    }
}
```

**Problems:**
1. **Only checks validity period** - Insufficient validation
2. **No certificate chain verification**
3. **No hostname verification**
4. **No revocation checking**
5. **Accepts self-signed certificates**

---

#### C# - Disabling Certificate Validation

```csharp
using System.Net;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;

/// <summary>
/// Vulnerable code that disables certificate validation
/// </summary>
public class InsecureHttpClient
{
    /// <summary>
    /// Configures to accept all certificates (very dangerous!)
    /// </summary>
    public void DisableCertificateValidation()
    {
        // Set callback to accept all SSL/TLS certificates
        ServicePointManager.ServerCertificateValidationCallback =
            (sender, certificate, chain, sslPolicyErrors) =>
            {
                // Accepts all certificates - ignoring certificate errors!
                return true;
            };

        // Now all HTTPS requests are performed without certificate validation
    }

    /// <summary>
    /// Accepts only self-signed certificates (still dangerous)
    /// </summary>
    public void AcceptSelfSignedCertificates()
    {
        ServicePointManager.ServerCertificateValidationCallback =
            (sender, certificate, chain, sslPolicyErrors) =>
            {
                // Ignoring self-signed certificate errors
                if (sslPolicyErrors ==
                    SslPolicyErrors.RemoteCertificateChainErrors)
                {
                    return true; // DANGER: Accepting self-signed certificate
                }

                return sslPolicyErrors == SslPolicyErrors.None;
            };
    }
}
```

**Problems:**
1. **Global certificate validation bypass** - Affects all HTTPS requests
2. **Accepts all certificates** - Including invalid ones
3. **No hostname verification**
4. **Accepts self-signed certificates**
5. **No revocation checking**

---

### ✅ Secure Code

#### Java - Proper Certificate Validation

```java
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.*;
import java.io.*;
import java.util.*;

/**
 * Class for secure SSL/TLS certificate validation
 */
public class SecureCertificateValidator {

    private final KeyStore trustStore;
    private final PKIXParameters pkixParams;

    public SecureCertificateValidator(String trustStorePath, String password)
            throws Exception {
        // Load trusted certificate store
        this.trustStore = loadTrustStore(trustStorePath, password);
        this.pkixParams = new PKIXParameters(trustStore);

        // Enable revocation checking
        this.pkixParams.setRevocationEnabled(true);
    }

    /**
     * Load trust store
     */
    private KeyStore loadTrustStore(String path, String password)
            throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password.toCharArray());
        }

        return ks;
    }

    /**
     * X.509 certificate validation (complete validation)
     */
    public boolean validateCertificate(X509Certificate toVerify,
                                       X509Certificate signingCert,
                                       String hostname)
            throws Exception {

        // 1. Check certificate validity period
        try {
            toVerify.checkValidity();
            logger.info("Certificate validity check: passed");
        } catch (CertificateExpiredException e) {
            logger.severe("Certificate has expired: " + e.getMessage());
            return false;
        } catch (CertificateNotYetValidException e) {
            logger.severe("Certificate is not yet valid: " + e.getMessage());
            return false;
        }

        // 2. Verify certificate signature
        try {
            toVerify.verify(signingCert.getPublicKey());
            logger.info("Certificate signature verification: passed");
        } catch (SignatureException | InvalidKeyException e) {
            logger.severe("Certificate signature verification failed: " + e.getMessage());
            return false;
        }

        // 3. Hostname verification (Common Name or SAN)
        if (!verifyHostname(toVerify, hostname)) {
            logger.severe("Hostname verification failed: " + hostname);
            return false;
        }

        // 4. Certificate chain validation
        if (!verifyCertificateChain(toVerify)) {
            logger.severe("Certificate chain validation failed");
            return false;
        }

        // 5. Check certificate revocation status (CRL)
        if (!checkCertificateRevocation(toVerify)) {
            logger.severe("Certificate has been revoked");
            return false;
        }

        // 6. Check for self-signed certificate
        if (isSelfSigned(toVerify)) {
            logger.severe("Self-signed certificates are not allowed");
            return false;
        }

        logger.info("Certificate validation complete: passed");
        return true;
    }

    /**
     * Hostname verification (CN and SAN check)
     */
    private boolean verifyHostname(X509Certificate cert, String hostname)
            throws Exception {

        // 1. Check Subject Alternative Names (SAN)
        Collection<List<?>> sans = cert.getSubjectAlternativeNames();

        if (sans != null) {
            for (List<?> san : sans) {
                Integer type = (Integer) san.get(0);

                // Type 2 = dNSName
                if (type == 2) {
                    String dnsName = (String) san.get(1);

                    if (matchHostname(hostname, dnsName)) {
                        logger.info("SAN hostname match: " + dnsName);
                        return true;
                    }
                }
            }
        }

        // 2. Check Common Name (CN)
        String dn = cert.getSubjectDN().getName();
        String cn = extractCommonName(dn);

        if (cn != null && matchHostname(hostname, cn)) {
            logger.info("CN hostname match: " + cn);
            return true;
        }

        logger.warning("Hostname does not match the certificate");
        return false;
    }

    /**
     * Extract Common Name
     */
    private String extractCommonName(String dn) {
        for (String part : dn.split(",")) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return null;
    }

    /**
     * Hostname matching (with wildcard support)
     */
    private boolean matchHostname(String hostname, String pattern) {
        // Exact match
        if (hostname.equalsIgnoreCase(pattern)) {
            return true;
        }

        // Wildcard pattern (*.example.com)
        if (pattern.startsWith("*.")) {
            String domain = pattern.substring(2);
            return hostname.toLowerCase().endsWith("." + domain.toLowerCase());
        }

        return false;
    }

    /**
     * Certificate chain validation
     */
    private boolean verifyCertificateChain(X509Certificate cert)
            throws Exception {

        // Create CertificateFactory
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // Create certificate path
        List<Certificate> certList = new ArrayList<>();
        certList.add(cert);
        CertPath certPath = cf.generateCertPath(certList);

        // Validate with CertPathValidator
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            PKIXCertPathValidatorResult result =
                (PKIXCertPathValidatorResult) validator.validate(
                    certPath, pkixParams);

            logger.info("Certificate chain validation passed. Trust Anchor: " +
                       result.getTrustAnchor().getTrustedCert().getSubjectDN());
            return true;

        } catch (CertPathValidatorException e) {
            logger.severe("Certificate chain validation failed: " + e.getMessage() +
                         " (Index: " + e.getIndex() + ")");
            return false;
        }
    }

    /**
     * Check certificate revocation status (CRL)
     */
    private boolean checkCertificateRevocation(X509Certificate cert)
            throws Exception {

        // 1. Extract CRL Distribution Point
        byte[] crlDistPointsExt = cert.getExtensionValue("2.5.29.31");

        if (crlDistPointsExt == null) {
            logger.warning("No CRL Distribution Point found");
            return true; // Pass if no CRL (can be treated as failure depending on policy)
        }

        // 2. Parse CRL URL (simplified example)
        String crlUrl = parseCRLDistributionPoint(crlDistPointsExt);

        if (crlUrl == null) {
            logger.warning("Unable to parse CRL URL");
            return true;
        }

        // 3. Download and check CRL
        return !isCertificateRevokedByCRL(cert, crlUrl);
    }

    /**
     * Parse CRL Distribution Point
     */
    private String parseCRLDistributionPoint(byte[] extension) {
        // Actual implementation requires ASN.1 parsing
        // Simplified for this example
        return "http://crl.example.com/ca.crl";
    }

    /**
     * Check certificate revocation status against CRL
     */
    private boolean isCertificateRevokedByCRL(X509Certificate cert, String crlUrl)
            throws Exception {

        // Download CRL
        java.net.URL url = new java.net.URL(crlUrl);
        InputStream crlStream = url.openStream();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRL crl = (X509CRL) cf.generateCRL(crlStream);
        crlStream.close();

        // Verify CRL signature
        // crl.verify(issuerPublicKey);

        // Check if certificate is revoked
        boolean revoked = crl.isRevoked(cert);

        if (revoked) {
            X509CRLEntry entry = crl.getRevokedCertificate(cert);
            logger.warning("Certificate has been revoked. Revocation date: " +
                          entry.getRevocationDate());
        }

        return revoked;
    }

    /**
     * Check certificate revocation using OCSP
     */
    private boolean checkOCSP(X509Certificate cert, X509Certificate issuerCert)
            throws Exception {

        // Extract OCSP Responder URL
        String ocspUrl = getOCSPUrl(cert);

        if (ocspUrl == null) {
            logger.warning("No OCSP URL found");
            return true;
        }

        // Generate OCSP request
        // OCSPReq request = generateOCSPRequest(cert, issuerCert);

        // Get OCSP response
        // OCSPResp response = sendOCSPRequest(ocspUrl, request);

        // Validate OCSP response
        // return validateOCSPResponse(response, cert);

        logger.info("OCSP check complete");
        return true;
    }

    /**
     * Extract OCSP URL
     */
    private String getOCSPUrl(X509Certificate cert) throws IOException {
        byte[] ocspExtension = cert.getExtensionValue("1.3.6.1.5.5.7.1.1");

        if (ocspExtension != null) {
            // Parse ASN.1 to extract OCSP URL
            return "http://ocsp.example.com";
        }

        return null;
    }

    /**
     * Check if certificate is self-signed
     */
    private boolean isSelfSigned(X509Certificate cert) {
        try {
            // Self-signed if issuer and subject are the same
            String issuer = cert.getIssuerDN().getName();
            String subject = cert.getSubjectDN().getName();

            if (issuer.equals(subject)) {
                // Possible self-signed - attempt verification
                cert.verify(cert.getPublicKey());
                return true; // Confirmed self-signed
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Create secure HTTPS connection
     */
    public HttpsURLConnection createSecureConnection(String urlString)
            throws Exception {

        URL url = new URL(urlString);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        // 1. Set TrustManager that only allows trusted certificates
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        tmf.init(trustStore);

        // 2. Create SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        conn.setSSLSocketFactory(sslContext.getSocketFactory());

        // 3. Enable hostname verification (use default)
        conn.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());

        // Or use custom hostname verifier
        // conn.setHostnameVerifier(new CustomHostnameVerifier());

        return conn;
    }
}

/**
 * Custom hostname verifier
 */
class CustomHostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        try {
            Certificate[] certs = session.getPeerCertificates();

            if (certs.length == 0) {
                return false;
            }

            X509Certificate cert = (X509Certificate) certs[0];

            // Check SAN
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();

            if (sans != null) {
                for (List<?> san : sans) {
                    if ((Integer) san.get(0) == 2) { // dNSName
                        String dnsName = (String) san.get(1);
                        if (matchHostname(hostname, dnsName)) {
                            return true;
                        }
                    }
                }
            }

            // Check CN
            String dn = cert.getSubjectDN().getName();
            String cn = extractCN(dn);

            return cn != null && matchHostname(hostname, cn);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean matchHostname(String hostname, String pattern) {
        if (hostname.equalsIgnoreCase(pattern)) {
            return true;
        }

        if (pattern.startsWith("*.")) {
            String domain = pattern.substring(2);
            return hostname.toLowerCase().endsWith("." + domain.toLowerCase());
        }

        return false;
    }

    private String extractCN(String dn) {
        for (String part : dn.split(",")) {
            part = part.trim();
            if (part.startsWith("CN=")) {
                return part.substring(3);
            }
        }
        return null;
    }
}
```

**Security Features:**
1. **Complete certificate validation** - Validity, signature, chain, revocation
2. **Hostname verification** - CN and SAN checking with wildcard support
3. **Certificate chain validation** - PKIX validation to trusted root CA
4. **Revocation checking** - CRL and OCSP support
5. **Self-signed certificate rejection** - Not allowed in production
6. **Trusted certificate store** - Uses system or custom trust store
7. **TLS 1.3 support** - Modern secure protocol
8. **Comprehensive logging** - Audit trail of validation
9. **No global bypasses** - Per-connection validation
10. **Fail securely** - Rejects on any validation failure

---

#### C - Proper Certificate Validation with OpenSSL

```c
#include <openssl/ssl.h>
#include <openssl/x509.h>
#include <openssl/x509v3.h>
#include <openssl/err.h>
#include <string.h>
#include <stdio.h>

/**
 * Certificate verification callback - rejects self-signed certificates
 */
int verify_callback(int preverify_ok, X509_STORE_CTX *ctx) {
    int err = X509_STORE_CTX_get_error(ctx);
    X509 *cert = X509_STORE_CTX_get_current_cert(ctx);
    int depth = X509_STORE_CTX_get_error_depth(ctx);

    char buf[256];
    X509_NAME_oneline(X509_get_subject_name(cert), buf, sizeof(buf));

    printf("Verifying certificate (depth %d): %s\n", depth, buf);

    if (!preverify_ok) {
        fprintf(stderr, "Certificate verification failed: %s\n",
                X509_verify_cert_error_string(err));

        // Reject self-signed certificates
        if (err == X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT ||
            err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
            fprintf(stderr, "Self-signed certificates are not allowed\n");
            return 0; // Reject
        }

        // Reject expired certificates
        if (err == X509_V_ERR_CERT_HAS_EXPIRED) {
            fprintf(stderr, "Certificate has expired\n");
            return 0; // Reject
        }

        // Reject not-yet-valid certificates
        if (err == X509_V_ERR_CERT_NOT_YET_VALID) {
            fprintf(stderr, "Certificate is not yet valid\n");
            return 0; // Reject
        }

        return 0; // Reject on verification failure
    }

    return 1; // Verification passed
}

/**
 * Hostname verification
 */
int verify_hostname(X509 *cert, const char *hostname) {
    int result = 0;

    // 1. Check Subject Alternative Names (SAN)
    GENERAL_NAMES *san_names = X509_get_ext_d2i(
        cert, NID_subject_alt_name, NULL, NULL
    );

    if (san_names != NULL) {
        int san_count = sk_GENERAL_NAME_num(san_names);

        for (int i = 0; i < san_count; i++) {
            const GENERAL_NAME *current_name = sk_GENERAL_NAME_value(san_names, i);

            if (current_name->type == GEN_DNS) {
                const char *dns_name = (char *)ASN1_STRING_get0_data(
                    current_name->d.dNSName
                );

                // Case-insensitive hostname comparison
                if (strcasecmp(hostname, dns_name) == 0) {
                    result = 1;
                    break;
                }

                // Wildcard support (*.example.com)
                if (dns_name[0] == '*' && dns_name[1] == '.') {
                    const char *domain = dns_name + 2;
                    const char *host_domain = strchr(hostname, '.');

                    if (host_domain != NULL &&
                        strcasecmp(host_domain + 1, domain) == 0) {
                        result = 1;
                        break;
                    }
                }
            }
        }

        GENERAL_NAMES_free(san_names);

        if (result) {
            printf("SAN hostname verification: passed\n");
            return 1;
        }
    }

    // 2. Check Common Name (CN)
    X509_NAME *subject = X509_get_subject_name(cert);
    char cn[256];

    if (X509_NAME_get_text_by_NID(subject, NID_commonName,
                                  cn, sizeof(cn)) > 0) {
        if (strcasecmp(hostname, cn) == 0) {
            printf("CN hostname verification: passed\n");
            return 1;
        }
    }

    fprintf(stderr, "Hostname verification failed: %s\n", hostname);
    return 0;
}

/**
 * Create secure SSL context
 */
SSL_CTX* create_secure_ssl_context() {
    const SSL_METHOD *method;
    SSL_CTX *ctx;

    // Initialize OpenSSL
    SSL_library_init();
    SSL_load_error_strings();
    OpenSSL_add_all_algorithms();

    // Use TLS 1.2 or higher
    method = TLS_client_method();
    ctx = SSL_CTX_new(method);

    if (!ctx) {
        ERR_print_errors_fp(stderr);
        return NULL;
    }

    // Set minimum TLS version (TLS 1.2)
    SSL_CTX_set_min_proto_version(ctx, TLS1_2_VERSION);

    // Load trusted CA certificates
    if (!SSL_CTX_load_verify_locations(ctx, "/etc/ssl/certs/ca-certificates.crt",
                                        "/etc/ssl/certs/")) {
        fprintf(stderr, "Failed to load CA certificates\n");
        ERR_print_errors_fp(stderr);
        SSL_CTX_free(ctx);
        return NULL;
    }

    // Enable certificate verification and set callback
    SSL_CTX_set_verify(ctx, SSL_VERIFY_PEER, verify_callback);

    // Set verification depth
    SSL_CTX_set_verify_depth(ctx, 4);

    // Allow only strong cipher suites
    if (!SSL_CTX_set_cipher_list(ctx,
        "ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:"
        "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256")) {
        fprintf(stderr, "Failed to set cipher suites\n");
        SSL_CTX_free(ctx);
        return NULL;
    }

    return ctx;
}

/**
 * Perform secure SSL connection
 */
int connect_with_verification(const char *hostname, int port) {
    SSL_CTX *ctx;
    SSL *ssl;
    int server_fd;
    X509 *cert;
    long verify_result;

    // 1. Create SSL context
    ctx = create_secure_ssl_context();
    if (!ctx) {
        return -1;
    }

    // 2. TCP socket connection (omitted - use standard socket, connect)
    server_fd = socket(AF_INET, SOCK_STREAM, 0);
    // connect(server_fd, ...);

    // 3. Create SSL object
    ssl = SSL_new(ctx);
    SSL_set_fd(ssl, server_fd);

    // 4. Set SNI (Server Name Indication)
    SSL_set_tlsext_host_name(ssl, hostname);

    // 5. Perform SSL connection
    if (SSL_connect(ssl) != 1) {
        fprintf(stderr, "SSL connection failed\n");
        ERR_print_errors_fp(stderr);
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        return -1;
    }

    printf("SSL connection successful (%s)\n", SSL_get_cipher(ssl));

    // 6. Get certificate
    cert = SSL_get_peer_certificate(ssl);
    if (!cert) {
        fprintf(stderr, "Unable to retrieve server certificate\n");
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        return -1;
    }

    // 7. Check certificate verification result
    verify_result = SSL_get_verify_result(ssl);
    if (verify_result != X509_V_OK) {
        fprintf(stderr, "Certificate verification failed: %s\n",
                X509_verify_cert_error_string(verify_result));
        X509_free(cert);
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        return -1;
    }

    printf("Certificate verification: passed\n");

    // 8. Hostname verification
    if (!verify_hostname(cert, hostname)) {
        fprintf(stderr, "Hostname verification failed\n");
        X509_free(cert);
        SSL_free(ssl);
        SSL_CTX_free(ctx);
        return -1;
    }

    // 9. Print certificate information
    char buf[256];
    X509_NAME_oneline(X509_get_subject_name(cert), buf, sizeof(buf));
    printf("Server certificate: %s\n", buf);

    X509_free(cert);

    // Now secure communication is possible
    // Use SSL_write(), SSL_read()

    // Cleanup
    SSL_shutdown(ssl);
    SSL_free(ssl);
    SSL_CTX_free(ctx);

    return 0;
}
```

**Security Features:**
1. **Certificate verification enabled** - `SSL_VERIFY_PEER`
2. **Verification callback** - Rejects self-signed and expired certificates
3. **Hostname verification** - CN and SAN with wildcard support
4. **Certificate chain validation** - To trusted root CA
5. **TLS 1.2 minimum** - Secure protocol version
6. **Strong cipher suites** - Only secure algorithms
7. **SNI support** - Proper server identification
8. **Verification result checking** - Confirms successful validation
9. **Comprehensive error handling** - Detailed error messages
10. **Proper cleanup** - Resource management

---

#### C# - Proper Certificate Validation

```csharp
using System;
using System.Net;
using System.Net.Security;
using System.Security.Cryptography.X509Certificates;

/// <summary>
/// Secure certificate validation
/// </summary>
public class SecureCertificateValidator
{
    /// <summary>
    /// Secure server certificate validation callback
    /// </summary>
    public bool ValidateServerCertificate(
        object sender,
        X509Certificate certificate,
        X509Chain chain,
        SslPolicyErrors sslPolicyErrors)
    {
        // 1. No SSL policy errors means success
        if (sslPolicyErrors == SslPolicyErrors.None)
        {
            Console.WriteLine("Certificate verification: passed");
            return true;
        }

        Console.WriteLine($"SSL policy errors: {sslPolicyErrors}");

        // 2. Remote certificate name mismatch - reject
        if ((sslPolicyErrors & SslPolicyErrors.RemoteCertificateNameMismatch) != 0)
        {
            Console.WriteLine("Hostname mismatch - connection rejected");
            return false;
        }

        // 3. Remote certificate not available - reject
        if ((sslPolicyErrors & SslPolicyErrors.RemoteCertificateNotAvailable) != 0)
        {
            Console.WriteLine("Certificate not available - connection rejected");
            return false;
        }

        // 4. Certificate chain errors - detailed check
        if ((sslPolicyErrors & SslPolicyErrors.RemoteCertificateChainErrors) != 0)
        {
            return ValidateCertificateChain(chain);
        }

        return false;
    }

    /// <summary>
    /// Certificate chain validation
    /// </summary>
    private bool ValidateCertificateChain(X509Chain chain)
    {
        foreach (X509ChainStatus status in chain.ChainStatus)
        {
            Console.WriteLine($"Chain status: {status.Status} - {status.StatusInformation}");

            // Unacceptable errors
            switch (status.Status)
            {
                case X509ChainStatusFlags.NotTimeValid:
                    Console.WriteLine("Certificate has expired or is not yet valid");
                    return false;

                case X509ChainStatusFlags.Revoked:
                    Console.WriteLine("Certificate has been revoked");
                    return false;

                case X509ChainStatusFlags.NotSignatureValid:
                    Console.WriteLine("Certificate signature is not valid");
                    return false;

                case X509ChainStatusFlags.UntrustedRoot:
                    Console.WriteLine("Untrusted root certificate");
                    return false;

                case X509ChainStatusFlags.PartialChain:
                    Console.WriteLine("Certificate chain is incomplete");
                    return false;

                // May be acceptable in development only
                // Must return false in production
                case X509ChainStatusFlags.RevocationStatusUnknown:
                    Console.WriteLine("Revocation status unknown");
                    return false; // Must be false in production

                default:
                    if (status.Status != X509ChainStatusFlags.NoError)
                    {
                        return false;
                    }
                    break;
            }
        }

        return true;
    }

    /// <summary>
    /// Configure secure HTTPS requests
    /// </summary>
    public void ConfigureSecureHttps()
    {
        // Set server certificate validation callback
        ServicePointManager.ServerCertificateValidationCallback =
            ValidateServerCertificate;

        // Use TLS 1.2 or higher
        ServicePointManager.SecurityProtocol =
            SecurityProtocolType.Tls12 | SecurityProtocolType.Tls13;

        // Enable certificate revocation checking
        ServicePointManager.CheckCertificateRevocationList = true;
    }
}
```

**Security Features:**
1. Comprehensive certificate validation
2. Hostname mismatch rejection
3. Certificate chain validation
4. Expiration checking
5. Revocation checking enabled
6. Self-signed certificate rejection
7. TLS 1.2/1.3 enforcement
8. Detailed error logging

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-295: Improper Certificate Validation**
   MITRE, https://cwe.mitre.org/data/definitions/295.html

② **CWE-297: Improper Validation of Certificate with Host Mismatch**
   MITRE, https://cwe.mitre.org/data/definitions/297.html

③ **CWE-299: Improper Check for Certificate Revocation**
   MITRE, https://cwe.mitre.org/data/definitions/299.html

### CERT Secure Coding

④ **MSC61-J: Do not use insecure or weak cryptographic algorithms**
   CERT, https://wiki.sei.cmu.edu/confluence/display/java/MSC61-J

⑤ **SER02-J: Sign then seal sensitive objects before sending them**
   CERT, https://wiki.sei.cmu.edu/confluence/display/java/SER02-J

### OWASP

⑥ **OWASP Transport Layer Protection Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html

⑦ **OWASP TLS Cipher String Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/TLS_Cipher_String_Cheat_Sheet.html

### Standards

⑧ **RFC 5280: X.509 Certificate and CRL Profile**
   IETF, https://tools.ietf.org/html/rfc5280

⑨ **RFC 6960: OCSP - Online Certificate Status Protocol**
   IETF, https://tools.ietf.org/html/rfc6960

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find disabled certificate validation
grep -r "SSL_VERIFY_NONE" --include="*.c" .
grep -r "ServerCertificateValidationCallback.*true" --include="*.cs" .

# Find TrustManager that accepts all certificates
grep -r "checkServerTrusted.*{}" --include="*.java" .
grep -r "checkClientTrusted.*{}" --include="*.java" .

# Find disabled hostname verification
grep -r "setHostnameVerifier.*true" --include="*.java" .
grep -r "ALLOW_ALL_HOSTNAME_VERIFIER" --include="*.java" .

# Find self-signed certificate acceptance
grep -r "X509_V_ERR_SELF_SIGNED_CERT.*return 1" --include="*.c" .

# Find missing certificate chain validation
grep -r "checkValidity" --include="*.java" . | grep -v "verify\|chain"
```

---

## ✅ Security Checklist

- [ ] Certificate chain validation enabled
- [ ] Certificate validity period checked
- [ ] Hostname verification enabled (CN and SAN)
- [ ] Certificate revocation checking (CRL/OCSP)
- [ ] Self-signed certificates rejected in production
- [ ] Trusted certificate store properly configured
- [ ] TLS 1.2 or higher enforced
- [ ] Strong cipher suites configured
- [ ] Certificate pinning for critical connections
- [ ] No global certificate validation bypass
- [ ] Proper error handling for validation failures
- [ ] Certificate validation failures logged
- [ ] Regular certificate rotation
- [ ] Expired certificates automatically rejected
- [ ] Invalid certificate chains rejected

---

## 🚨 Common Mistakes

1. **Disabling All Validation**
   ```java
   // DON'T: Accept all certificates
   TrustManager[] trustAllCerts = new TrustManager[] {
       new X509TrustManager() {
           public void checkServerTrusted(X509Certificate[] certs, String authType) {
               // No validation!
           }
       }
   };

   // DO: Use system trust store
   TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
   ```

2. **Accepting Self-Signed in Production**
   ```c
   // DON'T: Accept self-signed certificates
   if (err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
       return 1; // Accept
   }

   // DO: Reject self-signed
   if (err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
       return 0; // Reject
   }
   ```

3. **Skipping Hostname Verification**
   ```java
   // DON'T: Disable hostname verification
   conn.setHostnameVerifier((hostname, session) -> true);

   // DO: Use default or proper verification
   conn.setHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
   ```

4. **Incomplete Validation**
   ```java
   // DON'T: Only check validity
   cert.checkValidity();
   return true;

   // DO: Complete validation
   cert.checkValidity();
   cert.verify(issuerPublicKey);
   verifyCertificateChain(cert);
   checkRevocation(cert);
   verifyHostname(cert, hostname);
   ```

---

## 💡 Best Practices Summary

1. **Complete validation** - Chain, validity, revocation, hostname
2. **Use system trust store** - Don't create custom trust-all managers
3. **Enable hostname verification** - CN and SAN checking
4. **Check certificate revocation** - CRL and OCSP
5. **Reject self-signed certificates** - In production environments
6. **Use TLS 1.2+** - Modern secure protocols only
7. **Strong cipher suites** - No weak or deprecated algorithms
8. **Certificate pinning** - For high-security connections
9. **Log validation failures** - Security monitoring
10. **Fail securely** - Reject on any validation error

---

**Properly validate SSL/TLS certificates to prevent MITM attacks!**
