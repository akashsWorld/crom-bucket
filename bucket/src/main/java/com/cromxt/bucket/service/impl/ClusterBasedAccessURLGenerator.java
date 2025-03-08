package com.cromxt.bucket.service.impl;


import com.cromxt.bucket.service.AccessURLGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev","prod"})
@RequiredArgsConstructor
public class ClusterBasedAccessURLGenerator implements AccessURLGenerator {

    private final BucketInformationService bucketInformationService;

    @Override
    public String generateAccessURL(String fileId) {
        return "aLongURL";
    }
}
