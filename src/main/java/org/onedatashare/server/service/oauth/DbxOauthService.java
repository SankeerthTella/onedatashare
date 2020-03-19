package org.onedatashare.server.service.oauth;

import com.dropbox.core.*;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.users.FullAccount;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
    final String key;
    final String secret;
    final String redirectUri;
    final String identifier;
}


@Service
public class DbxOauthService implements OAuthService  {
    @Autowired
    private DbxConfig dbxConfig;

    private DbxAppInfo secrets;
    private DbxRequestConfig config;
    private DbxSessionStore sessionStore;
    private DbxWebAuth auth;
    private String token = null;

    private static final String STATE = "state", CODE = "code";

    @PostConstruct
    private void postConstructInit(){
        secrets = new DbxAppInfo(dbxConfig.key, dbxConfig.secret);
        config = DbxRequestConfig.newBuilder(dbxConfig.identifier).build();
        sessionStore = new DbxSessionStore() {
            public void clear() { set(null); }
            public String get() { return token; }
            public void set(String s) {
                token = s;
            }
        };

        auth = new DbxWebAuth(config, secrets);
    }

    public String start(){
        return auth.authorize(DbxWebAuth
                .Request
                .newBuilder()
                .withRedirectUri(dbxConfig.redirectUri, sessionStore)
                .build());
    }

    public OAuthCredential finish(Map<String, String> queryParameters) {
        Map<String,String[]> map = new HashMap();
        map.put(STATE, new String[] {queryParameters.get(STATE)});
        map.put(CODE, new String[] {queryParameters.get(CODE)});
        try {
            DbxAuthFinish finish = auth.finishFromRedirect(dbxConfig.redirectUri, sessionStore, map);
            OAuthCredential cred = new OAuthCredential(finish.getAccessToken());
            FullAccount account = new DbxClientV2(config, finish.getAccessToken()).users().getCurrentAccount();
            cred.name = "Dropbox: " + account.getEmail();
            cred.dropboxID = account.getAccountId();
            return cred;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}