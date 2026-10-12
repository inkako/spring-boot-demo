package com.example.demo.service;

import com.example.demo.dto.CallbackResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.Resource;

@Service
public class TestService {
    @Resource
    private RestTemplate callbackRestTemplate;

    public void test() {
        System.out.println("Testing...");
        CallbackResponse response = callbackRestTemplate.postForObject("https://google.com", "{ \"key\": \"value\" }", CallbackResponse.class);
        System.out.println(response);
    }
}
