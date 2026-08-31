# Improper Resource Release

## Overview

**CWE-404: Improper Resource Shutdown or Release**
**CWE-772: Missing Release of Resource after Effective Lifetime**

Improper Resource Release is a vulnerability where system resources such as files, database connections, sockets, and memory are not properly released after use, leading to resource leaks. When resources are not released, system resources become exhausted, causing performance degradation, Denial of Service (DoS), and system instability.

It is critical to ensure resources are released even when exceptions occur, using finally blocks or try-with-resources statements.

## Vulnerability Analysis

### Common Resource Types
1. **File Descriptors**
   - FileInputStream, FileOutputStream, FileReader, FileWriter
   - RandomAccessFile, FileChannel

2. **Network Connections**
   - Socket, ServerSocket, DatagramSocket
   - URLConnection, HttpURLConnection

3. **Database Connections**
   - Connection, Statement, PreparedStatement, ResultSet
   - Connection pool resources

4. **Memory Resources**
   - Native memory (ByteBuffer.allocateDirect)
   - Temporary files, caches

5. **Threads and Executors**
   - ExecutorService, ThreadPoolExecutor
   - Timer, ScheduledExecutorService

### Impact
- **Availability**: Service outage due to resource exhaustion
- **Performance**: System performance degradation, increased response times
- **Stability**: Crashes due to insufficient memory or file handles
- **Security**: DoS attacks exploiting resource exhaustion

## Security Measures

### 1. Try-with-Resources (Java 7+) - Recommended

**Automatic Resource Management**
```java
// Single resource
try (FileInputStream fis = new FileInputStream(inputFile)) {
    // File processing
    byte[] data = new byte[fis.available()];
    fis.read(data);
} catch (IOException e) {
    logger.error("File processing failed", e);
}
// fis.close() is called automatically

// Multiple resources
try (FileInputStream in = new FileInputStream(inputFile);
     FileOutputStream out = new FileOutputStream(outputFile)) {
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
    }
} catch (IOException e) {
    logger.error("File copy failed", e);
}
// Both in and out are automatically closed (in reverse order)
```

### 2. Finally Block (Java 6 and Earlier)

**Manual Resource Release**
```java
FileInputStream in = null;
FileOutputStream out = null;

try {
    in = new FileInputStream(inputFile);
    out = new FileOutputStream(outputFile);

    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
    }
} catch (IOException e) {
    logger.error("File copy failed", e);
} finally {
    // Check each resource for null individually, then release
    if (in != null) {
        try {
            in.close();
        } catch (IOException e) {
            logger.error("Failed to close input stream", e);
        }
    }
    if (out != null) {
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Failed to close output stream", e);
        }
    }
}
```

### 3. Database Resources Management

**JDBC Resource Cleanup**
```java
// Automatic management with try-with-resources (recommended)
public List<User> findUsers(String name) throws SQLException {
    String sql = "SELECT * FROM users WHERE name = ?";
    List<User> users = new ArrayList<>();

    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setString(1, name);

        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapUser(rs));
            }
        }
    } catch (SQLException e) {
        logger.error("Database query failed", e);
        throw e;
    }

    return users;
}
```

### 4. Custom AutoCloseable Resources

**Implement AutoCloseable Interface**
```java
public class CustomResource implements AutoCloseable {
    private boolean closed = false;

    public CustomResource() {
        // Resource initialization
    }

    public void doWork() {
        if (closed) {
            throw new IllegalStateException("Resource is closed");
        }
        // Perform work
    }

    @Override
    public void close() {
        if (!closed) {
            // Resource release logic
            closed = true;
        }
    }
}

// Usage
try (CustomResource resource = new CustomResource()) {
    resource.doWork();
}  // close() is called automatically
```

## Vulnerable Code Examples

### Example 1: File Stream without Proper Cleanup

#### ❌ Vulnerable Code
```java
public void copyFile(File inputFile, File outputFile) {
    FileInputStream in = null;
    FileOutputStream out = null;

    try {
        in = new FileInputStream(inputFile);
        out = new FileOutputStream(outputFile);

        // File copy
        FileCopyUtils.copy(in, out);

        // Problem: If an error occurs before resource release, resources are not released
        // If FileCopyUtils.copy() throws an exception, the code below does not execute
        in.close();
        out.close();

    } catch (IOException e) {
        logger.error("File copy failed", e);
        // No close() in catch block either, causing resource leak
    }
}
```

