# Information Exposure Through Comments

**CWE-615: Information Exposure Through Comments**

## Overview

Information Exposure Through Comments is a security vulnerability that occurs when developers include sensitive information such as database credentials, passwords, API keys, internal IP addresses, and system architecture details in source code comments. This information can be exposed to attackers through source code disclosure, analysis of compiled binaries, client-side code (JavaScript, HTML), and can be exploited as critical information for system compromise.

## Severity
- **CVSS v3.1 Score**: 7.5 (High)
- **Risk Level**: High
- **Impact**: Confidentiality Impact, System Access Information Disclosure

## Vulnerability Impact

### Attack Scenarios

#### Scenario 1: API Key Exposure Through JavaScript Comments
```
1. Developer leaves a test API key as a comment in a front-end JavaScript file
2. Comment removal is missed during web application deployment
3. Attacker examines JavaScript file using browser developer tools
4. Production API key discovered in comments
5. Attacker makes unauthorized service calls using the API key
6. Result: API abuse, billing overcharges, data leakage
```

#### Scenario 2: Database Credential Exposure Through Source Code Repository
```
1. Developer writes actual database credentials as comments in database connection code
2. The code is committed to a public Git repository
3. Attacker searches for "password" keyword on GitHub, GitLab, etc.
4. Database connection info (host, port, account, password) found in comments
5. Attacker directly accesses the database and exfiltrates all data
6. Result: Large-scale personal data breach, regulatory violations
```

#### Scenario 3: Comment Information Extraction From Compiled Binaries
```
1. Developer includes administrator account information as comments in Java code
2. Application JAR file is deployed
3. Attacker decompiles the JAR file
4. Administrator account information from comments found in decompiled code
5. Attacker gains system access with administrator privileges
6. Result: Full system compromise
```

#### Scenario 4: Internal System Architecture Exposure
```
1. Developer documents internal network structure, IP ranges, and server list in comments
2. The information is included in web server configuration files or publicly accessible scripts
3. Attacker accesses configuration file during vulnerability scanning
4. Internal system architecture information obtained from comments
5. Attacker performs targeted precision attacks against internal systems
6. Result: Internal system penetration, lateral movement attacks
```

## Vulnerable Code Examples

### Java - Vulnerable Code (Database Information Exposure)

```java
// Vulnerable example: DB connection info included in comments
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class VulnerableDatabaseConnection {

    // Vulnerability: Actual DB connection info included in comments
    // DB Connection Info
    // Host: db.production.company.com
    // Port: 3306
    // Database: userdb
    // Account: root
    // Password: P@ssw0rd!2024#Admin

    public Connection connectToDatabase() {
        Connection conn = null;

        try {
            // Production DB connection
            // jdbc:mysql://db.production.company.com:3306/userdb
            // Account: root / P@ssw0rd!2024#Admin
            String url = getDbUrl();
            String username = getDbUsername();
            String password = getDbPassword();

            conn = DriverManager.getConnection(url, username, password);

        } catch (SQLException e) {
            e.printStackTrace();
            // TODO: Change to logging later (notify admin@company.com)
        }

        return conn;
    }

    // Vulnerability: Hardcoded info and comments
    private String getDbUrl() {
        // Dev server: jdbc:mysql://dev-db:3306/testdb
        // Production server: jdbc:mysql://db.production.company.com:3306/userdb
        return "jdbc:mysql://localhost:3306/mydb";
    }

    private String getDbUsername() {
        // Using admin account (approved by security team - 2024.01.15)
        return "dbuser";
    }

    private String getDbPassword() {
        // Temporary password: temp123 (needs to be changed!)
        // Actual password: P@ssw0rd!2024#Admin
        return "dbpass";
    }
}
```

### Java - Vulnerable Code (API Key and Secret Exposure)

```java
// Vulnerable example: API keys and secret info exposed in comments
public class VulnerableApiClient {

    // Vulnerability: API keys included in comments
    // AWS Access Key: AKIAIOSFODNN7EXAMPLE
    // AWS Secret Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
    // Region: ap-northeast-2

    // Vulnerability: External service API keys
    /*
     * Google API Key List:
     * - Development: AIzaSyDev123Example
     * - Staging: AIzaSyStg456Example
     * - Production: AIzaSyProd789Example (in use!)
     */

    private static final String API_ENDPOINT = "https://api.example.com";

    public void callExternalApi() {
        // Stripe API Secret Key: sk_live_<REDACTED_EXAMPLE_KEY>
        String apiKey = getApiKey();

        // PayPal Client ID: AXYz1234567890abcdefGHIJKLMNOP
        // PayPal Secret: EFGHijklmnop9876543210ZYXabc
        String clientId = getClientId();

        // Service call
        makeApiCall(apiKey);
    }

    // Vulnerability: JWT secret key in comments
    private String getJwtSecret() {
        // JWT Signing Key: my-super-secret-jwt-signing-key-2024
        // Expiration: 3600 seconds (1 hour)
        return System.getenv("JWT_SECRET");
    }

    // Vulnerability: OAuth info in comments
    /*
     * OAuth 2.0 Configuration:
     * Client ID: 1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com
     * Client Secret: GOCSPX-1234567890abcdefghijklmnop
     * Redirect URI: https://app.company.com/oauth/callback
     */
    private void configureOAuth() {
        // Implementation
    }

    private String getApiKey() { return ""; }
    private String getClientId() { return ""; }
    private void makeApiCall(String key) { }
}
```

