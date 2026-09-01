# Use of One-Way Hash without Salt

**CWE-759: Use of a One-Way Hash without a Salt**

## Overview

Use of a One-Way Hash without a Salt is a security vulnerability that occurs when storing passwords or sensitive data using only hash functions without salt (random arbitrary values). Without salt, identical inputs always produce identical hash values, making the system vulnerable to rainbow table attacks, dictionary attacks, and identification of identical passwords. Salt adds a unique random value to each password, ensuring that identical passwords produce different hash values.

## Severity
- **CVSS v3.1 Score**: 7.4 (High)
- **Risk Level**: High
- **Impact**: Confidentiality Impact, Mass Account Compromise

## Vulnerability Impact

### Attack Scenarios

#### Scenario 1: Rainbow Table Attack
```
1. Web application stores passwords hashed with SHA-256 only
2. Attacker steals user table via SQL Injection
3. Stolen hash value: 5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8 (password)
4. Attacker compares against pre-computed rainbow tables
5. Original password "password" found in rainbow table matching the hash value
6. Result: Multiple users' passwords recovered within seconds
```

#### Scenario 2: Identical Password Identification and Targeted Attack
```
1. System hashes passwords with MD5 without salt
2. Attacker obtains a database backup file
3. Hash value analysis results:
   - user1: 5f4dcc3b5aa765d61d8327deb882cf99
   - user2: 5f4dcc3b5aa765d61d8327deb882cf99
   - user3: e10adc3949ba59abbe56e057f20f883e
4. Confirmed that user1 and user2 use the same password
5. Attacker cracks user1's password via dictionary attack: "password"
6. Immediate access to user2's account using the same password
7. Result: Cracking one password compromises multiple accounts
```

#### Scenario 3: GPU-Based High-Speed Brute Force Attack
```
1. Web service hashes passwords with SHA-1 (no salt)
2. Attacker gains admin database access via phishing
3. Extracts 1 million user hash values
4. Attacker uses GPU cluster to compute billions of hashes per second
5. Hashes all common password combinations and compares
6. All simple passwords of 8 characters or fewer cracked within hours
7. Result: Mass account compromise of users with weak passwords
```

#### Scenario 4: Dictionary Attack
```
1. Online shopping mall stores passwords with SHA-256 only
2. Attacker obtains DB dump through a web application vulnerability
3. Prepares a list of 100,000 common passwords:
   - password, 123456, qwerty, admin, letmein, etc.
4. Hashes each password with SHA-256 and compares against DB hashes
5. Discovers thousands of users are using passwords from the dictionary
6. Result: Immediate compromise of accounts using simple passwords
```

## Vulnerable Code Examples

### Java - Vulnerable Code (SHA-256 without Salt)

```java
// Vulnerable example: Using only SHA-256 hash without salt
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class VulnerablePasswordHasher {

    /**
     * Vulnerable password hashing (no salt)
     *
     * Problems:
     * 1. Identical passwords always produce identical hashes
     * 2. Vulnerable to rainbow table attacks
     * 3. Vulnerable to dictionary attacks
     * 4. Can be cracked quickly with parallel processing
     */
    public String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Vulnerability: Hashing password without salt
        byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

        // Return as Base64 encoded string
        return Base64.getEncoder().encodeToString(hashedBytes);
    }

    /**
     * Password verification
     *
     * Vulnerability: Since identical passwords always produce identical hashes,
     * attackers can determine if other accounts use the same password by examining hash values
     */
    public boolean verifyPassword(String password, String storedHash)
            throws NoSuchAlgorithmException {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(storedHash);
    }

    // Usage example
    public void registerUser(String username, String password) throws NoSuchAlgorithmException {
        String hashedPassword = hashPassword(password);

        // Vulnerability: Storing only the hash without salt
        // DB: INSERT INTO users (username, password_hash) VALUES (?, ?)
        saveToDatabase(username, hashedPassword);

        /*
         * All users with the same password "password123" will have
         * the same hash value:
         * "XohImNooBHFR0OVvjcYpJ3NgPQ1qut1iEO9yG1dVols="
         *
         * If an attacker cracks one user's password,
         * they instantly know the password of all users with the same hash!
         */
    }

    private void saveToDatabase(String username, String passwordHash) {
        // DB save logic
    }
}
```

### Java - Vulnerable Code (MD5 without Salt)

