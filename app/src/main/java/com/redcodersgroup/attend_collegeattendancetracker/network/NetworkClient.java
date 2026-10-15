package com.redcodersgroup.attend_collegeattendancetracker.network;

import androidx.annotation.NonNull;

import com.redcodersgroup.attend_collegeattendancetracker.network.model.RefreshTokenRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.RefreshTokenResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {
    public static final String WEB_ERP_HOST = "https://erp.bbsbec.edu.in/";
    public static final String MOBILE_API_HOST = "https://api.bbsbec.edu.in/";
    public static final String WEB_LOGIN_URL = "https://erp.bbsbec.edu.in/authentication/api/login";
    public static final String COLLEGE_ID = "1399";

    private static BbsbecApiService apiService;
    private static BbsbecApiService refreshService;

    public static BbsbecApiService getClient() {
        return getApiService();
    }

    public static synchronized BbsbecApiService getApiService() {
        if (apiService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor authHeaderInterceptor = chain -> {
                Request original = chain.request();
                Request.Builder builder = original.newBuilder()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("X-College-Id", COLLEGE_ID);

                String token = TokenManager.getAccessToken();
                if (token != null && !token.trim().isEmpty() && !original.url().toString().contains("authentication/api/login")) {
                    builder.header("Authorization", "Bearer " + token);
                }

                return chain.proceed(builder.build());
            };

            Authenticator tokenAuthenticator = new Authenticator() {
                @Override
                public Request authenticate(Route route, @NonNull Response response) throws IOException {
                    if (responseCount(response) >= 2) {
                        return null; // Don't retry more than once
                    }

                    String refreshToken = TokenManager.getRefreshToken();
                    if (refreshToken == null || refreshToken.trim().isEmpty()) {
                        return null;
                    }

                    Call<RefreshTokenResponse> refreshCall = getRefreshService().refreshToken(new RefreshTokenRequest(refreshToken));
                    retrofit2.Response<RefreshTokenResponse> refreshResp = refreshCall.execute();

                    if (refreshResp.isSuccessful() && refreshResp.body() != null && refreshResp.body().getData() != null) {
                        String newAccessToken = refreshResp.body().getData().getAccessToken();
                        if (newAccessToken != null && !newAccessToken.trim().isEmpty()) {
                            TokenManager.updateAccessToken(newAccessToken);
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + newAccessToken)
                                    .build();
                        }
                    }
                    return null;
                }

                private int responseCount(Response response) {
                    int count = 1;
                    while ((response = response.priorResponse()) != null) {
                        count++;
                    }
                    return count;
                }
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(authHeaderInterceptor)
                    .addInterceptor(logging)
                    .authenticator(tokenAuthenticator)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MOBILE_API_HOST)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(BbsbecApiService.class);
        }
        return apiService;
    }

    public static synchronized BbsbecApiService getRefreshService() {
        if (refreshService == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MOBILE_API_HOST)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            refreshService = retrofit.create(BbsbecApiService.class);
        }
        return refreshService;
    }
}