### Java - Vulnerable Code (System Architecture Information Exposure)

```java
// Vulnerable example: System architecture info exposed in comments
public class VulnerableSystemConfig {

    /*
     * Internal System Architecture:
     *
     * [DMZ Zone]
     * - Web Servers: 10.1.1.10-15 (nginx)
     * - API Gateway: 10.1.1.20 (Kong)
     *
     * [Application Zone]
     * - App Servers: 10.2.1.10-30 (Tomcat 9.0)
     * - Redis: 10.2.1.40 (Cache Server)
     *
     * [DB Zone]
     * - Master DB: 10.3.1.10 (MySQL 8.0)
     * - Slave DB: 10.3.1.11-12 (Read-Only)
     * - MongoDB: 10.3.1.20 (Document DB)
     *
     * [Management Zone]
     * - Jenkins: 10.4.1.10 (CI/CD)
     * - Monitoring: 10.4.1.20 (Grafana)
     * - Vault: 10.4.1.30 (Secrets Management) - Access Forbidden!
     */

    // Vulnerability: Administrator account information
    /*
     * System Administrator Accounts:
     * - root / Adm!n2024$Pass
     * - sysadmin / Sys@dmin!234
     * - deploy / D3pl0y#2024
     *
     * DB Administrators:
     * - dbadmin / DBp@ss2024
     */

    // Vulnerability: Firewall policy information
    // Firewall Allowed Ports:
    // - 8080 (Web App)
    // - 9090 (Admin Console) - No IP restriction (security issue!)
    // - 3306 (MySQL) - External access currently allowed
    // - 27017 (MongoDB) - No authentication

    private static final String INTERNAL_API = "http://10.2.1.25:8080";

    // Vulnerability: Security vulnerability notes
    /*
     * TODO: Security Issues (URGENT!)
     * 1. Admin default password has not been changed
     * 2. /admin path authentication bypass possible (CVE-2023-XXXXX)
     * 3. No file upload validation - malicious code upload possible
     * 4. SQL Injection vulnerability (UserService.java line 234)
     */
}
```

### C# - Vulnerable Code

```csharp
// Vulnerable example: Information exposure through comments in C#
using System;
using System.Data.SqlClient;

public class VulnerableDataAccess
{
    // Vulnerability: Database connection info in comments
    /*
     * SQL Server Connection Info:
     * Server: sql.production.company.com
     * Database: CustomerDB
     * Account: sa
     * Password: P@ssw0rd123!
     *
     * Connection String:
     * Server=sql.production.company.com;Database=CustomerDB;User Id=sa;Password=P@ssw0rd123!;
     */

    private string connectionString;

    public VulnerableDataAccess()
    {
        // Dev server: Server=localhost;Database=TestDB;Integrated Security=true;
        // Production server: Server=sql.production.company.com;Database=CustomerDB;User Id=sa;Password=P@ssw0rd123!;
        connectionString = GetConnectionString();
    }

    // Vulnerability: Azure Storage key in comments
    /*
     * Azure Storage Account:
     * Account Name: companystorage
     * Access Key: Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==
     * Connection String: DefaultEndpointsProtocol=https;AccountName=companystorage;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;EndpointSuffix=core.windows.net
     */
    private string azureStorageKey = "stored-securely";

    // Vulnerability: Encryption key in comments
    private byte[] GetEncryptionKey()
    {
        // AES-256 Encryption Key (Base64):
        // Key: 3s6v9y$B&E)H@McQfTjWnZr4u7x!A%D*
        // IV: F-JaNdRgUkXp2s5v
        return Convert.FromBase64String(Environment.GetEnvironmentVariable("ENCRYPTION_KEY"));
    }

    // Vulnerability: External service credentials
    /*
     * SendGrid API:
     * API Key: SG.1234567890abcdefghijklmnopqrstuvwxyz.1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOP
     *
     * Twilio:
     * Account SID: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     * Auth Token: your_auth_token_here
     */

    private string GetConnectionString() => "";
}
```