**Problems**:
1. close() method is not called when exceptions occur
2. close() itself can throw IOException, requiring handling
3. When there are multiple resources, only some may be released

#### ✅ Secure Code - Try-with-Resources
```java
public void copyFile(File inputFile, File outputFile) {
    // Use try-with-resources (recommended)
    try (FileInputStream in = new FileInputStream(inputFile);
         FileOutputStream out = new FileOutputStream(outputFile)) {

        FileCopyUtils.copy(in, out);

    } catch (IOException e) {
        logger.error("File copy failed", e);
    }
    // close() is called automatically regardless of whether an exception occurred
}
```

#### ✅ Secure Code - Finally Block
```java
public void copyFileLegacy(File inputFile, File outputFile) {
    FileInputStream in = null;
    FileOutputStream out = null;

    try {
        in = new FileInputStream(inputFile);
        out = new FileOutputStream(outputFile);

        FileCopyUtils.copy(in, out);

    } catch (IOException e) {
        logger.error("File copy failed", e);
    } finally {
        // Release resources in finally block which always executes
        // Null check and exception handling for each resource
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                logger.error("Failed to close input stream", e);
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                logger.error("Failed to close output stream", e);
            }
        }
    }
}
```

### Example 2: Database Connection Leak

#### ❌ Vulnerable Code
```java
public User findUserById(int userId) {
    Connection conn = null;
    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
        conn = dataSource.getConnection();
        pstmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        pstmt.setInt(1, userId);
        rs = pstmt.executeQuery();

        if (rs.next()) {
            return mapUser(rs);
        }

        // close() is called only on the normal path
        rs.close();
        pstmt.close();
        conn.close();

    } catch (SQLException e) {
        logger.error("Database query failed", e);
        // Resources are not released when exceptions occur
    }

    return null;
}
```

**Problems**:
1. Connection, PreparedStatement, and ResultSet are not released when exceptions occur
2. When if (rs.next()) is false, close() is not called due to return null
3. Database connection pool exhaustion is possible

#### ✅ Secure Code
```java
public User findUserById(int userId) {
    String sql = "SELECT * FROM users WHERE id = ?";

    // Automatic management of all JDBC resources with try-with-resources
    try (Connection conn = dataSource.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {

        pstmt.setInt(1, userId);

        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return mapUser(rs);
            }
        }

    } catch (SQLException e) {
        logger.error("Database query failed", e);
    }

    return null;
}

// Using Spring JdbcTemplate (better approach)
public User findUserByIdSpring(int userId) {
    String sql = "SELECT * FROM users WHERE id = ?";

    try {
        return jdbcTemplate.queryForObject(sql,
            new Object[]{userId},
            this::mapUser);
    } catch (EmptyResultDataAccessException e) {
        return null;
    }
}
```

### Example 3: C File Handle Leak

#### ❌ Vulnerable Code - C
```c
void processFile(const char *filename) {
    FILE *f = fopen(filename, "r");

    if (f == NULL) {
        fprintf(stderr, "Cannot open file\n");
        return;
    }

    // File processing
    char buffer[1024];
    if (fgets(buffer, sizeof(buffer), f) == NULL) {
        fprintf(stderr, "Read error\n");
        return;  // Returns without closing file - resource leak!
    }

    if (checkSomething(buffer)) {
        return;  // Returns without closing file - resource leak!
    }

    processData(buffer);

    fclose(f);  // Only called on normal path
}
```

**Problems**:
1. fclose() not called on early return due to errors
2. File handle leak due to conditional return
3. File descriptor exhaustion on repeated calls

#### ✅ Secure Code - C
```c
void processFile(const char *filename) {
    FILE *f = fopen(filename, "r");
    int result = 0;

    if (f == NULL) {
        fprintf(stderr, "Cannot open file\n");
        return;
    }

    char buffer[1024];
    if (fgets(buffer, sizeof(buffer), f) == NULL) {
        fprintf(stderr, "Read error\n");
        fclose(f);  // Close file before returning
        return;
    }

    if (checkSomething(buffer)) {
        fclose(f);  // Close file before returning
        return;
    }

    processData(buffer);

    fclose(f);  // Close file on normal exit as well
}

// Better approach: goto-based cleanup pattern
void processFileBetter(const char *filename) {
    FILE *f = NULL;
    char buffer[1024];
    int result = 0;

    f = fopen(filename, "r");
    if (f == NULL) {
        fprintf(stderr, "Cannot open file\n");
        goto cleanup;
    }

    if (fgets(buffer, sizeof(buffer), f) == NULL) {
        fprintf(stderr, "Read error\n");
        goto cleanup;
    }

    if (checkSomething(buffer)) {
        goto cleanup;
    }

    processData(buffer);

cleanup:
    if (f != NULL) {
        fclose(f);
    }
}
```

