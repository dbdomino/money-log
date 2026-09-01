# Hard-Coded Password (CWE-259)

**Severity**: 🔴 CRITICAL
**Category**: Security Features
**OWASP Top 10**: A02:2021 – Cryptographic Failures

---

## Overview

### Attack Description

Hard-coded passwords occur when authentication credentials, database passwords, API keys, or cryptographic keys are embedded directly in source code and used for internal authentication or encryption. Since source code is often accessible to developers, stored in version control, or can be reverse-engineered from compiled binaries, hardcoded secrets can be easily discovered by attackers, leading to the exposure of sensitive information such as administrator credentials and encrypted data.

### Impact

**Potential consequences:**
- Unauthorized system access
- Database compromise
- Privilege escalation
- API abuse
- Data breach
- Cannot change passwords without code redeployment
- Credentials exposed in source code repositories
- Reverse engineering from binaries
- Compliance violations

---

## Security Measures

### Key Principles

Passwords should be encrypted and stored in a separate file. When encrypting sensitive information, use a non-constant encryption key rather than a hard-coded one, and never store constant-form encryption keys inside source code.

**Primary Defenses:**

1. **External Configuration**
   - Store credentials in configuration files (outside source code)
   - Use environment variables
   - Property files with restricted permissions
   - Configuration management systems

2. **Secure Credential Storage**
   - Use secret management systems (Vault, AWS Secrets Manager, Azure Key Vault)
   - Encrypt configuration files
   - Restrict file permissions (600/400)
   - Never commit secrets to version control

3. **Encryption Key Management**
   - Store encryption keys in HSM or key vault
   - Use key derivation functions (PBKDF2, scrypt)
   - Rotate keys periodically
   - Separate key storage from encrypted data

4. **Access Control**
   - Limit who can access configuration files
   - Use different credentials for different environments (dev/staging/prod)
   - Implement credential rotation policies
   - Audit access to secrets

5. **Code Review & Scanning**
   - Scan code for hardcoded secrets
   - Use pre-commit hooks to prevent accidental commits
   - Regular security audits
   - Remove secrets from version control history

---

## Code Examples

### Attack Scenario

In the following example, the password of an authenticated user is stored in plain text in the database.

**Attack:**
```java
// Source code leaked or reverse-engineered
// Attacker finds hardcoded credentials:
private static final String PASS = "SCOTT"; // DB PW:

// Attacker can now access database directly
```

---

### ❌ Vulnerable Code

#### Java - Hardcoded Database Password

```java
public class MemberDAO {
    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String URL = "jdbc:oracle:thin:@192.168.0.3:1521:ORCL";
    private static final String USER = "SCOTT"; // DB ID:
    // The DB password is stored in plain text in the source code.
    private static final String PASS = "SCOTT"; // DB PW:
    ......

    public Connection getConn() {
        Connection con = null;
        try {
            Class.forName(DRIVER);
            con = DriverManager.getConnection(URL, USER, PASS);
            ......
        }
    }
}
```

**Problems:**
1. **Hardcoded password** - Database password in source code
2. Plain text storage in code
3. Cannot change password without recompiling
4. Exposed in version control
5. Visible in decompiled bytecode
6. Same password likely used across environments

---

#### C# - Hardcoded Credentials

```csharp
string UserName = "username";
string Password = "password";
// Creates NetworkCredential using a plain text password
NetworkCredential myCred = new NetworkCredential(UserName, Password);
```

**Problems:**
1. Hardcoded username and password
2. Plain text in source code
3. Cannot rotate credentials
4. Exposed in compiled assembly

---

#### C - Hardcoded Password in Database Connection

```c
int dbaccess(char *server, char *user) {
    SQLHENV henv;
    SQLHDBC hdbc;
    char *password = "password";
    SQLAllocHandle(SQL_HANDLE_ENV, SQL_NULL_HANDLE, &henv);
    SQLAllocHandle(SQL_HANDLE_DBC, henv, &hdbc);
    SQLConnect(hdbc,
              (SQLCHAR*) server,
              (SQLSMALLINT) strlen(server),
              (SQLCHAR*) user,
              (SQLSMALLINT) strlen(user),
              // Connects directly without encrypting the password.
              (SQLCHAR*) password,
              (SQLSMALLINT) strlen(password) );
    return 0;
}
```