```java
// Vulnerable example: MD5 hash (no salt + weak algorithm)
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class VulnerableMD5Hasher {

    /**
     * Highly vulnerable MD5 hashing
     *
     * Problems:
     * 1. MD5 is a weak hash function with known collisions
     * 2. No salt makes it vulnerable to rainbow table attacks
     * 3. MD5 is very fast, making brute force attacks easy
     * 4. As of 2024, MD5 is prohibited for security purposes
     */
    public String hashPasswordMD5(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Vulnerability: MD5 + no salt
            byte[] messageDigest = md.digest(password.getBytes());

            // Convert to hex string
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);

            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }

            return hashtext;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * MD5 rainbow table examples:
     *
     * MD5 hashes of common passwords (publicly available online):
     * - password: 5f4dcc3b5aa765d61d8327deb882cf99
     * - 123456: e10adc3949ba59abbe56e057f20f883e
     * - admin: 21232f297a57a5a743894a0e4a801fc3
     * - qwerty: d8578edf8458ce06fbc5bb76a58c5ca4
     *
     * Attackers can enter hash values on sites like https://crackstation.net/
     * to instantly recover the original password!
     */

    // Usage example
    public void storeUserPassword(String userId, String password) {
        String hash = hashPasswordMD5(password);

        // Vulnerability: Storing MD5 hash as-is
        // If an attacker steals the hash, they can recover the password instantly using online tools
        System.out.println("Storing hash for user " + userId + ": " + hash);
    }
}
```

### Java - Vulnerable Code (Fixed Salt)

```java
// Vulnerable example: Using the same salt for all users
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class VulnerableFixedSaltHasher {

    // Vulnerability: Using the same salt for all users
    // This is nearly as vulnerable as not using salt at all
    private static final String FIXED_SALT = "MyAppSalt2024";

    /**
     * Hashing with a fixed salt (still vulnerable)
     *
     * Problems:
     * 1. All users share the same salt
     * 2. If an attacker discovers this salt, they can generate a rainbow table for it
     * 3. Identical passwords still produce identical hashes
     * 4. Source code leaks also expose the salt
     */
    public String hashPasswordWithFixedSalt(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Vulnerability: Same salt for all users
        String saltedPassword = FIXED_SALT + password;
        byte[] hashedBytes = md.digest(saltedPassword.getBytes(StandardCharsets.UTF_8));

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashedBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /*
     * Problems with a fixed salt:
     *
     * User1: password "hello123" -> SHA-256(MyAppSalt2024hello123) = abc123...
     * User2: password "hello123" -> SHA-256(MyAppSalt2024hello123) = abc123...
     *
     * Identical passwords still produce identical hashes!
     * Attackers can build a custom rainbow table using
     * "MyAppSalt2024" + common password combinations
     */
}
```

### C# - Vulnerable Code

```csharp
// Vulnerable example: SHA-256 without salt in C#
using System;
using System.Security.Cryptography;
using System.Text;

public class VulnerablePasswordHasher
{
    /// <summary>
    /// Vulnerable password hashing (no salt)
    ///
    /// Problems:
    /// - Identical passwords produce identical hashes
    /// - Vulnerable to rainbow table attacks
    /// - Vulnerable to parallel GPU attacks
    /// </summary>
    public string HashPassword(string password)
    {
        using (SHA256 sha256 = SHA256.Create())
        {
            // Vulnerability: Hashing password without salt
            byte[] bytes = sha256.ComputeHash(Encoding.UTF8.GetBytes(password));

            // Convert to hex string
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < bytes.Length; i++)
            {
                builder.Append(bytes[i].ToString("x2"));
            }

            return builder.ToString();
        }
    }

    /// <summary>
    /// Password verification
    /// </summary>
    public bool VerifyPassword(string password, string storedHash)
    {
        string hashedInput = HashPassword(password);
        return hashedInput.Equals(storedHash, StringComparison.OrdinalIgnoreCase);
    }

    // Usage example
    public void RegisterUser(string username, string password)
    {
        // Vulnerability: Storing only hash without salt
        string passwordHash = HashPassword(password);

        // DB save: INSERT INTO Users (Username, PasswordHash) VALUES (@username, @hash)
        SaveToDatabase(username, passwordHash);

        /*
         * Security problem scenario:
         * 1000 users use "password123"
         * -> 1000 identical hash values stored
         * -> Attacker cracks one and compromises all 1000 accounts!
         */
    }

    private void SaveToDatabase(string username, string passwordHash)
    {
        // DB save logic
    }
}

// Vulnerable example: Using MD5 (even more dangerous)
public class VulnerableMD5Hasher
{
    public string HashPasswordMD5(string password)
    {
        using (MD5 md5 = MD5.Create())
        {
            // Vulnerability: MD5 + no salt (extremely dangerous)
            byte[] inputBytes = Encoding.UTF8.GetBytes(password);
            byte[] hashBytes = md5.ComputeHash(inputBytes);

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < hashBytes.Length; i++)
            {
                sb.Append(hashBytes[i].ToString("x2"));
            }

            return sb.ToString();
        }
    }

    /*
     * Problems with MD5:
     * 1. Weak algorithm with known collisions
     * 2. Billions of hashes computable per second (GPU)
     * 3. Instantly crackable using online rainbow tables
     * 4. Prohibited by security standards
     */
}
```

