package org.onedatashare.server.controller.endpoint;

import org.onedatashare.server.model.core.Stat;
import org.onedatashare.server.model.error.UnsupportedOperationException;
import org.onedatashare.server.model.request.OperationRequestData;
import org.onedatashare.server.model.request.RequestData;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.service.GridftpService;
import org.onedatashare.server.service.oauth.GridftpAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Controller
@RequestMapping("/api/gridftp")
public class GridFtpController extends OAuthEndpointBaseController{
    @Autowired
    private GridftpService gridftpService;

    @Autowired
    private GridftpAuthService gridftpAuthService;

    @Override
    protected Mono<Stat> listOperation(RequestData requestData) {
        UserAction userAction = UserAction.convertToUserAction(requestData);
        return gridftpService.list(null, userAction).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<ResponseEntity> mkdirOperation(OperationRequestData operationRequestData) {
        UserAction userAction = UserAction.convertToUserAction(operationRequestData);
        return gridftpService.mkdir(null, userAction).map(this::returnOnSuccess).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<ResponseEntity> deleteOperation(OperationRequestData operationRequestData) {
        UserAction userAction = UserAction.convertToUserAction(operationRequestData);
        return gridftpService.delete(null, userAction).map(this::returnOnSuccess).subscribeOn(Schedulers.elastic());
    }

    @Override
    protected Mono<Stat> uploadOperation() {
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    protected Mono<String> downloadOperation(RequestData requestData){
        return Mono.error(new UnsupportedOperationException());
    }

    @Override
    protected Rendering initiateOauthOperation() {
        return this.redirectTo(gridftpAuthService.start());
    }

    @Override
    protected Rendering completeOauthOperation(Map<String, String> queryParameters) {
        return null;
    }
}