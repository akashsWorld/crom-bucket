package com.cromxt.crombucket.sdk.clients.impl;

import com.crombucket.common.mediaservice.response.MediaObjects;
import com.crombucket.common.routeing.MediaDetails;
import com.crombucket.common.routeing.StorageServerAddress;
import com.crombucket.proto.storagenode.*;
import com.cromxt.crombucket.sdk.BucketUserDetails;
import com.cromxt.crombucket.sdk.FileVisibility;
import com.cromxt.crombucket.sdk.UpdateFileVisibilityRequest;
import com.cromxt.crombucket.sdk.clients.CromBucketWebClient;
import com.cromxt.crombucket.sdk.exceptions.CromBucketServerException;
import com.cromxt.crombucket.sdk.response.FileResponse;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;


@Slf4j
@RequiredArgsConstructor
public class CromBucketWebClientImpl extends CromBucketWebClient {

    private final BucketUserDetails clientCredentials;
    private final RestClient restClient;


    @Override
    public FileResponse saveFile(MultipartFile file) throws IOException {
        return initiateUploading(FileVisibility.PUBLIC, file.getSize(), extractExtension(Objects.requireNonNull(file.getOriginalFilename())), file.getInputStream());
    }

    @Override
    public FileResponse saveFile(MultipartFile file, FileVisibility fileVisibility) throws IOException {
        return initiateUploading(fileVisibility, file.getSize(), extractExtension(Objects.requireNonNull(file.getOriginalFilename())), file.getInputStream());
    }

    @Override
    public void deleteFile(String mediaId) throws CromBucketServerException {
        deleteMedias(List.of(mediaId));
    }

    @Override
    public void deleteMany(List<String> mediaIds) {
        deleteMedias(mediaIds);
    }

    @Override
    public FileResponse changeFileVisibility(UpdateFileVisibilityRequest updateFileVisibilityRequest) {
        return updateVisibility(updateFileVisibilityRequest);
    }

    @Override
    public List<FileResponse> changeFileVisibility(List<UpdateFileVisibilityRequest> updateFileVisibilityRequests) {
        return null;
    }

    @Override
    public FileResponse updateFile(String mediaId, MultipartFile file) throws IOException {
        return initiateUpdate(mediaId, file, FileVisibility.PUBLIC);
    }

    @Override
    public FileResponse updateFile(String mediaId, MultipartFile file, FileVisibility visibility) throws IOException {
        return initiateUpdate(mediaId, file, visibility);
    }

    private FileResponse initiateUpdate(
            String mediaId,
            MultipartFile file,
            FileVisibility visibility
    ) {
        deleteFile(mediaId);
        FileResponse fileResponse = null;
        try {
            fileResponse = initiateUploading(visibility, file.getSize(), extractExtension(Objects.requireNonNull(file.getOriginalFilename())), file.getInputStream());
        } catch (IOException ioException) {
            log.error("Error occurred while update the file with message {}", ioException.getMessage());
        }
        return fileResponse;
    }

    private FileResponse updateVisibility(UpdateFileVisibilityRequest updateFileVisibilityRequest) {
        String url = String.format("%s/%s", clientCredentials.getBaseUrl(), MEDIA_MANAGER_URL);
        MediaObjects mediaObjects = restClient
                .put()
                .uri(URI.create(url))
                .body(updateFileVisibilityRequest)
                .header("Authorization", clientCredentials.getClientSecret())
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new CromBucketServerException("Some error occurred.");
                })
                .body(MediaObjects.class);
        return FileResponse.builder()
                .mediaId(mediaObjects.getMediaId())
                .visibility(FileVisibility.valueOf(mediaObjects.getVisibility()))
                .accessUrl(mediaObjects.getAccessUrl())
                .fileSize(mediaObjects.getFileSize())
                .build();
    }

    private FileResponse initiateUploading(
            FileVisibility visibility,
            Long fileSize,
            String extension,
            InputStream fileData
    ) {

        StorageServerAddress storageServer = fetchStorageServer(fileSize);

        ManagedChannel channel = createNettyManagedChannel(storageServer);

        Metadata headers = generateHeaders(extension, clientCredentials.getClientSecret(), visibility);

        FileHandlerServiceGrpc.FileHandlerServiceStub fileHandlerGrpcStub = FileHandlerServiceGrpc.newStub(channel).withInterceptors(
                MetadataUtils.newAttachHeadersInterceptor(headers)
        );

        List<FileResponse> fileResponseList = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        StreamObserver<FileUploadRequest> requestStreamObserver = fileHandlerGrpcStub.uploadFile(new StreamObserver<>() {
            @Override
            public void onNext(MediaUploadResponse mediaUploadResponse) {
                if (mediaUploadResponse.getStatus() == OperationStatus.ERROR) {
                    return;
                }
                MediaObjectDetails mediaObjectDetails = mediaUploadResponse.getMediaObjectDetails();
                FileVisibility savedFileVisibility = getVisibility(mediaObjectDetails.getVisibility());
                FileResponse fileResponse = FileResponse.builder()
                        .mediaId(mediaObjectDetails.getMediaId())
                        .accessUrl(mediaObjectDetails.getAccessUrl())
                        .fileSize(mediaObjectDetails.getFileSize())
                        .visibility(savedFileVisibility)
                        .build();
                fileResponseList.add(fileResponse);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.fillInStackTrace();
                log.error("Some error occurred while saving the file with message {}", throwable.getMessage());
                countDownLatch.countDown();
            }

            @Override
            public void onCompleted() {
                channel.shutdown();
                countDownLatch.countDown();
            }
        });

        try {
            byte[] chunkData = new byte[4096];
            int length;
            while ((length = fileData.read(chunkData)) > 0) {
                requestStreamObserver.onNext(
                        FileUploadRequest
                                .newBuilder()
                                .setFile(ByteString.copyFrom(chunkData, 0, length))
                                .build()
                );
            }
            requestStreamObserver.onCompleted();
            countDownLatch.await();
        } catch (IOException ex) {
            throw new CromBucketServerException("Error occurred while read the file from stream data.");
        } catch (InterruptedException exception) {
            throw new CromBucketServerException("Some process interrupt the thread flow");
        }
        if (fileResponseList.isEmpty()) {
            throw new CromBucketServerException("Some error occurred on the Server.");
        }

        return fileResponseList.get(0);
    }


    private void deleteMedias(List<String> mediaIds) {
        String url = String.format("%s/%s/delete", clientCredentials.getBaseUrl(), MEDIA_MANAGER_URL);
        restClient
                .post()
                .uri(URI.create(url))
                .header("Authorization", clientCredentials.getClientSecret())
                .body(mediaIds)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                    throw new CromBucketServerException("Some error occurred");
                })
                .toBodilessEntity();
    }

    private StorageServerAddress fetchStorageServer(Long fileSize) {
        String url = String.format("%s/%s", clientCredentials.getBaseUrl(), BUCKET_MANAGER_URL);
        MediaDetails mediaDetails = MediaDetails.builder()
                .fileSize(fileSize)
                .build();
        return restClient
                .post()
                .uri(URI.create(url))
                .header("Authorization", clientCredentials.getClientSecret())
                .body(mediaDetails)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (clientRequest, clientResponse) -> {
                    throw new CromBucketServerException("Some error occurred.");
                })
                .body(StorageServerAddress.class);
    }
}
