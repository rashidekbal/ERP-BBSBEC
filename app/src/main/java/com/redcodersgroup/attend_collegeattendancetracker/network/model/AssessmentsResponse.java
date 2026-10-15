package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

@JsonAdapter(AssessmentsResponse.Deserializer.class)
public class AssessmentsResponse {
    private int status;
    private String message;
    private List<AssessmentItem> data = new ArrayList<>();

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<AssessmentItem> getData() {
        return data != null ? data : new ArrayList<>();
    }

    public static class AssessmentItem {
        @SerializedName(value = "subject_code", alternate = {"code"})
        private String subjectCode;

        @SerializedName(value = "subject_name", alternate = {"name", "subject"})
        private String subjectName;

        @SerializedName(value = "mst1_marks", alternate = {"mst1", "mst_1_marks"})
        private Double mst1Marks;

        @SerializedName("mst1_max")
        private double mst1Max = 24.0;

        @SerializedName(value = "mst2_marks", alternate = {"mst2", "mst_2_marks"})
        private Double mst2Marks;

        @SerializedName("mst2_max")
        private double mst2Max = 24.0;

        @SerializedName(value = "mst3_marks", alternate = {"mst_3_marks", "mst3"})
        private Double mst3Marks;

        @SerializedName(value = "mst3_max", alternate = {"mst_3_max"})
        private Double mst3Max = 24.0;

        @SerializedName("assignment_marks")
        private double assignmentMarks;

        @SerializedName("assignment_max")
        private double assignmentMax;

        @SerializedName("attendance_marks")
        private double attendanceMarks;

        @SerializedName("attendance_max")
        private double attendanceMax;

        @SerializedName("total_internal")
        private double totalInternal;

        @SerializedName("max_internal")
        private double maxInternal;

        public String getSubjectCode() {
            return subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public double getMst1Marks() {
            return mst1Marks != null ? mst1Marks : -1.0;
        }

        public double getMst1Max() {
            return mst1Max;
        }

        public double getMst2Marks() {
            return mst2Marks != null ? mst2Marks : -1.0;
        }

        public double getMst2Max() {
            return mst2Max;
        }

        public double getMst3Marks() {
            return mst3Marks != null ? mst3Marks : -1.0;
        }

        public double getMst3Max() {
            return mst3Max != null ? mst3Max : 24.0;
        }

        public double getAssignmentMarks() {
            return assignmentMarks;
        }

        public double getAssignmentMax() {
            return assignmentMax;
        }

        public double getAttendanceMarks() {
            return attendanceMarks;
        }

        public double getAttendanceMax() {
            return attendanceMax;
        }

        public double getTotalInternal() {
            return totalInternal;
        }

        public double getMaxInternal() {
            return maxInternal;
        }
    }

    public static class Deserializer implements JsonDeserializer<AssessmentsResponse> {
        @Override
        public AssessmentsResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            AssessmentsResponse response = new AssessmentsResponse();
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
            Type listType = new TypeToken<List<AssessmentItem>>() {}.getType();

            if (dataElem.isJsonArray()) {
                response.data = gson.fromJson(dataElem, listType);
            } else if (dataElem.isJsonObject()) {
                JsonObject dataObj = dataElem.getAsJsonObject();
                if (dataObj.has("results") && dataObj.get("results").isJsonArray()) {
                    JsonArray resultsArr = dataObj.getAsJsonArray("results");
                    java.util.Map<Integer, AssessmentItem> map = new java.util.HashMap<>();
                    for (JsonElement el : resultsArr) {
                        if (!el.isJsonObject()) continue;
                        JsonObject resObj = el.getAsJsonObject();
                        int subjectId = resObj.has("subject") && !resObj.get("subject").isJsonNull() ? resObj.get("subject").getAsInt() : 0;
                        String name = resObj.has("name") && !resObj.get("name").isJsonNull() ? resObj.get("name").getAsString() : "";
                        double maxMarks = resObj.has("maxmarks") && !resObj.get("maxmarks").isJsonNull() ? resObj.get("maxmarks").getAsDouble() : 24.0;

                        AssessmentItem item = map.computeIfAbsent(subjectId, k -> {
                            AssessmentItem a = new AssessmentItem();
                            a.subjectCode = String.valueOf(subjectId);
                            return a;
                        });

                        String normName = name.toUpperCase().replace(" ", "").replace("-", "");
                        if (normName.contains("MST1")) {
                            item.mst1Max = maxMarks;
                            if (resObj.has("marks") && !resObj.get("marks").isJsonNull()) {
                                item.mst1Marks = resObj.get("marks").getAsDouble();
                            }
                        } else if (normName.contains("MST2")) {
                            item.mst2Max = maxMarks;
                            if (resObj.has("marks") && !resObj.get("marks").isJsonNull()) {
                                item.mst2Marks = resObj.get("marks").getAsDouble();
                            }
                        } else if (normName.contains("MST3")) {
                            item.mst3Max = maxMarks;
                            if (resObj.has("marks") && !resObj.get("marks").isJsonNull()) {
                                item.mst3Marks = resObj.get("marks").getAsDouble();
                            }
                        }
                    }
                    response.data = new ArrayList<>(map.values());
                } else if (dataObj.has("assessments") && !dataObj.get("assessments").isJsonNull()) {
                    response.data = gson.fromJson(dataObj.get("assessments"), listType);
                }
            }

            return response;
        }
    }
}