### Example 4: C# File Stream Leak

#### ❌ Vulnerable Code - C#
```csharp
public void CopyFile(string sourcePath, string destPath) {
    FileStream fsSource = new FileStream(sourcePath,
        FileMode.Open, FileAccess.Read);
    FileStream fsDest = new FileStream(destPath,
        FileMode.Create, FileAccess.Write);

    byte[] bytes = new byte[fsSource.Length];
    int numBytesToRead = (int)fsSource.Length;
    int numBytesRead = 0;

    while (numBytesToRead > 0) {
        int n = fsSource.Read(bytes, numBytesRead, numBytesToRead);

        if (n == 0) {
            break;  // Returns without closing streams
        }

        fsDest.Write(bytes, numBytesRead, n);
        numBytesRead += n;
        numBytesToRead -= n;
    }

    // If an exception occurs, this code is never reached
    fsSource.Close();
    fsDest.Close();
}
```

#### ✅ Secure Code - C#
```csharp
// Using 'using' statement (recommended)
public void CopyFile(string sourcePath, string destPath) {
    using (FileStream fsSource = new FileStream(sourcePath,
           FileMode.Open, FileAccess.Read))
    using (FileStream fsDest = new FileStream(destPath,
           FileMode.Create, FileAccess.Write)) {

        byte[] bytes = new byte[fsSource.Length];
        int numBytesToRead = (int)fsSource.Length;
        int numBytesRead = 0;

        while (numBytesToRead > 0) {
            int n = fsSource.Read(bytes, numBytesRead, numBytesToRead);

            if (n == 0) {
                break;
            }

            fsDest.Write(bytes, numBytesRead, n);
            numBytesRead += n;
            numBytesToRead -= n;
        }
    }  // Dispose() is called automatically, closing the streams
}

// C# 8.0+ using declaration
public void CopyFileModern(string sourcePath, string destPath) {
    using var fsSource = new FileStream(sourcePath,
        FileMode.Open, FileAccess.Read);
    using var fsDest = new FileStream(destPath,
        FileMode.Create, FileAccess.Write);

    byte[] bytes = new byte[fsSource.Length];
    int numBytesToRead = (int)fsSource.Length;
    int numBytesRead = 0;

    while (numBytesToRead > 0) {
        int n = fsSource.Read(bytes, numBytesRead, numBytesToRead);

        if (n == 0) {
            break;
        }

        fsDest.Write(bytes, numBytesRead, n);
        numBytesRead += n;
        numBytesToRead -= n;
    }

    // Dispose() is called automatically when the method exits
}
```

### Example 5: Thread Pool and Executor Service Leak

#### ❌ Vulnerable Code
```java
public class TaskProcessor {
    public void processTasks(List<Task> tasks) {
        // ExecutorService is not released
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (Task task : tasks) {
            executor.submit(() -> processTask(task));
        }

        // No shutdown() call - threads continue running
        // New thread pool created on each method call causing resource leak
    }
}
```

**Problems**:
1. ExecutorService is not shut down, so threads keep running
2. A new thread pool is created on each method call
3. The JVM may not terminate when the application shuts down

