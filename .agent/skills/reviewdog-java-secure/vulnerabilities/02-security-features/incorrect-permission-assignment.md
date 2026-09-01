# Incorrect Permission Assignment for Critical Resource (CWE-732)

**Severity**: 🟠 HIGH
**Category**: Security Features
**OWASP Top 10**: A01:2021 – Broken Access Control

---

## Overview

### Attack Description

Incorrect permission assignment occurs when files, directories, or other system resources are created with overly permissive access controls. If access permissions on temporary files created for internal system operations, backup files, and similar resources are not set or are set too loosely, attackers can access those files. This commonly occurs on Unix-based operating systems when file or directory access permissions are not properly configured, allowing unauthorized users to read, modify, or execute critical resources, leading to information disclosure, data tampering, or privilege escalation.

### Impact

**Potential consequences:**
- Information disclosure (reading sensitive files)
- Data tampering (modifying critical files)
- Configuration manipulation
- Privilege escalation
- Code execution (if executable files writable)
- System compromise
- Compliance violations

---

## Security Measures

### Key Principles

Assign appropriate access permissions to critical files and directories. In particular, set permissions for system files, configuration files, and temporary files according to the Principle of Least Privilege.

**Primary Defenses:**

1. **Principle of Least Privilege**
   - Grant minimum necessary permissions
   - Restrict file access to owner only when possible
   - Use 600 (rw-------) for sensitive files
   - Use 700 (rwx------) for sensitive directories

2. **Secure File Creation**
   - Set permissions immediately after creation
   - Use restrictive umask (0077 or 0027)
   - Never create world-writable files (avoid 777)
   - Use secure temp file creation methods

3. **Permission Validation**
   - Verify permissions after file creation
   - Check directory permissions for uploaded files
   - Audit file permissions regularly
   - Use security tools to detect misconfigurations

4. **Secure Defaults**
   - Configure restrictive default permissions
   - Use application-specific service accounts
   - Separate public and private resources
   - Store sensitive files outside web root

---

## Code Examples

### Attack Scenario

The following example shows a case where a temporary file is created without setting permissions, allowing other users to read the file.

**Attack:**
```bash
# Application creates temp file with default permissions
# File: /tmp/sensitive_data_12345.tmp
# Permissions: -rw-r--r-- (644) - world-readable!

# Attacker can read the file
cat /tmp/sensitive_data_12345.tmp
```

---

### ❌ Vulnerable Code

#### Java - No Permission Setting

```java
import java.io.*;

public class TempFileExample {
    public void createTempFile() throws IOException {
        // No permission setting when creating temp file
        File tempFile = File.createTempFile("sensitive_data_", ".tmp");

        // Writing sensitive data
        FileWriter writer = new FileWriter(tempFile);
        writer.write("username=admin\n");
        writer.write("password=secret123\n");
        writer.write("api_key=sk-1234567890abcdef\n");
        writer.close();

        System.out.println("Temp file created: " + tempFile.getAbsolutePath());
        // File is created with default permissions (typically 644: rw-r--r--)
        // Other users can read it!
    }
}
```

**Problems:**
1. Default file permissions used (typically 644 on Unix)
2. File is world-readable
3. Sensitive data in temp file
4. No cleanup on failure
5. File may persist after program exit

**File Permissions (Unix):**
```
-rw-r--r-- (644)
 ^^^ ^^^ ^^^
 |   |   |
 |   |   +-- Others: read only
 |   +------ Group: read only
 +---------- Owner: read/write

Anyone on the system can read this file!
```

---

#### C - Insecure File Creation

```c
#include <stdio.h>
#include <stdlib.h>

void create_config_file() {
    FILE* fp;

    // Creating configuration file (no permission setting)
    fp = fopen("/tmp/app_config.conf", "w");

    if (fp == NULL) {
        perror("Failed to create file");
        return;
    }

    // Writing sensitive configuration information
    fprintf(fp, "db_host=localhost\n");
    fprintf(fp, "db_user=admin\n");
    fprintf(fp, "db_password=Pa$$w0rd123\n");

    fclose(fp);

    // File is created according to the default umask
    // Typically 022 umask results in 644 permissions (world-readable)
}
```

**Problems:**
1. Uses `fopen()` which respects umask
2. No explicit permission setting
3. Passwords in plain text
4. File readable by all users
5. No secure cleanup

