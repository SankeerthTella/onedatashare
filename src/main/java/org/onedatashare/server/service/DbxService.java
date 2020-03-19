package org.onedatashare.server.service;

import org.onedatashare.server.model.core.*;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.model.useraction.UserActionResource;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.module.dropbox.DbxResource;
import org.onedatashare.server.module.dropbox.DbxSession;
import org.onedatashare.server.service.oauth.DbxOauthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Service
public class DbxService extends OAuthResourceService{

    @Autowired
    private UserService userService;

    @Autowired
    private DbxOauthService dbxOauthService;

    public Mono<DbxResource> getDbxResourceWithUserActionUri(String cookie, UserAction userAction) {
        if(userAction.getCredential().isTokenSaved()) {
            return userService.getLoggedInUser(cookie)
                    .handle((usr, sink) ->{
                        this.fetchCredentialsFromUserAction(usr, sink, userAction);
                    })
                    .map(credential -> new DbxSession(URI.create(userAction.getUri()), (Credential) credential))
                    .flatMap(DbxSession::initialize)
                    .flatMap(dbxSession -> dbxSession.select(pathFromDbxUri(userAction.getUri())));
        }
        else{
            return Mono.just(new OAuthCredential(userAction.getCredential().getToken()))
                    .map(oAuthCred -> new DbxSession(URI.create(userAction.getUri()), oAuthCred))
                    .flatMap(DbxSession::initialize)
                    .flatMap(dbxSession -> {
                        String path = pathFromDbxUri(userAction.getUri());
                        return dbxSession.select(path);
                    });
        }
    }

    public Mono<DbxResource> getDbxResourceWithJobSourceOrDestination(String cookie, UserActionResource userActionResource) {
        final String path = pathFromDbxUri(userActionResource.getUri());
        return userService.getLoggedInUser(cookie)
                .map(User::getCredentials)
                .map(uuidCredentialMap ->
                        uuidCredentialMap.get(UUID.fromString(userActionResource.getCredential().getUuid())))
                .map(credential -> new DbxSession(URI.create(userActionResource.getUri()), credential))
                .flatMap(DbxSession::initialize)
                .flatMap(dbxSession -> dbxSession.select(path));
    }

    public String pathFromDbxUri(String uri) {
        String path = "";
        if(uri.contains(ODSConstants.DROPBOX_URI_SCHEME)){
            path = uri.substring(ODSConstants.DROPBOX_URI_SCHEME.length() - 1);
        }
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getDbxResourceWithUserActionUri(cookie, userAction).flatMap(DbxResource::stat);
    }

    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getDbxResourceWithUserActionUri(cookie, userAction)
                .flatMap(DbxResource::mkdir)
                .map(r -> true);
    }

    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getDbxResourceWithUserActionUri(cookie, userAction)
                .flatMap(DbxResource::delete)
                .map(val -> true);
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getDbxResourceWithUserActionUri(cookie,userAction)
                .flatMap(DbxResource::generateDownloadLink).subscribeOn(Schedulers.elastic());
    }

    @Override
    public Mono<String> getOAuthUrl() {
        return Mono.fromSupplier(() -> dbxOauthService.start());
    }

    public Mono<String> completeOAuth(Map<String, String> queryParameters) {
        return dbxOauthService.finish(queryParameters)
            .flatMap(oauthCred -> userService.saveCredential(oauthCred))
            .map(uuid -> "/oauth/uuid?identifier=" + uuid)
            .switchIfEmpty(Mono.just("/oauth/ExistingCredDropbox"));
    }
}
