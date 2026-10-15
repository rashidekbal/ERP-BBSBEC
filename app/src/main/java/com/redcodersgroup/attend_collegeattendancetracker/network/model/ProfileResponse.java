package com.redcodersgroup.attend_collegeattendancetracker.network.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@JsonAdapter(ProfileResponse.Deserializer.class)
public class ProfileResponse {
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
        private int id;
        private String rollNo;
        private String universityRollNo;
        private String firstName;
        private String middleName;
        private String lastName;
        private String email;
        private String mobileNo;
        private String branch;
        private int semester;
        private String course;
        private String classGroup;
        private String practicalGroup;
        private String batch;
        private String admissionType;
        private String scholarship;
        private String photo;
        private String signature;

        // Personal & Family
        private String fatherName;
        private String motherName;
        private String fatherProfession;
        private String motherProfession;
        private String parentMobile;
        private String dob;
        private String aadhar;
        private String gender;
        private String category;
        private String status;
        private String bloodGroup;

        // Address
        private String communicationAddress;
        private String permanentAddress;

        // Education
        private String highSchoolBoard;
        private String highSchoolPassingYear;
        private String highSchoolMarks;
        private String highSchoolMaxMarks;
        private String highSchoolPercentage;
        private String interBoard;
        private String interPassingYear;
        private String interMarks;
        private String interMaxMarks;
        private String interPercentage;

        private List<Integer> selectedSubjects = new ArrayList<>();

        public int getId() { return id; }
        public String getRollNo() { return rollNo != null ? rollNo : ""; }
        public String getUniversityRollNo() { return universityRollNo != null ? universityRollNo : ""; }
        public String getFirstName() { return firstName != null ? firstName : ""; }
        public String getMiddleName() { return middleName != null ? middleName : ""; }
        public String getLastName() { return lastName != null ? lastName : ""; }
        public String getFullName() {
            StringBuilder sb = new StringBuilder();
            if (firstName != null && !firstName.isEmpty()) sb.append(firstName);
            if (middleName != null && !middleName.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(middleName);
            }
            if (lastName != null && !lastName.isEmpty()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(lastName);
            }
            String res = sb.toString().trim();
            return res.isEmpty() ? "Student" : res;
        }
        public String getEmail() { return email != null ? email : ""; }
        public String getMobileNo() { return mobileNo != null ? mobileNo : ""; }
        public String getBranch() { return branch != null ? branch : ""; }
        public int getSemester() { return semester > 0 ? semester : 1; }
        public String getCourse() { return course != null ? course : ""; }
        public String getClassGroup() { return classGroup != null ? classGroup : ""; }
        public String getPracticalGroup() { return practicalGroup != null ? practicalGroup : ""; }
        public String getBatch() { return batch != null ? batch : ""; }
        public String getAdmissionType() { return admissionType != null ? admissionType : ""; }
        public String getScholarship() { return scholarship != null ? scholarship : ""; }
        public String getPhoto() { return photo; }
        public String getSignature() { return signature; }

        public String getFatherName() { return fatherName != null ? fatherName : ""; }
        public String getMotherName() { return motherName != null ? motherName : ""; }
        public String getFatherProfession() { return fatherProfession != null ? fatherProfession : ""; }
        public String getMotherProfession() { return motherProfession != null ? motherProfession : ""; }
        public String getParentMobile() { return parentMobile != null ? parentMobile : ""; }
        public String getDob() { return dob != null ? dob : ""; }
        public String getAadhar() { return aadhar != null ? aadhar : ""; }
        public String getGender() { return gender != null ? gender : ""; }
        public String getCategory() { return category != null ? category : ""; }
        public String getStatus() { return status != null ? status : ""; }
        public String getBloodGroup() { return bloodGroup != null ? bloodGroup : ""; }

        public String getCommunicationAddress() { return communicationAddress != null ? communicationAddress : ""; }
        public String getPermanentAddress() { return permanentAddress != null ? permanentAddress : ""; }

        public String getHighSchoolBoard() { return highSchoolBoard != null ? highSchoolBoard : ""; }
        public String getHighSchoolPassingYear() { return highSchoolPassingYear != null ? highSchoolPassingYear : ""; }
        public String getHighSchoolMarks() { return highSchoolMarks != null ? highSchoolMarks : ""; }
        public String getHighSchoolMaxMarks() { return highSchoolMaxMarks != null ? highSchoolMaxMarks : ""; }
        public String getHighSchoolPercentage() { return highSchoolPercentage != null ? highSchoolPercentage : ""; }

