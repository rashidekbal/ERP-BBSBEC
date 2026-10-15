package com.redcodersgroup.attend_collegeattendancetracker.network;

import com.redcodersgroup.attend_collegeattendancetracker.network.model.AssessmentsResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.AttendanceResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.ProfileResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.RefreshTokenRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.RefreshTokenResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.TimetableResponse;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

public interface BbsbecApiService {

    @POST
    Call<WebLoginResponse> webLogin(@Url String url, @Body WebLoginRequest request);

    @POST("mobile/token/refresh/")
    Call<RefreshTokenResponse> refreshToken(@Body RefreshTokenRequest request);

    @GET("student/profile/")
    Call<ProfileResponse> getStudentProfile();

    @GET("student/class-attendance/")
    Call<AttendanceResponse> getClassAttendance(
            @retrofit2.http.Query("subject") Integer subjectId,
            @retrofit2.http.Query("type") String type
    );

    @GET("student/timetable/")
    Call<TimetableResponse> getTimetable();

    @GET("student/assessments/")
    Call<AssessmentsResponse> getAssessments();

    @GET("student/collegesession/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.CollegeSessionResponse> getCollegeSession();

    @GET("student/fetchsubjects/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.EnrolledSubjectsResponse> getFetchSubjects();

    @GET("student/studentsemesterregistration/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.SemesterRegistrationResponse> getSemesterRegistrations();

    @GET("student/fetchstudentsubjects/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.EligibleSubjectsResponse> getEligibleSubjects();

    @GET("student/fees-history/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.FeesHistoryResponse> getFeesHistory();

    @GET("student/payments/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.PaymentsResponse> getPayments();

    @GET("webmail/mail/inbox/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.WebmailInboxResponse> getWebmailInbox(
            @retrofit2.http.Query("position") int position,
            @retrofit2.http.Query("limit") int limit
    );

    @GET("notifications/inbox/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationsInboxResponse> getNotificationsInbox();

    @GET("notifications/unread-count/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationUnreadCountResponse> getNotificationUnreadCount();

    @POST("notifications/read/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationActionResponse> markNotificationRead(
            @Body com.redcodersgroup.attend_collegeattendancetracker.network.model.MarkNotificationReadRequest request
    );

    @POST("notifications/read-all/")
    Call<com.redcodersgroup.attend_collegeattendancetracker.network.model.NotificationActionResponse> markAllNotificationsRead(
            @Body java.util.Map<String, Object> emptyBody
    );
}
