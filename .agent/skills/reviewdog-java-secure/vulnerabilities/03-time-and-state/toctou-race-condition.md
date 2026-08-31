# TOCTOU Race Condition

## Metadata
- **CWE**: CWE-367 (Time-of-check Time-of-use (TOCTOU) Race Condition)
- **Category**: Time and State
- **Severity**: High
- **Language**: Java, C#, C
- **OWASP Top 10**: A04:2021 - Insecure Design

---

## Overview

**TOCTOU (Time-of-Check Time-of-Use) Race Condition** is a vulnerability that occurs in concurrent/parallel systems when the state of a resource (file, socket, etc.) changes between the time it is checked and the time it is used.

In parallel systems (multi-process or multi-application), the state of a resource is checked before it is used. However, because the time of checking and the time of using differ, a resource that existed at the Time of Check may disappear or change state by the Time of Use.

For example, in a parallel system with processes A and B, process A checks whether a file exists (TOC) before using it. At that point, process B has not yet deleted the file, so process A determines it exists. However, by the time process A attempts to use the file (TOU), the file may have been deleted by process B, causing errors.

**Attack Scenario:**
1. Process A checks if a resource (e.g., file) exists (Time of Check)
2. Process B deletes or modifies the resource
3. Process A attempts to use the resource (Time of Use)
4. Process A fails or exhibits unexpected behavior because the resource state changed

This can lead to:
- Data corruption
- Privilege escalation (e.g., symlink attacks)
- Denial of Service
- Unauthorized access

---

## Security Measures

**Prevention Strategies:**

When shared resources (e.g., files) are accessed by multiple processes, use synchronization constructs to ensure only one process can access the resource at a time (synchronized, mutex, etc.), while minimizing performance impact by synchronizing only around the critical section.

1. **Use Synchronization Mechanisms**
   - Java: `synchronized` blocks or `ReentrantLock`
   - C: `mutex_lock()` and `mutex_unlock()`
   - C#: `lock` statement or `[MethodImpl(MethodImplOptions.Synchronized)]`

2. **Atomic Operations**
   - Combine check and use into a single atomic operation
   - Use file operations with exclusive flags (e.g., `O_CREAT | O_EXCL` in C)

3. **Minimize Critical Section**
   - Only synchronize the minimum necessary code to reduce performance impact

4. **Avoid File System Race Conditions**
   - Use file descriptors instead of file paths where possible
   - Verify resource state immediately before use, not in advance

---

## Code Examples

### ❌ Vulnerable Code Example 1: Java - Unsynchronized File Access

```java
class FileMgmtThread extends Thread {
    private String manageType = "";

    public FileMgmtThread(String type) {
        manageType = type;
    }

    // In a multi-threaded environment, multiple processes may access
    // the shared resource simultaneously, making this unsafe.
    public void run() {
        try {
            if (manageType.equals("READ")) {
                File f = new File("Test_367.txt");
                if (f.exists()) {
                    BufferedReader br
                        = new BufferedReader(new FileReader(f));
                    br.close();
                }
            } else if (manageType.equals("DELETE")) {
                File f = new File("Test_367.txt");
                if (f.exists()) {
                    f.delete();
                }
            } else { /* ... */ }
        } catch (IOException e) { /* ... */ }
    }
}

public class CWE367 {
    public static void main(String[] args) {
        FileMgmtThread fileAccessThread = new FileMgmtThread("READ");
        FileMgmtThread fileDeleteThread = new FileMgmtThread("DELETE");
        // File read and delete run simultaneously, which is unsafe.
        fileAccessThread.start();
        fileDeleteThread.start();
    }
}
```

**Why Vulnerable:**
- No synchronization between threads
- File existence check (`f.exists()`) and file operation (`FileReader(f)` or `f.delete()`) are not atomic
- Race condition window between check and use

---

### ✅ Secure Code Example 1: Java - Synchronized Access