        public String getInterBoard() { return interBoard != null ? interBoard : ""; }
        public String getInterPassingYear() { return interPassingYear != null ? interPassingYear : ""; }
        public String getInterMarks() { return interMarks != null ? interMarks : ""; }
        public String getInterMaxMarks() { return interMaxMarks != null ? interMaxMarks : ""; }
        public String getInterPercentage() { return interPercentage != null ? interPercentage : ""; }

        public List<Integer> getSelectedSubjects() { return selectedSubjects != null ? selectedSubjects : new ArrayList<>(); }
    }

    public static class Deserializer implements JsonDeserializer<ProfileResponse> {
        @Override
        public ProfileResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ProfileResponse response = new ProfileResponse();
            if (!json.isJsonObject()) return response;

            JsonObject root = json.getAsJsonObject();
            if (root.has("status") && !root.get("status").isJsonNull()) {
                try {
                    response.status = root.get("status").getAsInt();
                } catch (Exception ignored) {}
            }
            if (root.has("message") && !root.get("message").isJsonNull()) {
                try {
                    response.message = root.get("message").getAsString();
                } catch (Exception ignored) {}
            }

            if (!root.has("data") || root.get("data").isJsonNull()) {
                return response;
            }

            JsonElement dataElem = root.get("data");
            JsonObject itemObj = null;

            if (dataElem.isJsonArray()) {
                JsonArray arr = dataElem.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).isJsonObject()) {
                    itemObj = arr.get(0).getAsJsonObject();
                }
            } else if (dataElem.isJsonObject()) {
                itemObj = dataElem.getAsJsonObject();
            }

            if (itemObj != null) {
                Data data = new Data();

                if (itemObj.has("id") && !itemObj.get("id").isJsonNull()) {
                    try { data.id = itemObj.get("id").getAsInt(); } catch (Exception ignored) {}
                }

                // Roll No mappings
                data.rollNo = extractString(itemObj, "collegerollno", "roll_no", "rollno");
                data.universityRollNo = extractString(itemObj, "universityrollno", "reg_no", "regno");

                // Names
                data.firstName = extractString(itemObj, "firstname", "first_name");
                data.middleName = extractString(itemObj, "middlename", "middle_name");
                data.lastName = extractString(itemObj, "lastname", "last_name");

                // Contacts
                data.email = extractString(itemObj, "studentemail", "email");
                data.mobileNo = extractString(itemObj, "studentmobile", "mobileno", "mobile_no");
                data.parentMobile = extractString(itemObj, "parentmobile", "parent_mobile");

                // Academic & Groups
                data.branch = extractNameOrString(itemObj, "branch");
                data.course = extractNameOrString(itemObj, "course");
                data.classGroup = extractNameOrString(itemObj, "classgroup", "class_group", "section");
                data.practicalGroup = extractNameOrString(itemObj, "practicalgroup", "practical_group");
                data.batch = extractNameOrString(itemObj, "batch");
                data.admissionType = extractNameOrString(itemObj, "admissiontype", "admission_type");

                // Semester
                if (itemObj.has("semester") && !itemObj.get("semester").isJsonNull()) {
                    JsonElement semElem = itemObj.get("semester");
                    if (semElem.isJsonObject()) {
                        JsonObject semObj = semElem.getAsJsonObject();
                        if (semObj.has("name") && !semObj.get("name").isJsonNull()) {
                            try { data.semester = semObj.get("name").getAsInt(); } catch (Exception ignored) {}
                        }
                    } else if (semElem.isJsonPrimitive()) {
                        try { data.semester = semElem.getAsInt(); } catch (Exception ignored) {}
                    }
                }

                // Scholarship
                if (itemObj.has("scholarshiptypes") && itemObj.get("scholarshiptypes").isJsonArray()) {
                    JsonArray sArr = itemObj.getAsJsonArray("scholarshiptypes");
                    if (sArr.size() > 0 && sArr.get(0).isJsonObject()) {
                        JsonObject sObj = sArr.get(0).getAsJsonObject();
                        if (sObj.has("name") && !sObj.get("name").isJsonNull()) {
                            data.scholarship = sObj.get("name").getAsString();
                        }
                    }
                }
                if (data.scholarship.isEmpty()) {
                    data.scholarship = extractString(itemObj, "scholarship");
                }

                // Personal details
                data.fatherName = extractString(itemObj, "fathername", "father_name");
                data.motherName = extractString(itemObj, "mothername", "mother_name");
                data.fatherProfession = extractString(itemObj, "fatherprofession", "father_profession");
                data.motherProfession = extractString(itemObj, "motherprofession", "mother_profession");
                data.gender = extractString(itemObj, "gendertype", "gender");
                data.dob = extractString(itemObj, "dateofbirth", "dob");
                data.aadhar = extractString(itemObj, "aadhar");
                data.bloodGroup = extractString(itemObj, "blood_group", "bloodgroup");
                data.category = extractString(itemObj, "categorytype", "category");
                data.status = extractString(itemObj, "status");

                // Addresses
                data.communicationAddress = buildFullAddress(itemObj,
                        "communicationaddress", "communicationcity", "communicationstate", "communicationcountry", "communicationpincode",
                        "address", "city", "state", "pincode");

                data.permanentAddress = buildFullAddress(itemObj,
                        "parmanentaddress", "permanentcity", "permanentstate", "permanentcountry", "permanentpincode",
                        "permanent_address", "permanent_city", "permanent_state", "permanent_pincode");

                if (data.permanentAddress.isEmpty()) {
                    data.permanentAddress = data.communicationAddress;
                }

                // High School / 10th
                data.highSchoolBoard = extractString(itemObj, "highschoolboard", "tenth_board");
                data.highSchoolPassingYear = extractString(itemObj, "highschoolpassingyear", "tenth_passing_year");
                data.highSchoolMarks = extractString(itemObj, "highschoolmarksobtained", "tenth_marks");
                data.highSchoolMaxMarks = extractString(itemObj, "highschoolmaxmarks", "tenth_max_marks");
                data.highSchoolPercentage = extractString(itemObj, "highschoolpercentage", "tenth_percentage");

                // Intermediate / 12th
                data.interBoard = extractString(itemObj, "interboard", "twelfth_board");
                data.interPassingYear = extractString(itemObj, "interpassingyear", "twelfth_passing_year");
                data.interMarks = extractString(itemObj, "intermarksobtained", "twelfth_marks");
                data.interMaxMarks = extractString(itemObj, "intermaxmarks", "twelfth_max_marks");
                data.interPercentage = extractString(itemObj, "interpercentage", "twelfth_percentage");

                // Photos & Signatures
                data.photo = extractString(itemObj, "photo");
                data.signature = extractString(itemObj, "signature");

                if (itemObj.has("selectedsubjects") && itemObj.get("selectedsubjects").isJsonArray()) {
                    data.selectedSubjects = new ArrayList<>();
                    for (JsonElement el : itemObj.getAsJsonArray("selectedsubjects")) {
                        try {
                            data.selectedSubjects.add(el.getAsInt());
                        } catch (Exception ignored) {}
                    }
                }

                response.data = data;
            }

            return response;
        }

        private static String extractString(JsonObject obj, String... keys) {
            for (String key : keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull()) {
                    JsonElement el = obj.get(key);
                    if (el.isJsonPrimitive()) {
                        return el.getAsString().trim();
                    }
                }
            }
            return "";
        }

        private static String extractNameOrString(JsonObject obj, String... keys) {
            for (String key : keys) {
                if (obj.has(key) && !obj.get(key).isJsonNull()) {
                    JsonElement el = obj.get(key);
                    if (el.isJsonObject()) {
                        JsonObject o = el.getAsJsonObject();
                        if (o.has("name") && !o.get("name").isJsonNull()) {
                            return o.get("name").getAsString().trim();
                        }
                    } else if (el.isJsonPrimitive()) {
                        return el.getAsString().trim();
                    }
                }
            }
            return "";
        }

        private static String buildFullAddress(JsonObject obj,
                                               String streetKey1, String cityKey1, String stateKey1, String countryKey1, String pinKey1,
                                               String streetKey2, String cityKey2, String stateKey2, String pinKey2) {
            String street = extractString(obj, streetKey1, streetKey2);
            String city = extractString(obj, cityKey1, cityKey2);
            String state = extractString(obj, stateKey1, stateKey2);
            String country = extractString(obj, countryKey1);
            String pin = extractString(obj, pinKey1, pinKey2);

            StringBuilder sb = new StringBuilder();
            if (!street.isEmpty()) sb.append(street);
            if (!city.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(city);
            }
            if (!state.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(state);
            }
            if (!pin.isEmpty()) {
                if (sb.length() > 0) sb.append(" - ");
                sb.append(pin);
            }
            if (!country.isEmpty()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(country);
            }
            return sb.toString().trim();
        }
    }
}
