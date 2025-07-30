/*******************************************************************************
 * Copyright (c) 2024 Red Hat, IBM Corporation and others.
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

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * This class contains utility functions to determine experiment type
 */
public class ExperimentTypeUtil {
    public static boolean isContainerExperiment(AnalyzerConstants.ExperimentType experimentType) {
        return experimentType == null || AnalyzerConstants.ExperimentType.CONTAINER.equals(experimentType);
    }

    public static boolean isNamespaceExperiment(AnalyzerConstants.ExperimentType experimentType) {
        return experimentType != null && AnalyzerConstants.ExperimentType.NAMESPACE.equals(experimentType);
    }

    public static long getBitMaskForExperimentType(AnalyzerConstants.ExperimentType type) {
        // TODO: to be updated as part of bharath's PR
        return 0L;
    }

    public class ExperimentTypeSerializer implements JsonSerializer<AnalyzerConstants.ExperimentType>, JsonDeserializer<AnalyzerConstants.ExperimentType> {
        @Override
        public JsonElement serialize(AnalyzerConstants.ExperimentType experimentType, Type typeOfT, JsonSerializationContext context) {
            if (experimentType != null) {
                return new JsonPrimitive(experimentType.name().toLowerCase());
            }
            return null;
        }

        @Override
        public AnalyzerConstants.ExperimentType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String experimentType = json.getAsString();
            if (experimentType != null) {
                try {
                    return AnalyzerConstants.ExperimentType.valueOf(experimentType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new JsonParseException(String.format(AnalyzerErrorConstants.APIErrors.CreateExperimentAPI.INVALID_EXPERIMENT_TYPE, experimentType), e);
                }
            }
            return null;
        }
    }

    /**
     * Checks if the particular experiment is a container experiment by checking if
     * the container bit is set.
     *
     * Refer: AnalyserConstants.ExperimentBitMask ENUM
     *
     * Returns true if the 0th BIT (Container Bit) is set
     * Returns false if the oth BIT (Container Bit) is NOT set
     * @param experimentBitset
     * @return
     */
    public static boolean isContainerExperiment(long experimentBitset) {
        return AnalyzerConstants.ExperimentBitMask.CONTAINER_BIT.isSet(experimentBitset);
    }

    /**
     * Checks if the particular experiment is a namespace experiment by checking if
     * the namespace bit is set
     *
     * Refer: AnalyserConstants.ExperimentBitMask ENUM
     *
     * Returns true if the 3rd BIT (Namespace Bit) is set
     * Returns false if the 3rd BIT (Namespace Bit) is NOT set
     * @param experimentBitset
     * @return
     */
    public static boolean isNamespaceExperiment(long experimentBitset) {
        return AnalyzerConstants.ExperimentBitMask.NAMESPACE_BIT.isSet(experimentBitset);
    }

    /**
     * Converts the ExperimentType Enum value to the bit mask by setting appropriate bit
     * @param type
     * @return
     */
    public static long getBitMaskForExperimentType(AnalyzerConstants.ExperimentType type) {
        if (type == null) return 0L;

        switch (type) {
            case CONTAINER:
                return AnalyzerConstants.ExperimentBitMask.CONTAINER_BIT.getMask();
            case WORKLOAD:
                return AnalyzerConstants.ExperimentBitMask.WORKLOAD_BIT.getMask();
            case NAMESPACE:
                return AnalyzerConstants.ExperimentBitMask.NAMESPACE_BIT.getMask();
            case CLUSTER:
                return AnalyzerConstants.ExperimentBitMask.CLUSTER_BIT.getMask();
            default:
                return 0L;
        }
    }


    /**
     * Converts the bitmask to experiment type enum value
     *
     * NOTE: Only the infra bits [0-7] are checked to convert to experiment type enum
     * Refer: AnalyserConstants.ExperimentBitMask ENUM
     * @param bitMask
     * @return
     */
    public static AnalyzerConstants.ExperimentType getExperimentTypeFromBitMask(long bitMask) {
        // Should Ideally return NULL but assumes that it's container if nothing set
        if  (bitMask == 0) return AnalyzerConstants.ExperimentType.CONTAINER;

        if (AnalyzerConstants.ExperimentBitMask.CONTAINER_BIT.isSet(bitMask)) {
            return AnalyzerConstants.ExperimentType.CONTAINER;
        }

        if (AnalyzerConstants.ExperimentBitMask.WORKLOAD_BIT.isSet(bitMask)) {
            return AnalyzerConstants.ExperimentType.WORKLOAD;
        }

        if (AnalyzerConstants.ExperimentBitMask.NAMESPACE_BIT.isSet(bitMask)) {
            return AnalyzerConstants.ExperimentType.NAMESPACE;
        }

        if (AnalyzerConstants.ExperimentBitMask.CLUSTER_BIT.isSet(bitMask)) {
            return AnalyzerConstants.ExperimentType.CLUSTER;
        }
        // Should Ideally return NULL but assumes that it's container if nothing set
        return AnalyzerConstants.ExperimentType.CONTAINER;
    }

    /**
     * Set the particular bit to the mask
     * @param currentMask
     * @param type
     * @return
     */
    public static long setExperimentBit(long currentMask, AnalyzerConstants.ExperimentType type) {
        return currentMask | getBitMaskForExperimentType(type);
    }

    // TODO: Need to be updated when we need to generate the bitset based on many attributes
    // other than just container or namespace type, Method signature changes, after you pass more args for deciding the
    // bitset, Please proceed changing all the callers if the signature is changed.
    public static long getExperimentType(AnalyzerConstants.ExperimentType experimentType) {
        long bitset = 0L;
        bitset = setExperimentBit(bitset, experimentType);
        return bitset;
    }
}
