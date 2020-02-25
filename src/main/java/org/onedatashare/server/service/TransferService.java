package org.onedatashare.server.service;

import org.onedatashare.module.globusapi.GlobusClient;
import org.onedatashare.server.model.core.*;
import org.onedatashare.server.model.credential.GlobusWebClientCredential;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.model.credential.UploadCredential;
import org.onedatashare.server.model.credential.UserInfoCredential;
import org.onedatashare.server.model.request.TransferRequest;
import org.onedatashare.server.model.useraction.IdMap;
import org.onedatashare.server.model.useraction.UserActionResource;
import org.onedatashare.server.module.box.BoxSession;
import org.onedatashare.server.module.clientupload.ClientUploadSession;
import org.onedatashare.server.module.dropbox.DbxSession;
import org.onedatashare.server.module.googledrive.GoogleDriveSession;
import org.onedatashare.server.module.gridftp.GridftpSession;
import org.onedatashare.server.module.http.HttpSession;
import org.onedatashare.server.module.vfs.VfsSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.onedatashare.server.model.core.ODSConstants.*;

@Service
public class TransferService {
    @Autowired
    private UserService userService;

    @Autowired
    private JobService jobService;

    @Autowired
    private DecryptionService decryptionService;

    // Hashmap that stores the disposable threads for every transfer
    private ConcurrentHashMap<UUID, Disposable> ongoingJobs = new ConcurrentHashMap<>();

    public Mono<Resource> getResourceWithUserActionResource(User userObj, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        String id = userActionResource.getId();
        ArrayList<IdMap> idMap = userActionResource.getMap();
        return Mono.just(userObj)
                .flatMap(user -> createCredential(userActionResource, user))
                .map(credential -> createSession(userActionResource.getUri(), credential))
                .flatMap(session -> {
                    if (session instanceof GoogleDriveSession && !userActionResource.getCredential().isTokenSaved())
                        return ((GoogleDriveSession) session).initializeNotSaved();
                    if (session instanceof BoxSession && !userActionResource.getCredential().isTokenSaved())
                        return ((BoxSession) session).initializeNotSaved();
                    else
                        return session.initialize();
                })
                .flatMap(session -> ((Session) session).select(path, id, idMap));
    }


    public Mono<Resource> getResourceWithUserActionResource(String cookie, UserActionResource userActionResource) {
        final String path = pathFromUri(userActionResource.getUri());
        String id = userActionResource.getId();
        ArrayList<IdMap> idMap = userActionResource.getMap();
        return userService.getLoggedInUser(cookie)
                .flatMap(user -> createCredential(userActionResource, user))
                .map(credential -> createSession(userActionResource.getUri(), credential))
                .flatMap(session -> {
                    if (session instanceof GoogleDriveSession && !userActionResource.getCredential().isTokenSaved())
                        return ((GoogleDriveSession) session).initializeNotSaved();
                    if (session instanceof BoxSession && !userActionResource.getCredential().isTokenSaved())
                        return ((BoxSession) session).initializeNotSaved();
                    else
                        return session.initialize();
                })
                .flatMap(session -> ((Session) session).select(path, id, idMap));
    }