### C - Vulnerable Code

```c
// Vulnerable example: MD5 hash without salt in C
#include <stdio.h>
#include <string.h>
#include <openssl/md5.h>
#include <openssl/sha.h>

/**
 * Vulnerable MD5 hash function (no salt)
 *
 * Problems:
 * - Salt parameter is passed as NULL
 * - Identical passwords always produce identical hashes
 * - Vulnerable to rainbow table attacks
 */
void vulnerable_md5_hash(const char* password, unsigned char* output) {
    MD5_CTX ctx;
    MD5_Init(&ctx);

    // Vulnerability: Hashing password without salt
    MD5_Update(&ctx, password, strlen(password));

    MD5_Final(output, &ctx);
}

/**
 * Vulnerable SHA-256 hash function (no salt)
 */
void vulnerable_sha256_hash(const char* password, unsigned char* output) {
    SHA256_CTX ctx;
    SHA256_Init(&ctx);

    // Vulnerability: Hashing password without salt
    SHA256_Update(&ctx, password, strlen(password));

    SHA256_Final(output, &ctx);
}

/**
 * Vulnerable password storage function
 */
void store_password_insecure(const char* username, const char* password) {
    unsigned char hash[MD5_DIGEST_LENGTH];

    // Vulnerability: MD5 + no salt
    vulnerable_md5_hash(password, hash);

    // Convert hash to hex string
    char hash_str[MD5_DIGEST_LENGTH * 2 + 1];
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
        sprintf(hash_str + (i * 2), "%02x", hash[i]);
    }
    hash_str[MD5_DIGEST_LENGTH * 2] = '\0';

    printf("User: %s, Hash: %s\n", username, hash_str);

    // Vulnerability: Storing only the unsalted hash in DB
    // INSERT INTO users (username, password_hash) VALUES (?, ?)

    /*
     * Security problem:
     * password "admin" -> MD5: 21232f297a57a5a743894a0e4a801fc3
     * This hash is publicly available online and can be instantly reversed
     */
}

/**
 * Vulnerable password verification function
 */
int verify_password_insecure(const char* password, const unsigned char* stored_hash) {
    unsigned char computed_hash[MD5_DIGEST_LENGTH];

    vulnerable_md5_hash(password, computed_hash);

    // Compare hashes
    return memcmp(computed_hash, stored_hash, MD5_DIGEST_LENGTH) == 0;
}

/**
 * Fixed salt usage (still vulnerable)
 */
void hash_with_fixed_salt(const char* password, unsigned char* output) {
    const char* FIXED_SALT = "MySalt123"; // Vulnerability: Same salt for all users

    SHA256_CTX ctx;
    SHA256_Init(&ctx);

    // Vulnerability: Using a fixed salt
    SHA256_Update(&ctx, FIXED_SALT, strlen(FIXED_SALT));
    SHA256_Update(&ctx, password, strlen(password));

    SHA256_Final(output, &ctx);

    /*
     * Problems with fixed salt:
     * - All users share the same salt
     * - A rainbow table can be built using "MySalt123" + password combinations
     * - Identical passwords still produce identical hashes
     */
}

int main() {
    // Vulnerable usage example
    store_password_insecure("admin", "password123");
    store_password_insecure("user1", "password123");
    // Both users with the same password will have identical stored hashes!

    return 0;
}
```

