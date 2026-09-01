# Code Injection (CWE-94)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

Code Injection allows attackers to inject arbitrary code into an application, which is then executed by the interpreter. The attacker inserts arbitrary code to alter the software's intended behavior, causing it to operate abnormally. Code injection differs from OS command injection in that it is limited only by the capabilities of the programming language itself. This is particularly dangerous in dynamic languages like JavaScript, Python, and PHP where `eval()` or similar functions can execute code at runtime.

### Impact

If a vulnerable program allows user input to contain code, attackers can inject code not intended by the developer to seize privileges, bypass authentication, or execute system commands.

**Potential consequences:**
- Arbitrary code execution on the server
- Data theft or modification
- Authentication bypass
- Complete system compromise
- Remote code execution (RCE)

---

## Security Measures

### Key Principles

Do not use functions that can execute dynamic code. If necessary, implement whitelist-based validation for external input values to ensure that executable dynamic code is not treated as input. Alternatively, filter user input used in dynamic code to include only valid characters.

**Primary Defenses:**

1. **Avoid Dynamic Code Execution**
   - Never use `eval()`, `new Function()`, `Runtime.exec()` with user input
   - Use safer alternatives like parsing and validation

2. **Whitelist Validation**
   - If dynamic code is unavoidable, use strict whitelist
   - Only allow predefined, safe commands/scripts
   - Validate against allowed patterns with regex

3. **Input Sanitization**
   - Remove or escape special characters
   - Validate input format matches expected pattern
   - Use safe APIs that don't execute code

4. **Principle of Least Privilege**
   - Run code execution engines in sandboxed environments
   - Limit permissions of execution context

---

## Code Examples

### Attack Scenario

The following example script uses `javax.script.ScriptEngineManager` to execute and output user input via `ScriptEngineManager()`. In this case, an attacker can submit crafted input to create or overwrite files using attack code.

If attacker inputs: `new Function("return process.env")()`, sensitive environment variables could be exposed.

---

### ❌ Vulnerable Code

#### Java - ScriptEngine.eval() with User Input

```java
public class CodeInjectionController {
    @RequestMapping(value = "/execute", method = RequestMethod.GET)
    public String execute(@RequestParam("src") String src) throws ScriptException {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine scriptEngine =
            scriptEngineManager.getEngineByName("javascript");
        // External input value src is executed via the JavaScript eval function - unsafe
        String retValue = (String)scriptEngine.eval(src);
        return retValue;
    }
}
```

**Problem:** User-controlled `src` parameter is directly passed to `eval()`, allowing arbitrary JavaScript code execution.

**Attack Example:**
```
/execute?src=new java.lang.ProcessBuilder('cat','/etc/passwd').start()
```

#### JavaScript - new Function()

```html
<body>
<%
String name = request.getparameter("name");
%>
...
<script>
// External input value name is converted to a function and executed using JavaScript new Function()
(new Function(%=name%))();
</script>
</body>
```

**Problem:** User input directly embedded in `new Function()`, allowing arbitrary JavaScript execution.

---

### ✅ Secure Code

#### Java - Whitelist Validation

```java
@RequestMapping(value = "/execute", method = RequestMethod.GET)
public String execute(@RequestParam("src") String src) throws ScriptException {
    // Step 1: Do not allow special character input values
    if (src.matches("[ \\ | \\ w]*") == false) {
        throw new IllegalArgumentException();
    }

    ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("javascript");
    String retValue = (String)scriptEngine.eval(src);
    return retValue;
}
```

**Improvements:**
- Input validation using regex whitelist
- Only allows word characters (alphanumeric + underscore)
- Throws exception for invalid input
- **Note:** Still risky - better to avoid `eval()` entirely

#### Java - Whitelist with Predefined Commands

```java
@RequestMapping(value = "/execute", method = RequestMethod.GET)
public String execute(@RequestParam("src") String src) throws ScriptException {
    // If the valid character is "_", call the method to execute
    if (src.matches("UNDER_BAR") == true) {
        ...
        // If the valid character is "$", call the method to execute
    } else if (src.matches("DOLLAR") == true) {
        ...
        // Throw exception for invalid special character input
    } else {
        throw new IllegalArgumentException();
    }
    ...
}
```

**Best Practice:**
- Map user input to predefined safe functions
- No dynamic code execution
- Complete control over what can be executed

#### Better Alternative - Avoid eval() Entirely

