# Cleartext Storage of Sensitive Information (CWE-312)

**Severity**: 🟠 HIGH
**Category**: Security Features
**OWASP Top 10**: A02:2021 – Cryptographic Failures

---

## Overview

### Attack Description

Cleartext storage of sensitive information occurs when passwords, personal data, financial information, or other sensitive data is stored without encryption. If an attacker gains access to the database or file system, they can obtain the sensitive information directly. Similarly, transmitting sensitive information over unencrypted channels exposes it to network sniffing attacks.

### Impact

**Potential consequences:**
- Mass data breach
- Identity theft
- Financial fraud
- Account takeover
- Privacy violations
- Regulatory fines (GDPR: up to 20M EUR, CCPA, PCI-DSS)
- Reputation damage
- Legal liability

---

## Security Measures

### Key Principles

Sensitive information must be encrypted before storage or transmission. Passwords should be stored using one-way hash functions, and personal and financial information should be encrypted using strong cryptographic algorithms.

**Primary Defenses:**

1. **Encrypt at Rest**
   - Encrypt sensitive data in databases
   - Encrypt files containing sensitive information
   - Use strong encryption (AES-256)
   - Encrypt backups
   - Use database-level or application-level encryption

2. **Encrypt in Transit**
   - Use TLS 1.2+ for all network communications
   - HTTPS for web applications
   - Encrypted database connections
   - VPN for administrative access
   - Never transmit passwords over HTTP

3. **Password Storage**
   - Use bcrypt, scrypt, or Argon2 (NOT reversible encryption)
   - Never store passwords in plain text
   - Use unique salt per password
   - Sufficient work factor (bcrypt rounds: 12+)

4. **Key Management**
   - Store encryption keys separately from data
   - Use Hardware Security Module (HSM) or key vault
   - Rotate keys periodically
   - Never hardcode keys in source code

5. **Data Classification**
   - Identify what data is sensitive
   - Apply appropriate protection levels
   - Minimize collection of sensitive data
   - Securely delete data when no longer needed

---

## Code Examples

### Attack Scenario

The following example stores user passwords in plain text in the database. If the database is compromised, all user passwords are exposed.

**Attack:**
```sql
-- Attacker gains database access
SELECT username, password FROM users;

-- Results:
username  | password
----------|----------
admin     | Admin123!
john      | password123
alice     | MyP@ssw0rd

-- All passwords exposed in plain text!
```

---

### ❌ Vulnerable Code

#### Java - Plain Text Password Storage

```java
import java.sql.*;

public class UserRegistration {

    /**
     * Insecure: stores password in plain text
     */
    public void registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            // Stores password in plain text (dangerous!)
            pstmt.setString(2, password);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Plain text comparison during login
     */
    public boolean login(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Plain text comparison
                return password.equals(storedPassword);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
```

**Problems:**
1. **Plain text storage** - Password stored as-is
2. Database breach exposes all passwords
3. No encryption or hashing
4. Violates all security standards
5. Enables insider threats

---

#### Java - Unencrypted Sensitive Data

```java
public class CustomerData {

    /**
     * Insecure: stores credit card information in plain text
     */
    public void saveCustomerInfo(String customerId, String creditCardNumber,
                                 String ssn) {
        String sql = "INSERT INTO customers (id, credit_card, ssn) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customerId);
            // Stores credit card number in plain text (PCI-DSS violation!)
            pstmt.setString(2, creditCardNumber);
            // Stores SSN/national ID in plain text (privacy law violation!)
            pstmt.setString(3, ssn);

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
```

**Problems:**
1. Credit card numbers in plain text (PCI-DSS violation)
2. SSN/personal IDs in plain text (privacy law violation)
3. No encryption
4. Massive liability if breached
5. Regulatory fines

---

### ✅ Secure Code

#### Java - Secure Password Storage with bcrypt

```java
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class SecureUserRegistration {

    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Secure: hashes password with bcrypt before storage
     */
    public void registerUser(String username, String password) {
        // 1. Hash password (bcrypt automatically includes salt)
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));

        // 2. Store hashed password
        String sql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);  // Store hash

            pstmt.executeUpdate();

            System.out.println("User registered with hashed password");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hash verification during login
     */
    public boolean login(String username, String password) {
        String sql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                // bcrypt verification (constant-time comparison)
                return BCrypt.checkpw(password, storedHash);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
```

