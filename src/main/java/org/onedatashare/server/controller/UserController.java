package org.onedatashare.server.controller;

import org.onedatashare.server.model.core.ODSConstants;
import org.onedatashare.server.model.error.ForbiddenAction;
import org.onedatashare.server.model.error.InvalidFieldException;
import org.onedatashare.server.model.error.NotFoundException;
import org.onedatashare.server.model.error.OldPwdMatchingException;
import org.onedatashare.server.model.request.UserRequestData;
import org.onedatashare.server.model.useraction.UserAction;
import org.onedatashare.server.service.ODSLoggerService;
import org.onedatashare.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for handling GET requests to User DB
 */
@RestController
@RequestMapping("/api/stork/user")
public class UserController {

    @Autowired
    private UserService userService;

    final int TIMEOUT_IN_MINUTES = 1440;

    /**
     * Handler for user information/ perference requests
     * @param headers - Incoming request headers
     * @param userRequestData - Data needed to make a user request
     * @return Object
     */
    @PostMapping
    public Object performAction(@RequestHeader HttpHeaders headers, @RequestBody UserRequestData userRequestData) {
        String cookie = headers.getFirst(ODSConstants.COOKIE);
        UserAction userAction = UserAction.convertToUserAction(userRequestData);
        switch(userAction.getAction()) {
            case "history":
                return userService.saveHistory(userAction.getUri(), cookie);
            case "getUser":
                return userService.getUserFromCookie(userAction.getEmail(), cookie);
            case "updateSaveOAuth":
                return userService.updateSaveOAuth(cookie, userAction.isSaveOAuth());
            case "deleteCredential":
                return userService.deleteCredential(cookie, userAction.getUuid());
            case "deleteHistory":
                return userService.deleteHistory(cookie, userAction.getUri());
            case "updateViewPreference":
                return userService.updateViewPreference(userAction.getEmail(), userAction.isCompactViewEnabled());
            default:
                return Mono.empty();
        }
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<NotFoundException> handle(NotFoundException notfound) {
        return new ResponseEntity<>(notfound, notfound.status);
    }

    @GetMapping
    public Object getHistory(@RequestHeader HttpHeaders headers) {
        return userService.getHistory(headers.getFirst(ODSConstants.COOKIE));
    }

    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<InvalidFieldException> handle(InvalidFieldException invf){
        ODSLoggerService.logError(invf.getMessage());
        return new ResponseEntity<>(invf, invf.status);
    }

    @ExceptionHandler(ForbiddenAction.class)
    public ResponseEntity<ForbiddenAction> handle(ForbiddenAction fa){
        ODSLoggerService.logError(fa.getMessage());
        return new ResponseEntity<>(fa, fa.status);
    }

    @ExceptionHandler(OldPwdMatchingException.class)
    public ResponseEntity<OldPwdMatchingException> handle(OldPwdMatchingException oe){
        ODSLoggerService.logError(oe.getMessage());
        return new ResponseEntity<>(oe, oe.status);
    }
}
