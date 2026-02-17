package com.nukesz.tinyhttp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerTest {

    private static final int PORT = ThreadLocalRandom.current().nextInt(10_000, 60_000);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static Server server;

    @BeforeAll
    public static void setupServer() {
        executorService.submit(() -> {
            server = new Server(PORT);
            server.handle("/", (request) -> switch (request.method()) {
                case GET -> new Response(HttpStatus.OK, Map.of(), null);
                case POST -> new Response(HttpStatus.CREATED, Map.of(), "Entry created");
                default -> new Response(HttpStatus.NOT_FOUND, Map.of(), "404 Not Found");
            });
            server.handle("/ping", (_) -> new Response(HttpStatus.OK, Map.of(), "Pong"));
            server.handle("/unicode", (_) -> new Response(HttpStatus.OK, Map.of(), "é"));
            server.start();
        });
    }

    @AfterAll
    static void afterAll() {
        server.stop();
        executorService.close();
    }

    @Test
    public void getRootRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK for GET /");
    }

    @Test
    public void getNotFoundRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/notFound/");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "Expected 404 Not Found for GET /notFound");
    }

    @Test
    public void getPingRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/ping");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK for GET /ping");
        assertEquals("Pong", response.body(), "Expected body 'Pong' for GET /ping");
    }

    @Test
    public void postRootRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/");

        String requestBody = "name=Alice&age=30";

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Expected 201 OK for POST /");
    }

    @Test
    public void postJsonRootRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/");

        String jsonBody = """
        {
          "name": "Alice",
          "age": 30
        }""";

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Expected 201 OK for POST /");
    }

    @Test
    public void unicodeResponseContentLengthIsByteAccurate() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/unicode");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK for GET /unicode");
        assertEquals("é", response.body(), "Expected exact UTF-8 body without extra newline");
        assertTrue(
                response.headers().firstValue("Content-Length").isPresent()
                        && response.headers().firstValue("Content-Length").orElseThrow().equals("2"),
                "Expected byte-accurate Content-Length=2 for UTF-8 body"
        );
    }

    @Test
    public void responseDateHeaderIsRfc1123AndCurrent() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/ping");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        Instant beforeSend = Instant.now();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        Instant afterSend = Instant.now();

        String dateHeader = response.headers().firstValue("Date").orElseThrow();
        Instant dateInstant = ZonedDateTime.parse(dateHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        Instant lowerBound = beforeSend.minusSeconds(1);
        Instant upperBound = afterSend.plusSeconds(1);

        assertEquals(200, response.statusCode(), "Expected 200 OK for GET /ping");
        assertTrue(!dateInstant.isBefore(lowerBound) && !dateInstant.isAfter(upperBound),
                "Expected Date header to be generated at response time");
    }

    @Test
    public void malformedRequestLineReturnsBadRequest() throws Exception {
        String statusLine = sendRawRequestAndReadStatusLine(
                "GET /only-two-parts\r\n" +
                "Host: 127.0.0.1\r\n" +
                "\r\n"
        );

        assertEquals("HTTP/1.1 400 Bad Request", statusLine, "Expected 400 for malformed request line");
    }

    @Test
    public void malformedHeaderReturnsBadRequest() throws Exception {
        String statusLine = sendRawRequestAndReadStatusLine(
                "GET / HTTP/1.1\r\n" +
                "Host 127.0.0.1\r\n" +
                "\r\n"
        );

        assertEquals("HTTP/1.1 400 Bad Request", statusLine, "Expected 400 for malformed header");
    }

    private String sendRawRequestAndReadStatusLine(String rawRequest) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", PORT)) {
            OutputStream out = socket.getOutputStream();
            out.write(rawRequest.getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return in.readLine();
        }
    }
}
