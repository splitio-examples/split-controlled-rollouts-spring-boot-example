package com.example.demo;

import io.split.client.SplitClient;
import io.split.client.api.SplitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import split.com.google.gson.Gson;

import java.security.Principal;

@RestController
public class DemoController {

    // simple inner class for unpacking our treatment config
    class TimeDelayConfig {
        int timedelay;
    }

    Logger logger = LoggerFactory.getLogger(DemoController.class);

    SplitClient splitClient;

    public DemoController(SplitClient splitClient) {
        this.splitClient = splitClient;
    }

    // treatment name pulled from application.properties
    @Value("#{ @environment['split.api.treatement-name'] }")
    private String treatmentName;

    @GetMapping("/")
    public String home(Principal principal) {

        // start time for perofrmance tracking
        long startTime = System.nanoTime();

        // get the user name (you use this to retrieve the split treatment)
        String userName = principal.getName();
        logger.info("Authenticated user " + userName + " : " + principal.toString());

        // get the split treatment for the given authenticated user
        SplitResult result = splitClient.getTreatmentWithConfig(userName, treatmentName);
        String treatment = result.treatment();

        // our flag
        boolean getPithySayingFromGitHub;

        // set the boolean flag according to the treatment state
        if (treatment.equals("on")) {
            logger.info("Treatment " + treatmentName + " is ON");
            getPithySayingFromGitHub = true;
        } else if (treatment.equals("off")) {
            getPithySayingFromGitHub = false;
            logger.info("Treatment " + treatmentName + " is OFF");
        } else {
            throw new RuntimeException("Couldn't retrieve treatment from Split.");
        }

        // get the millis delay value from the treatment configuration
        long timeDelay = 0;
        if (null != result.config()) {
            Gson gson = new Gson();
            TimeDelayConfig config = gson.fromJson(result.config(), TimeDelayConfig.class);
            logger.info("timeDelay = " + config.timedelay);
            timeDelay = config.timedelay;
        }

        // build the response based on the treatment values

        // default response
        String response = "Hey, " + userName;

        // is this feature turned on in our treatment?
        if (getPithySayingFromGitHub) {
            String responseBody = WebClient
                    .create("https://api.github.com/zen")
                    .get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            response = "Hey, " + userName + ", GitHub says: " + responseBody;
        }

        // does the configuration specify a delay?
        if (timeDelay > 0) {
            try {
                logger.info("Threat.sleeping for " + timeDelay + " milliseconds");
                Thread.sleep(timeDelay);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // stop time for performance measuring
        long stopTime = System.nanoTime();

        // calculate millis execution time
        double methodDurationMillis = (double)(stopTime - startTime) / 1000000;

        logger.info("methodDurationMillis = " + methodDurationMillis);

        // track the performance metric
        splitClient.track(userName, "user", "home_method_time_millis", methodDurationMillis);

        return response;

    }

}
