package cn.nebulaedata.utils;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.sql.Types.BOOLEAN;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.FORMULA;
import static org.apache.tomcat.util.bcel.classfile.ElementValue.STRING;

/**
 * @author 徐衍旭
 * @date 2022/8/11 14:57
 * @note
 */
public class ExcelUtils {

    /**
     * 将数据写入excel
     *
     * @param dataIndexList
     * @param dataTypeIdList
     * @param titleList
     * @param aliasList
     * @param unitIdList
     * @param dataList
     * @param k
     * @param response
     * @throws UnsupportedEncodingException
     */
    public synchronized static void writeExcel(List<String> dataIndexList, List<String> dataTypeIdList, List<String> titleList, List<String> aliasList, List<String> unitIdList, List<Map<String, Object>> dataList, int k, HttpServletResponse response) throws UnsupportedEncodingException {
        //创建Excel文件薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFCellStyle lockstyle = workbook.createCellStyle(); // 定义上锁样式
        lockstyle.setLocked(true);
        lockstyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());//设置上锁的单元格背景色
//        lockstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);//单元格填充效果

        HSSFCellStyle unlockStyle = workbook.createCellStyle();  // 定义未上锁样式
        unlockStyle.setLocked(false);//设置未锁定
        //创建工作表sheeet
        HSSFSheet sheet0 = workbook.createSheet();  // 记录数据信息
        for (int i = 0; i <= 100; i++) {
            sheet0.setDefaultColumnStyle(i, unlockStyle);
        }
        HSSFSheet sheet1 = workbook.createSheet();  // 记录表头信息
        sheet1.protectSheet("nebulae2022");  // 锁定单元格保护
        HSSFCell cell_title = null;
        // 设置数据主键
        for (int j = 0; j < 1; j++) {
            //创建第j+1行
            HSSFRow row = sheet0.createRow(j);
            cell_title = row.createCell(0);
            cell_title.setCellStyle(lockstyle);
            cell_title.setCellValue("实例名称");
            for (int i = 0; i < dataIndexList.size(); i++) {
                cell_title = row.createCell(i + 1);
                cell_title.setCellStyle(lockstyle);
                cell_title.setCellValue(dataIndexList.get(i));
            }
        }
        //追加数据
        for (int j = 0; j < dataList.size(); j++) {
            Map<String, Object> data = dataList.get(j);
            List<Object> valueList = (List) data.get("valueList");
            HSSFRow nextrow = sheet0.createRow(j + 1); // 数据从第2行开始录入
            HSSFCell cell_value_name = nextrow.createCell(0);
            cell_value_name.setCellStyle(unlockStyle);
            cell_value_name.setCellValue(String.valueOf(data.get("formName")));  // 实例名
            for (int i = 0; i < dataIndexList.size(); i++) {
                HSSFCell cell_value = nextrow.createCell(i + 1);
                cell_value.setCellStyle(unlockStyle);
                cell_value.setCellValue(valueList.get(i)==null?"":String.valueOf(valueList.get(i)));
            }
        }
        sheet0.protectSheet("nebulae2022");

        // 在另一张sheet中保存表头
        for (int j = 0; j < 5; j++) {
            //创建第j+1行
            HSSFRow row = sheet1.createRow(j);
            for (int i = 0; i < dataIndexList.size(); i++) {
                cell_title = row.createCell(i);
                if (j == 0) {
                    cell_title.setCellValue(dataIndexList.get(i));
                } else if (j == 1) {
                    cell_title.setCellValue(dataTypeIdList.get(i));
                } else if (j == 2) {
                    cell_title.setCellValue(titleList.get(i));
                } else if (j == 3) {
                    cell_title.setCellValue(aliasList.get(i));
                } else if (j == 4) {
                    cell_title.setCellValue(unitIdList.get(i));
                }
            }
        }

        workbook.setSheetHidden(1,true); // 隐藏第二个sheet

