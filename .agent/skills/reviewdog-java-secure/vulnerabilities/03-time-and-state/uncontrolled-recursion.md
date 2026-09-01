# Uncontrolled Recursion / Non-terminating Loops or Recursive Functions

## Metadata
- **CWE**: CWE-674 (Uncontrolled Recursion), CWE-835 (Loop with Unreachable Exit Condition 'Infinite Loop')
- **Category**: Time and State
- **Severity**: Medium to High
- **Language**: C, Java, C#
- **OWASP Top 10**: A04:2021 - Insecure Design

---

## Overview

**Uncontrolled Recursion** occurs when a recursive function lacks proper termination conditions (base case), leading to:
- **Stack Overflow**: Excessive stack memory consumption
- **Resource Exhaustion**: CPU and memory depletion
- **Denial of Service**: System becomes unresponsive

Failing to control the recursion cycle causes excessive use of allocated memory or program stack resources, making the system vulnerable. In most cases, a recursive function without a base case falls into an infinite loop, causing resource exhaustion and disrupting normal system service.

**Infinite Loops** occur when loop conditions can never be satisfied, consuming resources indefinitely.

**Attack Scenarios:**
1. Attacker provides deeply nested input (e.g., XML, JSON) triggering deep recursion
2. Missing or incorrect base case causes infinite recursion
3. Loop exit condition depends on external input that never arrives
4. Recursive algorithms process attacker-controlled tree structures

**Impact:**
- Application crashes (StackOverflowError in Java)
- Denial of Service
- Resource exhaustion
- System instability

---

## Security Measures

All recursive calls must be controlled by limiting the number of recursion calls or by setting an initial value (constant) to control recursion.

**Prevention Strategies:**

1. **Always Define Base Cases**
   - Every recursive function MUST have a clear termination condition
   - Validate base case is reachable

2. **Limit Recursion Depth**
   - Set maximum recursion depth counter
   - Throw exception or return error when limit exceeded

3. **Use Iteration Instead of Recursion**
   - Convert recursive algorithms to iterative when possible
   - Tail recursion optimization (if language supports it)

4. **Validate Input Data**
   - Limit input size/depth for nested structures
   - Reject maliciously crafted deep inputs (e.g., XML bombs)

5. **Set Loop Exit Conditions**
   - Ensure all loops have reachable exit conditions
   - Add timeout or iteration limits for potentially infinite loops

---

## Code Examples

### ❌ Vulnerable Code Example 1: C - Missing Base Case

```c
#include <stdio.h>

int factorial(int i)
{
    // No exit condition for the recursive function, causing an infinite loop.
    return i * factorial(i - 1);
}

int main()
{
    int num = 5;
    int result = factorial(num);
    printf("%d! : %d\n", num, result);
    return 0;
}
```

**Why Vulnerable:**
- No base case to stop recursion
- `factorial(-1)`, `factorial(-2)`, ... continues infinitely
- Causes stack overflow
- Process crashes or system becomes unresponsive

---

### ✅ Secure Code Example 1: C - Proper Base Case

```c
#include <stdio.h>

int factorial(int i)
{
    // When using recursive functions, an exit condition like the
    // following must be provided.
    if (i <= 1) {
        return 1;
    }
    return i * factorial(i - 1);
}

int main()
{
    int num = 5;
    int result = factorial(num);
    printf("%d! : %d\n", num, result);
    return 0;
}
```

**Security Benefits:**
- Base case `if (i <= 1)` ensures recursion terminates
- Handles edge cases (0! = 1, negative inputs)
- Prevents stack overflow
- Predictable resource usage

**Execution Flow:**
```
factorial(5) = 5 * factorial(4)
factorial(4) = 4 * factorial(3)
factorial(3) = 3 * factorial(2)
factorial(2) = 2 * factorial(1)
factorial(1) = 1  // Base case reached, recursion stops
Result: 5 * 4 * 3 * 2 * 1 = 120
```

---

### ❌ Vulnerable Code Example 2: Java - Deep Recursion Without Limit

```java
public class DeepRecursion {
    public int processTree(TreeNode node) {
        if (node == null) {
            return 0;
        }
        // No depth limit - attacker can provide deeply nested tree
        int leftSum = processTree(node.left);
        int rightSum = processTree(node.right);
        return node.value + leftSum + rightSum;
    }
}
```

**Why Vulnerable:**
- No recursion depth limit
- Attacker can craft deeply nested tree (e.g., 100,000 levels deep)
- Causes `StackOverflowError`

---

### ✅ Secure Code Example 2: Java - Recursion Depth Limit

