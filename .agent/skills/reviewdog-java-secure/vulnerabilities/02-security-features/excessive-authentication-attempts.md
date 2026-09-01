# Improper Restriction of Excessive Authentication Attempts

**CWE-307: Improper Restriction of Excessive Authentication Attempts**

## Overview

Improper Restriction of Excessive Authentication Attempts is a security vulnerability that occurs when systems allow unlimited authentication attempts in login, password reset, OTP verification, and other authentication processes. Attackers can exploit this through brute force attacks, credential stuffing, and dictionary attacks to compromise accounts through repeated authentication attempts. Defense mechanisms such as attempt limiting, account lockout, increasing delays, and CAPTCHA are essential.

## Severity
- **CVSS v3.1 Score**: 7.5 (High)
- **Risk Level**: High
- **Impact**: Account Compromise, Unauthorized Access

## Vulnerability Impact

### Attack Scenarios

#### Scenario 1: Admin Account Compromise via Brute Force Attack
```
1. Attacker discovers the login page of a web application
2. Confirms existence of "admin" account
3. Starts brute force password guessing using automated tools (Hydra, Burp Suite)
4. System allows unlimited login attempts without any restrictions
5. Sequentially tries 100,000 common password combinations
6. Discovers weak password "admin123" and successfully logs in
7. Result: Administrator privileges obtained, full system compromise
```

#### Scenario 2: Credential Stuffing Attack
```
1. Attacker obtains 1 million leaked credentials from other sites
2. Attempts login on target service with the same email/password combinations
3. System has no per-IP attempt limit, allowing 100+ attempts per second
4. Uses distributed botnet to attack simultaneously from multiple IPs
5. Compromises thousands of accounts from users who reuse passwords
6. Result: Mass account compromise, personal data breach
```

#### Scenario 3: OTP Brute Force Attack
```
1. User requests a password reset
2. System sends a 6-digit numeric OTP via SMS
3. Attacker intercepts the reset session via man-in-the-middle attack
4. Automatically tries all 1 million combinations from 000000 to 999999
5. System has no OTP attempt limit
6. Discovers the correct OTP and successfully resets the password
7. Result: Account compromise, two-factor authentication bypass
```

#### Scenario 4: Distributed Brute Force Attack
```
1. Attacker obtains thousands of botnet IPs
2. Makes a small number of login attempts from each IP (10 per day per IP)
3. Bypasses IP-based limits while conducting a large-scale attack
4. System has no per-account limiting, making defense impossible
5. Cracks weak passwords through millions of attempts over a week
6. Result: Takes time but eventually succeeds in account compromise
```

## Vulnerable Code Examples

### Java - Vulnerable Code (Unlimited Login Attempts)

```java
// Vulnerable example: No login attempt limit
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

public class VulnerableLoginServlet extends HttpServlet {

    /**
     * Vulnerable login handling
     *
     * Problems:
     * 1. No login attempt limit
     * 2. No delay on failure
     * 3. No account lockout mechanism
     * 4. No CAPTCHA
     * 5. Unlimited brute force attacks possible
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // Vulnerability: Infinite loop allowing unlimited attempts until login succeeds
        int result = FAIL;
        while (result == FAIL) {
            result = verifyUser(username, password);

            if (result == FAIL) {
                // Vulnerability: No restrictions or delays on failure
                System.out.println("Login failed, trying again...");
            }
        }

        if (result == SUCCESS) {
            // Login successful
            HttpSession session = request.getSession(true);
            session.setAttribute("username", username);
            response.sendRedirect("/dashboard");
        } else {
            response.sendRedirect("/login?error=1");
        }
    }

    /**
     * Vulnerable user verification
     */
    private int verifyUser(String username, String password) {
        try (Connection conn = getDBConnection()) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();

            // Vulnerability: Only checks success/failure, no attempt counting
            if (rs.next()) {
                return SUCCESS;
            } else {
                return FAIL;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return FAIL;
        }
    }

    private static final int SUCCESS = 1;
    private static final int FAIL = 0;

    private Connection getDBConnection() throws SQLException {
        // DB connection
        return null;
    }
}

/*
 * Attack scenario:
 *
 * Attacker makes 100 login attempts per second with automated tools:
 * POST /login username=admin&password=password1
 * POST /login username=admin&password=password2
 * POST /login username=admin&password=password3
 * ...
 * POST /login username=admin&password=admin123 (success!)
 *
 * With no attempt limit, thousands or tens of thousands of attempts are possible
 */
```

### Java - Vulnerable Code (Unlimited OTP Attempts)

