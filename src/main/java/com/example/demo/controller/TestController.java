package com.example.demo.controller;

import com.example.demo.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@Slf4j
@RestController
public class TestController {
    @Resource
    private TestService testService;

    @GetMapping("/test")
    public void test() {
        testService.test();
    }

    @GetMapping("/test2")
    public String test2() {
        log.info("test2...");
        return "test2!!!";
    }
}
