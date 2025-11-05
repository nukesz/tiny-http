package com.nukesz.tinyhttp;

public record Request(String method, String path, String protocol) {
}
