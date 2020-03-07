package org.onedatashare.server.controller.endpoint;

import org.onedatashare.server.model.core.Stat;
import org.onedatashare.server.model.request.OperationRequestData;
import org.onedatashare.server.model.request.RequestData;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.service.DbxService;
import org.onedatashare.server.service.oauth.DbxOauthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Controller
@RequestMapping("/api/dropbox")
public class DbxController extends OAuthEndpointBaseController{
    @Autowired
    private DbxService dbxService;

    @Autowired
    private DbxOauthService dbxOauthService;


    @GetMapping("/a")
    public Rendering oauth(){
        return Rendering.redirectTo(dbxOauthService.start()).build();
    }

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
        return this.redirectTo(dbxOauthService.start());
    }

    @Override
    protected Rendering completeOauthOperation(Map<String, String> queryParameters) {
        return null;
    }
}