package cn.nebulaedata.web.excel.common;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author 贾亦真
 * @date 2022/4/25 09:54
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
@Data
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@AllArgsConstructor
public class CommonExcelModel {

    @ExcelProperty(index = 0)
    private String index0;
    @ExcelProperty(index = 1)
    private String index1;
    @ExcelProperty(index = 2)
    private String index2;
    @ExcelProperty(index = 3)
    private String index3;
    @ExcelProperty(index = 4)
    private String index4;
    @ExcelProperty(index = 5)
    private String index5;
    @ExcelProperty(index = 6)
    private String index6;
    @ExcelProperty(index = 7)
    private String index7;
    @ExcelProperty(index = 8)
    private String index8;
    @ExcelProperty(index = 9)
    private String index9;
    @ExcelProperty(index = 10)
    private String index10;
    @ExcelProperty(index = 11)
    private String index11;
    @ExcelProperty(index = 12)
    private String index12;
    @ExcelProperty(index = 13)
    private String index13;
    @ExcelProperty(index = 14)
    private String index14;
    @ExcelProperty(index = 15)
    private String index15;
    @ExcelProperty(index = 16)
    private String index16;
    @ExcelProperty(index = 17)
    private String index17;
    @ExcelProperty(index = 18)
    private String index18;
    @ExcelProperty(index = 19)
    private String index19;
    @ExcelProperty(index = 20)
    private String index20;
    @ExcelProperty(index = 21)
    private String index21;
    @ExcelProperty(index = 22)
    private String index22;
    @ExcelProperty(index = 23)
    private String index23;
    @ExcelProperty(index = 24)
    private String index24;
    @ExcelProperty(index = 25)
    private String index25;
    @ExcelProperty(index = 26)
    private String index26;
    @ExcelProperty(index = 27)
    private String index27;
    @ExcelProperty(index = 28)
    private String index28;
    @ExcelProperty(index = 29)
    private String index29;
    @ExcelProperty(index = 30)
    private String index30;
    @ExcelProperty(index = 31)
    private String index31;
    @ExcelProperty(index = 32)
    private String index32;
    @ExcelProperty(index = 33)
    private String index33;
    @ExcelProperty(index = 34)
    private String index34;
    @ExcelProperty(index = 35)
    private String index35;
    @ExcelProperty(index = 36)
    private String index36;
    @ExcelProperty(index = 37)
    private String index37;
    @ExcelProperty(index = 38)
    private String index38;
    @ExcelProperty(index = 39)
    private String index39;
    @ExcelProperty(index = 40)
    private String index40;
    @ExcelProperty(index = 41)
    private String index41;
    @ExcelProperty(index = 42)
    private String index42;
    @ExcelProperty(index = 43)
    private String index43;
    @ExcelProperty(index = 44)
    private String index44;
    @ExcelProperty(index = 45)
    private String index45;
    @ExcelProperty(index = 46)
    private String index46;
    @ExcelProperty(index = 47)
    private String index47;
    @ExcelProperty(index = 48)
    private String index48;
    @ExcelProperty(index = 49)
    private String index49;
    @ExcelProperty(index = 50)
    private String index50;
    @ExcelProperty(index = 51)
    private String index51;
    @ExcelProperty(index = 52)
    private String index52;
    @ExcelProperty(index = 53)
    private String index53;
    @ExcelProperty(index = 54)
    private String index54;
    @ExcelProperty(index = 55)
    private String index55;
    @ExcelProperty(index = 56)
    private String index56;
    @ExcelProperty(index = 57)
    private String index57;
    @ExcelProperty(index = 58)
    private String index58;
    @ExcelProperty(index = 59)
    private String index59;
    @ExcelProperty(index = 60)
    private String index60;
    @ExcelProperty(index = 61)
    private String index61;
    @ExcelProperty(index = 62)
    private String index62;
    @ExcelProperty(index = 63)
    private String index63;
    @ExcelProperty(index = 64)
    private String index64;
    @ExcelProperty(index = 65)
    private String index65;
    @ExcelProperty(index = 66)
    private String index66;
    @ExcelProperty(index = 67)
    private String index67;
    @ExcelProperty(index = 68)
    private String index68;
    @ExcelProperty(index = 69)
    private String index69;
    @ExcelProperty(index = 70)
    private String index70;
    @ExcelProperty(index = 71)
    private String index71;
    @ExcelProperty(index = 72)
    private String index72;
    @ExcelProperty(index = 73)
    private String index73;
    @ExcelProperty(index = 74)
    private String index74;
    @ExcelProperty(index = 75)
    private String index75;
    @ExcelProperty(index = 76)
    private String index76;
    @ExcelProperty(index = 77)
    private String index77;
    @ExcelProperty(index = 78)
    private String index78;
    @ExcelProperty(index = 79)
    private String index79;
    @ExcelProperty(index = 80)
    private String index80;
    @ExcelProperty(index = 81)
    private String index81;
    @ExcelProperty(index = 82)
    private String index82;
    @ExcelProperty(index = 83)
    private String index83;
    @ExcelProperty(index = 84)
    private String index84;
    @ExcelProperty(index = 85)
    private String index85;
    @ExcelProperty(index = 86)
    private String index86;
    @ExcelProperty(index = 87)
    private String index87;
    @ExcelProperty(index = 88)
    private String index88;
    @ExcelProperty(index = 89)
    private String index89;
    @ExcelProperty(index = 90)
    private String index90;
    @ExcelProperty(index = 91)
    private String index91;
    @ExcelProperty(index = 92)
    private String index92;
    @ExcelProperty(index = 93)
    private String index93;
    @ExcelProperty(index = 94)
    private String index94;
    @ExcelProperty(index = 95)
    private String index95;
    @ExcelProperty(index = 96)
    private String index96;
    @ExcelProperty(index = 97)
    private String index97;
    @ExcelProperty(index = 98)
    private String index98;
    @ExcelProperty(index = 99)
    private String index99;
    @ExcelProperty(index = 100)
    private String index100;


    public String getItemByReflect(CommonExcelModel dto, int num) {
        try {
            Method m1 = CommonExcelModel.class.getDeclaredMethod(String.format("getIndex%s", num));
            return (String) m1.invoke(dto);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("REFLECT_METHOD_ERROR_EXCEPTION", e);
        }
    }

    public String checkExcelHeader(List<CommonExcelModel> excelData, String excelFlag) {
        if (StringUtils.isBlank(excelFlag)) {
            excelFlag = "";
        }
        CommonExcelModel commonExcelModel = excelData.get(0);
        List<String> excelHeader = new ArrayList<>();
        for(int i = 0; i <= 100 ; i++){
            String itemByReflect = commonExcelModel.getItemByReflect(commonExcelModel, i);
            if(StringUtils.isNotBlank(itemByReflect)){
                excelHeader.add(itemByReflect);
            }
        }
        List<String> tempList = new ArrayList<>();

        return message(tempList, excelHeader);
    }

    public String message(List<String> tempList,List<String> excelHeader){

        boolean flag = false;
        String errorMessage = "上传数据有误,缺少字段信息:";
        for (String tempStr : tempList) {
            if(!excelHeader.contains(tempStr)){
                flag = true;
                errorMessage += tempStr + ",";
            }
        }
        if(!flag){
            errorMessage = "";
        }else {
            errorMessage = errorMessage.substring(0, errorMessage.length()-1) + "!";
        }
        return errorMessage;
    }


}