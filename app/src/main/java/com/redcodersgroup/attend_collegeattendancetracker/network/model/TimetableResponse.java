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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(TimetableResponse.Deserializer.class)
public class TimetableResponse {
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
        private Map<String, List<PeriodItem>> days = new HashMap<>();

        public Map<String, List<PeriodItem>> getDays() {
            return days != null ? days : new HashMap<>();
        }
    }

    public static class PeriodItem implements Comparable<PeriodItem> {
        @SerializedName(value = "period", alternate = {"period_no"})
        private int period;

        @SerializedName("start_time")
        private String startTime;

        @SerializedName("end_time")
        private String endTime;

        @SerializedName(value = "subject_code", alternate = {"code"})
        private String subjectCode;

        @SerializedName(value = "subject_name", alternate = {"name", "subject", "course_title"})
        private String subjectName;

        @SerializedName(value = "teacher_name", alternate = {"teacher", "faculty"})
        private String teacherName;

        @SerializedName(value = "room_no", alternate = {"room", "classroom", "roomno"})
        private String roomNo;

        @SerializedName("type")
        private String type;

        public int getPeriod() {
            return period;
        }

        public String getStartTime() {
            return startTime != null ? startTime : "";
        }

        public String getEndTime() {
            return endTime != null ? endTime : "";
        }

        public String getSubjectCode() {
            return subjectCode != null ? subjectCode : "";
        }

        public String getSubjectName() {
            return subjectName != null ? subjectName : "";
        }

        public String getTeacherName() {
            return teacherName != null ? teacherName : "";
        }

        public String getRoomNo() {
            return roomNo != null ? roomNo : "";
        }

        public String getType() {
            return type != null ? type : "Theory";
        }

        public boolean isLab() {
            return "Lab".equalsIgnoreCase(type) || "practical".equalsIgnoreCase(type);
        }

        @Override
        public int compareTo(PeriodItem o) {
            return Integer.compare(this.period, o.period);
        }
    }

    public static class Deserializer implements JsonDeserializer<TimetableResponse> {
        @Override
        public TimetableResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            TimetableResponse response = new TimetableResponse();
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

                // Case 1: Live BBSBEC ERP structure {"class": [...], "practical": [...]}
                if (dataObj.has("class") || dataObj.has("practical")) {
                    Map<String, List<PeriodItem>> daysMap = new HashMap<>();

                    if (dataObj.has("class") && dataObj.get("class").isJsonArray()) {
                        parseErpSlots(dataObj.getAsJsonArray("class"), "Theory", daysMap);
                    }
                    if (dataObj.has("practical") && dataObj.get("practical").isJsonArray()) {
                        parseErpSlots(dataObj.getAsJsonArray("practical"), "Lab", daysMap);
                    }

                    // Sort each day's periods by period number
                    for (List<PeriodItem> list : daysMap.values()) {
                        Collections.sort(list);
                    }
                    data.days = daysMap;
                }
                // Case 2: Standard {"days": {"Monday": [...]}}
                else if (dataObj.has("days") && !dataObj.get("days").isJsonNull()) {
                    Type mapType = new TypeToken<Map<String, List<PeriodItem>>>() {}.getType();
                    data.days = gson.fromJson(dataObj.get("days"), mapType);
                } else {
                    Type mapType = new TypeToken<Map<String, List<PeriodItem>>>() {}.getType();
                    try {
                        data.days = gson.fromJson(dataObj, mapType);
                    } catch (Exception ignored) {}
                }
            }

            response.data = data;
            return response;
        }

        private void parseErpSlots(JsonArray array, String defaultType, Map<String, List<PeriodItem>> daysMap) {
            for (JsonElement el : array) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();

                String dayRaw = obj.has("day") && !obj.get("day").isJsonNull() ? obj.get("day").getAsString() : "MONDAY";
                String dayName = normalizeDay(dayRaw);

                String hour = obj.has("hour") && !obj.get("hour").isJsonNull() ? obj.get("hour").getAsString() : "";
                int periodNo = mapHourToPeriod(hour);

                PeriodItem item = new PeriodItem();
                item.period = periodNo;
                item.startTime = hour;
                item.endTime = mapHourToEndTime(hour);
                item.type = defaultType;

                if (obj.has("subject") && obj.get("subject").isJsonObject()) {
                    JsonObject subjObj = obj.getAsJsonObject("subject");
                    if (subjObj.has("course_title") && !subjObj.get("course_title").isJsonNull()) {
                        item.subjectName = subjObj.get("course_title").getAsString();
                    } else if (subjObj.has("name") && !subjObj.get("name").isJsonNull()) {
                        item.subjectName = subjObj.get("name").getAsString();
                    }
                    if (subjObj.has("code") && !subjObj.get("code").isJsonNull()) {
                        item.subjectCode = subjObj.get("code").getAsString();
                    }
                    if (subjObj.has("is_lab") && !subjObj.get("is_lab").isJsonNull()) {
                        if (subjObj.get("is_lab").getAsBoolean()) {
                            item.type = "Lab";
                        }
                    }
                }

                if (obj.has("teacher") && obj.get("teacher").isJsonObject()) {
                    JsonObject teacherObj = obj.getAsJsonObject("teacher");
                    if (teacherObj.has("name") && !teacherObj.get("name").isJsonNull()) {
                        item.teacherName = teacherObj.get("name").getAsString();
                    }
                }

                if (obj.has("roomno") && !obj.get("roomno").isJsonNull()) {
                    item.roomNo = obj.get("roomno").getAsString();
                }

                List<PeriodItem> list = daysMap.computeIfAbsent(dayName, k -> new ArrayList<>());
                list.add(item);
            }
        }

        private static int mapHourToPeriod(String hour) {
            if (hour == null || hour.trim().isEmpty()) return 1;
            hour = hour.trim();
            if (hour.startsWith("09:00") || hour.startsWith("9:00")) return 1;
            if (hour.startsWith("09:50") || hour.startsWith("9:50")) return 2;
            if (hour.startsWith("10:40")) return 3;
            if (hour.startsWith("11:30")) return 4;
            if (hour.startsWith("13:10") || hour.startsWith("01:10") || hour.startsWith("1:10")) return 5;
            if (hour.startsWith("14:00") || hour.startsWith("02:00") || hour.startsWith("2:00")) return 6;
            if (hour.startsWith("14:50") || hour.startsWith("02:50") || hour.startsWith("2:50")) return 7;
            if (hour.startsWith("15:40") || hour.startsWith("03:40") || hour.startsWith("3:40")) return 8;
            try {
                int p = Integer.parseInt(hour);
                if (p >= 1 && p <= 9) return p;
            } catch (Exception ignored) {}
            return 1;
        }

        private static String mapHourToEndTime(String hour) {
            if (hour == null || hour.trim().isEmpty()) return "";
            hour = hour.trim();
            if (hour.startsWith("09:00") || hour.startsWith("9:00")) return "09:50";
            if (hour.startsWith("09:50") || hour.startsWith("9:50")) return "10:40";
            if (hour.startsWith("10:40")) return "11:30";
            if (hour.startsWith("11:30")) return "12:20";
            if (hour.startsWith("13:10") || hour.startsWith("01:10") || hour.startsWith("1:10")) return "14:00";
            if (hour.startsWith("14:00") || hour.startsWith("02:00") || hour.startsWith("2:00")) return "14:50";
            if (hour.startsWith("14:50") || hour.startsWith("02:50") || hour.startsWith("2:50")) return "15:40";
            if (hour.startsWith("15:40") || hour.startsWith("03:40") || hour.startsWith("3:40")) return "16:30";
            return "";
        }

        private static String normalizeDay(String raw) {
            if (raw == null || raw.trim().isEmpty()) return "Monday";
            raw = raw.trim();
            if (raw.equalsIgnoreCase("monday") || raw.equalsIgnoreCase("mon")) return "Monday";
            if (raw.equalsIgnoreCase("tuesday") || raw.equalsIgnoreCase("tue")) return "Tuesday";
            if (raw.equalsIgnoreCase("wednesday") || raw.equalsIgnoreCase("wed")) return "Wednesday";
            if (raw.equalsIgnoreCase("thursday") || raw.equalsIgnoreCase("thu")) return "Thursday";
            if (raw.equalsIgnoreCase("friday") || raw.equalsIgnoreCase("fri")) return "Friday";
            if (raw.equalsIgnoreCase("saturday") || raw.equalsIgnoreCase("sat")) return "Saturday";
            if (raw.equalsIgnoreCase("sunday") || raw.equalsIgnoreCase("sun")) return "Sunday";
            return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
        }
    }
}
