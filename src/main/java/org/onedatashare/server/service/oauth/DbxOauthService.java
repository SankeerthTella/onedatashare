package org.onedatashare.server.service.oauth;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "dropbox")
@ConstructorBinding
@AllArgsConstructor
@Getter
class DbxConfig {
    final String clientId;
    final String clientSecret;
    final String redirectUri;
    final String identifier;
}


@Service
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DbxOauthService  {
    @Autowired
    private DbxConfig dbxConfig;

    private DbxAppInfo secrets;
    private DbxRequestConfig config;
    private DbxSessionStore sessionStore;
    private DbxWebAuth auth;

    private String oAuthUrl;

    @PostConstruct
    private void postConstructInit(){
        secrets = new DbxAppInfo(dbxConfig.clientId, dbxConfig.clientSecret);
        config = DbxRequestConfig.newBuilder(dbxConfig.identifier).build();
        sessionStore = new DbxSessionStore() {
            public void clear() { set(null); }
            public String get() { return dbxConfig.clientId; }
            public void set(String s) {
            }
        };

        auth = new DbxWebAuth(config, secrets);
        // Authorize the DbxWebAuth auth as well as redirect the user to the finishURI, done this way to appease OAuth 2.0
        oAuthUrl = auth.authorize(DbxWebAuth
                .Request
                .newBuilder()
                .withRedirectUri(dbxConfig.redirectUri, sessionStore)
                .build());
    }

    public String start(){
        return oAuthUrl;
    }

    public Mono<OAuthCredential> finish(String token) {
        Map<String,String[]> map = new HashMap();
        map.put("state", new String[] {dbxConfig.clientId});
        map.put("code", new String[] {token});
        try {
            DbxAuthFinish finish = auth.finishFromRedirect(dbxConfig.redirectUri, sessionStore, map);
            OAuthCredential cred = new OAuthCredential(finish.getAccessToken());
            FullAccount account = new DbxClientV2(config, finish.getAccessToken()).users().getCurrentAccount();
            cred.name = "Dropbox: " + account.getEmail();
            cred.dropboxID = account.getAccountId();
            return Mono.just(cred);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}