**Security Features:**
1. **bcrypt hashing** - One-way function (irreversible)
2. **Automatic salting** - Unique salt per password
3. **Work factor** - Slow by design (brute-force resistant)
4. **Constant-time comparison** - Timing attack resistant
5. No plain text storage

**Database Schema:**
```sql
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL,  -- bcrypt hash
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

#### ✅ Better Practice - Encrypted Sensitive Data Storage

```java
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.sql.*;
import java.util.Base64;

/**
 * Secure: stores sensitive information with encryption
 */
public class SecureCustomerData {

    private final SecretKey encryptionKey;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    public SecureCustomerData(SecretKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    /**
     * Encrypt and store customer information
     */
    public void saveCustomerInfo(String customerId, String creditCardNumber,
                                 String ssn) throws Exception {

        // 1. Encrypt sensitive data
        String encryptedCreditCard = encrypt(creditCardNumber);
        String encryptedSSN = encrypt(ssn);

        // 2. Store encrypted data
        String sql = "INSERT INTO customers " +
                    "(id, credit_card_encrypted, ssn_encrypted) " +
                    "VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customerId);
            pstmt.setString(2, encryptedCreditCard);
            pstmt.setString(3, encryptedSSN);

            pstmt.executeUpdate();

            System.out.println("Customer data saved with encryption");
        }
    }

