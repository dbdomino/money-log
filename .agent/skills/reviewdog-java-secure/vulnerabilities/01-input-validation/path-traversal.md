# Path Traversal (CWE-22)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A01:2021 – Broken Access Control

---

## Overview

### Attack Description

Path Traversal (also known as Directory Traversal) occurs when an application uses unvalidated external input to access files and system resources. If access or identification of system resources such as files and servers is allowed using unvalidated external input values, attackers can manipulate input values to arbitrarily access resources protected by the system. Through path traversal and resource injection vulnerabilities, attackers can cause resource modification/deletion, system information disclosure, and service disruptions from resource conflicts.

### Impact

Through path traversal and resource injection, attackers can gain unauthorized permissions to modify or execute files related to system configuration.

**Potential consequences:**
- Read sensitive files (`/etc/passwd`, configuration files, source code)
- Overwrite critical system files
- Execute arbitrary files
- Information disclosure
- Privilege escalation
- Denial of service

---

## Security Measures

### Key Principles

When using external input as identifiers for resources (files, socket ports, etc.), apply proper validation or ensure that selection is made from a predefined list of acceptable values. Especially when external input is a filename, use filters that can remove characters posing path traversal risks (` / \ .. `).

**Primary Defenses:**

1. **Path Canonicalization**
   - Convert all paths to canonical (absolute) form
   - Use `File.getCanonicalPath()` in Java
   - Compare canonical path against allowed base directory

2. **Whitelist Validation**
   - Maintain whitelist of allowed files/directories
   - Only allow access to predefined safe locations
   - Validate filenames match allowed patterns

3. **Filter Dangerous Characters**
   - Remove or block `../`, `..\\`, and similar sequences
   - Filter path traversal sequences: `%2e%2e/`, `..%2f`, etc.
   - Block absolute paths if only relative paths expected

4. **Chroot/Jail Environment**
   - Use chroot jail to restrict file access
   - Configure web server document root properly
   - Use security manager in Java

---

## Code Examples

### Attack Scenario

An external input value (P) is used to set the file path for copying contents to a buffer. If an attacker passes a value like `../../rootFile.txt` for P, the contents of an unintended file are written to the buffer, adversely affecting the system.

**Example Attack:**
```
filename=../../../etc/passwd
filename=..\..\..\..\windows\system32\drivers\etc\hosts
```

---

### ❌ Vulnerable Code

#### Java - Direct File Access

```java
// Using externally received values without validation is unsafe
String fileName = request.getParameter("P");
BufferedInputStream bis = null;
BufferedOutputStream bos = null;
FileInputStream fis = null;
try {
```

**Continuation:**
```java
response.setHeader("Content-Disposition", "attachment;filename="+fileName+";");
...
// Externally received value is used in file processing without validation or processing - unsafe
fis = new FileInputStream("C:/datas/" + fileName);
bis = new BufferedInputStream(fis);
bos = new BufferedOutputStream(response.getOutputStream());
```

**Problem:**
- `fileName` from user input used directly
- No validation of `../` sequences
- Can access files outside `/datas/` directory

---

### ✅ Secure Code

#### Java - Path Canonicalization and Validation

```java
String fileName = request.getParameter("P");
BufferedInputStream bis = null;
BufferedOutputStream bos = null;
FileInputStream fis = null;
try {
    response.setHeader("Content-Disposition", "attachment;filename="+fileName+";");
    ...
    // Remove path traversal characters (/ \ & .. etc.) from externally received values before use
    fileName = filename.replaceAll("WW.", "").replaceAll("/", "").replaceAll("WWWW", "");
    fis = new FileInputStream("C:/datas/" + fileName);
    bis = new BufferedInputStream(fis);
    bos = new BufferedOutputStream(response.getOutputStream());
    int read;
    while((read = bis.read(buffer, 0, 1024)) != -1) {
        bos.write(buffer,0,read);
    }
}
```

**Better - Use Canonical Path:**
```java
public class ShowHelp {
    private final static String safeDir = "c:WWhelp_filesWW";

    public static void main(String[] args) throws IOException {
        String helpFile = args[0];

        // Check for and handle path traversal character strings
        if (helpFile != null) {
            helpFile = helpFile.replaceAll("WW. {2, }|/WWWW", "");
        }

        try (BufferedReader br = new BufferedReader(
            new FileReader(new FileReader(safeDir + helpFile)))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        }
        ...
    }
}
```

#### Best Practice - Canonical Path Validation

```java
public class ShowHelpSolution {
    private final static String safeDir = "c:WWhelp_filesWW";
    // Check for and handle path traversal character strings before use
    public static void main(String[] args) throws IOException {
        String helpFile = args[0];
        if (helpFile != null) {
            helpFile = helpFile.replaceAll("WW. {2, }|/WWWW", "");
        }
        try (BufferedReader br = new BufferedReader(
            new FileReader(new FileReader(safeDir + helpFile)))) {
        ...
```

