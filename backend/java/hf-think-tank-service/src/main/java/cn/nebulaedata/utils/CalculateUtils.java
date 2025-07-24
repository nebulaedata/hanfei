package cn.nebulaedata.utils;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 注意类名必须为 Main, 不要有任何 package xxx 信息

/**
 * @ClassName
 * @Description
 * @Author liulvhua
 * @Date 2023/4/5
 **/
public class CalculateUtils {
    private static Character pop1;


    public static Double getAnswer(String s) throws Exception {
        s = s.replaceAll("%", "*0.01");
        if (!checkFormulaRight(s)) throw new Exception();
        return parseF(s);
    }

    /**
     * 判断是否是公式
     *
     * @param formula
     * @return
     */
    public static Boolean checkFormula(String formula) {
        if (StringUtils.isBlank(formula)) return false;
        if (StringUtils.isBlank(formula.replaceAll("[0-9+\\-*/.()%]", ""))) return true;
        return false;
    }

    public static Boolean checkFormulaRight(String formula) {
        // (1) 不能出现连续的+-*/.%
        // (2) +*/.%不能开头
        // (3) +*/.-不能结尾
        // (4*) 括号成对
        // (5*) 先左后右
        // (5*) 不能没有数字
        // (6*) 左括号的左边的第一个字符只能为空或+-*/
        // (7*) 右括号的右边的第一个字符只能为空或+-*/
        String regex = "^(?![*/.%])(?!.*[*+-/.%]{2})(?!.*(\\.\\d){2})(.*)(?<![+*/.-])$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(formula);
        return matcher.find();
    }


    private static Double parseF(String s) throws ArithmeticException {
        if (!s.contains("F")) {
            return getResult(s);
        }
        //"F(1=1)(F(2>1)(3+2))";
        int index = s.indexOf("F");
        while ((index = s.indexOf("F")) != -1) {
            String con = s.substring(index + 2, s.indexOf(")"));
            double result = getResult(con);
            if (result > 0) {
                int tempIndex = s.indexOf("(", 2);
                String expr = s.substring(tempIndex + 1, s.lastIndexOf(")"));
                return parseF(expr);
            } else {
                return null;
            }
        }
        return null;
    }

    private static double getResult(String expr) throws ArithmeticException {
        Queue<String> queue = new LinkedList<String>();
        Stack<OperSign> signQueue = new Stack<OperSign>();
        Stack<String> calStack = new Stack<String>();
        List<String> result = changeExpr(expr);
        //自定义优先级（小括号）
        int customPro = 0;
        for (int i = 0; i < result.size(); i++) {
            if ("(".equals(result.get(i))) {
                customPro += 10;
                continue;
            }
            if (")".equals(result.get(i))) {
                customPro -= 10;
                continue;
            }
            OperSign operSign = toOperSign(result.get(i) + "");
            if (operSign == null) {
                queue.add(result.get(i) + "");
                continue;
            }
            operSign.setPrority(operSign.getPrority() + customPro);
            //
            if (signQueue.size() == 0) {
                signQueue.add(operSign);
                continue;
            }
//将优先级高的出栈
            while (signQueue.size() > 0) {
                OperSign pop = signQueue.pop();
                if (pop.getPrority() < operSign.getPrority()) {
                    signQueue.add(pop);
                    break;
                }
                queue.add(pop.getOperSign() + "");
            }
            signQueue.add(operSign);
        }
        while (signQueue.size() != 0) {
            queue.add(signQueue.pop().getOperSign() + "");
        }
//        System.out.println(queue);
        //计算
        String temp = null;
        while ((temp = queue.poll()) != null) {
            OperSign operSign = toOperSign(temp);
            if (operSign == null) {
                calStack.add(temp);
                continue;
            }
            cal(operSign, calStack);
        }
        return new Double(calStack.pop());
    }

    private static List<String> changeExpr(String expr) {
        List<String> result = new ArrayList<String>();
        Pattern compile = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher matcher = compile.matcher(expr);
        int start = 0;
        while (matcher.find()) {
            int flag = 0;
            for (; start < matcher.start(); start++) {
                String temp = expr.charAt(start) + "";
                if (toOperSign(temp) != null) {
                    flag++;
                }
                if (flag == 2) {
                    result.add("0");
                }
                result.add(temp);
            }
            start = matcher.end();
            String group = matcher.group();
            result.add(group);
        }

//      Matcher matcher=m
        return result;
    }

