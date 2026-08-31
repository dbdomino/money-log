# Weak Password Requirements (CWE-521)

**Severity**: 🔴 HIGH
**Category**: Security Features
**OWASP Top 10**: A07:2021 – Identification and Authentication Failures

---

## Overview

### Attack Description

Weak password requirements allow users to create simple, easily guessable passwords like "password123", "admin", or "12345678". Without enforcing complexity rules (length, special characters, numbers, etc.), systems become vulnerable to various password attacks including brute force, dictionary attacks, and credential stuffing.

### Impact

**Potential consequences:**
- Unauthorized account access
- Account takeover attacks
- Brute force attacks succeed quickly
- Dictionary attacks compromise accounts
- Credential stuffing attacks
- Lateral movement after initial compromise
- Data breach through compromised accounts
- Identity theft
- Compliance violations (PCI-DSS, GDPR, etc.)
- Reputation damage

---

## Security Measures

### Key Principles

Passwords must meet a minimum length requirement and enforce complexity by combining uppercase letters, lowercase letters, numbers, and special characters. Additionally, common passwords, dictionary words, and passwords based on user information must be blocked.

**Primary Defenses:**

1. **Password Complexity Requirements**
   - Minimum length: 8-10 characters (12+ for high-security systems)
   - Mix of character types: uppercase, lowercase, numbers, special characters
   - Not based on username or common words
   - Not sequential or repetitive patterns

2. **Password Validation**
   - Regular expression pattern matching
   - Check against common password lists (e.g., Have I Been Pwned)
   - Reject dictionary words
   - Reject personal information (name, birthday)
   - Reject previously breached passwords

3. **Additional Security Measures**
   - Multi-factor authentication (MFA)
   - Account lockout after failed attempts
   - Password expiration policies (with caution)
   - Password history to prevent reuse
   - Secure password storage (bcrypt, scrypt, Argon2)

4. **User Guidance**
   - Clear password requirements displayed
   - Password strength meter
   - Suggestions for strong passwords
   - Education on password security

5. **Technical Controls**
   - Rate limiting on login attempts
   - CAPTCHA after multiple failures
   - Anomaly detection for unusual login patterns
   - Logging and monitoring of authentication events

---

## Code Examples

### Attack Scenario

An attacker exploits weak password policies to perform the following attacks.

**Attack Sequence:**
```bash
# Attacker tries common passwords
1. admin/admin
2. admin/password
3. admin/123456
4. admin/admin123

# Without password complexity requirements, one of these likely works
# With weak passwords, brute force takes minutes instead of years
```

**Dictionary Attack Example:**
```python
# Attacker uses common password list
passwords = ['password', '123456', 'admin', 'letmein', 'welcome']
for pwd in passwords:
    if try_login(username, pwd):
        print(f"Account compromised with password: {pwd}")
        break
```

---

### ❌ Vulnerable Code

#### Java - No Password Validation

```java
public class UserRegistration {

    /**
     * Vulnerable code with no password validation
     */
    public boolean registerUser(String username, String password) {
        // No password complexity check - any password is accepted
        if (password == null || password.isEmpty()) {
            return false;
        }

        // Register user
        User user = new User(username, password);
        userRepository.save(user);
        return true;
    }
}
```

**Problems:**
1. **No complexity requirements** - Accepts any non-empty password
2. No length validation
3. No character type requirements
4. Allows "123", "password", "admin"
5. Vulnerable to brute force attacks
6. Vulnerable to dictionary attacks

---

#### Java - Minimal Length Check Only

```java
public class WeakPasswordValidator {

    /**
     * Insufficient validation that only checks length
     */
    public boolean isValidPassword(String password) {
        // Only checks minimum length without verifying complexity
        if (password == null || password.length() < 6) {
            return false;
        }
        return true;
    }
}
```

**Problems:**
1. **Only checks length** - No complexity requirements
2. Too short minimum (6 characters)
3. Allows "aaaaaa" or "123456"
4. No uppercase/lowercase/special character requirements
5. Still vulnerable to attacks

---

#### C# - Insufficient Validation

