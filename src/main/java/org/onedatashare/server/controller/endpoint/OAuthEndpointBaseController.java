package org.onedatashare.server.controller.endpoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;

public abstract class OAuthEndpointBaseController extends EndpointBaseController{

    @GetMapping("/initiate-oauth")
    public Rendering initiateOauth(){
        return initiateOauthOperation();
    }

    @GetMapping("/complete-oauth")
    public Rendering completeOauth(@RequestParam Map<String, String> queryParameters){
        return completeOauthOperation(queryParameters);
    }

    protected abstract Rendering initiateOauthOperation();

    protected abstract Rendering completeOauthOperation(Map<String, String> queryParameters);

    protected Rendering redirectTo(String url){
        return Rendering.redirectTo(url).build();
    }
}
