package cn.nebulaedata.scheduled;

import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.service.impl.FileOperationServiceImpl;
//import cn.nebulaedata.service.impl.Neo4jServiceImpl;
import cn.nebulaedata.utils.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author 徐衍旭
 * @date 2021/11/15 15:44
 * @note
 */
@Configuration      //1.主要用于标记配置类，兼备Component的效果。
@EnableScheduling   // 2.开启定时任务
public class ParamDailyTask {

    @Autowired
    private FileOperationMapper fileOperationMapper;

    @Autowired
    private RedisUtils redisUtils;

//    @Autowired
//    private Neo4jServiceImpl neo4jServiceImpl;

    @Autowired
    private FileOperationServiceImpl fileOperationService;


    @Value("${doc-frame-service.env-name}")
    private String envName;

    //1.清除过期参数 标注 书签
    @Scheduled(cron = "0 0 1 * * ?")  //每天凌晨1点执行一次
//    @PostConstruct
    //或直接指定时间间隔，例如：5秒
//    @Scheduled(fixedRate=5000)
    private void configureTasks() {
        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -7);
        Date yesterday = cal.getTime();
        SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
        String yesterday1 = sp.format(yesterday);//获取昨天日期

        System.out.println(yesterday1);
        Integer integer = fileOperationMapper.deleteParamDailyDao(yesterday1);
        System.out.println("清除了" + integer + "条过期参数");
        integer = fileOperationMapper.deleteTagDailyDao(yesterday1);
        System.out.println("清除了" + integer + "条过期标注");
        integer = fileOperationMapper.deleteBookmarkDailyDao(yesterday1);
        System.out.println("清除了" + integer + "条过期书签");
    }

    //TODO 定时删除数据管理
    //2.定时清除数据管理
    @Scheduled(cron = "0 0 2 * * ?")  //每天凌晨2点执行一次
//    @PostConstruct
    //或直接指定时间间隔，例如：5秒
//    @Scheduled(fixedRate=5000)
    private void configureTasks2() {
        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);
        Date yesterday = cal.getTime();
        SimpleDateFormat sp = new SimpleDateFormat("yyyy-MM-dd");
        String yesterday1 = sp.format(yesterday);//获取昨天日期

        System.out.println(yesterday1);
        List<String> tableList = fileOperationMapper.getDelDmTableListDao(yesterday1);
        if (tableList.size() != 0) {
            Integer integer = fileOperationMapper.clearDmTableDailyDao(tableList);
            System.out.println("清除了" + integer + "条过期数据管理表");
            integer = fileOperationMapper.clearDmColumnsDailyDao(tableList);
            System.out.println("清除了" + integer + "条过期数据管理表头");
            integer = fileOperationMapper.clearDmDataDailyDao(tableList);
            System.out.println("清除了" + integer + "条过期数据管理表数据");
        } else {
            System.out.println("数据管理无需清理");
        }
    }

    //TODO 定时删除参数填写历史
//    //3.添加定时任务
//    @Scheduled(cron = "0 0 3 * * ?")  //每天凌晨3点执行一次
////    @PostConstruct
//    //或直接指定时间间隔，例如：5秒
////    @Scheduled(fixedRate=5000)
//    private void configureTasks2() {
//        System.err.println("执行静态定时任务时间: " + LocalDateTime.now());
//
//        Calendar cal= Calendar.getInstance();
//        cal.add(Calendar.DATE,-7);
//        Date yesterday=cal.getTime();
//        SimpleDateFormat sp=new SimpleDateFormat("yyyy-MM-dd");
//        String yesterday1=sp.format(yesterday);//获取昨天日期
//
//        System.out.println(yesterday1);
//        Integer integer = fileOperationMapper.(yesterday1);
//        System.out.println("清除了"+integer+"条过期数据管理数据");
//    }

//    //3.定时向neo4j发送心跳
//    @Scheduled(cron = "0 */2 * * * ?")  //每隔两分钟执行一次
////    @PostConstruct
//    //或直接指定时间间隔，例如：5秒
////    @Scheduled(fixedRate=5000)
//    private void configureTasks3() throws Exception {
//        System.err.println("Neo4j: " + LocalDateTime.now() + " heartBeat");
//        neo4jServiceImpl.getMovieListService();
//    }

    //4.定时向redis发送心跳
    @Scheduled(cron = "0 */2 * * * ?")  //每隔两分钟执行一次
//    @PostConstruct
    //或直接指定时间间隔，例如：5秒
//    @Scheduled(fixedRate=5000)
    private void configureTasks4() {
        System.err.println("Redis: " + LocalDateTime.now() + " heartBeat");
        redisUtils.set(envName + "_redis_heartBeat", LocalDateTime.now(), -1);
    }

    //5.定时向redis发送心跳
    @Scheduled(cron = "0 */5 7-21 * * ?")  //每天7-21点 每隔5分钟执行一次
//    @PostConstruct
    //或直接指定时间间隔，例如：5秒
//    @Scheduled(fixedRate=5000)
    private void configureTasks5() throws Exception {
        System.err.println("LawExtractText: " + LocalDateTime.now());
        String lawExtractTextListStr = fileOperationService.getLawExtractTextListsService(10000);
        // 写入缓存  5分钟更新一次
        redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 300);
    }

}
