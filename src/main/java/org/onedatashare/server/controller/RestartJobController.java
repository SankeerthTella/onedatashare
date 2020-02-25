package org.onedatashare.server.controller;

import org.onedatashare.server.model.core.Job;
import org.onedatashare.server.model.request.JobActionRequest;
import org.onedatashare.server.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller for handling Restart Job Requests
 */
@RestController
@RequestMapping("/api/stork/restart")
public class RestartJobController {

    @Autowired
    private TransferService transferService;

    /**
     * Handler for requests of restart Job
     * @param jobActionRequest - Request data with Job details
     * @return Mono\<Job\>
     */
    @PostMapping
    public Mono<Job> restartJob(@RequestBody JobActionRequest jobActionRequest){
        return transferService.restartJob(jobActionRequest);
    }
}
