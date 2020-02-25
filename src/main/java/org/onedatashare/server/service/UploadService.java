package org.onedatashare.server.service;

import org.codehaus.jackson.map.ObjectMapper;
import org.onedatashare.server.model.credential.UploadCredential;
import org.onedatashare.server.model.core.*;
import org.onedatashare.server.model.request.TransferRequest;
import org.onedatashare.server.model.useraction.IdMap;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.model.useraction.UserActionCredential;
import org.onedatashare.server.model.useraction.UserActionResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class UploadService {

    @Autowired
    UserService userService;

    @Autowired
    JobService jobService;

    @Autowired
    TransferService resourceService;

    private static Map<UUID, LinkedBlockingQueue<Slice>> ongoingUploads = new HashMap<UUID, LinkedBlockingQueue<Slice>>();

    public Mono<Boolean> uploadChunk(UUID uuid, Mono<FilePart> filePart, String credential,
                                 String directoryPath, String fileName, Long totalFileSize, String googleDriveId, String idmap) {
        if (ongoingUploads.containsKey(uuid)) {
            if(ongoingUploads.get(uuid).isEmpty())
                return sendFilePart(filePart, ongoingUploads.get(uuid)).map(size -> true);
            else return Mono.just(false);
        } else {
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setSrc(new UserActionResource());
            transferRequest.getSrc().setUri(ODSConstants.UPLOAD_IDENTIFIER);
            LinkedBlockingQueue<Slice> uploadQueue = new LinkedBlockingQueue<Slice>();

            transferRequest.getSrc().setUploader( new UploadCredential(uploadQueue, totalFileSize, fileName) );
            transferRequest.setDest( new UserActionResource());
            transferRequest.getDest().setId( googleDriveId );

            try {
                if(directoryPath.endsWith("/")) {
                    transferRequest.getDest().setUri( directoryPath+URLEncoder.encode(fileName,"UTF-8") );
                } else {
                    transferRequest.getDest().setUri( directoryPath+"/"+URLEncoder.encode(fileName,"UTF-8") );
                }
                ObjectMapper mapper = new ObjectMapper();
                transferRequest.getDest().setCredential( mapper.readValue(credential, UserActionCredential.class) );
                IdMap[] idms = mapper.readValue(idmap, IdMap[].class);
                transferRequest.getDest().setMap( new ArrayList<>(Arrays.asList(idms)) );
            }catch(Exception e){
                e.printStackTrace();
            }

            resourceService.submit(transferRequest).subscribe();

                return sendFilePart(filePart, uploadQueue).map(size -> {
                    if (size < totalFileSize) {
                        ongoingUploads.put(uuid, uploadQueue);
                    }
                    return true;
                });
        }
    }

    public Mono<Integer> sendFilePart(Mono<FilePart> pfr, LinkedBlockingQueue<Slice> qugue){

        return pfr.flatMapMany(fp -> fp.content())
                .reduce(new ByteArrayOutputStream(), (acc, newbuf)->{
                    try
                    {
                        Slice slc = new Slice(newbuf.asByteBuffer());
                        acc.write(slc.asBytes(), 0, slc.length());
                    }catch(Exception e){}
                    return acc;
        }).map(content ->  {
            ODSLoggerService.logInfo("uploading " + content.size());
            Slice slc = new Slice(content.toByteArray());
            qugue.add(slc);
            return slc.length();
        });
    }

    public Mono<Void> finishUpload(UUID uuid) {
        if(!ongoingUploads.containsKey(uuid)){
            return Mono.error(null);
        }
        ongoingUploads.remove(uuid);
        return Mono.just(null);
    }
}
