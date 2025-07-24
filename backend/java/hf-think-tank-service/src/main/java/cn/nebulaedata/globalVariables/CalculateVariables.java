package cn.nebulaedata.globalVariables;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2023/12/14 15:11
 * @note
 */
@Component
public class CalculateVariables {

    private static CalculateVariables instance;


    private Map<String, Map<String,Object>> calculateValues;



    private CalculateVariables() {
        // 初始化全局变量
        calculateValues = new HashMap<>();
    }

    public static CalculateVariables getInstance() {
        if (instance == null) {
            instance = new CalculateVariables();
        }
        return instance;
    }

    public Map<String, Map<String,Object>> getCalculateValues() {
        return calculateValues;
    }

    // ...
}
