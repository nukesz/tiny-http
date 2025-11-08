package com.nukesz.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiny HTTP implementing <a href="https://datatracker.ietf.org/doc/html/rfc2616">HTTP/1.1</a>
 */
public class Main {

    static void main(String[] args) {
        int portNumber = 9090;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Tiny HTTP server is running and waiting for client connections...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().start(() -> {
                    try {
                        acceptIncomingClientConnections(clientSocket);
                    } catch (IOException e) {
                        System.out.println("Exception caught when accepting incoming client");
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + portNumber + " or listening for a connection");
            System.out.println(e.getMessage());
        }
    }

    private static void acceptIncomingClientConnections(Socket clientSocket) throws IOException {
        System.out.println("Client connected!");

        Request clientMessage = readMessage(clientSocket);
        System.out.println("Client request := " + clientMessage);

        sendResponse(clientSocket);

        clientSocket.close();
    }

    private static Request readMessage(Socket clientSocket) throws IOException {
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

    private static void readBody(BufferedReader in, int contentLength) throws IOException {
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

    private static String readRequestLine(BufferedReader in) throws IOException {
        return in.readLine();
    }

    private static Map<String, String> readHeaders(BufferedReader in) throws IOException {
        String inputLine;
        Map<String, String> headers = new HashMap<>();
        while ((inputLine = in.readLine()) != null && !inputLine.isBlank()) {
            String[] header = inputLine.split(": ");
            headers.put(header[0], header[1]);
        }
        return headers;
    }

    private static void sendResponse(Socket clientSocket) throws IOException {
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        out.println("HTTP/1.1 200 OK");
        out.println("Date: Sun, 02 Nov 2025 15:00:00 GMT");
        out.println("Server: tinyhttp/0.1");
        out.println("Content-Type: text/html; charset=utf-8");
        out.println("Content-Length: 46");
        out.println("");
        out.println("<html><body><h1>Hello world</h1></body></html>");
    }
}
