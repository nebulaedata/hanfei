//package cn.nebulaedata.service.impl;
//
//
//
//import cn.nebulaedata.service.Neo4jService;
//import cn.nebulaedata.vo.TResponseVo;
//import org.apache.commons.lang.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.Set;
//
///**
// * @author 徐衍旭
// * @date 2022/5/20 14:26
// * @note
// */
//@Service
//public class Neo4jServiceImpl implements Neo4jService {
////    @Autowired
////    MovieRepository movieRepo;
////
////    @Autowired
////    PersonRepository personRepo;
////
////    @Autowired
////    FileIndexEntityRepository fileIndexEntityRepository;
////    @Autowired
////    LabelEntityRepository labelEntityRepository;
////    @Autowired
////    KnowledgeClassEntityRepository knowledgeClassEntityRepository;
////    @Autowired
////    LawsAndRegulationsEntityRepository lawsAndRegulationsEntityRepository;
////    @Autowired
////    LawForceEntityRepository lawForceEntityRepository;
////    @Autowired
////    LawEntityRepository lawEntityRepository;
////
////    /**
////     * 新建一个电影信息
////     *
////     * @param movie@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo newMovieService(Movie movie) throws Exception {
////        movieRepo.save(movie);
////        return TResponseVo.success("新电影创建完成");
////    }
////
////    /**
////     * 删除一个电影信息
////     *
////     * @param movie@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo delMovieService(Movie movie) throws Exception {
////        movieRepo.deleteById(movie.getId());
////        return TResponseVo.success("删除电影完成");
////    }
////
////    /**
////     * 修改一个电影信息
////     *
////     * @param movie@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo updateMovieService(Movie movie) throws Exception {
//////        movieRepo.
////        return null;
////    }
////
////    /**
////     * 查询一个电影信息
////     *
////     * @param movie@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo getMovieInfoService(Movie movie) throws Exception {
////        Movie byTitle = movieRepo.findByTitle(movie.getTitle());
////        return TResponseVo.success(byTitle);
////    }
////
////    /**
////     * 查询电影清单
////     *
////     * @param
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo getMovieListService() throws Exception {
////        Iterable<Movie> all = movieRepo.findAll();
//////        System.out.println("11111111");
////        return TResponseVo.success(all);
////    }
////
////
////    /**
////     * 新增人员信息
////     *
////     * @param person@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo newPersonService(Person person) throws Exception {
////        personRepo.save(person);
////        return TResponseVo.success("新建人物完成");
////    }
////
////    /**
////     * 获取人员信息
////     *
////     * @param person@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo getPersonInfoService(Person person) throws Exception {
////        Person p = personRepo.findByName(person.getName());
////        return TResponseVo.success(p);
////    }
////
////    /**
////     * 查询人员清单
////     *
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo getPersonListService() throws Exception {
////        Iterable<Person> all = personRepo.findAll();
////        return TResponseVo.success(all);
////    }
////
////    /**
////     * 新增参演信息
////     *
////     * @param
////     * @param
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo addActorToMovieService(String name, String title) throws Exception {
////        Person p = personRepo.findByName(name);
////        Movie movie = movieRepo.findByTitle(title);
////        p.addActor(movie);
////        personRepo.save(p);
////        return TResponseVo.success("新增参演信息成功");
////    }
////
////
////    /**
////     * 获取文档清单
////     *
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo getFileIndexListNeoService() throws Exception {
////        Iterable<FileIndexEntity> all = fileIndexEntityRepository.findAll();
////        return TResponseVo.success(all);
////    }
////
////    /**
////     * 新建一个文档信息
////     *
////     * @param
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo newFileIndexNeoService(FileIndexEntity fileIndexEntity) throws Exception {
////        if (StringUtils.isBlank(fileIndexEntity.getFileUuid()) || StringUtils.isBlank(fileIndexEntity.getFileName())) {
////            return TResponseVo.error("必填参数为空");
////        }
////        fileIndexEntityRepository.save(fileIndexEntity);
////        return TResponseVo.success("新文档实体创建完成");
////    }
////
////
////    /**
////     * 新增生成信息
////     *
////     * @param fileUuid1
////     * @param fileUuid2
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo addFileIndexToFileIndexService(String fileUuid1, String fileUuid2) throws Exception {
////        FileIndexEntity f1 = fileIndexEntityRepository.findByFileUuid(fileUuid1);
////        FileIndexEntity f2 = fileIndexEntityRepository.findByFileUuid(fileUuid2);
////        f1.addActor(f2);
////        fileIndexEntityRepository.save(f1);
////        return TResponseVo.success("新增文档使用信息");
////    }
////
////    /**
////     * 删除节点信息
////     *
////     * @param @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo delFileIndexNeoService(String fileUuid) throws Exception {
////        FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
////        Set<LabelEntity> labels = f.getLabels();
////        for (LabelEntity label : labels) {
////            // 同时删除label节点
////            labelEntityRepository.delete(label);
////        }
////        fileIndexEntityRepository.delete(f);
////        return TResponseVo.success("删除");
////    }
////
////    /**
////     * 新建一个标签图节点信息
////     *
////     * @param labelEntity@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo newLabelNeoService(LabelEntity labelEntity) throws Exception {
////        if (StringUtils.isBlank(labelEntity.getLabelUuid()) || StringUtils.isBlank(labelEntity.getLabelContent())) {
////            return TResponseVo.error("必填参数为空");
////        }
////        labelEntityRepository.save(labelEntity);
////        return TResponseVo.success("新文档实体创建完成");
////    }
////
////    /**
////     * 新增关系:标签->模板
////     *
////     * @param labelUuid
////     * @param fileUuid
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo addLabelToFileIndexService(String labelUuid, String fileUuid) throws Exception {
////        FileIndexEntity f = fileIndexEntityRepository.findByFileUuid(fileUuid);
////        LabelEntity l = labelEntityRepository.findByLabelUuid(labelUuid);
////        f.addLabel(l);
////        fileIndexEntityRepository.save(f);
////        return TResponseVo.success("新增关系:标签->模板");
////    }
//
////    /**
////     * 删除USE_IN关系:文档实体间的关系
////     *
////     * @param fileUuid1
////     * @param fileUuid2
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo delFileIndexFromFileIndexService(String fileUuid1, String fileUuid2) throws Exception {
////        String cql = "MATCH (n:FileIndexEntity{fileUuid:'" + fileUuid1 + "'}) <-[r:USE_IN]- (m:FileIndexEntity{fileUuid:'" + fileUuid2 + "'}) delete r";
////        new Neo4jApiUtil().saveNeo4jInfo(cql);
////        return TResponseVo.success("删除关系完成");
////    }
////
////    /**
////     * 删除关系:删除指定文档的所有指定关系
////     *
////     * @param fileUuid1
////     * @param relation
////     * @return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo delFileIndexFromFileIndexAllService(String fileUuid1, String relation) throws Exception {
////        String cql = "MATCH (n:FileIndexEntity{fileUuid:'" + fileUuid1 + "'}) <-[r:"+ relation +"]- (m:FileIndexEntity) delete r";
////        new Neo4jApiUtil().saveNeo4jInfo(cql);
////        return TResponseVo.success("删除关系完成");
////    }
//
//
/////**
//// * 法律法规
////  */
////    /**
////     * 新建一个知识体系分类节点信息
////     *
////     * @param knowledgeClassEntity@return
////     * @throws Exception
////     */
////    @Override
////    public TResponseVo newKnowledgeClassEntityService(KnowledgeClassEntity knowledgeClassEntity) throws Exception {
////        if (StringUtils.isBlank(knowledgeClassEntity.getUuid()) || StringUtils.isBlank(knowledgeClassEntity.getName())) {
////            return TResponseVo.error("必填参数为空");
////        }
////        knowledgeClassEntityRepository.save(knowledgeClassEntity);
////        return TResponseVo.success("新知识体系分类实体创建完成");
////    }
//
//
//}