```java
class FileMgmtThread extends Thread {
    private static final String SYNC = "SYNC";
    private String manageType = "";

    public FileMgmtThread(String type) {
        manageType = type;
    }

    public void run() {
        // In a multi-threaded environment, use synchronized to prevent
        // simultaneous access.
        synchronized(SYNC) {
            try {
                if (manageType.equals("READ")) {
                    File f = new File("Test_367.txt");
                    if (f.exists()) {
                        BufferedReader br
                            = new BufferedReader(new FileReader(f));
                        br.close();
                    }
                } else if (manageType.equals("DELETE")) {
                    File f = new File("Test_367.txt");
                    if (f.exists()) {
                        f.delete();
                    }
                } else { /* ... */ }
            } catch (IOException e) { /* ... */ }
        }
    }
}

public class CWE367 {
    public static void main(String[] args) {
        FileMgmtThread fileAccessThread = new FileMgmtThread("READ");
        FileMgmtThread fileDeleteThread = new FileMgmtThread("DELETE");
        fileAccessThread.start();
        fileDeleteThread.start();
    }
}
```

**Security Benefits:**
- `synchronized(SYNC)` ensures only one thread can execute the critical section
- Eliminates race condition between file check and file use
- Operations are now atomic from the perspective of concurrent threads

---

### ❌ Vulnerable Code Example 2: C# - Unsynchronized File Access

```csharp
// In a multi-threaded environment, simultaneous access is possible and unsafe.
public void ReadFile(String f)
{
    if(File.Exists(f))
    {
        File.ReadAllLines(f);
    }
}
```

**Why Vulnerable:**
- No synchronization mechanism
- File could be deleted between `Exists()` check and `ReadAllLines()` call

---

### ✅ Secure Code Example 2: C# - Synchronized Method

```csharp
// In a multi-threaded environment, use synchronization to prevent
// simultaneous access.
[MethodImpl(MethodImplOptions.Synchronized)]
public void ReadFile(String f)
{
    if(File.Exists(f))
    {
        File.ReadAllLines(f);
    }
}
```

**Security Benefits:**
- `MethodImpl(MethodImplOptions.Synchronized)` provides method-level synchronization
- Prevents concurrent execution of the method

---

### ❌ Vulnerable Code Example 3: C - Bank Account Race Condition

```c
static volatile double account;

void deposit(int amount) {
    // Accessing shared resource without a lock
    account += amount;
}

void withdraw(int amount) {
    account -= amount;
}
```

**Why Vulnerable:**
- No locking mechanism for shared resource `account`
- Classic race condition: deposit and withdraw can interleave, causing incorrect final balance

**Example Scenario:**
```
(ex1) Normal execution:
deposit(100) called : (account: 0)
deposit(100) finished: (account: 100)
withdraw(100) called : (account: 100)
withdraw(100) finished: (account: 0)

(ex2) Race condition:
deposit(100) called : (account: 0)
withdraw(100) called : (account: 0)
deposit(100) finished: (account: 100)
withdraw(100) finished: (account: -100)  // Incorrect!
```

The account balance becomes -100 instead of 0 due to race condition.

---

### ✅ Secure Code Example 3: C - Mutex-Protected Operations

```c
static volatile double account;
static mtx_t account_lock;

void deposit(int amount) {
    // Use mutex_lock and mutex_unlock to restrict access to the shared resource.
    mutex_lock(&account_lock);
    account += amount;
    mutex_unlock(&account_lock);
}

void withdraw(int amount) {
    mutex_lock(&account_lock);
    account -= amount;
    mutex_unlock(&account_lock);
}
```

**Security Benefits:**
- `mutex_lock()` ensures exclusive access to shared resource
- Only one thread can modify `account` at a time
- Prevents interleaving of operations

---

## References

