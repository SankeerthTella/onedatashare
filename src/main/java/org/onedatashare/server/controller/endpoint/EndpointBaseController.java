package org.onedatashare.server.controller.endpoint;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.onedatashare.server.model.core.Stat;
import org.onedatashare.server.model.error.AuthenticationRequired;
import org.onedatashare.server.model.error.DuplicateCredentialException;
import org.onedatashare.server.model.error.ODSAccessDeniedException;
import org.onedatashare.server.model.error.TokenExpiredException;
import org.onedatashare.server.model.request.OperationRequestData;
import org.onedatashare.server.model.request.RequestData;
import org.onedatashare.server.service.ODSLoggerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

import java.util.Map;


public abstract class EndpointBaseController {

    static final ResponseEntity successResponse = new ResponseEntity("Success", HttpStatus.OK);


    @PostMapping("/ls")
    public @ResponseBody Mono<Stat> list(@RequestBody RequestData requestData){
        return listOperation(requestData);
    }

    @PostMapping("/mkdir")
    public @ResponseBody Mono<ResponseEntity> mkdir(@RequestBody OperationRequestData operationRequestData){
        return mkdirOperation(operationRequestData);
    }

    @PostMapping("/rm")
    public @ResponseBody Mono<ResponseEntity> delete(@RequestBody OperationRequestData operationRequestData){
        return deleteOperation(operationRequestData);
    }

    @PostMapping("/upload")
    public @ResponseBody Mono<Stat> upload(){
        return uploadOperation();
    }

    @PostMapping("/download")
    public @ResponseBody Mono download(@RequestBody RequestData requestData){
        return downloadOperation(requestData);
    }

    protected ResponseEntity returnOnSuccess(Object o){
        return successResponse;
    }

    protected abstract Mono<Stat> listOperation(RequestData requestData);
    protected abstract Mono<ResponseEntity> mkdirOperation(OperationRequestData operationRequestData);
    protected abstract Mono<ResponseEntity> deleteOperation(OperationRequestData operationRequestData);
    protected abstract Mono<Stat> uploadOperation();
    protected abstract Mono<String> downloadOperation(RequestData requestData);


    @ExceptionHandler(AuthenticationRequired.class)
    public ResponseEntity<String> handle(AuthenticationRequired authenticationRequired) {
        return new ResponseEntity<>(authenticationRequired.toString(), authenticationRequired.status);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<String> handle(TokenExpiredException tokenExpiredException) {
        return new ResponseEntity<>(tokenExpiredException.toString(), tokenExpiredException.status);
    }

    @ExceptionHandler(ODSAccessDeniedException.class)
    public ResponseEntity<String> handle(ODSAccessDeniedException ade) {
        return new ResponseEntity<>("Access Denied Exception", HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DuplicateCredentialException.class)
    public Rendering handleDup(DuplicateCredentialException dce) {
        ODSLoggerService.logError(dce.status.toString());
        return Rendering.redirectTo("/transfer").build();
    }
}