# XPath Injection (CWE-643)

**Severity**: 🟠 HIGH
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

XPath Injection occurs when unvalidated external input values are used as strings to construct XQuery or XPath queries, allowing attackers to arbitrarily change the query structure and execute arbitrary queries to view unauthorized data or bypass authentication. Similar to SQL Injection, attackers can manipulate XPath queries to bypass authentication, access unauthorized data, or extract sensitive information from XML documents.

### Impact

**Potential consequences:**
- Authentication bypass
- Unauthorized data access
- Information disclosure from XML databases
- Data manipulation
- Complete compromise of XML data store

---

## Security Measures

### Key Principles

Filter special characters and query reserved words from external input data used in XQuery or XPath queries, and use XQuery that supports parameterized queries.

**Primary Defenses:**

1. **Parameterized XPath Queries**
   - Use parameterized XPath expressions
   - Never concatenate user input into XPath
   - Use variable binding (QName)

2. **Input Validation**
   - Whitelist allowed characters
   - Filter XPath metacharacters: `' " / [ ] ( ) and or`
   - Validate input format

3. **Escape Special Characters**
   - Escape XPath special characters
   - Use proper encoding
   - Sanitize before query construction

4. **Least Privilege**
   - Limit XML document access
   - Apply principle of least privilege
   - Use access control on XML data

---

## Code Examples

### Attack Scenario - XQuery Injection

In the following example, external input (name) is used as part of the parameters in a query created by executeQuery. If `something or '1'='1'` is passed as the value of name, the following query can be executed, which would output all values in the file.

```xpath
doc('users.xml')/userlist/user[uname='something' or '1'='1']
```

This always returns true, bypassing authentication.

---

### ❌ Vulnerable Code

#### Java - XPath with String Concatenation

```java
// External input value is used in the XQuery expression without validation
String name = props.getProperty("name");
......
// External input value causes the query structure to change - unsafe
String es = "doc('users.xml')/userlist/user[uname='"+name+"']";
XQPreparedExpression expr = conn.prepareExpression(es);
XQResultSequence result = expr.executeQuery();
```

**Problems:**
1. User input (`name`) directly concatenated into XPath
2. No validation or escaping
3. Can inject XPath operators: `or`, `and`, `'`, etc.
4. Authentication bypass possible

**Attack Examples:**

```java
// Normal input: "john"
// XPath: doc('users.xml')/userlist/user[uname='john']

// Attack 1: "' or '1'='1"
// XPath: doc('users.xml')/userlist/user[uname='' or '1'='1']
// Result: Returns ALL users

// Attack 2: "' or 1=1]/*[name()='password"
// XPath: doc('users.xml')/userlist/user[uname='' or 1=1]/*[name()='password']
// Result: Extracts all passwords

// Attack 3: "admin' and '1'='1"
// XPath: doc('users.xml')/userlist/user[uname='admin' and '1'='1']
// Result: Access admin account without password
```

---

#### C# - Vulnerable XQuery

```csharp
// Constructing XQuery statement from external input values
String squery =
    "for $user in doc(users.xml)//user[username="
    + UserTextBox.Text
    + "and pass="
    + PwdTextBox.Text
    + "] return $user";

Processor processor = new Processor();
XQueryCompiler compiler = processor.NewXQueryCompiler();
XdmNode indoc = processor.NewDocumentBuilder().Build(new
Uri(Server.MapPath("users.xml")));
using (StreamReader query = new StreamReader(squery))
{
    XQueryCompiler compiler = processor.NewXQueryCompiler();
    XQueryExecutable exp = compiler.Compile(query.ReadToEnd());
```

---

### ✅ Secure Code

#### Java - Parameterized XQuery with QName

```java
// Use the bindString function to prevent query structure changes from external input values

String name = props.getProperty("name");
......
String es = "doc('users.xml')/userlist/user[uname=$xname]";
XQPreparedExpression expr = conn.prepareExpression(es);
expr.bindString(new QName("xname"), name, null);
XQResultSequence result = expr.executeQuery();
```

**Security Features:**
1. **Uses variable binding** - `$xname` instead of concatenation
2. **bindString()** - Properly escapes and validates input
3. **Type-safe** - QName ensures proper variable binding
4. **Query structure fixed** - Cannot be modified by user input