## Secure Code Examples

### Java - Secure Code (Salt + PBKDF2)

```java
// Secure example: Using salt and PBKDF2
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Secure password hashing class
 *
 * Security features:
 * - Generates a unique salt for each password
 * - Uses PBKDF2 algorithm (slow hash function)
 * - Increases cracking time with iteration count
 * - Stores salt and hash together
 */
public class SecurePasswordHasher {

    private static final int SALT_LENGTH = 16; // 128 bits
    private static final int HASH_LENGTH = 32; // 256 bits
    private static final int ITERATIONS = 310000; // OWASP 2023 recommendation: 310,000 or more

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Password hashing
     *
     * @param password the original password
     * @return a string combining salt and hash
     */
    public static String hashPassword(String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        // 1. Generate a unique salt for each password
        byte[] salt = generateSalt();

        // 2. Generate hash with PBKDF2
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_LENGTH);

        // 3. Combine and return salt and hash (Base64 encoded)
        return String.format("%s:%s",
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(hash)
        );
    }

    /**
     * Password verification
     *
     * @param password the input password
     * @param storedHash the stored hash (including salt)
     * @return whether the password matches
     */
    public static boolean verifyPassword(String password, String storedHash)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        // Separate salt and hash from stored hash
        String[] parts = storedHash.split(":");
        if (parts.length != 2) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] hash = Base64.getDecoder().decode(parts[1]);

        // Hash the input password with the same salt
        byte[] computedHash = pbkdf2(password.toCharArray(), salt, ITERATIONS, hash.length);

        // Compare hashes (timing attack resistant)
        return slowEquals(hash, computedHash);
    }

    /**
     * Secure random salt generation
     *
     * @return a random salt byte array
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }

    /**
     * PBKDF2 hash generation
     *
     * @param password the password
     * @param salt the salt
     * @param iterations the iteration count
     * @param keyLength the output key length
     * @return the hash byte array
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength * 8);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

        return factory.generateSecret(spec).getEncoded();
    }

    /**
     * Timing attack resistant comparison function
     *
     * @param a first byte array
     * @param b second byte array
     * @return whether the two arrays are identical
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    // Usage example
    public static void main(String[] args) {
        try {
            // Registration: Password hashing
            String password = "MySecurePassword123!";
            String hashedPassword = hashPassword(password);

            System.out.println("Original password: " + password);
            System.out.println("Hashed (with salt): " + hashedPassword);

            // DB storage: INSERT INTO users (username, password_hash) VALUES (?, ?)
            // hashedPassword format: "salt(Base64):hash(Base64)"

            // Login: Password verification
            String inputPassword = "MySecurePassword123!";
            boolean isValid = verifyPassword(inputPassword, hashedPassword);

            System.out.println("Password valid: " + isValid);

            /*
             * Output example:
             * Original password: MySecurePassword123!
             * Hashed (with salt): Xj3kL9mP2nQ5vR8tY1wZ4a==:Hj8Km2Lp5Nq9Rs3Tv7Wx1Yz4Ba...
             *
             * Even the same password produces different hashes due to different salts:
             * User1 "password123" -> 7Kq3....:9Px5....
             * User2 "password123" -> 2Mn8....:4Ry2....
             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### Java - Secure Code (Using BCrypt)

```java
// Secure example: Using BCrypt library (recommended)
import org.mindrot.jbcrypt.BCrypt;

/**
 * Secure password hashing with BCrypt
 *
 * Advantages of BCrypt:
 * - Automatic salt generation and management
 * - Slow hash function (defends against brute force)
 * - Adjustable computation cost via work factor
 * - Simple API
 *
 * Maven dependency:
 * <dependency>
 *     <groupId>org.mindrot</groupId>
 *     <artifactId>jbcrypt</artifactId>
 *     <version>0.4</version>
 * </dependency>
 */
public class BCryptPasswordHasher {

    // Work factor: 10-12 recommended (higher is slower and more secure)
    // 2^12 = 4096 rounds
    private static final int WORK_FACTOR = 12;

    /**
     * Password hashing (salt auto-generated)
     *
     * @param password the original password
     * @return BCrypt hash (includes salt)
     */
    public static String hashPassword(String password) {
        // BCrypt automatically generates salt and hashes
        return BCrypt.hashpw(password, BCrypt.gensalt(WORK_FACTOR));
    }

