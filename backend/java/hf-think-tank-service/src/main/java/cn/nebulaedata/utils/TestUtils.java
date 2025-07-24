package cn.nebulaedata.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang.StringUtils;
import org.apache.tomcat.jni.Time;
import org.ehcache.impl.internal.concurrent.ConcurrentHashMap;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 徐衍旭
 * @date 2023/11/24 18:32
 * @note
 */
public class TestUtils {
    private static Character pop1;

    public static void a() {
        HashMap<String, String> map1 = new HashMap<>();
        HashMap<String, String> map2 = new HashMap<>();
        map1.put("a", "1");
        map1.put("b", "2");
        map2.put("a", "2");
        map1.putAll(map2);
        System.out.println(JSON.toJSONString(map1));

        System.out.println(aaa());

        String s3 = "tools_bj_sit_roomKey_1f46fcb4db634e46b9e342100b61fbed_71eebe8f891b444ca3f847a4d12c83ea";
        String s4 = s3.split("roomKey")[1].split("_")[1];
        String s5 = s3.split("roomKey")[1].split("_")[2];
        System.out.println(s4);
        System.out.println(s5);

        Date date1 = new Date();


        String s2 = "room_apply_result_accept,room_apply_result";
        String s1 = s2.replaceAll(",", "','");
        System.out.println(s1);
        System.out.println(s2);

        System.out.println(null + "2");

        ArrayList<Object> list = new ArrayList<Object>() {{
            add("66666");
        }};
//        HashMap<String, Object> map1 = new HashMap<String, Object>() {{
//            put("a",list);
//            put("b","2");
//            put("c","3");
//        }};
//        HashMap<String, Object> map2 = new HashMap<>();
//        map2.putAll(map1);
//        list.add("7777");
////        System.out.println(map1.get("a") == map2.get("a"));
//        map1.put("a","9999");
//        System.out.println(JSON.toJSONString(map1));
//        System.out.println(JSON.toJSONString(map2));


        String s = UUID.randomUUID().toString().replaceAll("-", "");
        System.out.println(s.length());

        String string = Base64.getEncoder().encodeToString("111/222/333".getBytes());
        System.out.println(string);
        byte[] decode = Base64.getDecoder().decode(string);
        System.out.println(new String(decode));

//        Double[] num = {1.11, 2.22, 3.33, 4.44, 5.55, 6.66};
//        String[] str = {"Hello", "World", "你好", "世界"};
//
//        Generic01 generic01 = new Generic01();
//        generic01.toGeneric01(num);
//        generic01.toGeneric01(str);
//
//
//        Generic02<Double> doubleGeneric02 = new Generic02<>();
//        doubleGeneric02.toGeneric02(num);
//        Generic02<String> doubleGeneric03 = new Generic02<>();
//        doubleGeneric03.toGeneric02(str);
//
//        try {
//            Class<?> cls = MyClass.class;
//            Field field = cls.getDeclaredField("myAttribute");
//            if (field == null) {
//                throw new RuntimeException("The class 'MyClass' does not have the attribute 'myAttribute'");
//            }
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        }

