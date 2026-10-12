package com.example.demo.modules.jasypt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Jasypt 加解密示例 Controller。
 * <p>
 * 注入 jasypt-spring-boot-starter 自动配置的 StringEncryptor，
 * 提供 HTTP 接口进行加密/解密操作。
 * <p>
 * 示例请求：
 * <pre>
 *   GET /jasypt/encrypt?plainText=mySecretValue
 *   GET /jasypt/decrypt?cipherText=ENC(xxxx)
 * </pre>
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/jasypt")
public class JasyptController {

    /**
     * jasypt-spring-boot-starter 自动注入的加密器，
     * 默认 bean 名称为 "jasyptStringEncryptor"
     */
    private final StringEncryptor jasyptStringEncryptor;

    /**
     * 加密接口：传入明文，返回 ENC(...) 格式的密文
     */
    @GetMapping("/encrypt")
    public Map<String, String> encrypt(@RequestParam String plainText) {
        String encrypted = "ENC(" + jasyptStringEncryptor.encrypt(plainText) + ")";
        log.info("Jasypt 加密: {} -> {}", plainText, encrypted);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("plainText", plainText);
        result.put("encryptedText", encrypted);
        return result;
    }

    /**
     * 解密接口：传入 ENC(...) 格式密文或纯密文，返回明文
     */
    @GetMapping("/decrypt")
    public Map<String, String> decrypt(@RequestParam String cipherText) {
        String raw = cipherText;
        if (raw.startsWith("ENC(") && raw.endsWith(")")) {
            raw = raw.substring(4, raw.length() - 1);
        }
        String decrypted = jasyptStringEncryptor.decrypt(raw);
        log.info("Jasypt 解密: {} -> {}", cipherText, decrypted);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("cipherText", cipherText);
        result.put("plainText", decrypted);
        return result;
    }
}