    /**
     * Password verification
     *
     * @param password the input password
     * @param hashedPassword the stored BCrypt hash
     * @return whether the password matches
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

    // Usage example
    public static void main(String[] args) {
        String password = "MySecurePassword123!";

        // Registration: Password hashing
        String hash1 = hashPassword(password);
        String hash2 = hashPassword(password);

        System.out.println("Password: " + password);
        System.out.println("Hash 1: " + hash1);
        System.out.println("Hash 2: " + hash2);
        System.out.println("Hashes are different: " + !hash1.equals(hash2));

        // Login: Password verification
        boolean isValid = verifyPassword(password, hash1);
        System.out.println("Password valid: " + isValid);

        /*
         * BCrypt hash example:
         * $2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW
         *
         * Format: $2a$[cost]$[22-char salt][31-char hash]
         * - $2a$: BCrypt version
         * - 12: Work factor
         * - Next 22 chars: Salt (Base64)
         * - Remaining 31 chars: Hash (Base64)
         *
         * The same password produces a different hash each time!
         */
    }
}
```

### Java - Secure Code (Using Argon2)

```java
// Secure example: Using Argon2 (latest recommended algorithm)
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

/**
 * Modern password hashing with Argon2
 *
 * Advantages of Argon2:
 * - 2015 Password Hashing Competition winner
 * - Resistant to GPU/ASIC attacks
 * - Memory-hard function (defends against parallel attacks)
 * - Argon2id: Defends against both side-channel and GPU attacks
 *
 * Maven dependency:
 * <dependency>
 *     <groupId>de.mkammerer</groupId>
 *     <artifactId>argon2-jvm</artifactId>
 *     <version>2.11</version>
 * </dependency>
 */
public class Argon2PasswordHasher {

    private static final Argon2 argon2 = Argon2Factory.create(
        Argon2Factory.Argon2Types.ARGON2id,
        32, // Salt length
        64  // Hash length
    );

    // Argon2 parameters (2023 OWASP recommendation)
    private static final int ITERATIONS = 3;      // Iteration count
    private static final int MEMORY = 65536;      // Memory (KB): 64MB
    private static final int PARALLELISM = 4;     // Parallelism degree

    /**
     * Password hashing
     *
     * @param password the original password
     * @return Argon2 hash
     */
    public static String hashPassword(String password) {
        return argon2.hash(ITERATIONS, MEMORY, PARALLELISM, password.toCharArray());
    }

    /**
     * Password verification
     *
     * @param password the input password
     * @param hash the stored Argon2 hash
     * @return whether the password matches
     */
    public static boolean verifyPassword(String password, String hash) {
        return argon2.verify(hash, password.toCharArray());
    }

    // Usage example
    public static void main(String[] args) {
        try {
            String password = "MySecurePassword123!";

            // Registration: Password hashing
            String hash = hashPassword(password);

            System.out.println("Password: " + password);
            System.out.println("Argon2 hash: " + hash);

            // Login: Password verification
            boolean isValid = verifyPassword(password, hash);
            System.out.println("Password valid: " + isValid);

            /*
             * Argon2 hash example:
             * $argon2id$v=19$m=65536,t=3,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+dWRWJTmaaJObG
             *
             * Format:
             * $argon2id$v=19$m=65536,t=3,p=4$[salt]$[hash]
             * - argon2id: Argon2 type
             * - v=19: Version
             * - m=65536: Memory (64MB)
             * - t=3: Iteration count
             * - p=4: Parallelism degree
             */

        } finally {
            // Remove Argon2 instance from memory
            argon2.wipeArray(password.toCharArray());
        }
    }
}
```

### C# - Secure Code

```csharp
// Secure example: Using Rfc2898DeriveBytes (PBKDF2) in C#
using System;
using System.Security.Cryptography;
using Microsoft.AspNetCore.Cryptography.KeyDerivation;

/// <summary>
/// Secure password hashing class
///
/// Security features:
/// - Generates a unique salt for each password
/// - Uses PBKDF2-HMAC-SHA256
/// - High iteration count increases cracking time
/// </summary>
public class SecurePasswordHasher
{
    private const int SaltSize = 128 / 8; // 128 bits
    private const int HashSize = 256 / 8; // 256 bits
    private const int Iterations = 310000; // OWASP 2023 recommendation

