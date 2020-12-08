package com.autotune.collection;

/**
 * Holds information about the fields in type tag in the autotune object yaml
 */
public class TypeInfo
{
    private String typeName;
    private String typeValue;

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(String typeValue) {
        this.typeValue = typeValue;
    }

    @Override
    public String toString() {
        return "TypeInfo{" +
                "typeName='" + typeName + '\'' +
                ", typeValue='" + typeValue + '\'' +
                '}';
    }
}
