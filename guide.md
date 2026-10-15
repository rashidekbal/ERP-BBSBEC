# BBSBEC Student Portal API – Master Reference & Integration Guide

Comprehensive, end-to-end API documentation for the Baba Banda Singh Bahadur Engineering College (BBSBEC) backend. This guide details **every endpoint**, all **required request fields** (`snake_case`), **full JSON response schemas**, headers, Postman integration, and the **Web Login workaround** that eliminates device approval lockouts.

---

## 1. Global Configuration & Architecture

| Parameter | Value | Description |
| :--- | :--- | :--- |
| **Web ERP Host** | `https://erp.bbsbec.edu.in` | Web Portal host (No Device ID checks) |
| **Mobile API Host** | `https://api.bbsbec.edu.in` | Mobile App backend host |
| **Dev Fallback** | `http://10.3.10.99:8000` | Campus Internal Host |
| **Realtime Push** | `mqtts://api.bbsbec.edu.in:8883` | Secure MQTT Broker |
| **App Version** | `1.0.4` | App version expected by mobile endpoints |
| **College ID** | `1399` | BBSBEC Multi-Tenant ID |

### Global Headers
For authenticated requests, include:
```http
Accept: application/json
Content-Type: application/json
Authorization: Bearer {{accessToken}}
X-College-Id: 1399
```

> [!IMPORTANT]
> **Trailing Slashes**: Mobile endpoints on `https://api.bbsbec.edu.in` require a trailing slash `/` (e.g., `/student/profile/`). The web login endpoint is `https://erp.bbsbec.edu.in/authentication/api/login`.

---

## 2. Recommended Authentication Flow (Zero Device Lockout)

Because the mobile endpoint (`/mobile/login/`) enforces device whitelisting (`403 DEVICE_PENDING_APPROVAL`), the **cleanest and fastest method** is to authenticate via the **Web ERP Login API**:

### The Web Login Endpoint
- **URL**: `POST https://erp.bbsbec.edu.in/authentication/api/login`
- **Headers**:
  ```http
  Content-Type: application/json
  Accept: application/json
  ```
- **Request Body**:
  ```json
  {
    "email": "rasidekbal29@gmail.com",
    "password": "your_password"
  }
  ```

### Live Response Schema
```json
{
  "status": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires": "2026-10-15T12:12:42",
    "login": {
      "id": 1066,
      "email": "rasidekbal29@gmail.com",
      "is_staff": false,
      "is_active": true,
      "is_superuser": false
    },
    "profile": {
      "id": 216,
      "name": "Rasid Ekbal",
      "designation": "Student",
      "mobileno": 9123481953,
      "whatsappno": null,
      "email": "rasidekbal29@gmail.com",
      "interests": null,
      "createdAt": "2026-06-29T00:13:51.033624+05:30",
      "updatedAt": "2026-06-29T00:13:51.033636+05:30",
      "photo": null,
      "profile_type": "student",
      "user": 1066,
      "department": 1,
      "working_departments": [1]
    }
  },
  "client": "152.59.83.23",
  "signature": "a309e42034197d9798196efbd6a5f04ef492cfb55c3e32d9ed974baa8a4ea323"
}
```

- **Token Mapping**:
  - `data.token`: Bearer access token for all requests.
  - `data.refresh`: Token to refresh sessions.

---

## 3. Postman Quick Start & Automation

