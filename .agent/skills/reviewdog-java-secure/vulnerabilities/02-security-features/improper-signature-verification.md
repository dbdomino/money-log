# Improper Signature Verification (CWE-347)

**Severity**: 🔴 HIGH
**Category**: Security Features
**OWASP Top 10**: A02:2021 – Cryptographic Failures

---

## Overview

### Attack Description

Improper signature verification occurs when applications fail to validate digital signatures on code, documents, or data. Digital signatures ensure authenticity and integrity - they prove the content came from a trusted source and hasn't been tampered with. Without proper verification, attackers can inject malicious code, tamper with data, or impersonate legitimate sources. In particular, failing to verify signatures on JAR files, executables, or software updates can lead to malicious code execution.

### Impact

**Potential consequences:**
- Malicious code execution
- Man-in-the-middle attacks
- Software supply chain attacks
- Data tampering
- Impersonation of trusted entities
- Backdoor installation
- Privilege escalation
- Compliance violations
- Loss of trust and reputation
- System compromise

---

## Security Measures

### Key Principles

Before executing JAR files, executables, or software updates, you must always verify the digital signature to confirm the code's origin and integrity. Signature verification should be performed through a trusted certificate chain.

**Primary Defenses:**

1. **JAR File Signature Verification**
   - Enable signature verification when opening JAR files
   - Verify all JAR entries are signed
   - Check for valid code signers
   - Validate certificate chains

2. **Certificate Validation**
   - Verify certificate validity period
   - Check certificate revocation status (CRL/OCSP)
   - Validate certificate chain to trusted root
   - Verify certificate purpose and key usage
   - Check for certificate pinning when applicable

3. **Code Signing Best Practices**
   - Sign all distributed code and libraries
   - Use strong signing algorithms (RSA 2048+, ECDSA)
   - Timestamp signatures for long-term validity
   - Protect private signing keys in HSM
   - Implement code signing certificate policies

4. **Runtime Verification**
   - Verify signatures before code execution
   - Re-verify after any modification
   - Log all verification attempts
   - Fail securely on verification errors
   - Don't fall back to unsigned code

5. **Trust Management**
   - Maintain list of trusted certificates
   - Implement certificate pinning for critical components
   - Regular certificate rotation
   - Monitor for certificate anomalies

---

## Code Examples

### Attack Scenario

An attacker injects a malicious JAR file into a system that lacks signature verification.

**Attack Sequence:**
```java
// 1. Attacker creates malicious JAR file
// malicious.jar contains backdoor code

// 2. Application loads JAR without verification
JarFile jf = new JarFile("malicious.jar"); // No signature check!

// 3. Malicious code executes with application privileges
// Backdoor established, data exfiltration begins
```

**Supply Chain Attack:**
```bash
# Attacker compromises update server
# Replaces legitimate update.jar with malicious version

# Application downloads and installs without signature verification
wget http://updates.example.com/update.jar
java -jar update.jar  # Malicious code executes!
```

---

### ❌ Vulnerable Code

#### Java - No Signature Verification

```java
import java.util.jar.*;
import java.io.*;

/**
 * Vulnerable code that loads JAR files without signature verification
 */
public class UnsafeJarLoader {

    /**
     * Opens and processes a JAR file without signature verification
     */
    public void loadJarFile(String jarPath) throws IOException {
        File f = new File(jarPath);

        // Setting the verify flag to false disables signature verification,
        // or using the default constructor means verification is not performed
        JarFile jf = new JarFile(f); // No signature verification!

        Enumeration<JarEntry> entries = jf.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Processing JAR entries without checking signatures
            InputStream is = jf.getInputStream(entry);

            // Malicious code will execute even if present
            processEntry(is);
        }

        jf.close();
    }

    private void processEntry(InputStream is) throws IOException {
        // JAR entry processing logic
        byte[] buffer = new byte[1024];
        while (is.read(buffer) != -1) {
            // Process data
        }
    }
}
```