---

#### Java - Complete Secure Example with XPath

```java
public class SecureXPathQuery {

    public NodeList queryUsers(String username) throws XPathExpressionException {
        // Load XML document
        Document doc = loadXMLDocument("users.xml");

        // Use XPath with variables (parameterized)
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // Create XPath expression with variable
        XPathExpression expr = xpath.compile(
            "/userlist/user[uname=$username]"
        );

        // Alternative: use parameterized XQuery factory
        // This creates a query that prevents injection
        XQueryFactory().createXQuery(new File("login.xq"));
        Document doc = new Builder().build("users.xml");

        // Read the login.xq file containing the parameterized query to create a parameterized query
        XQuery xquery = new XQueryFactory().createXQuery(new File("login.xq"));
    }
}
```

**login.xq file:**
```xquery
[ login.xq file ]
declare variable $loginID as xs:string external;
declare variable $password as xs:string external;

//users/user[@loginID=$loginID and @password=$password]
// Preventing XPath Injection using XQuery
String nm = props.getProperty("name");
String pw = props.getProperty("password");
Document doc = new Builder().build("users.xml");
```

---

#### ✅ Java - Safe XPath with Parameterization

```java
// Read external input values name and password from properties and store in nm, pw
String nm = props.getProperty("name");
String pw = props.getProperty("password");
......
XPathFactory factory = XPathFactory.newInstance();
```

**Complete implementation:**
```java
XPath xpath = factory.newXPath();
......
// An unsafe query statement is created using unvalidated input values nm, pw
// and stored in the expr variable
XPathExpression expr = xpath.compile("//users/user[login/text()='"+nm+"' and
    password/text()='"+pw+"']/home_dir/text()");
// The unsafe query in expr is evaluated and the result is stored
Object result = expr.evaluate(doc, XPathConstants.NODESET);
// The result is converted to NodeList type and stored in nodes
NodeList nodes = (NodeList) result;
for (int i=0; i<nodes.getLength(); i++) {
    String value = nodes.item(i).getNodeValue();
    if (value.indexOf("(") < 0 ) {
        // An attacker could identify names and passwords
        System.out.println(value);
    }
}
```

---

#### ✅ C# - Filtered Input

```csharp
String squery =
    "for $user in doc(users.xml)//user[username="
    + UserTextBox.Text
    + "and pass="
    + PwdTextBox.Text
    + "] return $user";
// Filter dangerous strings using string filtering
string validatedQuery = squery.Replace("/","");
Processor processor = new Processor();
XQueryCompiler compiler = processor.NewXQueryCompiler();

XdmNode indoc = processor.NewDocumentBuilder().Build(new
Uri(Server.MapPath("users.xml")));
using (StreamReader query = new StreamReader(validatedQuery))
{
```

**Note:** Filtering is fragile - parameterization is better!

---

### ✅ Best Practice - Input Validation + Parameterization

```java
public class SecureXPathExample {

    // Whitelist allowed characters
    private static final Pattern SAFE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    public NodeList secureQuery(String username, String password)
            throws XPathExpressionException {

        // 1. Input validation (whitelist)
        if (!SAFE_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException("Invalid username format");
        }
        if (!SAFE_PATTERN.matcher(password).matches()) {
            throw new IllegalArgumentException("Invalid password format");
        }

        // 2. Use parameterized XPath (if supported)
        // Note: Standard Java XPath doesn't support variables well
        // Better: Use XQuery with proper parameterization

        Document doc = loadDocument("users.xml");
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        // 3. If concatenation unavoidable, escape properly
        String safeUsername = escapeXPath(username);
        String safePassword = escapeXPath(password);

        String expression = String.format(
            "//user[uname='%s' and pass='%s']",
            safeUsername, safePassword
        );

        XPathExpression expr = xpath.compile(expression);
        return (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
    }

    private String escapeXPath(String input) {
        // Escape XPath special characters
        return input.replace("'", "&apos;")
                    .replace("\"", "&quot;");
    }
}
```

---

### XPath Attack Variations