```csharp
public class AccountManager
{
    // No password complexity requirements
    public bool CreateAccount(string username, string password)
    {
        // Only blocks empty passwords
        if (string.IsNullOrWhiteSpace(password))
        {
            return false;
        }

        // Any password is accepted
        var user = new User
        {
            Username = username,
            Password = HashPassword(password)
        };

        database.SaveUser(user);
        return true;
    }
}
```

**Problems:**
1. No password complexity validation
2. Accepts single character passwords
3. No character type requirements
4. Vulnerable to weak passwords

---

#### C - No Password Policy

```c
#include <string.h>
#include <stdio.h>

int create_user(char *username, char *password) {
    // No password validation
    if (password == NULL || strlen(password) == 0) {
        return -1;
    }

    // Any password is accepted
    // Save to database
    save_user_to_db(username, password);
    return 0;
}
```

**Problems:**
1. No password validation
2. No complexity requirements
3. No length requirements beyond empty check
4. Accepts any password

---

### ✅ Secure Code

#### Java - Comprehensive Password Validation

```java
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashSet;
import java.util.Set;

/**
 * Strong password complexity validation
 */
public class SecurePasswordValidator {

    // Password complexity requirements
    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 128;

    // Regex pattern - at least 1 uppercase, lowercase, digit, and special character
    private static final String PASSWORD_PATTERN =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()])[A-Za-z\\d@$!%*?&#^()]{10,}$";

    private static final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

    // List of commonly used weak passwords
    private static final Set<String> COMMON_PASSWORDS = new HashSet<>();

    static {
        COMMON_PASSWORDS.add("password");
        COMMON_PASSWORDS.add("password123");
        COMMON_PASSWORDS.add("admin");
        COMMON_PASSWORDS.add("admin123");
        COMMON_PASSWORDS.add("12345678");
        COMMON_PASSWORDS.add("qwerty");
        COMMON_PASSWORDS.add("letmein");
        COMMON_PASSWORDS.add("welcome");
        // In practice, a much larger common password list should be included
    }

    /**
     * Password complexity validation
     *
     * @param password the password to validate
     * @return the validation result
     */
    public PasswordValidationResult validatePassword(String password, String username) {
        PasswordValidationResult result = new PasswordValidationResult();

        // 1. Null or empty string check
        if (password == null || password.isEmpty()) {
            result.setValid(false);
            result.addError("Password cannot be empty.");
            return result;
        }

        // 2. Length validation
        if (password.length() < MIN_LENGTH) {
            result.setValid(false);
            result.addError(String.format(
                "Password must be at least %d characters long.", MIN_LENGTH));
        }

        if (password.length() > MAX_LENGTH) {
            result.setValid(false);
            result.addError(String.format(
                "Password must not exceed %d characters.", MAX_LENGTH));
        }

        // 3. Complexity validation - regex pattern matching
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches()) {
            result.setValid(false);

            // Detailed error messages
            if (!password.matches(".*[a-z].*")) {
                result.addError("At least 1 lowercase letter is required.");
            }
            if (!password.matches(".*[A-Z].*")) {
                result.addError("At least 1 uppercase letter is required.");
            }
            if (!password.matches(".*\\d.*")) {
                result.addError("At least 1 digit is required.");
            }
            if (!password.matches(".*[@$!%*?&#^()].*")) {
                result.addError("At least 1 special character (@$!%*?&#^()) is required.");
            }
        }

        // 4. Common password check
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            result.setValid(false);
            result.addError("Commonly used passwords are not allowed.");
        }

        // 5. Username inclusion check
        if (username != null && !username.isEmpty()) {
            if (password.toLowerCase().contains(username.toLowerCase())) {
                result.setValid(false);
                result.addError("Password must not contain the username.");
            }
        }

        // 6. Sequential character check (e.g., "12345", "abcde")
        if (hasSequentialCharacters(password)) {
            result.setValid(false);
            result.addError("Sequential character sequences are not allowed.");
        }

        // 7. Repeating character check (e.g., "aaaa", "1111")
        if (hasRepeatingCharacters(password)) {
            result.setValid(false);
            result.addError("The same character cannot be used more than 4 times consecutively.");
        }

        return result;
    }

    /**
     * Sequential character check
     */
    private boolean hasSequentialCharacters(String password) {
        int sequentialCount = 0;
        for (int i = 0; i < password.length() - 1; i++) {
            if (password.charAt(i) + 1 == password.charAt(i + 1)) {
                sequentialCount++;
                if (sequentialCount >= 3) {
                    return true;
                }
            } else {
                sequentialCount = 0;
            }
        }
        return false;
    }

    /**
     * Repeating character check
     */
    private boolean hasRepeatingCharacters(String password) {
        int repeatCount = 1;
        for (int i = 0; i < password.length() - 1; i++) {
            if (password.charAt(i) == password.charAt(i + 1)) {
                repeatCount++;
                if (repeatCount >= 4) {
                    return true;
                }
            } else {
                repeatCount = 1;
            }
        }
        return false;
    }

    /**
     * Password strength calculation
     */
    public PasswordStrength calculateStrength(String password) {
        int score = 0;

        // Length score
        if (password.length() >= 10) score += 2;
        if (password.length() >= 12) score += 1;
        if (password.length() >= 16) score += 1;

        // Complexity score
        if (password.matches(".*[a-z].*")) score += 1;
        if (password.matches(".*[A-Z].*")) score += 1;
        if (password.matches(".*\\d.*")) score += 1;
        if (password.matches(".*[@$!%*?&#^()].*")) score += 2;

        // Diversity score
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        if (uniqueChars.size() >= 8) score += 1;

        // Strength determination
        if (score <= 4) return PasswordStrength.WEAK;
        if (score <= 6) return PasswordStrength.MEDIUM;
        if (score <= 8) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }
}

/**
 * Password validation result class
 */
class PasswordValidationResult {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.valid = false;
    }

    public List<String> getErrors() {
        return errors;
    }
}

/**
 * Password strength enum
 */
enum PasswordStrength {
    WEAK, MEDIUM, STRONG, VERY_STRONG
}
```