---

### ✅ Secure Code

#### Java - Secure File Permissions

```java
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Set;

public class SecureTempFileExample {

    public void createSecureTempFile() throws IOException {
        // 1. Set permissions to allow only the owner to read/write
        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rw-------");
        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(perms);

        // 2. Create temporary file with permissions set
        Path tempFile = Files.createTempFile("sensitive_data_", ".tmp", attr);

        try {
            // 3. Write data
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
                writer.write("username=admin\n");
                writer.write("password=secret123\n");
                writer.write("api_key=sk-1234567890abcdef\n");
            }

            System.out.println("Secure temp file: " + tempFile.toAbsolutePath());

            // Verify file permissions
            Set<PosixFilePermission> actualPerms =
                Files.getPosixFilePermissions(tempFile);
            System.out.println("Permissions: " +
                PosixFilePermissions.toString(actualPerms));

        } finally {
            // 4. Delete after use
            Files.deleteIfExists(tempFile);
        }
    }
}
```

**Security Features:**
1. `PosixFilePermissions` for explicit permission setting
2. 600 permissions (rw-------)  - owner only
3. Permissions set at creation time
4. Try-finally for guaranteed cleanup
5. Permission verification

**File Permissions:**
```
-rw------- (600)
 ^^^ ^^^ ^^^
 |   |   |
 |   |   +-- Others: no access
 |   +------ Group: no access
 +---------- Owner: read/write only

Only the owner can access this file!
```

---

#### ✅ Better Practice - Complete File Security

```java
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class SecureFileManager {

    // Secure directory for temp files
    private static final Path SECURE_TEMP_DIR;

    static {
        try {
            // Create secure temporary directory
            Set<PosixFilePermission> dirPerms =
                PosixFilePermissions.fromString("rwx------"); // 700
            FileAttribute<Set<PosixFilePermission>> dirAttr =
                PosixFilePermissions.asFileAttribute(dirPerms);

            SECURE_TEMP_DIR = Files.createTempDirectory("secure_app_", dirAttr);

            // Register shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    deleteDirectory(SECURE_TEMP_DIR);
                } catch (IOException e) {
                    System.err.println("Failed to cleanup temp directory");
                }
            }));

        } catch (IOException e) {
            throw new RuntimeException("Failed to create secure temp directory", e);
        }
    }

    /**
     * Create secure temporary file with restricted permissions
     */
    public Path createSecureTempFile(String prefix, String suffix)
            throws IOException {

        // Set owner-only permissions (600)
        Set<PosixFilePermission> perms = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        );

        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(perms);

        // Create file in secure directory with restrictive permissions
        Path tempFile = Files.createTempFile(
            SECURE_TEMP_DIR,
            prefix,
            suffix,
            attr
        );

        // Verify permissions
        verifyFilePermissions(tempFile, perms);

        return tempFile;
    }

    /**
     * Create secure directory with restricted permissions
     */
    public Path createSecureDirectory(String prefix) throws IOException {

        // Set owner-only permissions (700)
        Set<PosixFilePermission> perms = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        );

        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(perms);

        Path dir = Files.createTempDirectory(SECURE_TEMP_DIR, prefix, attr);

        verifyFilePermissions(dir, perms);

        return dir;
    }

    /**
     * Write sensitive data to file with secure permissions
     */
    public void writeSensitiveData(Path file, String data) throws IOException {

        // Ensure restrictive permissions before writing
        Set<PosixFilePermission> perms = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
        );

        Files.setPosixFilePermissions(file, perms);

        // Write data
        Files.writeString(file, data, StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING);

        // Verify permissions after write
        verifyFilePermissions(file, perms);
    }

    /**
     * Verify file has correct permissions
     */
    private void verifyFilePermissions(Path file,
                                      Set<PosixFilePermission> expectedPerms)
            throws IOException {

        Set<PosixFilePermission> actualPerms =
            Files.getPosixFilePermissions(file);

        if (!actualPerms.equals(expectedPerms)) {
            throw new SecurityException(
                "File permissions mismatch. Expected: " +
                PosixFilePermissions.toString(expectedPerms) +
                ", Actual: " +
                PosixFilePermissions.toString(actualPerms)
            );
        }
    }

    /**
     * Secure file deletion
     */
    public void secureDelete(Path file) throws IOException {
        if (Files.exists(file)) {
            // Overwrite file content before deletion (optional, for sensitive data)
            if (Files.isRegularFile(file)) {
                overwriteFile(file);
            }

            Files.delete(file);
        }
    }

    /**
     * Overwrite file with random data before deletion
     */
    private void overwriteFile(Path file) throws IOException {
        long size = Files.size(file);
        byte[] zeros = new byte[(int) Math.min(size, 8192)];

        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            long written = 0;
            while (written < size) {
                int toWrite = (int) Math.min(zeros.length, size - written);
                fos.write(zeros, 0, toWrite);
                written += toWrite;
            }
        }
    }

    /**
     * Recursively delete directory
     */
    private static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        Files.walk(directory)
            .sorted(Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.err.println("Failed to delete: " + path);
                }
            });
    }
}
```

