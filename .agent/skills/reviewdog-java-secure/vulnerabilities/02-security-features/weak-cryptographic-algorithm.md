# Use of Weak Cryptographic Algorithm (CWE-327)

**Severity**: 🟠 HIGH
**Category**: Security Features
**OWASP Top 10**: A02:2021 – Cryptographic Failures

---

## Overview

### Attack Description

When encrypting plaintext data for protection, using cryptographic algorithms known to be insecure can allow attackers to decrypt the ciphertext and expose sensitive information. For example, algorithms such as DES and RC2 are vulnerable to brute-force attacks due to their short key lengths (DES uses only a 56-bit key). Attackers can exploit known weaknesses in these algorithms to decrypt data, forge signatures, or bypass authentication mechanisms.

### Impact

**Potential consequences:**
- Data confidentiality breach (decryption of encrypted data)
- Authentication bypass
- Digital signature forgery
- Man-in-the-middle attacks
- Compliance violations (PCI-DSS, HIPAA, GDPR)
- Loss of customer trust
- Financial and legal consequences

---

## Security Measures

### Key Principles

Prohibit the use of cryptographic algorithms known to be insecure, and use strong cryptographic algorithms instead. Use algorithms recommended by NIST, such as AES, RSA, and SHA-256.

**Primary Defenses:**

1. **Use Strong Algorithms**
   - **Symmetric Encryption**: AES-256, ChaCha20
   - **Asymmetric Encryption**: RSA-2048+, ECC (P-256+)
   - **Hashing**: SHA-256, SHA-3, bcrypt, scrypt, Argon2
   - **Avoid**: DES, 3DES, RC2, RC4, MD5, SHA-1

2. **Adequate Key Lengths**
   - AES: Minimum 128-bit (prefer 256-bit)
   - RSA: Minimum 2048-bit (prefer 3072-bit or 4096-bit)
   - ECC: Minimum 256-bit
   - Never use less than recommended lengths

3. **Secure Modes of Operation**
   - Use authenticated encryption: **GCM**, **CCM**
   - Avoid ECB mode (insecure)
   - Use CBC with HMAC or authenticated modes
   - Always use IV/Nonce (random, unique)

4. **Key Management**
   - Generate cryptographically secure random keys
   - Store keys securely (HSM, key vault, encrypted)
   - Rotate keys periodically
   - Never hard-code keys in source code

5. **Password Hashing**
   - Use **bcrypt**, **scrypt**, or **Argon2** (not SHA-256 alone)
   - Include salt (unique per password)
   - Use sufficient work factor / iterations
   - Never store passwords in plain text

---

## Code Examples

### Attack Scenario

The following example uses the insecure DES algorithm for encryption. DES can be easily cracked with modern computing power due to its 56-bit key length.

**Attack:**
```
1. Intercept encrypted traffic using DES
2. Use brute-force attack (56-bit keyspace = 2^56 possibilities)
3. Modern GPU can crack DES in hours/days
4. Decrypt sensitive data
```

---

### ❌ Vulnerable Code

#### Java - DES Encryption (Weak)

```java
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

public class WeakEncryption {

    /**
     * Insecure DES encryption
     */
    public byte[] encrypt(String plainText, String key) throws Exception {
        // Using DES algorithm (vulnerable!)
        SecretKeySpec keySpec = new SecretKeySpec(
            key.getBytes(),
            "DES"  // ❌ DES is weak (56-bit key)
        );

        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        return cipher.doFinal(plainText.getBytes());
    }

    public String decrypt(byte[] cipherText, String key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(
            key.getBytes(),
            "DES"
        );

        Cipher cipher = Cipher.getInstance("DES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted);
    }
}
```

**Problems:**
1. **DES algorithm** - Only 56-bit key (weak)
2. **ECB mode** (default when not specified) - Insecure
3. **No IV** - Deterministic encryption
4. Key from string (likely not cryptographically secure)
5. No authenticated encryption (vulnerable to tampering)

---

#### Java - MD5 Hashing (Broken)

```java
import java.security.*;

public class WeakHashing {

    /**
     * Insecure MD5 hash
     */
    public String hashPassword(String password) throws Exception {
        // Using MD5 algorithm (vulnerable!)
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(password.getBytes());

        // Convert to hex
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }
}
```