### JavaScript - Vulnerable Code (Client-Side)

```javascript
// Vulnerable example: API key and configuration info exposure in JavaScript

// Vulnerability: Firebase configuration info
/*
const firebaseConfig = {
  apiKey: "AIzaSyDZXXXXXXXXXXXXXXXXXXXXXXXXXXX",
  authDomain: "myapp-12345.firebaseapp.com",
  projectId: "myapp-12345",
  storageBucket: "myapp-12345.appspot.com",
  messagingSenderId: "123456789012",
  appId: "1:123456789012:web:abcdef1234567890",
  measurementId: "G-XXXXXXXXXX"
};
*/

// Vulnerability: Google Maps API key
// Production API Key: AIzaSyBprod1234567890abcdefghijklmnop
// Development API Key: AIzaSyDev9876543210zyxwvutsrqponmlkjih
const GOOGLE_MAPS_API_KEY = 'AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXX';

class VulnerableApiClient {
    constructor() {
        // Vulnerability: REST API endpoints and credentials
        /*
         * API Endpoints:
         * Development: https://dev-api.company.com
         * Staging: https://staging-api.company.com
         * Production: https://api.company.com
         *
         * API Token: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkFkbWluIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
         */
        this.apiUrl = 'https://api.company.com';
        this.apiToken = this.getApiToken();
    }

    // Vulnerability: AWS credentials
    /*
     * AWS Credentials (temporary - until end of 2024):
     * Access Key ID: AKIAIOSFODNN7EXAMPLE
     * Secret Access Key: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
     * Region: us-west-2
     *
     * S3 Bucket: company-private-data
     * CloudFront Distribution: d111111abcdef8.cloudfront.net
     */

    // Vulnerability: OAuth Client Secret (must NEVER be exposed to the client!)
    // Client ID: 1234567890-abcdefghijklmnopqrstuvwxyz.apps.googleusercontent.com
    // Client Secret: GOCSPX-AbCdEfGhIjKlMnOpQrStUvWxYz (this value must only be used on the server!)
    configureOAuth() {
        return {
            clientId: '1234567890-abc.apps.googleusercontent.com',
            // Client Secret is managed on the server
            redirectUri: 'https://app.company.com/callback'
        };
    }

    getApiToken() {
        // Actual implementation
        return '';
    }
}

// Vulnerability: Default admin account information
/*
 * Default Admin Accounts (for initial setup):
 * Username: admin
 * Password: admin123
 *
 * Super Admin:
 * Username: superadmin
 * Password: P@ssw0rd!2024
 */
```

### C - Vulnerable Code

```c
// Vulnerable example: Information exposure through comments in C
#include <stdio.h>
#include <string.h>
#include <mysql/mysql.h>

/*
 * Vulnerability: Database connection info
 *
 * MySQL Server Info:
 * Host: 192.168.1.100
 * Port: 3306
 * Database: production_db
 * User: root
 * Password: MyS3cr3tP@ss2024!
 */

#define DB_HOST "localhost"
#define DB_USER "user"
#define DB_PASS "pass"
#define DB_NAME "database"

// Vulnerability: FTP server info
/*
 * FTP Backup Server:
 * Address: ftp.backup.company.com
 * Port: 21
 * Account: ftpbackup
 * Password: Backup#2024$Pass
 * Backup Path: /backup/daily/
 */

// Vulnerability: Encryption key
/*
 * AES Encryption Settings:
 * Key: 0x2b7e151628aed2a6abf7158809cf4f3c
 * IV: 0x000102030405060708090a0b0c0d0e0f
 */
static const unsigned char aes_key[] = {
    0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae, 0xd2, 0xa6,
    0xab, 0xf7, 0x15, 0x88, 0x09, 0xcf, 0x4f, 0x3c
};

// Vulnerability: System management info
/*
 * System Management Accounts:
 * - Linux: root / Toor!2024#Admin
 * - Windows: Administrator / W!nAdm1n2024
 *
 * SSH Key Location: /root/.ssh/id_rsa
 * Key Passphrase: ssh_key_passphrase_2024
 */

// Vulnerability: License key
/*
 * Software Licenses:
 * Product Key: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX
 * Activation Code: 1234-5678-9ABC-DEF0
 * Expiration: 2024-12-31
 */

MYSQL* connect_to_database() {
    MYSQL *conn = mysql_init(NULL);

    // Actual production DB connection
    // mysql -h 192.168.1.100 -u root -p'MyS3cr3tP@ss2024!'
    if (!mysql_real_connect(conn, DB_HOST, DB_USER, DB_PASS, DB_NAME, 0, NULL, 0)) {
        fprintf(stderr, "MySQL connection error: %s\n", mysql_error(conn));
        return NULL;
    }

    return conn;
}

// Vulnerability: Hardcoded admin verification
int is_admin_user(const char* username, const char* password) {
    // Emergency access master account
    // ID: master_admin
    // PW: M@ster2024!Emergency
    if (strcmp(username, "admin") == 0 && strcmp(password, "admin") == 0) {
        return 1; // Temporary - security issue!
    }
    return 0;
}

int main() {
    // Vulnerability: LDAP server info
    /*
     * LDAP Configuration:
     * Server: ldap://ldap.company.com:389
     * Base DN: dc=company,dc=com
     * Bind DN: cn=admin,dc=company,dc=com
     * Bind Password: Ldap@dm1nP@ss
     */

    MYSQL *conn = connect_to_database();
    if (conn) {
        printf("Database connected\n");
        mysql_close(conn);
    }

    return 0;
}
```

