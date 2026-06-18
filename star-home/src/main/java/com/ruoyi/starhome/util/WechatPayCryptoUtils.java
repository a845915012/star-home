package com.ruoyi.starhome.util;

import com.ruoyi.common.exception.ServiceException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付签名与解密工具
 */
public final class WechatPayCryptoUtils {

    private static final String PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    private static final String PRIVATE_KEY_END = "-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    private static final String PUBLIC_KEY_END = "-----END PUBLIC KEY-----";
    private static final int GCM_TAG_LENGTH = 128;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private WechatPayCryptoUtils() {
    }

    public static String signWithRsaSha256(String content, String pemPrivateKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(loadPrivateKey(pemPrivateKey));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new ServiceException("微信支付签名失败: " + e.getMessage());
        }
    }

    public static boolean verifyWithRsaSha256(String content, String signatureValue, String pemPublicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(loadPublicKey(pemPublicKey));
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signatureValue));
        } catch (Exception e) {
            throw new ServiceException("微信支付验签失败: " + e.getMessage());
        }
    }

    public static String decryptAesGcm(String apiV3Key, String associatedData, String nonce, String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
            if (associatedData != null) {
                cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
            }
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("微信支付通知解密失败: " + e.getMessage());
        }
    }

    public static String sha1Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            char[] chars = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int value = bytes[i] & 0xFF;
                chars[i * 2] = HEX[value >>> 4];
                chars[i * 2 + 1] = HEX[value & 0x0F];
            }
            return new String(chars);
        } catch (Exception e) {
            throw new ServiceException("生成SHA1签名失败: " + e.getMessage());
        }
    }

    private static PrivateKey loadPrivateKey(String pemPrivateKey) throws Exception {
        String normalized = normalizeKey(pemPrivateKey, PRIVATE_KEY_BEGIN, PRIVATE_KEY_END);
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private static PublicKey loadPublicKey(String pemPublicKey) throws Exception {
        String normalized = normalizeKey(pemPublicKey, PUBLIC_KEY_BEGIN, PUBLIC_KEY_END);
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private static String normalizeKey(String content, String begin, String end) {
        if (content == null || content.isBlank()) {
            throw new ServiceException("微信支付密钥未配置");
        }
        return content.replace("\\n", "\n")
                .replace(begin, "")
                .replace(end, "")
                .replaceAll("\\s+", "");
    }
}
