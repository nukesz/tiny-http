package com.nukesz.tinyhttp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

        List<String> clientMessage = readMessage(clientSocket);
        System.out.println("Client says: = " + clientMessage);

        sendResponse(clientSocket);

        clientSocket.close();
    }

    private static List<String> readMessage(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        String inputLine;
        List<String> lines = new ArrayList<>();
        while ((inputLine = in.readLine()) != null && !inputLine.isBlank()) {
            lines.add(inputLine);
        }
        return lines;
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