**Problems:**
1. **No signature verification** - JAR file not verified
2. Uses default constructor or `verify=false`
3. No certificate validation
4. No signer information check
5. Malicious code can execute unchecked
6. Supply chain attack vulnerability

---

#### Java - Incomplete Signature Check

```java
import java.util.jar.*;
import java.security.cert.*;

public class WeakJarVerification {

    /**
     * Incomplete signature verification
     */
    public void loadJarWithWeakVerification(String jarPath) throws Exception {
        File f = new File(jarPath);

        // Verify flag set to true, but...
        JarFile jf = new JarFile(f, true);

        Enumeration<JarEntry> entries = jf.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();

            // Reads the entry but does not check the signature verification result
            InputStream is = jf.getInputStream(entry);
            byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                // Just reading - not checking signatures!
            }

            // No CodeSigner check - does not verify whether signatures were actually validated
            // entry.getCodeSigners() is never called
        }

        jf.close();
    }
}
```

**Problems:**
1. **Verification flag enabled but not checked** - Results ignored
2. No CodeSigner validation
3. No certificate chain verification
4. No certificate revocation check
5. Silent failure mode
6. False sense of security

---

#### C - No Signature Verification for Executables

```c
#include <stdio.h>
#include <stdlib.h>

/**
 * Executes a file without digital signature verification
 */
int execute_plugin(const char *plugin_path) {
    // Loading a plugin without digital signature verification
    // Malicious plugins can be executed

    // Direct execution without signature verification
    char command[256];
    snprintf(command, sizeof(command), "%s", plugin_path);

    // Executing unverified code
    return system(command);
}

/**
 * Loading DLL/SO without signature verification
 */
void* load_library_unsafe(const char *lib_path) {
    // Loading a library without signature verification
    void *handle = dlopen(lib_path, RTLD_LAZY);

    if (!handle) {
        fprintf(stderr, "Failed to load library\n");
        return NULL;
    }

    return handle;
}
```

**Problems:**
1. No signature verification before execution
2. No certificate validation
3. Unsigned code can execute
4. DLL/SO injection vulnerability
5. Malware can masquerade as plugin

---

#### Java - Accepting Self-Signed Certificates

```java
import java.security.cert.*;

public class WeakCertificateValidation {

    /**
     * Unconditionally accepts self-signed certificates
     */
    public boolean validateCertificate(X509Certificate cert) {
        try {
            // Only checks certificate validity period
            cert.checkValidity();

            // Does not check whether it is self-signed
            // Does not verify if issued by a trusted CA
            // No certificate chain verification

            return true; // Accepts any certificate
        } catch (CertificateException e) {
            return false;
        }
    }
}
```

**Problems:**
1. **Accepts self-signed certificates** - No CA validation
2. No certificate chain verification
3. No revocation check
4. No certificate pinning
5. Man-in-the-middle vulnerability

---

### ✅ Secure Code

#### Java - Proper JAR Signature Verification