**Problems:**
1. **MD5 is broken** - Collision attacks possible
2. **No salt** - Vulnerable to rainbow table attacks
3. **Fast hash** - Vulnerable to brute-force
4. Not designed for password hashing
5. Same password = same hash (no uniqueness)

---

### ✅ Secure Code

#### Java - AES-256 Encryption (Strong)

```java
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

public class StrongEncryption {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;  // AES-256
    private static final int GCM_TAG_LENGTH = 128;  // 16 bytes
    private static final int GCM_IV_LENGTH = 12;  // 96 bits recommended for GCM

    /**
     * Secure AES-256-GCM encryption
     */
    public byte[] encrypt(String plainText, SecretKey key) throws Exception {
        // 1. Generate random IV (GCM mode)
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 2. Configure GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // 3. AES-GCM encryption
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // 4. Combine IV and ciphertext (IV is not secret)
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        return combined;
    }

    public String decrypt(byte[] combined, SecretKey key) throws Exception {
        // 1. Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        // 2. Extract ciphertext
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // 3. Configure GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // 4. Decrypt
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        byte[] decrypted = cipher.doFinal(cipherText);
        return new String(decrypted, "UTF-8");
    }

    /**
     * Secure key generation
     */
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(KEY_SIZE, new SecureRandom());
        return keyGen.generateKey();
    }
}
```

**Security Features:**
1. **AES-256** - Strong symmetric encryption
2. **GCM mode** - Authenticated encryption (prevents tampering)
3. **Random IV** - Unique per encryption
4. **SecureRandom** - Cryptographically secure randomness
5. **96-bit IV** - Recommended for GCM
6. **128-bit auth tag** - Strong authentication

---

#### ✅ Better Practice - Password Hashing with bcrypt

```java
import org.mindrot.jbcrypt.BCrypt;

/**
 * Secure password hashing (bcrypt)
 */
public class SecurePasswordHashing {

    // bcrypt work factor (log2 rounds)
    // 10 = 2^10 = 1024 iterations
    // Higher = more secure but slower
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash password
     */
    public String hashPassword(String password) {
        // bcrypt automatically generates and includes a salt
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify password
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

**Security Features:**
1. **bcrypt** - Designed for password hashing
2. **Automatic salting** - Unique salt per password
3. **Work factor** - Adjustable computational cost
4. **Slow by design** - Resistant to brute-force
5. **Constant-time comparison** - Prevents timing attacks

---

#### ✅ Best Practice - Complete Cryptography Service

```java
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.Base64;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Comprehensive cryptography service
 */
public class CryptographyService {

    // AES Configuration
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    // RSA Configuration
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final int RSA_KEY_SIZE = 2048;

    // Password Hashing Configuration
    private static final int BCRYPT_ROUNDS = 12;

    private final SecureRandom secureRandom;

    public CryptographyService() {
        this.secureRandom = new SecureRandom();
    }

    // ==================== Symmetric Encryption (AES-GCM) ====================

