# Download of Code Without Integrity Check

**CWE-494: Download of Code Without Integrity Check**

## Overview

Download of Code Without Integrity Check is a security vulnerability that occurs when an application downloads executable code or libraries from a remote server without verifying their integrity. If an attacker replaces the code with malicious content through man-in-the-middle (MITM) attacks, DNS spoofing, or server compromise, the application executes it without verification, exposing the system to severe security threats. Integrity checks such as digital signature verification, checksum validation, and HTTPS usage are essential.

## Severity
- **CVSS v3.1 Score**: 8.1 (High)
- **Risk Level**: High
- **Impact**: Remote Code Execution, System Compromise

## Vulnerability Impact

### Attack Scenarios

#### Scenario 1: Malicious Code Injection via Man-in-the-Middle (MITM) Attack
```
1. Java application downloads a plugin JAR file over HTTP
2. Attacker intercepts network traffic via ARP spoofing on a public Wi-Fi network
3. Attacker detects the download request and modifies the response with a malicious JAR file
4. Application loads the malicious JAR without integrity verification
5. Malicious code executes through the class loader and gains system privileges
6. Result: Backdoor installation, data exfiltration, system compromise
```

#### Scenario 2: Malicious Server Redirect via DNS Spoofing
```
1. Application downloads an update module from update.example.com
2. Attacker changes update.example.com to a malicious server IP via DNS spoofing
3. Application downloads a forged update file from the malicious server
4. Update is installed without digital signature verification
5. Malicious code executes through the auto-update process
6. Result: Malware distribution to the entire user base
```

#### Scenario 3: Supply Chain Attack
```
1. A third-party library CDN server is compromised
2. Attacker replaces the legitimate library file with a backdoored version
3. Developer automatically downloads the library from the CDN during the build process
4. Library is included without checksum or signature verification
5. The built application is distributed to thousands of users
6. Result: Large-scale supply chain attack, widespread system compromise
```

#### Scenario 4: Plugin System Exploitation
```
1. A plugin-based application downloads plugins from user-specified URLs
2. Attacker tricks the user into entering a malicious plugin URL via phishing
3. Application downloads DLL/SO files from the malicious server
4. Native library is loaded without signature verification
5. Malicious native code bypasses memory protections and calls system APIs
6. Result: Privilege escalation, kernel-level rootkit installation
```

## Vulnerable Code Examples

### Java - Vulnerable Code (URLClassLoader Without Integrity Verification)

```java
// Vulnerable example: Loading remote JAR files without integrity verification
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;

public class VulnerablePluginLoader {

    /**
     * Vulnerable plugin loading
     *
     * Problems:
     * 1. Downloads over HTTP (MITM attack possible)
     * 2. No digital signature verification
     * 3. No checksum validation
     * 4. Executes untrusted code
     */
    public void loadPluginUnsafe(String pluginUrl) {
        try {
            // Vulnerability: Loading classes directly from HTTP URL
            URL[] classURLs = new URL[]{new URL(pluginUrl)};
            URLClassLoader loader = new URLClassLoader(classURLs);

            // Vulnerability: Loading and executing class without integrity verification
            Class<?> pluginClass = Class.forName("com.example.Plugin", true, loader);
            Object pluginInstance = pluginClass.getDeclaredConstructor().newInstance();

            // Execute plugin method
            Method initMethod = pluginClass.getMethod("initialize");
            initMethod.invoke(pluginInstance);

            System.out.println("Plugin loaded successfully");

            /*
             * Security risks:
             * - Attacker can inject malicious JAR via MITM
             * - Malicious code runs with application privileges
             * - All operations including file system access and network communication are possible
             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vulnerable auto-update
     */
    public void autoUpdateUnsafe(String updateServerUrl) {
        try {
            String updateUrl = updateServerUrl + "/latest/app-update.jar";

            // Vulnerability: Downloading update file over HTTP
            URL url = new URL(updateUrl);
            URLClassLoader loader = new URLClassLoader(new URL[]{url});

            // Vulnerability: Applying update without signature verification
            Class<?> updaterClass = loader.loadClass("com.example.Updater");
            Object updater = updaterClass.getDeclaredConstructor().newInstance();

            Method updateMethod = updaterClass.getMethod("applyUpdate");
            updateMethod.invoke(updater);

            System.out.println("Update applied successfully");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Usage example
    public static void main(String[] args) {
        VulnerablePluginLoader loader = new VulnerablePluginLoader();

        // Vulnerability: Loading remote code without verification
        loader.loadPluginUnsafe("http://plugins.example.com/myplugin.jar");

        // Vulnerability: Auto-updating without verification
        loader.autoUpdateUnsafe("http://updates.example.com");
    }
}
```

