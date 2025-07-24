//package cn.nebulaedata.controller;
//
//import cn.nebulaedata.entity2.UserNode;
//import cn.nebulaedata.service.impl.Neo4jServiceImpl;
//import cn.nebulaedata.vo.TResponseVo;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
///**
// * @author 徐衍旭
// * @date 2022/5/20 14:49
// * @note
// */
//@Controller
//@RequestMapping("/neo")
//public class Neo4jController {
//    @Autowired
//    private Neo4jServiceImpl neo4jServiceImpl;
//
//    @PostMapping("/getMovieList")
//    public TResponseVo getMovieList() throws Exception {
//        return neo4jServiceImpl.getMovieListService();
//    }
//}
