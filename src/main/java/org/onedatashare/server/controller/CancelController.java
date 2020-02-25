package org.onedatashare.server.controller;

import org.onedatashare.server.model.core.Job;
import org.onedatashare.server.model.request.JobActionRequest;
import org.onedatashare.server.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller that captures request to cancel a transfer that is in progress.
 * Invoked when user clicks the cancel button on the queue page (or admin clicks on history page)
 */
@RestController
@RequestMapping("/api/stork/cancel")
public class CancelController {

    @Autowired
    private TransferService transferService;

    /**
     * Handler that invokes the service to cancel an ongoing job.
     * @param jobActionRequest - Model containing the job ID of the transfer job to be stopped
     * @return Object - Mono of job that was stopped
     */
    @PostMapping
    public Mono<Job> cancel(@RequestBody JobActionRequest jobActionRequest) {
        return transferService.cancel(jobActionRequest.getJobUUID());
    }
}
