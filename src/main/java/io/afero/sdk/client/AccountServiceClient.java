/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.sdk.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.afero.sdk.client.afero.models.AccessToken;
import okhttp3.ResponseBody;
import rx.Observable;

public interface AccountServiceClient {

    class AccountVerificationPending extends Throwable {}
    class InvalidPhoneNumber extends Throwable {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Profile {
        public String email;

        public String firstName;
        public String lastName;

        public String phone;

        public String zipcode;

        public String status;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CreateAccountParams {
        public String description;

        public String firstName;
        public String lastName;

        public String email;
        public String password;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CreateAccountResult {
        public String email;
        public String password;
    }

    class LoginResult {
        public AccessToken token;
        public Profile profile;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class ResponseError {
        public int statusCode;
        public String errorCode;
        public String errorMessage;
    }

    enum ErrorType {
        ACCOUNT_ALREADY_EXISTS,
        PASSWORD_NOT_SECURE
    }


    Observable<CreateAccountResult> createAccount(CreateAccountParams body);

    Observable<Void> resetPassword(String email);

    Observable<LoginResult> login(String email, String password);

    Observable<Void> resendVerificationEmail(String email);

    ResponseError getResponseError(Throwable e);

    boolean isError(ResponseError err, ErrorType errorType);

    Observable<Profile> getProfile();

    Observable<Void> updateProfile(Profile profile);

    boolean isUserInfoRequired();

    boolean isUserInfoComplete(Profile userProfile);

}
