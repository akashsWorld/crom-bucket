package com.crombucket.storageservice.auth;

public interface BucketAuthorizationBase {

    boolean isRequestAuthorized(String secret);

    String extractClientId(String secret);

    default String getApiKey(){
        return "a-long-api-key";
    }
}