```java
import java.util.jar.*;
import java.security.cert.*;
import java.security.*;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Class that securely verifies JAR file signatures
 */
public class SecureJarVerifier {

    private static final Logger logger = Logger.getLogger(
        SecureJarVerifier.class.getName()
    );

    /**
     * Verifies the digital signature of a JAR file
     *
     * @param jarPath the JAR file path
     * @return whether verification succeeded
     */
    public boolean verifyJarSignature(String jarPath) throws Exception {
        File f = new File(jarPath);

        if (!f.exists()) {
            logger.severe("JAR file does not exist: " + jarPath);
            return false;
        }

        // Set verify flag to true to enable signature verification
        JarFile jf = null;
        try {
            jf = new JarFile(f, true);

            // Read all entries to verify signatures
            Enumeration<JarEntry> entries = jf.entries();

            // Check that the JAR has at least one signed entry
            boolean hasSignedEntry = false;

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Skip directories and meta files
                if (entry.isDirectory() ||
                    entry.getName().startsWith("META-INF/")) {
                    continue;
                }

                // The entry must be fully read to trigger signature verification
                InputStream is = jf.getInputStream(entry);
                byte[] buffer = new byte[8192];
                while (is.read(buffer) != -1) {
                    // Reading the entire entry - this triggers signature verification
                }
                is.close();

                // Check signer information
                CodeSigner[] signers = entry.getCodeSigners();

                if (signers == null || signers.length == 0) {
                    logger.warning("Unsigned entry found: " + entry.getName());
                    return false; // All entries must be signed
                }

                hasSignedEntry = true;

                // Verify the certificate chain of each signer
                for (CodeSigner signer : signers) {
                    if (!verifyCodeSigner(signer)) {
                        logger.severe("Invalid signer: " + entry.getName());
                        return false;
                    }
                }

                logger.info("Verified entry: " + entry.getName() +
                           " (signers: " + signers.length + ")");
            }

            if (!hasSignedEntry) {
                logger.severe("No signed entries found.");
                return false;
            }

            logger.info("JAR file signature verification succeeded: " + jarPath);
            return true;

        } finally {
            if (jf != null) {
                jf.close();
            }
        }
    }

    /**
     * Verifies the certificate chain of a CodeSigner
     */
    private boolean verifyCodeSigner(CodeSigner signer) throws Exception {
        List<? extends Certificate> certChain = signer.getSignerCertPath()
                                                      .getCertificates();

        if (certChain == null || certChain.isEmpty()) {
            logger.warning("No certificate chain found.");
            return false;
        }

        // Verify the first certificate (code signing certificate)
        X509Certificate signingCert = (X509Certificate) certChain.get(0);

        // 1. Check certificate validity period
        try {
            signingCert.checkValidity();
        } catch (CertificateException e) {
            logger.severe("Certificate validity check failed: " + e.getMessage());
            return false;
        }

        // 2. Check certificate purpose (code signing)
        boolean[] keyUsage = signingCert.getKeyUsage();
        if (keyUsage != null && !keyUsage[0]) { // digitalSignature
            logger.warning("Certificate is not intended for code signing.");
            return false;
        }

        // 3. Verify certificate chain (up to trusted root CA)
        if (!verifyCertificateChain(certChain)) {
            logger.severe("Certificate chain verification failed");
            return false;
        }

        // 4. Check certificate revocation status (CRL or OCSP)
        if (!checkCertificateRevocation(signingCert)) {
            logger.severe("Certificate has been revoked.");
            return false;
        }

        return true;
    }

    /**
     * Verifies the certificate chain up to a trusted root CA
     */
    private boolean verifyCertificateChain(List<? extends Certificate> certChain)
            throws Exception {

        // Load the system's trusted certificate store
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        // Or load a custom trust store
        // FileInputStream fis = new FileInputStream("truststore.jks");
        // trustStore.load(fis, "password".toCharArray());

        // Configure PKIXParameters
        PKIXParameters params = new PKIXParameters(trustStore);
        params.setRevocationEnabled(false); // CRL is checked separately

        // Create CertPath
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath certPath = cf.generateCertPath(certChain);

        // Verify certificate chain with CertPathValidator
        CertPathValidator validator = CertPathValidator.getInstance("PKIX");

        try {
            PKIXCertPathValidatorResult result =
                (PKIXCertPathValidatorResult) validator.validate(certPath, params);

            logger.info("Certificate chain verification succeeded. Trust Anchor: " +
                       result.getTrustAnchor());
            return true;

        } catch (CertPathValidatorException e) {
            logger.severe("Certificate chain verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks certificate revocation status
     */
    private boolean checkCertificateRevocation(X509Certificate cert)
            throws Exception {

        // Check CRL (Certificate Revocation List)
        String crlUrl = getCRLDistributionPoint(cert);

        if (crlUrl != null) {
            if (isCertificateRevoked(cert, crlUrl)) {
                logger.warning("Certificate has been revoked by CRL.");
                return false;
            }
        }

        // Also consider OCSP (Online Certificate Status Protocol) check
        // OCSPResp response = checkOCSP(cert);

        return true;
    }

    /**
     * Extracts the CRL Distribution Point URL
     */
    private String getCRLDistributionPoint(X509Certificate cert) {
        try {
            byte[] crlExtension = cert.getExtensionValue("2.5.29.31");
            if (crlExtension != null) {
                // CRL Distribution Point parsing logic
                // Actual implementation requires ASN.1 parsing
                return "http://crl.example.com/ca.crl";
            }
        } catch (Exception e) {
            logger.warning("Failed to extract CRL Distribution Point: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks certificate revocation status using CRL
     */
    private boolean isCertificateRevoked(X509Certificate cert, String crlUrl)
            throws Exception {

        // Download CRL
        URL url = new URL(crlUrl);
        InputStream crlStream = url.openStream();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509CRL crl = (X509CRL) cf.generateCRL(crlStream);
        crlStream.close();

        // Check the certificate against the CRL
        return crl.isRevoked(cert);
    }
}
```