1. Import 📦 **[`bbsbec_api_postman_collection.json`](file:///c:/Users/rtech/Downloads/EazyShare/Android%20Phone%20ep6f/index.android.bundle_Decompiler.com/bbsbec_api_postman_collection.json)**.
2. In `1. Mobile & Session`, open **`Web Login (Bypasses Device ID - Recommended)`**.
3. In the **Body** tab, enter your email and password, then click **Send**.
4. The built-in **Postman Tests script** will automatically save your tokens:
   ```javascript
   var jsonData = pm.response.json();
   if (jsonData && jsonData.data) {
       var token = jsonData.data.token || jsonData.data.access;
       var refToken = jsonData.data.refresh;

       if (token) {
           pm.collectionVariables.set("accessToken", token);
           pm.environment.set("accessToken", token);
           console.log("[+] Access Token set: " + token.substring(0, 20) + "...");
       }
       if (refToken) {
           pm.collectionVariables.set("refreshToken", refToken);
           pm.environment.set("refreshToken", refToken);
       }
   }
   ```
5. All requests in **`Student Portal`** and **`Notifications`** inherit `Bearer {{accessToken}}` automatically.

---

## 4. Complete Endpoint Reference: Request Fields & Response Schemas

---

### Category 1: Mobile & Session Management (`/mobile/`)

#### 1.0 Web Portal Login (Recommended)
- **Route**: `POST https://erp.bbsbec.edu.in/authentication/api/login`
- **Auth Required**: No
- **Headers**: `Content-Type: application/json`, `Accept: application/json`
- **Request Body**:
  ```json
  {
    "email": "student@bbsbec.ac.in",
    "password": "your_password"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "token": "eyJhbGciOiJIUzI1Ni...",
      "refresh": "eyJhbGciOiJIUzI1Ni...",
      "expires": "2026-10-15T12:12:42",
      "login": { "id": 1066, "email": "student@bbsbec.ac.in", "is_active": true },
      "profile": { "id": 216, "name": "Rasid Ekbal", "mobileno": 9123481953, "profile_type": "student" }
    }
  }
  ```

---

#### 1.1 Server Clock Synchronization
- **Route**: `GET https://api.bbsbec.edu.in/mobile/time/`
- **Auth Required**: No
- **Headers**: `Accept: application/json`
- **Request Body**: None
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "server_time": 1792003362,
      "timezone": "Asia/Kolkata"
    }
  }
  ```

---

#### 1.2 College Profile
- **Route**: `GET https://api.bbsbec.edu.in/mobile/college/{college_id}/`
- **Auth Required**: No
- **Path Parameters**:
  - `college_id` *(**Required**, default: `1399`)*
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "id": 1399,
      "name": "Baba Banda Singh Bahadur Engineering College",
      "code": "BBSBEC",
      "logo": "https://api.bbsbec.edu.in/media/colleges/1399/logo.png",
      "support_email": "support@bbsbec.ac.in",
      "features": {
        "attendance": true,
        "fees": true,
        "timetable": true,
        "assessments": true,
        "feedback": true
      }
    }
  }
  ```

---

#### 1.3 Mobile Login (Requires Pre-Approved Device)
- **Route**: `POST https://api.bbsbec.edu.in/mobile/login/`
- **Auth Required**: No
- **Headers**: `Content-Type: application/json`, `Accept: application/json`, `X-College-Id: 1399`
- **Request Body (`snake_case`)**:
  ```json
  {
    "email": "student@bbsbec.ac.in",
    "password": "your_password",
    "device_id": "d4b83f12-78a4-49c2-9e81-a6b4129e01ff",
    "device_name": "Postman-Client",
    "platform": "android",
    "app_version": "1.0.4"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "access": "eyJhbGciOiJIUzI1Ni...",
      "refresh": "eyJhbGciOiJIUzI1Ni...",
      "device_secret": "e2a1b945...",
      "user": { "id": 1066, "name": "Rasid Ekbal", "role": "student" },
      "permissions": ["view_student_attendance", "view_student_timetable"]
    }
  }
  ```

---

#### 1.4 Refresh Access Token
- **Route**: `POST https://api.bbsbec.edu.in/mobile/token/refresh/`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "refresh": "{{refreshToken}}"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "access": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expires": "2026-10-15T18:12:42"
    }
  }
  ```

---

#### 1.5 Fetch User Permissions
- **Route**: `GET https://api.bbsbec.edu.in/mobile/permissions/`
- **Auth Required**: Yes (`Bearer {{accessToken}}`)
- **Headers**: `X-College-Id: 1399`
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": [
      "view_student_profile",
      "view_student_attendance",
      "view_student_timetable",
      "view_student_fees_history",
      "view_student_assignment_marks",
      "view_student_mst_marks",
      "add_student_payment"
    ]
  }
  ```

---

#### 1.6 Register Device Info
- **Route**: `POST https://api.bbsbec.edu.in/mobile/device/`
- **Auth Required**: Yes
- **Request Body**:
  ```json
  {
    "device_id": "{{deviceId}}",
    "device_name": "Postman-Client",
    "platform": "android",
    "app_version": "1.0.4"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Device registered successfully."
  }
  ```

---