**Security Features:**
1. **Minimum length enforcement** - 10+ characters required
2. **Complexity requirements** - Uppercase, lowercase, numbers, special chars
3. **Common password blocking** - Rejects well-known weak passwords
4. **Username check** - Prevents password containing username
5. **Sequential character detection** - Blocks "12345", "abcde"
6. **Repeating character detection** - Blocks "aaaa", "1111"
7. **Password strength calculation** - Provides feedback to users
8. **Detailed error messages** - Helps users create strong passwords

---

#### Java - Registration with Password Validation

```java
import java.util.logging.Logger;

/**
 * User registration with password validation
 */
public class SecureUserRegistration {

    private static final Logger logger = Logger.getLogger(
        SecureUserRegistration.class.getName()
    );

    private final SecurePasswordValidator passwordValidator;
    private final PasswordHistoryService passwordHistory;

    public SecureUserRegistration() {
        this.passwordValidator = new SecurePasswordValidator();
        this.passwordHistory = new PasswordHistoryService();
    }

    /**
     * Secure user registration
     */
    public RegistrationResult registerUser(String username, String password,
                                           String email) {
        RegistrationResult result = new RegistrationResult();

        // 1. Password complexity validation
        PasswordValidationResult validation =
            passwordValidator.validatePassword(password, username);

        if (!validation.isValid()) {
            result.setSuccess(false);
            result.setErrors(validation.getErrors());
            logger.warning(String.format(
                "Password validation failed for user: %s", username));
            return result;
        }

        // 2. Password strength check
        PasswordStrength strength = passwordValidator.calculateStrength(password);
        if (strength == PasswordStrength.WEAK) {
            result.setSuccess(false);
            result.addError("Password is too weak. Please use a stronger password.");
            return result;
        }

        // 3. Breached password check (e.g., Have I Been Pwned API)
        if (isPasswordBreached(password)) {
            result.setSuccess(false);
            result.addError(
                "This password has been previously exposed in a data breach. Please use a different password.");
            return result;
        }

        // 4. Store password with a secure hashing algorithm
        String hashedPassword = hashPasswordSecurely(password);

        // 5. Create user
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hashedPassword);
        user.setEmail(email);
        user.setPasswordStrength(strength);
        user.setPasswordCreatedDate(LocalDateTime.now());

        // 6. Save to database
        userRepository.save(user);

        // 7. Record password history
        passwordHistory.recordPassword(username, hashedPassword);

        result.setSuccess(true);
        logger.info(String.format("User registered successfully: %s", username));

        return result;
    }

    /**
     * Secure password hashing using bcrypt
     */
    private String hashPasswordSecurely(String password) {
        // bcrypt with cost factor 12
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Breached password check (example)
     */
    private boolean isPasswordBreached(String password) {
        // Check against Have I Been Pwned API or internal breached password DB
        // Actual implementation requires API call or DB query
        return false;
    }
}
```