```java
// Vulnerable example: No OTP verification attempt limit
import java.util.concurrent.ConcurrentHashMap;
import java.util.Random;

public class VulnerableOTPService {

    // OTP storage (in practice, use Redis, etc.)
    private ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();
    private Random random = new Random();

    /**
     * Generate and send OTP
     */
    public void generateAndSendOTP(String userId, String phoneNumber) {
        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Store OTP
        otpStore.put(userId, otp);

        // Send SMS
        sendSMS(phoneNumber, "Your OTP is: " + otp);

        System.out.println("OTP sent to " + phoneNumber + ": " + otp);
    }

    /**
     * Vulnerable OTP verification (no attempt limit)
     *
     * Problems:
     * 1. Unlimited OTP attempts possible
     * 2. 6-digit number has only 1 million combinations (brute-forceable)
     * 3. No delay on failed attempts
     * 4. No lockout mechanism
     */
    public boolean verifyOTP(String userId, String inputOTP) {
        String storedOTP = otpStore.get(userId);

        if (storedOTP == null) {
            return false;
        }

        // Vulnerability: Unlimited attempts possible
        if (storedOTP.equals(inputOTP)) {
            // OTP verification successful - remove after use
            otpStore.remove(userId);
            return true;
        }

        // Vulnerability: Can keep trying after failure
        return false;
    }

    /**
     * Vulnerable password reset
     */
    public boolean resetPassword(String userId, String otp, String newPassword) {
        // Vulnerability: Unlimited OTP attempts possible
        while (!verifyOTP(userId, otp)) {
            // Keep trying
            System.out.println("Invalid OTP, trying again...");
        }

        // OTP verification successful - change password
        updatePassword(userId, newPassword);
        return true;
    }

    private void sendSMS(String phoneNumber, String message) {
        // SMS sending logic
    }

    private void updatePassword(String userId, String newPassword) {
        // Password update logic
    }
}

/*
 * Attack scenario:
 *
 * Attacker brute forces OTP:
 * verifyOTP("victim", "000000") -> false
 * verifyOTP("victim", "000001") -> false
 * verifyOTP("victim", "000002") -> false
 * ...
 * verifyOTP("victim", "123456") -> true (success!)
 *
 * In the worst case, all OTPs can be cracked in 1 million attempts
 */
```

### Java - Vulnerable Code (No IP-Based Restrictions)

```java
// Vulnerable example: No IP-based access restrictions
import javax.servlet.http.*;
import java.io.IOException;

public class VulnerableAuthenticationController {

    /**
     * Vulnerable login (no IP restrictions)
     *
     * Problems:
     * 1. No per-IP attempt limit
     * 2. Cannot defend against distributed attacks
     * 3. Unlimited attempts from the same IP
     */
    public void login(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String clientIP = request.getRemoteAddr();

        // Vulnerability: No IP tracking or restrictions
        boolean authenticated = authenticate(username, password);

        if (authenticated) {
            HttpSession session = request.getSession(true);
            session.setAttribute("user", username);
            response.sendRedirect("/dashboard");
        } else {
            // Vulnerability: No failure count tracking
            response.sendRedirect("/login?error=1");
        }

        /*
         * Vulnerabilities:
         * - Over 1000 attempts per second possible from same IP
         * - Cannot defend against distributed attacks using botnets
         * - No rate limiting
         */
    }

    /**
     * Vulnerable API authentication (no rate limiting)
     */
    public void apiLogin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String apiKey = request.getHeader("X-API-Key");
        String clientIP = request.getRemoteAddr();

        // Vulnerability: No API call limit
        boolean valid = validateAPIKey(apiKey);

        if (valid) {
            response.setStatus(200);
            response.getWriter().write("{\"status\":\"success\"}");
        } else {
            // Vulnerability: No restrictions or blocking on failure
            response.setStatus(401);
            response.getWriter().write("{\"status\":\"unauthorized\"}");
        }

        /*
         * Attack scenario:
         * Attacker can try thousands of API keys per second:
         * GET /api/data (X-API-Key: aaaa-bbbb-cccc-dddd)
         * GET /api/data (X-API-Key: aaaa-bbbb-cccc-ddde)
         * GET /api/data (X-API-Key: aaaa-bbbb-cccc-dddf)
         * ...
         */
    }

    private boolean authenticate(String username, String password) {
        // Authentication logic
        return false;
    }

    private boolean validateAPIKey(String apiKey) {
        // API key validation logic
        return false;
    }
}
```

### C# - Vulnerable Code