#### 1.7 User Logout
- **Route**: `POST https://api.bbsbec.edu.in/mobile/logout/`
- **Auth Required**: Yes (`Bearer {{accessToken}}`)
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Successfully logged out."
  }
  ```

---

### Category 2: Authentication & Password Recovery (`/authentication/`)

#### 2.1 Fetch ERP2 Profile
- **Route**: `GET https://api.bbsbec.edu.in/authentication/erp2profile/`
- **Auth Required**: Yes (`Bearer {{accessToken}}`)
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "erp_user_id": 1066,
      "erp_student_id": 216,
      "is_profile_complete": true,
      "last_synced": "2026-10-14T20:10:00Z"
    }
  }
  ```

---

#### 2.2 Request Password Reset OTP
- **Route**: `POST https://api.bbsbec.edu.in/authentication/forgetpasswordotp/`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "email": "student@bbsbec.ac.in"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "OTP has been sent to your registered mobile and email."
  }
  ```

---

#### 2.3 Reset Password with OTP
- **Route**: `POST https://api.bbsbec.edu.in/authentication/forgetpasswordchange/`
- **Auth Required**: No
- **Request Body**:
  ```json
  {
    "email": "student@bbsbec.ac.in",
    "otp": "123456",
    "password": "NewSecurePassword#123"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Password updated successfully. Please log in with your new password."
  }
  ```

---

### Category 3: Student Academic Portal (`/student/`)

#### 3.1 Student Profile
- **Fetch Profile**: `GET https://api.bbsbec.edu.in/student/profile/`
- **Update Profile**: `PATCH https://api.bbsbec.edu.in/student/profile/`
- **Auth Required**: Yes
- **PATCH Request Body**:
  ```json
  {
    "mobileno": "9123481953",
    "address": "College Campus Hostel, BBSBEC, Fatehgarh Sahib",
    "emergency_contact": "9876543210"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "id": 216,
      "user_id": 1066,
      "roll_no": "2104123",
      "reg_no": "19054321",
      "first_name": "Rasid",
      "last_name": "Ekbal",
      "email": "rasidekbal29@gmail.com",
      "mobileno": "9123481953",
      "father_name": "Md. Ekbal",
      "mother_name": "R. Khatun",
      "gender": "Male",
      "dob": "2003-05-15",
      "blood_group": "B+",
      "address": "College Campus Hostel, BBSBEC, Fatehgarh Sahib",
      "branch": "Computer Science & Engineering",
      "branch_id": 1,
      "semester": 7,
      "section": "A",
      "batch": "2021-2025",
      "photo": "https://api.bbsbec.edu.in/media/photos/216.jpg",
      "signature": "https://api.bbsbec.edu.in/media/signatures/216.jpg"
    }
  }
  ```

---

#### 3.2 Update Profile Images (Photo & Signature)
- **Route**: `POST https://api.bbsbec.edu.in/student/profileimage/`
- **Auth Required**: Yes
- **Content-Type**: `multipart/form-data`
- **Form Fields**: `photo` (File binary), `signature` (File binary)
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Profile images uploaded successfully.",
    "data": {
      "photo_url": "https://api.bbsbec.edu.in/media/photos/216.jpg",
      "signature_url": "https://api.bbsbec.edu.in/media/signatures/216.jpg"
    }
  }
  ```

---

#### 3.3 Class Attendance
- **Route**: `GET https://api.bbsbec.edu.in/student/class-attendance/`
- **Auth Required**: Yes
- **Query Parameters**: `session_id` *(Optional)*
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "overall_percentage": 78.5,
      "total_delivered": 120,
      "total_attended": 94,
      "subjects": [
        {
          "subject_id": 101,
          "subject_code": "BTCS-501",
          "subject_name": "Computer Networks",
          "teacher_name": "Er. Hardeep Singh",
          "total_lectures": 34,
          "attended_lectures": 28,
          "duty_leaves": 2,
          "medical_leaves": 0,
          "percentage": 82.35,
          "is_low_attendance": false,
          "records": [
            {
              "attendance_id": 5012,
              "date": "2026-10-12",
              "hour": 2,
              "mark": "P",
              "remarks": "Present"
            },
            {
              "attendance_id": 5013,
              "date": "2026-10-13",
              "hour": 3,
              "mark": "AB",
              "remarks": "Absent"
            }
          ]
        }
      ]
    }
  }
  ```

---

#### 3.4 Weekly Timetable
- **Route**: `GET https://api.bbsbec.edu.in/student/timetable/`
- **Auth Required**: Yes
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "days": {
        "Monday": [
          {
            "period": 1,
            "start_time": "09:00:00",
            "end_time": "09:50:00",
            "subject_code": "BTCS-701",
            "subject_name": "Cloud Computing",
            "teacher_name": "Dr. Jatinder Singh",
            "room_no": "LH-102",
            "type": "Theory"
          },
          {
            "period": 2,
            "start_time": "09:50:00",
            "end_time": "10:40:00",
            "subject_code": "BTCS-702",
            "subject_name": "Machine Learning",
            "teacher_name": "Er. Balpreet Kaur",
            "room_no": "LH-102",
            "type": "Theory"
          }
        ],
        "Tuesday": [ ... ],
        "Wednesday": [ ... ]
      }
    }
  }
  ```

---

#### 3.5 Internal Assessments & Marks
- **Route**: `GET https://api.bbsbec.edu.in/student/assessments/`
- **Auth Required**: Yes
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": [
      {
        "subject_code": "BTCS-701",
        "subject_name": "Cloud Computing",
        "mst1_marks": 21.5,
        "mst1_max": 24.0,
        "mst2_marks": 20.0,
        "mst2_max": 24.0,
        "assignment_marks": 10.0,
        "assignment_max": 10.0,
        "attendance_marks": 5.0,
        "attendance_max": 5.0,
        "total_internal": 38.5,
        "max_internal": 40.0
      }
    ]
  }
  ```

---

#### 3.6 College Session & Subjects

##### A. Active College Session (`GET https://api.bbsbec.edu.in/student/collegesession/`)
```json
{
  "status": 200,
  "data": {
    "session_id": 12,
    "name": "July - Dec 2026",
    "start_date": "2026-07-15",
    "end_date": "2026-12-20",
    "is_current": true
  }
}
```