**Security Features:**
1. Comprehensive password validation
2. Password strength check
3. Breached password detection
4. Secure password hashing (bcrypt)
5. Password history tracking
6. Detailed logging
7. User feedback on validation failures

---

#### C# - Password Complexity Validation

```csharp
using System;
using System.Text.RegularExpressions;
using System.Linq;

/// <summary>
/// Password complexity validation
/// </summary>
public class PasswordComplexityValidator
{
    private const int MinLength = 10;
    private const int MaxLength = 128;

    // Complexity regex - at least 1 uppercase, lowercase, digit, and special character
    private static readonly Regex ComplexityPattern = new Regex(
        @"^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()])[A-Za-z\d@$!%*?&#^()]{10,}$"
    );

    private static readonly string[] CommonPasswords = {
        "password", "password123", "admin", "admin123",
        "12345678", "qwerty", "letmein", "welcome"
    };

    /// <summary>
    /// Password complexity validation
    /// </summary>
    public ValidationResult ValidatePassword(string password, string username = null)
    {
        var result = new ValidationResult { IsValid = true };

        // 1. Null or empty string check
        if (string.IsNullOrEmpty(password))
        {
            result.IsValid = false;
            result.AddError("Password cannot be empty.");
            return result;
        }

        // 2. Length validation
        if (password.Length < MinLength)
        {
            result.IsValid = false;
            result.AddError($"Password must be at least {MinLength} characters long.");
        }

        if (password.Length > MaxLength)
        {
            result.IsValid = false;
            result.AddError($"Password must not exceed {MaxLength} characters.");
        }

        // 3. Complexity validation
        if (!ComplexityPattern.IsMatch(password))
        {
            result.IsValid = false;

            if (!password.Any(char.IsLower))
                result.AddError("At least 1 lowercase letter is required.");

            if (!password.Any(char.IsUpper))
                result.AddError("At least 1 uppercase letter is required.");

            if (!password.Any(char.IsDigit))
                result.AddError("At least 1 digit is required.");

            if (!password.Any(c => "@$!%*?&#^()".Contains(c)))
                result.AddError("At least 1 special character (@$!%*?&#^()) is required.");
        }

        // 4. Common password check
        if (CommonPasswords.Contains(password.ToLower()))
        {
            result.IsValid = false;
            result.AddError("Commonly used passwords are not allowed.");
        }

        // 5. Username inclusion check
        if (!string.IsNullOrEmpty(username) &&
            password.ToLower().Contains(username.ToLower()))
        {
            result.IsValid = false;
            result.AddError("Password must not contain the username.");
        }

        return result;
    }
}

public class ValidationResult
{
    public bool IsValid { get; set; }
    public List<string> Errors { get; } = new List<string>();

    public void AddError(string error)
    {
        Errors.Add(error);
        IsValid = false;
    }
}
```

**Security Features:**
1. Minimum 10 character length
2. Uppercase, lowercase, number, special character requirements
3. Common password blocking
4. Username inclusion check
5. Detailed validation messages

---

#### C - Password Complexity Check

