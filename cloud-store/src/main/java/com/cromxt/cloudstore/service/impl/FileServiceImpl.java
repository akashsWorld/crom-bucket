package com.cromxt.cloudstore.service.impl;


import com.cromxt.cloudstore.clients.BucketClient;
import com.cromxt.cloudstore.clients.RouteServiceClient;
import com.cromxt.cloudstore.dtos.requests.FileUploadRequest;
import com.cromxt.cloudstore.dtos.response.FileResponse;
import com.cromxt.cloudstore.dtos.response.MediaObjectDetails;
import com.cromxt.cloudstore.entity.MediaObjects;
import com.cromxt.cloudstore.repository.MediaRepository;
import com.cromxt.cloudstore.service.FileService;
import com.cromxt.dtos.requests.FileMetaData;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FileServiceImpl implements FileService {

    private final BucketClient bucketClient;
    private final RouteServiceClient routeService;
    private final MediaRepository mediaRepository;

    public FileServiceImpl(BucketClient bucketClient,
                           RouteServiceClient routeService,
                           MediaRepository mediaRepository) {
        this.bucketClient = bucketClient;
        this.routeService = routeService;
        this.mediaRepository = mediaRepository;
    }

    @Override
    public Mono<FileResponse> saveFile(FileUploadRequest fileUploadRequest) {
        FilePart mediaObject = fileUploadRequest.mediaObject();
        String fileExtension = getFileExtension(mediaObject.filename());

        FileMetaData.FileMetaDataBuilder fileMetaDataBuilder = FileMetaData
                .builder()
                .fileExtension(fileExtension);

        Mono<FileMetaData> fileMetaDataMono = getFileSize(mediaObject)
                .map(fileSize -> fileMetaDataBuilder.fileSize(fileSize).build());


        Mono<MediaObjectDetails> mediaObjectDetailsMono = fileMetaDataMono.flatMap(fileMetaData ->
                routeService
                        .getBucketId(fileMetaData)
                        .flatMap(bucketDetails -> bucketClient
                                .uploadFile(
                                        mediaObject.content(),
                                        bucketDetails
                                )
                        )
        );
        return mediaObjectDetailsMono.flatMap(mediaObjectDetails ->
                        mediaRepository
                                .save(MediaObjects
                                        .builder()
                                        .name(mediaObjectDetails.getFileId())
                                        .size(mediaObjectDetails.getFileSize())
                                        .bucketId(mediaObjectDetails.getBucketId())
                                        .fileExtension(mediaObjectDetails.getFileExtension())
                                        .build()
                                )
                )
                .map(mediaObjectDetails ->
                        new FileResponse(mediaUrlBuilder(
                                mediaObjectDetails.getBucketId(),
                                mediaObjectDetails.getName()
                        )
                        )
                );
    }

    private String getFileExtension(String fileName) {
        return fileName
                .substring(fileName
                        .lastIndexOf(".")
                );
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

    private String mediaUrlBuilder(String bucketId, String fileId) {
//       TODO:Implement the media url pattern later according to design.
        return String.format("https://%s-%s", bucketId, fileId);
    }


}
