package com.redcodersgroup.attend_collegeattendancetracker.network;

import androidx.annotation.NonNull;

import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginRequest;
import com.redcodersgroup.attend_collegeattendancetracker.network.model.WebLoginResponse;

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
    public static final String MOBILE_API_HOST = "https://api.bbsbec.edu.in/";
    public static final String WEB_LOGIN_URL = "https://erp.bbsbec.edu.in/authentication/api/login";
    public static final String COLLEGE_ID = "1399";

    private static BbsbecApiService apiService;
    private static BbsbecApiService authService;

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
                    // Do not attempt re-login if the failed request was login itself
                    if (response.request().url().toString().contains("authentication/api/login")) {
                        return null;
                    }

                    if (responseCount(response) >= 2) {
                        return null; // Don't retry more than once
                    }

                    if (!TokenManager.hasSavedCredentials()) {
                        return null;
                    }

                    synchronized (NetworkClient.class) {
                        String currentToken = TokenManager.getAccessToken();
                        String requestAuth = response.request().header("Authorization");
                        if (currentToken != null && !currentToken.trim().isEmpty()) {
                            String currentBearer = "Bearer " + currentToken;
                            if (!currentBearer.equals(requestAuth)) {
                                // Token was already updated by another concurrent thread/request
                                return response.request().newBuilder()
                                        .header("Authorization", currentBearer)
                                        .build();
                            }
                        }

                        // Automated re-login: extracts and updates token only (no data sync)
                        boolean success = reloginSync();
                        if (success) {
                            String updatedToken = TokenManager.getAccessToken();
                            if (updatedToken != null && !updatedToken.trim().isEmpty()) {
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + updatedToken)
                                        .build();
                            }
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

    public static synchronized BbsbecApiService getAuthService() {
        if (authService == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            Interceptor headersInterceptor = chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json")
                        .header("X-College-Id", COLLEGE_ID)
                        .build();
                return chain.proceed(request);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(headersInterceptor)
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(MOBILE_API_HOST)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            authService = retrofit.create(BbsbecApiService.class);
        }
        return authService;
    }

    public static synchronized BbsbecApiService getRefreshService() {
        return getAuthService();
    }

    /**
     * Executes a synchronous re-login using saved credentials to refresh the access token.
     * Only updates the access and refresh tokens; does NOT trigger data sync.
     *
     * @return true if login succeeded and token was updated, false otherwise.
     */
    public static synchronized boolean reloginSync() {
        String savedUsername = TokenManager.getSavedUsername();
        String savedPassword = TokenManager.getSavedPassword();
        if (savedUsername == null || savedUsername.trim().isEmpty() ||
                savedPassword == null || savedPassword.trim().isEmpty()) {
            return false;
        }

        try {
            WebLoginRequest loginReq = new WebLoginRequest(savedUsername, savedPassword);
            Call<WebLoginResponse> loginCall = getAuthService().webLogin(WEB_LOGIN_URL, loginReq);
            retrofit2.Response<WebLoginResponse> loginResp = loginCall.execute();

            if (loginResp.isSuccessful() && loginResp.body() != null && loginResp.body().getData() != null) {
                WebLoginResponse.Data data = loginResp.body().getData();
                String newToken = data.getToken();
                String newRefresh = data.getRefresh();

                if (newToken != null && !newToken.trim().isEmpty()) {
                    TokenManager.updateAccessToken(newToken);
                    if (newRefresh != null && !newRefresh.trim().isEmpty()) {
                        TokenManager.updateRefreshToken(newRefresh);
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            // Re-login failed (e.g. network timeout or server unreachable)
        }
        return false;
    }
}