### CWE
- [CWE-367: Time-of-check Time-of-use (TOCTOU) Race Condition](http://cwe.mitre.org/data/definitions/367.html)

### CERT Secure Coding
- [Avoid TOCTOU race conditions while accessing files](http://www.securecoding.cert.org/confluence/display/c/FIO45-C.+Avoid+TOCTOU+conditions+while+accessing+files)

### Additional Resources
- MITRE ATT&CK: Race Conditions
- OWASP: Race Condition vulnerabilities
- Java Concurrency in Practice (Book)
- POSIX Threads Programming

---

## Detection Methods

### Static Analysis Patterns

**Grep Patterns:**
```bash
# Find file operations without synchronization
grep -rn "f\.exists()" --include="*.java" .
grep -rn "File\.Exists" --include="*.cs" .

# Find potential race conditions in file access
grep -rn "new File(" --include="*.java" . | grep -v "synchronized"

# Find unsynchronized shared resource access
grep -rn "static.*volatile" --include="*.{c,cpp}" .
grep -rn "account\s*[+\-]=" --include="*.{c,cpp}" . | grep -v "mutex"
```

### Code Review Checklist
- [ ] File existence checks followed by file operations
- [ ] Shared resources accessed by multiple threads without synchronization
- [ ] Check-then-act patterns in concurrent code
- [ ] Time-dependent security decisions
- [ ] File operations using path names instead of file descriptors

### Dynamic Analysis
- Test with thread interleaving tools (e.g., ThreadSanitizer, Helgrind)
- Stress test with concurrent access scenarios
- Monitor for FileNotFoundException or access errors under load

---

## Security Verification Checklist

### Design Phase
- [ ] Identify all shared resources accessed by concurrent processes/threads
- [ ] Design synchronization strategy (locks, semaphores, atomic operations)
- [ ] Minimize critical sections for performance

### Implementation Phase
- [ ] Use `synchronized` (Java), `lock` (C#), or `mutex` (C) for shared resource access
- [ ] Combine check and use operations atomically
- [ ] Use file descriptors instead of file paths where possible
- [ ] Avoid time-of-check-time-of-use patterns

### Testing Phase
- [ ] Test with multiple concurrent threads/processes
- [ ] Verify thread safety with static analysis tools
- [ ] Use race condition detection tools (ThreadSanitizer, etc.)
- [ ] Stress test under high concurrency

### Deployment Phase
- [ ] Document all synchronized resources
- [ ] Review locking strategy for deadlock potential
- [ ] Monitor for race condition errors in production logs

---

## Additional Security Measures

### Java Specific
```java
// Use ReentrantLock for more control
import java.util.concurrent.locks.ReentrantLock;

class FileMgmtThread extends Thread {
    private static final ReentrantLock lock = new ReentrantLock();

    public void run() {
        lock.lock();
        try {
            // Critical section
            if (manageType.equals("READ")) {
                // File operations
            }
        } finally {
            lock.unlock();
        }
    }
}
```

### C Specific - Atomic File Creation
```c
// Use O_CREAT | O_EXCL for atomic file creation
#include <fcntl.h>
#include <sys/stat.h>

int fd = open("/tmp/tempfile", O_WRONLY | O_CREAT | O_EXCL,
               S_IRUSR | S_IWUSR);
if (fd == -1) {
    if (errno == EEXIST) {
        // File already exists - race condition avoided
    }
} else {
    // File created successfully and exclusively
    // Perform operations...
    close(fd);
}
```

### C# Specific - Lock Statement
```csharp
private static readonly object fileLock = new object();

public void SafeFileOperation(string path)
{
    lock (fileLock)
    {
        if (File.Exists(path))
        {
            File.ReadAllLines(path);
        }
    }
}
```

---

## Related Vulnerabilities

- **CWE-362**: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition')
- **CWE-366**: Race Condition within a Thread
- **CWE-364**: Signal Handler Race Condition
- **CWE-365**: Race Condition in Switch
- **CWE-691**: Insufficient Control Flow Management

---

## Severity and Impact

### CVSS v3.1 Base Score: 7.0 (High)
- **Attack Vector**: Local
- **Attack Complexity**: High
- **Privileges Required**: None
- **User Interaction**: None
- **Impact**: Data corruption, privilege escalation, unauthorized access

### Business Impact
- **Data Integrity**: Account balances, file contents can be corrupted
- **Availability**: Application crashes or hangs
- **Security**: Privilege escalation through symlink attacks
- **Compliance**: Violation of data integrity requirements

---

## Summary

TOCTOU race conditions occur when the state of a resource changes between checking it and using it in concurrent environments. Always use proper synchronization mechanisms (`synchronized`, `mutex`, `lock`) to protect shared resources. Combine check-and-use operations atomically, and minimize critical sections for performance.

**Key Takeaways:**
1. Never rely on check-then-act patterns in concurrent code
2. Use synchronization primitives for all shared resource access
3. Prefer atomic operations when possible
4. Test thoroughly with concurrent access scenarios
