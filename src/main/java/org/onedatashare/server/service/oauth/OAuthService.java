    package org.onedatashare.server.service.oauth;

    import org.onedatashare.server.model.credential.OAuthCredential;

    import java.util.Map;

    public interface OAuthService {
        String start();
        OAuthCredential finish(Map<String, String> queryParameters);
    }