**Security Features:**
1. **JAR signature verification enabled** - `JarFile(f, true)`
2. **All entries verified** - Reads entire entry to trigger verification
3. **CodeSigner validation** - Checks all signers for each entry
4. **Certificate chain verification** - Validates to trusted root CA
5. **Certificate validity check** - Ensures not expired
6. **Certificate purpose verification** - Confirms code signing usage
7. **Revocation checking** - Verifies certificate not revoked (CRL)
8. **Comprehensive logging** - Audit trail of verification
9. **Fail securely** - Rejects if any verification fails
10. **No unsigned entries** - All entries must be signed

---

#### Java - Certificate Verification

```java
import java.security.cert.*;
import java.security.*;
import java.io.*;
import java.util.*;

/**
 * Digital certificate verification
 */
public class CertificateVerifier {

    /**
     * X.509 certificate verification
     */
    public boolean verifyCertificate(X509Certificate toVerify,
                                     X509Certificate signingCert)
            throws Exception {

        // 1. Check certificate validity period
        try {
            toVerify.checkValidity();
            logger.info("Certificate validity period: OK");
        } catch (CertificateExpiredException e) {
            logger.severe("Certificate has expired.");
            return false;
        } catch (CertificateNotYetValidException e) {
            logger.severe("Certificate is not yet valid.");
            return false;
        }

        // 2. Signature verification - verify with the parent certificate's public key
        try {
            toVerify.verify(signingCert.getPublicKey());
            logger.info("Certificate signature verification: succeeded");
        } catch (SignatureException e) {
            logger.severe("Certificate signature verification failed: " + e.getMessage());
            return false;
        } catch (InvalidKeyException e) {
            logger.severe("Invalid key: " + e.getMessage());
            return false;
        }

        // 3. Check certificate subject
        String subject = toVerify.getSubjectDN().getName();
        logger.info("Certificate subject: " + subject);

        // 4. Check certificate issuer
        String issuer = toVerify.getIssuerDN().getName();
        String expectedIssuer = signingCert.getSubjectDN().getName();

        if (!issuer.equals(expectedIssuer)) {
            logger.severe("Certificate issuer does not match.");
            return false;
        }

        // 5. Check Basic Constraints (for CA certificates)
        int pathLen = toVerify.getBasicConstraints();
        if (pathLen >= 0) {
            logger.info("This is a CA certificate. Path Length: " + pathLen);
        }

        // 6. Check Key Usage
        boolean[] keyUsage = toVerify.getKeyUsage();
        if (keyUsage != null) {
            logger.info("Key Usage:");
            if (keyUsage[0]) logger.info("  - Digital Signature");
            if (keyUsage[1]) logger.info("  - Non Repudiation");
            if (keyUsage[2]) logger.info("  - Key Encipherment");
            if (keyUsage[3]) logger.info("  - Data Encipherment");
            if (keyUsage[4]) logger.info("  - Key Agreement");
            if (keyUsage[5]) logger.info("  - Certificate Signing");
            if (keyUsage[6]) logger.info("  - CRL Signing");
        }

        // 7. Check Extended Key Usage
        try {
            List<String> extKeyUsage = toVerify.getExtendedKeyUsage();
            if (extKeyUsage != null) {
                logger.info("Extended Key Usage: " + extKeyUsage);
            }
        } catch (CertificateParsingException e) {
            logger.warning("Failed to parse Extended Key Usage");
        }

        // 8. Check certificate revocation status (CRL or OCSP)
        if (!checkRevocationStatus(toVerify)) {
            logger.severe("Certificate has been revoked.");
            return false;
        }

        logger.info("Certificate verification complete: succeeded");
        return true;
    }

    /**
     * Check certificate revocation status
     */
    private boolean checkRevocationStatus(X509Certificate cert) {
        // Check revocation status via CRL or OCSP
        // Actual implementation required
        return true;
    }
}
```

