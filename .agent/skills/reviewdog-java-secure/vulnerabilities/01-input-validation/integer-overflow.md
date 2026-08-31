# Integer Overflow (CWE-190)

**Severity**: 🟠 HIGH
**Category**: Input Validation & Representation
**OWASP Top 10**: A04:2021 – Insecure Design

---

## Overview

### Attack Description

Integer Overflow occurs when an arithmetic operation produces a result that exceeds the maximum value that can be stored in the allocated memory space for that integer type. Since integer types have a fixed size, attempting to store a value larger than the allowed range causes the stored value to unexpectedly become a very small number or a negative number, leading to unexpected program behavior. This is especially dangerous when user-provided input values are used for loop control, memory allocation, or memory copy operations, as integer overflow in these contexts can cause security vulnerabilities.

### Impact

**Potential consequences:**
- Buffer overflow attacks
- Denial of Service (DoS)
- Infinite loops
- Memory corruption
- Bypass of security checks
- Incorrect calculations leading to business logic flaws

---

## Security Measures

### Key Principles

Verify the range of the language/platform integer types before use. When using integer variables in arithmetic operations, use modules that check the range of result values. Especially when using external input values for dynamic memory allocation, verify that the variable values are within an appropriate range.

**Primary Defenses:**

1. **Range Validation**
   - Validate input against min/max values for data type
   - Check results of arithmetic operations
   - Use checked arithmetic methods

2. **Use Larger Data Types**
   - Use `long` instead of `int` when possible
   - Use `BigInteger` for arbitrary precision
   - Consider 64-bit types for calculations

3. **Checked Arithmetic**
   - Java 8+: `Math.addExact()`, `Math.multiplyExact()`
   - Throws `ArithmeticException` on overflow
   - Use for critical calculations

4. **Input Sanitization**
   - Validate input range before arithmetic
   - Reject values outside acceptable range
   - Use defensive programming

---

## Code Examples

### Attack Scenario

In the following example, an external input value (slf_msg_param_num) is used to dynamically calculate a value that determines the array size. If the calculated value (param_ct) becomes negative due to overflow, the array size becomes negative, causing system problems.

**Attack Example:**
```java
// If slf_msg_param_num = 2147483647 (Integer.MAX_VALUE)
// param_ct = 2147483647 + 1 = -2147483648 (overflow!)
// Creating array with negative size causes exception
```

---

### ❌ Vulnerable Code

#### Java - No Overflow Check

```java
String msg_str = "";
String tmp = request.getParameter("slf_msg_param_num");
tmp = StringUtil.isNullTrim(tmp);
if (tmp.equals("0")) {
    msg_str = PropertyUtil.getValue(msg_id);
} else {
    // Using external input as integer without validating the input size is unsafe
    int param_ct = Integer.parseInt(tmp);
    String[] strArr = new String[param_ct];
```

**Problems:**
1. No validation of input value
2. No check for overflow after `parseInt()`
3. Can create array with negative size (after overflow)
4. No bounds checking

**Attack:**
```java
// Input: slf_msg_param_num=2147483647
// param_ct = 2147483647
// Further operations may overflow:
// param_ct + 1 = -2147483648 (overflow)
```

---

### ✅ Secure Code

#### Java - Range Validation

```java
String msg_str = "";
String tmp = request.getParameter("slf_msg_param_num");
tmp = StringUtil.isNullTrim(tmp);
if (tmp.equals("0")) {
    msg_str = PropertyUtil.getValue(msg_id);
} else {
    // Validate the input size when using external input as integer
    try {
        int param_ct = Integer.parseInt(tmp);
        if (param_ct < 0) {
            throw new Exception();
        }
        String[] strArr = new String[param_ct];
    } catch(Exception e) {
        msg_str = "Invalid input value.";
    }
}
```

**Security Features:**
1. Parse integer in try-catch
2. Validate value is non-negative
3. Throw exception for invalid input
4. Error handling for parse failures

---

#### ✅ Better Practice - Complete Validation

