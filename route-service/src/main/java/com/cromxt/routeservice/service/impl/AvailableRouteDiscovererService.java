package com.cromxt.routeservice.service.impl;

import com.cromxt.buckets.BucketInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AvailableRouteDiscovererService {


    private final Map<String, AvailableBuckets> ALL_BUCKETS = new HashMap<>();

    public AvailableRouteDiscovererService() {

    }

    @KafkaListener(topics = "buckets")
    private void bucketListUpdated(BucketInformation bucketInformation) {
        ALL_BUCKETS.put(bucketInformation.getBucketId(),
                AvailableBuckets.builder()
                        .hostName(bucketInformation.getBucketHostName())
                        .port(bucketInformation.getPort())
                        .initializationTime(System.currentTimeMillis())
                        .availableSpaceInBytes(bucketInformation.getAvailableSpaceInBytes())
                        .build());

    }

    @Builder
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class AvailableBuckets {

        private String hostName;
        private Integer port;
        private Long initializationTime;
        private Long availableSpaceInBytes;
    }

//    Needs as JSON parser.
    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    private static class BucketData {
//        Add extra metadata if needed.
        List<Buckets> buckets;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    private static class Buckets {
        String bucketId;
        String hostName;
        Integer port;
    }

}