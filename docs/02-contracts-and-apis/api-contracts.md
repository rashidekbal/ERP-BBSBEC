# API Contracts & Network Interface Signatures

All external API interactions communicate with the **BBSBEC Institutional ERP System**.

---

## 1. Network Infrastructure & Common Headers

### Base Hosts
- **API Gateway (`MOBILE_API_HOST`)**: `https://api.bbsbec.edu.in/`
- **Web Portal (`WEB_LOGIN_URL`)**: `https://erp.bbsbec.edu.in/authentication/api/login`

### Default Request Headers
Every outgoing HTTP request (except the explicit login endpoint) is automatically injected with the following headers via `NetworkClient.authHeaderInterceptor`:

```http
Accept: application/json
Content-Type: application/json
X-College-Id: 1399
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

### Automatic 401 Re-Authentication & Token Recovery
When any endpoint returns `HTTP 401 Unauthorized`, the OkHttp `Authenticator` interceptor traps the failure:
1. Verifies that the failing request is not the login URL itself.
2. Checks that retry count is `< 2`.
3. Checks if `TokenManager.hasSavedCredentials()` is true.
4. Executes a thread-safe synchronous re-login (`NetworkClient.reloginSync()`).
5. Updates the stored Bearer token in `TokenManager`.
6. Retries the original HTTP request transparently with the renewed `Authorization: Bearer <token>` header.

---

## 2. Authentication Contracts

### 2.1 Web Login
- **Method**: `POST`
- **Full URL**: `https://erp.bbsbec.edu.in/authentication/api/login`
- **Retrofit Declaration**: `Call<WebLoginResponse> webLogin(@Url String url, @Body WebLoginRequest request);`
- **Request Body (`WebLoginRequest`)**:
  ```json
  {
    "email": "student@bbsbec.ac.in",
    "password": "plainTextPassword"
  }
  ```
