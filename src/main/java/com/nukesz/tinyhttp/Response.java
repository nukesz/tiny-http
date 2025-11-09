package com.nukesz.tinyhttp;

import java.util.Map;

public record Response(HttpStatus status, Map<String, String> headers, String body) {
}
