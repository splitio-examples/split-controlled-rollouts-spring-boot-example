package com.example.demo.controller;

import com.example.demo.controller.DemoController;
import io.split.client.SplitClient;
import io.split.client.api.SplitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@RestController
public class TriggerAlertController {

    // treatment name pulled from application.properties
    @Value("#{ @environment['split.api.treatement-name'] }")
    private String treatmentName;

    Logger logger = LoggerFactory.getLogger(DemoController.class);

    SplitClient splitClient;

    public TriggerAlertController(SplitClient splitClient) {
        this.splitClient = splitClient;
    }

    @GetMapping("trigger-alert")
    public String triggerAlert() {

        for (int i=0; i<1000; i++) {

            String uuid = UUID.randomUUID().toString();
            SplitResult result = splitClient.getTreatmentWithConfig(uuid, treatmentName);
            String treatment = result.treatment();

            int simulatedDelay = 0;

            // set the boolean flag according to the treatment state
            if (treatment.equals("on")) {
                simulatedDelay = ThreadLocalRandom.current().nextInt(5000, 5500 + 1);
            } else if (treatment.equals("off")) {
                simulatedDelay = ThreadLocalRandom.current().nextInt(10, 50 + 1);
            } else {
                throw new RuntimeException("Couldn't retrieve treatment from Split.");
            }

            splitClient.track(uuid, "user", "home_method_time_millis", simulatedDelay);

            logger.info("(" + i + ") treatment = " + treatment + ", simulatedDelay = " + simulatedDelay);

        }

        return "Done";
    }

}
