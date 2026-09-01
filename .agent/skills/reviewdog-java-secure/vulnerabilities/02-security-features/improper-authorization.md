# Improper Authorization (CWE-285)

**Severity**: 🔴 CRITICAL
**Category**: Security Features
**OWASP Top 10**: A01:2021 – Broken Access Control

---

## Overview

### Attack Description

Improper authorization occurs when an application does not properly verify whether a user has appropriate permissions before allowing them to perform an action. If authorization checks are missing or insufficient, attackers can use unauthorized functions or access unauthorized data, including horizontal privilege escalation (accessing other users' data) and vertical privilege escalation (gaining admin privileges).

### Impact

**Potential consequences:**
- Horizontal privilege escalation (access other users' data)
- Vertical privilege escalation (gain admin privileges)
- Unauthorized data modification or deletion
- Bypassing business logic restrictions
- Financial fraud
- Privacy violations
- Compliance violations (GDPR, HIPAA, PCI-DSS)

---

## Security Measures

### Key Principles

Restrict access to resources such as databases and files based on user permissions, and verify access rights before granting access.

**Primary Defenses:**

1. **Access Control Validation**
   - Verify user has permission for each action
   - Check authorization on server-side (never client-side only)
   - Validate both authentication AND authorization
   - Use allow-list (whitelist) approach

2. **Ownership Verification**
   - Verify user owns the resource being accessed
   - Compare session user ID with resource owner ID
   - Prevent horizontal privilege escalation
   - Use indirect object references

3. **Role-Based Access Control (RBAC)**
   - Define clear roles and permissions
   - Map users to roles
   - Check role before allowing access
   - Implement least privilege principle

4. **Attribute-Based Access Control (ABAC)**
   - Use attributes (user, resource, environment)
   - Fine-grained access control policies
   - Context-aware authorization
   - Dynamic policy evaluation

5. **Centralized Access Control**
   - Single point of authorization logic
   - Consistent enforcement across application
   - Easier to audit and maintain
   - Use framework security features

---

## Code Examples

### Attack Scenario

In the following example, a user is authenticated but there is no check for whether they have permission to delete another user's post. Simply by manipulating the parameter (articleNum), they can delete any article, making it insecure.

**Attack:**
```
# User A (authenticated) tries to delete User B's article
POST /board/delete.jsp?articleNum=123

# Even though User A doesn't own article 123,
# the code doesn't check ownership and allows deletion
```

---

### ❌ Vulnerable Code

#### Java JSP - No Authorization Check

```jsp
<!-- Checks authentication when deleting a post, but allows deletion without verifying the author -->
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.*" %>
<%
    // Authentication check
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("/login.jsp");
        return;
    }

    // Deletes article using only the article number (no author verification)
    String articleNum = request.getParameter("articleNum");

    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

    // Deletes without verifying the author
    PreparedStatement pstmt = conn.prepareStatement(
        "DELETE FROM board WHERE article_num = ?"
    );
    pstmt.setString(1, articleNum);
    pstmt.executeUpdate();

    out.println("The post has been deleted.");
%>
```

**Problems:**
1. Checks authentication but NOT authorization
2. No ownership verification
3. Any authenticated user can delete any article
4. Vulnerable to horizontal privilege escalation
5. Insecure Direct Object Reference (IDOR)

**Attack Example:**
```bash
# User A can delete User B's articles
curl -X POST "http://example.com/board/delete.jsp?articleNum=999" \
     -H "Cookie: JSESSIONID=user_a_session"

# Success! Even though User A doesn't own article 999
```

---

#### Java Servlet - Missing Ownership Check

```java
@WebServlet("/board/delete")
public class DeleteArticleServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // Authentication check
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendRedirect("/login.jsp");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        String articleNum = request.getParameter("articleNum");

        try {
            BoardDAO dao = new BoardDAO();
            // Deletes without verifying the author
            dao.deleteArticle(articleNum);

            response.getWriter().println("Article deleted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Problems:**
1. Gets user ID from session but doesn't use it for authorization
2. No check if `userId` owns `articleNum`
3. IDOR vulnerability

---

### ✅ Secure Code

#### Java JSP - With Ownership Verification

```jsp
<!-- Verifies the author when deleting a post -->
<%@ page contentType="text/html; charset=utf-8" %>
<%@ page import="java.sql.*" %>
<%
    // 1. Authentication check
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("userId") == null) {
        response.sendRedirect("/login.jsp");
        return;
    }

    String userId = (String) session.getAttribute("userId");
    String articleNum = request.getParameter("articleNum");

    Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);

    // 2. Author verification query
    PreparedStatement checkStmt = conn.prepareStatement(
        "SELECT writer_id FROM board WHERE article_num = ?"
    );
    checkStmt.setString(1, articleNum);
    ResultSet rs = checkStmt.executeQuery();

    if (!rs.next()) {
        out.println("Post not found.");
        return;
    }

    String writerId = rs.getString("writer_id");

    // 3. Verify the user is the author
    if (!userId.equals(writerId)) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        out.println("You do not have permission to delete this post.");
        return;
    }

    // 4. Delete after ownership confirmation
    PreparedStatement deleteStmt = conn.prepareStatement(
        "DELETE FROM board WHERE article_num = ? AND writer_id = ?"
    );
    deleteStmt.setString(1, articleNum);
    deleteStmt.setString(2, userId);
    int deleted = deleteStmt.executeUpdate();

    if (deleted > 0) {
        out.println("The post has been deleted.");
    } else {
        out.println("Deletion failed.");
    }
