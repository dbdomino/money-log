# LDAP Injection (CWE-90)

**Severity**: 🟠 HIGH
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

LDAP Injection occurs when user input is used to construct LDAP (Lightweight Directory Access Protocol) queries without proper validation or sanitization. If a web application fails to properly handle user-provided input, an attacker can alter the structure of LDAP statements, causing the process to operate with the same permissions as the component executing the command. This is similar to SQL injection -- attackers can manipulate LDAP queries to bypass authentication, access unauthorized data, or modify directory information.

### Impact

When external input values are used as part of LDAP query statements or results without proper processing, an attacker can freely modify the contents of the LDAP query when it is executed.

**Potential consequences:**
- Authentication bypass
- Unauthorized access to directory information
- Information disclosure (user data, email addresses, etc.)
- Privilege escalation
- Data modification in LDAP directory

---

## Security Measures

### Key Principles

Remove special characters from user input values used in DN (Distinguished Name) and filters. If special characters must be used, process them so that characters such as `= + < > # ; \` are recognized as literal characters rather than executable commands.

**Primary Defenses:**

1. **Input Validation**
   - Whitelist allowed characters for DN and filter
   - Reject special LDAP characters: `* ( ) \ / & | ! =`
   - Validate input format and length

2. **Escape LDAP Special Characters**
   - Escape DN special characters: `, \ # + < > ; " =`
   - Escape filter special characters: `* ( ) \ NUL`
   - Use proper encoding functions

3. **Use Safe APIs**
   - Use parameterized LDAP queries if available
   - Framework-provided safe LDAP functions
   - Input validation libraries

4. **Least Privilege**
   - Limit LDAP bind account permissions
   - Use read-only accounts when possible
   - Restrict access to sensitive directory attributes

---

## Code Examples

### Attack Scenario

If the values `*` are passed for the userSN and userPassword variables, the filter string becomes `(&(sn=*)(userPassword=*))` which always evaluates to true, potentially causing unintended application behavior.

**Normal LDAP Filter:**
```ldap
(&(sn=John)(userPassword=secret123))
```

**Attack 1: Authentication Bypass**
```ldap
# Input: sn=* userPassword=*
(&(sn=*)(userPassword=*))
# Returns all users (always true)
```

**Attack 2: OR Injection**
```ldap
# Input: sn=*)(|(sn=*
(&(sn=*)(|(sn=*)(userPassword=anything))
# Bypasses password check
```

---

### ❌ Vulnerable Code

#### Java - LDAP Query with User Input

```java
private void searchRecord(String userSN, String userPassword) throws
NamingException {
    Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY,
"com.sun.jndi.ldap.LdapCtxFactory");
    try {
        DirContext dctx = new InitialDirContext(env);
        SearchControls sc = new SearchControls();
        String[] attributeFilter = {"cn", "mail"};
        sc.setReturningAttributes(attributeFilter);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String base = "dc=example,dc=com";

        // No validation for attack strings that could manipulate the LDAP filter
        // in userSN and userPassword values - unsafe
        String filter = "(&(sn=" + userSN + ")(userPassword=" + userPassword + "))";

        NamingEnumeration<?> results = dctx.search(base, filter, sc);
        while (results.hasMore()) {
            SearchResult sr = (SearchResult) results.next();
            Attributes attrs = sr.getAttributes();
            Attribute attr = attrs.get("cn");
            .....
        }
        dctx.close();
    } catch (NamingException e) { ... }
}
```

**Problems:**
1. User input (`userSN`, `userPassword`) directly concatenated
2. No validation of LDAP special characters
3. No escaping of filter metacharacters
4. Vulnerable to LDAP injection attacks

**Attack Examples:**
```java
// Normal: searchRecord("Smith", "password123")
// Filter: (&(sn=Smith)(userPassword=password123))

// Attack 1: searchRecord("*", "*")
// Filter: (&(sn=*)(userPassword=*))
// Result: Returns ALL users

// Attack 2: searchRecord("*)(|(sn=*", "anything")
// Filter: (&(sn=*)(|(sn=*)(userPassword=anything))
// Result: Bypasses authentication
```

---

### ✅ Secure Code

#### Java - Input Validation with Whitelist