### Java - Vulnerable Code (File Download and Execution)

```java
// Vulnerable example: Downloading and executing file without integrity verification
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VulnerableFileDownloader {

    /**
     * Vulnerable file download and execution
     *
     * Problems:
     * 1. Uses HTTP (no encryption)
     * 2. No checksum verification
     * 3. No digital signature verification
     * 4. Immediately executes without file content verification
     */
    public void downloadAndExecuteUnsafe(String downloadUrl, String savePath) {
        try {
            // Vulnerability: Downloading executable file over HTTP
            URL url = new URL(downloadUrl);
            URLConnection connection = url.openConnection();

            // File download
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(savePath)) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("File downloaded: " + savePath);

            // Vulnerability: Immediately executing without integrity verification
            executeDownloadedFile(savePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Execute downloaded file
     */
    private void executeDownloadedFile(String filePath) {
        try {
            // Vulnerability: Executing external process without verification
            if (filePath.endsWith(".exe") || filePath.endsWith(".bat")) {
                // Windows executable
                Runtime.getRuntime().exec(filePath);
            } else if (filePath.endsWith(".sh")) {
                // Linux shell script
                Runtime.getRuntime().exec(new String[]{"sh", filePath});
            } else if (filePath.endsWith(".jar")) {
                // JAR file
                Runtime.getRuntime().exec(new String[]{"java", "-jar", filePath});
            }

            System.out.println("Executed: " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Vulnerable library download
     */
    public void downloadLibraryUnsafe(String libraryUrl) {
        try {
            URL url = new URL(libraryUrl);

            // Vulnerability: Downloading native library over HTTP
            String fileName = Paths.get(url.getPath()).getFileName().toString();
            Path downloadPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);

            try (InputStream in = url.openStream()) {
                Files.copy(in, downloadPath);
            }

            // Vulnerability: Loading native library without integrity verification
            System.load(downloadPath.toString());

            System.out.println("Native library loaded: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Usage example
    public static void main(String[] args) {
        VulnerableFileDownloader downloader = new VulnerableFileDownloader();

        // Vulnerability: Downloading and executing executable without verification
        downloader.downloadAndExecuteUnsafe(
            "http://downloads.example.com/update.exe",
            "C:\\temp\\update.exe"
        );

        // Vulnerability: Downloading and loading native library without verification
        downloader.downloadLibraryUnsafe("http://libs.example.com/native.dll");
    }
}
```

### C# - Vulnerable Code

```csharp
// Vulnerable example: Code download without integrity verification in C#
using System;
using System.IO;
using System.Net;
using System.Reflection;

public class VulnerableCodeDownloader
{
    /// <summary>
    /// Vulnerable assembly download and load
    ///
    /// Problems:
    /// - Uses HTTP (no encryption)
    /// - No digital signature verification
    /// - No hash verification
    /// </summary>
    public void LoadAssemblyFromUrlUnsafe(string assemblyUrl)
    {
        try
        {
            // Vulnerability: Downloading assembly over HTTP
            using (WebClient client = new WebClient())
            {
                byte[] assemblyData = client.DownloadData(assemblyUrl);

                // Vulnerability: Loading assembly without integrity verification
                Assembly assembly = Assembly.Load(assemblyData);

                // Find type in assembly and execute
                Type pluginType = assembly.GetType("MyPlugin.PluginClass");
                if (pluginType != null)
                {
                    object plugin = Activator.CreateInstance(pluginType);
                    MethodInfo method = pluginType.GetMethod("Execute");
                    method?.Invoke(plugin, null);
                }

                Console.WriteLine("Assembly loaded and executed");
            }

            /*
             * Security risks:
             * - Malicious assembly injection possible via MITM attack
             * - Malicious code runs with full system privileges
             */

        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex.Message}");
        }
    }

    /// <summary>
    /// Vulnerable file download and execution
    /// </summary>
    public void DownloadAndExecuteUnsafe(string fileUrl, string savePath)
    {
        try
        {
            // Vulnerability: Downloading executable over HTTP
            using (WebClient client = new WebClient())
            {
                client.DownloadFile(fileUrl, savePath);
            }

            Console.WriteLine($"Downloaded: {savePath}");

            // Vulnerability: Executing downloaded file without verification
            System.Diagnostics.Process.Start(savePath);

        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex.Message}");
        }
    }

    /// <summary>
    /// Vulnerable auto-update
    /// </summary>
    public void AutoUpdateUnsafe(string updateUrl)
    {
        try
        {
            string tempPath = Path.Combine(Path.GetTempPath(), "update.exe");

            // Vulnerability: Downloading update over HTTP
            using (WebClient client = new WebClient())
            {
                client.DownloadFile(updateUrl, tempPath);
            }

            // Vulnerability: Executing update without signature verification
            System.Diagnostics.Process.Start(tempPath);

            // Exit current application
            Environment.Exit(0);

        }
        catch (Exception ex)
        {
            Console.WriteLine($"Update failed: {ex.Message}");
        }
    }
}
```

