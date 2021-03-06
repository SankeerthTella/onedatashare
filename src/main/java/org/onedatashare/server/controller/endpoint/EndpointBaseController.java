/**
 ##**************************************************************
 ##
 ## Copyright (C) 2018-2020, OneDataShare Team, 
 ## Department of Computer Science and Engineering,
 ## University at Buffalo, Buffalo, NY, 14260.
 ## 
 ## Licensed under the Apache License, Version 2.0 (the "License"); you
 ## may not use this file except in compliance with the License.  You may
 ## obtain a copy of the License at
 ## 
 ##    http://www.apache.org/licenses/LICENSE-2.0
 ## 
 ## Unless required by applicable law or agreed to in writing, software
 ## distributed under the License is distributed on an "AS IS" BASIS,
 ## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ## See the License for the specific language governing permissions and
 ## limitations under the License.
 ##
 ##**************************************************************
 */


package org.onedatashare.server.controller.endpoint;

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
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

public abstract class EndpointBaseController {

    static final ResponseEntity successResponse = new ResponseEntity("Success", HttpStatus.OK);

    @GetMapping("/oauth")
    public Rendering oauth(@RequestBody RequestData requestData){
        return oauthOperation();
    }

    @PostMapping("/ls")
    public Mono<Stat> list(@RequestBody RequestData requestData){
        return listOperation(requestData);
    }

    @PostMapping("/mkdir")
    public Mono<ResponseEntity> mkdir(@RequestBody OperationRequestData operationRequestData){
        return mkdirOperation(operationRequestData);
    }

    @PostMapping("/rm")
    public Mono<ResponseEntity> delete(@RequestBody OperationRequestData operationRequestData){
        return deleteOperation(operationRequestData);
    }

    @PostMapping("/upload")
    public Mono<Stat> upload(){
        return uploadOperation();
    }

    @PostMapping("/download")
    public Mono download(@RequestBody RequestData requestData){
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
    protected abstract Rendering oauthOperation();

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