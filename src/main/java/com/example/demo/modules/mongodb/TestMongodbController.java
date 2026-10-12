package com.example.demo.modules.mongodb;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/mongodb")
public class TestMongodbController {
    private final MongoTemplate mongoTemplate;

    @RequestMapping("/test")
    public String test() {
        FooBar fooBar = new FooBar();
        fooBar.setName("test");
        fooBar.setDescription("test description");
        mongoTemplate.save(fooBar);
        return "test";
    }

}
