package com.example.demo.service;

import com.example.demo.controller.DemoController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.split.client.SplitClient;
import io.split.client.api.SplitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SplitServiceImpl implements SplitService {

    SplitClient splitClient;
    ObjectMapper mapper;

    Logger logger = LoggerFactory.getLogger(DemoController.class);

    // treatment name pulled from application.properties
    @Value("#{ @environment['split.api.treatement-name'] }")
    private String treatmentName;

    public SplitServiceImpl(SplitClient splitClient, ObjectMapper mapper) {
        this.splitClient = splitClient;
        this.mapper = mapper;
    }

    @Override
    public long getTimeDelay(String userName) throws JsonProcessingException {
        SplitResult result = getResult(userName);
        long timeDelay = 0;
        if (null != result.config()) {
            TimeDelayConfig config = mapper.readValue(result.config(), TimeDelayConfig.class);
            logger.info("timeDelay = " + config.timedelay);
            timeDelay = config.timedelay;
        }
        return timeDelay;
    }

    @Override
    public String getResponse(String userName) {
        SplitResult result = getResult(userName);
        String treatment = result.treatment();
        String response = "Hey, " + userName;
        if ("on".equals(treatment)) {
            logger.info("Treatment " + treatmentName + " is ON");
            String responseBody = WebClient
                    .create("https://api.github.com/zen")
                    .get()
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            response = "Hey, " + userName + ", GitHub says: " + responseBody;
        } else if ("off".equals(treatment)) {
            logger.info("Treatment " + treatmentName + " is OFF");
        } else {
            logger.debug("Unexpected treatment result: " + treatment);
        }
        return response;
    }

    @Override
    public void doTrack(String userName, long methodDurationMillis) {
        // track the performance metric
        splitClient.track(userName, "user", "home_method_time_millis", methodDurationMillis);
    }

    private SplitResult getResult(String userName) {
        return splitClient.getTreatmentWithConfig(userName, treatmentName);
    }

    // simple inner class for unpacking our treatment config
    static class TimeDelayConfig {
        private int timedelay;

        public int getTimedelay() {
            return timedelay;
        }

        public void setTimedelay(int timedelay) {
            this.timedelay = timedelay;
        }
    }
}