**Security Features:**
1. **Validity period check** - `checkValidity()`
2. **Signature verification** - `verify(signingCert.getPublicKey())`
3. **Issuer validation** - Confirms certificate chain
4. **Key usage verification** - Ensures proper certificate purpose
5. **Extended key usage check** - Additional purpose validation
6. **Revocation status check** - CRL/OCSP verification
7. **Comprehensive logging** - Audit trail

---

#### C# - Code Signature Verification

```csharp
using System;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Pkcs;

/// <summary>
/// Code signature verification
/// </summary>
public class CodeSignatureVerifier
{
    /// <summary>
    /// Verifies the digital signature of a file
    /// </summary>
    public bool VerifyFileSignature(string filePath)
    {
        try
        {
            // Load the signed file
            byte[] fileContent = File.ReadAllBytes(filePath);

            // Create SignedCms object
            SignedCms signedCms = new SignedCms();
            signedCms.Decode(fileContent);

            // Verify signature
            signedCms.CheckSignature(true); // Also verifies certificate chain

            // Check signer information
            foreach (SignerInfo signer in signedCms.SignerInfos)
            {
                // Verify certificate
                if (!VerifyCertificate(signer.Certificate))
                {
                    Console.WriteLine("Certificate verification failed");
                    return false;
                }

                // Check signature algorithm
                Console.WriteLine($"Signature algorithm: {signer.DigestAlgorithm.FriendlyName}");

                // Check timestamp
                if (signer.SignedAttributes != null)
                {
                    Console.WriteLine("Signature includes a timestamp.");
                }
            }

            Console.WriteLine("File signature verification succeeded");
            return true;
        }
        catch (CryptographicException ex)
        {
            Console.WriteLine($"Signature verification failed: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// X.509 certificate verification
    /// </summary>
    private bool VerifyCertificate(X509Certificate2 cert)
    {
        // 1. Certificate chain builder
        X509Chain chain = new X509Chain();

        // 2. Chain verification settings
        chain.ChainPolicy.RevocationMode = X509RevocationMode.Online;
        chain.ChainPolicy.RevocationFlag = X509RevocationFlag.EntireChain;
        chain.ChainPolicy.VerificationFlags = X509VerificationFlags.NoFlag;

        // 3. Build and verify chain
        bool chainBuilt = chain.Build(cert);

        if (!chainBuilt)
        {
            Console.WriteLine("Certificate chain verification failed:");
            foreach (X509ChainStatus status in chain.ChainStatus)
            {
                Console.WriteLine($"  - {status.StatusInformation}");
            }
            return false;
        }

        Console.WriteLine("Certificate chain verification succeeded");
        return true;
    }
}
```

**Security Features:**
1. Full signature verification with `CheckSignature(true)`
2. Certificate chain validation
3. Revocation checking enabled
4. Signer information verification
5. Timestamp validation

---

#### C - Signature Verification with OpenSSL