##### B. Enrolled Subjects (`GET https://api.bbsbec.edu.in/student/fetchsubjects/`)
```json
{
  "status": 200,
  "data": [
    {
      "id": 101,
      "subject_code": "BTCS-701",
      "subject_name": "Cloud Computing",
      "credits": 4,
      "type": "Theory"
    },
    {
      "id": 102,
      "subject_code": "BTCS-702",
      "subject_name": "Machine Learning",
      "credits": 4,
      "type": "Theory"
    }
  ]
}
```

##### C. Registration Eligible Subjects (`GET https://api.bbsbec.edu.in/student/fetchstudentsubjects/`)
```json
{
  "status": 200,
  "data": [
    {
      "id": 101,
      "subject_code": "BTCS-701",
      "subject_name": "Cloud Computing",
      "credits": 4,
      "is_compulsory": true
    },
    {
      "id": 102,
      "subject_code": "BTCS-702",
      "subject_name": "Machine Learning",
      "credits": 4,
      "is_compulsory": false
    }
  ]
}
```

---

#### 3.7 Semester Registration

##### A. List Registrations (`GET https://api.bbsbec.edu.in/student/studentsemesterregistration/`)
```json
{
  "status": 200,
  "data": [
    {
      "id": 45,
      "session_id": 12,
      "session_name": "July-Dec 2026",
      "semester": 7,
      "status": "APPROVED",
      "submitted_at": "2026-07-20T10:30:00Z",
      "subjects": [
        { "id": 101, "code": "BTCS-701", "name": "Cloud Computing", "credits": 4 },
        { "id": 102, "code": "BTCS-702", "name": "Machine Learning", "credits": 4 }
      ]
    }
  ]
}
```

##### B. Submit Semester Registration (`POST https://api.bbsbec.edu.in/student/studentsemesterregistration/`)
- **Request Body**:
  ```json
  {
    "session_id": 12,
    "semester": 7,
    "subjects": [101, 102]
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 201,
    "message": "Semester registration submitted successfully.",
    "data": { "id": 45, "status": "PENDING_APPROVAL" }
  }
  ```

---

#### 3.8 Fees History & Ledger
- **Route**: `GET https://api.bbsbec.edu.in/student/fees-history/`
- **Auth Required**: Yes
- **Response Schema**:
  ```json
  {
    "status": 200,
    "data": {
      "total_fee": 85000.0,
      "amount_paid": 50000.0,
      "balance": 35000.0,
      "installments": [
        {
          "installment_no": 1,
          "head_name": "Tuition Fee",
          "amount": 45000.0,
          "due_date": "2026-08-15",
          "status": "PAID",
          "receipt_no": "REC-2026-1042"
        },
        {
          "installment_no": 2,
          "head_name": "Exam & Development Fee",
          "amount": 40000.0,
          "due_date": "2026-11-30",
          "status": "PENDING",
          "receipt_no": null
        }
      ]
    }
  }
  ```

