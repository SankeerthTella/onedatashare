package org.onedatashare.server.controller.endpoint;

import org.onedatashare.server.service.ODSLoggerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public abstract class OAuthEndpointBaseController extends EndpointBaseController{

    @GetMapping(value = "/initiate-oauth")
    public Mono<Rendering> initiateOauth(){
        return initiateOauthOperation();
    }

    @GetMapping(value = "/complete-oauth")
    public Mono<Rendering> completeOauth(@RequestParam Map<String, String> queryParameters){
        return completeOauthOperation(queryParameters);
    }

    protected abstract Mono<Rendering> initiateOauthOperation();

    protected abstract Mono<Rendering> completeOauthOperation(Map<String, String> queryParameters);

    protected Rendering redirectTo(String url){
        return Rendering.redirectTo(url).build();
    }

    protected Mono<Rendering> handleOAuthError(String type, String errorDescription){
        return Mono.fromSupplier(() -> {
            StringBuilder errorStringBuilder = new StringBuilder();
            try{
                errorStringBuilder.append(URLEncoder.encode(errorDescription, "UTF-8"));
                errorStringBuilder.insert(0, "?error=");
            } catch (UnsupportedEncodingException e) {
                ODSLoggerService.logError(errorDescription);
            }
            return Rendering.redirectTo("/transfer" + errorStringBuilder.toString()).build();
        });
    }
}
