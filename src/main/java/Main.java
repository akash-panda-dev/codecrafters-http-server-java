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

  @Override
  public String toString() {
    return code + " " + message + CRLF;
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
  String userAgent;
  String host;

  private HttpRequest(String method, String path, String version, String userAgent, String host) {
    this.method = method;
    this.path = path;
    this.version = version;
    this.userAgent = userAgent;
    this.host = host;
  }

  public static HttpRequest parseHttpRequest(BufferedReader reader) throws IOException {
    String requestLine = reader.readLine();
    String[] parts = requestLine.split(" ");
    if (parts.length != 3) {
      return null;
    }
    String method = parts[0];
    String path = parts[1];
    String version = parts[2];
    String userAgent = "";
    String host = "";

    String headerLine;
    while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
      if (headerLine.toLowerCase().startsWith("user-agent:")) {
        userAgent = headerLine.substring("User-Agent:".length()).trim();
      } else if (headerLine.toLowerCase().startsWith("host:")) {
        host = headerLine.substring("Host:".length()).trim();
      }
    }

    return new HttpRequest(method, path, version, userAgent, host);
  }
}

class HttpResponse {
  ResponseCode responseCode;
  ContentType contentType;
  int contentLength;
  String version;
  String body;

  private HttpResponse(ResponseCode responseCode, ContentType contentType, String body) {
    this.responseCode = responseCode;
    this.contentType = contentType;
    this.body = body;
    this.contentLength = body.length();
    this.version = "HTTP/1.1";
  }

  public static HttpResponse createOkResponse(ContentType contentType, String body) {
    return new HttpResponse(ResponseCode.OK, contentType, body);
  }

  public static HttpResponse createNotFoundResponse(ContentType contentType, String body) {
    return new HttpResponse(ResponseCode.NOT_FOUND, contentType, body);
  }

  public String getResponseString() {
    String headers = version + " " + responseCode.toString() +
        "Content-Type: " + contentType.type + ResponseCode.CRLF +
        "Content-Length: " + contentLength + ResponseCode.CRLF +
        ResponseCode.CRLF;
    return headers + body;
  }
}

public class Main {

  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);

      try (Socket clientSocket = serverSocket.accept()) {
        System.out.println("Accepted new connection");

        InputStream inputStream = clientSocket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStream outputStream = clientSocket.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true);

        while (true) {
          HttpRequest request = HttpRequest.parseHttpRequest(reader);
          HttpResponse response;

          if (request != null && request.path != null) {
            if (request.path.equals("/")) {
              response = HttpResponse.createOkResponse(ContentType.TXT, "Hello World!");
              writer.println(response.getResponseString());
            } else if (request.path.split("/")[1].equals("echo")) {
              String message = request.path.substring(request.path.indexOf("/echo/") + 6);
              response = HttpResponse.createOkResponse(ContentType.TXT, message);
              writer.println(response.getResponseString());
            } else if (request.path.split("/")[1].equals("user-agent")) {
              response = HttpResponse.createOkResponse(ContentType.TXT, request.userAgent);
              writer.println(response.getResponseString());
            } else {
              response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
              writer.println(response.getResponseString());
            }
          } else {
            // Handle the invalid request or send an error response
            response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
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
