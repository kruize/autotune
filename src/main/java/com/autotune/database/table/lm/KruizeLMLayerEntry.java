/*******************************************************************************
 * Copyright (c) 2026 Red Hat, IBM Corporation and others.
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

package com.autotune.database.table.lm;

import com.autotune.analyzer.kruizeLayer.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * This is a Java class named KruizeLMLayerEntry annotated with JPA annotations.
 * It represents a table named kruize_lm_layer in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * api_version: A string representing the API version of the layer.
 * kind: A string representing the kind/type of the layer.
 * metadata: A JSON object containing layer metadata (e.g., name).
 * layer_name: A unique identifier for the layer (primary key).
 * layer_level: An integer representing the hierarchy level of the layer.
 * details: A string containing additional details or description of the layer.
 * layer_presence: A JSON object defining how to detect the layer's presence (presence, queries, or label).
 * tunables: A JSON array containing the tunable parameters for this layer.
 * <p>
 * The KruizeLMLayerEntry class also has getters and setters for all its fields.
 */
@Entity
@Table(name = "kruize_lm_layer")
public class KruizeLMLayerEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(KruizeLMLayerEntry.class);

    private String api_version;
    private String kind;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode metadata;
    @Id
    private String layer_name;
    private Integer layer_level;
    private String details;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode layer_presence;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode tunables;

    // Getters and setters
    public String getApi_version() {
        return api_version;
    }

    public void setApi_version(String api_version) {
        this.api_version = api_version;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public JsonNode getMetadata() {
        return metadata;
    }

    public void setMetadata(JsonNode metadata) {
        this.metadata = metadata;
    }

    public String getLayer_name() {
        return layer_name;
    }

    public void setLayer_name(String layer_name) {
        this.layer_name = layer_name;
    }

    public Integer getLayer_level() {
        return layer_level;
    }

    public void setLayer_level(Integer layer_level) {
        this.layer_level = layer_level;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public JsonNode getLayer_presence() {
        return layer_presence;
    }

    public void setLayer_presence(JsonNode layer_presence) {
        this.layer_presence = layer_presence;
    }

    public JsonNode getTunables() {
        return tunables;
    }

    public void setTunables(JsonNode tunables) {
        this.tunables = tunables;
    }

    @Override
    public String toString() {
        return "KruizeLMLayerEntry{" +
                "api_version='" + api_version + '\'' +
                ", kind='" + kind + '\'' +
                ", metadata=" + metadata +
                ", layer_name='" + layer_name + '\'' +
                ", layer_level=" + layer_level +
                ", details='" + details + '\'' +
                ", layer_presence=" + layer_presence +
                ", tunables=" + tunables +
                '}';
    }

    /**
     * Converts this database entry to a KruizeLayer domain object
     *
     * @return KruizeLayer object with all fields populated from this entry
     */
    public KruizeLayer getKruizeLayer() {
        KruizeLayer layer = new KruizeLayer();
        layer.setApiVersion(this.api_version);
        layer.setKind(this.kind);
        layer.setMetadata(convertJsonNodeToLayerMetadata(this.metadata));
        layer.setLayerName(this.layer_name);
        layer.setLayerLevel(this.layer_level);
        layer.setDetails(this.details);
        layer.setLayerPresence(convertJsonNodeToLayerPresence(this.layer_presence));
        layer.setTunables(convertJsonNodeToTunables(this.tunables));
        return layer;
    }

    /**
     * Converts JsonNode to LayerMetadata object
     *
     * @param jsonNode The JsonNode to convert
     * @return LayerMetadata object, or null if jsonNode is null/empty
     */
    private LayerMetadata convertJsonNodeToLayerMetadata(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        try {
            return objectMapper.treeToValue(jsonNode, LayerMetadata.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert JsonNode to LayerMetadata: {}", e.getMessage());
            throw new RuntimeException("Failed to convert JsonNode to LayerMetadata", e);
        }
    }

    /**
     * Converts JsonNode to LayerPresence object
     *
     * @param jsonNode The JsonNode to convert
     * @return LayerPresence object, or null if jsonNode is null/empty
     */
    private LayerPresence convertJsonNodeToLayerPresence(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return null;
        }

        try {
            return objectMapper.treeToValue(jsonNode, LayerPresence.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert JsonNode to LayerPresence: {}", e.getMessage());
            throw new RuntimeException("Failed to convert JsonNode to LayerPresence", e);
        }
    }

    /**
     * Converts JsonNode to ArrayList of Tunable objects
     *
     * @param jsonNode The JsonNode to convert
     * @return ArrayList of Tunable objects, or empty list if jsonNode is null/empty
     */
    private ArrayList<Tunable> convertJsonNodeToTunables(JsonNode jsonNode) {
        ObjectMapper objectMapper = new ObjectMapper();
        if (jsonNode == null || jsonNode.isNull()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.convertValue(jsonNode,
                    objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, Tunable.class));
        } catch (Exception e) {
            LOGGER.error("Failed to convert JsonNode to ArrayList<Tunable>: {}", e.getMessage());
            throw new RuntimeException("Failed to convert JsonNode to ArrayList<Tunable>", e);
        }
    }
}
