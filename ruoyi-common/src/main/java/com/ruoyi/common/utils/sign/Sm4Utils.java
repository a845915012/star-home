package com.ruoyi.common.utils.sign;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SM4国密对称加密工具类
 * <p>
 * SM4分组密码算法，密钥长度128位(16字节)，CBC模式，PKCS5Padding填充。
 * 前端随机生成IV（16字符），加密后将IV原文拼接在Base64密文后面传输。
 * 后端解密时截取后16个字符作为IV（16字节）。
 * </p>
 *
 * @author starhome
 */
public class Sm4Utils {

    private static final Logger log = LoggerFactory.getLogger(Sm4Utils.class);

    /** SM4算法名称 */
    private static final String ALGORITHM = "SM4";

    /** SM4加密算法/CBC模式/PKCS5Padding填充 */
    private static final String CIPHER_ALGORITHM = "SM4/CBC/PKCS5Padding";

    /** IV长度（字节） */
    private static final int IV_LENGTH = 16;

    /** 默认密钥（16字节），可通过配置覆盖 */
    private static byte[] defaultKey = "StarHomeSM4Key!@#".getBytes(StandardCharsets.UTF_8);

    static {
        // 注册Bouncy Castle Provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 设置SM4密钥（由Spring Boot启动时从配置文件注入）
     *
     * @param key 密钥字符串（16字节）
     */
    public static void setKey(String key) {
        if (key != null && key.length() >= 16) {
            defaultKey = key.getBytes(StandardCharsets.UTF_8);
            log.info("SM4密钥已从配置文件加载");
        }
    }

    /**
     * SM4-CBC加密（Base64输出，IV原文拼接在密文后面）
     *
     * @param plainText 明文
     * @param key       密钥（16字节）
     * @param iv        初始向量（16字节）
     * @return Base64编码的密文 + IV原文
     */
    public static String encrypt(String plainText, byte[] key, byte[] iv) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            String cipherBase64 = Base64.getEncoder().encodeToString(encrypted);
            // IV原文直接拼接在Base64密文后面
            return cipherBase64 + new String(iv, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM4加密失败", e);
            throw new RuntimeException("SM4加密失败", e);
        }
    }

    /**
     * SM4-CBC解密（前端拼接格式：Base64密文 + IV原始字符串）
     * <p>
     * 前端随机生成16字符IV字符串，加密后将IV原文拼接在Base64密文后面。
     * 后端截取后16个字符作为IV（16字节）。
     * </p>
     *
     * @param cipherTextWithIv Base64密文 + IV原文（拼接后的字符串）
     * @param key              密钥（16字节）
     * @return 明文
     */
    public static String decrypt(String cipherTextWithIv, byte[] key) {
        try {
            // 截取后16个字符作为IV原文（16字节）
            if (cipherTextWithIv.length() <= IV_LENGTH) {
                throw new IllegalArgumentException("密文长度不足，无法提取IV");
            }
            String ivStr = cipherTextWithIv.substring(cipherTextWithIv.length() - IV_LENGTH);
            String cipherBase64 = cipherTextWithIv.substring(0, cipherTextWithIv.length() - IV_LENGTH);
            byte[] iv = ivStr.getBytes(StandardCharsets.UTF_8);

            SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("SM4解密失败", e);
            throw new RuntimeException("SM4解密失败", e);
        }
    }

    /**
     * SM4-CBC加密（使用默认密钥，Base64输出，IV原文拼接在密文后面）
     *
     * @param plainText 明文
     * @param iv        初始向量（16字节）
     * @return Base64编码的密文 + IV原文
     */
    public static String encrypt(String plainText, byte[] iv) {
        return encrypt(plainText, defaultKey, iv);
    }

    /**
     * SM4-CBC解密（使用默认密钥，前端拼接格式：Base64密文 + IV原文）
     *
     * @param cipherTextWithIv Base64密文 + IV原文（拼接后的字符串）
     * @return 明文
     */
    public static String decrypt(String cipherTextWithIv) {
        return decrypt(cipherTextWithIv, defaultKey);
    }
}
