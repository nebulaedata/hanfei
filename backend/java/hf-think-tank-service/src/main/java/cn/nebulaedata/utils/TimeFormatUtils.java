package cn.nebulaedata.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static cn.nebulaedata.utils.JsonKeyUtils.stringToList;

/**
 * @author 徐衍旭
 * @date 2021/11/9 10:35
 * @note
 */
public class TimeFormatUtils {
    public static Date getDateByString(String time) {
        Date date = null;
        if (time == null)
            return date;
        String date_format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat format = new SimpleDateFormat(date_format);
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getShortTime(long dateline) {
        String shortstring = null;
        String time = timestampToStr(dateline);
        Date date = getDateByString(time);
        if (date == null) return shortstring;

        String ago = getAgo(date);
        return ago;
    }

    public static String getShortTime(Date date) {
        String shortstring = null;
        if (date == null) return shortstring;

        String ago = getAgo(date);
        return ago;
    }

    public static String getShortTime(String dateStr) {
        Date date = getDateByString(dateStr);
        String shortstring = null;
        if (date == null) return shortstring;

        String ago = getAgo(date);
        return ago;
    }

    public static String getAgo(Date date) {
        // 获取昨天与今天的分界时间戳 l1
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        String todayymd = sdf1.format(new Date());
        String todayymd2 = todayymd + " 00:00:00";
        long l1 = getDateByString(todayymd2).getTime();
        // 获取前天与昨天的分界时间戳 l2
        long l2 = l1 - 86400000;
        // 获取大前天与前天的分界时间戳 l3
        long l3 = l2 - 86400000;

        // 待格式化时间戳
        long targetTime = date.getTime();
        // 单独获取时分秒信息
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String HMS = sdf.format(date);
        // 开始判断
        long now = Calendar.getInstance().getTimeInMillis();
        long deltime = (now - targetTime) / 1000;
        String shortstring = null;
        if (l1 < targetTime) {
            // 时间在今天
            if (deltime > 60 * 60) {
                shortstring = (int) (deltime / (60 * 60)) + "小时前";
            } else if (deltime > 60) {
                shortstring = (int) (deltime / (60)) + "分前";
            } else if (deltime > 15) {
                shortstring = deltime + "秒前";
            } else {
                shortstring = "刚刚";
            }
        } else if (l2 < targetTime && targetTime <= l1) {
            // 时间在昨天
            shortstring = "昨天 " + HMS;
        } else if (l3 < targetTime && targetTime <= l2) {
            // 时间在前天
            shortstring = "前天 " + HMS;
        } else {
            if (deltime > 365 * 24 * 60 * 60) {
                shortstring = (int) (deltime / (365 * 24 * 60 * 60)) + "年前";
            } else if (deltime > 30 * 24 * 60 * 60) {
                shortstring = (int) (deltime / (30 * 24 * 60 * 60)) + "个月前";
            } else if (deltime > 24 * 60 * 60) {
                shortstring = (int) (deltime / (24 * 60 * 60)) + "天前";
            }
        }
        return shortstring;
    }

    //Timestamp转化为String:
    public static String timestampToStr(long dateline) {
        Timestamp timestamp = new Timestamp(dateline * 1000);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        return df.format(timestamp);
    }

    //Timestamp转化为String:
    public static String timestampToStr(Date dateline) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//定义格式，不显示毫秒
        return df.format(dateline);
    }

