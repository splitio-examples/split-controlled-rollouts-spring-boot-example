package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class DemoController {

    Logger logger = LoggerFactory.getLogger(DemoController.class);

    SplitService splitService;

    public DemoController(SplitService splitService) {
        this.splitService = splitService;
    }


    @GetMapping("/")
    public String home(Principal principal) throws InterruptedException, JsonProcessingException {

        // start time for performance tracking
        long startTime = System.nanoTime();

        // get the user name (you use this to retrieve the split treatment)
        String userName = principal.getName();
        logger.info("Authenticated user " + userName + " : " + principal.toString());

        // get the millis delay value from the treatment configuration
        long timeDelay = splitService.getTimeDelay(userName);

        // does the configuration specify a delay?
        if (timeDelay > 0) {
            logger.info("Threat.sleeping for " + timeDelay + " milliseconds");
            Thread.sleep(timeDelay);
        }

        // build the response based on the treatment values
        String response = splitService.getResponse(userName);

        // stop time for performance measuring
        long stopTime = System.nanoTime();

        // calculate millis execution time
        long methodDurationMillis = (stopTime - startTime) / 1000000;

        logger.info("methodDurationMillis = " + methodDurationMillis);

        splitService.doTrack(userName, methodDurationMillis);

        return response;
    }

}