```csharp
// Vulnerable example: No login attempt limit in C#
using System;
using System.Data.SqlClient;
using System.Web;

public class VulnerableAuthController
{
    /// <summary>
    /// Vulnerable login handling
    ///
    /// Problems:
    /// - No attempt limit
    /// - No account lockout
    /// - No delay
    /// </summary>
    public bool Login(string username, string password)
    {
        int result = 0;

        // Vulnerability: Infinite loop until success
        while (result == 0)
        {
            result = VerifyUser(username, password);

            if (result == 0)
            {
                Console.WriteLine("Login failed, retrying...");
                // Vulnerability: No delay or restrictions on failure
            }
        }

        if (result == 1)
        {
            // Login successful
            HttpContext.Current.Session["Username"] = username;
            return true;
        }

        return false;
    }

    /// <summary>
    /// Vulnerable user verification
    /// </summary>
    private int VerifyUser(string username, string password)
    {
        string connectionString = GetConnectionString();

        using (SqlConnection conn = new SqlConnection(connectionString))
        {
            conn.Open();

            string query = "SELECT COUNT(*) FROM Users WHERE Username = @username AND Password = @password";
            SqlCommand cmd = new SqlCommand(query, conn);
            cmd.Parameters.AddWithValue("@username", username);
            cmd.Parameters.AddWithValue("@password", password);

            int count = (int)cmd.ExecuteScalar();

            // Vulnerability: No attempt count tracking
            return count > 0 ? 1 : 0;
        }
    }

    /// <summary>
    /// Vulnerable PIN verification
    /// </summary>
    public bool VerifyPIN(string userId, string pin)
    {
        string correctPIN = GetUserPIN(userId);

        // Vulnerability: Unlimited PIN attempts possible
        // A 4-digit PIN has only 10,000 possible combinations
        return pin == correctPIN;
    }

    private string GetConnectionString() => "";
    private string GetUserPIN(string userId) => "1234";
}
```

### C - Vulnerable Code

```c
// Vulnerable example: No login attempt limit in C
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#define MAX_USERNAME 50
#define MAX_PASSWORD 50

/**
 * Vulnerable user verification
 */
int verify_user(const char* username, const char* password) {
    // Simple hardcoded verification (example)
    if (strcmp(username, "admin") == 0 && strcmp(password, "admin123") == 0) {
        return 1; // Success
    }
    return 0; // Failure
}

/**
 * Vulnerable login function (unlimited attempts)
 *
 * Problems:
 * - Infinite attempts via while loop
 * - No attempt limit
 * - No delay on failure
 */
int vulnerable_login() {
    char username[MAX_USERNAME];
    char password[MAX_PASSWORD];
    int is_valid_user = 0;

    printf("=== Login System ===\n");

    // Vulnerability: Can keep trying until success
    while (is_valid_user == 0) {
        printf("Username: ");
        scanf("%49s", username);

        printf("Password: ");
        scanf("%49s", password);

        is_valid_user = verify_user(username, password);

        if (is_valid_user == 0) {
            // Vulnerability: No restrictions or delays on failure
            printf("Invalid credentials. Try again.\n\n");
        }
    }

    printf("Login successful!\n");
    return 1;
}

/**
 * Vulnerable PIN verification (unlimited attempts)
 */
int verify_pin_vulnerable(const char* correct_pin) {
    char input_pin[5];

    printf("=== PIN Verification ===\n");

    // Vulnerability: Unlimited PIN attempts possible
    while (1) {
        printf("Enter 4-digit PIN: ");
        scanf("%4s", input_pin);

        if (strcmp(input_pin, correct_pin) == 0) {
            printf("PIN verified successfully!\n");
            return 1;
        } else {
            // Vulnerability: Can keep trying after failure
            printf("Invalid PIN. Try again.\n");
        }
    }

    return 0;
}

/**
 * Vulnerable main function
 */
int main() {
    // Vulnerability: Unlimited login attempts
    vulnerable_login();

    // Vulnerability: Unlimited PIN attempts
    verify_pin_vulnerable("1234");

    /*
     * Attack scenario:
     * Attacker tries all combinations with an automated script:
     *
     * for password in password_list:
     *     try_login("admin", password)
     *
     * Takes time but eventually cracks weak passwords
     */

    return 0;
}
```

## Secure Code Examples

### Java - Secure Code (Attempt Limiting)

