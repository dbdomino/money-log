# SQL Injection (CWE-89)

**Severity**: ⚠️ CRITICAL
**Category**: Input Validation & Representation
**OWASP Top 10**: A03:2021 – Injection

---

## Overview

### Attack Description

SQL Injection allows attackers to manipulate database queries by injecting malicious SQL code through user input fields or URL parameters. This occurs when web applications connected to databases fail to properly validate input data, allowing attackers to insert SQL statements into input values and URL input fields to view or manipulate information from the database.

### Impact

In vulnerable web applications, values entered by users are passed to dynamic queries without filtering, causing developer-unintended queries to be generated, which can have adverse effects including information disclosure.

**Potential consequences:**
- Unauthorized data access
- Data modification or deletion
- Authentication bypass
- Complete database compromise
- Remote code execution (in some configurations)

---

## Security Measures

### Key Principles

Use PreparedStatement<sup>5</sup> objects to send precompiled query statements (constants) to the database. When using PreparedStatement, filter special characters and query reserved words from external input values used in database queries. When using frameworks such as Struts or Spring, apply input validation models and security modules appropriate to the situation.

**Primary Defense: Parameterized Queries (PreparedStatement)**

1. **Use PreparedStatement** instead of Statement
   - Separates SQL logic from data
   - Prevents SQL injection by design
   - Database compiles query once, parameters are added safely

2. **Use ORM Framework Parameter Binding**
   - MyBatis: Use `#{}` syntax (NOT `${}`)
   - Hibernate: Use named parameters or positional parameters
   - Avoid string concatenation in all cases

3. **Input Validation (Defense in Depth)**
   - Whitelist validation for expected input patterns
   - Escape special SQL characters if dynamic queries are unavoidable
   - Use stored procedures with parameterized calls

<sup>5</sup> PreparedStatement: A precompiled query object supported by MySQL, Oracle, DB2, SQL Server, etc., accessible via Java's JDBC, Perl's DBI, PHP's PDO, and ASP's ADO.

---

## Code Examples

### Example Attack Scenario

The following is an example of unsafe code where the externally received value of `gubun` is used to construct an SQL query without any validation. In this case, if the value `a' or 'a' = 'a` is entered for `gubun`, the condition changes to `b_gubun = 'a' or 'a' = 'a'`, altering the query structure so that all contents of the board table are retrieved.

If attacker provides input: `a' or 'a' = 'a`, the query becomes:
```sql
SELECT * FROM board WHERE b_gubun = 'a' or 'a' = 'a'
```
This returns ALL records from the board table!

---

### ❌ Vulnerable Code

#### JDBC - String Concatenation

```java
// Using externally received values without validation is unsafe
String gubun = request.getParameter("gubun");
......
String sql = "SELECT * FROM board WHERE b_gubun = '" + gubun + "'";
Connection con = db.getConnection();
Statement stmt = con.createStatement();
// Externally received value is executed as a query without validation or processing - unsafe
ResultSet rs = stmt.executeQuery(sql);
```

**Problem:** User input (`gubun`) is directly concatenated into the SQL query, allowing injection attacks.

---

### ✅ Secure Code

#### JDBC - PreparedStatement

```java
String gubun = request.getParameter("gubun");
......
// 1. Use ? binding variables for PreparedStatement to safely handle
//    externally received user input values
String sql = "SELECT * FROM board WHERE b_gubun = ?";
Connection con = db.getConnection();
// 2. Use PreparedStatement
PreparedStatement pstmt = con.prepareStatement(sql);
// 3. Create PreparedStatement object as a constant string and set parameters
//    using methods like setString, making it safe
pstmt.setString(1, gubun);
ResultSet rs = pstmt.executeQuery();
```

**Solution:**
- Query structure is defined with `?` placeholder
- User input is bound using `setString()` method
- Database treats input as data, not executable SQL

---

#### MyBatis - Parameter Type="map" (Secure/Vulnerable Comparison)

##### ❌ Vulnerable MyBatis

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
......
<select id="boardSearch" parameterType="map" resultType="BoardDto">
  // Using $ symbol causes the externally entered keyword value to be concatenated
  // as a string into the query - unsafe
  select * from tbl_board where title like '%$ {keyword}%' order by pos asc
</select>
```

**Problem:** `${}` syntax performs string substitution, vulnerable to SQL injection.

##### ✅ Secure MyBatis

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
  "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
......
<select id="boardSearch" parameterType="map" resultType="BoardDto">
  // Modified to use # symbol for parameter binding instead of $ - safe
  select * from tbl_board where title like '%|# {keyword}|%' order by pos asc
</select>
```

**Solution:** `#{}` syntax uses parameterized queries, preventing SQL injection.

**MyBatis Data Map can expose unintended information when externally entered values are used as string concatenation in SQL query construction.**

---

#### Hibernate - Named Parameters (Secure/Vulnerable Comparison)

##### ❌ Vulnerable Hibernate (String Concatenation)

```java
import org.hibernate.Query
import org.hibernate.Session
......
// Using externally received values without validation is unsafe
String name = request.getParameter("name");
// Hibernate uses PreparedStatement by default, but without parameter binding it is unsafe
Query query = session.createQuery("from Student where studentName = '" + name + "'");
```