        //response为HttpServletResponse对象
        try {
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("fileName", "UTF-8") + ".xls");
            response.setContentType("application/msexcel");
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }


    /**
     * 上传读excel
     */
    private final static String excel2003L = ".xls";    //2003- 版本的excel
    private final static String excel2007U = ".xlsx";  //2007版本

    /**
     * @param in,fileName k = 0 获取模型数据
     *                    k = 1 获取模型表头
     * @return
     * @throws
     * @Description：获取IO流中的数据，组装成List<List<Object>>对象
     */
    public static List<List<String>> getListByExcel(InputStream in, String fileName, int k) throws Exception {
        List<List<String>> list = null;

        //创建Excel工作薄
        Workbook work = getWorkbook(in, fileName);
        if (null == work) {
            throw new Exception("创建Excel工作薄为空！");
        }
        Sheet sheet = null;  //页数
        Row row = null;  //行数
        Cell cell = null;  //列数

        list = new ArrayList<List<String>>();

        sheet = work.getSheetAt(k);
        if (sheet == null) {
            return list;
        }

        //遍历当前sheet中的所有行
        for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
//            if (k == 0 && j == 1) {
//                continue;
//            } // 把第一行的dataIndex拿出来 并过滤掉第二行的title

            row = sheet.getRow(j);
            if (row == null) {
                continue;
            }

            //遍历所有的列
            List<String> li = new ArrayList<String>();
            for (int y = 0; y < row.getLastCellNum(); y++) {
                try {
                    cell = row.getCell(y);
                    System.out.println(String.valueOf(y) + " " + cell.toString());
                    li.add(getValue(cell));
                } catch (Exception e) {
                    li.add(null);
                }
            }
            list.add(li);

        }

//        System.out.println("list : " + JSON.toJSONString(list));

        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;

    }

    /**
     * @param inStr,fileName
     * @return
     * @throws Exception
     * @Description：根据文件后缀，自适应上传文件的版本
     */
    public static Workbook getWorkbook(InputStream inStr, String fileName) throws Exception {
        Workbook wb = null;
        String fileType = fileName.substring(fileName.lastIndexOf("."));
        if (excel2003L.equals(fileType)) {
            wb = new HSSFWorkbook(inStr);  //2003-
        } else if (excel2007U.equals(fileType)) {
            wb = new XSSFWorkbook(inStr);  //2007+
        } else {
            throw new Exception("解析的文件格式有误！");
        }
        return wb;
    }

    /**
     * @param cell
     * @return
     * @Description：对表格中数值进行格式化
     */
    //解决excel类型问题，获得数值
    public static String getValue(Cell cell) {
        String value = "";
        if (null == cell) {
            return value;
        }
        switch (cell.getCellType()) {
            //数值型
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    //如果是date类型则 ，获取该cell的date值
                    Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    value = format.format(date);
                } else {// 纯数字
                    BigDecimal big = new BigDecimal(cell.getNumericCellValue());
                    value = big.toString();
                    //解决1234.0  去掉后面的.0
                    if (null != value && !"".equals(value.trim())) {
                        String[] item = value.split("[.]");
                        if (1 < item.length && "0".equals(item[1])) {
                            value = item[0];
                        }
                    }
                }
                break;
            //字符串类型
            case STRING:
                value = cell.getStringCellValue();
                break;
            // 公式类型
            case FORMULA:
                //读公式计算值
                value = String.valueOf(cell.getNumericCellValue());
                if (value.equals("NaN")) {// 如果获取的数据值为非法值,则转换为获取字符串
                    value = cell.getStringCellValue();
                }
                break;
            // 布尔类型
            case BOOLEAN:
                value = " " + cell.getBooleanCellValue();
                break;
            default:
                value = cell.getStringCellValue();
        }
        if ("null".endsWith(value.trim())) {
            value = "";
        }
        return value;
    }

    public static void main(String[] args) throws Exception {

        File file = new File("C:\\Users\\xuyanxu\\Downloads\\模拟一个模型 (1).xls");
        InputStream in = new FileInputStream(file);
        getListByExcel(in, "fileName.xls", 0);


//        ArrayList<Object> dataIndexList = new ArrayList<>();
//        dataIndexList.add("1");
//        dataIndexList.add("2");
//        dataIndexList.add("3");
//        //创建Excel文件薄
//        HSSFWorkbook workbook = new HSSFWorkbook();
//        //创建工作表sheeet
//        HSSFSheet sheet = workbook.createSheet();
//        HSSFSheet sheet2 = workbook.createSheet();
//        sheet.protectSheet("123");  // 锁定单元格保护
//        HSSFCell cell_title = null;
//        for (int j = 0; j < 4; j++) {
//            //创建第j+1行
//            HSSFRow row = sheet.createRow(j);
//            for (int i = 0; i < dataIndexList.size(); i++) {
//                cell_title = row.createCell(i);
//
//            }
//        }
//
//
//        //创建一个文件
//        try {
//            File file = new File("C:\\Users\\xuyanxu\\Desktop\\excel\\poi_test_1.xls");
//            file.createNewFile();
//            FileOutputStream stream = FileUtils.openOutputStream(file);
//            workbook.write(stream);
//            stream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

}