        Date date2 = new Date();
        System.out.println(date2.getTime() - date1.getTime());
    }

    public static String aaa() {
        try {
            return "222";
        } catch (Exception e) {
            return "333";
        } finally {
            return "111";
        }
    }

    public static void bbb() throws InterruptedException {
        for (int i = 0; i <= 20; i++) {
            System.out.println(i); // 输出当前数字

            Thread.sleep(1000); // 休眠1秒
        }
    }

    public static void ccc() throws InterruptedException {
        String input = "Hello (World) [你好1] [你好2]";
        String regex = "\\[(.*?)]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        while (matcher.find()) {
            String result = matcher.group(1);
            System.out.println(result);
        }
    }

    public static void ddd() throws InterruptedException {
        String input = "Hello (World) [你好1] [你好2]";
        String s = input.replaceAll("你好", "nihao");
        System.out.println(input);
        System.out.println(s);
    }

    public static void ddd2() throws InterruptedException {
        String input = "[7d6d5fc23f024385b0350f3d3c650659] + [01770c2a9d3e4fa7852812cc1818113a] + [9dcc107efbd346d4a1c862d15b4f2ca5] + [8c10d0a9d15540019bb018815cc0bcda] + [4f093c7510bf4c6096c1ee404f60cfb6] ";
        String s = input.replaceAll("\\[7d6d5fc23f024385b0350f3d3c650659\\]", "nihao");
        System.out.println(input);
        System.out.println(s);
    }

    public static void eee() throws InterruptedException {
        String input = "{\"726e14e4-409d-4ee2-982a-4b76865e39eb\":\"龙傲天\",\"c617f4d0-1e47-4566-96b4-3fcd5e4162ce\":{\"770027\":150},\"5dc57081-c5ae-4e17-86ba-8e26b21d3018\":{\"770027\":150},\"3287bf49-1c09-43dc-a916-872b92b7734f\":{\"770027\":150},\"key\":\"b811a3a1-5b89-4b33-8273-4d2cd9b01574\"}";
        Map<String, Object> map = JSON.parseObject(input, Map.class);
        Object o = map.get("3287bf49-1c09-43dc-a916-872b92b7734f");
        if (o == null) {

        } else if (o instanceof String) {

        } else if (o instanceof Map) {
            Map<String, Object> o1 = (Map) o;
            Set<String> strings = o1.keySet();
            for (String s : strings) {
                System.out.println(o1.get(s));
            }
        } else if (o instanceof List) {

        } else {

        }


    }

    public static void fff() throws Exception {
//        HashMap<String, Object> stringStringHashMap = new HashMap<>();
        ConcurrentHashMap<String, Object> concurrentHashMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            new Thread(() -> {

//                stringStringHashMap.put("taskId", finalI);
                concurrentHashMap.put("taskId", finalI);
//                System.out.println("-------------------");
//                System.out.println("stringStringHashMap : " + stringStringHashMap.get("taskId"));
                System.out.println("  concurrentHashMap : " + concurrentHashMap.get("taskId"));
            }).start();
        }// 休眠1秒

    }


    private static Boolean checkFormula(String formula) {
        long startTime = System.currentTimeMillis();
        String[] strings = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "+", "-", "*", "/", "(", ")", "%", "."};

        if (StringUtils.isBlank(formula)) {
            return false;
        }
        for (int i = 0; i < formula.length(); i++) {
            String substring = formula.substring(i, i + 1);
            for (int j = 0; j < strings.length; j++) {
                if (strings[j].equals(substring)) {
                    break;
                } else if (j == strings.length - 1) {
                    return false;
                }
            }
        }
        long endTime = System.currentTimeMillis();
        long timeLong = endTime - startTime;
        System.out.println(timeLong);
        return true;
    }

    public static Boolean checkFormula2(String formula) {
        long startTime = System.currentTimeMillis();
        if (StringUtils.isBlank(formula)) return false;
        if (StringUtils.isBlank(formula.replaceAll("[0-9+\\-*/.()%]", ""))) {
            long endTime = System.currentTimeMillis();
            long timeLong = endTime - startTime;
            System.out.println(timeLong);
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Exception {

        String a = "1235+(90*2)-2/3*0+90%***********************************************";
        System.out.println(checkFormula(a) + " -- " + checkFormula2(a));


    }
}


class Generic01 {
    public <T> T toGeneric01(T[] arr) {
        return arr[arr.length - 1];
    }
}

class Generic02<T> {
    public T toGeneric02(T[] arr) {
        return arr[arr.length - 1];
    }
}

class Generic03 {
    public <T> void toGeneric03(T[] arr) {
        T t = arr[arr.length - 1];
    }
}

class MyClass {
    private String myAttribute;

    // getters and setters...
}


/**
 * java规定，变量没有初始化不能使用，全局变量也就是类的属性，java会在编译的时候，自动将他们初始化，
 * 所以可以不进行变量初始化的操作，☆☆☆☆☆但是（局部）变量必须初始化☆☆☆☆☆
 * <p>
 * 静态数据的初始化
 * 初始化的顺序是：先静态对象，而后是“非静态”对象。
 * <p>
 * 非静态成员初始化
 * 在一个类中，非静态成员的初始化，发生在任何方法（包括构造方法）被调用之前。并且它们定义的顺序，决定了初始化的顺序
 * 变量初始化完成之后才厚调用构造函数
 * <p>
 * 先加载类:执行静态块,静态变量赋值 因为静态部分是依赖于类,而不是依赖于对象存在的,所以静态部分的加载优先于对象存在
 */
class Singleton {

    //1.
    private static int x = 0;

    private static int y;

    //2.
    private static Singleton instance = new Singleton();

    /**
     * -----------------------------------------------------------------------------------------
     * 构造方法是new对象的时候进行调用(此时变量已经初始化完成)
     * 如果注释1在上面 注释2在下面 打印结果为 1 1  准备阶段先对静态变量进行x,y进行赋初始值0,0
     * 然后new Singleton对象开始调用构造函数的x++,y++所以x,y的值变为1,1
     * <p>
     * 注意:不可以在构造函数的参数初始化表中对静态成员变量初始化，但是可以在构造函数中修改静态变量的值
     * 如果注释2在上面 注释1在下面 打印结果为 0 1
     * 先进行++操作 x,y的值变为1,1 后面对x,y进行初始化操作
     */
    private Singleton() {
        x++;
        y++;
    }

    public static void main(String[] args) {
        System.out.println(Singleton.x);
        System.out.println(Singleton.y);
    }

}
