package org.onedatashare.server.controller;

import org.onedatashare.server.model.core.Job;
import org.onedatashare.server.model.request.JobActionRequest;
import org.onedatashare.server.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller that deletes a Job from the queue page
 */
@RestController
@RequestMapping("/api/stork/deleteJob")
public class DeleteJobController {

    @Autowired
    private TransferService transferService;

    /**
     * Handler for the request for deleting a Job on the queue page.
     *
     * @param jobActionRequest - Data to perform an operation on the Job
     * @return a map containing all the endpoint credentials linked to the user account as a Mono
     */
    @PostMapping
    public Mono<Job> restartJob(@RequestBody JobActionRequest jobActionRequest){
        return transferService.deleteJob(jobActionRequest.getJobUUID());
    }
}