```java
private void searchRecord(String userSN, String userPassword) throws
NamingException {
    Hashtable<String, String> env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY,
"com.sun.jndi.ldap.LdapCtxFactory");
    try {
        DirContext dctx = new InitialDirContext(env);
        SearchControls sc = new SearchControls();
        String[] attributeFilter = {"cn", "mail"};
        sc.setReturningAttributes(attributeFilter);
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String base = "dc=example,dc=com";

        // Remove strings that could manipulate the LDAP filter from userSN and userPassword values
        if (!userSN.matches("[\\W\\w\\W\\s]*") || !userPassword.matches("[\\W\\w]*")) {
            throw new IllegalArgumentException("Invalid input");
        }

        String filter = "(&(sn=" + userSN + ")(userPassword=" + userPassword + "))";
        NamingEnumeration<?> results = dctx.search(base, filter, sc);
        while (results.hasMore()) {
            SearchResult sr = (SearchResult) results.next();
            Attributes attrs = sr.getAttributes();
            Attribute attr = attrs.get("cn");
            ......
        }
        dctx.close();
    } catch (NamingException e) { ... }
}
```

**Security Features:**
1. Input validation using regex
2. Whitelist approach for allowed characters
3. Throws exception for invalid input

---

#### ✅ Java - Best Practice with Character Escaping

```java
public class SecureLDAPQuery {

    // LDAP filter metacharacters that need escaping
    private static final String[] LDAP_FILTER_METACHARACTERS = {
        "\\", "*", "(", ")", "\u0000"
    };

    // LDAP DN metacharacters
    private static final String[] LDAP_DN_METACHARACTERS = {
        "\\", ",", "+", "\"", "<", ">", ";"
    };

    public List<SearchResult> searchUsers(String username, String password)
            throws NamingException {

        // 1. Validate input (whitelist)
        if (!isValidLDAPInput(username) || !isValidLDAPInput(password)) {
            throw new IllegalArgumentException("Invalid LDAP input");
        }

        // 2. Escape special characters
        String safeUsername = escapeLDAPFilter(username);
        String safePassword = escapeLDAPFilter(password);

        // 3. Build filter with escaped values
        String filter = String.format(
            "(&(uid=%s)(userPassword=%s))",
            safeUsername,
            safePassword
        );

        // 4. Execute search
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:389");

        DirContext ctx = new InitialDirContext(env);
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results =
            ctx.search("dc=example,dc=com", filter, sc);

        List<SearchResult> resultList = new ArrayList<>();
        while (results.hasMore()) {
            resultList.add(results.next());
        }

        ctx.close();
        return resultList;
    }

    private boolean isValidLDAPInput(String input) {
        // Whitelist: only alphanumeric, dash, underscore
        return input != null && input.matches("^[a-zA-Z0-9_-]+$");
    }

    private String escapeLDAPFilter(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder escaped = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '\\':
                    escaped.append("\\5c");
                    break;
                case '*':
                    escaped.append("\\2a");
                    break;
                case '(':
                    escaped.append("\\28");
                    break;
                case ')':
                    escaped.append("\\29");
                    break;
                case '\u0000':
                    escaped.append("\\00");
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private String escapeLDAPDN(String input) {
        if (input == null) {
            return null;
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // Leading or trailing space
            if ((i == 0 || i == input.length() - 1) && c == ' ') {
                escaped.append("\\ ");
                continue;
            }

            switch (c) {
                case '\\':
                case ',':
                case '+':
                case '"':
                case '<':
                case '>':
                case ';':
                case '=':
                case '#':
                    escaped.append('\\').append(c);
                    break;
                default:
                    escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
```

**Security Features:**
1. Input validation (whitelist alphanumeric)
2. Escape LDAP filter metacharacters
3. Escape LDAP DN metacharacters
4. Separate escaping for filter vs DN
5. Handle null bytes and special cases

---

### C# Example

#### ❌ Vulnerable C#

```csharp
static void SearchRecord(string userSN, string userPW)
{
    try {
        DirectoryEntry oDE;
        oDE = new DirectoryEntry(GetStrPath());
        // Executes LDAP query with unauthenticated external binding
        foreach(DirectoryEntry objChildDE om oDE.Children) {
            ...
        }
    } catch (NamingException e) { ... }
}
```

#### ✅ Secure C# with Input Filtering

```csharp
void LDAPInjection() {
    char *filter = getenv("Filter");
    int error_code; int i;
    LDAP *ld = NULL;
    LDAPMessage *result;
    // Using external filter without validation
    error_code = ldap_search_ext_s(ld, FIND_DN, LDAP_SCOPE_BASE, filter,
        NULL, 0, NULL, NULL, LDAP_NO_LIMIT, LDAP_NO_LIMIT, &result);
}
```