```java
// Secure example: Login attempt limiting
import javax.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Secure login service
 *
 * Security features:
 * - Per-account attempt limiting
 * - Temporary account lockout
 * - IP-based rate limiting
 * - Progressive delay
 * - Login failure event logging
 */
public class SecureLoginServlet extends HttpServlet {

    // Per-account login attempt tracking
    private static final ConcurrentHashMap<String, LoginAttempt> attemptTracker = new ConcurrentHashMap<>();

    // Per-IP rate limiting
    private static final ConcurrentHashMap<String, IPRateLimit> ipRateLimits = new ConcurrentHashMap<>();

    // Security settings
    private static final int MAX_ATTEMPTS = 5;              // Maximum attempt count
    private static final long LOCKOUT_DURATION_MS = 300000; // 5-minute lockout
    private static final int IP_MAX_REQUESTS = 20;          // Maximum requests per IP per minute
    private static final long IP_WINDOW_MS = 60000;         // 1-minute window

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String clientIP = request.getRemoteAddr();

        // 1. IP-based rate limiting check
        if (isIPRateLimited(clientIP)) {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("Too many requests. Please try again later.");
            logSecurityEvent("IP rate limited", clientIP, username);
            return;
        }

        // 2. Account lockout status check
        if (isAccountLocked(username)) {
            response.setStatus(403); // Forbidden
            response.getWriter().write("Account temporarily locked. Please try again later.");
            logSecurityEvent("Account locked", clientIP, username);
            return;
        }

        // 3. Authentication attempt
        boolean authenticated = authenticate(username, password);

        if (authenticated) {
            // Login successful - reset attempt count
            resetLoginAttempts(username);

            HttpSession session = request.getSession(true);
            session.setAttribute("username", username);

            logSecurityEvent("Login successful", clientIP, username);
            response.sendRedirect("/dashboard");

        } else {
            // Login failed - increment attempt count
            incrementLoginAttempts(username, clientIP);

            LoginAttempt attempt = attemptTracker.get(username);
            int remainingAttempts = MAX_ATTEMPTS - attempt.getCount();

            logSecurityEvent("Login failed", clientIP, username);

            if (remainingAttempts <= 0) {
                response.getWriter().write("Account locked due to too many failed attempts.");
            } else {
                response.getWriter().write(
                    "Invalid credentials. " + remainingAttempts + " attempts remaining."
                );
            }

            // Apply progressive delay
            applyProgressiveDelay(attempt.getCount());
        }
    }

    /**
     * Check account lockout status
     */
    private boolean isAccountLocked(String username) {
        LoginAttempt attempt = attemptTracker.get(username);

        if (attempt == null) {
            return false;
        }

        // Check if maximum attempts exceeded
        if (attempt.getCount() >= MAX_ATTEMPTS) {
            // Check if lockout time has elapsed
            long timeSinceLock = System.currentTimeMillis() - attempt.getLockTime();

            if (timeSinceLock < LOCKOUT_DURATION_MS) {
                return true; // Still locked
            } else {
                // Unlock - reset attempt count
                resetLoginAttempts(username);
                return false;
            }
        }

        return false;
    }

    /**
     * Increment login attempt count
     */
    private void incrementLoginAttempts(String username, String clientIP) {
        attemptTracker.compute(username, (key, attempt) -> {
            if (attempt == null) {
                return new LoginAttempt(1, clientIP);
            } else {
                attempt.increment();
                if (attempt.getCount() >= MAX_ATTEMPTS) {
                    attempt.lock();
                }
                return attempt;
            }
        });
    }

    /**
     * Reset login attempt count
     */
    private void resetLoginAttempts(String username) {
        attemptTracker.remove(username);
    }

    /**
     * IP-based rate limiting
     */
    private boolean isIPRateLimited(String clientIP) {
        long currentTime = System.currentTimeMillis();

        IPRateLimit rateLimit = ipRateLimits.compute(clientIP, (key, limit) -> {
            if (limit == null || (currentTime - limit.getWindowStart()) > IP_WINDOW_MS) {
                // New window starts
                return new IPRateLimit(currentTime, 1);
            } else {
                // Existing window - increment counter
                limit.increment();
                return limit;
            }
        });

        return rateLimit.getCount() > IP_MAX_REQUESTS;
    }

    /**
     * Progressive delay (brute force defense)
     */
    private void applyProgressiveDelay(int attemptCount) {
        if (attemptCount <= 1) {
            return; // No delay for first failure
        }

        // Delay increases with attempt count
        // 2nd: 1s, 3rd: 2s, 4th: 4s, 5th: 8s
        long delayMs = (long) Math.pow(2, attemptCount - 2) * 1000;
        delayMs = Math.min(delayMs, 10000); // Maximum 10 seconds

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Security event logging
     */
    private void logSecurityEvent(String event, String ip, String username) {
        System.out.println(String.format(
            "[SECURITY] %s | IP: %s | Username: %s | Time: %s",
            event, ip, username, new java.util.Date()
        ));

        // In production, send to SIEM
    }

    private boolean authenticate(String username, String password) {
        // Actual authentication logic
        return false;
    }

    /**
     * Login attempt tracking class
     */
    private static class LoginAttempt {
        private int count;
        private long lockTime;
        private String lastIP;

        public LoginAttempt(int count, String ip) {
            this.count = count;
            this.lastIP = ip;
        }

        public void increment() {
            this.count++;
        }

        public void lock() {
            this.lockTime = System.currentTimeMillis();
        }

        public int getCount() {
            return count;
        }

        public long getLockTime() {
            return lockTime;
        }
    }

    /**
     * IP rate limiting class
     */
    private static class IPRateLimit {
        private long windowStart;
        private int count;

        public IPRateLimit(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }

        public void increment() {
            this.count++;
        }

        public long getWindowStart() {
            return windowStart;
        }

        public int getCount() {
            return count;
        }
    }
}
```

### Java - Secure Code (OTP Attempt Limiting)

