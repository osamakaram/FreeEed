/*
 *
 * Copyright SHMsoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.freeeed.services;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Free, per-user activation keys.
 *
 * A user registers by emailing us (from the in-app dialog). We reply by hand
 * with the key produced for their email; they type it back into the app to
 * activate. The key is verified entirely offline - no server needed - because
 * it is a deterministic HMAC of the (normalized) email under a shared secret.
 *
 * This is FREE registration, not payment: the key costs nothing and unlocks the
 * same software for everyone. Its only purpose is to make sure every user has
 * exchanged an email with us, so we can keep in touch (FreeEed issue #549).
 *
 * Note: because the secret below ships in the open-source build, a determined
 * user could derive their own key from the source. That is acceptable - the
 * point is the email handshake, not copy protection. If we ever want truly
 * unforgeable keys we would switch to an asymmetric signature (public key in
 * the app, private key held only by us).
 *
 * To generate a key when replying to a user, run:
 *   java -cp freeeed-processing-...-jar-with-dependencies.jar \
 *        org.freeeed.services.Activation user@example.com
 *
 * @author mark
 */
public final class Activation {

    // Shared secret used to derive keys. Free-registration speed bump, not DRM.
    private static final String SECRET = "FreeEed-activation-2026-9Qz7Kx2Tn5Rb";

    private Activation() {
    }

    /** Lowercase + trim so the key doesn't depend on capitalization or spaces. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Generate the activation key for an email, e.g. "FE-1A2B-3C4D-5E6F".
     *
     * @param email the user's email address
     * @return formatted activation key, or "" if the email is blank
     */
    public static String generateKey(String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            return "";
        }
        String hex = hmacSha256Hex(SECRET, normalized).toUpperCase();
        String body = hex.substring(0, 12);
        return "FE-" + body.substring(0, 4) + "-" + body.substring(4, 8) + "-" + body.substring(8, 12);
    }

    /**
     * Validate a key the user typed against their email. Dashes, spaces and case
     * are ignored, so "fe1a2b3c4d5e6f" and "FE-1A2B-3C4D-5E6F" both pass.
     *
     * @param email the email the user registered with
     * @param enteredKey the key the user typed
     * @return true if the key matches the email
     */
    public static boolean isValid(String email, String enteredKey) {
        if (enteredKey == null) {
            return false;
        }
        String expected = canonical(generateKey(email));
        String actual = canonical(enteredKey);
        return !expected.isEmpty() && expected.equals(actual);
    }

    private static String canonical(String key) {
        return key == null ? "" : key.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            // HmacSHA256 is guaranteed present on every JVM; this should never happen.
            throw new IllegalStateException("Could not compute activation key", e);
        }
    }

    /** Key generator for manual replies: prints the key for the given email(s). */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: Activation <email> [<email> ...]");
            return;
        }
        for (String email : args) {
            System.out.println(email + "\t" + generateKey(email));
        }
    }
}
