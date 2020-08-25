package com.github.manolo8.darkbot.backpage.hangar;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Hangar {
    @SerializedName("hangarID") private int hangarId;
    @SerializedName("hangar_is_active") private boolean hangarActive;
    @SerializedName("config") private Map<String, Config> configs;
    private General general;

    public int getHangarId() {
        return hangarId;
    }

    public boolean isHangarActive() {
        return hangarActive;
    }

    public Config getConfig(int config) {
        return getConfigs().get(String.valueOf(config));
    }

    public Map<String, Config> getConfigs() {
        return configs;
    }

    public General getGeneral() {
        return general;
    }

    @Override
    public String toString() {
        return "Hangar{" +
                "hangarId=" + hangarId +
                ", hangarIsActive=" + hangarActive +
                ", configs=" + configs +
                ", general=" + general +
                '}';
    }

    public static class HangarAdapter implements JsonDeserializer<List<Hangar>> {

        public List<Hangar> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return json.isJsonArray() ? context.deserialize(json, typeOfT) :
                    json.getAsJsonObject().entrySet().stream()
                            .map(e -> (Hangar) context.deserialize(e.getValue(), Hangar.class))
                            .collect(Collectors.toList());
        }
    }
}