```java
// Secure example: OTP attempt limiting
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure OTP service
 *
 * Security features:
 * - OTP attempt limit (maximum 3)
 * - OTP expiration time (5 minutes)
 * - Progressive delay on failure
 * - OTP invalidation on consecutive failures
 */
public class SecureOTPService {

    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final long OTP_EXPIRY_MS = 300000; // 5 minutes
    private static final long OTP_COOLDOWN_MS = 60000; // 1 minute cooldown

    private ConcurrentHashMap<String, OTPData> otpStore = new ConcurrentHashMap<>();
    private SecureRandom random = new SecureRandom();

    /**
     * Generate and send OTP
     */
    public boolean generateAndSendOTP(String userId, String phoneNumber) {
        // Check OTP resend cooldown
        OTPData existingOTP = otpStore.get(userId);
        if (existingOTP != null) {
            long timeSinceGeneration = System.currentTimeMillis() - existingOTP.getGeneratedTime();
            if (timeSinceGeneration < OTP_COOLDOWN_MS) {
                System.out.println("Please wait before requesting a new OTP");
                return false;
            }
        }

        // Generate 6-digit numeric OTP
        String otp = String.format("%06d", random.nextInt(1000000));

        // Create and store OTP data
        OTPData otpData = new OTPData(otp, System.currentTimeMillis());
        otpStore.put(userId, otpData);

        // Send SMS
        sendSMS(phoneNumber, "Your OTP is: " + otp + ". Valid for 5 minutes.");

        System.out.println("OTP generated for user: " + userId);
        return true;
    }

    /**
     * Secure OTP verification (with attempt limiting)
     */
    public boolean verifyOTP(String userId, String inputOTP) {
        OTPData otpData = otpStore.get(userId);

        if (otpData == null) {
            System.out.println("No OTP found for user: " + userId);
            return false;
        }

        // Check OTP expiration
        long currentTime = System.currentTimeMillis();
        if ((currentTime - otpData.getGeneratedTime()) > OTP_EXPIRY_MS) {
            otpStore.remove(userId);
            System.out.println("OTP expired for user: " + userId);
            return false;
        }

        // Check maximum attempt count
        if (otpData.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
            otpStore.remove(userId);
            System.out.println("Maximum OTP attempts exceeded for user: " + userId);
            logSecurityEvent("OTP max attempts exceeded", userId);
            return false;
        }

        // OTP verification
        if (otpData.getOtp().equals(inputOTP)) {
            // Verification successful - remove OTP
            otpStore.remove(userId);
            System.out.println("OTP verified successfully for user: " + userId);
            return true;
        } else {
            // Verification failed - increment attempt count
            otpData.incrementAttempt();

            int remainingAttempts = MAX_OTP_ATTEMPTS - otpData.getAttemptCount();
            System.out.println("Invalid OTP. " + remainingAttempts + " attempts remaining.");

            // Progressive delay
            applyDelay(otpData.getAttemptCount());

            // Invalidate OTP when maximum attempts reached
            if (remainingAttempts <= 0) {
                otpStore.remove(userId);
                logSecurityEvent("OTP invalidated due to failed attempts", userId);
            }

            return false;
        }
    }

    /**
     * Apply progressive delay
     */
    private void applyDelay(int attemptCount) {
        long delayMs = attemptCount * 1000L; // 1s, 2s, 3s...
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Security event logging
     */
    private void logSecurityEvent(String event, String userId) {
        System.out.println(String.format(
            "[SECURITY] %s | User: %s | Time: %s",
            event, userId, new java.util.Date()
        ));
    }

    private void sendSMS(String phoneNumber, String message) {
        // SMS sending logic
        System.out.println("SMS sent to " + phoneNumber + ": " + message);
    }

    /**
     * OTP data class
     */
    private static class OTPData {
        private final String otp;
        private final long generatedTime;
        private int attemptCount;

        public OTPData(String otp, long generatedTime) {
            this.otp = otp;
            this.generatedTime = generatedTime;
            this.attemptCount = 0;
        }

        public String getOtp() {
            return otp;
        }

        public long getGeneratedTime() {
            return generatedTime;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public void incrementAttempt() {
            this.attemptCount++;
        }
    }

    // Usage example
    public static void main(String[] args) {
        SecureOTPService otpService = new SecureOTPService();

        // Generate OTP
        otpService.generateAndSendOTP("user123", "+821012345678");

        // OTP verification (maximum 3 attempts)
        System.out.println(otpService.verifyOTP("user123", "000000")); // false (1st failure)
        System.out.println(otpService.verifyOTP("user123", "111111")); // false (2nd failure)
        System.out.println(otpService.verifyOTP("user123", "222222")); // false (3rd failure)
        System.out.println(otpService.verifyOTP("user123", "123456")); // false (OTP invalidated)
    }
}
```

### Java - Secure Code (CAPTCHA Integration)

