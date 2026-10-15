package com.redcodersgroup.attend_collegeattendancetracker.db.user;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "user")
public class UserSchema {
    @PrimaryKey(autoGenerate = true)
    private int uid;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "collegeName")
    private String collegeName;

    @ColumnInfo(name = "department")
    private String department;
    @ColumnInfo(name = "branch")
    private String branch;
    @ColumnInfo(name = "semester")
    private int semester;
    @ColumnInfo(name = "sem_startDate")
    private long semStartDate;
    @ColumnInfo(name = "sem_endDate")
    private long semEndDate;
    @ColumnInfo(name = "working_days")
    private int workingDays;
    @ColumnInfo(name = "first_period_start_time")
    private long firstPeriodStartTime;
    @ColumnInfo(name = "attendanceCriteria")
    private int attendanceCriteria;
    @ColumnInfo(name = "themeColor")
    private String themeColor;
    @ColumnInfo(name = "maxMarks", defaultValue = "24")
    private int maxMarks;
    @ColumnInfo(name = "rollNumber")
    private String rollNumber;
    @ColumnInfo(name = "phone")
    private String phone;
    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "universityRollNumber")
    private String universityRollNumber;

    @ColumnInfo(name = "classGroup")
    private String classGroup;

    @ColumnInfo(name = "course")
    private String course;

    @ColumnInfo(name = "erpStudentId", defaultValue = "0")
    private int erpStudentId;

    @ColumnInfo(name = "photoUrl")
    private String photoUrl;

    @ColumnInfo(name = "fatherName")
    private String fatherName;

    @ColumnInfo(name = "motherName")
    private String motherName;

    @ColumnInfo(name = "fatherProfession")
    private String fatherProfession;

    @ColumnInfo(name = "motherProfession")
    private String motherProfession;

    @ColumnInfo(name = "gender")
    private String gender;

    @ColumnInfo(name = "dob")
    private String dob;

    @ColumnInfo(name = "aadhar")
    private String aadhar;

    @ColumnInfo(name = "bloodGroup")
    private String bloodGroup;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "status")
    private String status;

    @ColumnInfo(name = "parentMobile")
    private String parentMobile;

    @ColumnInfo(name = "batch")
    private String batch;

    @ColumnInfo(name = "admissionType")
    private String admissionType;

    @ColumnInfo(name = "practicalGroup")
    private String practicalGroup;

    @ColumnInfo(name = "communicationAddress")
    private String communicationAddress;

    @ColumnInfo(name = "permanentAddress")
    private String permanentAddress;

    @ColumnInfo(name = "highSchoolBoard")
    private String highSchoolBoard;

    @ColumnInfo(name = "highSchoolPassingYear")
    private String highSchoolPassingYear;

    @ColumnInfo(name = "highSchoolMarks")
    private String highSchoolMarks;

    @ColumnInfo(name = "highSchoolMaxMarks")
    private String highSchoolMaxMarks;

    @ColumnInfo(name = "highSchoolPercentage")
    private String highSchoolPercentage;

    @ColumnInfo(name = "interBoard")
    private String interBoard;

    @ColumnInfo(name = "interPassingYear")
    private String interPassingYear;

    @ColumnInfo(name = "interMarks")
    private String interMarks;

    @ColumnInfo(name = "interMaxMarks")
    private String interMaxMarks;

    @ColumnInfo(name = "interPercentage")
    private String interPercentage;

    @ColumnInfo(name = "scholarship")
    private String scholarship;

    public UserSchema(int uid, String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor, int maxMarks, String rollNumber, String phone, String email) {
        this.uid = uid;
        this.name = name;
        this.collegeName = collegeName;
        this.department = department;
        this.branch = branch;
        this.semester = semester;
        this.semStartDate = semStartDate;
        this.semEndDate = semEndDate;
        this.workingDays = workingDays;
        this.firstPeriodStartTime = firstPeriodStartTime;
        this.attendanceCriteria = attendanceCriteria;
        this.themeColor = themeColor;
        this.maxMarks = maxMarks <= 0 ? 24 : maxMarks;
        this.rollNumber = rollNumber;
        this.phone = phone;
        this.email = email;
    }

    @Ignore
    public UserSchema(int uid, String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor, int maxMarks) {
        this(uid, name, collegeName, department, branch, semester, semStartDate, semEndDate, workingDays, firstPeriodStartTime, attendanceCriteria, themeColor, maxMarks, null, null, null);
    }

    @Ignore
    public UserSchema(int uid, String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor) {
        this(uid, name, collegeName, department, branch, semester, semStartDate, semEndDate, workingDays, firstPeriodStartTime, attendanceCriteria, themeColor, 24, null, null, null);
    }

    @Ignore
    public UserSchema(String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor, int maxMarks, String rollNumber, String phone, String email) {
        this.name = name;
        this.collegeName = collegeName;
        this.department = department;
        this.branch = branch;
        this.semester = semester;
        this.semStartDate = semStartDate;
        this.semEndDate = semEndDate;
        this.workingDays = workingDays;
        this.firstPeriodStartTime = firstPeriodStartTime;
        this.attendanceCriteria = attendanceCriteria;
        this.themeColor = themeColor;
        this.maxMarks = maxMarks <= 0 ? 24 : maxMarks;
        this.rollNumber = rollNumber;
        this.phone = phone;
        this.email = email;
    }

    @Ignore
    public UserSchema(String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor, int maxMarks) {
        this(name, collegeName, department, branch, semester, semStartDate, semEndDate, workingDays, firstPeriodStartTime, attendanceCriteria, themeColor, maxMarks, null, null, null);
    }

    @Ignore
    public UserSchema(String name, String collegeName, String department, String branch, int semester, long semStartDate, long semEndDate, int workingDays, long firstPeriodStartTime, int attendanceCriteria, String themeColor) {
        this(name, collegeName, department, branch, semester, semStartDate, semEndDate, workingDays, firstPeriodStartTime, attendanceCriteria, themeColor, 24, null, null, null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCollegeName() {
        return collegeName;
    }

    public void setCollegeName(String collegeName) {
        this.collegeName = collegeName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public int getSemester() {
        return semester;
    }

    public void setSemester(int semester) {
        this.semester = semester;
    }

    public long getSemStartDate() {
        return semStartDate;
    }

    public void setSemStartDate(long semStartDate) {
        this.semStartDate = semStartDate;
    }

    public long getSemEndDate() {
        return semEndDate;
    }

    public void setSemEndDate(long semEndDate) {
        this.semEndDate = semEndDate;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(int workingDays) {
        this.workingDays = workingDays;
    }

    public long getFirstPeriodStartTime() {
        return firstPeriodStartTime;
    }

    public void setFirstPeriodStartTime(long firstPeriodStartTime) {
        this.firstPeriodStartTime = firstPeriodStartTime;
    }

    public int getAttendanceCriteria() {
        return attendanceCriteria;
    }

    public void setAttendanceCriteria(int attendanceCriteria) {
        this.attendanceCriteria = attendanceCriteria;
    }

    public String getThemeColor() {
        return themeColor;
    }

    public void setThemeColor(String themeColor) {
        this.themeColor = themeColor;
    }

    public int getMaxMarks() {
        return maxMarks <= 0 ? 24 : maxMarks;
    }

    public void setMaxMarks(int maxMarks) {
        this.maxMarks = maxMarks <= 0 ? 24 : maxMarks;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUniversityRollNumber() {
        return universityRollNumber != null ? universityRollNumber : "";
    }

    public void setUniversityRollNumber(String universityRollNumber) {
        this.universityRollNumber = universityRollNumber;
    }

    public String getClassGroup() {
        return classGroup != null ? classGroup : "";
    }

    public void setClassGroup(String classGroup) {
        this.classGroup = classGroup;
    }

    public String getCourse() {
        return course != null ? course : "";
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public int getErpStudentId() {
        return erpStudentId;
    }

    public void setErpStudentId(int erpStudentId) {
        this.erpStudentId = erpStudentId;
    }

    public String getPhotoUrl() {
        return photoUrl != null ? photoUrl : "";
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getFatherName() { return fatherName != null ? fatherName : ""; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName != null ? motherName : ""; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getFatherProfession() { return fatherProfession != null ? fatherProfession : ""; }
    public void setFatherProfession(String fatherProfession) { this.fatherProfession = fatherProfession; }

    public String getMotherProfession() { return motherProfession != null ? motherProfession : ""; }
    public void setMotherProfession(String motherProfession) { this.motherProfession = motherProfession; }

    public String getGender() { return gender != null ? gender : ""; }
    public void setGender(String gender) { this.gender = gender; }

    public String getDob() { return dob != null ? dob : ""; }
    public void setDob(String dob) { this.dob = dob; }

    public String getAadhar() { return aadhar != null ? aadhar : ""; }
    public void setAadhar(String aadhar) { this.aadhar = aadhar; }

    public String getBloodGroup() { return bloodGroup != null ? bloodGroup : ""; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getCategory() { return category != null ? category : ""; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status != null ? status : ""; }
    public void setStatus(String status) { this.status = status; }

    public String getParentMobile() { return parentMobile != null ? parentMobile : ""; }
    public void setParentMobile(String parentMobile) { this.parentMobile = parentMobile; }

    public String getBatch() { return batch != null ? batch : ""; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getAdmissionType() { return admissionType != null ? admissionType : ""; }
    public void setAdmissionType(String admissionType) { this.admissionType = admissionType; }

    public String getPracticalGroup() { return practicalGroup != null ? practicalGroup : ""; }
    public void setPracticalGroup(String practicalGroup) { this.practicalGroup = practicalGroup; }

    public String getCommunicationAddress() { return communicationAddress != null ? communicationAddress : ""; }
    public void setCommunicationAddress(String communicationAddress) { this.communicationAddress = communicationAddress; }

    public String getPermanentAddress() { return permanentAddress != null ? permanentAddress : ""; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getHighSchoolBoard() { return highSchoolBoard != null ? highSchoolBoard : ""; }
    public void setHighSchoolBoard(String highSchoolBoard) { this.highSchoolBoard = highSchoolBoard; }

    public String getHighSchoolPassingYear() { return highSchoolPassingYear != null ? highSchoolPassingYear : ""; }
    public void setHighSchoolPassingYear(String highSchoolPassingYear) { this.highSchoolPassingYear = highSchoolPassingYear; }

    public String getHighSchoolMarks() { return highSchoolMarks != null ? highSchoolMarks : ""; }
    public void setHighSchoolMarks(String highSchoolMarks) { this.highSchoolMarks = highSchoolMarks; }

    public String getHighSchoolMaxMarks() { return highSchoolMaxMarks != null ? highSchoolMaxMarks : ""; }
    public void setHighSchoolMaxMarks(String highSchoolMaxMarks) { this.highSchoolMaxMarks = highSchoolMaxMarks; }

    public String getHighSchoolPercentage() { return highSchoolPercentage != null ? highSchoolPercentage : ""; }
    public void setHighSchoolPercentage(String highSchoolPercentage) { this.highSchoolPercentage = highSchoolPercentage; }

    public String getInterBoard() { return interBoard != null ? interBoard : ""; }
    public void setInterBoard(String interBoard) { this.interBoard = interBoard; }

    public String getInterPassingYear() { return interPassingYear != null ? interPassingYear : ""; }
    public void setInterPassingYear(String interPassingYear) { this.interPassingYear = interPassingYear; }

    public String getInterMarks() { return interMarks != null ? interMarks : ""; }
    public void setInterMarks(String interMarks) { this.interMarks = interMarks; }

    public String getInterMaxMarks() { return interMaxMarks != null ? interMaxMarks : ""; }
    public void setInterMaxMarks(String interMaxMarks) { this.interMaxMarks = interMaxMarks; }

    public String getInterPercentage() { return interPercentage != null ? interPercentage : ""; }
    public void setInterPercentage(String interPercentage) { this.interPercentage = interPercentage; }

    public String getScholarship() { return scholarship != null ? scholarship : ""; }
    public void setScholarship(String scholarship) { this.scholarship = scholarship; }
}