```c
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/x509.h>
#include <openssl/pem.h>
#include <stdio.h>
#include <string.h>

/**
 * Digital signature verification (using OpenSSL)
 */
int verify_signature(const char *data, size_t data_len,
                     const unsigned char *signature, size_t sig_len,
                     const char *cert_file)
{
    FILE *fp;
    X509 *cert = NULL;
    EVP_PKEY *pubkey = NULL;
    EVP_MD_CTX *mdctx = NULL;
    int result = 0;

    // 1. Load certificate file
    fp = fopen(cert_file, "r");
    if (!fp) {
        fprintf(stderr, "Failed to open certificate file\n");
        return 0;
    }

    cert = PEM_read_X509(fp, NULL, NULL, NULL);
    fclose(fp);

    if (!cert) {
        fprintf(stderr, "Failed to read certificate\n");
        return 0;
    }

    // 2. Check certificate validity period
    if (X509_cmp_current_time(X509_get_notBefore(cert)) >= 0 ||
        X509_cmp_current_time(X509_get_notAfter(cert)) <= 0) {
        fprintf(stderr, "Certificate validity period has expired\n");
        X509_free(cert);
        return 0;
    }

    // 3. Extract public key
    pubkey = X509_get_pubkey(cert);
    if (!pubkey) {
        fprintf(stderr, "Failed to extract public key\n");
        X509_free(cert);
        return 0;
    }

    // 4. Create signature verification context
    mdctx = EVP_MD_CTX_new();
    if (!mdctx) {
        fprintf(stderr, "Failed to create MD context\n");
        goto cleanup;
    }

    // 5. Initialize verification (using SHA-256)
    if (EVP_DigestVerifyInit(mdctx, NULL, EVP_sha256(), NULL, pubkey) != 1) {
        fprintf(stderr, "Failed to initialize verification\n");
        goto cleanup;
    }

    // 6. Update data
    if (EVP_DigestVerifyUpdate(mdctx, data, data_len) != 1) {
        fprintf(stderr, "Failed to update data\n");
        goto cleanup;
    }

    // 7. Verify signature
    if (EVP_DigestVerifyFinal(mdctx, signature, sig_len) == 1) {
        printf("Signature verification succeeded\n");
        result = 1;
    } else {
        fprintf(stderr, "Signature verification failed\n");
        result = 0;
    }

cleanup:
    if (mdctx) EVP_MD_CTX_free(mdctx);
    if (pubkey) EVP_PKEY_free(pubkey);
    if (cert) X509_free(cert);

    return result;
}

/**
 * Certificate chain verification
 */
int verify_certificate_chain(X509 *cert, X509_STORE *trust_store) {
    X509_STORE_CTX *ctx;
    int result;

    // Create verification context
    ctx = X509_STORE_CTX_new();
    if (!ctx) {
        return 0;
    }

    // Initialize verification
    if (!X509_STORE_CTX_init(ctx, trust_store, cert, NULL)) {
        X509_STORE_CTX_free(ctx);
        return 0;
    }

    // Verify certificate chain
    result = X509_verify_cert(ctx);

    if (result != 1) {
        int err = X509_STORE_CTX_get_error(ctx);
        fprintf(stderr, "Certificate chain verification failed: %s\n",
                X509_verify_cert_error_string(err));

        // Do not accept self-signed certificate errors
        if (err == X509_V_ERR_DEPTH_ZERO_SELF_SIGNED_CERT ||
            err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
            fprintf(stderr, "Self-signed certificates are not allowed\n");
        }
    }

    X509_STORE_CTX_free(ctx);
    return result;
}
```

**Security Features:**
1. Certificate file loading and validation
2. Certificate validity period check
3. Public key extraction
4. Digital signature verification with SHA-256
5. Certificate chain validation
6. Rejection of self-signed certificates
7. Proper error handling

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-347: Improper Verification of Cryptographic Signature**
   MITRE, https://cwe.mitre.org/data/definitions/347.html

② **CWE-295: Improper Certificate Validation**
   MITRE, https://cwe.mitre.org/data/definitions/295.html

③ **CWE-494: Download of Code Without Integrity Check**
   MITRE, https://cwe.mitre.org/data/definitions/494.html

### CERT Secure Coding

④ **SIG00-J: Do not rely on the default automatic signature verification**
   CERT, https://wiki.sei.cmu.edu/confluence/display/java/SIG00-J