%>
```

**Security Features:**
1. Authentication check (session validation)
2. Ownership verification (check writer_id)
3. Authorization check (userId == writerId)
4. SQL DELETE includes both article_num AND writer_id
5. Proper error handling
6. Returns 403 Forbidden for unauthorized access

---

#### ✅ Better Practice - DAO Pattern with Authorization

```java
@WebServlet("/board/delete")
public class SecureDeleteArticleServlet extends HttpServlet {

    @Autowired
    private BoardService boardService;

    protected void doPost(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Authentication check
        HttpSession session = request.getSession(false);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().println("Authentication required");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        String articleNumStr = request.getParameter("articleNum");

        try {
            int articleNum = Integer.parseInt(articleNumStr);

            // 2. Authorization check via service layer
            boolean isAuthorized = boardService.canDeleteArticle(
                userId, articleNum
            );

            if (!isAuthorized) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().println(
                    "You don't have permission to delete this article"
                );
                return;
            }

            // 3. Perform deletion with authorization confirmed
            boardService.deleteArticle(userId, articleNum);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Article deleted successfully");

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Invalid article number");
        } catch (Exception e) {
            logger.error("Error deleting article", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("Error deleting article");
        }
    }
}

/**
 * Service layer with authorization logic
 */
@Service
public class BoardService {

    @Autowired
    private BoardDAO boardDAO;

    /**
     * Check if user can delete article
     */
    public boolean canDeleteArticle(String userId, int articleNum) {
        // Get article
        Article article = boardDAO.findById(articleNum);

        if (article == null) {
            return false;
        }

        // Check ownership
        if (article.getWriterId().equals(userId)) {
            return true;
        }

        // Check if user is admin (optional)
        User user = userDAO.findById(userId);
        if ("ADMIN".equals(user.getRole())) {
            return true;
        }

        return false;
    }

    /**
     * Delete article with authorization
     */
    @Transactional
    public void deleteArticle(String userId, int articleNum)
            throws UnauthorizedException {

        // Double-check authorization
        if (!canDeleteArticle(userId, articleNum)) {
            throw new UnauthorizedException(
                "User " + userId + " cannot delete article " + articleNum
            );
        }

        // Perform deletion
        boardDAO.deleteByIdAndWriter(articleNum, userId);

        // Log action for audit
        auditLog.info("User {} deleted article {}", userId, articleNum);
    }
}

/**
 * DAO with authorization in query
 */
@Repository
public class BoardDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Delete article - includes writer verification in WHERE clause
     */
    public int deleteByIdAndWriter(int articleNum, String writerId) {
        String sql = "DELETE FROM board WHERE article_num = ? AND writer_id = ?";

        return jdbcTemplate.update(sql, articleNum, writerId);
    }

    public Article findById(int articleNum) {
        String sql = "SELECT * FROM board WHERE article_num = ?";

        return jdbcTemplate.queryForObject(
            sql,
            new Object[]{articleNum},
            new ArticleRowMapper()
        );
    }
}
```

**Security Features:**
1. Authorization check in service layer
2. Ownership verification before deletion
3. Admin override capability
4. Double-check in deleteArticle method
5. SQL includes both ID and writer_id
6. Audit logging
7. Proper exception handling
8. Separation of concerns (Servlet → Service → DAO)

---

#### ✅ Best Practice - Spring Security Method Security

```java
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