## Secure Code Examples

### Java - Secure Code

```java
// Secure example: Using external configuration files
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database connection management class
 *
 * Configuration is loaded from external property files.
 * Sensitive information is not included in comments.
 */
public class SecureDatabaseConnection {

    private Properties dbProperties;

    public SecureDatabaseConnection() {
        loadConfiguration();
    }

    /**
     * Load DB configuration from external configuration file.
     * Configuration file is not included in version control (.gitignore).
     */
    private void loadConfiguration() {
        dbProperties = new Properties();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {

            if (input == null) {
                throw new RuntimeException("Configuration file not found");
            }

            dbProperties.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load database configuration", e);
        }
    }

    /**
     * Create database connection
     *
     * @return database connection object
     * @throws SQLException on connection failure
     */
    public Connection connectToDatabase() throws SQLException {
        String url = dbProperties.getProperty("db.url");
        String username = dbProperties.getProperty("db.username");
        String password = dbProperties.getProperty("db.password");

        return DriverManager.getConnection(url, username, password);
    }
}

/*
 * database.properties file example (external or encrypted configuration):
 *
 * db.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
 * db.username=${DB_USERNAME}
 * db.password=${DB_PASSWORD}
 *
 * Managed via environment variables:
 * export DB_HOST=localhost
 * export DB_PORT=3306
 * export DB_NAME=mydb
 * export DB_USERNAME=dbuser
 * export DB_PASSWORD=secure_password
 */
```

### Java - Secure Code (Using Environment Variables)

```java
// Secure example: Using environment variables and secret management systems
import java.util.Optional;

/**
 * Secure API Client
 *
 * All sensitive information is loaded from environment variables or secret management systems.
 * Comments only describe configuration methods, not actual values.
 */
public class SecureApiClient {

    private final String apiEndpoint;
    private final String apiKey;

    public SecureApiClient() {
        // Load API configuration from environment variables
        // Setup: export API_ENDPOINT=https://api.example.com
        this.apiEndpoint = getRequiredEnv("API_ENDPOINT");

        // API key loaded from environment variable or secret manager
        // Setup: export API_KEY=your_api_key_here
        this.apiKey = getRequiredEnv("API_KEY");
    }

    /**
     * Get required environment variable
     *
     * @param name environment variable name
     * @return environment variable value
     * @throws IllegalStateException if the environment variable is not set
     */
    private String getRequiredEnv(String name) {
        return Optional.ofNullable(System.getenv(name))
            .orElseThrow(() -> new IllegalStateException(
                "Required environment variable not set: " + name
            ));
    }

    /**
     * Call external API
     *
     * @param endpoint API endpoint path
     * @return API response
     */
    public String callApi(String endpoint) {
        String url = apiEndpoint + endpoint;
        // API call logic (using API key)
        return makeHttpRequest(url, apiKey);
    }

    private String makeHttpRequest(String url, String apiKey) {
        // HTTP request implementation
        return "";
    }
}

/*
 * Environment variable setup examples:
 *
 * Linux/Mac:
 * export API_ENDPOINT=https://api.example.com
 * export API_KEY=your_secret_api_key
 *
 * Windows:
 * set API_ENDPOINT=https://api.example.com
 * set API_KEY=your_secret_api_key
 *
 * Docker:
 * docker run -e API_ENDPOINT=https://api.example.com -e API_KEY=secret myapp
 *
 * Kubernetes Secret:
 * kubectl create secret generic api-credentials \
 *   --from-literal=API_KEY=your_secret_api_key
 */
```

### Java - Secure Code (Spring Boot Configuration)