    /**
     * Generate AES-256 key
     */
    public SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_SIZE, secureRandom);
        return keyGen.generateKey();
    }

    /**
     * Encrypt with AES-256-GCM
     */
    public String encryptAES(String plainText, SecretKey key)
            throws Exception {

        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);

        // Setup GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        // Combine IV + ciphertext
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

        // Base64 encode for storage
        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt with AES-256-GCM
     */
    public String decryptAES(String encryptedBase64, SecretKey key)
            throws Exception {

        // Decode from Base64
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);

        // Extract ciphertext
        byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        // Setup GCM parameters
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

        // Decrypt
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        byte[] decrypted = cipher.doFinal(cipherText);

        return new String(decrypted, "UTF-8");
    }

    // ==================== Asymmetric Encryption (RSA) ====================

    /**
     * Generate RSA-2048 key pair
     */
    public KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyGen.initialize(RSA_KEY_SIZE, secureRandom);
        return keyGen.generateKeyPair();
    }

    /**
     * Encrypt with RSA public key
     */
    public String encryptRSA(String plainText, PublicKey publicKey)
            throws Exception {

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    /**
     * Decrypt with RSA private key
     */
    public String decryptRSA(String encryptedBase64, PrivateKey privateKey)
            throws Exception {

        byte[] cipherText = Base64.getDecoder().decode(encryptedBase64);

        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(cipherText);

        return new String(decrypted, "UTF-8");
    }

    // ==================== Hashing (SHA-256) ====================

    /**
     * SHA-256 hash (for data integrity, NOT passwords!)
     */
    public String hashSHA256(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes("UTF-8"));

        return Base64.getEncoder().encodeToString(hash);
    }

    // ==================== Password Hashing (bcrypt) ====================

    /**
     * Hash password with bcrypt
     */
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify password against bcrypt hash
     */
    public boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Digital Signatures (RSA-SHA256) ====================

    /**
     * Sign data with RSA private key
     */
    public String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey, secureRandom);
        signature.update(data.getBytes("UTF-8"));

        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * Verify signature with RSA public key
     */
    public boolean verifySignature(String data, String signatureBase64,
                                   PublicKey publicKey) throws Exception {

        byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes("UTF-8"));

        return signature.verify(signatureBytes);
    }
}
```

**Usage Examples:**
```java
public class CryptoUsageExample {
    public static void main(String[] args) throws Exception {
        CryptographyService crypto = new CryptographyService();

        // 1. Symmetric Encryption (AES-256-GCM)
        SecretKey aesKey = crypto.generateAESKey();
        String encrypted = crypto.encryptAES("Sensitive data", aesKey);
        String decrypted = crypto.decryptAES(encrypted, aesKey);

        // 2. Asymmetric Encryption (RSA-2048)
        KeyPair rsaKeyPair = crypto.generateRSAKeyPair();
        String rsaEncrypted = crypto.encryptRSA("Secret message",
            rsaKeyPair.getPublic());
        String rsaDecrypted = crypto.decryptRSA(rsaEncrypted,
            rsaKeyPair.getPrivate());

        // 3. Password Hashing (bcrypt)
        String passwordHash = crypto.hashPassword("MyP@ssw0rd!");
        boolean isValid = crypto.verifyPassword("MyP@ssw0rd!", passwordHash);

        // 4. Digital Signature (RSA-SHA256)
        String signature = crypto.sign("Document content",
            rsaKeyPair.getPrivate());
        boolean verified = crypto.verifySignature("Document content",
            signature, rsaKeyPair.getPublic());

        // 5. Data Integrity (SHA-256)
        String hash = crypto.hashSHA256("File content");
    }
}
```

**Security Features:**
1. AES-256-GCM for symmetric encryption
2. RSA-2048+ with OAEP padding
3. bcrypt for password hashing
4. SHA-256 for data integrity
5. RSA-SHA256 for digital signatures
6. Proper IV/salt generation
7. Authenticated encryption (GCM)
8. SecureRandom for all randomness

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-327: Use of a Broken or Risky Cryptographic Algorithm**
   MITRE, https://cwe.mitre.org/data/definitions/327.html

② **CWE-326: Inadequate Encryption Strength**
   MITRE, https://cwe.mitre.org/data/definitions/326.html

③ **CWE-328: Use of Weak Hash**
   MITRE, https://cwe.mitre.org/data/definitions/328.html

### NIST Recommendations

④ **NIST Special Publication 800-175B: Guideline for Using Cryptographic Standards**
   NIST, https://csrc.nist.gov/publications/detail/sp/800-175b/rev-1/final

⑤ **NIST SP 800-57: Recommendation for Key Management**
   NIST, https://csrc.nist.gov/publications/detail/sp/800-57-part-1/rev-5/final

### OWASP

⑥ **A02:2021 – Cryptographic Failures**
   OWASP Top 10, https://owasp.org/Top10/A02_2021-Cryptographic_Failures/

⑦ **Cryptographic Storage Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find weak algorithms
grep -r "getInstance.*DES\|getInstance.*RC2\|getInstance.*RC4" --include="*.java" .
grep -r "getInstance.*MD5\|getInstance.*SHA1\|getInstance.*SHA-1" --include="*.java" .

# Find ECB mode (insecure)
grep -r "AES/ECB" --include="*.java" .

# Find hardcoded keys
grep -r "SecretKeySpec.*\"" --include="*.java" .

# Find weak RSA key sizes
grep -r "initialize.*512\|initialize.*1024" --include="*.java" .
```

---