```java
public class SecureIntegerHandler {

    private static final int MAX_ARRAY_SIZE = 10000;  // Reasonable limit

    public String[] createSafeArray(String input) {
        // 1. Null/empty check
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be empty");
        }

        // 2. Parse with error handling
        int size;
        try {
            size = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + input);
        }

        // 3. Range validation
        if (size < 0) {
            throw new IllegalArgumentException(
                "Array size cannot be negative: " + size);
        }

        if (size > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                "Array size exceeds maximum allowed: " + size);
        }

        // 4. Create array with validated size
        return new String[size];
    }

    // Safe arithmetic operations
    public int safeAdd(int a, int b) {
        try {
            return Math.addExact(a, b);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Integer overflow in addition", e);
        }
    }

    public int safeMultiply(int a, int b) {
        try {
            return Math.multiplyExact(a, b);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Integer overflow in multiplication", e);
        }
    }

    public long safeCast(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value out of int range: " + value);
        }
        return (int) value;
    }
}
```

**Security Features:**
1. Maximum size limit enforced
2. Negative value check
3. NumberFormatException handling
4. Use `Math.addExact()` / `Math.multiplyExact()` for checked arithmetic
5. Safe casting with range check

---

### C# Example

#### ❌ Vulnerable C#

```csharp
public static void Main(string[] args)
{
    // Overflow occurs when external input value is too large
    int usrNum = Int32.Parse(args[0]);
    string[] array = {"one", "two", "three", "four"};
    string num = array[usrNum];
}
```

#### ✅ Secure C# with checked

```csharp
public static void Main(string[] args)
{
    // Use checked block to detect overflow and validate size
    try {
        int usrNum = checked(Int32.Parse(args[0]));
        string[] array = {"one", "two", "three", "four"};
        if(usrNum < 0)string num = array[usrNum];
    }
    catch (System.OverflowException e) { ... }
}
```

**Security Features:**
1. `checked` keyword for overflow detection
2. Throws `OverflowException` on overflow
3. Array bounds check

---

### C Example

#### ❌ Vulnerable C

```c
id main(int argc, char* argv[])
{
    // Overflow occurs when external input value is too large
    int usr_num = 0;
    char* num_array[] = {"one", "two", "three", "four"};
    char* num = NULL;
    usr_num = atoi(argv[1]);
    num = num_array[usr_num];
}
```

#### ✅ Secure C with Validation

```c
id main(int argc, char* argv[])
{
    // Validate input size to prevent overflow when using external input
    int usr_num = 0;
    char* num_array[] = {"one", "two", "three", "four"};
    char* num = NULL;
    usr_num = atoi(argv[1]);
    if (usr_num >= 0 && usr_num < 4) {
        num = num_array[usr_num];
    }
}
```

**Security Features:**
1. Range check (0 <= value < array_size)
2. Prevents out-of-bounds access
3. Prevents negative index

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-190: Integer Overflow**
   MITRE, http://cwe.mitre.org/data/definitions/190.html

### CERT

② **Enforce limits on integer values originating from tainted sources**
   CERT, http://www.securecoding.cert.org/confluence/display/c/INT04-C.+Enforce+limits+ on+integer+ values+originating+from+tainted+sources

③ **Verify that all integer values are in range**
   CERT, http://www.securecoding.cert.org/confluence/display/c/INT08-C.+Verify+that+all+ integer+values+are+in+range

### OWASP

④ **Integer overflow**
   OWASP, https://www.owasp.org/index.php/OWASP_Periodic_Table_of_Vulnerabilities_-_Integer_Overflow/Underflow

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find integer parsing without validation
grep -r "Integer.parseInt.*request" .
grep -r "Int32.Parse.*args" .
grep -r "atoi(" *.c

# Find arithmetic without overflow check
grep -r "\\+ .*parseInt" .
grep -r "\\* .*parseInt" .