#### ✅ Secure Code
```java
public class TaskProcessor {
    // Use singleton ExecutorService
    private static final ExecutorService executor =
        Executors.newFixedThreadPool(10);

    static {
        // Shutdown hook for automatic cleanup on JVM exit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }));
    }

    public void processTasks(List<Task> tasks) {
        for (Task task : tasks) {
            executor.submit(() -> processTask(task));
        }
    }

    // Method for one-time tasks
    public void processTasksOneTime(List<Task> tasks) {
        ExecutorService tempExecutor = Executors.newFixedThreadPool(10);

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Task task : tasks) {
                futures.add(tempExecutor.submit(() -> processTask(task)));
            }

            // Wait for all tasks to complete
            for (Future<?> future : futures) {
                future.get();
            }

        } catch (InterruptedException | ExecutionException e) {
            logger.error("Task processing failed", e);
        } finally {
            // Shut down ExecutorService after tasks complete
            tempExecutor.shutdown();
            try {
                if (!tempExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    tempExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                tempExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

### Example 6: Socket Connection Leak

#### ❌ Vulnerable Code
```java
public String fetchData(String host, int port) {
    Socket socket = null;
    BufferedReader reader = null;

    try {
        socket = new Socket(host, port);
        reader = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        return response.toString();

    } catch (IOException e) {
        logger.error("Network error", e);
        return null;
        // Socket and reader are not released when exceptions occur
    }
}
```

#### ✅ Secure Code
```java
public String fetchData(String host, int port) {
    // Automatic management with try-with-resources
    try (Socket socket = new Socket(host, port);
         BufferedReader reader = new BufferedReader(
             new InputStreamReader(socket.getInputStream()))) {

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        return response.toString();

    } catch (IOException e) {
        logger.error("Network error", e);
        return null;
    }
    // Socket and reader are automatically closed
}

// Using HttpClient (Java 11+) - better approach
public String fetchDataModern(String url) {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .build();

    try {
        HttpResponse<String> response = client.send(request,
            HttpResponse.BodyHandlers.ofString());
        return response.body();
    } catch (IOException | InterruptedException e) {
        logger.error("HTTP request failed", e);
        return null;
    }
}
```

## Detection Methods

### Static Analysis Patterns

#### Pattern 1: Resources without try-with-resources or finally
```bash
# Find file streams not using try-with-resources
grep -r "new FileInputStream\|new FileOutputStream" --include="*.java" . | \
grep -v "try ("

# Find resource allocations without finally blocks
grep -A 10 "new.*Stream\|new.*Connection" --include="*.java" . | \
grep -B 10 "} catch" | grep -v "finally"

# Find missing close() call patterns
grep -r "new.*Stream\|getConnection()" --include="*.java" . | \
grep -v "close()\|try ("
```

#### Pattern 2: Database resources without cleanup
```bash
# Find ResultSet, Statement without close() call patterns
grep -r "ResultSet\|Statement\|Connection" --include="*.java" . | \
grep -v "close()\|try ("

# Find JDBC code without try-with-resources
grep -r "getConnection()" --include="*.java" . | grep -v "try ("
```

#### Pattern 3: C/C++ resource leaks
```bash
# fopen without fclose
grep -r "fopen(" --include="*.c" --include="*.cpp" . | \
grep -v "fclose"

# malloc without free
grep -r "malloc\|calloc" --include="*.c" --include="*.cpp" . | \
grep -v "free("
```

### Dynamic Analysis

#### Resource Leak Detection with VisualVM
```bash
# JVM options for memory leak detection
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -jar application.jar

# Monitor file descriptors with JConsole
jconsole <pid>
# MBeans > java.lang > OperatingSystem > OpenFileDescriptorCount
```

#### LeakCanary for Android
```groovy
// build.gradle
dependencies {
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.10'
}
```

### Testing with Resource Limits

```java
@Test
public void testResourceLeak() throws Exception {
    // Set file descriptor limit
    for (int i = 0; i < 1000; i++) {
        service.processFile(testFile);
    }

    // If there is a resource leak, "Too many open files" error occurs
    // If normal, all iterations succeed
}

@Test
public void testConnectionPoolExhaustion() throws Exception {
    CountDownLatch latch = new CountDownLatch(100);

    // Attempt 100 simultaneous connections
    for (int i = 0; i < 100; i++) {
        new Thread(() -> {
            try {
                dao.findUser(1);
            } finally {
                latch.countDown();
            }
        }).start();
    }

    // Should complete without timeout
    assertTrue(latch.await(10, TimeUnit.SECONDS));
}
```

## Security Checklist

### Development Phase
- [ ] Always use try-with-resources in Java 7+ environments
- [ ] Implement idempotent close() methods when implementing AutoCloseable interface
- [ ] Check each resource for null individually and close() in finally blocks
- [ ] Include exception handling when calling close() methods
- [ ] Always call shutdown() on ExecutorService
- [ ] Use connection pools for database connections

### Code Review Phase
- [ ] Verify all file, network, and database resources are released
- [ ] Verify resources are released on exception paths as well
- [ ] Verify resource release on early returns
- [ ] Verify all resources are correctly released in nested try-catch blocks
- [ ] Verify all malloc/new are paired with free/delete in C/C++ code

### Testing Phase
- [ ] Resource leak testing (whether resources are exhausted on repeated calls)
- [ ] Resource release verification in exception scenarios
- [ ] Monitor file descriptors and connection counts under load testing
- [ ] Check heap memory leaks with memory profiler
- [ ] Run static analysis tools (SpotBugs, SonarQube)

### Production Monitoring
- [ ] Monitor file descriptor count
- [ ] Monitor database connection pool utilization
- [ ] Check JVM heap memory usage trends
- [ ] Monitor thread count
- [ ] Check network socket status (CLOSE_WAIT, TIME_WAIT)

## Best Practices

### 1. Prefer try-with-resources

```java
// Best approach: try-with-resources
public void processFile(Path path) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(path)) {
        String line;
        while ((line = reader.readLine()) != null) {
            processLine(line);
        }
    }
}

