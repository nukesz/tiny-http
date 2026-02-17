package com.nukesz.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Tiny HTTP implementing <a href="https://datatracker.ietf.org/doc/html/rfc2616">HTTP/1.1</a>
 */
public class Server {
    private final int portNumber;
    private final Map<String, Function<Request, Response>> pathHandles = new HashMap<>();
    private ServerSocket serverSocket;
    private boolean acceptingClients = true;

    public Server(int portNumber) {
        this.portNumber = portNumber;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            this.serverSocket = serverSocket;
            System.out.println("Tiny HTTP server is running and waiting for client connections...");
            handleConnections(serverSocket);
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

    public void handle(String path, Function<Request, Response> function) {
        pathHandles.put(path, function);
    }

    public void stop() {
        acceptingClients = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to close server socket.");
        }
    }

    private void handleConnections(ServerSocket serverSocket) throws IOException {
        while (acceptingClients) {
            Socket clientSocket = serverSocket.accept();
            Thread.ofVirtual().start(() -> {
                try {
                    acceptIncomingClientConnections(clientSocket);
                } catch (IOException e) {
                    System.out.println("Exception caught when accepting incoming client");
                }
            });
        }
    }

    private void acceptIncomingClientConnections(Socket clientSocket) throws IOException {
        System.out.println("Client connected!");

        Request clientMessage = readMessage(clientSocket);
        System.out.println("Client request := " + clientMessage);

        Function<Request, Response> pathHandle = pathHandles.get(clientMessage.path());
        if (pathHandle != null) {
            Response response = pathHandle.apply(clientMessage);
            sendResponse(clientSocket, response);
        } else {
            sendNotFoundResponse(clientSocket);
        }

        clientSocket.close();
    }

    private Request readMessage(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String requestLine = readRequestLine(in);
        Map<String, String> headers = readHeaders(in);
        String contentLengthHeader = headers.get("Content-Length");
        if (contentLengthHeader != null) {
            readBody(in, Integer.parseInt(contentLengthHeader));
        }

        var requestLineSplit = requestLine.split(" ");
        var requestMethod = HttpRequestMethod.fromString(requestLineSplit[0]);
        return new Request(requestMethod, requestLineSplit[1], requestLineSplit[2]);
    }

    private void readBody(BufferedReader in, int contentLength) throws IOException {
        char[] bodyChars = new char[contentLength];
        int read = 0;
        while (read < contentLength) {
            int r = in.read(bodyChars, read, contentLength - read);
            if (r == -1) {
                throw new IOException("Unexpected end of stream");
            }
            read += r;
        }
        String body = new String(bodyChars);
        System.out.println("Content Length: " + read);
        System.out.println("Content Body: " + body);
    }

    private String readRequestLine(BufferedReader in) throws IOException {
        return in.readLine();
    }

    private Map<String, String> readHeaders(BufferedReader in) throws IOException {
        String inputLine;
        Map<String, String> headers = new HashMap<>();
        while ((inputLine = in.readLine()) != null && !inputLine.isBlank()) {
            String[] header = inputLine.split(": ");
            headers.put(header[0], header[1]);
        }
        return headers;
    }

    private void sendNotFoundResponse(Socket clientSocket) throws IOException {
        Response response = new Response(
                HttpStatus.NOT_FOUND,
                Map.of("Content-Type", "text/plain; charset=utf-8",
                        "Connection", "close"),
                "404 Not Found");
        sendResponse(clientSocket, response);
    }

    private void sendResponse(Socket clientSocket, Response response) throws IOException {
        OutputStream out = clientSocket.getOutputStream();
        byte[] bodyBytes = toBodyBytes(response.body());
        out.write(buildResponseHeaders(response, bodyBytes).getBytes(StandardCharsets.UTF_8));
        writeResponseBody(out, bodyBytes);
        out.flush();
    }

    private byte[] toBodyBytes(String body) {
        if (body == null) {
            return null;
        }
        return body.getBytes(StandardCharsets.UTF_8);
    }

    private String buildResponseHeaders(Response response, byte[] bodyBytes) {
        StringBuilder responseHeaders = new StringBuilder();
        appendStatusLine(responseHeaders, response);
        appendContentLengthHeader(responseHeaders, bodyBytes);
        appendDefaultHeaders(responseHeaders);
        appendCustomHeaders(responseHeaders, response.headers());
        responseHeaders.append("\r\n");
        return responseHeaders.toString();
    }

    private void appendStatusLine(StringBuilder responseHeaders, Response response) {
        responseHeaders
                .append("HTTP/1.1 ")
                .append(response.status().code())
                .append(" ")
                .append(response.status().reason())
                .append("\r\n");
    }

    private void appendContentLengthHeader(StringBuilder responseHeaders, byte[] bodyBytes) {
        if (bodyBytes != null) {
            responseHeaders.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        }
    }

    private void appendDefaultHeaders(StringBuilder responseHeaders) {
        responseHeaders.append("Date: Sun, 02 Nov 2025 15:00:00 GMT").append("\r\n");
        responseHeaders.append("Server: tinyhttp/0.1").append("\r\n");
    }

    private void appendCustomHeaders(StringBuilder responseHeaders, Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            responseHeaders.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
        }
    }

    private void writeResponseBody(OutputStream out, byte[] bodyBytes) throws IOException {
        if (bodyBytes != null) {
            out.write(bodyBytes);
        }
    }
}