### C - Vulnerable Code

```c
// Vulnerable example: File download without integrity verification in C
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>

/**
 * File write callback
 */
size_t write_data(void *ptr, size_t size, size_t nmemb, FILE *stream) {
    return fwrite(ptr, size, nmemb, stream);
}

/**
 * Vulnerable file download
 *
 * Problems:
 * - Uses HTTP (no encryption)
 * - No checksum verification
 * - No SSL certificate verification
 */
int download_file_unsafe(const char* url, const char* output_path) {
    CURL *curl;
    FILE *fp;
    CURLcode res;

    curl = curl_easy_init();
    if (!curl) {
        fprintf(stderr, "Failed to initialize CURL\n");
        return -1;
    }

    fp = fopen(output_path, "wb");
    if (!fp) {
        fprintf(stderr, "Failed to open file for writing: %s\n", output_path);
        curl_easy_cleanup(curl);
        return -1;
    }

    // Vulnerability: HTTP URL (no encryption)
    curl_easy_setopt(curl, CURLOPT_URL, url);

    // Vulnerability: SSL certificate verification disabled
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 0L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 0L);

    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_data);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, fp);

    // File download
    res = curl_easy_perform(curl);

    fclose(fp);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK) {
        fprintf(stderr, "Download failed: %s\n", curl_easy_strerror(res));
        return -1;
    }

    printf("File downloaded: %s\n", output_path);

    // Vulnerability: No checksum verification
    // Vulnerability: No digital signature verification

    return 0;
}

/**
 * Vulnerable library download and load
 */
int download_and_load_library_unsafe(const char* lib_url, const char* lib_path) {
    // Vulnerability: Downloading library without integrity verification
    if (download_file_unsafe(lib_url, lib_path) != 0) {
        return -1;
    }

    // Vulnerability: Loading dynamic library without verification
#ifdef _WIN32
    HMODULE lib = LoadLibrary(lib_path);
    if (!lib) {
        fprintf(stderr, "Failed to load library\n");
        return -1;
    }
#else
    void* lib = dlopen(lib_path, RTLD_LAZY);
    if (!lib) {
        fprintf(stderr, "Failed to load library: %s\n", dlerror());
        return -1;
    }
#endif

    printf("Library loaded: %s\n", lib_path);

    /*
     * Security risks:
     * - Attacker can replace with malicious library
     * - Native code execution can lead to full system compromise
     */

    return 0;
}

/**
 * Vulnerable update download and execution
 */
int auto_update_unsafe(const char* update_url) {
    const char* update_path = "/tmp/update.sh";

    // Vulnerability: Downloading update script over HTTP
    if (download_file_unsafe(update_url, update_path) != 0) {
        return -1;
    }

    // Vulnerability: Executing script without integrity verification
    char command[256];
    snprintf(command, sizeof(command), "sh %s", update_path);

    int result = system(command);

    if (result == 0) {
        printf("Update completed successfully\n");
    } else {
        fprintf(stderr, "Update failed\n");
    }

    return result;
}

int main() {
    // Vulnerable usage examples

    // Vulnerability: Downloading file without verification
    download_file_unsafe("http://downloads.example.com/plugin.so", "/tmp/plugin.so");

    // Vulnerability: Downloading and loading library without verification
    download_and_load_library_unsafe("http://libs.example.com/libmalicious.so", "/tmp/lib.so");

    // Vulnerability: Executing update without verification
    auto_update_unsafe("http://updates.example.com/update.sh");

    return 0;
}

/*
 * Compilation:
 * gcc -o vulnerable_download vulnerable_download.c -lcurl -ldl
 */
```

## Secure Code Examples

### Java - Secure Code (Digital Signature Verification)