```java
@RequestMapping(value = "/calculate", method = RequestMethod.GET)
public String calculate(
    @RequestParam("operation") String operation,
    @RequestParam("value1") int value1,
    @RequestParam("value2") int value2
) {
    // Use a controlled switch statement instead of eval
    int result;
    switch (operation) {
        case "add":
            result = value1 + value2;
            break;
        case "subtract":
            result = value1 - value2;
            break;
        case "multiply":
            result = value1 * value2;
            break;
        case "divide":
            if (value2 != 0) {
                result = value1 / value2;
            } else {
                throw new IllegalArgumentException("Division by zero");
            }
            break;
        default:
            throw new IllegalArgumentException("Invalid operation");
    }
    return String.valueOf(result);
}
```

**Why This Is Better:**
- No code execution at all
- Predefined, safe operations
- Type-safe parameters
- Easy to test and maintain

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-94: Improper Control of Generation of Code ('Code Injection')**
   MITRE, http://cwe.mitre.org/data/definitions/94.html

② **CWE-95: Improper Neutralization of Directives in Dynamically Evaluated Code ('Eval Injection')**
   MITRE, http://cwe.mitre.org/data/definitions/95.html

### OWASP

③ **Code Injection Software Attack**
   OWASP, https://owasp.org/www-community/attacks/Code_Injection

---

## 🔍 Detection Patterns (Grep/Search)

Use these patterns to detect potential code injection vulnerabilities:

```bash
# Find ScriptEngine usage
grep -r "ScriptEngine.*eval" .
grep -r "ScriptEngineManager" .

# Find JavaScript eval in JSP
grep -r "new Function" *.jsp
grep -r "<%=.*%>" *.jsp

# Find Runtime.exec with variables
grep -r "Runtime.*exec.*request" .

# Find other dangerous functions
grep -r "\.eval(" .
grep -r "executeScript" .
```

---

## ✅ Security Checklist

- [ ] No use of `eval()`, `new Function()`, or similar dynamic code execution
- [ ] If eval unavoidable, strict whitelist validation applied
- [ ] User input never directly passed to code execution functions
- [ ] Input validated against regex pattern
- [ ] Special characters filtered or rejected
- [ ] Sandboxing applied if code execution required
- [ ] Prefer static alternatives (switch statements, predefined functions)
- [ ] Code review completed for all dynamic code paths

---

## 🎯 Framework-Specific Guidance

### Java
❌ **Never use:** `ScriptEngine.eval()` with user input
✅ **Instead:** Map input to predefined functions

### JavaScript/Node.js
❌ **Never use:** `eval()`, `new Function()`, `setTimeout(string)`, `setInterval(string)`
✅ **Instead:** Parse JSON with `JSON.parse()`, use function references

### Python
❌ **Never use:** `eval()`, `exec()`, `compile()`
✅ **Instead:** Use `ast.literal_eval()` for safe evaluation, or predefined functions

### PHP
❌ **Never use:** `eval()`, `assert()` with user input
✅ **Instead:** Use switch statements or lookup tables

---

## 💡 Safe Alternatives

### Instead of eval() for JSON parsing:
```java
// DON'T: eval(jsonString)
// DO: Use Jackson or Gson
ObjectMapper mapper = new ObjectMapper();
MyObject obj = mapper.readValue(jsonString, MyObject.class);
```

### Instead of eval() for math expressions:
```java
// DON'T: eval("2 + 2")
// DO: Use expression parser library
Expression expression = new ExpressionBuilder("2 + 2").build();
double result = expression.evaluate();
```

### Instead of eval() for dynamic behavior:
```java
// DON'T: eval(userCode)
// DO: Use Strategy pattern
interface Operation {
    int execute(int a, int b);
}

Map<String, Operation> operations = new HashMap<>();
operations.put("add", (a, b) -> a + b);
operations.put("multiply", (a, b) -> a * b);

Operation op = operations.get(userInput);
if (op != null) {
    result = op.execute(x, y);
}
```

---

## 🚨 Common Pitfalls

1. **Thinking validation makes eval() safe**
   - Even with validation, eval() is dangerous
   - Attackers find creative bypasses
   - Better to avoid entirely

2. **Using eval() for convenience**
   - "It's easier to use eval()"
   - The convenience isn't worth the security risk
   - Alternatives exist for every use case

3. **Trusting "internal" sources**
   - "This input comes from our database"
   - Database could be compromised
   - Defense in depth: validate everywhere

---

**Never execute user input as code - Use safe alternatives!**
