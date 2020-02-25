package org.onedatashare.server.model.request;

import lombok.Data;
import org.onedatashare.server.model.useraction.UserActionCredential;
import org.onedatashare.server.model.useraction.UserActionResource;

import java.util.Map;

@Data
public class TransferRequest {
    @Data
    public class SourceInfo{
        private String sourceDirectoryURI;
        private Map<String, String> filesToTransferMap;
        private UserActionCredential endpointCredential;
    }

    @Data
    public class DestinationInfo{
        private String destinationDirectoryURI;
        private String destinationId;
        private UserActionCredential endpointCredential;
    }

    @Deprecated private UserActionResource src;
    @Deprecated private UserActionResource dest;
    private SourceInfo sourceInfo;
    private DestinationInfo destinationInfo;
    private TransferOptions options;
}