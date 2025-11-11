package com.nukesz.tinyhttp;

import java.io.IOException;

public enum HttpRequestMethod {
    CONNECT,
    DELETE,
    GET,
    HEAD,
    OPTIONS,
    PATCH,
    POST,
    PUT,
    TRACE;

    public static HttpRequestMethod fromString(String name) throws IOException {
        for (HttpRequestMethod method : values()) {
            if (method.name().equalsIgnoreCase(name)) {
                return method;
            }
        }
        throw new IOException("Invalid HTTP Request Method: " + name);
    }
}