/**
 * Controller with method-level security
 */
@RestController
@RequestMapping("/api/board")
public class BoardController {

    @Autowired
    private BoardService boardService;

    /**
     * Delete article - requires authentication
     */
    @DeleteMapping("/{articleNum}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteArticle(
            @PathVariable int articleNum,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            String userId = currentUser.getUsername();

            // Authorization check
            boardService.deleteArticleIfAuthorized(userId, articleNum);

            return ResponseEntity.ok()
                .body(Map.of("message", "Article deleted successfully"));

        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You don't have permission"));

        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Article not found"));

        } catch (Exception e) {
            logger.error("Error deleting article", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete article"));
        }
    }

    /**
     * Update article - custom authorization check
     */
    @PutMapping("/{articleNum}")
    @PreAuthorize("@boardSecurityService.canModify(#articleNum, authentication)")
    public ResponseEntity<?> updateArticle(
            @PathVariable int articleNum,
            @RequestBody ArticleUpdateRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        try {
            String userId = currentUser.getUsername();
            boardService.updateArticle(userId, articleNum, request);

            return ResponseEntity.ok()
                .body(Map.of("message", "Article updated successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Update failed"));
        }
    }
}

/**
 * Custom security service for fine-grained authorization
 */
@Service("boardSecurityService")
public class BoardSecurityService {

    @Autowired
    private BoardRepository boardRepository;

    /**
     * Check if user can modify article
     */
    public boolean canModify(int articleNum, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String userId = authentication.getName();

        // Get article
        Optional<Article> articleOpt = boardRepository.findById(articleNum);
        if (!articleOpt.isPresent()) {
            return false;
        }

        Article article = articleOpt.get();

        // Check ownership
        if (article.getWriterId().equals(userId)) {
            return true;
        }

        // Check admin role
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        return isAdmin;
    }

    /**
     * Check if user can view article (for private articles)
     */
    public boolean canView(int articleNum, Authentication authentication) {
        Optional<Article> articleOpt = boardRepository.findById(articleNum);
        if (!articleOpt.isPresent()) {
            return false;
        }

        Article article = articleOpt.get();

        // Public articles - anyone can view
        if (article.isPublic()) {
            return true;
        }

        // Private articles - only owner or admin
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String userId = authentication.getName();

        return article.getWriterId().equals(userId) ||
               authentication.getAuthorities().stream()
                   .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}

/**
 * JPA Repository with authorization queries
 */
@Repository
public interface BoardRepository extends JpaRepository<Article, Integer> {

    /**
     * Find article by ID and writer (for authorization)
     */
    Optional<Article> findByArticleNumAndWriterId(int articleNum, String writerId);

    /**
     * Delete article by ID and writer (authorization in query)
     */
    @Modifying
    @Query("DELETE FROM Article a WHERE a.articleNum = :articleNum AND a.writerId = :writerId")
    int deleteByArticleNumAndWriterId(
        @Param("articleNum") int articleNum,
        @Param("writerId") String writerId
    );

    /**
     * Find all articles by writer
     */
    List<Article> findByWriterId(String writerId);

