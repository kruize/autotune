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
package com.autotune.database.helper;

import com.autotune.utils.CredentialEncryptor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter that transparently encrypts credentials when writing
 * to the database and decrypts when reading. Applied to any {@code JsonNode}
 * column that holds sensitive data.
 */
@Converter
public class EncryptedJsonConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        return CredentialEncryptor.encrypt(attribute.toString());
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            String decrypted = CredentialEncryptor.decrypt(dbData);
            return objectMapper.readTree(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert encrypted credentials to JsonNode", e);
        }
    }
}
