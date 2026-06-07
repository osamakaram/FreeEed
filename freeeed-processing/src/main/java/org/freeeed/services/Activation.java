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
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Free, per-user activation - verification only.
 *
 * A user registers by emailing us (from the in-app dialog). We reply with the
 * activation key produced for their email; they paste it back into the app to
 * activate. The key is an Ed25519 signature over the (normalized) email, so it
 * is verified entirely offline - no server needed.
 *
 * This class holds ONLY the PUBLIC key, so the open-source app can verify a key
 * but can never create one. The private signing key lives only in our private
 * AI Advisor repo (scripts/freeeed_activation.py); it never ships here. To mint
 * a key when replying to a user, run there:
 *   python scripts/freeeed_activation.py user@example.com
 *
 * The key is free and unlocks the same software for everyone - its only purpose
 * is to make sure every user has exchanged an email with us (FreeEed issue #549).
 *
 * @author mark
 */
public final class Activation {

    // Ed25519 public key (X.509 SubjectPublicKeyInfo, base64). Verify-only.
    // The matching private key is kept off this repo (AI Advisor).
    private static final String PUBLIC_KEY_B64 =
            "MCowBQYDK2VwAyEAUnWK8a5xlj+1FYvZAXmwujgyEyb1iOetYjwJnYBaeEQ=";

    private Activation() {
    }

    /** Lowercase + trim so the key doesn't depend on capitalization or spaces. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Validate a key the user pasted against their email. The key is a URL-safe
     * base64 Ed25519 signature over the normalized email; surrounding whitespace
     * is ignored.
     *
     * @param email the email the user registered with
     * @param enteredKey the activation key the user pasted
     * @return true if the key is a valid signature for the email
     */
    public static boolean isValid(String email, String enteredKey) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty() || enteredKey == null) {
            return false;
        }
        try {
            byte[] signature = Base64.getUrlDecoder().decode(stripWhitespace(enteredKey));
            byte[] spki = Base64.getDecoder().decode(PUBLIC_KEY_B64);
            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(spki));
            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(normalized.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signature);
        } catch (Exception e) {
            // Malformed key, wrong length, unknown algorithm, etc. - treat as invalid.
            return false;
        }
    }

    private static String stripWhitespace(String s) {
        return s.replaceAll("\\s", "");
    }
}