```java
// Secure example: Spring Boot configuration management
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Application Configuration Class
 *
 * Configuration is loaded from application.yml or environment variables.
 * Sensitive information is managed through secret management systems such as Spring Cloud Config, Vault, etc.
 */
@Configuration
@ConfigurationProperties(prefix = "app")
public class SecureApplicationConfig {

    /**
     * Database configuration
     *
     * Setup (application.yml):
     * app:
     *   database:
     *     url: ${DB_URL}
     *     username: ${DB_USERNAME}
     *     password: ${DB_PASSWORD}
     */
    private DatabaseConfig database;

    /**
     * External API configuration
     *
     * Secret management:
     * - Development: local environment variables
     * - Staging/Production: AWS Secrets Manager, HashiCorp Vault, etc.
     */
    private ApiConfig api;

    public static class DatabaseConfig {
        private String url;
        private String username;
        private String password;

        // Getters and setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ApiConfig {
        private String endpoint;
        private String key;

        // Getters and setters
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    public DatabaseConfig getDatabase() { return database; }
    public void setDatabase(DatabaseConfig database) { this.database = database; }
    public ApiConfig getApi() { return api; }
    public void setApi(ApiConfig api) { this.api = api; }
}

/**
 * Configuration file example (application.yml):
 *
 * app:
 *   database:
 *     url: ${DB_URL:jdbc:mysql://localhost:3306/mydb}
 *     username: ${DB_USERNAME}
 *     password: ${DB_PASSWORD}
 *   api:
 *     endpoint: ${API_ENDPOINT:https://api.example.com}
 *     key: ${API_KEY}
 *
 * Per-environment configuration:
 * - application-dev.yml: Development environment settings
 * - application-prod.yml: Production environment settings (exclude actual values, placeholders only)
 *
 * Secret injection:
 * - Kubernetes: Mount Secrets as environment variables
 * - AWS: Parameter Store or Secrets Manager
 * - Azure: Key Vault
 * - HashiCorp: Vault
 */
```

### C# - Secure Code

```csharp
// Secure example: Configuration management in C#
using Microsoft.Extensions.Configuration;
using System;
using System.IO;

/// <summary>
/// Secure data access class
///
/// Configuration is loaded from appsettings.json and environment variables.
/// Sensitive information is not included in comments and is managed through User Secrets or Azure Key Vault.
/// </summary>
public class SecureDataAccess
{
    private readonly string connectionString;
    private readonly IConfiguration configuration;

    public SecureDataAccess()
    {
        // Load configuration: appsettings.json, environment variables, User Secrets
        configuration = new ConfigurationBuilder()
            .SetBasePath(Directory.GetCurrentDirectory())
            .AddJsonFile("appsettings.json", optional: false, reloadOnChange: true)
            .AddJsonFile($"appsettings.{Environment.GetEnvironmentVariable("ASPNETCORE_ENVIRONMENT")}.json", optional: true)
            .AddEnvironmentVariables()
            .AddUserSecrets<SecureDataAccess>() // For development environment
            .Build();

        // Connection string loaded from environment variable or secrets
        connectionString = configuration.GetConnectionString("DefaultConnection")
            ?? throw new InvalidOperationException("Database connection string not configured");
    }

    /// <summary>
    /// Get API key
    ///
    /// Setup:
    /// - Development: dotnet user-secrets set "ApiSettings:Key" "your_api_key"
    /// - Production: Azure Key Vault, AWS Secrets Manager, etc.
    /// </summary>
    private string GetApiKey()
    {
        return configuration["ApiSettings:Key"]
            ?? throw new InvalidOperationException("API key not configured");
    }

    /// <summary>
    /// Get Azure Storage connection string
    ///
    /// Configuration is managed via environment variables or Azure Key Vault.
    /// </summary>
    private string GetAzureStorageConnectionString()
    {
        return configuration["AzureStorage:ConnectionString"]
            ?? Environment.GetEnvironmentVariable("AZURE_STORAGE_CONNECTION_STRING")
            ?? throw new InvalidOperationException("Azure Storage connection string not configured");
    }
}

/*
 * appsettings.json example:
 * {
 *   "ConnectionStrings": {
 *     "DefaultConnection": "Server=(localdb)\\mssqllocaldb;Database=MyDb;Trusted_Connection=True;"
 *   },
 *   "ApiSettings": {
 *     "Endpoint": "https://api.example.com"
 *   }
 * }
 *
 * Sensitive information is excluded and managed via User Secrets:
 * dotnet user-secrets init
 * dotnet user-secrets set "ConnectionStrings:DefaultConnection" "Server=prod-server;Database=ProdDb;User Id=user;Password=pass;"
 * dotnet user-secrets set "ApiSettings:Key" "actual_api_key_value"
 *
 * In production, use Azure Key Vault:
 * services.AddAzureKeyVault(
 *     $"https://{keyVaultName}.vault.azure.net/",
 *     clientId,
 *     certificate);
 */
```

