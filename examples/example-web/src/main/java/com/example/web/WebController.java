package com.example.web;

import com.cromxt.crombucket.sdk.FileVisibility;
import com.cromxt.crombucket.sdk.UpdateFileVisibilityRequest;
import com.cromxt.crombucket.sdk.clients.CromBucketWebClient;
import com.cromxt.crombucket.sdk.response.FileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/web")
public class WebController {

    private final CromBucketWebClient cromBucketWebClient;

    @PostMapping
    public FileResponse saveFile(@RequestParam("file") MultipartFile multipartFile,
            @RequestParam(required = false, defaultValue = "PUBLIC") FileVisibility visibility) throws IOException {
        return cromBucketWebClient.saveFile(multipartFile, visibility);
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable String mediaId) {
        cromBucketWebClient.deleteFile(mediaId);
    }

    @PatchMapping
    public FileResponse changeVisibility(@RequestBody UpdateFileVisibilityRequest updateFileVisibilityRequest) {
        return cromBucketWebClient.changeFileVisibility(updateFileVisibilityRequest);
    }

}