```java
// Secure example: Digital signature verification and HTTPS usage
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Secure code downloader
 *
 * Security features:
 * - HTTPS usage (encrypted transmission)
 * - Digital signature verification
 * - Checksum validation
 * - Trusted certificate chain verification
 */
public class SecureCodeDownloader {

    private PublicKey trustedPublicKey;

    public SecureCodeDownloader() {
        loadTrustedPublicKey();
    }

    /**
     * Load trusted public key
     */
    private void loadTrustedPublicKey() {
        try {
            // Load public key from trusted certificate embedded in the application
            InputStream certStream = getClass().getResourceAsStream("/trusted-cert.pem");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert = cf.generateCertificate(certStream);
            trustedPublicKey = cert.getPublicKey();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load trusted public key", e);
        }
    }

    /**
     * Secure JAR file download and load
     *
     * @param jarUrl HTTPS URL
     * @param expectedChecksum SHA-256 checksum
     * @return Path of downloaded JAR file
     */
    public Path downloadJarSecurely(String jarUrl, String expectedChecksum)
            throws IOException, GeneralSecurityException {

        // 1. Verify HTTPS connection
        if (!jarUrl.startsWith("https://")) {
            throw new SecurityException("Only HTTPS URLs are allowed");
        }

        // 2. Download JAR file
        Path downloadPath = downloadFile(jarUrl);

        // 3. Checksum verification
        if (!verifyChecksum(downloadPath, expectedChecksum)) {
            Files.deleteIfExists(downloadPath);
            throw new SecurityException("Checksum verification failed");
        }

        // 4. Digital signature verification
        if (!verifyJarSignature(downloadPath)) {
            Files.deleteIfExists(downloadPath);
            throw new SecurityException("Digital signature verification failed");
        }

        System.out.println("JAR file verified successfully");
        return downloadPath;
    }

    /**
     * Download file over HTTPS
     */
    private Path downloadFile(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        // Enforce SSL/TLS settings
        connection.setSSLSocketFactory(getSecureSSLSocketFactory());

        String fileName = Paths.get(url.getPath()).getFileName().toString();
        Path downloadPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);

        try (InputStream in = connection.getInputStream();
             OutputStream out = Files.newOutputStream(downloadPath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return downloadPath;
    }

    /**
     * SHA-256 checksum verification
     */
    private boolean verifyChecksum(Path filePath, String expectedChecksum)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        String actualChecksum = bytesToHex(hashBytes);

        return actualChecksum.equalsIgnoreCase(expectedChecksum);
    }

    /**
     * JAR file digital signature verification
     */
    private boolean verifyJarSignature(Path jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile(), true)) {

            // Read all entries in JAR to trigger signature verification
            java.util.Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // Skip directories and metadata files
                if (entry.isDirectory() || entry.getName().startsWith("META-INF/")) {
                    continue;
                }

                // Read entry (triggers signature verification)
                try (InputStream is = jarFile.getInputStream(entry)) {
                    byte[] buffer = new byte[8192];
                    while (is.read(buffer) != -1) {
                        // Reading file content (signature verification)
                    }
                }

                // Signature verification
                Certificate[] certs = entry.getCertificates();
                if (certs == null || certs.length == 0) {
                    System.err.println("No signature found for: " + entry.getName());
                    return false;
                }

                // Verify signature with trusted public key
                if (!verifyCertificates(certs)) {
                    System.err.println("Invalid certificate for: " + entry.getName());
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Certificate chain verification
     */
    private boolean verifyCertificates(Certificate[] certs) {
        // Compare with trusted public key
        for (Certificate cert : certs) {
            if (cert.getPublicKey().equals(trustedPublicKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert byte array to hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Get security-hardened SSLSocketFactory
     */
    private javax.net.ssl.SSLSocketFactory getSecureSSLSocketFactory() {
        // Use TLS 1.2 or higher, disable weak cipher suites, etc.
        // Actual implementation required
        return (javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault();
    }

    // Usage example
    public static void main(String[] args) {
        try {
            SecureCodeDownloader downloader = new SecureCodeDownloader();

            // HTTPS URL and expected checksum
            String jarUrl = "https://secure-downloads.example.com/plugin.jar";
            String expectedChecksum = "a3c5f2e9b1d4c7e8f0a2b5d8c1e4f7a9b2c5d8e1f4a7b0c3d6e9f2a5b8c1d4e7";

            // Secure download and verification
            Path jarPath = downloader.downloadJarSecurely(jarUrl, expectedChecksum);

            System.out.println("JAR file safely downloaded and verified: " + jarPath);

            // Now the verified JAR can be loaded safely
            // Load using URLClassLoader

        } catch (Exception e) {
            System.err.println("Failed to download or verify JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Java - Secure Code (Checksum Verification)

```java
// Secure example: Checksum-based integrity verification
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;

/**
 * Checksum-based secure downloader
 */