```
* XPath Metacharacters to filter:
'   (single quote)
"   (double quote)
/   (path separator)
//  (descendant selector)
[ ] (predicates)
( ) (grouping)
and, or (operators)
```

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-643: Improper Neutralization of Data within XPath Expressions ('XPath Injection')**
   MITRE, https://cwe.mitre.org/data/definitions/643.html

② **CWE-91: XML Injection (aka Blind XPath Injection)**
   MITRE, https://cwe.mitre.org/data/definitions/91.html

### OWASP

③ **XPath Injection**
   OWASP, https://owasp.org/www-community/attacks/XPATH_Injection

④ **Testing for XPath Injection**
   OWASP, https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/07-Input_Validation_Testing/09-Testing_for_XPath_Injection

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find XPath usage
grep -r "XPathFactory" .
grep -r "xpath.compile" .
grep -r "XQPreparedExpression" .
grep -r "XQueryCompiler" .

# Find string concatenation in XPath
grep -r "xpath.*compile.*\\+" .
grep -r "XQuery.*\\+.*request" .

# Find potentially vulnerable patterns
grep -r "doc\\('.*'\\).*\\+" .
```

---

## ✅ Security Checklist

- [ ] No user input concatenated into XPath expressions
- [ ] Parameterized XPath/XQuery used (with bindString)
- [ ] Input validation applied (whitelist)
- [ ] XPath metacharacters filtered: `' " / [ ] ( ) and or`
- [ ] Use of QName for variable binding
- [ ] External query files with parameters (login.xq pattern)
- [ ] Proper escaping if concatenation unavoidable
- [ ] XPath injection testing completed
- [ ] Code review for all XPath/XQuery usage

---

## 🎯 XPath Injection Attack Examples

### Authentication Bypass

```xpath
# Normal query
//user[uname='john' and pass='secret']

# Attack 1: Always true
//user[uname='' or '1'='1' and pass='']
# Bypass authentication

# Attack 2: Comment out password check
//user[uname='admin' or '1'='1'] | //user[pass='']
# Access admin without password
```

### Data Extraction

```xpath
# Extract all usernames
//user[uname='' or 1=1]/uname

# Extract all passwords
//user[uname='' or 1=1]/password

# Blind injection (boolean-based)
//user[uname='admin' and substring(password,1,1)='a']
```

### XML Structure Discovery

```xpath
# Count nodes
//user[uname='' or count(//user)>0]

# Extract node names
//user[uname='' or name(/*[1])='users']
```

---

## 💡 Safe Alternatives

### Use JSON Instead of XML

```java
// Instead of XPath on XML
JSONObject json = new JSONObject(jsonString);
String username = json.getString("username");
```

### Use Relational Database

```java
// Instead of XML database with XPath
// Use SQL database with PreparedStatement
PreparedStatement pstmt = conn.prepareStatement(
    "SELECT * FROM users WHERE username = ?");
pstmt.setString(1, username);
```

---

## 🚨 Common Mistakes

1. **String Concatenation**
   ```java
   // DON'T: Concatenate user input
   String query = "//user[name='" + userInput + "']";

   // DO: Use parameterization
   String query = "//user[name=$username]";
   expr.bindString(new QName("username"), userInput, null);
   ```

2. **Insufficient Validation**
   ```java
   // DON'T: Only filter some characters
   input = input.replace("'", ""); // Still vulnerable to "

   // DO: Whitelist approach
   if (!input.matches("[a-zA-Z0-9]+")) {
       throw new IllegalArgumentException();
   }
   ```

3. **Trust XML Content**
   ```java
   // DON'T: Assume XML content is safe
   // Attacker may control XML document

   // DO: Validate XML schema and content
   ```

---

## 💡 Best Practices Summary

1. **Never concatenate user input** - Use parameterized queries
2. **Use bindString() with QName** - Proper variable binding
3. **Validate input** - Whitelist allowed characters
4. **External query files** - Separate queries from code
5. **Escape if needed** - Last resort, prefer parameterization
6. **Consider alternatives** - JSON, relational DB
7. **Test for injection** - Include in security testing

---

**Always use parameterized XPath queries - Never concatenate user input!**