**Problem:** String concatenation in HQL query allows injection.

##### ✅ Secure Hibernate (Named Parameters)

```java
import org.hibernate.Query
import org.hibernate.Session
......
String name = request.getParameter("name");
// 1. Use named parameter variables for parameter binding
Query query = session.createQuery("from Student where studentName = :name");
// 2. Use parameter binding to prevent query structure modification by external input values
query.setString(0, name);
```

Also:

```java
import org.hibernate.Query
import org.hibernate.Session
......
String name = request.getParameter("name");
// 1. Use named parameter variables for parameter binding
Query query = session.createQuery("from Student where studentName = :name");
// 2. Use parameter binding to prevent query structure modification by external input values
query.setParameter("name", name);
```

**Solution:**
- Use named parameters (`:name`)
- Bind values using `setString()` or `setParameter()`
- Prevents query structure manipulation

---

#### C# - SqlCommand Parameters (Secure/Vulnerable Comparison)

##### ❌ Vulnerable C#

```csharp
public void ButtonClickBad(object sender, EventArgs e)
{
    string connect = "MyConnString";
    string usrinput = Request["ID"];
    // Using externally received values directly in SQL query is unsafe
    string query = "Select * From Products Where ProductID = '" + usrinput;
    using (var conn = new SqlConnection(connect))
    {
```

##### ✅ Secure C#

```csharp
using (var cmd = new SqlCommand(query, conn))
{
    conn.Open();
    cmd.ExecuteReader(); /* BUG */
}
```

Parameter binding must be used to eliminate the risk of query structure modification.

```csharp
void ButtonClickGood(object sender, EventArgs e)
{
    string connect = "MyConnString";
    string usrinput = Request["ID"];
    // Use @ for parameter binding. External input values cannot modify the query structure.
    string query = "Select * From Products Where ProductID = @ProductID";
    using (var conn = new SqlConnection(connect))
    {
        using (var cmd = new SqlCommand(query, conn))
        {
            cmd.Parameters.AddWithValue("@ProductID",
            Convert.ToInt32(Request["ProductID"]));
            conn.Open();
            cmd.ExecuteReader();
        }
    }
}
```

**Solution:** Use parameterized queries with `@` placeholders and `AddWithValue()` method.

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-89: SQL Injection**
   MITRE, http://cwe.mitre.org/data/definitions/89.html

### OWASP

② **Threat and Vulnerability: "SQL Injection"**
   Microsoft, http://technet.microsoft.com/en-us/library/ms161953%28v=SQL.105%29.aspx

③ **Input validation and Data Sanitization, Threat and Vulnerability: Prevent SQL Injection**
   CERT, http://www.securecoding.cert.org/confluence/display/java/IDS00-J.+Prevent+SQL+injection

④ **SQL Injection Prevention Cheat Sheet**
   OWASP, https://www.owasp.org/index.php/SQL_Injection_Prevention_Cheat_Sheet

### Real-World Example

**Online Banking Hijacking Attempt via Web Shell Discovered** (2012-03-05, Security News)

A case was discovered where a web shell named 'Shylock' targeted online banking customers. When a user logged into their account for online banking transactions, the attacker hijacked the session. The attacker posed as a customer support representative through a fake real-time chat window, tricking customers into believing their session had been compromised, then extracting customer information through the real-time chat.

---

## 🔍 Detection Patterns (Grep/Search)

Use these patterns to detect potential SQL injection vulnerabilities:

```bash
# Find string concatenation in SQL queries
grep -r "\"SELECT.*+.*request\\.getParameter" .
grep -r "\"INSERT.*+.*request\\.getParameter" .
grep -r "\"UPDATE.*+.*request\\.getParameter" .

# Find Statement usage (should use PreparedStatement)
grep -r "Statement stmt.*createStatement" .
grep -r "\\.executeQuery(.*+.*)" .

# Find MyBatis $ syntax (vulnerable)
grep -r '\$\s*{' *.xml

# Find Hibernate string concatenation
grep -r "createQuery(\".*+.*\")" .
```

---

## ✅ Security Checklist

- [ ] All user input is validated before use in SQL queries
- [ ] PreparedStatement is used instead of Statement
- [ ] MyBatis uses `#{}` syntax, never `${}`
- [ ] Hibernate uses named parameters or positional parameters
- [ ] No string concatenation in query construction
- [ ] Stored procedures use parameterized calls
- [ ] Input validation applied (whitelist approach)
- [ ] Error messages don't reveal database structure
- [ ] Database account uses principle of least privilege

---

## 🎯 Framework-Specific Guidance

### JDBC
✅ **Always use:** `PreparedStatement` with `setString()`, `setInt()`, etc.
❌ **Never use:** `Statement` with string concatenation

### MyBatis
✅ **Always use:** `#{}` for parameter binding
❌ **Never use:** `${}` (only use for table/column names with strict validation)

### Hibernate/JPA
✅ **Always use:** Named parameters (`:param`) or positional parameters (`?1`)
❌ **Never use:** String concatenation in HQL/JPQL

### Spring Data JPA
✅ **Always use:** `@Query` with `?1` or `:param` syntax
✅ **Use:** Method query derivation (automatic parameterization)

---

**Always use PreparedStatement and parameter binding for secure database access!**