public class ChecksumVerifiedDownloader {

    /**
     * Secure file download (with checksum verification)
     *
     * @param fileUrl HTTPS URL
     * @param checksumUrl Checksum file URL (filename.sha256)
     * @param savePath Save path
     */
    public boolean downloadWithChecksumVerification(String fileUrl,
                                                     String checksumUrl,
                                                     String savePath)
            throws IOException, NoSuchAlgorithmException {

        // 1. Verify HTTPS
        if (!fileUrl.startsWith("https://") || !checksumUrl.startsWith("https://")) {
            throw new SecurityException("Only HTTPS URLs are allowed");
        }

        // 2. Download expected checksum
        String expectedChecksum = downloadChecksum(checksumUrl);

        // 3. Download file
        downloadFile(fileUrl, savePath);

        // 4. Calculate checksum of downloaded file
        String actualChecksum = calculateFileChecksum(savePath);

        // 5. Compare checksums
        if (!actualChecksum.equalsIgnoreCase(expectedChecksum)) {
            // Checksum mismatch - delete file
            Files.deleteIfExists(Paths.get(savePath));
            throw new SecurityException("Checksum mismatch! File may be corrupted or tampered.");
        }

        System.out.println("File downloaded and verified successfully");
        return true;
    }

    /**
     * Download checksum file
     */
    private String downloadChecksum(String checksumUrl) throws IOException {
        URL url = new URL(checksumUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            String line = reader.readLine();
            if (line != null) {
                // Checksum file format: "checksum filename" or "checksum"
                return line.split("\\s+")[0];
            }

            throw new IOException("Empty checksum file");
        }
    }

