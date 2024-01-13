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

enum ContentType {
  TXT("text/plain");

  String type;

  ContentType(String type) {
    this.type = type;
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

class HttpResponse {
  ResponseCode responseCode;
  ContentType contentType;
  int contentLength;
  String body;

  private HttpResponse(ResponseCode responseCode, ContentType contentType, String body) {
    this.responseCode = responseCode;
    this.contentType = contentType;
    this.body = body;
    this.contentLength = body.length();
  }

  public static HttpResponse createOkResponse(ContentType contentType, String body) {
    return new HttpResponse(ResponseCode.OK, contentType, body);
  }

  public static HttpResponse createNotFoundResponse(ContentType contentType, String body) {
    return new HttpResponse(ResponseCode.NOT_FOUND, contentType, body);
  }

  public String getResponseString() {
    String headers = responseCode.getResponseLine() +
        "Content-Type: " + contentType.type + ResponseCode.CRLF +
        "Content-Length: " + contentLength + ResponseCode.CRLF +
        ResponseCode.CRLF;
    return headers + body;
  }
}

public class Main {

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    // Using try-with-resources to ensure both ServerSocket and Socket are closed
    // automatically
    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);
      // Accept a connection and also declare the clientSocket in the
      // try-with-resources
      try (Socket clientSocket = serverSocket.accept()) {
        System.out.println("Accepted new connection");
        // Handle the clientSocket here

        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);

        while (true) {
          String line = reader.readLine();
          HttpRequest request = HttpRequest.parseHttpRequest(line);

          if (request != null && request.path != null) {
            if (request.path.equals("/")) {
              HttpResponse response = HttpResponse.createOkResponse(ContentType.TXT, "Hello World!");
              writer.println(response.getResponseString());
            } else if (request.path.split("/")[1].equals("echo")) {
              String message = request.path.substring(request.path.indexOf("/echo/") + 6);
              HttpResponse response = HttpResponse.createOkResponse(ContentType.TXT, message);
              writer.println(response.getResponseString());
            } else {
              HttpResponse response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
              writer.println(response.getResponseString());
            }
          } else {
            // Handle the invalid request or send an error response
            HttpResponse response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
            writer.println(response.getResponseString());
          }

          break;
        }

      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }

  }
}