    //Timestamp转化为String:
    public static String timestampToStr(Date dateline,String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern); // 自定义格式
        return df.format(dateline);
    }

    /**
     * 获取当前时间戳
     * @return
     */
    public static String now() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return df.format(new Date());
    }
    //Timestamp转化为String:
    public static String now(String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern); // 自定义格式
        return df.format(new Date());
    }


    /**
     * 字符串时间转按格式输出
     *
     * @param
     */
    public static String getFormatTimeStr(String time, String style) {
        Date date = null;
        if (time == null || "".equals(time) || "null".equals(time)  ) {
            return null;
        }

        if (style == null) {
            return time;
        }

        // 转换大写特殊处理  一共三种情况
        if (style.equals("二〇二一年九月九日")) {
            return dataToUpper(time);
        } else if (style.equals("二〇二一年九月")) {
            return dataToUpper(time).substring(0,8);
        } else if (style.equals("二〇二一年")) {
            return dataToUpper(time).substring(0,5);
        }

        String date_format = "yyyy-MM-dd HH:mm:ss";
        if (time.length()==10) {
            // 只有年月日
            date_format = "yyyy-MM-dd";
        }

        SimpleDateFormat format = new SimpleDateFormat(date_format);
        try {
            date = format.parse(time);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        SimpleDateFormat sdf = new SimpleDateFormat(style);
        String newTimeStyle = sdf.format(date);
        return newTimeStyle;
    }



    // 日期转化为大小写
    public static String dataToUpper(String dateString) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date dateTime = null;
        try {
            dateTime = simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar ca = Calendar.getInstance();
        ca.setTime(dateTime);
        int year = ca.get(Calendar.YEAR);
        int month = ca.get(Calendar.MONTH) + 1;
        int day = ca.get(Calendar.DAY_OF_MONTH);
        return numToUpper(year) + "年" + monthToUppder(month) + "月" + dayToUppder(day) + "日";
    }

    // 将数字转化为大写（字体格式可自己定义）
    public static String numToUpper(int num) {
        //String u[] = {"零","壹","贰","叁","肆","伍","陆","柒","捌","玖"};
        String u[] = {"零","一","二","三","四","五","六","七","八","九"};
        char[] str = String.valueOf(num).toCharArray();
        String rstr = "";
        for (int i = 0; i < str.length; i++) {
            rstr = rstr + u[Integer.parseInt(str[i] + "")];
        }
        return rstr;
    }

    // 月转化为大写
    public static String monthToUppder(int month) {
        if(month < 10) {
            return numToUpper(month);
        } else if(month == 10){
            return "十";
        } else {
            return "十" + numToUpper(month - 10);
        }
    }

    // 日转化为大写
    public static String dayToUppder(int day) {
        if(day < 20) {
            return monthToUppder(day);
        } else {
            char[] str = String.valueOf(day).toCharArray();
            if(str[1] == '0') {
                return numToUpper(Integer.parseInt(str[0] + "")) + "十";
            }else {
                return numToUpper(Integer.parseInt(str[0] + "")) + "十" + numToUpper(Integer.parseInt(str[1] + ""));
            }
        }
    }


    /**
     * 判断实际时长
     * @param dateStr
     * @return
     */
    public static String getFactUseMin(String dateStr) throws ParseException {
        List<String> strings = stringToList(dateStr, ",");
        long tot = 0;
        for (int i = 0;i<strings.size();i++) {
            if (i+1<strings.size()) {
                Date d1 = getDateByString(strings.get(i));
                Date d2 = getDateByString(strings.get(i+1));
                long aInteger = (d2.getTime() - d1.getTime()) / (60 * 1000) ;
                tot = tot + aInteger;
                i++;
            }
        }
        return String.valueOf(tot);
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(timestampToStr(new Date()));
//        long dateline = 1335189486;
//        System.out.println(getShortTime(dateline));
//        String time = "2021-11-14 23:52:03";
//        String factUseMin = getFactUseMin(null);
//        System.out.println(factUseMin);
//        System.out.println(getShortTime(time));
//
//        System.out.println(timestampToStr(new Date()));
//        Date dateByString = getDateByString(time);
//        System.out.println(dateByString);
//        String s = getAgo(dateByString);
//        System.out.println(s);

//        String yyyyMMddHHmmss = getFormatTimeStr("null", "二〇二一年");
//        System.out.println(yyyyMMddHHmmss.toString());
//        int i = 1;
//        while (i < 10) {
//            i += i;
//        }
//        System.out.println(i);
//
//        String contentText = "<parameter uuid=\"0e8b09bf-96a6-48fe-a878-48fd259816e0\" key=\"7a4aece2096d4fbcb0c8f3d572feb150\" title=\"参数\" placeholder=\"招标文件发布日期\" value=\"2021-11-14 23:52:03\"></parameter>";
//        Document doc = Jsoup.parse(contentText);
//        Elements elements = doc.select("parameter[uuid~=(" + "0e8b09bf-96a6-48fe-a878-48fd259816e0" + ")]");
//        for (Element element : elements) {
//            System.out.println(element);
//            String uuid = element.attr("uuid");
//            System.out.println(uuid);
//            element.removeAttr("uui111d");
//            System.out.println(element);
//        }
//
//        // 日期转大写
//        String enddate=dataToUpper(time);
//        System.out.println("转换之后："+enddate);


    }
}
