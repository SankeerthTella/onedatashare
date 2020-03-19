package org.onedatashare.server.service.oauth;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.*;
import lombok.AllArgsConstructor;

import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.service.ODSLoggerService;
import org.onedatashare.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;

/**
 *author: Javier Falca
 */
@ConfigurationProperties(prefix = "box")
@ConstructorBinding
@AllArgsConstructor
class BoxConfig {
    final String clientId;
    final String clientSecret;
    final String redirectUri;
    final String scope;
    final String authUri;
}

@Service
public class BoxOauthService implements OAuthService{

    @Autowired
    private UserService userService;

    @Autowired
    private BoxConfig boxConfig;

    public String start() {
        String box_redirect = boxConfig.authUri
                + "?response_type=code"
                + "&client_id=" + boxConfig.clientId
                + "&redirect_uri=" + boxConfig.redirectUri
                + "&scope=" + boxConfig.scope;

        return box_redirect;
    }

    /**
     * @param queryParameters: Access Token returned by Box Authentication using OAuth 2
     * @return OAuthCredential
     */

    public OAuthCredential finish(Map<String, String> queryParameters) {
        String code = queryParameters.get("code");
        // Instantiate new Box API connection object
        BoxAPIConnection client = new BoxAPIConnection(boxConfig.clientId, boxConfig.clientSecret, code);
        OAuthCredential oauth = new OAuthCredential(client.getAccessToken());
        BoxUser user = BoxUser.getCurrentUser(client);
        BoxUser.Info userInfo = user.getInfo();
        oauth.name = "Box: " + userInfo.getLogin();
        oauth.token = client.getAccessToken();
        oauth.refreshToken = client.getRefreshToken();
        Date currentTime = new Date();
        oauth.lastRefresh = new Date(currentTime.getTime());
        oauth.expiredTime = new Date(currentTime.getTime() + client.getExpires());
        return oauth;
    }
}