**Usage Example:**
```java
public class Application {
    public static void main(String[] args) {
        SecureFileManager fileManager = new SecureFileManager();

        try {
            // Create secure temp file
            Path tempFile = fileManager.createSecureTempFile("data_", ".tmp");

            // Write sensitive data
            String sensitiveData = "password=secret\napi_key=sk-12345";
            fileManager.writeSensitiveData(tempFile, sensitiveData);

            // Use the file...
            processFile(tempFile);

            // Secure deletion
            fileManager.secureDelete(tempFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

**Security Features:**
1. Secure temp directory with 700 permissions
2. Files created with 600 permissions (owner-only)
3. Permission verification after creation
4. Secure file deletion with overwrite
5. Automatic cleanup on shutdown
6. No world-readable or group-readable files
7. Comprehensive error handling

---

#### C - Secure File Creation with chmod

```c
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

void create_secure_config_file() {
    int fd;
    FILE* fp;
    mode_t old_umask;

    // 1. Set umask to be restrictive (077 = only owner can access newly created files)
    old_umask = umask(0077);

    // 2. Create file (open with O_CREAT)
    fd = open("/tmp/app_config.conf",
              O_WRONLY | O_CREAT | O_EXCL,  // O_EXCL: fail if exists
              S_IRUSR | S_IWUSR);             // 0600 permissions

    if (fd == -1) {
        perror("Failed to create file");
        umask(old_umask);
        return;
    }

    // 3. Explicitly set permissions (extra safety)
    if (fchmod(fd, S_IRUSR | S_IWUSR) == -1) {
        perror("Failed to set permissions");
        close(fd);
        umask(old_umask);
        return;
    }

    // 4. Convert to FILE* stream
    fp = fdopen(fd, "w");
    if (fp == NULL) {
        perror("Failed to fdopen");
        close(fd);
        umask(old_umask);
        return;
    }

    // 5. Write data
    fprintf(fp, "db_host=localhost\n");
    fprintf(fp, "db_user=admin\n");
    fprintf(fp, "db_password=Pa$$w0rd123\n");

    fclose(fp);  // Also closes fd

    // 6. Restore umask
    umask(old_umask);

    // File permissions: -rw------- (600)
    printf("Secure config file created with permissions 600\n");
}
```

**Security Features:**
1. Set restrictive umask (0077)
2. Use `open()` with explicit mode (0600)
3. `O_EXCL` flag prevents overwriting existing files
4. `fchmod()` for explicit permission setting
5. Restore original umask
6. File descriptor approach (more secure than `fopen`)

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-732: Incorrect Permission Assignment for Critical Resource**
   MITRE, https://cwe.mitre.org/data/definitions/732.html

② **CWE-276: Incorrect Default Permissions**
   MITRE, https://cwe.mitre.org/data/definitions/276.html

③ **CWE-377: Insecure Temporary File**
   MITRE, https://cwe.mitre.org/data/definitions/377.html

### CERT Secure Coding

④ **FIO06-C: Create files with appropriate access permissions**
   CERT, https://wiki.sei.cmu.edu/confluence/display/c/FIO06-C

⑤ **FIO15-J: Do not let user input control file system paths**
   CERT, https://wiki.sei.cmu.edu/confluence/display/java/FIO15-J

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find File.createTempFile without permission setting
grep -r "File.createTempFile" --include="*.java" .

# Find Files.createTempFile without FileAttribute
grep -r "Files.createTempFile" --include="*.java" . | grep -v "FileAttribute"

# Find C fopen without chmod
grep -r "fopen.*w" --include="*.c" . | grep -v "chmod"

# Check for world-writable files (Unix)
find . -type f -perm -002

# Check for world-readable sensitive files
find . -type f \( -name "*.conf" -o -name "*.config" -o -name "*password*" \) -perm -004
```

