import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

public class SimpleHttpServer {

    public static void main(String[] args) throws IOException {
        // Create an HTTP server on port 8000
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);

        // Define a handler for the "/SelfUpdateProgram.jar" endpoint
        server.createContext("/SelfUpdateProgram.jar", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                // Path to the JAR file
                String jarFilePath = "/Users/chichigershyy/Documents/NameTag/src/SelfUpdateProgram.jar";
                FileInputStream fis = new FileInputStream(jarFilePath);

                // Send response headers
                exchange.sendResponseHeaders(200, 0);

                // Write the JAR file to the output stream
                OutputStream os = exchange.getResponseBody();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
                os.close();
            }
        });

        // Start the server
        server.setExecutor(null); // Use the default executor
        server.start();
        System.out.println("Server started at http://localhost:8000");
    }
}