**Even Better - Complete Validation:**
```java
public class SecureFileAccess {
    private static final String SAFE_DIR = "C:/data/files/";
    private static final File SAFE_DIR_FILE = new File(SAFE_DIR);

    public File getSecureFile(String userInput) throws IOException {
        // 1. Remove dangerous characters
        String sanitized = userInput.replaceAll("[^a-zA-Z0-9._-]", "");

        // 2. Create file object
        File requestedFile = new File(SAFE_DIR, sanitized);

        // 3. Get canonical path
        String canonicalPath = requestedFile.getCanonicalPath();
        String safeCanonicalPath = SAFE_DIR_FILE.getCanonicalPath();

        // 4. Verify file is within safe directory
        if (!canonicalPath.startsWith(safeCanonicalPath)) {
            throw new SecurityException("Path traversal attempt detected");
        }

        // 5. Check file exists and is a file (not directory)
        if (!requestedFile.exists() || !requestedFile.isFile()) {
            throw new FileNotFoundException("File not found");
        }

        return requestedFile;
    }
}
```

**Security Features:**
1. Whitelist character validation
2. Canonical path comparison
3. Boundary check (starts with safe directory)
4. File existence and type validation

---

### C# Example

#### ❌ Vulnerable C#

```csharp
// External input value used in file processing without validation
string file = Request.QueryString["path"];
if (file != null)
{
    File.Delete(file); 6:
}
```

#### ✅ Secure C#

```csharp
string file = Request.QueryString["path"];
if (file != null)
{
    // Check for path traversal characters before processing the file
    if ((file.IndexOf("WW") > -1 || file.IndexOf("/") > -1)
    {
        Response.Write("Path Traversal Attack");
    }
    else
    {
        File.Delete(file);
    }
}
```

---

### C Example

#### ❌ Vulnerable C

```c
char* filename = getenv("reportfile");
FILE *fin = NULL;
// Using the filename received from external configuration directly
fin = fopen(filename, "r");
while (fgetsbuf, BUF_LEN, fin)) {
    // Print file contents
}
```

#### ✅ Secure C with Regex Validation

```c
FILE *fin = NULL;
regex_t regex;
int ret;
char* filename = getenv("reportfile");
ret = regcomp(&regex, ".*WW.WW.WW.*", 0);
// Detect strings with potential path traversal
ret = regexec(&regex, filename, 0, NULL, 0);
if (!ret) {
    // Path traversal string detected, handle error
}
// Use the filtered filename
fin = fopen(filename, "r");
while (fgetsbuf, BUF_LEN, fin)) {
    // Print file contents
}
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-99: Resource Injection**
   MITRE, http://cwe.mitre.org/data/definitions/99.html

② **CWE-22: Path Traversal**
   MITRE, http://cwe.mitre.org/data/definitions/22.html

### OWASP

③ **Path Traversal**
   OWASP, https://www.owasp.org/index.php/Path_Traversal

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find file operations with user input
grep -r "new File.*request\\.getParameter" .
grep -r "new FileInputStream.*request" .
grep -r "FileReader.*request" .

# Find path manipulation
grep -r "getParameter.*fileName" .
grep -r "getParameter.*path" .
grep -r "getParameter.*file" .

# C/C++ file operations
grep -r "fopen.*getenv" .
grep -r "open.*argv" .
```

---

## ✅ Security Checklist

- [ ] All file paths validated before use
- [ ] Canonical path comparison against safe directory
- [ ] `../`, `..\\` sequences filtered
- [ ] Whitelist of allowed files/directories enforced
- [ ] Only filename (not path) accepted from user input
- [ ] Absolute paths rejected if only relative paths expected
- [ ] URL-encoded path traversal sequences checked (`%2e%2e%2f`)
- [ ] Symlink following disabled or controlled
- [ ] File access limited by web server configuration
- [ ] Proper error handling (don't leak path information)

---

## 🎯 Path Traversal Attack Variations

### Common Attack Patterns

```
../../../etc/passwd              # Unix/Linux
..\..\..\..\windows\win.ini     # Windows
....//....//....//etc/passwd     # Double encoding
..%2F..%2F..%2Fetc%2Fpasswd     # URL encoding
..%252F..%252F..%252Fetc        # Double URL encoding
%2e%2e/%2e%2e/%2e%2e/           # Full URL encoding
..;/..;/..;/etc/passwd          # Null byte injection (old)
....\/....\/....\/etc/passwd    # Mixed separators
```

### Defense Against All Variations

```java
public String sanitizePath(String input) {
    if (input == null) return null;

    // Decode URL encoding
    String decoded = URLDecoder.decode(input, "UTF-8");

    // Remove all variations of ../
    decoded = decoded.replaceAll("\\.{2,}[/\\\\]", "");
    decoded = decoded.replaceAll("[/\\\\]\\.{2,}", "");

    // Remove null bytes
    decoded = decoded.replace("\0", "");

    // Allow only safe characters
    decoded = decoded.replaceAll("[^a-zA-Z0-9._-]", "");

    return decoded;
}
```

---

## 💡 Best Practices

1. **Use Indirect Object References**
   ```java
   // DON'T: Accept filename from user
   String filename = request.getParameter("file");

   // DO: Use ID mapping
   String fileId = request.getParameter("id");
   String filename = fileMapping.get(fileId); // Safe lookup
   ```

2. **Restrict to Document Root**
   ```java
   // Configure web server to jail file access
   // Apache: DocumentRoot /var/www/html
   // Nginx: root /usr/share/nginx/html;
   ```

3. **Use Framework Security**
   ```java
   // Spring Security
   @PreAuthorize("hasPermission(#file, 'read')")
   public File getFile(String file) { ... }
   ```

---

**Always validate canonical paths to prevent path traversal attacks!**