```java
// Secure example: Automation prevention using CAPTCHA
import javax.servlet.http.*;
import java.io.IOException;
import java.net.URLEncoder;
import org.json.JSONObject;

/**
 * Login service with CAPTCHA integration
 *
 * Uses Google reCAPTCHA v3
 */
public class CAPTCHALoginServlet extends HttpServlet {

    private static final String RECAPTCHA_SECRET_KEY = System.getenv("RECAPTCHA_SECRET");
    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";
    private static final double RECAPTCHA_THRESHOLD = 0.5; // 0.0 (bot) ~ 1.0 (human)

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String recaptchaResponse = request.getParameter("g-recaptcha-response");

        // 1. reCAPTCHA verification
        if (!verifyCaptcha(recaptchaResponse, request.getRemoteAddr())) {
            response.setStatus(403);
            response.getWriter().write("CAPTCHA verification failed. Please try again.");
            logSecurityEvent("CAPTCHA failed", request.getRemoteAddr(), username);
            return;
        }

        // 2. Authentication processing
        if (authenticate(username, password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("username", username);
            response.sendRedirect("/dashboard");
        } else {
            response.sendRedirect("/login?error=1");
        }
    }

    /**
     * Google reCAPTCHA verification
     */
    private boolean verifyCaptcha(String recaptchaResponse, String clientIP) {
        if (recaptchaResponse == null || recaptchaResponse.isEmpty()) {
            return false;
        }

        try {
            // reCAPTCHA API call
            String postData = "secret=" + URLEncoder.encode(RECAPTCHA_SECRET_KEY, "UTF-8")
                + "&response=" + URLEncoder.encode(recaptchaResponse, "UTF-8")
                + "&remoteip=" + URLEncoder.encode(clientIP, "UTF-8");

            java.net.URL url = new java.net.URL(RECAPTCHA_VERIFY_URL);
            javax.net.ssl.HttpsURLConnection conn = (javax.net.ssl.HttpsURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            java.io.OutputStream out = conn.getOutputStream();
            out.write(postData.getBytes());
            out.flush();
            out.close();

            // Parse response
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream())
            );
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            reader.close();

            JSONObject json = new JSONObject(result.toString());
            boolean success = json.getBoolean("success");
            double score = json.optDouble("score", 0.0);

            // reCAPTCHA v3 is score-based (0.0 ~ 1.0)
            return success && score >= RECAPTCHA_THRESHOLD;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean authenticate(String username, String password) {
        // Authentication logic
        return false;
    }

    private void logSecurityEvent(String event, String ip, String username) {
        System.out.println(String.format(
            "[SECURITY] %s | IP: %s | Username: %s",
            event, ip, username
        ));
    }
}

/*
 * HTML example:
 *
 * <form method="POST" action="/login">
 *     <input type="text" name="username" required>
 *     <input type="password" name="password" required>
 *     <input type="hidden" name="g-recaptcha-response" id="recaptcha-response">
 *     <button type="submit">Login</button>
 * </form>
 *
 * <script src="https://www.google.com/recaptcha/api.js?render=YOUR_SITE_KEY"></script>
 * <script>
 *     grecaptcha.ready(function() {
 *         grecaptcha.execute('YOUR_SITE_KEY', {action: 'login'}).then(function(token) {
 *             document.getElementById('recaptcha-response').value = token;
 *         });
 *     });
 * </script>
 */
```

### C# - Secure Code

```csharp
// Secure example: Login attempt limiting in C#
using System;
using System.Collections.Concurrent;
using System.Threading;

/// <summary>
/// Secure authentication service
/// </summary>
public class SecureAuthenticationService
{
    private const int MaxAttempts = 5;
    private const int LockoutDurationMinutes = 5;

    private static readonly ConcurrentDictionary<string, LoginAttempt> attemptTracker =
        new ConcurrentDictionary<string, LoginAttempt>();

    /// <summary>
    /// Secure login
    /// </summary>
    public bool Login(string username, string password)
    {
        // Check account lockout
        if (IsAccountLocked(username))
        {
            Console.WriteLine($"Account locked for {username}");
            return false;
        }

        // Authentication attempt
        bool authenticated = Authenticate(username, password);

        if (authenticated)
        {
            // Success - reset attempt count
            ResetLoginAttempts(username);
            return true;
        }
        else
        {
            // Failure - increment attempt count
            IncrementLoginAttempts(username);

            LoginAttempt attempt = attemptTracker[username];
            int remaining = MaxAttempts - attempt.Count;

            Console.WriteLine($"Login failed. {remaining} attempts remaining.");

            // Progressive delay
            ApplyProgressiveDelay(attempt.Count);

            return false;
        }
    }

    /// <summary>
    /// Check account lockout
    /// </summary>
    private bool IsAccountLocked(string username)
    {
        if (!attemptTracker.TryGetValue(username, out LoginAttempt attempt))
        {
            return false;
        }

        if (attempt.Count >= MaxAttempts)
        {
            TimeSpan lockDuration = DateTime.Now - attempt.LockTime;

            if (lockDuration.TotalMinutes < LockoutDurationMinutes)
            {
                return true; // Still locked
            }
            else
            {
                // Unlock
                ResetLoginAttempts(username);
                return false;
            }
        }

        return false;
    }

    /// <summary>
    /// Increment login attempt count
    /// </summary>
    private void IncrementLoginAttempts(string username)
    {
        attemptTracker.AddOrUpdate(username,
            new LoginAttempt { Count = 1 },
            (key, existing) =>
            {
                existing.Count++;
                if (existing.Count >= MaxAttempts)
                {
                    existing.LockTime = DateTime.Now;
                }
                return existing;
            });
    }

    /// <summary>
    /// Reset login attempt count
    /// </summary>
    private void ResetLoginAttempts(string username)
    {
        attemptTracker.TryRemove(username, out _);
    }

    /// <summary>
    /// Progressive delay
    /// </summary>
    private void ApplyProgressiveDelay(int attemptCount)
    {
        if (attemptCount <= 1) return;

        int delayMs = (int)Math.Pow(2, attemptCount - 2) * 1000;
        delayMs = Math.Min(delayMs, 10000); // Maximum 10 seconds

        Thread.Sleep(delayMs);
    }

    private bool Authenticate(string username, string password)
    {
        // Actual authentication logic
        return false;
    }

    private class LoginAttempt
    {
        public int Count { get; set; }
        public DateTime LockTime { get; set; }
    }
}
```

