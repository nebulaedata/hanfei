package cn.nebulaedata.globalVariables;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 徐衍旭
 * @date 2023/12/14 15:11
 * @note
 */
@Component
public class RoomServerVariables {

    private static RoomServerVariables instance;

    /**
     * 数据格式
     * {
     *     fileUuid1+fileVersionId1 : {
     *         paramUuid1:[paramText,userId],
     *         paramUuid2:[paramText,userId],
     *     },
     *     fileUuid2+fileVersionId2 : {
     *         paramUuid1:[paramText,userId],
     *         paramUuid2:[paramText,userId],
     *     },
     *     ...
     * }
     */
    private Map<String, Map<String,Object>> roomServerChangeValues;



    private RoomServerVariables() {
        // 初始化全局变量
        roomServerChangeValues = new ConcurrentHashMap<>();
    }

    //单例
    public static RoomServerVariables getInstance() {
        if (instance == null) {
            instance = new RoomServerVariables();
        }
        return instance;
    }

    public Map<String, Map<String,Object>> getRoomServerChangeValues() {
        return roomServerChangeValues;
    }

    // ...
}
