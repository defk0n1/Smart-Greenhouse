package com.greenhouse.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@ApplicationScoped
public class JwtVerifier {

    @Inject
    private KeyProvider keyProvider;

    public Map<String, Object> verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3)
                return null;

            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonObject header = Json.createReader(new StringReader(headerJson)).readObject();
            String kid = header.getString("kid");

            java.security.PublicKey publicKey = keyProvider.getPublicKey(kid);

            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKey);
            signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));

            if (!signature.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                return null;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonObject payload = Json.createReader(new StringReader(payloadJson)).readObject();

            long exp = payload.getJsonNumber("exp").longValue();
            if (Instant.now().getEpochSecond() > exp) {
                return null;
            }

            return Map.of(
                    "sub", payload.getString("sub"),
                    "scope", payload.getString("scope", ""),
                    "groups",
                    payload.containsKey("groups")
                            ? payload.getJsonArray("groups").getValuesAs(jakarta.json.JsonString::getString)
                            : java.util.Collections.emptyList(),
                    "tenant-id", payload.getString("tenant-id"));

        } catch (Exception e) {
            // Log warning instead of stack trace for expected errors (e.g. expired token,
            // unknown key)
            System.out.println("Token verification failed: " + e.getMessage());
            return null;
        }
    }
}