```c
#include <stdio.h>
#include <string.h>
#include <ctype.h>
#include <stdbool.h>

#define MIN_PASSWORD_LENGTH 10
#define MAX_PASSWORD_LENGTH 128

/**
 * Password complexity validation
 */
bool validate_password_complexity(const char *password, const char *username) {
    int length = strlen(password);
    bool has_lower = false;
    bool has_upper = false;
    bool has_digit = false;
    bool has_special = false;

    // 1. Length check
    if (length < MIN_PASSWORD_LENGTH) {
        fprintf(stderr, "Password must be at least %d characters long.\n",
                MIN_PASSWORD_LENGTH);
        return false;
    }

    if (length > MAX_PASSWORD_LENGTH) {
        fprintf(stderr, "Password must not exceed %d characters.\n",
                MAX_PASSWORD_LENGTH);
        return false;
    }

    // 2. Complexity check
    for (int i = 0; i < length; i++) {
        if (islower(password[i])) has_lower = true;
        if (isupper(password[i])) has_upper = true;
        if (isdigit(password[i])) has_digit = true;
        if (strchr("@$!%*?&#^()", password[i])) has_special = true;
    }

    if (!has_lower) {
        fprintf(stderr, "At least 1 lowercase letter is required.\n");
        return false;
    }

    if (!has_upper) {
        fprintf(stderr, "At least 1 uppercase letter is required.\n");
        return false;
    }

    if (!has_digit) {
        fprintf(stderr, "At least 1 digit is required.\n");
        return false;
    }

    if (!has_special) {
        fprintf(stderr, "At least 1 special character is required.\n");
        return false;
    }

    // 3. Username inclusion check
    if (username != NULL && strstr(password, username) != NULL) {
        fprintf(stderr, "Password must not contain the username.\n");
        return false;
    }

    // 4. Common password check
    const char *common_passwords[] = {
        "password", "password123", "admin", "12345678", NULL
    };

    for (int i = 0; common_passwords[i] != NULL; i++) {
        if (strcasecmp(password, common_passwords[i]) == 0) {
            fprintf(stderr, "Commonly used passwords are not allowed.\n");
            return false;
        }
    }

    return true;
}

/**
 * Secure user creation
 */
int create_user_secure(const char *username, const char *password) {
    // Password complexity validation
    if (!validate_password_complexity(password, username)) {
        return -1;
    }

    // Password hashing (use bcrypt or scrypt)
    char hashed_password[128];
    hash_password(password, hashed_password, sizeof(hashed_password));

    // Save to database
    save_user_to_db(username, hashed_password);

    return 0;
}
```

**Security Features:**
1. Length validation (10-128 characters)
2. Character type requirements (uppercase, lowercase, digit, special)
3. Username inclusion check
4. Common password blocking
5. Secure password hashing

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-521: Weak Password Requirements**
   MITRE, https://cwe.mitre.org/data/definitions/521.html

② **CWE-262: Not Using Password Aging**
   MITRE, https://cwe.mitre.org/data/definitions/262.html

③ **CWE-916: Use of Password Hash With Insufficient Computational Effort**
   MITRE, https://cwe.mitre.org/data/definitions/916.html

### OWASP

④ **OWASP Authentication Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html

⑤ **OWASP Password Storage Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html

### NIST Guidelines

⑥ **NIST SP 800-63B: Digital Identity Guidelines**
   NIST, https://pages.nist.gov/800-63-3/sp800-63b.html

### Industry Standards

⑦ **PCI-DSS Password Requirements**
   PCI Security Standards Council

⑧ **Have I Been Pwned - Breached Password Database**
   https://haveibeenpwned.com/Passwords

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find password validation without complexity checks
grep -r "password.*length" --include="*.java" . | grep -v "Pattern\|regex\|complexity"

# Find minimal password validation
grep -r "password.*isEmpty\|password.*null" --include="*.java" .

# Find password creation without validation
grep -r "setPassword\|createUser" --include="*.java" . | grep -v "validate"

# Check for hardcoded minimum lengths
grep -r "password.*length.*<.*[0-8]" --include="*.java" .

