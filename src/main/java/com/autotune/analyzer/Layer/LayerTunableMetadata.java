package com.autotune.analyzer.Layer;

import com.google.gson.annotations.SerializedName;

public class LayerTunableMetadata {
    private String name;

    private String description;

    @SerializedName("value_type")
    private String valueType;

    private String unit; // Optional

    private String type;

    @SerializedName("type_def")
    private TypeDef typeDef;

    public LayerTunableMetadata(String name, String description, String valueType, String unit, String type, TypeDef typeDef) {
        this.name = name;
        this.description = description;
        this.valueType = valueType;
        this.unit = unit;
        this.type = type;
        this.typeDef = typeDef;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TypeDef getTypeDef() {
        return typeDef;
    }

    public void setTypeDef(TypeDef typeDef) {
        this.typeDef = typeDef;
    }

    @Override
    public String toString() {
        return "LayerTunableMetadata{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", valueType='" + valueType + '\'' +
                ", unit='" + unit + '\'' +
                ", type='" + type + '\'' +
                ", typeDef=" + typeDef +
                '}';
    }
}