## ✅ Security Checklist

- [ ] No DES, 3DES, RC2, RC4, Blowfish
- [ ] AES-256 or ChaCha20 for symmetric encryption
- [ ] RSA-2048+ or ECC-256+ for asymmetric encryption
- [ ] GCM or CCM mode for authenticated encryption
- [ ] No ECB mode
- [ ] Random IV/Nonce for each encryption
- [ ] bcrypt, scrypt, or Argon2 for passwords
- [ ] No MD5 or SHA-1 for security purposes
- [ ] SHA-256+ for hashing
- [ ] SecureRandom for key generation
- [ ] No hardcoded cryptographic keys
- [ ] Key storage in secure vault/HSM
- [ ] Regular key rotation
- [ ] Crypto library kept up-to-date

---

## 🎯 Cryptographic Algorithm Guide

### ❌ DEPRECATED / BROKEN Algorithms

| Algorithm | Type | Status | Reason |
|-----------|------|--------|--------|
| DES | Symmetric | ❌ Broken | 56-bit key too small |
| 3DES | Symmetric | ❌ Deprecated | Replaced by AES |
| RC2 | Symmetric | ❌ Broken | Weak key schedule |
| RC4 | Stream | ❌ Broken | Biases in keystream |
| MD5 | Hash | ❌ Broken | Collision attacks |
| SHA-1 | Hash | ❌ Broken | Collision attacks (2017) |
| RSA-1024 | Asymmetric | ❌ Weak | Key too small |

### ✅ RECOMMENDED Algorithms

**Symmetric Encryption:**
- **AES-256** (Cipher: AES/GCM/NoPadding)
- **ChaCha20-Poly1305** (Authenticated encryption)

**Asymmetric Encryption:**
- **RSA-2048+** (Prefer 3072 or 4096 for long-term)
- **ECC P-256+** (Elliptic Curve)
- **Ed25519** (Digital signatures)

**Hashing (Data Integrity):**
- **SHA-256** (Common)
- **SHA-3** (Latest standard)
- **BLAKE2** (High performance)

**Password Hashing:**
- **Argon2** (Winner of Password Hashing Competition)
- **bcrypt** (Widely used, battle-tested)
- **scrypt** (Memory-hard)

**Key Derivation:**
- **PBKDF2** (Minimum 100,000 iterations)
- **Argon2**
- **scrypt**

---

## 🚨 Common Mistakes

1. **Using ECB Mode**
   ```java
   // DON'T: ECB mode is insecure
   Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");

   // DO: Use GCM or CBC mode
   Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
   ```

2. **Hardcoded Keys**
   ```java
   // DON'T: Hardcoded key
   String key = "1234567890123456";
   SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "AES");

   // DO: Generate secure random key
   KeyGenerator keyGen = KeyGenerator.getInstance("AES");
   keyGen.init(256, new SecureRandom());
   SecretKey key = keyGen.generateKey();
   ```

3. **Using MD5/SHA-1 for Passwords**
   ```java
   // DON'T: MD5/SHA-1 for passwords
   MessageDigest md = MessageDigest.getInstance("MD5");
   byte[] hash = md.digest(password.getBytes());

   // DO: Use bcrypt
   String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
   ```

4. **Reusing IV**
   ```java
   // DON'T: Fixed IV
   byte[] iv = new byte[16];  // All zeros!

   // DO: Random IV each time
   byte[] iv = new byte[12];
   new SecureRandom().nextBytes(iv);
   ```

---

## 💡 Best Practices Summary

1. **Strong algorithms** - AES-256, RSA-2048+, SHA-256+
2. **Authenticated encryption** - Use GCM or CCM modes
3. **Random IV** - Generate unique IV for each encryption
4. **SecureRandom** - Use for all cryptographic randomness
5. **bcrypt for passwords** - Never use fast hashes for passwords
6. **No hardcoded keys** - Use secure key management
7. **Key rotation** - Regularly rotate cryptographic keys
8. **Update libraries** - Keep crypto libraries current
9. **Never ECB mode** - Use CBC, GCM, or other secure modes
10. **Test thoroughly** - Verify crypto implementation

---

**Always use strong cryptographic algorithms approved by NIST and the security community! (AES-256, RSA-2048+, SHA-256, bcrypt)**
