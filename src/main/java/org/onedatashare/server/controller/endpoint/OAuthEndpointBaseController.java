package org.onedatashare.server.controller.endpoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.onedatashare.server.model.request.RequestData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class OAuthEndpointBaseController extends EndpointBaseController{
    @Data
    @AllArgsConstructor
    public class OAuthResponse {
        private String uri;
    }


    @GetMapping("/initiate-oauth")
    public Mono<OAuthResponse> initiateOauth(@RequestBody RequestData requestData){
        return initiateOauthOperation();
    }

    @GetMapping("/complete-oauth")
    public Mono<Rendering> completeOauth(@RequestParam Map<String, String> queryParameters){
        return completeOauthOperation(queryParameters);
    }

    protected abstract Mono<OAuthResponse> initiateOauthOperation();

    protected abstract Mono<Rendering> completeOauthOperation(Map<String, String> queryParameters);
}
