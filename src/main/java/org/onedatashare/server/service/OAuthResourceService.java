package org.onedatashare.server.service;

import reactor.core.publisher.Mono;

public abstract class OAuthResourceService extends ResourceService{
    public abstract Mono<String> getOAuthUrl();
    public abstract Mono<String> completeOAuth(String token);
}