# Find array allocation with user input
grep -r "new.*\\[.*parseInt" .
```

---

## ✅ Security Checklist

- [ ] All user input validated for integer range
- [ ] Checked arithmetic used (`Math.addExact()`, etc.)
- [ ] Maximum/minimum bounds enforced
- [ ] Negative values checked where inappropriate
- [ ] Array sizes validated before allocation
- [ ] `BigInteger` used for large calculations
- [ ] Overflow exceptions handled properly
- [ ] Integer overflow testing completed

---

## 🎯 Integer Types and Ranges

### Java Integer Types

| Type | Size | Min Value | Max Value |
|------|------|-----------|-----------|
| `byte` | 8-bit | -128 | 127 |
| `short` | 16-bit | -32,768 | 32,767 |
| `int` | 32-bit | -2,147,483,648 | 2,147,483,647 |
| `long` | 64-bit | -9,223,372,036,854,775,808 | 9,223,372,036,854,775,807 |

### Overflow Examples

```java
// Addition overflow
int max = Integer.MAX_VALUE;  // 2147483647
int overflow = max + 1;        // -2147483648 (overflow!)

// Multiplication overflow
int large = 1000000;
int product = large * large;   // -727379968 (overflow!)

// Subtraction underflow
int min = Integer.MIN_VALUE;   // -2147483648
int underflow = min - 1;       // 2147483647 (underflow!)
```

---

## 💡 Safe Arithmetic Patterns

### Java 8+ Math Methods

```java
// Safe addition
try {
    int result = Math.addExact(a, b);
} catch (ArithmeticException e) {
    // Handle overflow
}

// Safe subtraction
try {
    int result = Math.subtractExact(a, b);
} catch (ArithmeticException e) {
    // Handle underflow
}

// Safe multiplication
try {
    int result = Math.multiplyExact(a, b);
} catch (ArithmeticException e) {
    // Handle overflow
}

// Safe increment
try {
    int result = Math.incrementExact(value);
} catch (ArithmeticException e) {
    // Handle overflow
}

// Safe negation
try {
    int result = Math.negateExact(value);
} catch (ArithmeticException e) {
    // Handle overflow (MIN_VALUE cannot be negated)
}
```

### Using BigInteger

```java
import java.math.BigInteger;

// For arbitrary precision
BigInteger a = new BigInteger("999999999999999999");
BigInteger b = new BigInteger("999999999999999999");
BigInteger result = a.multiply(b);  // No overflow

// Validate against int range before conversion
if (result.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
    throw new ArithmeticException("Result too large for int");
}
int intResult = result.intValue();
```

---

## 🚨 Common Mistakes

1. **Casting Without Validation**
   ```java
   // DON'T: Cast long to int without check
   long largeValue = 3000000000L;
   int overflow = (int) largeValue;  // Overflow!

   // DO: Validate before cast
   if (largeValue > Integer.MAX_VALUE || largeValue < Integer.MIN_VALUE) {
       throw new IllegalArgumentException("Value out of range");
   }
   int safe = (int) largeValue;
   ```

2. **Ignoring Return Values**
   ```java
   // DON'T: Ignore potential overflow
   int size = userInput * itemSize;

   // DO: Check for overflow
   try {
       int size = Math.multiplyExact(userInput, itemSize);
   } catch (ArithmeticException e) {
       throw new IllegalArgumentException("Size calculation overflow");
   }
   ```

3. **Signed vs Unsigned Confusion**
   ```java
   // DON'T: Treat signed as unsigned
   int size = -1;
   byte[] buffer = new byte[size];  // NegativeArraySizeException

   // DO: Validate sign
   if (size < 0) {
       throw new IllegalArgumentException("Size must be positive");
   }
   ```

---

## 💡 Best Practices Summary

1. **Validate input range** - Check min/max before use
2. **Use checked arithmetic** - `Math.*Exact()` methods (Java 8+)
3. **Set reasonable limits** - Max array size, max calculation values
4. **Use larger types** - `long` instead of `int`, `BigInteger` for large numbers
5. **Handle exceptions** - Catch and handle overflow exceptions
6. **Test edge cases** - MAX_VALUE, MIN_VALUE, boundary conditions
7. **Defensive programming** - Assume overflow can happen

---

**Always check for integer overflow in arithmetic operations!**
