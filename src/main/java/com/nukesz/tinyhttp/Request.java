package com.nukesz.tinyhttp;

public record Request(HttpRequestMethod method, String path, String protocol) {
}
