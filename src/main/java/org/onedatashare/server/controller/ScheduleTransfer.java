package org.onedatashare.server.controller;

import org.onedatashare.server.model.core.Job;
import org.onedatashare.server.model.request.TransferRequest;
import org.onedatashare.server.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Contoller for handling file/folder transfer requests
 */
@RestController
@RequestMapping("/api/stork/submit")
public class ScheduleTransfer {

    @Autowired
    private TransferService transferService;

    /**
     * Handler for POST requests of transfers
     * @param transferRequest - Request data with transfer information
     * @return Mono\<Job\>
     */
    @PostMapping
    public Mono<Job> schedule(@RequestBody TransferRequest transferRequest) {
        return transferService.submit(transferRequest);
    }
}