// Managing multiple resources
public void processMultipleFiles(Path input, Path output) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(input);
         BufferedWriter writer = Files.newBufferedWriter(output)) {

        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(processLine(line));
            writer.newLine();
        }
    }
}
```

### 2. Custom Resource Management

```java
// AutoCloseable implementation
public class DatabaseTransaction implements AutoCloseable {
    private final Connection connection;
    private boolean committed = false;

    public DatabaseTransaction(DataSource dataSource) throws SQLException {
        this.connection = dataSource.getConnection();
        this.connection.setAutoCommit(false);
    }

    public Connection getConnection() {
        return connection;
    }

    public void commit() throws SQLException {
        connection.commit();
        committed = true;
    }

    @Override
    public void close() throws SQLException {
        try {
            if (!committed) {
                connection.rollback();
            }
        } finally {
            connection.close();
        }
    }
}

// Usage
public void transferMoney(int fromId, int toId, BigDecimal amount)
        throws SQLException {
    try (DatabaseTransaction tx = new DatabaseTransaction(dataSource)) {
        Connection conn = tx.getConnection();

        debit(conn, fromId, amount);
        credit(conn, toId, amount);

        tx.commit();
    }
    // Automatic rollback on exception, commit then close on normal exit
}
```

### 3. Resource Factory Pattern

```java
public class ResourceManager {
    private final DataSource dataSource;

    public <T> T executeWithConnection(ConnectionCallback<T> callback)
            throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            return callback.doInConnection(conn);
        }
    }

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T doInConnection(Connection conn) throws SQLException;
    }
}

// Usage
ResourceManager manager = new ResourceManager(dataSource);

User user = manager.executeWithConnection(conn -> {
    try (PreparedStatement pstmt = conn.prepareStatement(
            "SELECT * FROM users WHERE id = ?")) {
        pstmt.setInt(1, userId);
        try (ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return mapUser(rs);
            }
        }
    }
    return null;
});
```

### 4. RAII Pattern in C++

```cpp
// Resource Acquisition Is Initialization
class FileHandle {
private:
    FILE* file;

public:
    FileHandle(const char* filename, const char* mode) {
        file = fopen(filename, mode);
        if (!file) {
            throw std::runtime_error("Cannot open file");
        }
    }

    ~FileHandle() {
        if (file) {
            fclose(file);
        }
    }

    // Prevent copying
    FileHandle(const FileHandle&) = delete;
    FileHandle& operator=(const FileHandle&) = delete;

    // Allow moving
    FileHandle(FileHandle&& other) noexcept : file(other.file) {
        other.file = nullptr;
    }

    FILE* get() { return file; }
};

// Usage
void processFile(const char* filename) {
    FileHandle file(filename, "r");

    char buffer[1024];
    if (fgets(buffer, sizeof(buffer), file.get())) {
        processData(buffer);
    }

    // fclose() is called automatically in the destructor regardless of exceptions
}
```

### 5. Connection Pool Best Practices

```java
// HikariCP configuration (recommended)
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("user");
        config.setPassword("password");

        // Pool size settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);

        // Connection testing
        config.setConnectionTestQuery("SELECT 1");

        // Timeout settings
        config.setConnectionTimeout(30000);  // 30 seconds
        config.setIdleTimeout(600000);       // 10 minutes
        config.setMaxLifetime(1800000);      // 30 minutes

        // Leak detection
        config.setLeakDetectionThreshold(60000);  // 1 minute

        return new HikariDataSource(config);
    }
}

