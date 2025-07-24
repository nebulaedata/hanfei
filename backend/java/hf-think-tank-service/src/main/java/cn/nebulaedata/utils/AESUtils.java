package cn.nebulaedata.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AESUtils {
    private static final String AES_ALGORITHM = "AES/ECB/PKCS5Padding";
    public static final String key = "fK8hG2b3EerdWvh6";

    /**
     * 获取 cipher
     *
     * @param key
     * @param model
     * @return
     * @throws Exception
     */
    private static Cipher getCipher(byte[] key, int model) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(model, secretKeySpec);
        return cipher;
    }

    /**
     * AES加密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static String encrypt(byte[] data, byte[] key) {
        try {
            Cipher cipher = getCipher(key, Cipher.ENCRYPT_MODE);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * AES解密
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] data, byte[] key) {
        try {
            Cipher cipher = getCipher(key, Cipher.DECRYPT_MODE);
            return cipher.doFinal(Base64.getDecoder().decode(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static byte[] decrypt(byte[] data) {
        try {
            Cipher cipher = getCipher(key.getBytes(StandardCharsets.UTF_8), Cipher.DECRYPT_MODE);
            return cipher.doFinal(Base64.getDecoder().decode(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static void main(String[] args) throws Exception {
        String t = "nihao";
        String encrypt = encrypt(t.getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8));
        System.out.println(encrypt);
        String decrypt = new String(decrypt("eQbbgpWxUO45WbegpBZJ94Bd6fb2RH9gAdcD1U54EBfBq+mVtmdCjbFJClFurnGQWLAsx6ZHTpf32uSSXxOqme7kR3Qjw8PgDjqJAUq2YuO5hraNToHkv4m07ZQb2BiV/5wy6/FaXV4Pfydlej3/2MzBghPc9Nq+Xc49moirThLgh/1qe+kVx6hqSD6q9hWgwJKbChbCgHCAhrcSlVPUBLDiTJnfFPumfxiSJSKVMSd/XVd39lFCNIhx50NcJepA7r+cHQsuX1i2/PnZUJLBtHUNiOe5370MK+APgLmAeuMROWaYO/tZvfcEsVjMe6UUSJfzs1qDmgc7ChdR0eqGkxzJB9FjllhVgoFXgiPf3R2YJzF/OeSf4oubef0SMHuRTFBVMLCUVue0CL1JkEEtgOLch57R2GycHGWDGOeBGNFrR6vlGFlOEoXHEHv6z9/gBrC/KM7i4wtxt3RlhbPaduZo+NAhrV2zQghZtMh5FZwSAqwRL2qoEV3wx4jiW8s3CeT42TxUgG/8h0WgIG4qqJO5YO9ivuOL7I3iqkkppWKlHMG7JK1uCvOHLDX9M70C3XBAt3QoBHLrvTDarSrBm2irxei1PAStD1tJLJgxMVck2nWHprKj57uW6KSPKAoNpK8opOqviHCXgeCC3l6kZMlJr9LrpMAZVFtfoAAStHxtiWDHT/2DTBQgZpYugYdF3XbY24dzeibH2xKQx1chbgfUIb4gRZ1ZDAO3kh9XFYSkajTjoLv7fRzKVdu1bOWkzVFIRzybQq9PLQocOVZRocqSoIYrq2y7N88hPIz69x6oE9lBAkj4iTDl1dySDhNVdSMOZRVV2Ez/Zr5FRZg7FuL7L1/Y8L2gA8gaGogSD1Y=".getBytes(StandardCharsets.UTF_8), key.getBytes(StandardCharsets.UTF_8)));
        System.out.println(decrypt);
    }

    /**
     * 将二进制转换成16进制
     *
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

}