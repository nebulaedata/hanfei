//package cn.nebulaedata.utils;
//
//
//import cn.nebulaedata.dao.FileOperationMapper;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import org.apache.commons.collections4.MapUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.text.SimpleDateFormat;
//import java.util.*;
//
///**
// * @author 贾亦真
// * @date 2020/12/21 09:54
// * @note
// */
//
//@Component
//public class DocUtils {
//
//    @Autowired
//    private FileOperationMapper fileOperationMapper;
//
//    public static DocUtils docUtils;
//
//    @PostConstruct
//    public void init() {
//        docUtils = this;
//        docUtils.fileOperationMapper = this.fileOperationMapper;
//
//    }
//
//    /**
//     * 通知子文件变更(并非直接通知 而是将变化记录在hf_update_info表中)
//     *
//     * @param fileUuid
//     * @return
//     */
//    public String newUpdateInfo(String fileUuid, List<Map<String, String>> updateInfoList, String userId) {
//
//        // 获取需要通知的子文件清单
//        List<String> pushFileUuidList = fileOperationMapper.getPushFileUuidInfoDao(fileUuid);
//        String sql = "";
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        String dateString = formatter.format(new Date());
//        for (Map<String, String> updateInfo : updateInfoList) {
//            for (String pushFileUuid : pushFileUuidList) {
//                sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "") + "\",\"" + pushFileUuid + "\",\"" + updateInfo.get("updateOutlineId") + "\",\"" + updateInfo.get("updateOutlineIdAct") + "\",\"" + userId + "\",\"" + dateString + "\"),";
//            }
//        }
//        sql = sql.substring(0, sql.length() - 1);
//        fileOperationMapper.insertUpdateInfoDao(sql);
//        return "已通知子文件更新";
//
//    }
//
//    public static void main(String[] args) {
//        List<Map<String, String>> updateInfoList = new ArrayList<>();
//        HashMap<String, String> map = new HashMap<>();
//        map.put("updateOutlineId","13");
//        map.put("updateOutlineIdAct","修改");
//        updateInfoList.add(map);
//        new DocUtils().newUpdateInfo("1919",updateInfoList,"110");
//    }
//
//}
