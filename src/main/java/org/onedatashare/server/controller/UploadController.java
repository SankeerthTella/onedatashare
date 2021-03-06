/**
 ##**************************************************************
 ##
 ## Copyright (C) 2018-2020, OneDataShare Team, 
 ## Department of Computer Science and Engineering,
 ## University at Buffalo, Buffalo, NY, 14260.
 ## 
 ## Licensed under the Apache License, Version 2.0 (the "License"); you
 ## may not use this file except in compliance with the License.  You may
 ## obtain a copy of the License at
 ## 
 ##    http://www.apache.org/licenses/LICENSE-2.0
 ## 
 ## Unless required by applicable law or agreed to in writing, software
 ## distributed under the License is distributed on an "AS IS" BASIS,
 ## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ## See the License for the specific language governing permissions and
 ## limitations under the License.
 ##
 ##**************************************************************
 */


package org.onedatashare.server.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.core.ODSConstants;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/stork")
public class UploadController {

    @Autowired
    UploadService uploadService;

    //TODO: make asynchronous
    @PostMapping(value="/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<Object> upload(@RequestHeader HttpHeaders headers,
                               @RequestPart("directoryPath") String directoryPath,
                               @RequestPart("qqfilename") String fileName,
                               @RequestPart("map") String idMap,
                               @RequestPart("credential") String credential,
                               @RequestPart("id") String googledriveid,
                               @RequestPart("qquuid") String fileUUID,
                               @RequestPart("qqtotalfilesize") String totalFileSize,
                               @RequestPart("qqfile") Mono<FilePart> filePart){

        String cookie = headers.getFirst(ODSConstants.COOKIE);
        return uploadService.uploadChunk(cookie, UUID.fromString(fileUUID),
            filePart, credential, directoryPath, fileName,
            Long.parseLong(totalFileSize), googledriveid, idMap).map(success -> {
                FineUploaderResponse resp = new FineUploaderResponse();
                resp.success = success;
                resp.error = !success;
                return resp;
            });
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private class FineUploaderResponse {
        public boolean success;
        public boolean error;
    }
}

