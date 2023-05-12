package com.concise.backend.security;

import java.security.SecureRandom;
import java.util.Base64;

public class SecretKeyGenerator {
    private static final SecureRandom secureRandom = new SecureRandom(); //threadsafe
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //threadsafe

    public static String generateSecretKey(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    public static void main(String[] args) {
        String secretKey = generateSecretKey(32); // Generate a 32-byte secret key
        System.out.println(secretKey);
    }
}