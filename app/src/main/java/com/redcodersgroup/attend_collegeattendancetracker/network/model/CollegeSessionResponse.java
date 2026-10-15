package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

@JsonAdapter(CollegeSessionResponse.Deserializer.class)
public class CollegeSessionResponse {
    private int status;
    private String message;
    private Data data;

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Data getData() {
        return data;
    }

    public static class Data {
        @SerializedName(value = "session_id", alternate = {"id"})
        private int sessionId;

        @SerializedName("name")
        private String name;

        @SerializedName("start_date")
        private String startDate;

        @SerializedName("end_date")
        private String endDate;

        @SerializedName("is_current")
        private boolean isCurrent;

        public int getSessionId() {
            return sessionId;
        }

        public String getName() {
            return name != null ? name : "";
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public boolean isCurrent() {
            return isCurrent;
        }
    }

    public static class Deserializer implements JsonDeserializer<CollegeSessionResponse> {
        @Override
        public CollegeSessionResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            CollegeSessionResponse response = new CollegeSessionResponse();
            if (!json.isJsonObject()) return response;

            JsonObject root = json.getAsJsonObject();
            if (root.has("status") && !root.get("status").isJsonNull()) {
                try { response.status = root.get("status").getAsInt(); } catch (Exception ignored) {}
            }
            if (root.has("message") && !root.get("message").isJsonNull()) {
                try { response.message = root.get("message").getAsString(); } catch (Exception ignored) {}
            }

            if (!root.has("data") || root.get("data").isJsonNull()) {
                return response;
            }

            JsonElement dataElem = root.get("data");
            JsonObject obj = null;
            if (dataElem.isJsonObject()) {
                obj = dataElem.getAsJsonObject();
            } else if (dataElem.isJsonArray()) {
                JsonArray arr = dataElem.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                    obj = arr.get(0).getAsJsonObject();
                }
            }

            if (obj != null) {
                Data data = new Data();
                if (obj.has("session_id") && !obj.get("session_id").isJsonNull()) {
                    try { data.sessionId = obj.get("session_id").getAsInt(); } catch (Exception ignored) {}
                } else if (obj.has("id") && !obj.get("id").isJsonNull()) {
                    try { data.sessionId = obj.get("id").getAsInt(); } catch (Exception ignored) {}
                }

                if (obj.has("name") && !obj.get("name").isJsonNull()) {
                    data.name = obj.get("name").getAsString();
                } else if (obj.has("session") && !obj.get("session").isJsonNull()) {
                    data.name = obj.get("session").getAsString();
                }

                if (obj.has("start_date") && !obj.get("start_date").isJsonNull()) {
                    data.startDate = obj.get("start_date").getAsString();
                }
                if (obj.has("end_date") && !obj.get("end_date").isJsonNull()) {
                    data.endDate = obj.get("end_date").getAsString();
                }
                if (obj.has("is_current") && !obj.get("is_current").isJsonNull()) {
                    try { data.isCurrent = obj.get("is_current").getAsBoolean(); } catch (Exception ignored) {}
                }
                response.data = data;
            }

            return response;
        }
    }
}