**Secure version:**
```csharp
char *filter = getenv("Filter");
int error_code; int i;
LDAP *ld = NULL;
LDAPMessage *result;
// Use a fixed value for the username to retrieve information
for(i = 0; *(filter + i) != 0; i++) {
    // Check for potentially dangerous attack strings
    switch(*(filter + i)) {
        case '*':
        case '(':
        case ')':
            ...
            return;
    }
}
error_code = ldap_search_ext_s(ld, FIND_DN, LDAP_SCOPE_BASE, filter,
    NULL, 0, NULL, NULL, LDAP_NO_LIMIT, LDAP_NO_LIMIT, &result);
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-90: LDAP Injection**
   MITRE, http://cwe.mitre.org/data/definitions/90.html

### CERT

② **Prevent LDAP injection**
   CERT, http://www.securecoding.cert.org/confluence/display/java/IDS54-J.+Prevent+LDAP+injection

### OWASP

③ **LDAP injection**
   OWASP, https://www.owasp.org/index.php/LDAP_Injection_Prevention_Cheat_Sheet

### Other

④ **"LDAP Resources"**
   http://ldapman.org/

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find LDAP operations
grep -r "InitialDirContext" .
grep -r "LdapCtxFactory" .
grep -r "dctx.search" .
grep -r "DirectoryEntry" .

# Find potential concatenation in LDAP filters
grep -r "search.*\\+.*request" .
grep -r "filter.*=.*\\+.*getParameter" .

# Find ldap_search in C
grep -r "ldap_search" *.c
grep -r "ldap_search_ext" *.c
```

---

## ✅ Security Checklist

- [ ] All LDAP inputs validated (whitelist)
- [ ] LDAP filter metacharacters escaped: `* ( ) \ NUL`
- [ ] LDAP DN metacharacters escaped: `, \ # + < > ; " =`
- [ ] No user input directly concatenated into filters
- [ ] Input length limits enforced
- [ ] LDAP bind account uses least privilege
- [ ] Error messages don't leak directory structure
- [ ] LDAP injection testing completed
- [ ] Use of safe LDAP libraries

---

## 🎯 LDAP Filter Special Characters

### Characters That Need Escaping in Filters

| Character | Hex Escape | Description |
|-----------|------------|-------------|
| `*`       | `\2a`      | Wildcard |
| `(`       | `\28`      | Left parenthesis |
| `)`       | `\29`      | Right parenthesis |
| `\`       | `\5c`      | Backslash |
| `NUL`     | `\00`      | Null character |

### Characters That Need Escaping in DNs

| Character | Escape | Description |
|-----------|--------|-------------|
| `,`       | `\,`   | Comma |
| `\`       | `\\`   | Backslash |
| `#`       | `\#`   | Hash |
| `+`       | `\+`   | Plus |
| `<`       | `\<`   | Less than |
| `>`       | `\>`   | Greater than |
| `;`       | `\;`   | Semicolon |
| `"`       | `\"`   | Quote |
| `=`       | `\=`   | Equals |

---

## 💡 Attack Examples

### Authentication Bypass

```
# Normal filter
(&(uid=john)(userPassword=secret))

# Attack: uid=*
(&(uid=*)(userPassword=anything))
# Returns all users

# Attack: uid=admin)(|(uid=*
(&(uid=admin)(|(uid=*)(userPassword=ignored))
# Always true, bypasses password
```

### Information Disclosure

```
# Extract all email addresses
uid=*)(&(objectClass=*)(mail=*

# Results in:
(&(uid=*)(&(objectClass=*)(mail=*)(userPassword=ignored))
```

### Blind LDAP Injection

```
# Test if user exists
uid=admin*

# If returns results, user exists
# Can enumerate users character by character
uid=adm*
uid=admin*
```

---

## 🚨 Common Mistakes

1. **Only Filtering Some Characters**
   ```java
   // DON'T: Incomplete filtering
   input = input.replace("*", ""); // Still vulnerable to ( ) \

   // DO: Filter all LDAP metacharacters
   input = escapeLDAPFilter(input);
   ```

2. **Wrong Context Escaping**
   ```java
   // DON'T: Use filter escaping for DN
   String dn = "cn=" + escapeLDAPFilter(userName) + ",dc=example,dc=com";

   // DO: Use DN escaping for DN
   String dn = "cn=" + escapeLDAPDN(userName) + ",dc=example,dc=com";
   ```

3. **Trusting Internal Data**
   ```java
   // DON'T: Assume database data is safe
   String filter = "uid=" + dbUsername; // DB could be compromised

   // DO: Validate and escape all data
   String filter = "uid=" + escapeLDAPFilter(dbUsername);
   ```

---

## 💡 Best Practices Summary

1. **Whitelist validation** - Only allow alphanumeric and safe characters
2. **Escape metacharacters** - Use proper escaping for filter vs DN
3. **Never concatenate** - Don't build filters with string concatenation
4. **Least privilege** - Limit LDAP account permissions
5. **Use frameworks** - Leverage secure LDAP libraries
6. **Test thoroughly** - Include LDAP injection in security testing

---

**Always escape LDAP metacharacters and validate input!**
