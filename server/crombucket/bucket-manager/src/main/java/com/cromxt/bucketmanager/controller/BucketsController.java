package com.cromxt.bucketmanager.controller;

import com.cromxt.bucketmanager.dtos.request.BucketRequest;
import com.cromxt.bucketmanager.dtos.response.BucketResponse;
import com.cromxt.bucketmanager.service.BucketService;
import com.cromxt.common.crombucket.routeing.MediaDetails;
import com.cromxt.common.crombucket.routeing.StorageServerAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/api/v1/buckets")
@RequiredArgsConstructor
public class BucketsController {

    private final BucketService bucketService;

    @PostMapping
    public Mono<BucketResponse> createBucket(@RequestAttribute(name = "clientId") String clientId,
                                             @RequestBody BucketRequest bucketRequest) {
        return bucketService.addBucket(clientId, bucketRequest);
    }

    @PutMapping(value = "/{bucketId}")
    public Mono<BucketResponse> updateBucket(@PathVariable String bucketId,
                                             @RequestBody BucketRequest bucketRequest) {
        return bucketService.updateBucket(bucketId, bucketRequest);
    }

    @DeleteMapping(value = "/{bucketId}")
    public Mono<BucketResponse> deleteBucket(@PathVariable String bucketId) {
        return bucketService.removeBucket(bucketId);
    }

    @PostMapping(value = "/get-storage-address")
    public Mono<StorageServerAddress> getStorageAddress(@RequestAttribute String clientId,
                                                        @RequestBody MediaDetails mediaDetails) {
        return bucketService.fetchStorageServerAddress(clientId, mediaDetails);
    }

}