---

#### 3.9 Payment Records & Fee Masters

##### A. Payment Masters (`GET https://api.bbsbec.edu.in/student/payments/master/`)
```json
{
  "status": 200,
  "data": [
    {
      "id": 14,
      "name": "Semester Fee 2026-27",
      "category": "Academic",
      "min_amount": 1000.0,
      "max_amount": 90000.0,
      "is_active": true
    }
  ]
}
```

##### B. Submit Payment (`POST https://api.bbsbec.edu.in/student/payments/`)
- **Request Body**:
  ```json
  {
    "fee_master_id": 14,
    "amount": 45000.00,
    "payment_mode": "ONLINE",
    "transaction_ref": "TXN9876543210",
    "payer_mobile": "9123481953",
    "payer_alt_mobile": "9876543210",
    "remarks": "Tuition Fee 7th Semester"
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 201,
    "data": {
      "id": 341,
      "amount": 45000.00,
      "status": "PENDING_VERIFICATION",
      "reference_no": "TXN9876543210",
      "payment_date": "2026-10-15"
    }
  }
  ```

##### C. List Payments (`GET https://api.bbsbec.edu.in/student/payments/`)
```json
{
  "status": 200,
  "data": [
    {
      "id": 341,
      "amount": 45000.00,
      "payment_mode": "ONLINE",
      "payment_date": "2026-08-14",
      "reference_no": "TXN9876543210",
      "reference_type": "UPI",
      "upi_app": "GooglePay",
      "payer_name": "Rasid Ekbal",
      "payer_mobile": "9123481953",
      "status": "VERIFIED",
      "receipt_file": "https://api.bbsbec.edu.in/media/receipts/rec341.pdf"
    }
  ]
}
```

---

#### 3.10 Faculty Feedback & Course Outcomes

##### A. Pending Feedback Surveys (`GET https://api.bbsbec.edu.in/student/feedbackmanager/`)
```json
{
  "status": 200,
  "data": [
    {
      "feedback_id": 204,
      "subject_name": "Cloud Computing",
      "teacher_name": "Dr. Jatinder Singh",
      "is_submitted": false
    }
  ]
}
```

##### B. Submit Feedback (`POST https://api.bbsbec.edu.in/student/feedbackmanager/`)
- **Request Body**:
  ```json
  {
    "feedback_id": 204,
    "ratings": [
      { "question_id": 1, "score": 5 },
      { "question_id": 2, "score": 4 }
    ],
    "comments": "Great teaching methodology."
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Feedback submitted successfully."
  }
  ```

##### C. Course Outcomes (`GET https://api.bbsbec.edu.in/student/courseoutcome/`)
```json
{
  "status": 200,
  "data": [
    {
      "subject_code": "BTCS-701",
      "co_code": "CO-1",
      "description": "Understand cloud architecture and deployment models.",
      "attainment_percentage": 85.0
    }
  ]
}
```

---

### Category 4: Notifications (`/notifications/`)

#### 4.1 Fetch Inbox (`GET https://api.bbsbec.edu.in/notifications/inbox/`)
```json
{
  "status": 200,
  "data": {
    "count": 14,
    "next": null,
    "previous": null,
    "results": [
      {
        "id": 128,
        "title": "MST-1 Schedule Announced",
        "message": "Mid-semester test schedule for 7th semester has been uploaded.",
        "category": "Academic",
        "is_read": false,
        "created_at": "2026-10-10T14:20:00+05:30"
      }
    ]
  }
}
```

#### 4.2 Unread Count (`GET https://api.bbsbec.edu.in/notifications/unread-count/`)
```json
{
  "status": 200,
  "data": {
    "count": 3
  }
}
```

#### 4.3 Mark as Read (`POST https://api.bbsbec.edu.in/notifications/read/`)
- **Request Body**:
  ```json
  {
    "id": 128
  }
  ```
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "Notification marked as read."
  }
  ```

#### 4.4 Mark All Read (`POST https://api.bbsbec.edu.in/notifications/read-all/`)
- **Request Body**: Empty `{}`
- **Response Schema**:
  ```json
  {
    "status": 200,
    "message": "All notifications marked as read."
  }
  ```