    /// <summary>
    /// Password hashing
    /// </summary>
    public static string HashPassword(string password)
    {
        // 1. Generate salt with secure random
        byte[] salt = GenerateSalt();

        // 2. Generate hash with PBKDF2
        byte[] hash = KeyDerivation.Pbkdf2(
            password: password,
            salt: salt,
            prf: KeyDerivationPrf.HMACSHA256,
            iterationCount: Iterations,
            numBytesRequested: HashSize
        );

        // 3. Combine and return salt and hash
        return $"{Convert.ToBase64String(salt)}:{Convert.ToBase64String(hash)}";
    }

    /// <summary>
    /// Password verification
    /// </summary>
    public static bool VerifyPassword(string password, string storedHash)
    {
        // Separate salt and hash from stored hash
        string[] parts = storedHash.Split(':');
        if (parts.Length != 2)
        {
            return false;
        }

        byte[] salt = Convert.FromBase64String(parts[0]);
        byte[] hash = Convert.FromBase64String(parts[1]);

        // Hash the input password with the same salt
        byte[] computedHash = KeyDerivation.Pbkdf2(
            password: password,
            salt: salt,
            prf: KeyDerivationPrf.HMACSHA256,
            iterationCount: Iterations,
            numBytesRequested: hash.Length
        );

        // Timing attack resistant comparison
        return CryptographicOperations.FixedTimeEquals(hash, computedHash);
    }

    /// <summary>
    /// Secure salt generation
    /// </summary>
    private static byte[] GenerateSalt()
    {
        byte[] salt = new byte[SaltSize];
        using (var rng = RandomNumberGenerator.Create())
        {
            rng.GetBytes(salt);
        }
        return salt;
    }

    // Usage example
    public static void Example()
    {
        string password = "MySecurePassword123!";

        // Registration: Password hashing
        string hashedPassword = HashPassword(password);
        Console.WriteLine($"Original: {password}");
        Console.WriteLine($"Hashed: {hashedPassword}");

        // DB storage
        // INSERT INTO Users (Username, PasswordHash) VALUES (@username, @hashedPassword)

        // Login: Password verification
        bool isValid = VerifyPassword(password, hashedPassword);
        Console.WriteLine($"Valid: {isValid}");
    }
}

/// <summary>
/// Using BCrypt.Net (recommended)
///
/// NuGet package: BCrypt.Net-Next
/// </summary>
public class BCryptPasswordHasher
{
    private const int WorkFactor = 12;

    public static string HashPassword(string password)
    {
        return BCrypt.Net.BCrypt.HashPassword(password, WorkFactor);
    }

    public static bool VerifyPassword(string password, string hash)
    {
        return BCrypt.Net.BCrypt.Verify(password, hash);
    }
}
```

### C - Secure Code

```c
// Secure example: Using salt with SHA-256 in C
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <openssl/sha.h>
#include <openssl/rand.h>
#include <openssl/evp.h>

#define SALT_SIZE 16
#define HASH_SIZE 32
#define ITERATIONS 310000 // OWASP recommendation

/**
 * Secure salt generation
 */
int generate_salt(unsigned char* salt, size_t salt_len) {
    // Use OpenSSL's secure random number generator
    if (RAND_bytes(salt, salt_len) != 1) {
        fprintf(stderr, "Error generating random salt\n");
        return -1;
    }
    return 0;
}

/**
 * Secure hash generation using PBKDF2
 *
 * @param password the password
 * @param salt the salt
 * @param salt_len salt length
 * @param iterations iteration count
 * @param output output hash buffer
 * @param output_len output hash length
 */
int pbkdf2_hash(const char* password,
                const unsigned char* salt, size_t salt_len,
                int iterations,
                unsigned char* output, size_t output_len) {

    if (PKCS5_PBKDF2_HMAC(password, strlen(password),
                          salt, salt_len,
                          iterations,
                          EVP_sha256(),
                          output_len, output) != 1) {
        fprintf(stderr, "Error computing PBKDF2 hash\n");
        return -1;
    }

    return 0;
}

/**
 * Secure password hashing and storage
 */
int hash_password_secure(const char* password,
                         unsigned char* salt_out,
                         unsigned char* hash_out) {

    // 1. Generate a unique salt
    if (generate_salt(salt_out, SALT_SIZE) != 0) {
        return -1;
    }

    // 2. Generate hash with PBKDF2
    if (pbkdf2_hash(password, salt_out, SALT_SIZE,
                    ITERATIONS, hash_out, HASH_SIZE) != 0) {
        return -1;
    }

    return 0;
}

