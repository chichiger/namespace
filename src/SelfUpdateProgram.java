import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

public class SelfUpdateProgram {

    private static final String UPDATE_URL = "http://localhost:8000/SelfUpdateProgram.jar";
    private static final String CURRENT_JAR = "/Users/chichigershyy/Documents/NameTag/src/SelfUpdateProgram.jar";

    private static Thread appThread = null;
    private static Timer timer = new Timer(true);

    private static final String CHECK_INTERVAL_PROPERTY = "check.interval"; // Interval in milliseconds
    private static final String DEFAULT_CHECK_INTERVAL = "60000"; // Default 1 minute

    public static void main(String[] args) {
        restartApplication(); // Start the application

        System.out.println("Application started. Checking for updates.Updated..");

        // Start an update thread to check for updates every checkInterval
        long checkInterval = Long.parseLong(System.getProperty(CHECK_INTERVAL_PROPERTY, DEFAULT_CHECK_INTERVAL)); // Default 1 minute

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkForUpdates();
                } catch (IOException e) {
                    System.err.println("Failed to check for updates: " + e.getMessage());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0, checkInterval);

        // Keep the application running
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            // If app thread has been terminated, just exit the program
            Thread.currentThread().interrupt();
        }
    }

    private static void checkForUpdates() throws IOException, InterruptedException {
        URL url = new URL(UPDATE_URL);
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false); // Disable caching

        long remoteFileLastModified = connection.getLastModified();
        File localFile = new File(CURRENT_JAR);

        if (!localFile.exists() || localFile.lastModified() < remoteFileLastModified) {
            System.out.println("Update available. Downloading...");
            downloadUpdate();
            timer.cancel(); // Stop the timer to prevent multiple updates
            restartApplication();
        } else {
            System.out.println("No update available.");
        }
    }

    private static void downloadUpdate() throws IOException {
        URL url = new URL(UPDATE_URL);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(CURRENT_JAR)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        System.out.println("Update downloaded.");
    }

    private static void restartApplication() {
        // Terminate existing appThread and start a new thread with the updated jar file
        try {
            if (appThread != null) {
                appThread.interrupt();
                appThread.join();
            }
            appThread = new Thread(() -> {
                try {
                    launchJar(CURRENT_JAR);
                } catch (IOException e) {
                    System.err.println("Failed to launch the application: " + e.getMessage());
                }
            });
            appThread.start();
        } catch (InterruptedException e) {
            System.err.println("Application terminated: " + e.getMessage());
        }
    }

    public static String getJarFilePath() throws IOException, URISyntaxException {
        // Get the URL of the class (this assumes the class is loaded from a JAR)
        URL location = SelfUpdateProgram.class.getProtectionDomain().getCodeSource().getLocation();

        if (location != null) {
            // Convert URL to file path
            File file = new File(location.toURI());

            // Check if the file is a JAR file and return its path
            if (file.exists()) {
                return file.getAbsolutePath();
            } else {
                throw new IOException("File does not exist: " + location);
            }
        } else {
            throw new IOException("Cannot determine the location of the JAR file.");
        }
    }

    private static void launchJar(String jarPath) throws IOException {
        // Ensure the JAR file exists
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IOException("JAR file not found: " + jarPath);
        }

        // Build the command
        ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-jar", jarFile.getAbsolutePath()
        );
        processBuilder.inheritIO(); // Optional: Inherit I/O from the current process

        // Start the process
        Process process = processBuilder.start();

        // Wait for the process to finish if needed
        try {
            int exitCode = process.waitFor();
            System.out.println("JAR process exited with code: " + exitCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Process interrupted");
        }
    }
}