**Problems:**
1. Hardcoded password variable
2. Cannot change without recompilation
3. Visible in binary strings
4. No encryption

---

#### Java - Hardcoded Encryption Key

```java
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
......

public String encriptString(String usr) {
    // Using an encryption key inside source code is insecure.
    String key = "22df3023sf-2asn!@#/^a$";
    if (key != null) {
        byte[] bToEncrypt = usr.getBytes("UTF-8");
        SecretKeySpec sKeySpec = new SecretKeySpec(key.getBytes(), "AES");
        ......
    }
}
```

**Problems:**
1. **Hardcoded encryption key** - Key embedded in code
2. Same key used for all encryptions
3. Cannot rotate key without code change
4. Key visible in decompiled code
5. Compromises all encrypted data

---

### ✅ Secure Code

#### Java - Password from Configuration File

```java
public class MemberDAO {
    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String URL = "jdbc:oracle:thin:@192.168.0.3:1521:ORCL";
    private static final String USER = "SCOTT"; // DB ID
    ......

    public Connection getConn() {
        Connection con = null;
        try {
            Class.forName(DRIVER);
            // Reads the encrypted password from a properties file and decrypts it before use.
            String PASS = props.getProperty("EncryptedPswd");
            byte[] decryptedPswd = cipher.doFinal(PASS.getBytes());
            PASS = new String(decryptedPswd);
            con = DriverManager.getConnection(URL, USER, PASS);
            ......
        }
    }
}
```

**Security Features:**
1. Password stored in external properties file
2. Password encrypted in configuration
3. Decrypted at runtime
4. Can update password without code change

**Configuration File (db.properties):**
```properties
# Encrypted password (not plain text)
EncryptedPswd=U2FsdGVkX1+XGJ3...encrypted...base64...
```

---

#### ✅ Better Practice - Environment Variables

```java
public class SecureDatabaseConnection {

    /**
     * Get database connection using environment variables
     */
    public Connection getConnection() throws SQLException {
        // Read credentials from environment variables
        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        // Validate environment variables are set
        if (dbUrl == null || dbUser == null || dbPassword == null) {
            throw new IllegalStateException(
                "Database credentials not configured in environment variables"
            );
        }

        // Create connection
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }
}
```

**Environment Variables (.env file - NOT in version control):**
```bash
# .env (add to .gitignore!)
DB_URL=jdbc:postgresql://localhost:5432/mydb
DB_USER=dbuser
DB_PASSWORD=SecureP@ssw0rd!2024
```

**Security Features:**
1. No hardcoded credentials
2. Environment-specific configuration
3. Not stored in source code or version control
4. Easy to rotate credentials
5. Different passwords per environment

---

#### C# - SecureString for Passwords

```csharp
string UserName = "username";
string Password = "password";
SecureString SecurelyStoredPassword = new SecureString();

foreach (char c in Password)
{
    SecurelyStoredPassword.AppendChar(c);
}

// Creates NetworkCredential using an encrypted password
NetworkCredential secure_myCred = new NetworkCredential(UserName,
    SecurelyStoredPassword);
```

**Security Features:**
1. `SecureString` encrypts password in memory
2. Not stored as plain text string
3. Auto-cleanup when garbage collected
4. Prevents memory dumps from revealing password

---

#### C - Password from Environment Variable

