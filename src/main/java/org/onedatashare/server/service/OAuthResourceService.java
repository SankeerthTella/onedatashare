package org.onedatashare.server.service;

import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class OAuthResourceService extends ResourceService{
    public abstract Mono<String> getOAuthUrl();
    public abstract Mono<String> completeOAuth(Map<String, String> queryParameters);
}
