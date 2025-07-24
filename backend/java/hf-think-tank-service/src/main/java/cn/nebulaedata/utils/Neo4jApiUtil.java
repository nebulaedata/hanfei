//package cn.nebulaedata.utils;
//
///**
// * @author 徐衍旭
// * @date 2021/8/23 10:07
// * @note
// */
//
//import com.alibaba.fastjson.JSON;
//import org.neo4j.driver.v1.*;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//
//public class Neo4jApiUtil {
//
//    private static final Logger log = LoggerFactory.getLogger(Neo4jApiUtil.class);
//
//    private Driver createDrive(){
////        return GraphDatabase.driver( "bolt://47.98.211.72:7687", AuthTokens.basic( "neo4j", "nebulae2021" ) );
//        return GraphDatabase.driver( "bolt://116.62.58.225:7687", AuthTokens.basic( "neo4j", "nebulae2021" ) );
//    }
//    private Driver neo4jDriver = createDrive();
//    Session session = null;
//
//    /**
//     * 新增标签到图数据库
//     *
//     * @param createCql CREATE (a:Person {name: {name}, title: {title}})
//     * @return
//     */
//    public void saveNeo4jInfo(String createCql) {
//        try {
//            if (!StringUtils.isEmpty(createCql)) {
//                session = neo4jDriver.session();
//                session.run(createCql);
//            }
//        } catch (Exception e) {
//            log.error("新增图数据库标签异常" + e);
//        } finally {
//            if (session != null) {
//                session.close();
//            }
//        }
//    }
//
//    /**
//     * 查询数据
//     *
//     * @param queryCql MATCH (a:Person) WHERE a.name = {name} RETURN a.name AS name, a.title AS title"
//     * @return
//     */
//    public StatementResult queryNeo4jInfo(String queryCql) {
//        StatementResult result = null;
//        try {
//            if (!StringUtils.isEmpty(queryCql)) {
//                session = neo4jDriver.session();
//                result = session.run(queryCql);
//            }
//        } catch (Exception e) {
//            log.error("查询异常" + e);
//        } finally {
//            if (session != null) {
//                session.close();
//            }
//        }
//        return result;
//    }
//
//    public static void main(String[] args) {
//        Neo4jApiUtil neo4jApiUtil = new Neo4jApiUtil();
////        neo4jApiUtil.saveNeo4jInfo("CREATE (Keanu:Person {name:'Keanu Reeves123', born:1964})");
//        StatementResult statementResult = neo4jApiUtil.queryNeo4jInfo("MATCH (n:Person) RETURN n.name");
//        System.out.println(JSON.toJSONString(statementResult));
//    }
//}