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

@JsonAdapter(AttendanceResponse.Deserializer.class)
public class AttendanceResponse {
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
        private double overallPercentage;
        private int totalDelivered;
        private int totalAttended;
        private List<SubjectItem> subjects = new ArrayList<>();

        public double getOverallPercentage() {
            return overallPercentage;
        }

        public int getTotalDelivered() {
            return totalDelivered;
        }

        public int getTotalAttended() {
            return totalAttended;
        }

        public List<SubjectItem> getSubjects() {
            return subjects != null ? subjects : new ArrayList<>();
        }

        public void setSubjects(List<SubjectItem> subjects) {
            this.subjects = subjects;
        }
    }

    public static class SubjectItem {
        @SerializedName(value = "subject_id", alternate = {"id"})
        private int subjectId;

        @SerializedName(value = "subject_code", alternate = {"code"})
        private String subjectCode;

        @SerializedName(value = "subject_name", alternate = {"name", "course_title", "title"})
        private String subjectName;

        @SerializedName(value = "teacher_name", alternate = {"teacher", "faculty"})
        private String teacherName;

        @SerializedName(value = "total_lectures", alternate = {"totalLectures", "delivered", "total", "total_classes", "total_attendance"})
        private int totalLectures;

        @SerializedName(value = "attended_lectures", alternate = {"attendedLectures", "attended", "present", "total_present"})
        private int attendedLectures;

        @SerializedName(value = "duty_leaves", alternate = {"dutyLeaves", "duty_leave"})
        private int dutyLeaves;

        @SerializedName(value = "medical_leaves", alternate = {"medicalLeaves", "medical_leave"})
        private int medicalLeaves;

        @SerializedName(value = "percentage", alternate = {"attendance_percentage"})
        private double percentage;

        @SerializedName("is_low_attendance")
        private boolean isLowAttendance;

        @SerializedName(value = "records", alternate = {"marks", "attendance_records"})
        private List<RecordItem> records = new ArrayList<>();

        public int getSubjectId() {
            return subjectId;
        }

        public void setSubjectId(int subjectId) {
            this.subjectId = subjectId;
        }

        public String getSubjectCode() {
            return subjectCode;
        }

        public void setSubjectCode(String subjectCode) {
            this.subjectCode = subjectCode;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public void setTeacherName(String teacherName) {
            this.teacherName = teacherName;
        }

        public int getTotalLectures() {
            return totalLectures;
        }

        public void setTotalLectures(int totalLectures) {
            this.totalLectures = totalLectures;
        }

        public int getAttendedLectures() {
            return attendedLectures;
        }

        public void setAttendedLectures(int attendedLectures) {
            this.attendedLectures = attendedLectures;
        }

        public int getDutyLeaves() {
            return dutyLeaves;
        }

        public void setDutyLeaves(int dutyLeaves) {
            this.dutyLeaves = dutyLeaves;
        }

        public int getMedicalLeaves() {
            return medicalLeaves;
        }

        public void setMedicalLeaves(int medicalLeaves) {
            this.medicalLeaves = medicalLeaves;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }

        public boolean isLowAttendance() {
            return isLowAttendance;
        }

        public void setLowAttendance(boolean lowAttendance) {
            isLowAttendance = lowAttendance;
        }

        public List<RecordItem> getRecords() {
            return records != null ? records : new ArrayList<>();
        }

        public void setRecords(List<RecordItem> records) {
            this.records = records;
        }
    }

    public static class RecordItem {
        @SerializedName(value = "attendance_id", alternate = {"id"})
        private int attendanceId;

        @SerializedName(value = "lecture_session_id", alternate = {"lecture_session", "session_id"})
        private int lectureSessionId;

        @SerializedName("date")
        private String date;

        @SerializedName(value = "hour", alternate = {"period"})
        private String hour;

        @SerializedName(value = "mark", alternate = {"status", "attendance", "attendance_status"})
        private String mark;

        @SerializedName("remarks")
        private String remarks;

        public int getAttendanceId() {
            return attendanceId;
        }

        public void setAttendanceId(int attendanceId) {
            this.attendanceId = attendanceId;
        }

        public int getLectureSessionId() {
            return lectureSessionId;
        }

        public void setLectureSessionId(int lectureSessionId) {
            this.lectureSessionId = lectureSessionId;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getHour() {
            return hour;
        }

        public void setHour(String hour) {
            this.hour = hour;
        }

        public String getMark() {
            return mark;
        }

        public void setMark(String mark) {
            this.mark = mark;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }

    public static class Deserializer implements JsonDeserializer<AttendanceResponse> {
        @Override
        public AttendanceResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            AttendanceResponse response = new AttendanceResponse();
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
            Data data = new Data();
            Gson gson = new Gson();

            if (dataElem.isJsonObject()) {
                JsonObject dataObj = dataElem.getAsJsonObject();
                if (dataObj.has("overall_percentage") && !dataObj.get("overall_percentage").isJsonNull()) {
                    try { data.overallPercentage = dataObj.get("overall_percentage").getAsDouble(); } catch (Exception ignored) {}
                }
                if (dataObj.has("total_delivered") && !dataObj.get("total_delivered").isJsonNull()) {
                    try { data.totalDelivered = dataObj.get("total_delivered").getAsInt(); } catch (Exception ignored) {}
                }
                if (dataObj.has("total_attended") && !dataObj.get("total_attended").isJsonNull()) {
                    try { data.totalAttended = dataObj.get("total_attended").getAsInt(); } catch (Exception ignored) {}
                }
                if (dataObj.has("subjects") && !dataObj.get("subjects").isJsonNull()) {
                    Type listType = new TypeToken<List<SubjectItem>>() {}.getType();
                    data.subjects = gson.fromJson(dataObj.get("subjects"), listType);
                } else if (dataObj.has("records") || dataObj.has("marks") || dataObj.has("total_lectures") || dataObj.has("attended_lectures") || dataObj.has("subject_id")) {
                    // Single subject attendance response
                    SubjectItem item = gson.fromJson(dataObj, SubjectItem.class);
                    if (item != null) {
                        if (item.getRecords() != null && !item.getRecords().isEmpty()) {
                            int total = item.getRecords().size();
                            int attended = 0;
                            for (RecordItem r : item.getRecords()) {
                                String m = r.getMark() != null ? r.getMark().trim().toUpperCase() : "";
                                if ("P".equals(m) || "PRESENT".equals(m) || "DL".equals(m) || "DUTY".equals(m) || "DUTY LEAVE".equals(m)) {
                                    attended++;
                                }
                            }
                            item.setTotalLectures(total);
                            item.setAttendedLectures(attended);
                            item.setPercentage(total > 0 ? (attended * 100.0) / total : 0.0);
                        }
                        data.subjects.add(item);
                    }
                }
            } else if (dataElem.isJsonArray()) {
                JsonArray arr = dataElem.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                    JsonObject first = arr.get(0).getAsJsonObject();
                    if (first.has("mark") || first.has("attendance_id") || first.has("date")) {
                        // Array of attendance records
                        Type recordListType = new TypeToken<List<RecordItem>>() {}.getType();
                        List<RecordItem> records = gson.fromJson(dataElem, recordListType);
                        SubjectItem item = new SubjectItem();
                        item.records = records;
                        data.subjects.add(item);
                    } else {
                        // Array of subjects
                        Type listType = new TypeToken<List<SubjectItem>>() {}.getType();
                        data.subjects = gson.fromJson(dataElem, listType);
                    }
                }
            }

            response.data = data;
            return response;
        }
    }
}
