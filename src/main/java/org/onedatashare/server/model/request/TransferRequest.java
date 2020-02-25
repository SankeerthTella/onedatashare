package org.onedatashare.server.model.request;

import lombok.Data;
import org.onedatashare.server.model.useraction.UserActionCredential;
import org.onedatashare.server.model.useraction.UserActionResource;

import java.util.Map;

@Data
public class TransferRequest {
    UserActionResource src;
    UserActionResource dest;
    TransferOptions options;
}

@Data
class SourceInfo{
    private String sourceDirectoryURI;
    private Map<String, String> filesToTransferMap;
    private UserActionCredential endpointCredential;
}

@Data
class DestinationInfo{
    private String destinationDirectoryURI;
    private String destinationId;
    private UserActionCredential endpointCredential;
}