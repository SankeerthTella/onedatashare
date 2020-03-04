package org.onedatashare.server.service.oauth;

import com.box.sdk.BoxAPIConnection;
import com.box.sdk.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.service.ODSLoggerService;
import org.onedatashare.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.beans.ConstructorProperties;
import java.util.Date;

/**
 *author: Javier Falca
 */

@ConfigurationProperties(prefix = "box")
@ConstructorBinding
@AllArgsConstructor
class BoxConfig {
    final String client_id;
    final String client_secret;
    final String redirect_uri;
    final String scope;
    final String box_redirect;
}

@Service
public class BoxOauthService {

    @Autowired
    private UserService userService;

    @Autowired
    private BoxConfig boxConfig;

    public String start() {
        String box_redirect = boxConfig.box_redirect
                + "?response_type=code"
                + "&client_id=" + boxConfig.client_id
                + "&redirect_uri=" + boxConfig.redirect_uri
                + "&scope=" + boxConfig.scope;

        return box_redirect;
    }

    /**
     * @param code: Access Token returned by Box Authentication using OAuth 2
     * @return OAuthCredential
     */

    public Mono<OAuthCredential> finish(String code, String cookie) {

        // Instantiate new Box API connection object
        BoxAPIConnection client = new BoxAPIConnection(boxConfig.client_id, boxConfig.client_secret, code);
        OAuthCredential oauth = new OAuthCredential(client.getAccessToken());
        BoxUser user = BoxUser.getCurrentUser(client);
        BoxUser.Info userInfo = user.getInfo();
        oauth.name = "Box: " + userInfo.getLogin();
        oauth.token = client.getAccessToken();
        oauth.refreshToken = client.getRefreshToken();
        Date currentTime = new Date();
        oauth.lastRefresh = new Date(currentTime.getTime());
        oauth.expiredTime = new Date(currentTime.getTime() + client.getExpires());
        try{
            return userService.getCredentials(cookie).flatMap(val -> {
                for (Credential value : val.values()) {
                    OAuthCredential oauthVal = ((OAuthCredential) value);
                    if ((oauthVal.name != null && oauthVal.name.equals(oauth.name))) { //Checks if the ID already matches
                        return Mono.empty(); //Account already exists
                    }
                }
                return Mono.just(oauth);
            });
        } catch(Exception e) {
            ODSLoggerService.logError("Runtime exception occurred while finishing initializing Box oauth session");
            throw new RuntimeException(e);
        }
    }
}
