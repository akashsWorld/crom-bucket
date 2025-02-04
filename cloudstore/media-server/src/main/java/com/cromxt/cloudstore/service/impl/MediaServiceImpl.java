package com.cromxt.cloudstore.service.impl;


import com.cromxt.cloudstore.clients.BucketClient;
import com.cromxt.cloudstore.clients.RouteServiceClient;
import com.cromxt.cloudstore.dtos.MediaObjectMetadata;
import com.cromxt.cloudstore.dtos.requests.MediaUploadRequest;
import com.cromxt.cloudstore.dtos.response.FileResponse;
import com.cromxt.cloudstore.dtos.response.MediaObjectDetails;
import com.cromxt.cloudstore.entity.MediaObjects;
import com.cromxt.cloudstore.repository.MediaRepository;
import com.cromxt.cloudstore.service.MediaService;
import com.cromxt.dtos.client.requests.MediaMetadata;
import com.cromxt.dtos.client.response.BucketAddress;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class MediaServiceImpl implements MediaService {

    private final BucketClient bucketClient;
    private final RouteServiceClient routeService;
    private final MediaRepository mediaRepository;

    public MediaServiceImpl(BucketClient bucketClient,
                           RouteServiceClient routeService,
                           MediaRepository mediaRepository) {
        this.bucketClient = bucketClient;
        this.routeService = routeService;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Mono<FileResponse> saveFile(
            MediaUploadRequest fileUploadRequest,
            String fileName,
            Boolean hlsStatus
    ) {
        FilePart mediaObject = fileUploadRequest.media();
        String fileExtension = getFileExtension(mediaObject.filename());

        Mono<Long> fileSize = getFileSize(mediaObject);

        return fileSize.flatMap(totalFileSize -> {
            MediaMetadata fileMetaData = new MediaMetadata(totalFileSize, fileExtension);
            // TODO: Enable the route service after later.

            // Mono<BucketAddress> bucketDetails = routeService.getBucketAddress(fileMetaData);

            Mono<BucketAddress> bucketDetails = Mono.just(new BucketAddress("192.168.0.146", 9090));


            return bucketDetails.flatMap(bucket -> {
                MediaObjectMetadata mediaObjectMetadata =
                        MediaObjectMetadata.builder()
                                .fileName(fileName)
                                .contentType(fileExtension)
                                .hlsStatus(hlsStatus)
                                .build();
                Mono<MediaObjectDetails> mediaObjectDetailsMono = bucketClient.uploadFile(mediaObject.content(), mediaObjectMetadata, bucket);


                return mediaObjectDetailsMono.flatMap(mediaObjectDetails -> {
                    MediaObjects mediaObjects = MediaObjects.builder()
                            .fileExtension(fileExtension)
                            .name(mediaObjectDetails.getFileId())
                            .size(totalFileSize)
                            .bucketId(null)
                            .build();
                    return mediaRepository.save(mediaObjects)
                            .map(savedObject -> new FileResponse(
                                    accessUrlBuilder(savedObject.getBucketId(), savedObject.getId())
                            ));
                });

            });

        });
    }

    private String getFileExtension(String fileName) {
            String fileExtension = fileName
                .substring(fileName
                        .lastIndexOf(".")+1
                );
            if(fileExtension.isEmpty()){
                throw new RuntimeException("File extension is empty");
            }
            return fileExtension;
    }

    private Mono<Long> getFileSize(FilePart mediaObject) {
        return mediaObject
                .content()
                .map(dataBuffer -> {
                    long size = dataBuffer.readableByteCount();
                    dataBuffer.readPosition(0);
                    return size;
                })
                .reduce(Long::sum);
    }

    private String accessUrlBuilder(String bucketId, String fileId) {
//       TODO:Implement the media url pattern later according to design.
        return String.format("https://%s-%s", bucketId, fileId);
    }


}
