package org.onedatashare.server.service.oauth;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import lombok.AllArgsConstructor;
import org.onedatashare.server.model.core.Credential;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.result.view.RedirectView;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "dropbox")
@ConstructorBinding
@AllArgsConstructor
class DbxConfig {
    final String client_id;
    final String client_secret;
    final String redirect_uri;
    final String identifier;
}


@Service
public class DbxOauthService  {

    @Autowired
    private DbxConfig dbxConfig;

    @Autowired
    private UserService userService;

    private DbxAppInfo secrets;

    private DbxRequestConfig config;

    private DbxSessionStore sessionStore;

    private DbxWebAuth auth;

    public String start() {
        if (secrets == null) {
            throw new RuntimeException("Dropbox OAuth is disabled.");
        } if (auth != null) {
//            throw new IllegalStateException("Don't call this twice.");
        } try {
            auth = new DbxWebAuth(config, secrets);
            // Authorize the DbxWebAuth auth as well as redirect the user to the finishURI, done this way to appease OAuth 2.0
            return auth.authorize(DbxWebAuth.Request.newBuilder().withRedirectUri(dbxConfig.redirect_uri, sessionStore).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized Mono<OAuthCredential> finish(String token, String cookie) {
        Map<String,String[]> map = new HashMap();
        map.put("state", new String[] {dbxConfig.client_id});
        map.put("code", new String[] {token});
        try {
            DbxAuthFinish finish = auth.finishFromRedirect(dbxConfig.redirect_uri, sessionStore, map);
            OAuthCredential cred = new OAuthCredential(finish.getAccessToken());
            FullAccount account = new DbxClientV2(config, finish.getAccessToken()).users().getCurrentAccount();
            cred.name = "Dropbox: " + account.getEmail();
            cred.dropboxID = account.getAccountId();
            return userService.getCredentials(cookie).flatMap(val -> {

                for (Credential value: val.values()) {
                    OAuthCredential oauthVal = ((OAuthCredential) value);
                    if ((oauthVal.dropboxID != null && oauthVal.dropboxID.equals(cred.dropboxID))) { //Checks if the ID already matches
                        return Mono.empty();           //Account already exists
                    }
                }

                return Mono.just(cred);            //Account is not in the database, store as new
            });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public RedirectView redirectToDropboxAuth(Boolean value) {
        String url = start();
        return new RedirectView(url);
    }

    @PostConstruct
    public void postConstructInit(){
        secrets = new DbxAppInfo(dbxConfig.client_id, dbxConfig.client_secret);
        config = DbxRequestConfig.newBuilder(dbxConfig.identifier).build();
        sessionStore = new DbxSessionStore() {
            public void clear() { set(null); }
            public String get() { return dbxConfig.client_id; }
            public void set(String s) {
            }
        };
    }
}
