/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.autotune.utils;

import com.autotune.operator.KruizeDeploymentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encryption for credential values stored in the database.
 *
 * <p>The encryption key can be supplied explicitly via the {@code KRUIZE_ENCRYPTION_KEY}
 * environment variable (a 32-byte Base64-encoded string). When the variable is not set,
 * the key is derived deterministically from the database password using PBKDF2-HMAC-SHA256,
 * requiring zero additional configuration from downstream teams.</p>
 */
public final class CredentialEncryptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialEncryptor.class);

    private static final String ENV_KEY = "KRUIZE_ENCRYPTION_KEY";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENC_PREFIX = "ENC:";
    private static final int PBKDF2_ITERATIONS = 600_000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static volatile SecretKey secretKey;

    private CredentialEncryptor() {
    }

    private static SecretKey getKey() {
        if (secretKey == null) {
            synchronized (CredentialEncryptor.class) {
                if (secretKey == null) {
                    secretKey = initializeKey();
                }
            }
        }
        return secretKey;
    }

    private static SecretKey initializeKey() {
        String keyBase64 = System.getenv(ENV_KEY);
        if (keyBase64 != null && !keyBase64.trim().isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64.trim());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        ENV_KEY + " must be a Base64-encoded 256-bit (32-byte) key, got " + keyBytes.length + " bytes");
            }
            LOGGER.info("Credential encryption enabled (AES-256-GCM) using provided key");
            return new SecretKeySpec(keyBytes, "AES");
        }
        return deriveKeyFromDbPassword();
    }

    private static SecretKey deriveKeyFromDbPassword() {
        String dbPassword = KruizeDeploymentInfo.database_password;
        if (dbPassword == null || dbPassword.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot derive encryption key: database password is not configured. "
                    + "Either set " + ENV_KEY + " or ensure the database is configured before credentials are accessed.");
        }
        try {
            String saltInput = "KruizeCredentialEncryption:"
                    + nullSafe(KruizeDeploymentInfo.database_hostname) + ":"
                    + nullSafe(KruizeDeploymentInfo.database_port) + ":"
                    + nullSafe(KruizeDeploymentInfo.database_dbname);
            byte[] salt = saltInput.getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            KeySpec spec = new PBEKeySpec(dbPassword.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            byte[] derived = factory.generateSecret(spec).getEncoded();

            LOGGER.info("Credential encryption enabled (AES-256-GCM) using key derived from database password");
            return new SecretKeySpec(derived, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive encryption key from database password", e);
        }
    }

    private static String nullSafe(String value) {
        return value != null ? value : "";
    }

    /**
     * Encrypts a plaintext credential string.
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            SecretKey key = getKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    /**
     * Decrypts a credential string. If the value does not carry the {@code ENC:} prefix,
     * it is returned as-is (plaintext / legacy data).
     */
    public static String decrypt(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(ENC_PREFIX)) {
            return ciphertext;
        }
        try {
            SecretKey key = getKey();
            byte[] decoded = Base64.getDecoder().decode(ciphertext.substring(ENC_PREFIX.length()));

            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credentials", e);
        }
    }
}