    /**
     * Download file over HTTPS
     */
    private void downloadFile(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        try (InputStream in = connection.getInputStream();
             FileOutputStream out = new FileOutputStream(savePath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * Calculate SHA-256 checksum of file
     */
    private String calculateFileChecksum(String filePath)
            throws IOException, NoSuchAlgorithmException {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();

        // Convert to hexadecimal string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    // Usage example
    public static void main(String[] args) {
        try {
            ChecksumVerifiedDownloader downloader = new ChecksumVerifiedDownloader();

            // File and checksum URLs
            String fileUrl = "https://downloads.example.com/app-1.0.0.jar";
            String checksumUrl = "https://downloads.example.com/app-1.0.0.jar.sha256";
            String savePath = "/tmp/app-1.0.0.jar";

            // Download with checksum verification
            boolean success = downloader.downloadWithChecksumVerification(
                fileUrl, checksumUrl, savePath
            );

            if (success) {
                System.out.println("File is authentic and can be used safely");
            }

        } catch (Exception e) {
            System.err.println("Download or verification failed: " + e.getMessage());
        }
    }
}

/*
 * Checksum file example (app-1.0.0.jar.sha256):
 * a3c5f2e9b1d4c7e8f0a2b5d8c1e4f7a9b2c5d8e1f4a7b0c3d6e9f2a5b8c1d4e7  app-1.0.0.jar
 *
 * Or checksum only:
 * a3c5f2e9b1d4c7e8f0a2b5d8c1e4f7a9b2c5d8e1f4a7b0c3d6e9f2a5b8c1d4e7
 */
```

### C# - Secure Code

```csharp
// Secure example: Digital signature and checksum verification in C#
using System;
using System.IO;
using System.Net;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

/// <summary>
/// Secure code downloader
/// </summary>
public class SecureCodeDownloader
{
    private X509Certificate2 trustedCertificate;

    public SecureCodeDownloader(string trustedCertPath)
    {
        // Load trusted certificate
        trustedCertificate = new X509Certificate2(trustedCertPath);
    }

    /// <summary>
    /// Secure file download (with checksum verification)
    /// </summary>
    public bool DownloadWithIntegrityCheck(string fileUrl, string checksumUrl, string savePath)
    {
        try
        {
            // 1. Verify HTTPS
            if (!fileUrl.StartsWith("https://", StringComparison.OrdinalIgnoreCase))
            {
                throw new SecurityException("Only HTTPS URLs are allowed");
            }

            // 2. Download expected checksum
            string expectedChecksum = DownloadChecksum(checksumUrl);

            // 3. Download file
            DownloadFile(fileUrl, savePath);

            // 4. Checksum verification
            if (!VerifyFileChecksum(savePath, expectedChecksum))
            {
                File.Delete(savePath);
                throw new SecurityException("Checksum verification failed");
            }

            Console.WriteLine("File downloaded and verified successfully");
            return true;
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error: {ex.Message}");
            return false;
        }
    }

    /// <summary>
    /// Download file over HTTPS
    /// </summary>
    private void DownloadFile(string url, string savePath)
    {
        using (WebClient client = new WebClient())
        {
            // Use TLS 1.2 or higher
            ServicePointManager.SecurityProtocol = SecurityProtocolType.Tls12 | SecurityProtocolType.Tls13;

            client.DownloadFile(url, savePath);
        }
    }

    /// <summary>
    /// Download checksum
    /// </summary>
    private string DownloadChecksum(string url)
    {
        using (WebClient client = new WebClient())
        {
            string content = client.DownloadString(url);
            return content.Split(' ')[0].Trim();
        }
    }

    /// <summary>
    /// File checksum verification (SHA-256)
    /// </summary>
    private bool VerifyFileChecksum(string filePath, string expectedChecksum)
    {
        using (FileStream stream = File.OpenRead(filePath))
        using (SHA256 sha256 = SHA256.Create())
        {
            byte[] hashBytes = sha256.ComputeHash(stream);
            string actualChecksum = BitConverter.ToString(hashBytes).Replace("-", "").ToLowerInvariant();

            return actualChecksum.Equals(expectedChecksum, StringComparison.OrdinalIgnoreCase);
        }
    }

    /// <summary>
    /// Digital signature verification
    /// </summary>
    public bool VerifyFileSignature(string filePath, string signaturePath)
    {
        try
        {
            // Read file data
            byte[] fileData = File.ReadAllBytes(filePath);

            // Read signature data
            byte[] signature = File.ReadAllBytes(signaturePath);

            // Verify signature with RSA public key
            using (RSA rsa = trustedCertificate.GetRSAPublicKey())
            {
                return rsa.VerifyData(fileData, signature, HashAlgorithmName.SHA256, RSASignaturePadding.Pkcs1);
            }
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Signature verification failed: {ex.Message}");
            return false;
        }
    }

    // Usage example
    public static void Example()
    {
        string certPath = "trusted-cert.cer";
        SecureCodeDownloader downloader = new SecureCodeDownloader(certPath);

        // Download with checksum verification
        bool success = downloader.DownloadWithIntegrityCheck(
            "https://downloads.example.com/app.exe",
            "https://downloads.example.com/app.exe.sha256",
            "C:\\Downloads\\app.exe"
        );

        if (success)
        {
            // Digital signature verification
            bool signatureValid = downloader.VerifyFileSignature(
                "C:\\Downloads\\app.exe",
                "C:\\Downloads\\app.exe.sig"
            );

            if (signatureValid)
            {
                Console.WriteLine("File is authentic and can be executed safely");
            }
        }
    }
}
```

### C - Secure Code

```c
// Secure example: Checksum verification and HTTPS usage in C
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <curl/curl.h>
#include <openssl/sha.h>

#define MAX_CHECKSUM_LENGTH 65

/**
 * File write callback
 */
size_t write_callback(void *ptr, size_t size, size_t nmemb, FILE *stream) {
    return fwrite(ptr, size, nmemb, stream);
}

/**
 * Secure file download (HTTPS + SSL verification)
 */
int download_file_secure(const char* url, const char* output_path) {
    CURL *curl;
    FILE *fp;
    CURLcode res;

    curl = curl_easy_init();
    if (!curl) {
        fprintf(stderr, "Failed to initialize CURL\n");
        return -1;
    }

    fp = fopen(output_path, "wb");
    if (!fp) {
        fprintf(stderr, "Failed to open file: %s\n", output_path);
        curl_easy_cleanup(curl);
        return -1;
    }

    // Set HTTPS URL
    curl_easy_setopt(curl, CURLOPT_URL, url);

    // Enforce SSL/TLS settings
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);  // Verify server certificate
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);  // Verify hostname
    curl_easy_setopt(curl, CURLOPT_SSLVERSION, CURL_SSLVERSION_TLSv1_2); // TLS 1.2 or higher

    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, write_callback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, fp);

    res = curl_easy_perform(curl);

    fclose(fp);
    curl_easy_cleanup(curl);

    if (res != CURLE_OK) {
        fprintf(stderr, "Download failed: %s\n", curl_easy_strerror(res));
        return -1;
    }

    printf("File downloaded securely: %s\n", output_path);
    return 0;
}

/**
 * Calculate SHA-256 checksum of file
 */
int calculate_file_checksum(const char* filepath, char* checksum_out) {
    FILE *file = fopen(filepath, "rb");
    if (!file) {
        fprintf(stderr, "Failed to open file: %s\n", filepath);
        return -1;
    }

    SHA256_CTX sha256;
    SHA256_Init(&sha256);

    unsigned char buffer[8192];
    size_t bytes_read;

    while ((bytes_read = fread(buffer, 1, sizeof(buffer), file)) > 0) {
        SHA256_Update(&sha256, buffer, bytes_read);
    }

    unsigned char hash[SHA256_DIGEST_LENGTH];
    SHA256_Final(hash, &sha256);

    fclose(file);

    // Convert to hexadecimal string
    for (int i = 0; i < SHA256_DIGEST_LENGTH; i++) {
        sprintf(checksum_out + (i * 2), "%02x", hash[i]);
    }
    checksum_out[SHA256_DIGEST_LENGTH * 2] = '\0';

    return 0;
}

/**
 * Checksum verification
 */
int verify_file_integrity(const char* filepath, const char* expected_checksum) {
    char actual_checksum[MAX_CHECKSUM_LENGTH];

    if (calculate_file_checksum(filepath, actual_checksum) != 0) {
        return 0;
    }

    // Case-insensitive comparison
    if (strcasecmp(actual_checksum, expected_checksum) == 0) {
        printf("Checksum verified successfully\n");
        return 1;
    } else {
        fprintf(stderr, "Checksum mismatch!\n");
        fprintf(stderr, "Expected: %s\n", expected_checksum);
        fprintf(stderr, "Actual:   %s\n", actual_checksum);
        return 0;
    }
}

/**
 * Secure download and verification
 */
int download_with_integrity_check(const char* file_url,
                                    const char* checksum_url,
                                    const char* save_path) {

    char checksum_path[256];
    snprintf(checksum_path, sizeof(checksum_path), "%s.sha256", save_path);

    // 1. Download checksum file
    if (download_file_secure(checksum_url, checksum_path) != 0) {
        fprintf(stderr, "Failed to download checksum file\n");
        return -1;
    }

    // 2. Read expected checksum
    FILE *checksum_file = fopen(checksum_path, "r");
    if (!checksum_file) {
        fprintf(stderr, "Failed to open checksum file\n");
        return -1;
    }

    char expected_checksum[MAX_CHECKSUM_LENGTH];
    if (fscanf(checksum_file, "%64s", expected_checksum) != 1) {
        fprintf(stderr, "Failed to read checksum\n");
        fclose(checksum_file);
        return -1;
    }
    fclose(checksum_file);

    // 3. Download file
    if (download_file_secure(file_url, save_path) != 0) {
        fprintf(stderr, "Failed to download file\n");
        return -1;
    }

    // 4. Integrity verification
    if (!verify_file_integrity(save_path, expected_checksum)) {
        fprintf(stderr, "Integrity check failed - deleting file\n");
        remove(save_path);
        return -1;
    }

    printf("File downloaded and verified successfully\n");
    return 0;
}

int main() {
    // Secure usage example

    const char* file_url = "https://secure-downloads.example.com/plugin.so";
    const char* checksum_url = "https://secure-downloads.example.com/plugin.so.sha256";
    const char* save_path = "/tmp/plugin.so";

    // HTTPS + checksum verification
    if (download_with_integrity_check(file_url, checksum_url, save_path) == 0) {
        printf("File is safe to use\n");
        // Now the verified file can be loaded safely
    } else {
        fprintf(stderr, "Download or verification failed\n");
    }

    return 0;
}

/*
 * Compilation:
 * gcc -o secure_download secure_download.c -lcurl -lssl -lcrypto
 */
```

## Security Best Practices

### 1. Transport Security

```
Required:
✓ Use HTTPS (TLS 1.2 or higher)
✓ SSL/TLS certificate verification
✓ Hostname verification
✓ Strong cipher suites

Prohibited:
✗ Using HTTP
✗ Disabling SSL verification
✗ Unconditionally trusting self-signed certificates
✗ Legacy TLS versions (TLS 1.0, 1.1)
```

### 2. Integrity Verification

```
Recommended methods (in order of priority):

1. Digital Signature Verification
   - Public key cryptography based
   - Asymmetric encryption (RSA, ECDSA)
   - X.509 certificate chain verification
   - Strongest security

2. Checksum Verification
   - SHA-256, SHA-512
   - MD5, SHA-1 must NOT be used (weak)
   - Checksums must also be transmitted over HTTPS
   - Can detect tampering

3. HMAC Verification
   - Shared secret key based
   - Server-client authentication
   - Message Authentication Code
```

### 3. Code Loading Security

```java
// Secure class loader usage
public class SecureClassLoader extends URLClassLoader {