### C - Secure Code

```c
// Secure example: Login attempt limiting in C
#include <stdio.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define MAX_ATTEMPTS 5
#define LOCKOUT_DURATION 300  // 5 minutes (seconds)

typedef struct {
    int attempt_count;
    time_t lock_time;
} LoginAttempt;

LoginAttempt login_attempts[100];  // Simple example array
int attempt_tracker_size = 0;

/**
 * Check account lockout
 */
int is_account_locked(const char* username, int user_id) {
    if (user_id >= attempt_tracker_size || user_id < 0) {
        return 0;
    }

    LoginAttempt* attempt = &login_attempts[user_id];

    if (attempt->attempt_count >= MAX_ATTEMPTS) {
        time_t current_time = time(NULL);
        double elapsed = difftime(current_time, attempt->lock_time);

        if (elapsed < LOCKOUT_DURATION) {
            printf("Account locked. Try again in %.0f seconds.\n",
                   LOCKOUT_DURATION - elapsed);
            return 1;  // Still locked
        } else {
            // Unlock
            attempt->attempt_count = 0;
            return 0;
        }
    }

    return 0;
}

/**
 * Increment login attempt count
 */
void increment_login_attempts(int user_id) {
    if (user_id >= attempt_tracker_size) {
        attempt_tracker_size = user_id + 1;
    }

    login_attempts[user_id].attempt_count++;

    if (login_attempts[user_id].attempt_count >= MAX_ATTEMPTS) {
        login_attempts[user_id].lock_time = time(NULL);
        printf("Account locked due to too many failed attempts.\n");
    }
}

/**
 * Reset login attempt count
 */
void reset_login_attempts(int user_id) {
    if (user_id < attempt_tracker_size) {
        login_attempts[user_id].attempt_count = 0;
        login_attempts[user_id].lock_time = 0;
    }
}

/**
 * Progressive delay
 */
void apply_progressive_delay(int attempt_count) {
    if (attempt_count <= 1) {
        return;
    }

    int delay_seconds = 1 << (attempt_count - 2);  // 2^(n-2)
    if (delay_seconds > 10) {
        delay_seconds = 10;  // Maximum 10 seconds
    }

    printf("Please wait %d seconds...\n", delay_seconds);
    sleep(delay_seconds);
}

/**
 * Secure login function
 */
int secure_login() {
    char username[50];
    char password[50];
    int user_id = 0;  // Simple example

    printf("=== Secure Login System ===\n");

    printf("Username: ");
    scanf("%49s", username);

    // Check account lockout
    if (is_account_locked(username, user_id)) {
        return 0;
    }

    // Allow only up to maximum attempt count
    int remaining_attempts = MAX_ATTEMPTS - login_attempts[user_id].attempt_count;

    for (int i = 0; i < remaining_attempts; i++) {
        printf("Password: ");
        scanf("%49s", password);

        // Authentication verification
        if (strcmp(username, "admin") == 0 && strcmp(password, "admin123") == 0) {
            printf("Login successful!\n");
            reset_login_attempts(user_id);
            return 1;
        } else {
            increment_login_attempts(user_id);

            int attempts_left = MAX_ATTEMPTS - login_attempts[user_id].attempt_count;

            if (attempts_left > 0) {
                printf("Invalid credentials. %d attempts remaining.\n", attempts_left);
                apply_progressive_delay(login_attempts[user_id].attempt_count);
            } else {
                printf("Maximum attempts exceeded. Account locked.\n");
                return 0;
            }
        }
    }

    return 0;
}

int main() {
    secure_login();
    return 0;
}
```

## Security Best Practices

### 1. Defense-in-Depth Strategy

```
Account Level Controls:
- Attempt limiting (5 attempts)
- Temporary account lockout (5-15 minutes)
- Progressive delay (1s, 2s, 4s, 8s...)

IP Level Controls:
- Rate limiting (per-minute request limit)
- Distributed attack detection
- IP blocking (automatic/manual)

Application Level Controls:
- CAPTCHA (reCAPTCHA v3)
- 2FA/MFA enforcement
- Device fingerprinting

Network Level Controls:
- WAF (Web Application Firewall)
- DDoS protection
- Geo-blocking
```

