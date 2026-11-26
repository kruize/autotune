/*******************************************************************************
 * Copyright (c) 2022 Red Hat, IBM Corporation and others.
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
package com.autotune.analyzer.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * UTC data handler for GSON
 */
public class GsonUTCDateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

    private static final DateTimeFormatter UTC_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT)
            .withZone(ZoneOffset.UTC);

    public GsonUTCDateAdapter() {
    }

    @Override
    public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(UTC_TIMESTAMP.format(date.toInstant()));
    }

    @Override
    public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
        String text = jsonElement.getAsString();
        try {
            Instant instant = Instant.from(UTC_TIMESTAMP.parse(text));
            // Smart resolution can accept a calendar rollover (for example Feb 31)
            // as a different instant than lenient SimpleDateFormat. The canonical
            // form of a normal timestamp matches the input, so only the odd cases pay
            // for a per-call SimpleDateFormat.
            if (!text.equals(UTC_TIMESTAMP.format(instant))) {
                return parseLenient(text);
            }
            return Date.from(instant);
        } catch (DateTimeParseException exception) {
            return parseLenient(text);
        }
    }

    /**
     * {@link SimpleDateFormat} is lenient and accepts some values the formatter rejects.
     * A new formatter per call avoids the shared lock. Failure still becomes
     * {@link JsonParseException} with the parse exception as the cause.
     */
    private static Date parseLenient(String text) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return dateFormat.parse(text);
        } catch (ParseException exception) {
            throw new JsonParseException(exception);
        }
    }
}
