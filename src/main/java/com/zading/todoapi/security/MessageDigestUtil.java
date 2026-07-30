package com.zading.todoapi.security;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class MessageDigestUtil {
    private MessageDigestUtil() {
    }

    public static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
