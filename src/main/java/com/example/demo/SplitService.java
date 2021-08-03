package com.example.demo;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface SplitService {

    String getResponse(String userName);
    long getTimeDelay(String userName) throws JsonProcessingException;
    void doTrack(String userName, long methodDurationMillis);
}