/**
 * Password verification
 */
int verify_password_secure(const char* password,
                           const unsigned char* stored_salt,
                           const unsigned char* stored_hash) {

    unsigned char computed_hash[HASH_SIZE];

    // Recompute hash with the stored salt
    if (pbkdf2_hash(password, stored_salt, SALT_SIZE,
                    ITERATIONS, computed_hash, HASH_SIZE) != 0) {
        return -1;
    }

    // Timing attack resistant comparison
    int result = 0;
    for (int i = 0; i < HASH_SIZE; i++) {
        result |= stored_hash[i] ^ computed_hash[i];
    }

    return (result == 0) ? 1 : 0;
}

/**
 * Convert to hex string (for display)
 */
void bytes_to_hex(const unsigned char* bytes, size_t len, char* hex_out) {
    for (size_t i = 0; i < len; i++) {
        sprintf(hex_out + (i * 2), "%02x", bytes[i]);
    }
    hex_out[len * 2] = '\0';
}

int main() {
    const char* password = "MySecurePassword123!";
    unsigned char salt[SALT_SIZE];
    unsigned char hash[HASH_SIZE];

    // Registration: Password hashing
    if (hash_password_secure(password, salt, hash) == 0) {
        char salt_hex[SALT_SIZE * 2 + 1];
        char hash_hex[HASH_SIZE * 2 + 1];

        bytes_to_hex(salt, SALT_SIZE, salt_hex);
        bytes_to_hex(hash, HASH_SIZE, hash_hex);

        printf("Password: %s\n", password);
        printf("Salt (hex): %s\n", salt_hex);
        printf("Hash (hex): %s\n", hash_hex);

        // DB storage:
        // INSERT INTO users (username, salt, password_hash)
        // VALUES (?, ?, ?)
        // Both salt and hash must be stored

        // Login: Password verification
        int is_valid = verify_password_secure(password, salt, hash);
        printf("Password valid: %d\n", is_valid);

        /*
         * Security improvements:
         * 1. Unique salt generated for each user
         * 2. PBKDF2 significantly increases cracking time
         * 3. Salt stored alongside hash
         * 4. Same password produces different hashes
         */
    }

    return 0;
}

/*
 * Compilation:
 * gcc -o secure_hash secure_hash.c -lssl -lcrypto
 *
 * Required libraries:
 * - OpenSSL libssl
 * - OpenSSL libcrypto
 */
```

## Security Best Practices

### 1. Recommended Algorithms (as of 2024)

```
1st choice: Argon2id (latest recommendation)
- 2015 Password Hashing Competition winner
- Resistant to GPU/ASIC attacks
- Memory-hard function
- Parameters: m=65536, t=3, p=4

2nd choice: bcrypt
- Widely verified algorithm
- Work factor 12 or higher recommended
- Automatic salt management

3rd choice: PBKDF2-HMAC-SHA256
- NIST standard
- Iterations: 310,000 or more (OWASP 2023)
- Salt: minimum 128 bits

Prohibited: MD5, SHA-1, SHA-256 (plain hash)
```

### 2. Salt Management

```
Correct salt usage:
- Unique salt for each password
- Cryptographically secure random generator (SecureRandom, RAND_bytes)
- Minimum 128 bits (16 bytes) or more
- Stored alongside hash (can be stored in plaintext)

Incorrect salt usage:
- Same salt for all users
- Predictable salt (user ID, timestamp, etc.)
- Too short salt (< 64 bits)
- Hardcoded in source code
```

### 3. Iteration Count

```
OWASP Recommendations (2023):
- PBKDF2-HMAC-SHA256: 310,000 or more
- PBKDF2-HMAC-SHA512: 120,000 or more
- bcrypt: Work factor 12 or higher
- Argon2id: t=3, m=65536 (64MB), p=4

Performance vs Security:
- Target login time: 0.5 to 1 second
- Balance server load with security level
- Periodically increase iteration count (to keep up with hardware advances)
```

## Detection and Prevention

### Static Analysis Rules

```bash
# SpotBugs/FindBugs
- WEAK_MESSAGE_DIGEST_MD5: MD5 usage warning
- WEAK_MESSAGE_DIGEST_SHA1: SHA-1 usage warning

