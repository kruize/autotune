/*******************************************************************************
 * Copyright (c) 2023 Red Hat, IBM Corporation and others.
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
package com.autotune.database.table;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * This is a Java class named KruizePerformanceProfileEntry annotated with JPA annotations.
 * It represents a table named kruize_performance_profiles in a relational database.
 * <p>
 * The class has the following fields:
 * <p>
 * id: A unique identifier for each performance profile detail.
 * name: A string representing the name of the performance profile.
 * profile_version: A string representing the version of the performance profile.
 * k8s_type: A string representing kubernetes type.
 * SLO: A string representing the slo class, direction, Objective function and function variables.
 */
@Entity
@Table(name = "kruize_performance_profiles")
public class KruizePerformanceProfileEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long performance_profile_id;
    @Column(unique = true)
    private String name;
    private double profile_version;
    private String k8s_type;
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode slo;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getProfile_version() {
        return profile_version;
    }

    public void setProfile_version(double profile_version) {
        this.profile_version = profile_version;
    }

    public String getK8s_type() {
        return k8s_type;
    }

    public void setK8s_type(String k8s_type) {
        this.k8s_type = k8s_type;
    }

    public JsonNode getSlo() {
        return slo;
    }

    public void setSlo(JsonNode slo) {
        this.slo = slo;
    }
}
