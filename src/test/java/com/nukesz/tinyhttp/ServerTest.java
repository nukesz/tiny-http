package com.nukesz.tinyhttp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerTest {

    private static final int PORT = 9090;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static Server server;

    @BeforeAll
    public static void setupServer() {
        executorService.submit(() -> {
            server = new Server(PORT);
            server.handle("/", (request) -> {
                return new Response(200);
            });
            server.start();
        });

//        http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
//            if r.URL.Path != "/" {
//                http.NotFound(w, r)
//                return
//            }
//
//            if r.Method == "GET" {
//                fmt.Fprintf(w, "GET, %q", html.EscapeString(r.URL.Path))
//            } else if r.Method == "POST" {
//                fmt.Fprintf(w, "POST, %q", html.EscapeString(r.URL.Path))
//            } else {
//                http.Error(w, "Invalid request method.", 405)
//            }
//        })
//
//        log.Fatal(http.ListenAndServe(":8080", nil))
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
    public void postRootRequest() throws Exception {
        URI uri = new URI("http://127.0.0.1:" + PORT + "/");

        String requestBody = "name=Alice&age=30";

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Expected 200 OK for POST /");
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

        assertEquals(200, response.statusCode(), "Expected 200 OK for POST /");
    }
}
