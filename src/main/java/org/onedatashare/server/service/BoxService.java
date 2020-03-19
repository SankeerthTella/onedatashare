package org.onedatashare.server.service;


import org.onedatashare.server.model.core.*;
import org.onedatashare.server.model.credential.OAuthCredential;
import org.onedatashare.server.model.error.AuthenticationRequired;
import org.onedatashare.server.model.error.NotFoundException;
import org.onedatashare.server.model.error.TokenExpiredException;
import org.onedatashare.server.model.useraction.IdMap;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.module.box.BoxResource;
import org.onedatashare.server.module.box.BoxSession;
import org.onedatashare.server.service.oauth.BoxOauthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@Service
public class BoxService extends OAuthResourceService {

    @Autowired
    private UserService userService;

    @Autowired
    private BoxOauthService boxOauthService;

    @Override
    public Mono<Stat> list(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction).flatMap(BoxResource::stat);
    }

    @Override
    public Mono<Boolean> mkdir(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::mkdir)
                .map(r -> true);
    }

    @Override
    public Mono<Boolean> delete(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::delete)
                .map(val -> true);
    }

    @Override
    public Mono<String> download(String cookie, UserAction userAction) {
        return getBoxResourceUserActionUri(cookie, userAction)
                .flatMap(BoxResource::download);
    }

    public Mono<BoxResource> getBoxResourceUserActionUri(String cookie, UserAction userAction) {
        final String path = pathFromUri(userAction.getUri());
        String id = userAction.getId();
        ArrayList<IdMap> idMap = userAction.getMap();
        if (userAction.getCredential().isTokenSaved()) {
            return userService.getLoggedInUser(cookie)
                    .handle((usr, sink) -> {
                        this.fetchCredentialsFromUserAction(usr, sink, userAction);
                    })
                    .map(credential -> new BoxSession(URI.create(userAction.getUri()), (Credential) credential))
                    .flatMap(BoxSession::initialize)
                    .flatMap(boxSession -> boxSession.select(path, id, idMap))
                    .onErrorResume(throwable -> throwable instanceof TokenExpiredException, throwable -> {
                        userService.deleteBoxCredential(cookie, userAction.getCredential(), ((TokenExpiredException) throwable).cred).subscribe();
                        return null;
                    });
        } else {
            return Mono.just(new OAuthCredential(userAction.getCredential().getToken()))
                    .map(oAuthCred -> new BoxSession(URI.create(userAction.getUri()), oAuthCred))
                    .flatMap(BoxSession::initializeNotSaved)
                    .flatMap(boxSession -> boxSession.select(path, id, idMap));
        }
    }

    public String pathFromUri(String uri) {
        String path = "";
        if(uri.contains(ODSConstants.BOX_URI_SCHEME)){
            path = uri.substring(ODSConstants.BOX_URI_SCHEME.length() - 1);
        }
        try {
            path = java.net.URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return path;
    }

    @Override
    public Mono<String> getOAuthUrl() {
        return Mono.fromSupplier(() -> boxOauthService.start());
    }

    @Override
    public Mono<String> completeOAuth(Map<String, String> queryParameters) {
        return Mono.fromSupplier(() -> boxOauthService.finish(queryParameters))
                .flatMap(oauthCred -> userService.saveCredential(oauthCred))
                .map(uuid -> "/oauth/uuid?identifier=" + uuid)
                .switchIfEmpty(Mono.just("/oauth/ExistingCredBox"));
    }
}