```c
int dbaccess(char *server, char *user, char *passwd) {
    SQLHENV henv;
    SQLHDBC hdbc;
    char *key;
    // Sets encryption mode to AES-CBC.
    HCkCrypt2 crypt = CkCrypt2_putCryptAlgorithm(crypt,"aes");
    CkCrypt2_putCipherMode(crypt,"cbc");
    // Loads the encryption key from an external source.
    key = getenv("encrypt_key");
    CkCrypt2_SetEncodedKey(crypt,key,"hex");
    fp = fopen("config", "r");
    fgets(user, sizeof(user), fp);
    // Reads the password from a file.
    fgets(passwd, sizeof(passwd), fp);
    fclose(fp);
    // Performs password encryption.
    encPasswd = CkCrypt2_encryptStringENC(crypt, password);
    SQLAllocHandle(SQL_HANDLE_ENV, SQL_NULL_HANDLE, &henv);
    SQLAllocHandle(SQL_HANDLE_DBC, henv, &hdbc);
    SQLConnect(hdbc,
              (SQLCHAR*) server,
              (SQLSMALLINT) strlen(server),
              (SQLCHAR*) user,
              (SQLSMALLINT) strlen(user),
              // Uses the encrypted password.
              (SQLCHAR*) encPasswd,
              (SQLSMALLINT) strlen(verifiedPwd) );
    return 0;
}
```

**Security Features:**
1. Encryption key from environment variable
2. Password read from external file
3. Password encrypted before use
4. AES-CBC encryption mode
5. No hardcoded secrets

---

#### ✅ Best Practice - Comprehensive Secret Management

```java
import java.io.*;
import java.util.Properties;
import javax.crypto.*;

/**
 * Secure configuration and secret management
 */
public class SecureConfigurationManager {

    private static final String CONFIG_FILE = "config/app.properties";
    private final Properties properties;
    private final Cipher cipher;

    public SecureConfigurationManager() throws Exception {
        this.properties = loadProperties();
        this.cipher = initializeCipher();
    }

    /**
     * Load configuration from external file
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();

        // Load from external configuration file (not in source code)
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
        }

        return props;
    }

    /**
     * Initialize cipher with key from secure storage
     */
    private Cipher initializeCipher() throws Exception {
        // Get encryption key from environment variable or key vault
        String keyBase64 = System.getenv("ENCRYPTION_KEY");

        if (keyBase64 == null) {
            throw new IllegalStateException(
                "ENCRYPTION_KEY not set in environment"
            );
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        SecretKey key = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        return cipher;
    }

    /**
     * Get decrypted password from configuration
     */
    public String getDecryptedPassword(String propertyName) throws Exception {
        // Get encrypted password from properties
        String encryptedBase64 = properties.getProperty(propertyName);

        if (encryptedBase64 == null) {
            throw new IllegalArgumentException(
                "Property not found: " + propertyName
            );
        }

        // Decrypt password
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV (first 12 bytes for GCM)
        byte[] iv = new byte[12];
        System.arraycopy(encrypted, 0, iv, 0, 12);

        // Extract ciphertext
        byte[] ciphertext = new byte[encrypted.length - 12];
        System.arraycopy(encrypted, 12, ciphertext, 0, ciphertext.length);

        // Decrypt
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, getEncryptionKey(), gcmSpec);
        byte[] decrypted = cipher.doFinal(ciphertext);

        return new String(decrypted, "UTF-8");
    }

    /**
     * Get database connection with secure credentials
     */
    public Connection getDatabaseConnection() throws Exception {
        String dbUrl = properties.getProperty("db.url");
        String dbUser = properties.getProperty("db.user");
        String dbPassword = getDecryptedPassword("db.password.encrypted");

        Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);

        // Clear password from memory
        dbPassword = null;

        return conn;
    }

    /**
     * Get API key from secure storage
     */
    public String getApiKey(String serviceName) throws Exception {
        // Try environment variable first
        String envVarName = serviceName.toUpperCase() + "_API_KEY";
        String apiKey = System.getenv(envVarName);

        if (apiKey != null) {
            return apiKey;
        }

        // Fall back to encrypted configuration
        String propertyName = serviceName + ".api.key.encrypted";
        return getDecryptedPassword(propertyName);
    }

    private SecretKey getEncryptionKey() throws Exception {
        String keyBase64 = System.getenv("ENCRYPTION_KEY");
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
```

