import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

enum ResponseCode {
  OK(200, "OK"),
  NOT_FOUND(404, "Not Found"),
  CREATED(201, "Created");

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
  TXT("text/plain"),
  OCTET("application/octet-stream");

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
  String body;

  private HttpRequest(String method, String path, String version, String userAgent, String host, String body) {
    this.method = method;
    this.path = path;
    this.version = version;
    this.userAgent = userAgent;
    this.host = host;
    this.body = body;
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
    int contentLength = 0;
    String body = null;

    String headerLine;
    while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
      if (headerLine.toLowerCase().startsWith("user-agent:")) {
        userAgent = headerLine.substring("User-Agent:".length()).trim();
      } else if (headerLine.toLowerCase().startsWith("host:")) {
        host = headerLine.substring("Host:".length()).trim();
      } else if (headerLine.toLowerCase().startsWith("content-length:")) {
        contentLength = Integer.parseInt(headerLine.substring("Content-Length:".length()).trim());
      }
    }

    if ("POST".equalsIgnoreCase(method) && contentLength > 0) {
      char[] buffer = new char[contentLength];
      int bytesRead = reader.read(buffer, 0, contentLength);
      if (bytesRead > 0) {
        body = new String(buffer, 0, bytesRead);
      }
    }

    return new HttpRequest(method, path, version, userAgent, host, body);
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

  public static HttpResponse createCreatedResponse(ContentType contentType, String body) {
    return new HttpResponse(ResponseCode.CREATED, contentType, body);
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

  private static void handleRequest(Socket socket, Set<String> filesInDir, String directory) throws RuntimeException {
    try {
      System.out.println("Accepted new connection from " + socket.getRemoteSocketAddress());

      InputStream inputStream = socket.getInputStream();
      System.out.println("Got input stream");
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      OutputStream outputStream = socket.getOutputStream();
      PrintWriter writer = new PrintWriter(outputStream, true);

      HttpRequest request = HttpRequest.parseHttpRequest(reader);
      System.out.println("New connecttion " + request);
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
        } else if (request.path.startsWith("/files/")) {
          System.out.println("Processing files request");
          String requestFilename = request.path.substring(request.path.lastIndexOf("/") + 1);
          System.out.println("File to use: " + requestFilename);
          System.out.println("Method: " + request.method);
          if (request.method.equals("POST")) {
            System.out.println("POST requests");
            try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(directory + "/" + requestFilename))) {
              fileWriter.write(request.body);
              response = HttpResponse.createCreatedResponse(ContentType.TXT, "File Created");
              writer.println(response.getResponseString());
            } catch (IOException e) {
              System.out.println("IOException while writing file: " + e.getMessage());
              e.printStackTrace();
              response = HttpResponse.createNotFoundResponse(ContentType.TXT, "File Not Found");
              writer.println(response.getResponseString());
            }
          } else {

            if (filesInDir.contains(requestFilename)) {
              String fileContent = Files.readString(Path.of(directory + "/" + requestFilename));

              response = HttpResponse.createOkResponse(ContentType.OCTET, fileContent);
              writer.println(response.getResponseString());
            } else {
              response = HttpResponse.createNotFoundResponse(ContentType.OCTET, "File Not Found");
              writer.println(response.getResponseString());
            }
          }
        } else {
          response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
          writer.println(response.getResponseString());
        }
      } else {
        // Handle the invalid request or send an error response
        response = HttpResponse.createNotFoundResponse(ContentType.TXT, "Invalid Request");
        writer.println(response.getResponseString());
      }
    } catch (IOException e) {
      System.out.println("IOException while handling: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        System.out.println("IOException while closing socket: " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  private static Set<String> getFilesInDir(String dirPath) throws IOException {
    try {
      return Files.list(Paths.get(dirPath))
          .map(Path::toString)
          .map(path -> path.substring(path.lastIndexOf("/") + 1))
          .collect(Collectors.toSet());
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      throw e;
    }
  }

  public static void main(String[] args) {
    System.out.println("Logs from your program will appear here!");

    String dirPath = null;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("--directory") && i + 1 < args.length) {
        dirPath = args[i + 1];
        break;
      }
    }

    final String directory = dirPath;
    final Set<String> files = new HashSet<>();

    if (dirPath != null) {
      try {
        files.addAll(getFilesInDir(directory));
      } catch (IOException e) {
        System.out.println("IOException when trying to get files in directory: " + e.getMessage());
      }

      System.out.println("Files in directory: " + files);
    }

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      serverSocket.setReuseAddress(true);

      ExecutorService executorService = Executors.newCachedThreadPool();

      while (true) {
        try {
          Socket clientSocket = serverSocket.accept();
          executorService.submit(() -> handleRequest(clientSocket, files, directory));
        } catch (IOException e) {
          System.out.println("IOException: " + e.getMessage());
          throw e;
        }
      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      System.exit(1);
    }
  }
}