    /**
     * Find public articles
     */
    List<Article> findByIsPublicTrue();
}
```

**Security Features:**
1. `@PreAuthorize` for method-level security
2. Custom security service for complex authorization
3. SpEL expressions for authorization checks
4. `@AuthenticationPrincipal` for current user
5. Repository queries include authorization
6. Separation of concerns
7. Admin role support
8. Public/private article handling
9. Comprehensive error handling

---

## References

### CWE (Common Weakness Enumeration)

① **CWE-285: Improper Authorization**
   MITRE, https://cwe.mitre.org/data/definitions/285.html

② **CWE-639: Authorization Bypass Through User-Controlled Key**
   MITRE, https://cwe.mitre.org/data/definitions/639.html

③ **CWE-918: Insecure Direct Object Reference (IDOR)**
   OWASP, https://owasp.org/www-project-web-security-testing-guide/latest/4-Web_Application_Security_Testing/05-Authorization_Testing/04-Testing_for_Insecure_Direct_Object_References

### OWASP

④ **A01:2021 – Broken Access Control**
   OWASP Top 10, https://owasp.org/Top10/A01_2021-Broken_Access_Control/

⑤ **Authorization Cheat Sheet**
   OWASP, https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html

---

## 🔍 Detection Patterns (Grep/Search)

```bash
# Find DELETE/UPDATE without ownership check
grep -r "DELETE FROM.*WHERE.*=.*getParameter" --include="*.jsp" .
grep -r "UPDATE.*SET.*WHERE.*getParameter" --include="*.jsp" .

# Find missing authorization in servlets
grep -r "doPost\|doGet" --include="*.java" . | xargs grep -L "canDelete\|isAuthorized"

# Find IDOR vulnerabilities
grep -r "getParameter.*id\|getParameter.*num" --include="*.java" .

# Find queries without user_id/owner_id in WHERE clause
grep -r "DELETE\|UPDATE" --include="*.java" . | grep -v "user_id\|owner_id\|writer_id"
```

---

## ✅ Security Checklist

- [ ] Authorization check on every sensitive operation
- [ ] Ownership verification before modify/delete
- [ ] Server-side authorization (not client-side)
- [ ] SQL queries include owner/user ID in WHERE clause
- [ ] Indirect object references used (no direct IDs in URLs)
- [ ] Role-based access control implemented
- [ ] Admin override documented and audited
- [ ] Authorization logic centralized
- [ ] Failed authorization attempts logged
- [ ] IDOR testing completed
- [ ] Horizontal privilege escalation tested
- [ ] Vertical privilege escalation tested

---

## 🎯 Authorization Patterns

### 1. Ownership-Based Authorization

```java
public boolean isOwner(String userId, int resourceId) {
    Resource resource = resourceDAO.findById(resourceId);
    return resource != null && resource.getOwnerId().equals(userId);
}

// Use in service
if (!isOwner(userId, articleId)) {
    throw new UnauthorizedException("Not the owner");
}
```

### 2. Role-Based Authorization

```java
public enum Role {
    USER, MODERATOR, ADMIN
}

public boolean hasRole(String userId, Role requiredRole) {
    User user = userDAO.findById(userId);
    return user.getRole().ordinal() >= requiredRole.ordinal();
}
```

### 3. Permission-Based Authorization

```java
public enum Permission {
    READ, WRITE, DELETE, ADMIN
}

public boolean hasPermission(String userId, int resourceId,
                            Permission requiredPermission) {
    ResourcePermission perm = permissionDAO.find(userId, resourceId);
    return perm != null && perm.getPermissions().contains(requiredPermission);
}
```

---

## 🚨 Common Mistakes

1. **Authentication Without Authorization**
   ```java
   // DON'T: Only check if user is logged in
   if (session.getAttribute("userId") != null) {
       deleteArticle(articleId); // Anyone can delete anything!
   }

   // DO: Check both authentication AND authorization
   if (session.getAttribute("userId") != null &&
       isOwner(userId, articleId)) {
       deleteArticle(articleId);
   }
   ```

2. **Client-Side Authorization**
   ```javascript
   // DON'T: Client-side check
   if (currentUser.id === article.writerId) {
       // Show delete button
   }
   // Attacker can still call API directly!

   // DO: Server-side authorization in API
   ```

3. **Missing Authorization in SQL**
   ```java
   // DON'T: No ownership check in query
   DELETE FROM articles WHERE id = ?

   // DO: Include owner in WHERE clause
   DELETE FROM articles WHERE id = ? AND writer_id = ?
   ```

---

## 💡 Best Practices Summary

1. **Verify ownership** - Check user owns resource before allowing access
2. **Server-side only** - Never rely on client-side authorization
3. **Include in SQL** - Add owner/user_id in WHERE clauses
4. **Centralize logic** - Single authorization service
5. **Use frameworks** - Spring Security @PreAuthorize
6. **Audit logging** - Log all authorization failures
7. **Least privilege** - Grant minimum necessary permissions
8. **Test thoroughly** - Test privilege escalation scenarios

---

**Always verify user authorization before performing sensitive operations!**