```java
public class SecureRecursion {
    private static final int MAX_DEPTH = 1000;

    public int processTree(TreeNode node) {
        return processTreeHelper(node, 0);
    }

    private int processTreeHelper(TreeNode node, int depth) {
        if (node == null) {
            return 0;
        }

        // Recursion depth limit check
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                "Tree depth exceeds maximum allowed depth of " + MAX_DEPTH
            );
        }

        int leftSum = processTreeHelper(node.left, depth + 1);
        int rightSum = processTreeHelper(node.right, depth + 1);
        return node.value + leftSum + rightSum;
    }
}
```

**Security Benefits:**
- `MAX_DEPTH` limits recursion depth
- Throws exception when limit exceeded (fail securely)
- Prevents stack overflow attacks
- Provides clear error message

---

### ❌ Vulnerable Code Example 3: Java - Infinite Loop

```java
public void processData(InputStream input) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    String line;

    // Infinite loop with no termination condition
    while (true) {
        line = reader.readLine();
        if (line == null) {
            break;  // This might never be reached if stream doesn't close
        }
        processLine(line);
    }
}
```

**Why Vulnerable:**
- Relies on external stream to close (may never happen)
- Attacker can keep connection open indefinitely
- No timeout or iteration limit

---

### ✅ Secure Code Example 3: Java - Limited Loop with Timeout

```java
public void processData(InputStream input) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    String line;
    int lineCount = 0;
    final int MAX_LINES = 10000;

    // Maximum iteration limit
    while (lineCount < MAX_LINES) {
        line = reader.readLine();
        if (line == null) {
            break;
        }
        processLine(line);
        lineCount++;
    }

    if (lineCount >= MAX_LINES) {
        throw new IOException("Input exceeded maximum allowed lines: " + MAX_LINES);
    }
}
```

**Security Benefits:**
- `MAX_LINES` limits iterations
- Prevents infinite loop even if stream never closes
- Throws exception when limit exceeded
- Predictable resource consumption

---

### ❌ Vulnerable Code Example 4: C# - Uncontrolled XML Parsing

```csharp
public void ParseXml(XmlNode node)
{
    // No depth limit for nested XML
    ProcessNode(node);

    foreach (XmlNode child in node.ChildNodes)
    {
        ParseXml(child);  // Unbounded recursion
    }
}
```

**Why Vulnerable:**
- XML bomb attack: deeply nested XML elements
- No recursion depth limit
- Stack overflow

---

### ✅ Secure Code Example 4: C# - Limited XML Parsing

```csharp
private const int MAX_XML_DEPTH = 100;

public void ParseXml(XmlNode node)
{
    ParseXmlHelper(node, 0);
}

private void ParseXmlHelper(XmlNode node, int depth)
{
    if (depth > MAX_XML_DEPTH)
    {
        throw new InvalidOperationException(
            $"XML depth exceeds maximum allowed: {MAX_XML_DEPTH}"
        );
    }

    ProcessNode(node);

    foreach (XmlNode child in node.ChildNodes)
    {
        ParseXmlHelper(child, depth + 1);
    }
}
```

**Security Benefits:**
- Limits XML nesting depth
- Prevents XML bomb DoS attacks
- Fails fast with clear error message

---

## References

