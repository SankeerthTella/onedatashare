package org.onedatashare.server.controller.endpoint;

import org.onedatashare.server.model.core.Stat;
import org.onedatashare.server.model.request.OperationRequestData;
import org.onedatashare.server.model.request.RequestData;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.service.DbxService;
import org.onedatashare.server.service.ODSLoggerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Controller
@RequestMapping("/api/dropbox")
public class DbxController extends OAuthEndpointBaseController{
    @Autowired
    private DbxService dbxService;

    @Override
    protected Mono<Stat> listOperation(RequestData requestData) {
        UserAction userAction = UserAction.convertToUserAction(requestData);
        return dbxService.list(null, userAction).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<ResponseEntity> mkdirOperation(OperationRequestData operationRequestData) {
        UserAction userAction = UserAction.convertToUserAction(operationRequestData);
        return dbxService.mkdir(null, userAction).map(this::returnOnSuccess).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<ResponseEntity> deleteOperation(OperationRequestData operationRequestData) {
        UserAction userAction = UserAction.convertToUserAction(operationRequestData);
        return dbxService.delete(null, userAction).map(this::returnOnSuccess).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<Stat> uploadOperation() {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    protected Mono<String> downloadOperation(RequestData requestData){
        UserAction userAction = UserAction.convertToUserAction(requestData);
        return dbxService.download(null, userAction).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Rendering initiateOauthOperation() {
        return Rendering.redirectTo(dbxService.getOAuthUrl2()).build();
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

    @Override
    protected Mono<Rendering> completeOauthOperation(Map<String, String> queryParameters) {
        String code = queryParameters.get("code");
        if (code == null) {
            return handleOAuthError("", queryParameters.getOrDefault("error_description", "Unknown error"));
        }
        else {
            return dbxService.completeOAuth(code).map(this::redirectTo);
        }
    }
}