### JavaScript - Secure Code

```javascript
// Secure example: Using environment variables in JavaScript

/**
 * Secure API Client
 *
 * All sensitive information is loaded from environment variables.
 * The .env file is added to .gitignore to exclude it from version control.
 */
class SecureApiClient {
    constructor() {
        // Load configuration from environment variables (Node.js)
        // Setup: define in .env file or set as environment variables
        this.apiUrl = process.env.API_URL;
        this.apiKey = process.env.API_KEY;

        if (!this.apiUrl || !this.apiKey) {
            throw new Error('Required environment variables not set: API_URL, API_KEY');
        }
    }

    /**
     * API call
     *
     * @param {string} endpoint - API endpoint
     * @returns {Promise} API response
     */
    async callApi(endpoint) {
        const url = `${this.apiUrl}${endpoint}`;

        const response = await fetch(url, {
            headers: {
                'Authorization': `Bearer ${this.apiKey}`,
                'Content-Type': 'application/json'
            }
        });

        return response.json();
    }
}

/**
 * Firebase Configuration (Secure Method)
 *
 * Client-side Firebase configuration is designed to be safe even if exposed,
 * but Firebase Security Rules MUST be configured for access control.
 */
function initializeFirebase() {
    // Load Firebase configuration from environment variables
    const firebaseConfig = {
        apiKey: process.env.REACT_APP_FIREBASE_API_KEY,
        authDomain: process.env.REACT_APP_FIREBASE_AUTH_DOMAIN,
        projectId: process.env.REACT_APP_FIREBASE_PROJECT_ID,
        storageBucket: process.env.REACT_APP_FIREBASE_STORAGE_BUCKET,
        messagingSenderId: process.env.REACT_APP_FIREBASE_MESSAGING_SENDER_ID,
        appId: process.env.REACT_APP_FIREBASE_APP_ID
    };

    // Initialize Firebase
    // firebase.initializeApp(firebaseConfig);
}

/*
 * .env file example (add to .gitignore!):
 *
 * # API Configuration
 * API_URL=https://api.example.com
 * API_KEY=your_secret_api_key_here
 *
 * # Firebase Configuration (React App)
 * REACT_APP_FIREBASE_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXX
 * REACT_APP_FIREBASE_AUTH_DOMAIN=myapp.firebaseapp.com
 * REACT_APP_FIREBASE_PROJECT_ID=myapp-project
 * REACT_APP_FIREBASE_STORAGE_BUCKET=myapp.appspot.com
 * REACT_APP_FIREBASE_MESSAGING_SENDER_ID=123456789
 * REACT_APP_FIREBASE_APP_ID=1:123456789:web:abcdef
 *
 * .env file security:
 * - Must be added to .gitignore
 * - Provide a .env.example template file (without actual values)
 * - Inject via environment variables in CI/CD
 */

/*
 * .gitignore configuration:
 *
 * # Environment configuration files
 * .env
 * .env.local
 * .env.*.local
 *
 * # Configuration files
 * config/secrets.json
 * config/production.json
 *
 * # Authentication files
 * *.pem
 * *.key
 * serviceAccountKey.json
 */

module.exports = { SecureApiClient, initializeFirebase };
```

### C - Secure Code

