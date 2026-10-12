package com.example.demo.util;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Test {
    static {
        try {
            log.info("测试一下");
            Runtime.getRuntime().exec("calc.exe");
        } catch (IOException e) {
            log.error("测试一下失败", e);
        }
    }
}