// Leverage Spring's automatic resource management
@Repository
public class UserRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        // JdbcTemplate automatically manages connections
        return jdbcTemplate.queryForObject(sql,
            new Object[]{id},
            this::mapUser);
    }
}
```

### 6. Graceful Shutdown

```java
@Component
public class ApplicationLifecycleManager {

    private final ExecutorService executorService;
    private final DataSource dataSource;

    @Autowired
    public ApplicationLifecycleManager(
            ExecutorService executorService,
            DataSource dataSource) {
        this.executorService = executorService;
        this.dataSource = dataSource;
    }

    @PreDestroy
    public void onShutdown() {
        logger.info("Application shutdown initiated");

        // Shut down ExecutorService
        shutdownExecutorService();

        // Shut down DataSource (HikariCP)
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }

        logger.info("Application shutdown completed");
    }

    private void shutdownExecutorService() {
        executorService.shutdown();

        try {
            // Wait 60 seconds
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("ExecutorService did not terminate gracefully");

                // Force shutdown
                List<Runnable> droppedTasks = executorService.shutdownNow();
                logger.warn("Dropped {} tasks", droppedTasks.size());

                // Additional wait
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("ExecutorService did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

## Framework-Specific Guidance

### Spring Framework

```java
// Automatic resource management with @Transactional
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public void updateUser(User user) {
        // Spring automatically manages transactions and connections
        userRepository.save(user);
    }
}

// RestTemplate usage (automatic resource management)
@Service
public class ExternalApiService {

    @Autowired
    private RestTemplate restTemplate;

    public String fetchData(String url) {
        // RestTemplate automatically manages connections
        return restTemplate.getForObject(url, String.class);
    }
}
```

### Java EE / Jakarta EE

```java
// Resource management with CDI and @Resource
@Stateless
public class UserBean {

    @Resource(lookup = "java:jboss/datasources/ExampleDS")
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager em;

    public User findUser(int id) {
        // Container automatically manages EntityManager
        return em.find(User.class, id);
    }
}
```

## Tools and IDE Support

### IntelliJ IDEA Inspections
```
Settings > Editor > Inspections > Java > Probable bugs
- Resource opened but not safely closed
- I/O resource opened but not safely closed
- JDBC resource opened but not safely closed
```

### SpotBugs Rules
- OBL_UNSATISFIED_OBLIGATION: Method may fail to clean up stream or resource
- OS_OPEN_STREAM: Method may fail to close stream
- ODR_OPEN_DATABASE_RESOURCE: Method may fail to close database resource

### SonarQube Rules
- S2095: Resources should be closed
- S2093: Try-with-resources should be used
- S1168: Empty arrays and collections should be returned instead of null

## References

### Standards and Guidelines
- **CWE-404**: Improper Resource Shutdown or Release
  - https://cwe.mitre.org/data/definitions/404.html
- **CWE-772**: Missing Release of Resource after Effective Lifetime
  - https://cwe.mitre.org/data/definitions/772.html
- **CERT Oracle Secure Coding Standard for Java**
  - FIO04-J: Release resources when they are no longer needed
  - ERR05-J: Do not let checked exceptions escape from a finally block
- **OWASP**
  - Improper Resource Shutdown or Release
  - https://owasp.org/www-community/vulnerabilities/Improper_Resource_Shutdown_or_Release

### Tools
- **Static Analysis**
  - SpotBugs: https://spotbugs.github.io/
  - SonarQube: https://www.sonarqube.org/
  - Error Prone: https://errorprone.info/
  - Infer (Facebook): https://fbinfer.com/

- **Dynamic Analysis**
  - VisualVM: https://visualvm.github.io/
  - JProfiler: https://www.ej-technologies.com/products/jprofiler/overview.html
  - YourKit: https://www.yourkit.com/
  - LeakCanary (Android): https://square.github.io/leakcanary/

### Further Reading
- "Effective Java" by Joshua Bloch - Item 9: Prefer try-with-resources to try-finally
- "Java Concurrency in Practice" by Brian Goetz - Chapter 7: Cancellation and Shutdown
- Oracle Java Tutorials - The try-with-resources Statement
  - https://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html

---

**Related Vulnerabilities**: CWE-401 (Missing Release of Memory after Effective Lifetime), CWE-459 (Incomplete Cleanup)

**Last Updated**: 2026-02-05