    public String pathFromUri(String uri) {
        String path = "";
        if (uri.startsWith(DROPBOX_URI_SCHEME))
            path = uri.substring(DROPBOX_URI_SCHEME.length() - 1);
        else if (uri.startsWith(DRIVE_URI_SCHEME))
            path = uri.substring(DRIVE_URI_SCHEME.length() - 1);
        else
            path = uri;

        try {
            path = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Mono<Credential> createCredential(UserActionResource userActionResource, User user) {
        if (userActionResource.getUri().startsWith(DROPBOX_URI_SCHEME) ||
                userActionResource.getUri().startsWith(DRIVE_URI_SCHEME) || userActionResource.getUri().startsWith(BOX_URI_SCHEME)) {
            if (user.isSaveOAuthTokens()) {
                return Mono.just(
                        user.getCredentials().get(
                                UUID.fromString(userActionResource.getCredential()
                                        .getUuid())
                        ));
            }
            else {
                return Mono.just( new OAuthCredential(userActionResource.getCredential().getToken()));
            }
        }
        else if (userActionResource.getUri().equals(UPLOAD_IDENTIFIER)) {
            return Mono.just( userActionResource.getUploader() );
        }
        else if (userActionResource.getUri().startsWith(GRIDFTP_URI_SCHEME)) {
            GlobusClient gc = userService.getGlobusClientFromUser(user);
            return Mono.just(new GlobusWebClientCredential(userActionResource.getCredential().getGlobusEndpoint(), gc));
        }
        else if (userActionResource.getUri().startsWith(SFTP_URI_SCHEME) ||
                userActionResource.getUri().startsWith(SCP_URI_SCHEME)){
            return decryptionService.getDecryptedCredential(userActionResource.getCredential())
                    .map(cred -> new UserInfoCredential(cred));
        }
        else
            return Mono.just(new UserInfoCredential(userActionResource.getCredential()));
    }


    public Session createSession(String uri, Credential credential) {
        if (uri.startsWith(DROPBOX_URI_SCHEME)) {
            return new DbxSession(URI.create(uri), credential);
        }
        else if (uri.equals(UPLOAD_IDENTIFIER)) {
            UploadCredential upc = (UploadCredential) credential;
            return new ClientUploadSession(upc.getFux(), upc.getSize(), upc.getName());
        } else if (uri.startsWith(DRIVE_URI_SCHEME))
            return new GoogleDriveSession(URI.create(uri), credential);
        else if(uri.startsWith(ODSConstants.BOX_URI_SCHEME)) {
            return new BoxSession(URI.create(uri), credential);
        }
        else if(credential instanceof GlobusWebClientCredential) {
            return new GridftpSession(URI.create(uri), credential);
        }
        else if (uri.startsWith(HTTPS_URI_SCHEME) || uri.startsWith(HTTP_URI_SCHEME)) {
            return new HttpSession(URI.create(uri));
        }
        else {
            return new VfsSession(URI.create(uri), credential);
        }
    }

    public Mono<Job> submit(TransferRequest transferRequest) {
        AtomicReference<User> u = new AtomicReference<>();
        return userService.getLoggedInUser()
                .map(user -> {
                    Job job = new Job(transferRequest.getSourceInfo(), transferRequest.getDestinationInfo());
                    job.setStatus(JobStatus.scheduled);
                    job = user.saveJob(job);
                    userService.saveUser(user).subscribe();
                    u.set(user);
                    return job;
                })
                .flatMap(jobService::saveJob)
                .doOnSuccess(job -> processTransferFromJob(job, u.get()))
                .subscribeOn(Schedulers.elastic());
    }

    public Mono<Job> restartJob(String uuid) {
        return userService.getLoggedInUser()
                .flatMap(user -> jobService.findById(uuid)
                        .flatMap(job -> {
                            Job restartedJob = new Job(job.getSrc(), job.getDest());
                            boolean credsExists = updateJobCredentials(user, job);
                            if (!credsExists) {
                                return Mono.error(new Exception("Restart job failed since either or both credentials of the job do not exist"));
                            }
                            restartedJob.setStatus(JobStatus.scheduled);
                            restartedJob.setRestartedJob(true);
                            restartedJob = user.saveJob(restartedJob);
                            userService.saveUser(user).subscribe();
                            return Mono.just(restartedJob);
                        })
                        .flatMap(jobService::saveJob)
                        .doOnSuccess(restartedJob -> processTransferFromJob(restartedJob, new String())));
    }

    public Mono<Job> deleteJob(String uuid) {
        return jobService.findById(uuid)
                .map(job -> job.setDeleted(true))
                .flatMap(jobService::saveJob);
    }

    /**
     * This method cancel an ongoing transfer.
     * User email and job id passed in the request is used to obtain the job UUID,
     * which is in turn used to access the ongoing job flux from the ongoingJobs map.
     * This flux is then disposed and the job is evicted from the map to cancel the transfer.
     *
     * @param uuid
     * @return Mono of job that was stopped
     */
    public Mono<Job> cancel(String uuid) {
        return userService.getLoggedInUser()
                .flatMap((User user) -> jobService.findById(uuid)
                        .map(job -> {
                            try {
                                ongoingJobs.get(job.getUuid()).dispose();
                                ongoingJobs.remove(job.getUuid());
                            }catch (Exception e){
                                ODSLoggerService.logError("Unable to remove job " + job.getUuid() + "- Not found");
                            }
                            return job.setStatus(JobStatus.cancelled);
                        }))
                .flatMap(jobService::saveJob);
    }

    public boolean updateJobCredentials(User user, Job restartedJob) {
        boolean credsExist = true;
        if (restartedJob.getSrc().getCredential() != null) {
            UUID srcCredUUID = getCredUuidUsingCredName(user, restartedJob.getSrc().getCredential().getName());
            if (srcCredUUID != null) {
                if (!UUID.fromString(restartedJob.getSrc().getCredential().getUuid()).equals(srcCredUUID)) {
                    restartedJob.getSrc().getCredential().setUuid(srcCredUUID.toString());
                }
            } else
                credsExist = false;
        }

        if (!credsExist)
            return credsExist;    // don't want to check for dest cred if src cred doesn't exist

        if (restartedJob.getDest().getCredential() != null) {
            UUID destCredUUID = getCredUuidUsingCredName(user, restartedJob.getDest().getCredential().getName());
            if (destCredUUID != null) {
                if (!UUID.fromString(restartedJob.getDest().getCredential().getUuid()).equals(destCredUUID)) {
                    restartedJob.getDest().getCredential().setUuid(destCredUUID.toString());
                }
            } else
                credsExist = false;
        }

        return credsExist;
    }

    public UUID getCredUuidUsingCredName(User user, String credName) {
        for (Map.Entry<UUID, Credential> userCredEntry : user.getCredentials().entrySet()) {
            if (userCredEntry.getValue() instanceof OAuthCredential) {
                OAuthCredential cred = (OAuthCredential) userCredEntry.getValue();
                if (cred.getName().equals(credName)) {
                    return userCredEntry.getKey();
                }
            }
        }
        return null;
    }

    public void processTransferFromJob(Job job, User user) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        Disposable ongoingJob = getResourceWithUserActionResource(user, job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(user, job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> {
                    return transfer1.start(TRANSFER_SLICE_SIZE);
                })
                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
                .doOnCancel(new RunnableCanceler(job))
                .doFinally(s -> {
                    if (job.getStatus() != JobStatus.cancelled && job.getStatus() != JobStatus.failed)
                        job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                    ongoingJobs.remove(job.getUuid());
                })
                .map(job::updateJobWithTransferInfo)
                .flatMap(jobService::saveJob)
                .subscribe();
        ongoingJobs.put(job.getUuid(), ongoingJob);
    }

    public void processTransferFromJob(Job job, final String cookie) {
        Transfer<Resource, Resource> transfer = new Transfer<>();
        Disposable ongoingJob = getResourceWithUserActionResource(cookie, job.getSrc())
                .map(transfer::setSource)
                .flatMap(t -> getResourceWithUserActionResource(cookie, job.getDest()))
                .map(transfer::setDestination)
                .flux()
                .flatMap(transfer1 -> transfer1.start(TRANSFER_SLICE_SIZE))
                .doOnSubscribe(s -> job.setStatus(JobStatus.transferring))
                .doOnCancel(new RunnableCanceler(job))
                .doFinally(s -> {
                    if (job.getStatus() != JobStatus.cancelled && job.getStatus() != JobStatus.failed)
                        job.setStatus(JobStatus.complete);
                    jobService.saveJob(job).subscribe();
                    ongoingJobs.remove(job.getUuid());
                })
                .map(job::updateJobWithTransferInfo)
                .flatMap(jobService::saveJob)
                .subscribe();
        ongoingJobs.put(job.getUuid(), ongoingJob);
    }

    class RunnableCanceler implements Runnable {
        Job job;

        public RunnableCanceler(Job job) {
            this.job = job;
        }

        @Override
        public void run() {
            job.setStatus(JobStatus.failed);
        }
    }
}