**Configuration File (app.properties - encrypted values):**
```properties
# Database configuration
db.url=jdbc:postgresql://localhost:5432/mydb
db.user=app_user
# Encrypted password (AES-256-GCM)
db.password.encrypted=AQIDBA...base64_encrypted_value...

# API keys (encrypted)
stripe.api.key.encrypted=AQIDBA...base64_encrypted_value...
aws.api.key.encrypted=AQIDBA...base64_encrypted_value...
```

**Environment Variables:**
```bash
# Encryption key for config file (32 bytes base64-encoded for AES-256)
export ENCRYPTION_KEY="a3VuZ3N1bmt3b25iaW5AZXhhbXBsZS5jb20="

# Or use direct environment variables
export STRIPE_API_KEY="sk_live_..."
export AWS_API_KEY="AKIA..."
```

**Security Features:**
1. No hardcoded passwords or keys
2. Credentials in external configuration file
3. Passwords encrypted in configuration
4. Encryption key from environment variable
5. Support for direct environment variables
6. AES-256-GCM authenticated encryption
7. Password cleared from memory after use
8. Configuration file outside source code
9. Can rotate credentials without code changes

---

#### ✅ Cloud Secret Management (AWS Secrets Manager Example)

```java
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.google.gson.Gson;

/**
 * Using AWS Secrets Manager for credential management
 */
public class AWSSecretManager {

    private final SecretsManagerClient secretsClient;
    private final Gson gson;

    public AWSSecretManager() {
        this.secretsClient = SecretsManagerClient.builder().build();
        this.gson = new Gson();
    }

    /**
     * Get database credentials from AWS Secrets Manager
     */
    public DatabaseCredentials getDatabaseCredentials(String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(secretName)
            .build();

        GetSecretValueResponse response = secretsClient.getSecretValue(request);
        String secretString = response.secretString();

        // Parse JSON secret
        return gson.fromJson(secretString, DatabaseCredentials.class);
    }

    /**
     * Get database connection using AWS Secrets Manager
     */
    public Connection getSecureConnection(String secretName) throws SQLException {
        DatabaseCredentials creds = getDatabaseCredentials(secretName);

        return DriverManager.getConnection(
            creds.getHost(),
            creds.getUsername(),
            creds.getPassword()
        );
    }
}

class DatabaseCredentials {
    private String host;
    private String username;
    private String password;
    private String database;

    // Getters
    public String getHost() { return host; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
}
```

**AWS Secrets Manager Secret (JSON):**
```json
{
  "host": "mydb.us-east-1.rds.amazonaws.com",
  "username": "admin",
  "password": "SecureP@ssw0rd!2024",
  "database": "production"
}
```

**Security Features:**
1. Credentials stored in AWS Secrets Manager
2. Automatic encryption at rest
3. IAM-based access control
4. Automatic credential rotation
5. Audit logging
6. No credentials in code or config files

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-259: Use of Hard-coded Password**
   MITRE, http://cwe.mitre.org/data/definitions/259.html

② **CWE-321: Use of Hard-coded Cryptographic Key**
   MITRE, http://cwe.mitre.org/data/definitions/321.html

③ **CWE-798: Use of Hard-coded Credentials**
   MITRE, https://cwe.mitre.org/data/definitions/798.html

### CERT Secure Coding

④ **MSC03-J: Never hard code sensitive information**
   CERT, http://www.securecoding.cert.org/confluence/display/java/MSC03-J

⑤ **MSC18-C: Be careful while handling sensitive data**
   CERT, https://wiki.sei.cmu.edu/confluence/display/c/MSC18-C

### OWASP

⑥ **Password Management: Hardcoded Password**
   OWASP, https://www.owasp.org/index.php/Password_Management:_Hardcoded_Password

⑦ **Use of hard-coded password**
   OWASP, https://www.owasp.org/index.php/Use_of_hard-coded_password

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find hardcoded passwords
grep -r "password.*=.*\"" --include="*.java" . | grep -v "getProperty\|getenv"
grep -r "PASSWORD.*=.*\"" --include="*.java" .

# Find hardcoded database credentials
grep -r "PASS.*=.*\"" --include="*.java" .
grep -r "jdbc.*password" --include="*.java" .

