package com.cromxt.bucketservice.enity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "buckets-group")
public class BucketGroups {
    @Id
    private String id;
    private String clientId;
}
