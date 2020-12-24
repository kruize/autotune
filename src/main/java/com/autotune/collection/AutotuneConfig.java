/*******************************************************************************
 * Copyright (c) 2020 Red Hat, IBM Corporation and others.
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
package com.autotune.collection;

import com.autotune.application.Tunable;

import java.util.ArrayList;

/**
 * Container class for the AutotuneConfig kubernetes kind, which is used to tune
 * a layer (container, runtime, framework or application)
 *
 * Refer to examples dir for a reference AutotuneConfig yaml.
 */
public class AutotuneConfig
{
    int level;
    String name;
    String details;

    /*
    Used to detect the presence of the layer in an application. Autotune runs the query, looks for
    the key, and all applications in the query output are matched to the AutotuneConfig object.
    */
    String levelPresenceKey;
    String levelPresenceQuery;

    ArrayList<Tunable> tunables;

    public AutotuneConfig(String name, int level, String details, String levelPresenceQuery, String levelPresenceKey) {
        this.name = name;
        this.level = level;
        this.details = details;
        this.levelPresenceQuery = levelPresenceQuery;
        this.levelPresenceKey = levelPresenceKey;

        tunables = new ArrayList<>();
    }

    public AutotuneConfig(AutotuneConfig copy)
    {
        this.name = copy.getName();
        this.level = copy.getLevel();
        this.details = copy.getDetails();
        this.levelPresenceQuery = copy.getLevelPresenceQuery();
        this.levelPresenceKey = copy.getLevelPresenceKey();

        this.tunables = new ArrayList<>();
        this.tunables.addAll(copy.getTunables());
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Tunable> getTunables() {
        return tunables;
    }

    public void setTunables(ArrayList<Tunable> tunables) {
        this.tunables = tunables;
    }

    public String getLevelPresenceKey() {
        return levelPresenceKey;
    }

    public void setLevelPresenceKey(String levelPresenceKey) {
        this.levelPresenceKey = levelPresenceKey;
    }

    public String getLevelPresenceQuery() {
        return levelPresenceQuery;
    }

    public void setLevelPresenceQuery(String levelPresenceQuery) {
        this.levelPresenceQuery = levelPresenceQuery;
    }

    @Override
    public String toString() {
        return "AutotuneConfig{" +
                "level=" + level +
                ", name='" + name + '\'' +
                ", details='" + details + '\'' +
                ", levelPresenceQuery='" + levelPresenceQuery + '\'' +
                ", levelPresenceKey='" + levelPresenceKey + '\'' +
                ", tunables=" + tunables +
                '}';
    }
}
