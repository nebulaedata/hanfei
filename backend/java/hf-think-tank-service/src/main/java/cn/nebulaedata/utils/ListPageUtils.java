package cn.nebulaedata.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/3/31 15:08
 * @note
 */
public class ListPageUtils {

    public List<Map<Object, Object>> test0(List list, Integer pageSize, Integer pageNum) {
        List<Map<Object, Object>> datas = list;

        //总记录数
        Integer totalCount = datas.size();

        //分多少次处理
        Integer requestCount = totalCount / pageSize;

        for (int i = 0; i <= requestCount; i++) {
            Integer fromIndex = i * pageSize;
            //如果总数少于PAGE_SIZE,为了防止数组越界,toIndex直接使用totalCount即可
            int toIndex = Math.min(totalCount, (i + 1) * pageSize);
            List<Map<Object, Object>> subList = datas.subList(fromIndex, toIndex);
            if (i + 1 == pageNum) {
//                System.out.println(subList);
                return subList;
            }
            //总数不到一页或者刚好等于一页的时候,只需要处理一次就可以退出for循环了
            if (toIndex == totalCount) {
                break;
            }
        }
        return new ArrayList<>();
    }
}
