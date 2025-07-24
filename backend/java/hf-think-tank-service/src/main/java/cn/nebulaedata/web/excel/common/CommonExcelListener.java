package cn.nebulaedata.web.excel.common;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 贾亦真
 * @date 2022/4/25 10:57
 * @note
 * @logtemp private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
 */
@Slf4j
public class CommonExcelListener extends AnalysisEventListener<CommonExcelModel> {


    private static final int BATCH_COUNT = 2000;
    List<CommonExcelModel> list = new ArrayList<>();
    private CommonExcelSaveFun uploadDao;


    public CommonExcelListener(CommonExcelSaveFun uploadDao) {
        this.uploadDao = uploadDao;
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        log.info("解析到的表头数据: {}", JSON.toJSONString(headMap));
        HashMap<String, String> resultMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
            resultMap.put("index" + entry.getKey(), entry.getValue());
        }
        CommonExcelModel result = JSON.parseObject(JSON.toJSONString(resultMap), CommonExcelModel.class);
        list.add(result);
    }

    @Override
    public void invoke(CommonExcelModel ccQuestionExcelModel, AnalysisContext analysisContext) {
        list.add(ccQuestionExcelModel);
        if (list.size() >= BATCH_COUNT) {
            uploadDao.putDatas(list);
            list.clear();
        }
        if (uploadDao.getSheetName() == null) {
            uploadDao.setSheetName(analysisContext.readSheetHolder().getSheetName());
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        uploadDao.putDatas(list);
    }


}