# SonarQube
- java:S4790: Use of weak hash algorithm
- java:S2053: Salt not used in password hashing

# PMD
- UseOfWeakHashAlgorithm

# Checkstyle (custom)
# Detect MessageDigest.getInstance("MD5|SHA-1|SHA-256") pattern
```

### Code Review Checklist

```
[ ] Using bcrypt, Argon2, or PBKDF2 for password hashing?
[ ] Generating a unique salt for each password?
[ ] Salt generated with a cryptographically secure random generator?
[ ] Salt length is 128 bits or more?
[ ] Iteration count meets OWASP recommendations?
[ ] Not using MD5, SHA-1, or plain SHA-256 hashes?
[ ] Not using a fixed salt?
[ ] Using a timing attack resistant comparison function?
```

## Testing Methods

### Unit Tests

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHashingTest {

    @Test
    void testDifferentHashesForSamePassword() throws Exception {
        String password = "testPassword123";

        // Hash the same password twice
        String hash1 = SecurePasswordHasher.hashPassword(password);
        String hash2 = SecurePasswordHasher.hashPassword(password);

        // Different salts should produce different hashes
        assertNotEquals(hash1, hash2, "Same password should produce different hashes");
    }

    @Test
    void testPasswordVerification() throws Exception {
        String password = "MyPassword123!";
        String hash = SecurePasswordHasher.hashPassword(password);

        assertTrue(SecurePasswordHasher.verifyPassword(password, hash),
            "Correct password should verify successfully");

        assertFalse(SecurePasswordHasher.verifyPassword("WrongPassword", hash),
            "Wrong password should fail verification");
    }

    @Test
    void testSaltUniqueness() throws Exception {
        String password = "samePassword";
        int iterations = 100;
        Set<String> salts = new HashSet<>();

        for (int i = 0; i < iterations; i++) {
            String hash = SecurePasswordHasher.hashPassword(password);
            String salt = hash.split(":")[0]; // Extract salt portion
            salts.add(salt);
        }

        assertEquals(iterations, salts.size(),
            "All salts should be unique");
    }

    @Test
    void testHashingPerformance() throws Exception {
        String password = "testPassword";

        long startTime = System.currentTimeMillis();
        SecurePasswordHasher.hashPassword(password);
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Hashing should take at least 100ms (too fast is insecure)
        assertTrue(duration >= 100,
            "Hashing should take at least 100ms for security");

        // But should complete within 2 seconds for usability
        assertTrue(duration <= 2000,
            "Hashing should complete within 2 seconds for usability");
    }
}
```

## Related Vulnerabilities

- **CWE-760**: Use of a One-Way Hash with a Predictable Salt
- **CWE-326**: Inadequate Encryption Strength
- **CWE-916**: Use of Password Hash With Insufficient Computational Effort
- **CWE-327**: Use of a Broken or Risky Cryptographic Algorithm

## References

### Standards and Guides
- OWASP Password Storage Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
- NIST SP 800-63B: Digital Identity Guidelines
- CWE-759: https://cwe.mitre.org/data/definitions/759.html

### Libraries
- **Java**: BCrypt (jBCrypt), Argon2 (argon2-jvm), PBKDF2 (javax.crypto)
- **C#**: BCrypt.Net-Next, AspNetCore.Identity (built-in)
- **C/C++**: libsodium, OpenSSL PKCS5_PBKDF2_HMAC

### Tools
- hashcat: Hash cracking tool (for security testing)
- John the Ripper: Password cracking tool

## Checklist

### Development Phase
- [ ] Use bcrypt, Argon2, or PBKDF2
- [ ] Generate a unique salt for each password
- [ ] Generate salts with SecureRandom
- [ ] Salt length 128 bits or more
- [ ] Set appropriate iteration count (per OWASP recommendations)
- [ ] Prohibit use of MD5, SHA-1
- [ ] Use timing attack resistant comparison function

### Testing Phase
- [ ] Verify same password produces different hashes
- [ ] Password verification test
- [ ] Hashing performance test (0.5-1 second)
- [ ] Salt uniqueness test

### Migration
- [ ] Identify existing weak hashes
- [ ] Re-hash on user's next login
- [ ] Plan gradual migration

---

**Last updated**: 2025-02-05