### 2. Recommended Settings

```
Login Attempts:
- Maximum attempts: 3-5
- Lockout duration: 5-15 minutes
- Progressive delay: 2^n seconds (max 10 seconds)

OTP Verification:
- Maximum attempts: 3
- Validity period: 3-5 minutes
- Resend cooldown: 1 minute

Password Reset:
- Maximum attempts: 3
- Token validity: 1 hour
- Request limit: 3 per hour

API Authentication:
- Rate limiting: 60 per minute
- Burst: 10 per second
- IP block: When exceeding 1000 per hour
```

### 3. Logging and Monitoring

```java
// Security event logging
private void logAuthenticationEvent(String event, String username, String ip, boolean success) {
    SecurityEvent securityEvent = new SecurityEvent();
    securityEvent.setEventType(event);
    securityEvent.setUsername(username);
    securityEvent.setSourceIP(ip);
    securityEvent.setSuccess(success);
    securityEvent.setTimestamp(System.currentTimeMillis());

    // Send to SIEM
    sendToSIEM(securityEvent);

    // Real-time alerting (when threshold exceeded)
    if (isThresholdExceeded(username, ip)) {
        sendAlert("Possible brute force attack detected", username, ip);
    }
}
```

## Detection and Prevention

### Static Analysis

```bash
# SonarQube Rules
- java:S2976: Infinite loop detection
- java:S2589: Unnecessary conditional statements

# Custom Rules
# Detect while (result == FAIL) patterns
# Verify attempt limiting logic
```

### Code Review Checklist

```
[] Is login attempt limiting implemented?
[] Is there an account lockout mechanism?
[] Is IP-based rate limiting applied?
[] Is progressive delay applied on failure?
[] Is CAPTCHA integrated?
[] Are authentication failure events logged?
[] Are there no infinite loops or unlimited retry logic?
[] Are OTP attempt counts limited?
```

## Testing Methods

### Unit Tests

```java
@Test
void testAccountLockoutAfterMaxAttempts() {
    SecureLoginService service = new SecureLoginService();

    // 5 consecutive failures
    for (int i = 0; i < 5; i++) {
        assertFalse(service.login("testuser", "wrongpassword"));
    }

    // 6th attempt should be locked
    assertThrows(AccountLockedException.class, () -> {
        service.login("testuser", "correctpassword");
    });
}

@Test
void testProgressiveDelay() {
    long[] delays = new long[5];

    for (int i = 0; i < 5; i++) {
        long start = System.currentTimeMillis();
        service.login("user", "wrong");
        long end = System.currentTimeMillis();
        delays[i] = end - start;
    }

    // Verify delay time increases progressively
    assertTrue(delays[1] > delays[0]);
    assertTrue(delays[2] > delays[1]);
}

@Test
void testOTPMaxAttempts() {
    OTPService otp = new OTPService();
    otp.generate("user123");

    // 3 failures
    assertFalse(otp.verify("user123", "000000"));
    assertFalse(otp.verify("user123", "111111"));
    assertFalse(otp.verify("user123", "222222"));

    // 4th attempt - OTP has been invalidated
    assertFalse(otp.verify("user123", "correctOTP"));
}
```

### Security Tests

```bash
# Brute force testing with Burp Suite Intruder
# Verify attempt limiting works

# Automated attack testing with Hydra
hydra -l admin -P passwords.txt http-post-form "/login:username=^USER^&password=^PASS^:Invalid"

# Rate limiting testing with OWASP ZAP
# 100 requests per second -> verify blocking
```

## Related Vulnerabilities

- **CWE-307**: Improper Restriction of Excessive Authentication Attempts
- **CWE-798**: Use of Hard-coded Credentials
- **CWE-640**: Weak Password Recovery Mechanism for Forgotten Password
- **CWE-770**: Allocation of Resources Without Limits or Throttling

## References

### Standards and Guides
- OWASP Authentication Cheat Sheet
- NIST SP 800-63B: Digital Identity Guidelines
- CWE-307: https://cwe.mitre.org/data/definitions/307.html

### Tools
- Google reCAPTCHA v3
- Fail2Ban: Log-based IP blocking
- Redis: Rate limiting implementation

## Checklist

### Development Phase
- [ ] Per-account attempt limiting implemented
- [ ] Account lockout mechanism implemented
- [ ] IP-based rate limiting
- [ ] Progressive delay applied
- [ ] CAPTCHA integration
- [ ] Authentication failure logging

### Testing Phase
- [ ] Maximum attempt count testing
- [ ] Account lockout testing
- [ ] Lockout release timing testing
- [ ] Rate limiting testing
- [ ] Brute force attack simulation

### Monitoring
- [ ] Failed login attempt monitoring
- [ ] Account lockout event alerting
- [ ] Distributed attack pattern detection
- [ ] SIEM integration

---

**Last Updated**: 2025-02-05
