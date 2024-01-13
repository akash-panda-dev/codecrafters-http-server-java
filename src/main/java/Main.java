import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    // Using try-with-resources to ensure both ServerSocket and Socket are closed automatically
    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);
      // Accept a connection and also declare the clientSocket in the try-with-resources
      try (Socket clientSocket = serverSocket.accept()) {
        System.out.println("Accepted new connection");
        // Handle the clientSocket here

        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);

        writer.println("HTTP/1.1 200 OK\r\n\r\n");

      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }


    
  }
}
