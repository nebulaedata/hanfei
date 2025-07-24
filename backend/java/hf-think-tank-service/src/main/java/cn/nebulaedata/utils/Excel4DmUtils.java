package cn.nebulaedata.utils;

import cn.nebulaedata.pojo.DocframeParamsUnitDiPojo;
import cn.nebulaedata.pojo.HfDmColumns;
import cn.nebulaedata.pojo.HfDmData;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.sql.Types.BOOLEAN;
import static org.apache.poi.ss.usermodel.DataValidationConstraint.ValidationType.FORMULA;
import static org.apache.tomcat.util.bcel.classfile.ElementValue.STRING;

@Slf4j
public class Excel4DmUtils {
    /**
     * 上传读excel
     */
    private final static String excel2003L = ".xls";    //2003- 版本的excel
    private final static String excel2007U = ".xlsx";  //2007版本

    /**
     * @param work k = 0 获取模型数据
     *             k = 1 获取模型表头
     * @return
     * @throws
     * @Description：获取IO流中的数据，组装成List<List<Object>>对象
     */
//    public static List<List<String>> getListByExcel(File file, int k) throws Exception {
    public static List<List<String>> getListByExcel(Workbook work, int k) throws Exception {
        List<List<String>> list = null;

        //创建Excel工作薄
//        Workbook work = getWorkbook(inputStream);
        if (null == work) {
            throw new Exception("创建Excel工作薄为空！");
        }
        Sheet sheet = null;  //页数
        Row row = null;  //行数
        Cell cell = null;  //列数

        list = new ArrayList<List<String>>();
        //遍历Excel中所有的sheet
        for (int i = 0; i < 1; i++) {
//        for (int i = 0; i < work.getNumberOfSheets(); i++) {
            sheet = work.getSheetAt(k);
            if (sheet == null) {
                continue;
            }

            //遍历当前sheet中的所有行
            for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
                row = sheet.getRow(j);
                if (row == null) {
                    continue;
                }

                //遍历所有的列
                List<String> li = new ArrayList<String>();
                for (int y = 0; y < row.getLastCellNum(); y++) {
                    try {
                        cell = row.getCell(y);
//                        System.out.println(String.valueOf(y) + " " + cell.toString());
                        li.add(getValue(cell));
                    } catch (Exception e) {
                        li.add(null);
                    }
                }
                list.add(li);

            }
        }
        return list;
    }

    public static String getSheetName(Workbook work, int k) throws Exception {
        //创建Excel工作薄
//        Workbook work = getWorkbook(inputStream);
        if (null == work) {
            throw new Exception("创建Excel工作薄为空！");
        }
        return work.getSheetAt(k).getSheetName();
    }


    /**
     * @param inputStream
     * @return
     * @throws Exception
     * @Description：根据文件后缀，自适应上传文件的版本
     */
    public static Workbook getWorkbook(InputStream inputStream, String extension) throws Exception {
        Workbook workbook = null;
        if (".xlsx".equals(extension)) {
            try {
                workbook = new XSSFWorkbook(inputStream);
                System.out.println("1111");
            } catch (Exception e) {
                log.error("1111111111", e);
            }
        } else {
            try {
                workbook = new HSSFWorkbook(inputStream);
                System.out.println("1111");
            } catch (Exception e) {
                log.error("1111111111", e);
            }
        }
        return workbook;
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
            //数值型(包括时间)
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {  // 时间
                    //如果是date类型则 ，获取该cell的date值
                    Date date = DateUtil.getJavaDate(cell.getNumericCellValue());
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    value = format.format(date);
                } else { // 纯数字
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
                try {
                    value = cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        value = String.valueOf(cell.getNumericCellValue());
                        //解决1234.0  去掉后面的.0
                        if (null != value && !"".equals(value.trim())) {
                            String[] item = value.split("[.]");
                            if (1 < item.length && "0".equals(item[1])) {
                                value = item[0];
                            }
                        }
                    } catch (Exception e2) {
                        cell.getErrorCellValue();
                        value = "";
                    }
                }
//                value = ((XSSFCell) cell).getRawValue();
//                if (value.equals("NaN")) {// 如果获取的数据值为非法值,则转换为获取字符串
//                    value = cell.getStringCellValue();
//                }
                break;
            // 空值
//            case 3:
//                value = " " + cell.getBooleanCellValue();
//                break;
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

//    public static List<String> getColumnType(File file, int k) throws Exception {
//        // 判断列的类型 除空外全为同一种类型 就判断为该类型  否则一律按照string处理
//        List<List<String>> list = null;
//
//        //创建Excel工作薄
//        Workbook work = getWorkbook(file);
//        if (null == work) {
//            throw new Exception("创建Excel工作薄为空！");
//        }
//        Sheet sheet = null;  //页数
//        Row row = null;  //行数
//        Cell cell = null;  //列数
//
//        list = new ArrayList<List<String>>();
//        //遍历Excel中所有的sheet
//        for (int i = 0; i < 1; i++) {
////        for (int i = 0; i < work.getNumberOfSheets(); i++) {
//            sheet = work.getSheetAt(k);
//            if (sheet == null) {
//                continue;
//            }
//
//            //遍历当前sheet中的所有行
//            for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++) {
//                row = sheet.getRow(j);
//                if (row == null) {
//                    continue;
//                }
//
//                //遍历所有的列
//                List<String> li = new ArrayList<String>();
//                for (int y = 0; y < row.getLastCellNum(); y++) {
//                    try {
//                        cell = row.getCell(y);
////                        System.out.println(String.valueOf(y) + " " + cell.toString());
//                        li.add(getValue(cell));
//                    } catch (Exception e) {
//                        li.add(null);
//                    }
//                }
//                list.add(li);
//
//            }
//        }
//        return null;
//    }


    /**
     * 将数据写入excel
     *
     * @param dmTableColumnsList
     * @param dmDataList
     * @throws UnsupportedEncodingException
     */
    public synchronized static HSSFWorkbook writeExcel(String tableName, List<HfDmColumns> dmTableColumnsList, List<HfDmData> dmDataList, List<DocframeParamsUnitDiPojo> unitDi, String remark) throws UnsupportedEncodingException {
        HashMap<String, String> typeMap = new HashMap<String, String>();  // 快捷记录类型
        // 创建Excel文件薄
        HSSFWorkbook workbook = new HSSFWorkbook();

        HSSFCellStyle setBorderTitle = workbook.createCellStyle();
        setBorderTitle.setBorderBottom(BorderStyle.THIN); //下边框
        setBorderTitle.setBorderLeft(BorderStyle.THIN);//左边框
        setBorderTitle.setBorderTop(BorderStyle.THIN);//上边框
        setBorderTitle.setBorderRight(BorderStyle.THIN);//右边框
        HSSFFont fontTitle = workbook.createFont();
        fontTitle.setFontName("黑体");
        fontTitle.setFontHeightInPoints((short)11);
        fontTitle.setBold(true);  // 加粗
        setBorderTitle.setFont(fontTitle);
//        setBorderTitle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());//单元格背景色
//        setBorderTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);//单元格填充效果

        HSSFCellStyle setBorderData = workbook.createCellStyle();
        setBorderData.setBorderBottom(BorderStyle.THIN); //下边框
        setBorderData.setBorderLeft(BorderStyle.THIN);//左边框
        setBorderData.setBorderTop(BorderStyle.THIN);//上边框
        setBorderData.setBorderRight(BorderStyle.THIN);//右边框
        HSSFFont fontData = workbook.createFont();
        fontData.setFontName("仿宋_GB2312");
        setBorderData.setFont(fontData);

        // 创建工作表sheet
        HSSFSheet sheet0 = workbook.createSheet(tableName);  // 记录数据信息
        // 生成表头
        HSSFRow rowColumns = sheet0.createRow(0);
        HashMap<String, Integer> dataIndexAndColMap = new HashMap<>();
        for (int i = 0; i < dmTableColumnsList.size(); i++) {
            sheet0.setColumnWidth(i,5000);
            // title
            HSSFCell cell = rowColumns.createCell(i);
            cell.setCellValue(dmTableColumnsList.get(i).getTitle());
            cell.setCellStyle(setBorderTitle);
            // 保存index和列映射
            dataIndexAndColMap.put(dmTableColumnsList.get(i).getDataIndex(), i);
            // 保存类型
            typeMap.put(dmTableColumnsList.get(i).getDataIndex(), dmTableColumnsList.get(i).getFieldType());
        }

        // 追加数据
        for (int j = 0; j < dmDataList.size(); j++) {
            HSSFRow rowData = sheet0.createRow(j + 1);
            // 先按照表宽度 设置好表样
            for (int i = 0; i < dmTableColumnsList.size(); i++) {
                HSSFCell cell = rowData.createCell(i);
                cell.setCellStyle(setBorderData);
            }
            String dataContent = (String) dmDataList.get(j).getDataContent();
            Map<String, Object> dataContentMap = JSON.parseObject(dataContent, Map.class);
            for (String dataIndex : dataContentMap.keySet()) {
                Integer colNumber = dataIndexAndColMap.get(dataIndex); // 获取dataIndex所在列
                if (colNumber != null) {  // data中的dataIndex数量可能多于columns 所以匹配不到的pass掉
                    String fieldType = typeMap.get(dataIndex);
                    String unitName = "";
                    HSSFCell cell = null;
                    try {
                        cell = rowData.getCell(colNumber);
                    } catch (Exception e) {
                        cell = rowData.createCell(colNumber);
                    }
                    if ("20".equals(fieldType)) {  // 多选
                        cell.setCellValue(new JsonKeyUtils().listToString(JSON.parseObject(String.valueOf(dataContentMap.get(dataIndex)),List.class),";"));
//                        cell.setCellStyle(setBorderData);
                    } else if ("80".equals(fieldType) || "90".equals(fieldType)) { // 数值 金额
                        Map<String,String> map = JSON.parseObject(String.valueOf(dataContentMap.get(dataIndex)), Map.class); // 结构{unit:value}
                        // unit
                        Set<String> unitList = map.keySet();
                        for (String unit : unitList) {  // 只循环一次
                            for (DocframeParamsUnitDiPojo unitDiPojo : unitDi) {
                                if (unit.equals(unitDiPojo.getUnitId())) {
                                    unitName = unitDiPojo.getUnitName();
                                }
                            }
                            break;
                        }
                        // value
                        for (String unit : unitList) {  // 只循环一次
                            String value = String.valueOf(map.get(unit));
                            cell.setCellValue(value+unitName);
//                            cell.setCellStyle(setBorderData);
                        }
                    } else {
                        // 不用动
                        cell.setCellValue(String.valueOf(dataContentMap.get(dataIndex)));
//                        cell.setCellStyle(setBorderData);
                    }
                }
            }
        }



        // 追加备注
        if (StringUtils.isNotBlank(remark)) {
            HSSFCellStyle setBorderRemark = workbook.createCellStyle();
            HSSFFont fontRemark = workbook.createFont();
            fontRemark.setFontName("仿宋_GB2312");
            fontRemark.setBold(true);  // 加粗
            setBorderRemark.setFont(fontRemark);
            setBorderRemark.setWrapText(true);  // 自动换行
            setBorderRemark.setBorderBottom(BorderStyle.DOUBLE); //下边框
            setBorderRemark.setAlignment(HorizontalAlignment.LEFT); // 设置水平居左对齐
            setBorderRemark.setVerticalAlignment(VerticalAlignment.CENTER); // 设置垂直居中


            HSSFRow rowRemark = sheet0.createRow(dmDataList.size() + 1);
            rowRemark.setHeight((short)2000);
            HSSFCell cell = rowRemark.createCell(0);
            cell.setCellValue(remark);
            cell.setCellStyle(setBorderRemark);
            for (int i = 1; i < dmTableColumnsList.size(); i++) {
                cell = rowRemark.createCell(i);
                cell.setCellStyle(setBorderRemark);
            }

            // 合并单元格
            CellRangeAddress region = new CellRangeAddress(
                    dmDataList.size() + 1, // first row
                    dmDataList.size() + 1, // last row
                    0, // first column
                    dmTableColumnsList.size()-1  // last column
            );
            sheet0.addMergedRegion(region);
        }

        return workbook;
    }


    /**
     * 导出excel
     *
     * @param workbook
     * @param response
     */
    public synchronized static void exportExcel(String tableName, HSSFWorkbook workbook, HttpServletResponse response) {
        //response为HttpServletResponse对象
        try {
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(tableName + "_" + new TimeFormatUtils().now(), "UTF-8") + ".xls");
            response.setContentType("application/msexcel");
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * 下载至path 命名为fileName.xls
     * @param path 保存的路径
     * @param fileName 保存的文件名
     * @param workbook
     */
    public synchronized static void savePathExcel(String path, String fileName, HSSFWorkbook workbook) {
        try {
            // 创建文件夹
            File filePathNew = new File(path);
            if (!filePathNew.exists()) {
                filePathNew.mkdir();
            }
            System.out.println(path);
            FileOutputStream fout = new FileOutputStream(path + "/" + fileName + ".xls");
            workbook.write(fout);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {
//        File file = new File("C:\\Users\\xuyanxu\\Desktop\\电容器.xls");
//        List<List<String>> listByExcel = getListByExcel(file, 0);
//        System.out.println(JSON.toJSONString(listByExcel));

//        File file = new File("C:\\Users\\xuyanxu\\Desktop\\电容器.xls");
//        getColumnType(file, 0);

    }
}
