package com.autotune.analyzer.adapters;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;


/**
 * This adapter actually specifies the GSON to identify the type of implementation of DeviceDetails
 * to serialize or deserialize
 */
public class DeviceDetailsAdapter extends TypeAdapter<DeviceDetails> {

    @Override
    public void write(JsonWriter out, DeviceDetails value) throws IOException {
        out.beginObject();
        out.name("type").value(value.getType().name());

        if (value instanceof AcceleratorDeviceData accelerator) {
            out.name("manufacturer").value(accelerator.getManufacturer());
            out.name("modelName").value(accelerator.getModelName());
            out.name("hostName").value(accelerator.getHostName());
            out.name("UUID").value(accelerator.getUUID());
            out.name("deviceName").value(accelerator.getDeviceName());
            out.name("isMIGSupported").value(accelerator.isMIGSupported());
            out.name("isMIGPartition").value(accelerator.isMIGPartition());
        }
        // Add for other devices when added

        out.endObject();
    }

    @Override
    public DeviceDetails read(JsonReader in) throws IOException {
        String type = null;
        String manufacturer = null;
        String modelName = null;
        String hostName = null;
        String UUID = null;
        String deviceName = null;
        String profile = null;
        boolean isMIG = false;
        boolean isMIGPartition = false;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "type":
                    type = in.nextString();
                    break;
                case "manufacturer":
                    manufacturer = in.nextString();
                    break;
                case "modelName":
                    modelName = in.nextString();
                    break;
                case "hostName":
                    hostName = in.nextString();
                    break;
                case "UUID":
                    UUID = in.nextString();
                    break;
                case "deviceName":
                    deviceName = in.nextString();
                    break;
                case "isMIGSupported":
                    isMIG = in.nextBoolean();
                    break;
                case "isMIGPartition":
                    isMIGPartition = in.nextBoolean();
                    break;
                case "profile":
                    profile = in.nextString();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (type != null && type.equals(AnalyzerConstants.DeviceType.ACCELERATOR.name())) {
            return (DeviceDetails) new AcceleratorDeviceData(modelName, hostName, UUID, deviceName, profile, isMIG, isMIGPartition);
        }
        // Add for other device types if implemented in future

        return null;
    }
}