---

## ✅ Security Checklist

- [ ] All sensitive files created with restrictive permissions (600)
- [ ] Directories created with 700 permissions
- [ ] Never use 777 (world-writable) permissions
- [ ] Use `PosixFilePermissions` in Java for explicit control
- [ ] Set umask to 0077 or 0027 in C/C++
- [ ] Verify permissions after file creation
- [ ] Store sensitive files outside web root
- [ ] Use secure temp file creation methods
- [ ] Clean up temporary files
- [ ] Audit file permissions regularly
- [ ] No passwords or keys in world-readable files
- [ ] Test with least-privileged user accounts

---

## 🎯 Unix File Permissions Reference

### Permission Modes

| Octal | Binary | Symbolic | Description |
|-------|--------|----------|-------------|
| 700 | rwx------ | u=rwx,g=,o= | Owner only |
| 600 | rw------- | u=rw,g=,o= | Owner read/write |
| 755 | rwxr-xr-x | u=rwx,g=rx,o=rx | Public executable |
| 644 | rw-r--r-- | u=rw,g=r,o=r | Public readable |
| 750 | rwxr-x--- | u=rwx,g=rx,o= | Group readable |
| 640 | rw-r----- | u=rw,g=r,o= | Group readable file |

### Recommended Permissions

**Sensitive Files** (passwords, keys, configs):
- **600** (-rw-------): Owner read/write only
- **400** (-r--------): Owner read-only

**Sensitive Directories**:
- **700** (drwx------): Owner full access only

**Application Files**:
- **755** (drwxr-xr-x): Directories
- **644** (-rw-r--r--): Public files

**Executables**:
- **750** (rwxr-x---): Group executable
- **755** (rwxr-xr-x): Public executable

### Java Permission Constants

```java
// Owner only (600)
Set<PosixFilePermission> ownerOnly = EnumSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE
);

// Owner + Group read (640)
Set<PosixFilePermission> ownerGroupRead = EnumSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.GROUP_READ
);

// Directory owner only (700)
Set<PosixFilePermission> dirOwnerOnly = EnumSet.of(
    PosixFilePermission.OWNER_READ,
    PosixFilePermission.OWNER_WRITE,
    PosixFilePermission.OWNER_EXECUTE
);
```

---

## 🚨 Common Mistakes

1. **Using Default Permissions**
   ```java
   // DON'T: Rely on default permissions
   File.createTempFile("data", ".tmp");
   // May create world-readable file!

   // DO: Set explicit permissions
   Set<PosixFilePermission> perms =
       PosixFilePermissions.fromString("rw-------");
   Files.createTempFile("data", ".tmp",
       PosixFilePermissions.asFileAttribute(perms));
   ```

2. **World-Writable Files (777)**
   ```bash
   # DON'T: Never use 777
   chmod 777 config.properties

   # DO: Use least privilege
   chmod 600 config.properties
   ```

3. **Forgetting to Set Umask**
   ```c
   // DON'T: Use default umask
   fopen("sensitive.dat", "w");

   // DO: Set restrictive umask
   mode_t old = umask(0077);
   fopen("sensitive.dat", "w");
   umask(old);
   ```

4. **Not Verifying Permissions**
   ```java
   // DON'T: Assume permissions are set
   Files.createTempFile(path, prefix, suffix, attr);

   // DO: Verify after creation
   Files.createTempFile(path, prefix, suffix, attr);
   verifyPermissions(file, expectedPerms);
   ```

---

## 💡 Best Practices Summary

1. **Least privilege** - Grant minimum necessary permissions
2. **600 for sensitive files** - Owner read/write only
3. **700 for sensitive dirs** - Owner full access only
4. **Explicit permissions** - Don't rely on defaults
5. **Verify permissions** - Check after creation
6. **Secure temp files** - Use framework secure methods
7. **Clean up** - Delete temporary files
8. **Audit regularly** - Check file permissions
9. **Outside web root** - Store sensitive files securely
10. **Test as unprivileged** - Verify access controls

---

**Always set restrictive permissions (600/700) for sensitive files and directories!**
