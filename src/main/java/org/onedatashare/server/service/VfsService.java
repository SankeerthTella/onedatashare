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


package org.onedatashare.server.service;

import org.onedatashare.server.model.core.*;
import org.onedatashare.server.model.credential.UserInfoCredential;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.model.useraction.UserActionResource;
import org.onedatashare.server.module.vfs.VfsResource;
import org.onedatashare.server.module.vfs.VfsSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import static org.onedatashare.server.model.core.ODSConstants.TRANSFER_SLICE_SIZE;

@Service
public class VfsService extends ResourceService {
    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private DecryptionService decryptionService;

    public Mono<VfsResource> getResourceWithUserActionUri(String cookie, UserAction userAction) {
        final String path = pathFromUri(userAction.getUri());
        return userService.getLoggedInUser(cookie)
                .flatMap(user -> {
                    if(userAction.getCredential() == null){
                        // Credentials for FTP will be null
                        return Mono.just(new UserInfoCredential(null));
                    }
                    else {
                        return decryptionService.getDecryptedCredential(userAction.getCredential())
                                .map(userActionCred -> new UserInfoCredential(userActionCred));
                    }
                })
                .map(credential -> {
                    // Encoding the resource URI to avoid errors due to spaces in file/directory names
                    String encodedURI = userAction.getUri();

                    try {
                        encodedURI = URLEncoder.encode(userAction.getUri(), "UTF-8");
                    }
                    catch(UnsupportedEncodingException uee){
                        ODSLoggerService.logError("Exception encountered while encoding input URI");
                        Mono.error(uee);
                    }
                    return new VfsSession(URI.create(encodedURI), credential);
                })
                .flatMap(vfsSession -> vfsSession.initialize())
                .flatMap(vfsSession -> vfsSession.select(path, userAction.getPortNumber()));
    }

    public Mono<VfsResource> getResourceWithUserActionResource(String cookie, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        return userService.getLoggedInUser(cookie)
                .flatMap(user -> {
                    if(userActionResource.getCredential() == null){
                        // Credentials for FTP will be null
                        return Mono.just(new UserInfoCredential(null));
                    }
                    else {
                        return decryptionService.getDecryptedCredential(userActionResource.getCredential())
                                .map(userActionCred -> new UserInfoCredential(userActionCred));
                    }
                })
                .map(credential -> {
                    // Encoding the resource URI to avoid errors due to spaces in file/directory names
                    String encodedURI = userActionResource.getUri();
                    try {
                        encodedURI = URLEncoder.encode(userActionResource.getUri(), "UTF-8");
                    }
                    catch(UnsupportedEncodingException uee){
                        ODSLoggerService.logError("Exception encountered while encoding input URI");
                        Mono.error(uee);
                    }
                    return new VfsSession(URI.create(encodedURI), credential);
                })
                .flatMap(VfsSession::initialize)
                .flatMap(vfsSession -> vfsSession.select(path));
    }


    public String pathFromUri(String uri) {
        String path = "";
        if (uri.contains(ODSConstants.DROPBOX_URI_SCHEME)) {
            path = uri.substring(ODSConstants.DROPBOX_URI_SCHEME.length() - 1);
        } else path = uri;
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::stat);
    }

    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(VfsResource::mkdir)
                .map(r -> true);
    }

    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction)
                .flatMap(VfsResource::delete)
                .map(val -> true);
    }

    public Mono<Job> submit(String cookie, UserAction userAction) {
        return userService.getLoggedInUser(cookie)
                .map(user -> {
                    Job job = new Job(userAction.getSrc(), userAction.getDest());
                    job.setStatus(JobStatus.scheduled);
                    job = user.saveJob(job);
                    userService.saveUser(user).subscribe();
                    return job;
                })
                .flatMap(jobService::saveJob)
                .doOnSuccess(job -> processTransferFromJob(job, cookie))
                .subscribeOn(Schedulers.elastic());
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::getDownloadURL);
    }

    public void processTransferFromJob(Job job, String cookie) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        getResourceWithUserActionResource(cookie, job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(cookie, job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> transfer1.start(TRANSFER_SLICE_SIZE))
                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
                .doFinally(s -> {
                    job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                })
                .map(job::updateJobWithTransferInfo)
                .flatMap(jobService::saveJob)
                .subscribe();
    }

    public Mono<String> getDownloadURL(String cookie, UserAction userAction) {
        return getResourceWithUserActionUri(cookie, userAction).flatMap(VfsResource::getDownloadURL);
    }

    public Mono<ResponseEntity> getSftpDownloadStream(String cookie, UserActionResource userActionResource) {
        return getResourceWithUserActionResource(cookie, userActionResource).flatMap(VfsResource::sftpObject);
    }
}