```c
// Secure example: Using external configuration files in C
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_CONFIG_LINE 256

/**
 * Secure database connection structure
 *
 * Configuration is loaded from external files or environment variables.
 */
typedef struct {
    char host[64];
    int port;
    char database[64];
    char username[64];
    char password[128];
} DatabaseConfig;

/**
 * Load database configuration from environment variables
 *
 * Setup:
 * export DB_HOST=localhost
 * export DB_PORT=3306
 * export DB_NAME=mydb
 * export DB_USER=dbuser
 * export DB_PASS=secure_password
 */
int load_db_config_from_env(DatabaseConfig *config) {
    char *host = getenv("DB_HOST");
    char *port_str = getenv("DB_PORT");
    char *database = getenv("DB_NAME");
    char *username = getenv("DB_USER");
    char *password = getenv("DB_PASS");

    if (!host || !port_str || !database || !username || !password) {
        fprintf(stderr, "Error: Required environment variables not set\n");
        fprintf(stderr, "Required: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS\n");
        return -1;
    }

    strncpy(config->host, host, sizeof(config->host) - 1);
    config->port = atoi(port_str);
    strncpy(config->database, database, sizeof(config->database) - 1);
    strncpy(config->username, username, sizeof(config->username) - 1);
    strncpy(config->password, password, sizeof(config->password) - 1);

    return 0;
}

/**
 * Load database configuration from config file
 *
 * Config file format (db.conf):
 * DB_HOST=localhost
 * DB_PORT=3306
 * DB_NAME=mydb
 * DB_USER=dbuser
 * DB_PASS=secure_password
 *
 * Note: Config file should not be included in version control (.gitignore)
 */
int load_db_config_from_file(const char *config_file, DatabaseConfig *config) {
    FILE *file = fopen(config_file, "r");
    if (!file) {
        fprintf(stderr, "Error: Cannot open config file: %s\n", config_file);
        return -1;
    }

    char line[MAX_CONFIG_LINE];
    while (fgets(line, sizeof(line), file)) {
        // Skip comments and blank lines
        if (line[0] == '#' || line[0] == '\n') {
            continue;
        }

        char key[64], value[128];
        if (sscanf(line, "%63[^=]=%127s", key, value) == 2) {
            if (strcmp(key, "DB_HOST") == 0) {
                strncpy(config->host, value, sizeof(config->host) - 1);
            } else if (strcmp(key, "DB_PORT") == 0) {
                config->port = atoi(value);
            } else if (strcmp(key, "DB_NAME") == 0) {
                strncpy(config->database, value, sizeof(config->database) - 1);
            } else if (strcmp(key, "DB_USER") == 0) {
                strncpy(config->username, value, sizeof(config->username) - 1);
            } else if (strcmp(key, "DB_PASS") == 0) {
                strncpy(config->password, value, sizeof(config->password) - 1);
            }
        }
    }

    fclose(file);
    return 0;
}

/**
 * Load encryption key from file
 *
 * Key file should be stored in a secure location with proper permissions (chmod 600)
 */
int load_encryption_key(const char *key_file, unsigned char *key, size_t key_size) {
    FILE *file = fopen(key_file, "rb");
    if (!file) {
        fprintf(stderr, "Error: Cannot open key file: %s\n", key_file);
        return -1;
    }

    size_t read = fread(key, 1, key_size, file);
    fclose(file);

    if (read != key_size) {
        fprintf(stderr, "Error: Invalid key file size\n");
        return -1;
    }

    return 0;
}

int main() {
    DatabaseConfig db_config = {0};

    // Load configuration from environment variables
    if (load_db_config_from_env(&db_config) == 0) {
        printf("Database configuration loaded from environment\n");
        // Database connection logic
    }

    // Or load from config file
    // if (load_db_config_from_file("/etc/myapp/db.conf", &db_config) == 0) {
    //     printf("Database configuration loaded from file\n");
    // }

    return 0;
}

/*
 * Config file security:
 *
 * 1. File permissions:
 *    chmod 600 /etc/myapp/db.conf
 *    chown myapp:myapp /etc/myapp/db.conf
 *
 * 2. Exclude from version control:
 *    echo "*.conf" >> .gitignore
 *    echo "*.key" >> .gitignore
 *
 * 3. Provide example file:
 *    cp db.conf.example db.conf
 *    (db.conf.example contains only placeholders)
 */
```

## Security Best Practices

### 1. Comment Writing Principles

```
DO (Recommended):
- Explain the purpose and behavior of code
- Describe algorithms and complex logic
- TODO, FIXME (excluding sensitive information)
- Configuration methods and environment variable names (excluding actual values)

DON'T (Prohibited):
- Passwords, API keys, tokens
- Database connection information
- Internal system IP addresses
- Encryption keys, salt values
- License keys
- Personal information (emails, phone numbers, etc.)
- Security vulnerability details (CVE numbers, etc.)
```

### 2. Sensitive Information Management

```java
// Use environment variables
String apiKey = System.getenv("API_KEY");

// Configuration files (excluded from version control)
Properties props = loadProperties("config.properties");

// Secret management systems
// - HashiCorp Vault
// - AWS Secrets Manager
// - Azure Key Vault
// - Google Secret Manager
```

### 3. Pre-Commit Verification

```bash
# Git pre-commit hook setup
#!/bin/bash
# .git/hooks/pre-commit

# Search for sensitive information patterns
if git diff --cached | grep -E "(password|api_key|secret|token)\s*[:=].*['\"]"; then
    echo "Error: Potential sensitive information found in commit"
    echo "Please remove hardcoded credentials before committing"
    exit 1
fi

# Search for sensitive information in comments
if git diff --cached | grep -E "//.*password|/\*.*password"; then
    echo "Warning: Password found in comments"
    exit 1
fi
```

### 4. Automated Scanning Tools

```bash
# Install and use git-secrets
git secrets --install
git secrets --register-aws

# Secret scanning with gitleaks
gitleaks detect --source . --verbose

# History scanning with truffleHog
trufflehog git file://. --only-verified
```

## Detection and Prevention

### Static Analysis Tools

