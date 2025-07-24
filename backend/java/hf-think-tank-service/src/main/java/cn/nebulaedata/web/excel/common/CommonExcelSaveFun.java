package cn.nebulaedata.web.excel.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 贾亦真
 * @date 2022/4/25 10:31
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
@Component
public class CommonExcelSaveFun {

    private String sheetName;
    private List<CommonExcelModel> excelData = new ArrayList<>();

    public List<CommonExcelModel> getExcelData() {
        return excelData;
    }

    public void clearData() {
        excelData = new ArrayList<>();
    }

    public void putDatas(List<CommonExcelModel> once) {
        excelData.addAll(once);
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public String getSheetName() {
        return sheetName;
    }

}