    private static void cal(OperSign c, Stack<String> calStack) throws ArithmeticException {
        String pop2 = calStack.pop();
        String pop1 = "0";
        if (calStack.size() > 0) {
            pop1 = calStack.pop();
        }
        BigDecimal b = null;

        if ("+".equals(c.getOperSign())) {
            b = new BigDecimal(pop1).add(new BigDecimal(pop2));
        }
        if ("-".equals(c.getOperSign())) {
            b = new BigDecimal(pop1).subtract(new BigDecimal(pop2));
        }
        if ("*".equals(c.getOperSign())) {
            b = new BigDecimal(pop1).multiply(new BigDecimal(pop2));
        }
        if ("/".equals(c.getOperSign())) {
//            if ("0".equals(pop2)) {
//                b = new BigDecimal(pop1);
//            } else {
//                b = new BigDecimal(pop1).divide(new BigDecimal(pop2), 9, BigDecimal.ROUND_HALF_UP);
//            }
            b = new BigDecimal(pop1).divide(new BigDecimal(pop2), 9, BigDecimal.ROUND_HALF_UP);
        }
//        if (">".equals(c.getOperSign())) {
//            b = 0.0;
//            if (new BigDecimal(pop1) > new BigDecimal(pop2)) {
//                b = 1.0;
//            }
//        }
//        if ("<".equals(c.getOperSign())) {
//            b = 0.0;
//            if (new BigDecimal(pop1) < new BigDecimal(pop2)) {
//                b = 1.0;
//            }
//        }
//        if ("=".equals(c.getOperSign())) {
//            b = 0.0;
//            if (new BigDecimal(pop1) == new BigDecimal(pop2)) {
//                b = 1.0;
//            }
//        }
//        if ("&".equals(c.getOperSign())) {
//            b = 0.0;
//            if (new BigDecimal(pop1) > 0 && new BigDecimal(pop2) > 0) {
//                b = 1.0;
//            }
//        }
//        if ("|".equals(c.getOperSign())) {
//            b = 0.0;
//            if (new BigDecimal(pop1) + new BigDecimal(pop2) > 0) {
//                b = 1.0;
//            }
//        }

        calStack.add(b + "");
    }

    //  private static OperSign toOperSign(char c) {
//      if(c=='+'){
//          return new OperSign(c,1);
//      }
//      if(c=='-'){
//          return new OperSign(c,1);
//      }
//      if(c=='*'){
//          return new OperSign(c,2);
//      }
//      if(c=='/'){
//          return new OperSign(c,2);
//      }
//      return null;
//  }
    private static OperSign toOperSign(String c) {
        if ("+".equals(c)) {
            return new OperSign(c, 1);
        }
        if ("-".equals(c)) {
            return new OperSign(c, 1);
        }
        if ("*".equals(c)) {
            return new OperSign(c, 2);
        }
        if ("/".equals(c)) {
            return new OperSign(c, 2);
        }
        if (">".equals(c)) {
            return new OperSign(c, 0);
        }
        if ("<".equals(c)) {
            return new OperSign(c, 0);
        }
        if ("&".equals(c)) {
            return new OperSign(c, -1);
        }
        if ("|".equals(c)) {
            return new OperSign(c, -1);
        }
        if ("=".equals(c)) {
            return new OperSign(c, 0);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
//        System.out.println();
//        System.out.println(checkFormulaRight("1+1"));  // 1
//        System.out.println(checkFormulaRight("1++1"));  // 0
//        System.out.println(checkFormulaRight("+1+1"));  // 1
//        System.out.println(checkFormulaRight("/1+1"));  // 0
//        System.out.println(checkFormulaRight("-1+1"));  // 1
//        System.out.println(checkFormulaRight("1+1+"));  // 0
//        System.out.println(checkFormulaRight("-"));  // 0
//        System.out.println(checkFormulaRight("("));  // 0
        System.out.println(checkFormulaRight("2%*2.5+2.5"));  // 0
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        Double answer = getAnswer("()");
        System.out.println(answer);
//        String s = "1.2*2.3*3.3";
//        System.out.println(s + "= " + parseF(s));
//        s = "((0.13)+(-1.123))*(-32)";
//        System.out.println(s + "= " + parseF(s));
//        s = "0.13+-1.123*-32";
//        System.out.println(s + "= " + parseF(s));
//        s = "10/3*3";
//        System.out.println(s + "= " + parseF(s));
//        s = "10*3/3";
//        System.out.println(s + "= " + parseF(s));
//        Double answer = getAnswer("1/0");
//        System.out.println(answer);
//
//        String expr = "3*200-900/(-2*5-(-3-4)*(9+3))";
//        System.out.println(parseF(expr));

    }

}


class OperSign {
    private String operSign;
    private Integer prority;

    OperSign(String operSign, Integer prority) {
        this.operSign = operSign;
        this.prority = prority;
    }

    public String getOperSign() {
        return operSign;
    }

    public void setOperSign(String operSign) {
        this.operSign = operSign;
    }

    public Integer getPrority() {
        return prority;
    }

    public void setPrority(Integer prority) {
        this.prority = prority;
    }

}