# Find password fields without validation
grep -r "@Password\|private.*password" --include="*.java" .
```

---

## ✅ Security Checklist

- [ ] Minimum password length enforced (10+ characters)
- [ ] Maximum password length defined (128 characters recommended)
- [ ] Uppercase letter requirement
- [ ] Lowercase letter requirement
- [ ] Number requirement
- [ ] Special character requirement
- [ ] Common password blocking implemented
- [ ] Dictionary word checking
- [ ] Username inclusion check
- [ ] Sequential character detection
- [ ] Repeating character detection
- [ ] Breached password checking (Have I Been Pwned)
- [ ] Password strength meter displayed to users
- [ ] Clear password requirements shown
- [ ] Secure password hashing (bcrypt/scrypt/Argon2)
- [ ] Password history tracking
- [ ] Multi-factor authentication available
- [ ] Account lockout policy implemented
- [ ] Rate limiting on login attempts

---

## 🎯 Password Complexity Best Practices

### NIST Guidelines (2023)

**Recommended:**
- Minimum 8 characters (12+ for sensitive systems)
- Maximum 64+ characters supported
- Allow all printable ASCII and Unicode
- Check against breached password lists
- No complexity requirements that reduce entropy
- No mandatory password rotation
- Allow paste functionality
- Show password when typing (optional toggle)

**Not Recommended:**
- Forced periodic password changes without reason
- Complex rules that lead to predictable patterns
- Blocking common character substitutions (@ for a)
- Rejecting passwords based on composition rules alone

### Industry Standard

**Minimum Requirements:**
- **Length**: 10-12 characters minimum
- **Uppercase**: At least 1 uppercase letter
- **Lowercase**: At least 1 lowercase letter
- **Digit**: At least 1 number
- **Special**: At least 1 special character
- **No common passwords**: Block top 10,000 common passwords
- **No personal info**: Username, name, birthday not in password

### High-Security Systems

For systems requiring high security:
- **Length**: 16+ characters minimum
- **Complexity**: Mix of all character types
- **Passphrase**: Encourage multi-word passphrases
- **MFA**: Require multi-factor authentication
- **Breached check**: Verify against known breaches
- **Advanced hashing**: Use Argon2 or scrypt

---

## 🚨 Common Mistakes

1. **Too Short Minimum Length**
   ```java
   // DON'T: 6 characters is too short
   if (password.length() >= 6) { /* valid */ }

   // DO: 10-12 characters minimum
   if (password.length() >= 10) { /* valid */ }
   ```

2. **Length Check Only**
   ```java
   // DON'T: Only checking length
   if (password.length() >= 8) {
       return true; // Still allows "aaaaaaaa"
   }

   // DO: Check complexity too
   if (password.length() >= 10 && hasComplexity(password)) {
       return true;
   }
   ```

3. **Allowing Common Passwords**
   ```java
   // DON'T: Not checking common passwords
   if (meetsComplexity(password)) {
       return true; // Still allows "Password123!"
   }

   // DO: Block common passwords
   if (meetsComplexity(password) && !isCommonPassword(password)) {
       return true;
   }
   ```

4. **Poor Error Messages**
   ```java
   // DON'T: Vague error message
   return "Invalid password";

   // DO: Specific guidance
   return "Password must be at least 10 characters and include " +
          "uppercase, lowercase, number, and special character";
   ```

---

## 💡 Best Practices Summary

1. **Enforce minimum 10-12 characters** - Longer passwords are exponentially stronger
2. **Require character diversity** - Uppercase, lowercase, numbers, special characters
3. **Block common passwords** - Use known weak password lists
4. **Check against breaches** - Verify passwords haven't been compromised
5. **Provide clear guidance** - Show requirements and strength meter
6. **Don't allow personal info** - Block username, name, birthday in passwords
7. **Use secure hashing** - bcrypt, scrypt, or Argon2 (not MD5/SHA1)
8. **Implement MFA** - Passwords alone are not enough
9. **Allow passphrases** - "correct horse battery staple" style
10. **Don't force rotation** - Only require change when compromised

---

**Protect accounts with strong password requirements!**