    public SecureClassLoader(URL[] urls) {
        super(urls);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // Only load from allowed packages
        if (!isAllowedPackage(name)) {
            throw new SecurityException("Unauthorized package: " + name);
        }

        return super.findClass(name);
    }

    private boolean isAllowedPackage(String className) {
        // Whitelist-based verification
        String[] allowedPackages = {"com.mycompany.plugins."};

        for (String pkg : allowedPackages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }

        return false;
    }
}
```

## Detection and Prevention

### Static Analysis Rules

```bash
# SonarQube Rules
- java:S4792: Warning on HTTP usage
- java:S4830: Warning on SSL/TLS verification disabled

# SpotBugs Rules
- URLCONNECTION_SSRF_FD: URL connection without verification
- WEAK_TRUST_MANAGER: Weak TrustManager usage

# Checkstyle (Custom)
# Verify validation when using URLClassLoader, Class.forName
```

### Code Review Checklist

```
[] Using HTTPS? (HTTP prohibited)
[] SSL/TLS certificate verification enabled?
[] Digital signature or checksum verification?
[] Downloading only from trusted sources?
[] File deleted on download failure?
[] Code execution prevented on verification failure?
[] Timeout configured?
[] Error handling appropriate?
```

## Testing Methods

### Unit Tests

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecureDownloaderTest {

    @Test
    void testHttpUrlRejected() {
        SecureCodeDownloader downloader = new SecureCodeDownloader();

        // HTTP URL should be rejected
        assertThrows(SecurityException.class, () -> {
            downloader.downloadJarSecurely("http://example.com/file.jar", "checksum");
        });
    }

    @Test
    void testChecksumVerification() throws Exception {
        ChecksumVerifiedDownloader downloader = new ChecksumVerifiedDownloader();

        String testFile = createTestFile("test content");
        String actualChecksum = calculateChecksum(testFile);

        // Correct checksum
        assertTrue(verifyChecksum(testFile, actualChecksum));

        // Wrong checksum
        assertFalse(verifyChecksum(testFile, "invalid-checksum"));
    }

    @Test
    void testInvalidChecksumDeletesFile() {
        // Verify file is deleted on checksum mismatch
        Path testFile = createTempFile();

        try {
            downloadWithWrongChecksum(testFile);
        } catch (SecurityException e) {
            // Expected exception
        }

        assertFalse(Files.exists(testFile), "File should be deleted on checksum mismatch");
    }
}
```

### Integration Tests

```java
@Test
void testEndToEndSecureDownload() throws Exception {
    // Set up test server (HTTPS)
    MockHttpsServer server = new MockHttpsServer();
    server.addFile("/plugin.jar", getTestJarBytes());
    server.addFile("/plugin.jar.sha256", getTestChecksum());
    server.start();

    SecureCodeDownloader downloader = new SecureCodeDownloader();

    // Secure download
    Path downloadedFile = downloader.downloadJarSecurely(
        server.getUrl() + "/plugin.jar",
        getTestChecksum()
    );

    assertTrue(Files.exists(downloadedFile));
    assertTrue(verifyJarSignature(downloadedFile));

    server.stop();
}
```

## Related Vulnerabilities

- **CWE-829**: Inclusion of Functionality from Untrusted Control Sphere
- **CWE-494**: Download of Code Without Integrity Check
- **CWE-830**: Inclusion of Web Functionality from an Untrusted Source
- **CWE-353**: Missing Support for Integrity Check

## References

### Standards and Guides
- OWASP Top 10 2021: A08 - Software and Data Integrity Failures
- CWE-494: https://cwe.mitre.org/data/definitions/494.html
- NIST SP 800-161: Supply Chain Risk Management

### Tools
- jarsigner: JAR file signing and verification (built into JDK)
- signtool: Windows executable signing tool
- GPG: File signing and verification

## Checklist

### Development Phase
- [ ] HTTPS usage (HTTP prohibited)
- [ ] SSL/TLS certificate verification enabled
- [ ] Digital signature or checksum verification implemented
- [ ] File deleted on verification failure
- [ ] Trusted certificate chain verification
- [ ] Error handling and logging

### Testing Phase
- [ ] HTTP URL rejection test
- [ ] Checksum verification test
- [ ] Digital signature verification test
- [ ] MITM attack scenario test
- [ ] File deletion on verification failure confirmed

### Deployment Phase
- [ ] Production certificate configured
- [ ] Checksum files distributed
- [ ] Trusted download server configured
- [ ] Monitoring and alerting configured

---

**Last Updated**: 2025-02-05
