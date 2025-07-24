package cn.nebulaedata.utils;

import java.util.Random;

/**
 * @author 徐衍旭
 * @date 2021/9/24 14:40
 * @note
 */
public class RandomUtils {
    //生成8位数的 字母 大小随机 当做授权码
    public static String randomUtil() {
        Random r = new Random();
        String code = "";
        for (int i = 0; i < 8; ++i) {
            int temp = r.nextInt(52);
            char x = (char) (temp < 26 ? temp + 97 : (temp % 26) + 65);
            code += x;
        }
        return code;
    }

    public static void main(String[] args) {
        String s = RandomUtils.randomUtil();
        System.out.println(s);
    }
}
