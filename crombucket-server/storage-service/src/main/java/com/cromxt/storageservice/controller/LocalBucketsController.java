package com.cromxt.storageservice.controller;


import com.cromxt.storageservice.service.impl.StorageServerDetails;
import com.cromxt.crombucket.routeing.StorageServerAddress;
import com.cromxt.crombucket.routeing.MediaDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bucket-manager/api/v1/buckets")
@Profile({"local","local-docker","local-docker-dev"})
@RequiredArgsConstructor
public class LocalBucketsController {

    private final StorageServerDetails storageServerDetails;

    @PostMapping(value = "/fetch-storage-address")
    public StorageServerAddress getBucketDetails(@RequestBody MediaDetails ignored) {
        return StorageServerAddress.builder()
                .hostName(storageServerDetails.getRouteIp())
                .rpcPort(storageServerDetails.getRpcPort())
                .build();
    }
}
