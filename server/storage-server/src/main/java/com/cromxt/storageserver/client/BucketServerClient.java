package com.cromxt.storageserver.client;

import com.cromxt.common.crombucket.bucketmanager.UsersBucketInfo;
import reactor.core.publisher.Mono;

public interface BucketServerClient {

    Mono<UsersBucketInfo> getBucketInfoByClientId(String clientId);
}