- **Response Body (`WebLoginResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Login successful",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expires": "2026-11-03T23:59:59Z",
      "profile": {
        "id": 1042,
        "name": "Gurpreet Singh",
        "designation": "Student",
        "mobileno": "9876543210",
        "email": "student@bbsbec.ac.in",
        "photo": "https://erp.bbsbec.edu.in/media/photos/1042.jpg"
      }
    }
  }
  ```

### 2.2 Token Refresh
- **Method**: `POST`
- **Endpoint**: `mobile/token/refresh/`
- **Retrofit Declaration**: `Call<RefreshTokenResponse> refreshToken(@Body RefreshTokenRequest request);`
- **Request Body (`RefreshTokenRequest`)**:
  ```json
  {
    "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
- **Response Body (`RefreshTokenResponse`)**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```

---

## 3. Academic & ERP Data Contracts

### 3.1 Student Profile
- **Method**: `GET`
- **Endpoint**: `student/profile/`
- **Retrofit Declaration**: `Call<ProfileResponse> getStudentProfile();`
- **Response Body (`ProfileResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "id": 1042,
      "rollNo": "2104501",
      "universityRollNo": "1903456",
      "firstName": "Gurpreet",
      "middleName": "",
      "lastName": "Singh",
      "email": "gurpreet.2104501@bbsbec.ac.in",
      "mobileNo": "9876543210",
      "branch": "Computer Science and Engineering",
      "semester": 7,
      "course": "B.Tech",
      "classGroup": "CSE-A",
      "practicalGroup": "G1",
      "batch": "2021-2025",
      "admissionType": "Regular",
      "scholarship": "Post Matric",
      "photo": "https://erp.bbsbec.edu.in/media/photos/1042.jpg",
      "signature": "https://erp.bbsbec.edu.in/media/signatures/1042.jpg",
      "fatherName": "Harbhajan Singh",
      "motherName": "Kulwinder Kaur",
      "fatherProfession": "Business",
      "motherProfession": "Homemaker",
      "parentMobile": "9876543211",
      "dob": "2003-05-15",
      "aadhar": "123456789012",
      "bloodGroup": "B+",
      "category": "General",
      "status": "Active",
      "communicationAddress": "Fatehgarh Sahib, Punjab",
      "permanentAddress": "Fatehgarh Sahib, Punjab",
      "highSchoolBoard": "PSEB",
      "highSchoolPassingYear": "2019",
      "highSchoolMarks": "450",
      "highSchoolMaxMarks": "500",
      "highSchoolPercentage": "90.0",
      "interBoard": "PSEB",
      "interPassingYear": "2021",
      "interMarks": "430",
      "interMaxMarks": "500",
      "interPercentage": "86.0",
      "selectedSubjects": [101, 102, 103, 104]
    }
  }
  ```

### 3.2 Enrolled Subjects
- **Method**: `GET`
- **Endpoint**: `student/fetchsubjects/`
- **Retrofit Declaration**: `Call<EnrolledSubjectsResponse> getFetchSubjects();`
- **Response Body (`EnrolledSubjectsResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": [
      {
        "id": 101,
        "subject_code": "BTCS-701-18",
        "course_title": "Network Security and Cryptography",
        "credits": 4,
        "type": "Theory",
        "is_lab": false
      },
      {
        "id": 102,
        "subject_code": "BTCS-704-18",
        "course_title": "Network Security Lab",
        "credits": 1,
        "type": "Lab",
        "is_lab": true
      }
    ]
  }
  ```

### 3.3 Class Attendance per Subject
- **Method**: `GET`
- **Endpoint**: `student/class-attendance/`
- **Query Parameters**:
  - `subject` (Integer, required): Subject ID (e.g. `101`)
  - `type` (String, required): Class type (`theory`, `lab`, or `tutorial`)
- **Retrofit Declaration**:
  ```java
  @GET("student/class-attendance/")
  Call<AttendanceResponse> getClassAttendance(
      @Query("subject") Integer subjectId,
      @Query("type") String type
  );
  ```
- **Response Body (`AttendanceResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "overallPercentage": 82.5,
      "totalDelivered": 40,
      "totalAttended": 33,
      "subjects": [
        {
          "subject_id": 101,
          "subject_code": "BTCS-701-18",
          "subject_name": "Network Security and Cryptography",
          "teacher_name": "Er. Jaspreet Kaur",
          "total_lectures": 40,
          "attended_lectures": 33,
          "duty_leaves": 1,
          "medical_leaves": 0,
          "percentage": 82.5,
          "is_low_attendance": false,
          "records": [
            {
              "attendance_id": 90451,
              "lecture_session_id": 1205,
              "date": "2026-10-12",
              "hour": "Period 2 (09:50 - 10:40)",
              "mark": "P",
              "remarks": ""
            },
            {
              "attendance_id": 90452,
              "lecture_session_id": 1208,
              "date": "2026-10-14",
              "hour": "Period 2 (09:50 - 10:40)",
              "mark": "A",
              "remarks": "Absent without prior leave"
            }
          ]
        }
      ]
    }
  }
  ```

### 3.4 Timetable Schedule
- **Method**: `GET`
- **Endpoint**: `student/timetable/`
- **Retrofit Declaration**: `Call<TimetableResponse> getTimetable();`
- **Response Body (`TimetableResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "days": {
        "Monday": [
          {
            "period": 1,
            "start_time": "09:00",
            "end_time": "09:50",
            "subject_code": "BTCS-701-18",
            "subject_name": "Network Security",
            "teacher_name": "Er. Jaspreet Kaur",
            "room_no": "NB-204"
          },
          {
            "period": 2,
            "start_time": "09:50",
            "end_time": "10:40",
            "subject_code": "BTCS-702-18",
            "subject_name": "Cloud Computing",
            "teacher_name": "Dr. Baljit Singh",
            "room_no": "NB-204"
          }
        ]
      }
    }
  }
  ```

### 3.5 Internal Mid-Sem Assessments
- **Method**: `GET`
- **Endpoint**: `student/assessments/`
- **Retrofit Declaration**: `Call<AssessmentsResponse> getAssessments();`
- **Response Body (`AssessmentsResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": [
      {
        "subject_code": "BTCS-701-18",
        "subject_name": "Network Security and Cryptography",
        "mst1_marks": 21.5,
        "mst1_max": 24.0,
        "mst2_marks": 19.0,
        "mst2_max": 24.0,
        "mst3_marks": null,
        "mst3_max": 24.0,
        "assignment_marks": 10.0,
        "assignment_max": 10.0,
        "attendance_marks": 5.0,
        "attendance_max": 5.0,
        "total_marks": 55.5,
        "total_max": 63.0
      }
    ]
  }
  ```

### 3.6 College Session
- **Method**: `GET`
- **Endpoint**: `student/collegesession/`
- **Retrofit Declaration**: `Call<CollegeSessionResponse> getCollegeSession();`
- **Response Body (`CollegeSessionResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "session_id": 48,
      "name": "July - Dec 2026",
      "start_date": "2026-07-15",
      "end_date": "2026-11-30",
      "is_current": true
    }
  }
  ```

---

## 4. Notifications & Campus Announcements

### 4.1 Announcements Inbox
- **Method**: `GET`
- **Endpoint**: `notifications/inbox/`
- **Retrofit Declaration**: `Call<NotificationsInboxResponse> getNotificationsInbox();`
- **Response Body (`NotificationsInboxResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "count": 2,
      "next": null,
      "previous": null,
      "results": [
        {
          "id": 804,
          "title": "MST-2 Examination Schedule Published",
          "message": "Mid-Semester Examination 2 will commence from 18th November 2026. Check the department notice board.",
          "category": "Academic",
          "is_read": false,
          "created_at": "2026-11-02T10:30:00Z"
        }
      ]
    }
  }
  ```

### 4.2 Unread Announcements Count
- **Method**: `GET`
- **Endpoint**: `notifications/unread-count/`
- **Retrofit Declaration**: `Call<NotificationUnreadCountResponse> getNotificationUnreadCount();`
- **Response Body (`NotificationUnreadCountResponse`)**:
  ```json
  {
    "status": 200,
    "unread_count": 3
  }
  ```

### 4.3 Mark Single Notification Read
- **Method**: `POST`
- **Endpoint**: `notifications/read/`
- **Retrofit Declaration**: `Call<NotificationActionResponse> markNotificationRead(@Body MarkNotificationReadRequest request);`
- **Request Body (`MarkNotificationReadRequest`)**:
  ```json
  {
    "id": 804
  }
  ```
- **Response Body (`NotificationActionResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Notification marked as read",
    "success": true
  }
  ```

### 4.4 Mark All Notifications Read
- **Method**: `POST`
- **Endpoint**: `notifications/read-all/`
- **Retrofit Declaration**: `Call<NotificationActionResponse> markAllNotificationsRead(@Body Map<String, Object> emptyBody);`
- **Request Body**: `{}`
- **Response Body (`NotificationActionResponse`)**:
  ```json
  {
    "status": 200,
    "message": "All notifications marked as read",
    "success": true
  }
  ```

---

## 5. Webmail, Fees & Course Registration

### 5.1 Campus Webmail Inbox
- **Method**: `GET`
- **Endpoint**: `webmail/mail/inbox/`
- **Query Parameters**:
  - `position` (int, default `0`): Offset page index.
  - `limit` (int, default `20`): Maximum messages per request.
- **Retrofit Declaration**:
  ```java
  @GET("webmail/mail/inbox/")
  Call<WebmailInboxResponse> getWebmailInbox(@Query("position") int position, @Query("limit") int limit);
  ```
- **Response Body (`WebmailInboxResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "email_address": "student@bbsbec.ac.in",
      "total": 45,
      "position": 0,
      "limit": 20,
      "results": [
        {
          "id": "msg_908234",
          "thread_id": "thread_123",
          "from_name": "T&P Cell BBSBEC",
          "from_email": "placements@bbsbec.ac.in",
          "subject": "Campus Placement Drive — Registration Open",
          "preview": "Eligible 7th semester students must submit resumes...",
          "received_at": "2026-11-01 14:15:00",
          "is_read": false,
          "has_attachments": true,
          "body": "Detailed placement circular contents...",
          "html": "<p>Detailed placement circular contents...</p>"
        }
      ]
    }
  }
  ```

### 5.2 Fees History & Installments
- **Method**: `GET`
- **Endpoint**: `student/fees-history/`
- **Retrofit Declaration**: `Call<FeesHistoryResponse> getFeesHistory();`
- **Response Body (`FeesHistoryResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": {
      "total_fee": 48500.0,
      "amount_paid": 48500.0,
      "balance": 0.0,
      "installments": [
        {
          "installment_no": 1,
          "head_name": "Semester Tuition & Exam Fee",
          "amount": 48500.0,
          "due_date": "2026-08-15",
          "status": "PAID",
          "receipt_no": "REC-2026-7841"
        }
      ]
    }
  }
  ```

### 5.3 Semester Registration
- **Method**: `GET`
- **Endpoint**: `student/studentsemesterregistration/`
- **Retrofit Declaration**: `Call<SemesterRegistrationResponse> getSemesterRegistrations();`
- **Response Body (`SemesterRegistrationResponse`)**:
  ```json
  {
    "status": 200,
    "message": "Success",
    "data": [
      {
        "id": 1420,
        "session": { "id": 48, "name": "July - Dec 2026", "session": "2026-Odd" },
        "semester": 7,
        "status": "APPROVED",
        "applied_date": "2026-07-20",
        "remarks": "Verified by HOD CSE",
        "selected_subjects": [
          { "id": 101, "subject_code": "BTCS-701-18", "subject_name": "Network Security", "credits": 4, "type": "Theory" }
        ]
      }
    ]
  }
  ```