# Find hardcoded API keys
grep -r "api.*key.*=.*\"" --include="*.java" .
grep -r "API_KEY.*=.*\"" --include="*.java" .

# Find hardcoded encryption keys
grep -r "SecretKeySpec.*\"" --include="*.java" .
grep -r "key.*=.*\"[A-Za-z0-9]{16,}\"" --include="*.java" .

# Scan for secrets in git history
git log -p | grep -i "password\|api.key\|secret"

# Use tools
truffleHog --regex --entropy=True .
git-secrets --scan
```

---

## ✅ Security Checklist

- [ ] No hardcoded passwords in source code
- [ ] No hardcoded database credentials
- [ ] No hardcoded API keys
- [ ] No hardcoded encryption keys
- [ ] Credentials stored in external configuration
- [ ] Configuration files encrypted or access-restricted
- [ ] Environment variables used for secrets
- [ ] Secret management system implemented (Vault, AWS Secrets Manager)
- [ ] Different credentials per environment (dev/staging/prod)
- [ ] Credentials rotated periodically
- [ ] `.env` files in `.gitignore`
- [ ] Git history scanned for leaked secrets
- [ ] Pre-commit hooks prevent credential commits
- [ ] Security scanning tools integrated (TruffleHog, git-secrets)

---

## 🎯 Secret Management Solutions

### 1. Environment Variables

**Pros:**
- Simple to implement
- Supported by all platforms
- 12-factor app compliant

**Cons:**
- Visible in process list
- No versioning or audit trail
- Difficult to rotate

**Best For:** Development, simple applications

---

### 2. Configuration Files (Encrypted)

**Pros:**
- Version control friendly (encrypted values)
- Easy to manage multiple secrets
- Application-specific

**Cons:**
- Need to manage encryption keys
- Still need secure key storage

**Best For:** Small to medium applications

---

### 3. Secret Management Systems

**Options:**
- **HashiCorp Vault** - Enterprise secret management
- **AWS Secrets Manager** - AWS native solution
- **Azure Key Vault** - Azure native solution
- **Google Secret Manager** - GCP native solution

**Pros:**
- Centralized secret management
- Automatic rotation
- Audit logging
- Fine-grained access control
- Encryption at rest and in transit

**Cons:**
- Additional infrastructure
- Learning curve
- Cost

**Best For:** Production applications, enterprises

---

## 🚨 Common Mistakes

1. **Hardcoded in Constants**
   ```java
   // DON'T: Still hardcoded even as constant
   private static final String DB_PASSWORD = "secret123";

   // DO: Load from external source
   private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");
   ```

2. **Committed to Version Control**
   ```bash
   # DON'T: Commit .env file
   git add .env
   git commit -m "Add configuration"

   # DO: Add to .gitignore
   echo ".env" >> .gitignore
   echo "application-prod.properties" >> .gitignore
   ```

3. **Same Credentials Everywhere**
   ```java
   // DON'T: Same password for all environments
   db.password=admin123  // Used in dev AND prod

   // DO: Different passwords per environment
   # dev: db.password=dev_password
   # prod: db.password=complex_prod_password_2024!
   ```

4. **Plain Text in Config**
   ```properties
   # DON'T: Plain text password
   db.password=MyPassword123

   # DO: Encrypted password
   db.password.encrypted=AQICAHh...encrypted_base64...
   ```

---

## 💡 Best Practices Summary

1. **Never hardcode** - No passwords/keys in source code
2. **External storage** - Use config files, environment variables, or vaults
3. **Encrypt at rest** - Encrypt sensitive values in configuration
4. **Different per environment** - Dev/staging/prod use different credentials
5. **Rotate regularly** - Change passwords and keys periodically
6. **Use secret managers** - Vault, AWS Secrets Manager for production
7. **Restrict access** - Limit who can access secrets
8. **Scan code** - Use tools to detect accidental commits
9. **Clean git history** - Remove any leaked secrets from history
10. **Audit access** - Log who accesses secrets and when

---

**Never hard-code passwords, API keys, or encryption keys in source code!**