    /**
     * Retrieve and decrypt customer information
     */
    public CustomerInfo getCustomerInfo(String customerId) throws Exception {
        String sql = "SELECT credit_card_encrypted, ssn_encrypted " +
                    "FROM customers WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Decrypt
                String creditCard = decrypt(rs.getString("credit_card_encrypted"));
                String ssn = decrypt(rs.getString("ssn_encrypted"));

                return new CustomerInfo(customerId, creditCard, ssn);
            }
        }

        return null;
    }

    /**
     * AES-GCM encryption
     */
    private String encrypt(String plainText) throws Exception {
        // Random IV generation
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Combine IV + ciphertext
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        // Base64 encode for database storage
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * AES-GCM decryption
     */
    private String decrypt(String encryptedBase64) throws Exception {
        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        // Extract ciphertext
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, gcmSpec);
        byte[] decrypted = cipher.doFinal(cipherText);

        return new String(decrypted, "UTF-8");
    }
}
```

**Database Schema:**
```sql
CREATE TABLE customers (
    id VARCHAR(50) PRIMARY KEY,
    credit_card_encrypted TEXT NOT NULL,  -- Encrypted with AES-256-GCM
    ssn_encrypted TEXT NOT NULL,          -- Encrypted with AES-256-GCM
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Security Features:**
1. **AES-256-GCM encryption** - Strong symmetric encryption
2. **Unique IV** - Random IV for each encryption
3. **Authenticated encryption** - GCM prevents tampering
4. **Base64 encoding** - Safe storage in database
5. Encryption key stored separately from data

---

#### ✅ Best Practice - Comprehensive Data Protection

```java
import javax.crypto.*;
import java.security.*;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Complete data protection service
 */
public class DataProtectionService {

    private final CryptographyService cryptoService;
    private final AuditLogger auditLogger;

    public DataProtectionService(CryptographyService cryptoService,
                                AuditLogger auditLogger) {
        this.cryptoService = cryptoService;
        this.auditLogger = auditLogger;
    }

    /**
     * Register user with secure password storage
     */
    public void registerUser(String username, String password, String email)
            throws Exception {

        // 1. Hash password (bcrypt)
        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));

        // 2. Encrypt email (PII)
        SecretKey emailEncKey = getEmailEncryptionKey();
        String encryptedEmail = cryptoService.encryptAES(email, emailEncKey);

        // 3. Store in database
        String sql = "INSERT INTO users (username, password_hash, email_encrypted) " +
                    "VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, passwordHash);
            pstmt.setString(3, encryptedEmail);

            pstmt.executeUpdate();

            // 4. Audit log
            auditLogger.log("USER_REGISTERED", username);
        }
    }

    /**
     * Store payment information (PCI-DSS compliant)
     */
    public void savePaymentMethod(String userId, String creditCardNumber,
                                  String cvv, String expiryDate)
            throws Exception {

        // 1. Get payment encryption key (from HSM/vault)
        SecretKey paymentKey = getPaymentEncryptionKey();

        // 2. Encrypt all sensitive payment data
        String encryptedCard = cryptoService.encryptAES(creditCardNumber, paymentKey);
        String encryptedCVV = cryptoService.encryptAES(cvv, paymentKey);
        String encryptedExpiry = cryptoService.encryptAES(expiryDate, paymentKey);

        // 3. Store encrypted data
        String sql = "INSERT INTO payment_methods " +
                    "(user_id, card_encrypted, cvv_encrypted, expiry_encrypted) " +
                    "VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, encryptedCard);
            pstmt.setString(3, encryptedCVV);
            pstmt.setString(4, encryptedExpiry);

            pstmt.executeUpdate();

            // 4. Audit log (no sensitive data in log!)
            auditLogger.log("PAYMENT_METHOD_ADDED", userId);
        }

        // 5. Clear sensitive data from memory
        clearString(creditCardNumber);
        clearString(cvv);
        clearString(expiryDate);
    }

    /**
     * Encrypt data before sending over network
     */
    public String prepareForTransmission(String sensitiveData) throws Exception {
        // Encrypt with transport key (separate from storage key)
        SecretKey transportKey = getTransportEncryptionKey();
        return cryptoService.encryptAES(sensitiveData, transportKey);
    }

    /**
     * Search encrypted data (using tokenization)
     */
    public String tokenizeCreditCard(String creditCardNumber) throws Exception {
        // 1. Generate token
        String token = "tok_" + UUID.randomUUID().toString();

        // 2. Encrypt actual card number
        SecretKey key = getPaymentEncryptionKey();
        String encrypted = cryptoService.encryptAES(creditCardNumber, key);

        // 3. Store mapping
        String sql = "INSERT INTO payment_tokens (token, card_encrypted) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, token);
            pstmt.setString(2, encrypted);

            pstmt.executeUpdate();
        }

        // 4. Return token (safe to store/transmit)
        return token;
    }

    /**
     * Secure data deletion
     */
    public void deleteUserData(String userId) throws Exception {
        // 1. Overwrite sensitive data before deletion
        String sql = "UPDATE users SET " +
                    "password_hash = 'DELETED', " +
                    "email_encrypted = 'DELETED' " +
                    "WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        }

        // 2. Then delete
        String deleteSql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {

            pstmt.setString(1, userId);
            pstmt.executeUpdate();

            // 3. Audit log
            auditLogger.log("USER_DATA_DELETED", userId);
        }
    }

    /**
     * Clear sensitive string from memory
     */
    private void clearString(String sensitive) {
        if (sensitive != null) {
            char[] chars = sensitive.toCharArray();
            Arrays.fill(chars, '\0');
        }
    }

    // Key management methods (retrieve from secure vault)
    private SecretKey getEmailEncryptionKey() { /* ... */ return null; }
    private SecretKey getPaymentEncryptionKey() { /* ... */ return null; }
    private SecretKey getTransportEncryptionKey() { /* ... */ return null; }
    private Connection getConnection() { /* ... */ return null; }
}
```

**Configuration (application.properties):**
```properties
# Encryption settings
encryption.algorithm=AES/GCM/NoPadding
encryption.key.size=256

# Key storage
key.storage.type=HSM
key.storage.url=https://vault.example.com

# Password hashing
password.bcrypt.rounds=12

# TLS settings
server.ssl.enabled=true
server.ssl.protocol=TLSv1.3
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
```

**Security Features:**
1. Password hashing with bcrypt
2. AES-256-GCM for data encryption
3. Separate keys for different data types
4. Tokenization for searchable encrypted data
5. Secure data deletion
6. Audit logging
7. Memory clearing for sensitive data
8. TLS for data in transit
9. HSM/vault for key storage

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-312: Cleartext Storage of Sensitive Information**
   MITRE, https://cwe.mitre.org/data/definitions/312.html

② **CWE-319: Cleartext Transmission of Sensitive Information**
   MITRE, https://cwe.mitre.org/data/definitions/319.html

③ **CWE-259: Use of Hard-coded Password**
   MITRE, https://cwe.mitre.org/data/definitions/259.html

### Compliance Standards

④ **PCI-DSS Requirement 3: Protect stored cardholder data**
   PCI Security Standards Council, https://www.pcisecuritystandards.org

⑤ **GDPR Article 32: Security of processing**
   EU GDPR, https://gdpr-info.eu/art-32-gdpr/

### OWASP

⑥ **A02:2021 – Cryptographic Failures**
   OWASP Top 10, https://owasp.org/Top10/A02_2021-Cryptographic_Failures/

⑦ **Password Storage Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find plain text password storage
grep -r "password.*VALUES" --include="*.java" . | grep -v "hash\|encrypted"
grep -r "setString.*password" --include="*.java" . | grep -v "hash"

# Find plain text sensitive data
grep -r "credit.*card.*VALUES\|ssn.*VALUES" --include="*.java" .

# Find hardcoded passwords/keys
grep -r "password.*=.*\".*\"" --include="*.java" .
grep -r "SecretKeySpec.*\"" --include="*.java" .

# Check for HTTP (not HTTPS)
grep -r "http://.*password\|http://.*login" --include="*.properties" .
```

---

## ✅ Security Checklist

- [ ] All passwords hashed with bcrypt/scrypt/Argon2
- [ ] No passwords stored in plain text or reversible encryption
- [ ] Sensitive data (PII, payment) encrypted at rest (AES-256)
- [ ] TLS 1.2+ used for all network communications
- [ ] Database connections encrypted
- [ ] Encryption keys stored in HSM/vault (not in code)
- [ ] Unique salt/IV for each encrypted value
- [ ] Audit logging for sensitive data access
- [ ] Secure data deletion implemented
- [ ] No sensitive data in logs
- [ ] Backup files encrypted
- [ ] Compliance requirements met (PCI-DSS, GDPR, HIPAA)

---

## 🎯 Data Classification Guide

### Sensitivity Levels

**Critical (Highest Protection):**
- Passwords → **bcrypt/Argon2** (one-way hash)
- Credit card numbers → **AES-256 encryption** + **tokenization**
- SSN/National IDs → **AES-256 encryption**
- Private keys → **HSM storage**

**High:**
- Email addresses → **AES-256 encryption**
- Phone numbers → **AES-256 encryption** or **masking**
- Addresses → **AES-256 encryption**
- Medical records → **AES-256 encryption** (HIPAA)

**Medium:**
- User preferences → **Optional encryption**
- Order history → **Access control** + **audit logging**
- Session tokens → **Secure random generation** + **HTTPS only**

**Low:**
- Public profile data → **Access control only**
- Product catalog → **No encryption needed**

---

## 🚨 Common Mistakes

1. **Plain Text Passwords**
   ```java
   // DON'T: Store password as-is
   pstmt.setString(2, password);

   // DO: Hash with bcrypt
   String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
   pstmt.setString(2, hash);
   ```

2. **Reversible Password Encryption**
   ```java
   // DON'T: Encrypt passwords (they should be hashed!)
   String encrypted = encrypt(password, key);

   // DO: Hash passwords (one-way)
   String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
   ```

3. **HTTP for Sensitive Data**
   ```java
   // DON'T: HTTP for login
   http://example.com/login

   // DO: HTTPS for all sensitive operations
   https://example.com/login
   ```

4. **Logging Sensitive Data**
   ```java
   // DON'T: Log passwords or PII
   logger.info("User login: " + username + ", password: " + password);

   // DO: Log without sensitive data
   logger.info("User login attempt: " + username);
   ```

---

## 💡 Best Practices Summary

1. **Encrypt at rest** - AES-256 for sensitive data in databases
2. **Encrypt in transit** - TLS 1.2+ for all network traffic
3. **Hash passwords** - bcrypt/Argon2 (never encrypt or store plain)
4. **Separate keys** - Different keys for different data types
5. **Key vault** - HSM or secure vault for key storage
6. **Audit logging** - Log access to sensitive data
7. **Data classification** - Know what's sensitive
8. **Minimize collection** - Only collect necessary data
9. **Secure deletion** - Overwrite before deleting
10. **Compliance** - Meet regulatory requirements (PCI-DSS, GDPR)

---

**Always encrypt sensitive data at rest and in transit - Never store passwords in plain text!**
