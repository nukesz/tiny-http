package com.nukesz.tinyhttp;

import java.util.Map;

public class Main {

    static void main(String[] args) {
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
    }
}
