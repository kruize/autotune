package com.autotune.analyzer.adapters;

import com.autotune.analyzer.utils.AnalyzerConstants;
import com.autotune.common.data.system.info.device.DeviceDetails;
import com.autotune.common.data.system.info.device.accelerator.AcceleratorDeviceData;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;

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
            out.name("isMIG").value(accelerator.isMIG());
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
        boolean isMIG = false;

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
                case "isMIG":
                    isMIG = in.nextBoolean();
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (type != null && type.equals(AnalyzerConstants.DeviceType.ACCELERATOR.name())) {
            return (DeviceDetails) new AcceleratorDeviceData(modelName, hostName, UUID, deviceName, isMIG);
        }
        // Add for other device types if implemented in future

        return null;
    }
}