```bash
# SonarQube Rules
- squid:S2068: Credentials should not be hard-coded
- squid:CommentedOutCodeLine: Commented-out code should be removed

# SpotBugs Rules
- DMI_HARDCODED_ABSOLUTE_FILENAME
- DMI_CONSTANT_DB_PASSWORD

# PMD Rules
- CommentRequired
- CommentSize
- CommentContent (custom rule)

# Checkstyle Configuration
<module name="Regexp">
    <property name="format" value="password\s*[:=]"/>
    <property name="illegalPattern" value="true"/>
    <property name="message" value="Avoid hardcoding passwords"/>
</module>
```

### CI/CD Pipeline Integration

```yaml
# GitLab CI Example
security-scan:
  stage: test
  script:
    - gitleaks detect --source . --verbose --report-path gitleaks-report.json
    - trufflehog filesystem . --json > trufflehog-report.json
  artifacts:
    reports:
      container_scanning: gitleaks-report.json
  allow_failure: false

# GitHub Actions Example
name: Secret Scanning
on: [push, pull_request]
jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Gitleaks
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### Code Review Checklist

```
[] Are there no passwords or API keys included in comments?
[] Is there no database connection information in comments?
[] Are internal system IP addresses or network information not exposed?
[] Are there no encryption keys or salt values in comments?
[] Are there no detailed security vulnerability descriptions in TODO comments?
[] Are test account credentials not left in comments?
[] Are license keys or serial numbers not included?
[] Is sensitive information managed via environment variables or external configuration?
```

## Testing Methods

### Automated Scanning

```java
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

class CommentSecurityTest {

    private static final Pattern[] SENSITIVE_PATTERNS = {
        Pattern.compile("password\\s*[:=]\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("api[_-]?key\\s*[:=]\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("secret\\s*[:=]\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("token\\s*[:=]\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("//.*password.*[:=]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("/\\*.*password.*\\*/", Pattern.CASE_INSENSITIVE)
    };

    @Test
    void testNoSensitiveInfoInComments() throws IOException {
        Path srcPath = Paths.get("src/main/java");

        try (Stream<Path> paths = Files.walk(srcPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".java"))
                 .forEach(this::checkFileForSensitiveInfo);
        }
    }

    private void checkFileForSensitiveInfo(Path file) {
        try {
            String content = Files.readString(file);

            for (Pattern pattern : SENSITIVE_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    throw new AssertionError(
                        "Sensitive information found in file: " + file +
                        "\nPattern: " + pattern.pattern()
                    );
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading file: " + file, e);
        }
    }
}
```

### Manual Verification

```bash
# Search for sensitive information in source code
grep -r "password" --include="*.java" src/
grep -r "api.?key" --include="*.java" src/
grep -r "secret" --include="*.java" src/

# Search for specific patterns in comments
grep -r "//.*password.*=" --include="*.java" src/
grep -r "/\*.*password.*\*/" --include="*.java" src/

# Search for sensitive information in Git history
git log -p -S "password" --all
git log -p -S "api_key" --all
```

## Related Vulnerabilities

- **CWE-547**: Use of Hard-coded, Security-relevant Constants
- **CWE-798**: Use of Hard-coded Credentials
- **CWE-200**: Exposure of Sensitive Information to an Unauthorized Actor
- **CWE-540**: Inclusion of Sensitive Information in Source Code

## References

### Tools
- **git-secrets**: Secret detection tool developed by AWS
- **gitleaks**: Secret scanning for Git repositories
- **truffleHog**: High-entropy string detection in Git history
- **detect-secrets**: Secret detection tool developed by Yelp

### Guides
- OWASP Code Review Guide
- OWASP Testing Guide: Information Gathering
- CWE-615: https://cwe.mitre.org/data/definitions/615.html
- GitHub Secret Scanning: https://docs.github.com/en/code-security/secret-scanning

## Checklist

### Development Phase
- [ ] No sensitive information (passwords, API keys, tokens) included in comments
- [ ] Database connection information managed via external configuration
- [ ] Encryption keys managed via environment variables or secret management systems
- [ ] No detailed security vulnerability information in TODO comments
- [ ] Test account credentials removed from comments

### Pre-Commit
- [ ] Commit content verified with tools such as git-secrets
- [ ] Comment content reviewed
- [ ] Configuration files added to .gitignore confirmed

### Code Review
- [ ] Reviewed whether comments contain sensitive information
- [ ] Confirmed use of external configuration files
- [ ] Confirmed use of environment variables

### Pre-Deployment
- [ ] Final verification of sensitive information in comments in production code
- [ ] Confirmed configuration files are not included in version control
- [ ] Secret scanning tools integrated into CI/CD pipeline

---

**Last Updated**: 2025-02-05
