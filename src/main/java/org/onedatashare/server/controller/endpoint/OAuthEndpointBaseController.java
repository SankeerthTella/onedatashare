package org.onedatashare.server.controller.endpoint;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class OAuthEndpointBaseController extends EndpointBaseController{

    @GetMapping(value = "/initiate-oauth")
    public Rendering initiateOauth(){
        return initiateOauthOperation();
    }

    @GetMapping(value = "/complete-oauth")
    public Mono<Rendering> completeOauth(@RequestParam Map<String, String> queryParameters){
        return completeOauthOperation(queryParameters);
    }

    protected abstract Rendering initiateOauthOperation();

    protected abstract Mono<Rendering> completeOauthOperation(Map<String, String> queryParameters);

    protected Rendering redirectTo(String url){
        return Rendering.redirectTo(url).build();
    }
}
