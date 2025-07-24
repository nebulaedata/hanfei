package cn.nebulaedata.utils;

/**
 * @author 徐衍旭
 * @date 2024/1/8 14:27
 * @note
 */

import java.util.ArrayList;
import java.util.Scanner;

public class CalculateUtils2 {
    private final static ArrayList<Character> cArr = new ArrayList<Character>();//运算符集合
    private final static ArrayList<Double> dArr = new ArrayList<Double>();//操作数字符集合
    private static int index = 0;//运算符等级

    private static int get_index(char theta) {
        switch (theta) {
            case '+':
                index = 0;
                break;
            case '-':
                index = 1;
                break;
            case '*':
                index = 2;
                break;
            case '/':
                index = 3;
                break;
            case '(':
                index = 4;
                break;
            case ')':
                index = 5;
                break;
            case '#':
                index = 6;
            default:
                break;
        }
        return index;
    }

    private static char get_priority(char theta1, char theta2) {//获取theta1与theta2之间的优先级
        char[][] priority = {//算符间的优先级关系
                {'>', '>', '<', '<', '<', '>', '>'},
                {'>', '>', '<', '<', '<', '>', '>'},
                {'>', '>', '>', '>', '<', '>', '>'},
                {'>', '>', '>', '>', '<', '>', '>'},
                {'<', '<', '<', '<', '<', '=', '0'},
                {'>', '>', '>', '>', '0', '>', '>'},
                {'<', '<', '<', '<', '<', '0', '='},
        };

        int index1 = get_index(theta1);
        int index2 = get_index(theta2);
        return priority[index1][index2];//返回级别关系
    }

    private static double calculate(double b, char theta, double a) { //计算b theta a
        switch (theta) {
            case '+':
                return b + a;
            case '-':
                return b - a;
            case '*':
                return b * a;
            case '/':
                return b / a;
            default:
                break;
        }
        return 0;
    }

    public static double getAnswer(String s) {//表达式求值
        cArr.add('#');//首先将'#'添加到操作符集合 （增加）
        int counter = 0;//添加变量counter表示有多少个数字，实现多位数的四则运算
        char[] c = s.replaceAll(" ","").toCharArray();//将键盘输入的算式转成字符
        int loc = 0;//标记读取的字符串的位置
        //int i = 1;//用于小数相加
        while (c[loc] != '#' || !cArr.get(cArr.size() - 1).equals('#'))   //取出集合最后一个元素，不会删掉集合里边的元素
        {
            int i = 1;//用于小数相加
            if (Character.isDigit(c[loc]))   //若参数c为阿拉伯数字0~9，则返回false值，否则返回ture。
            {
                if (counter == 1)   //counter==1表示上一字符也是数字，所以要合并，比如23*23，要算23，而不是单独的2和3
                {
                    double t = dArr.get(dArr.size() - 1);
                    dArr.remove(dArr.size() - 1);//移除元素 （删除）
                    dArr.add(t * 10 + (c[loc] - '0'));//字符转数字的时候(如：‘8’ - ‘0’ 的计算结果就是8)
                } else {
                    dArr.add((double) (c[loc] - '0'));//将c数组对应的数值添加到操作数集合
                    counter++;//代表此数字为数字
                }
                loc++;//重新获取字符
            } else if (c[loc] == '.' && counter == 1)//小数点的计算
            {
                loc++;
                while (Character.isDigit(c[loc]))   //判断参数c[loc]为阿拉伯数字0~9
                {
                    int j = i;
                    double num = (double) c[loc];
                    num = (num - '0');//转成对应的数字
                    while (j != 0) {
                        j--;
                        num = num * 0.1;
                    }
                    double t = dArr.get(dArr.size() - 1);
                    t = t + num;
                    dArr.remove(dArr.size() - 1);//移除元素 （删除）
                    dArr.add(t);
                    loc++;//重新获取字符
                    i++;
                }
                counter = 0;//counter置零
            } else {
                counter = 0;//counter置零
                i = 0;//小数个数置零
                switch (get_priority(cArr.get(cArr.size() - 1), c[loc]))//获取运算符集合最后一个运算符与c[loc]之间的优先级，用'>'，'<'，'='表示
                {
                    case '<'://<则将c入栈
                        cArr.add(c[loc]);
                        loc++;//重新获取字符
                        break;
                    case '='://集合元素弹出，用于括号的处理
                        cArr.remove(cArr.size() - 1);
                        loc++;//重新获取字符
                        break;
                    case '>'://>则计算
                        char theta = cArr.get(cArr.size() - 1);
                        cArr.remove(cArr.size() - 1);
                        double a = dArr.get(dArr.size() - 1);
                        dArr.remove(dArr.size() - 1);
                        double b = dArr.get(dArr.size() - 1);
                        dArr.remove(dArr.size() - 1);
                        dArr.add(calculate(b, theta, a));
                }
            }
        }
        return dArr.get(dArr.size() - 1);   //求得元素的值
    }

    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        int t;     // 需要计算的表达式的个数
//        int i = 1;
//        System.out.print("请输入要计算的表达式个数:");
//        t = scanner.nextInt();
//        while (t != 0) {
//            t--;
//            System.out.println("请输入第" + i + "个表达式的运算,以#号结束:");
//            scanner = new Scanner(System.in);//刷新Scanner方法
//            String s1 = scanner.nextLine();//获取项
//            String s = s1.replaceAll("%", "*0.01");
//            cArr.clear();//释放运算符集合
//            dArr.clear();//释放操作数集合
//            double ans = getAnswer(s);//输入，得结果
//            System.out.print("结果：" + ans);
//            System.out.println();
//            i++;
//        }
        String s = "98 + 100 + 92 + 100 + 100#";
        System.out.println(getAnswer(s));
    }
}