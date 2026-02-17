# TinyHTTP

A minimal Java HTTP server built from scratch without any external dependencies.

## Example

`src/main/java/com/nukesz/tinyhttp/Main.java` contains a minimal route setup:

```java
Server server = new Server(9090);
server.handle("/ping", (request) -> switch (request.method()) {
    case GET -> new Response(
            HttpStatus.OK,
            Map.of("Content-Type", "text/plain; charset=utf-8"),
            "pong");
    default -> new Response(
            HttpStatus.METHOD_NOT_ALLOWED,
            Map.of("Content-Type", "text/plain; charset=utf-8"),
            "405 Method Not Allowed");
});
server.start();
```

Try it:

```bash
curl -i http://127.0.0.1:9090/ping
```

Expected status:
- `GET /ping` -> `200 OK`
- Non-`GET /ping` -> `405 Method Not Allowed`
