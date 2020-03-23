package org.onedatashare.server.service.oauth;

import org.onedatashare.module.globusapi.GlobusClient;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class GridFtpAuthService {
    private GlobusClient globusclient = new GlobusClient();

    public Mono<String> start() {
        try {
            // Authorize the DbxWebAuth auth as well as redirect the user to the finishURI, done this way to appease OAuth 2.0
            return globusclient.generateAuthURL();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Mono<OAuthCredential> finish(Map<String, String> queryParameters) {
        String token = queryParameters.get("code");
        try {
            return globusclient.getAccessToken(token).map(
                    acctoken -> {
                        OAuthCredential oa = new OAuthCredential(acctoken.getTransferAccessToken());
                        oa.expiredTime = acctoken.getExpiredTime();
                        oa.name = "GridFTP Client";
                        return oa;
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
