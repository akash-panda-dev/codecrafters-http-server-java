import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

enum ResponseCode {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found");

    int code;
    String message;
    static final String CRLF = "\r\n";

    ResponseCode(int code, String message) {
      this.code = code;
      this.message = message;
    }

    String getResponseLine() {
      return "HTTP/1.1 " + code + " " + message + CRLF;
    }
  }


class HttpRequest {
    String method;
    String path;
    String version;

    private HttpRequest(String method, String path, String version) {
      this.method = method;
      this.path = path;
      this.version = version;
    }

    public static HttpRequest parseHttpRequest(String requestFirstLine) {
      String[] parts = requestFirstLine.split(" ");
      if (parts.length != 3) {
        return null;
      }
      return new HttpRequest(parts[0], parts[1], parts[2]);
    }
  }

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


        while (true) {
          String line = reader.readLine();
          if (line == null || line.isEmpty()) {
            break;
          }
          HttpRequest request = HttpRequest.parseHttpRequest(line);

          if (request.path.equals("/")) {
            writer.println(ResponseCode.OK.getResponseLine());
          } else {
            writer.println(ResponseCode.NOT_FOUND.getResponseLine());
          }

          break;
        }

      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }


    
  }
}
