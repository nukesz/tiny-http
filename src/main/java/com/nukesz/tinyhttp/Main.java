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

    public static void main(String[] args) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(9090)) {
            System.out.println("Server is running and waiting for client connections...");
            while (true) {
                acceptIncomingClientConnections(serverSocket);
            }
        }
    }

    private static void acceptIncomingClientConnections(ServerSocket serverSocket) throws IOException {
        Socket clientSocket = serverSocket.accept();
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
