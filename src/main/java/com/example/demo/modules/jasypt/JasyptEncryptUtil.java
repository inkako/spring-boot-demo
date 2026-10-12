package com.example.demo.modules.jasypt;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * Jasypt 加密/解密工具类。
 * <p>
 * 可直接通过 main 方法独立运行，生成 ENC(...) 格式的加密串，
 * 也可在应用中注入 StringEncryptor 使用。
 * <p>
 * 默认配置与 application-local.yml 中 jasypt.encryptor 保持一致：
 * <ul>
 *   <li>algorithm: PBEWithMD5AndDES</li>
 *   <li>password: jasypt</li>
 * </ul>
 */
public class JasyptEncryptUtil {

    private static final String DEFAULT_ALGORITHM = "PBEWithMD5AndDES";
    private static final String DEFAULT_PASSWORD = "jasypt";
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    /**
     * 使用默认配置加密
     */
    public static String encrypt(String plainText) {
        return encrypt(plainText, DEFAULT_PASSWORD, DEFAULT_ALGORITHM);
    }

    /**
     * 使用自定义密钥和算法加密
     */
    public static String encrypt(String plainText, String password, String algorithm) {
        PooledPBEStringEncryptor encryptor = createEncryptor(password, algorithm);
        return PREFIX + encryptor.encrypt(plainText) + SUFFIX;
    }

    /**
     * 使用默认配置解密（传入带 ENC() 包裹的密文或纯密文均可）
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, DEFAULT_PASSWORD, DEFAULT_ALGORITHM);
    }

    /**
     * 使用自定义密钥和算法解密
     */
    public static String decrypt(String encryptedText, String password, String algorithm) {
        String cipherText = unwrap(encryptedText);
        PooledPBEStringEncryptor encryptor = createEncryptor(password, algorithm);
        return encryptor.decrypt(cipherText);
    }

    private static PooledPBEStringEncryptor createEncryptor(String password, String algorithm) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(algorithm);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 去除 ENC(...) 包裹
     */
    private static String unwrap(String text) {
        if (text != null && text.startsWith(PREFIX) && text.endsWith(SUFFIX)) {
            return text.substring(PREFIX.length(), text.length() - SUFFIX.length());
        }
        return text;
    }

    /**
     * 独立运行入口，用法：
     * <pre>
     *   // 使用默认密钥加密
     *   java JasyptEncryptUtil encrypt mySecretValue
     *
     *   // 使用默认密钥解密
     *   java JasyptEncryptUtil decrypt "ENC(xxxx)"
     *
     *   // 使用自定义密钥加密
     *   java JasyptEncryptUtil encrypt mySecretValue myPassword PBEWithMD5AndDES
     * </pre>
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("用法:");
            System.out.println("  加密(默认密钥): java JasyptEncryptUtil encrypt <明文>");
            System.out.println("  解密(默认密钥): java JasyptEncryptUtil decrypt <密文>");
            System.out.println("  加密(自定义):   java JasyptEncryptUtil encrypt <明文> <password> [algorithm]");
            System.out.println("  解密(自定义):   java JasyptEncryptUtil decrypt <密文> <password> [algorithm]");
            return;
        }

        String action = args[0];
        String text = args[1];
        String password = args.length > 2 ? args[2] : DEFAULT_PASSWORD;
        String algorithm = args.length > 3 ? args[3] : DEFAULT_ALGORITHM;

        System.out.println("========== Jasypt " + action + " ==========");
        System.out.println("算法: " + algorithm);
        System.out.println("密钥: " + password.replaceAll(".", "*"));

        if ("encrypt".equalsIgnoreCase(action)) {
            String encrypted = encrypt(text, password, algorithm);
            System.out.println("明文: " + text);
            System.out.println("密文: " + encrypted);
        } else if ("decrypt".equalsIgnoreCase(action)) {
            String decrypted = decrypt(text, password, algorithm);
            System.out.println("密文: " + text);
            System.out.println("明文: " + decrypted);
        } else {
            System.out.println("未知操作: " + action + "，请使用 encrypt 或 decrypt");
        }
    }
}
