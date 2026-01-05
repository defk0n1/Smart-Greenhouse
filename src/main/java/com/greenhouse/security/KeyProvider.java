package com.greenhouse.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.json.JsonObject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class KeyProvider {

    @Inject
    @ConfigProperty(name = "iam.url", defaultValue = "http://localhost:8080/iam")
    private String iamUrl;

    private final Map<String, PublicKey> cache = new ConcurrentHashMap<>();

    public PublicKey getPublicKey(String kid) {
        return cache.computeIfAbsent(kid, this::fetchPublicKey);
    }

    private PublicKey fetchPublicKey(String kid) {
        try (Client client = ClientBuilder.newClient()) {
            JsonObject jwk = client.target(iamUrl + "/jwk/" + kid)
                    .request()
                    .get(JsonObject.class);

            String x = jwk.getString("x");
            byte[] decoded = Base64.getUrlDecoder().decode(x);

            // Reconstruct Ed25519 public key
            // Note: This is a simplified reconstruction. In a real scenario with
            // BouncyCastle or Java 15+,
            // we might handle this differently. Assuming standard X.509 encoding for now or
            // raw bytes if supported.
            // However, Java's KeyFactory for Ed25519 expects a specific format.
            // Let's try to use the raw bytes if possible, or wrap them in an X.509
            // SubjectPublicKeyInfo structure if needed.
            // For simplicity and standard Java 17+ support:

            // Ed25519 public key OID: 1.3.101.112
            // SubjectPublicKeyInfo sequence:
            // 30 2a (Sequence, 42 bytes)
            // 30 05 (Sequence, 5 bytes)
            // 06 03 2b 65 70 (OID 1.3.101.112)
            // 03 21 (Bit String, 33 bytes)
            // 00 (Padding)
            // [32 bytes of key]

            byte[] prefix = new byte[] {
                    0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
            };

            byte[] encodedKey = new byte[prefix.length + decoded.length];
            System.arraycopy(prefix, 0, encodedKey, 0, prefix.length);
            System.arraycopy(decoded, 0, encodedKey, prefix.length, decoded.length);

            KeyFactory kf = KeyFactory.getInstance("Ed25519");
            return kf.generatePublic(new X509EncodedKeySpec(encodedKey));

        } catch (Exception e) {
            System.err.println("❌ Failed to fetch public key for kid: " + kid);
            System.err.println("   IAM URL: " + iamUrl);
            System.err.println("   Full URL: " + iamUrl + "/jwk/" + kid);
            System.err.println("   Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch public key for kid: " + kid, e);
        }
    }
}