### CWE
- [CWE-674: Uncontrolled Recursion](http://cwe.mitre.org/data/definitions/674.html)
- [CWE-835: Loop with Unreachable Exit Condition ('Infinite Loop')](http://cwe.mitre.org/data/definitions/835.html)

### Related CWEs
- CWE-399: Resource Management Errors
- CWE-400: Uncontrolled Resource Consumption
- CWE-770: Allocation of Resources Without Limits or Throttling

### Standards and Guidelines
- OWASP: Denial of Service
- CERT Java Coding Standard: MET05-J. Ensure that constructors do not call overridable methods
- CERT C Coding Standard: MEM12-C. Consider using a Goto-Chain when leaving a function on error

---

## Detection Methods

### Static Analysis Patterns

**Grep Patterns:**
```bash
# Find recursive functions without obvious base case
grep -rn "return.*\w\+\s*(" --include="*.{c,cpp,java,cs}" . | grep -v "if"

# Find while(true) or for(;;) loops
grep -rn "while\s*(true)" --include="*.{java,cs}" .
grep -rn "for\s*(;;)" --include="*.{c,cpp,java}" .

# Find factorial/fibonacci without base case
grep -rn "factorial\|fibonacci" --include="*.{c,cpp,java,cs}" .

# Find recursive calls in functions
grep -rn "def \(\w\+\).*\1(" --include="*.py" .
```

### Code Review Checklist
- [ ] Every recursive function has a clear base case
- [ ] Recursion depth is limited (counter or input validation)
- [ ] All loops have reachable exit conditions
- [ ] Infinite loops (`while(true)`) have internal break conditions
- [ ] Input size/depth is validated before processing
- [ ] Recursive parsing (XML, JSON) has depth limits

### Dynamic Analysis
- Test with deeply nested inputs (e.g., 10,000 level deep JSON)
- Monitor stack usage during execution
- Set JVM stack size limits: `java -Xss256k` to detect stack overflow
- Use profilers to detect infinite loops or deep recursion

---

## Security Verification Checklist

### Design Phase
- [ ] Identify all recursive algorithms
- [ ] Design iteration-based alternatives where possible
- [ ] Define maximum recursion depth limits
- [ ] Plan input validation for nested structures

### Implementation Phase
- [ ] Every recursive function has a base case
- [ ] Add depth counter parameter to recursive functions
- [ ] Validate and limit input depth/size
- [ ] Add maximum iteration limits to loops
- [ ] Use iterative algorithms for simple recursion (factorial, etc.)

### Testing Phase
- [ ] Test with maximum depth inputs
- [ ] Test with malformed deeply nested data
- [ ] Verify stack overflow exceptions are caught properly
- [ ] Load test to ensure resource limits work

### Code Examples to Avoid
```java
// ❌ BAD: No base case
int fibonacci(int n) {
    return fibonacci(n-1) + fibonacci(n-2);
}

// ❌ BAD: No depth limit
void processJson(JsonNode node) {
    for (JsonNode child : node.getChildren()) {
        processJson(child);  // Unlimited recursion
    }
}

// ❌ BAD: Unreachable exit condition
while (count < max) {
    // ... code that doesn't modify 'count'
}
```

---

## Additional Security Measures

### Convert Recursion to Iteration

**Recursive Factorial (Vulnerable to deep recursion):**
```java
int factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}
```

**Iterative Factorial (Safe):**
```java
int factorial(int n) {
    if (n < 0) {
        throw new IllegalArgumentException("n must be non-negative");
    }
    int result = 1;
    for (int i = 2; i <= n; i++) {
        result *= i;
    }
    return result;
}
```

### Use Tail Recursion (Language-Dependent)

Some languages optimize tail recursion into iteration:

```scala
// Scala example - tail recursive (optimized by compiler)
def factorial(n: Int): Int = {
    @tailrec
    def factorialHelper(n: Int, accumulator: Int): Int = {
        if (n <= 1) accumulator
        else factorialHelper(n - 1, n * accumulator)
    }
    factorialHelper(n, 1)
}
```

### Stack Size Configuration

**Java:**
```bash
# Limit stack size to detect overflow quickly
java -Xss256k YourApplication

# Increase stack size if legitimate deep recursion needed
java -Xss2m YourApplication
```

**C/C++:**
```bash
# Use ulimit to limit stack size on Unix systems
ulimit -s 256  # 256 KB stack limit
```

---

## Related Vulnerabilities

- **CWE-674**: Uncontrolled Recursion
- **CWE-835**: Loop with Unreachable Exit Condition ('Infinite Loop')
- **CWE-400**: Uncontrolled Resource Consumption
- **CWE-770**: Allocation of Resources Without Limits or Throttling
- **CWE-409**: Improper Handling of Highly Compressed Data (Data Amplification)

---

## Severity and Impact

### CVSS v3.1 Base Score: 6.5 (Medium to High)
- **Attack Vector**: Network
- **Attack Complexity**: Low
- **Privileges Required**: None
- **User Interaction**: None
- **Impact**: Availability (Denial of Service)

### Business Impact
- **Availability**: Service downtime, application crashes
- **Performance**: CPU and memory exhaustion
- **User Experience**: Slow or unresponsive application
- **Infrastructure Cost**: Increased resource consumption

---

## Real-World Attack Examples

### XML Bomb (Billion Laughs Attack)
```xml
<?xml version="1.0"?>
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol1 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol2 "&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;">
  <!-- ... continues for many levels ... -->
]>
<lolz>&lol9;</lolz>
```

**Defense:** Limit XML nesting depth and entity expansion.

### JSON Bomb
```json
{
  "a": {
    "b": {
      "c": {
        // ... 10,000 levels deep
      }
    }
  }
}
```

**Defense:** Limit JSON parsing depth (e.g., Jackson: `JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER`)

---

## Summary

Uncontrolled recursion and infinite loops can cause Denial of Service through resource exhaustion. Always implement proper base cases, limit recursion depth, validate input size, and prefer iteration over recursion when possible.

**Key Takeaways:**
1. Every recursive function MUST have a base case
2. Limit recursion depth with counters
3. Validate and limit input depth/size for nested structures
4. Convert simple recursion to iteration (factorial, fibonacci)
5. Set maximum iteration limits for potentially infinite loops
6. Test with deeply nested/large inputs to verify limits work