⑤ **SIG01-J: Verify JAR signatures**
   CERT, https://wiki.sei.cmu.edu/confluence/display/java/SIG01-J

### OWASP

⑥ **OWASP Code Signing**
   OWASP, https://owasp.org/www-community/controls/Code_Signing

⑦ **OWASP Transport Layer Protection Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Transport_Layer_Protection_Cheat_Sheet.html

### Standards

⑧ **RFC 5280: Internet X.509 Public Key Infrastructure Certificate and CRL Profile**
   IETF, https://tools.ietf.org/html/rfc5280

⑨ **PKCS #7: Cryptographic Message Syntax Standard**
   RSA Laboratories

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find JarFile without signature verification
grep -r "new JarFile" --include="*.java" . | grep -v "true"

# Find JAR loading without verification
grep -r "JarFile.*false" --include="*.java" .

# Find missing CodeSigner checks
grep -r "JarEntry" --include="*.java" . | grep -v "getCodeSigners"

# Find certificate validation issues
grep -r "checkValidity" --include="*.java" . | grep -v "verify"

# Find self-signed certificate acceptance
grep -r "X509_V_ERR_SELF_SIGNED" --include="*.c" .

# Find missing signature verification
grep -r "verify.*false\|setVerify.*false" --include="*.java" .
```

---

## ✅ Security Checklist

- [ ] JAR files opened with verification enabled (`JarFile(file, true)`)
- [ ] All JAR entries read completely to trigger verification
- [ ] CodeSigners validated for each entry
- [ ] Certificate validity period checked
- [ ] Certificate chain validated to trusted root CA
- [ ] Certificate revocation status checked (CRL/OCSP)
- [ ] Certificate purpose/key usage verified
- [ ] Self-signed certificates rejected
- [ ] Strong signature algorithms required (RSA 2048+, SHA-256+)
- [ ] Signature verification failures logged
- [ ] No fallback to unsigned code
- [ ] Trusted certificate store properly configured
- [ ] Certificate pinning for critical components
- [ ] Regular certificate rotation policy
- [ ] Code signing performed on all distributed code

---

## 🚨 Common Mistakes

1. **Verification Flag Not Set**
   ```java
   // DON'T: Default constructor doesn't verify
   JarFile jf = new JarFile(file);

   // DO: Enable verification
   JarFile jf = new JarFile(file, true);
   ```

2. **Not Checking CodeSigners**
   ```java
   // DON'T: Read entry but don't check signers
   InputStream is = jf.getInputStream(entry);
   // Process without verification

   // DO: Check CodeSigners
   CodeSigner[] signers = entry.getCodeSigners();
   if (signers == null || signers.length == 0) {
       throw new SecurityException("Unsigned entry");
   }
   ```

3. **Accepting Self-Signed Certificates**
   ```c
   // DON'T: Accept self-signed certificates
   if (err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
       return 1; // Accepting!
   }

   // DO: Reject self-signed certificates
   if (err == X509_V_ERR_SELF_SIGNED_CERT_IN_CHAIN) {
       return 0; // Reject
   }
   ```

4. **Not Reading Full Entry**
   ```java
   // DON'T: Partial read doesn't trigger verification
   byte[] buffer = new byte[100];
   is.read(buffer);

   // DO: Read entire entry
   while (is.read(buffer) != -1) {
       // Complete read triggers verification
   }
   ```

---

## 💡 Best Practices Summary

1. **Always verify signatures** - Before executing any code
2. **Enable JAR verification** - Use `JarFile(file, true)`
3. **Check all entries** - Every entry must be signed
4. **Validate certificate chains** - To trusted root CA
5. **Check revocation status** - Use CRL or OCSP
6. **Reject self-signed** - Don't trust self-signed certificates
7. **Use strong algorithms** - RSA 2048+, SHA-256+
8. **Timestamp signatures** - For long-term validity
9. **Log verification events** - Audit trail for security
10. **Fail securely** - Reject on any verification failure

---

**Always verify digital signatures on code and data!**
