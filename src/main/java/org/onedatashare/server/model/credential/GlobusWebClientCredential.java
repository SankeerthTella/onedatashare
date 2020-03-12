package org.onedatashare.server.model.credential;

import org.onedatashare.module.globusapi.EndPoint;
import org.onedatashare.module.globusapi.GlobusClient;
import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.core.EndpointType;

import java.util.Date;

public class GlobusWebClientCredential extends Credential {
    public EndPoint _endpoint;
    public GlobusClient _globusClient;

    public GlobusWebClientCredential(EndPoint endpoint, GlobusClient globusClient) {
        this.type = EndpointType.GRIDFTP;
        this._endpoint = endpoint;
        _globusClient = globusClient;
    }
}
