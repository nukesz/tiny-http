package com.nukesz.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
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

        if (pathHandles.containsKey(clientMessage.path())) {
            sendResponse(clientSocket);
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

        String[] requestLineSplit = requestLine.split(" ");
        return new Request(requestLineSplit[0], requestLineSplit[1], requestLineSplit[2]);
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

    private void sendResponse(Socket clientSocket) throws IOException {
        Response response = new Response(
                HttpStatus.OK,
                Map.of("Content-Type", "text/html; charset=utf-8",
                        "Content-Length", "46",
                        "Connection", "close"),
                "<html><body><h1>Hello world</h1></body></html>");
        sendResponse(clientSocket, response);
    }

    private void sendNotFoundResponse(Socket clientSocket) throws IOException {
        Response response = new Response(
                HttpStatus.NOT_FOUND,
                Map.of("Content-Type", "text/plain; charset=utf-8",
                        "Content-Length", "13",
                        "Connection", "close"),
                "404 Not Found");
        sendResponse(clientSocket, response);
    }

    private void sendResponse(Socket clientSocket, Response response) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("HTTP/1.1 " + response.status().code() + " " + response.status().reason());
        out.println("Date: Sun, 02 Nov 2025 15:00:00 GMT");
        out.println("Server: tinyhttp/0.1");
        for (Map.Entry<String, String> header : response.headers().entrySet()) {
            out.println(header.getKey() + ": " + header.getValue());
        }
        out.println("");
        String body = response.body();
        if (body != null) {
            out.println(body);
        }
    }
}
