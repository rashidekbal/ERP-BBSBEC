package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@JsonAdapter(EnrolledSubjectsResponse.Deserializer.class)
public class EnrolledSubjectsResponse {
    private int status;
    private String message;
    private List<SubjectItem> data = new ArrayList<>();

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<SubjectItem> getData() {
        return data != null ? data : new ArrayList<>();
    }

    public static class SubjectItem {
        @SerializedName(value = "id", alternate = {"subject_id"})
        private int id;

        @SerializedName(value = "subject_code", alternate = {"code"})
        private String subjectCode;

        @SerializedName(value = "course_title", alternate = {"subject_name", "name", "title"})
        private String subjectName;

        @SerializedName("credits")
        private int credits;

        @SerializedName("type")
        private String type;

        @SerializedName("is_lab")
        private boolean isLab;

        public int getId() {
            return id;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public int getCredits() {
            return credits;
        }

        public String getType() {
            return type;
        }

        public boolean isLab() {
            return isLab || (type != null && (type.equalsIgnoreCase("Lab") || type.equalsIgnoreCase("practical")));
        }

        public String getAttendanceQueryType() {
            if (isLab()) return "lab";
            if (type != null && type.toLowerCase().contains("tut")) return "tutorial";
            return "theory";
        }
    }

    public static class Deserializer implements JsonDeserializer<EnrolledSubjectsResponse> {
        @Override
        public EnrolledSubjectsResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            EnrolledSubjectsResponse response = new EnrolledSubjectsResponse();
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
            Gson gson = new Gson();
            Type listType = new TypeToken<List<SubjectItem>>() {}.getType();

            if (dataElem.isJsonArray()) {
                response.data = gson.fromJson(dataElem, listType);
            } else if (dataElem.isJsonObject()) {
                JsonObject dataObj = dataElem.getAsJsonObject();
                if (dataObj.has("subjects") && !dataObj.get("subjects").isJsonNull()) {
                    response.data = gson.fromJson(dataObj.get("subjects"), listType);
                }
            }

            return response;
        }
    }
}
