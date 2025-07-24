package cn.nebulaedata.controller;

//import cn.nebulaedata.entity.Movie;
//import cn.nebulaedata.entity.Person;

import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.form.CurrencyForm;
import cn.nebulaedata.form.DocParamsForm;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.FileOperationService;
import cn.nebulaedata.service.impl.WorkingTableServiceImpl;
import cn.nebulaedata.utils.*;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

//import cn.nebulaedata.relation.MovieRepository;
//import cn.nebulaedata.relation.PersonRepository;
//import cn.nebulaedata.utils.DocUtils;

/**
 * @author 徐衍旭
 * @date 2021/8/12 13:48
 * @note
 */
@RestController
@RequestMapping("/web/file/")
public class FileOperationController {
    private static final Logger LOG = LoggerFactory.getLogger(FileOperationController.class);

    @Autowired
    private WorkingTableServiceImpl workingTableServiceImpl;
    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private FileOperationService fileOperationService;
    @Value("${doc-frame-service.upload-path}")
    private String filePath;
    @Value("${doc-frame-service.doc2html-path}")
    private String htmlPath;
    @Value("${doc-frame-service.docUnzip-path}")
    private String docUnzipPath;
    @Value("${doc-frame-service.static-resource-path}")
    private String staticResourcePath;
    @Value("${doc-frame-service.pic-path}")
    private String picPath;
    @Value("${doc-frame-service.annex-path}")
    private String annexPath;
    @Value("${doc-frame-service.env-name}")
    private String envName;
    @Value("${do-post.dfb-url}")
    private String dfbUrl;
//    @Resource
//    public MovieRepository movieRepo;
//    @Resource
//    public PersonRepository personRepo;

//    @GetMapping("show")
//    public void show() {
//        System.out.println("123");
//        Movie m1 = new Movie("无问西东", "2018");
//        Movie m2 = new Movie("罗曼蒂克消亡史", "2016");
//        movieRepo.save(m1);
//        movieRepo.save(m2);
//    }
//
//    @GetMapping("show1")
//    public void testSavePerson() {
//        Person p1 = new Person("章子怡", "1979");
//        Person p2 = new Person("李芳芳", "1976");
//        Person p3 = new Person("程耳", "1970");
//        Movie m1 = movieRepo.findByTitle("罗曼蒂克消亡史");
//        Movie m2 = movieRepo.findByTitle("无问西东");
//        if (m1 != null) {
//            p1.addActor(m1);
//            p3.addDirector(m1);
//        }
//        if (m2 != null) {
//            p1.addActor(m2);
//            p2.addDirector(m2);
//        }
//        personRepo.save(p1);
//        personRepo.save(p2);
//        personRepo.save(p3);
//    }
//
//    @GetMapping("show2")
//    public TResponseVo testfindByTitle() {
//        Movie movie = movieRepo.findByTitle("罗曼蒂克消亡史");
//        System.out.println(movie);
//        return TResponseVo.success(movie);
//    }
//
//    @GetMapping("show3")
//    public TResponseVo testfindByName() {
//        Person person = personRepo.findByName("章子怡");
//        return TResponseVo.success(person);
//    }

    /**
     * 收藏
     *
     * @param fileUuid
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/addCollection")
    public TResponseVo addCollection(String fileUuid, String collectionType, HttpSession session) throws Exception {
        DocUserPojo userId = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(userId.getUserId()) || StringUtils.isBlank(collectionType)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.addCollectionService(userId.getUserId(), fileUuid, collectionType);
    }

    /**
     * 取消收藏
     *
     * @param fileUuid
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/delCollection")
    public TResponseVo delCollection(String fileUuid, HttpSession session) throws Exception {
        DocUserPojo userId = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(userId.getUserId())) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.delCollectionService(userId.getUserId(), fileUuid);
    }


    @GetMapping("/mycollection")
    public TResponseVo mycollection(CurrencyForm currencyForm, String collectionType, HttpSession session) throws Exception {
        String isPaged = currencyForm.getIsPaged();
        Integer pageNum = currencyForm.getPageNum();
        Integer pageSize = currencyForm.getPageSize();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        String paramNameLike = currencyForm.getParamNameLike();
        return fileOperationService.myCollectionService(collectionType, pageNum, pageSize, isPaged, userId, paramNameLike);
    }

    /**
     * 新增参数至参数库
     *
     * @param docParamsPojo
     * @param session
     * @return
     * @throws Exception
     */
    @PostMapping("/addParam")
    public TResponseVo addParam(@RequestBody DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        String paramsName = docParamsPojo.getParamsName();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        if (StringUtils.isBlank(paramsName) || StringUtils.isBlank(userId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.addParamService(docParamsPojo, userId);
    }

//    /**
//     * 新增参数至参数库2
//     *
//     * @param docParamsPojo
//     * @param session
//     * @return
//     * @throws Exception
//     */
//    @PostMapping("/addParam2")
//    public TResponseVo addParam2(@RequestBody DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
//        String paramsName = docParamsPojo.getParamsName();
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
////        String userId = "110";
//        if (StringUtils.isBlank(paramsName) || StringUtils.isBlank(userId)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.addParamService2(docParamsPojo, userId);
//    }

    /**
     * 参数类型维表
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getParamTypeDi")
    public TResponseVo getParamTypeDi() throws Exception {
        return fileOperationService.getParamTypeDiService();
    }

    /**
     * 参数填参角色维表
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getParamSceneDi")
    public TResponseVo getParamSceneDi() throws Exception {
        return fileOperationService.getParamSceneDiService();
    }

//    /**
//     * 参数数据来源维表
//     *
//     * @param
//     * @return
//     * @throws Exception
//     */
//    @GetMapping("/getParamSourceDi")
//    public TResponseVo getParamSourceDi() throws Exception {
//        return fileOperationService.getParamSourceDiService();
//    }

    /**
     * 参数数据来源维表
     *
     * @param
     * @return
     * @throws Exception
     */
    @GetMapping("/getParamGroupDi")
    public TResponseVo getParamGroupDi(String fileUuid, String fileVersionId) throws Exception {
        return fileOperationService.getParamGroupDiService(fileUuid, fileVersionId);
    }

    @GetMapping("/addParamGroup")
    public TResponseVo addParamGroup(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        return fileOperationService.addParamGroupService(docParamsGroupPojo);
    }

    @GetMapping("/delParamGroup")
    public TResponseVo delParamGroup(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        return fileOperationService.delParamGroupService(docParamsGroupPojo);
    }

    @GetMapping("/updateParamGroup")
    public TResponseVo updateParamGroup(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        return fileOperationService.updateParamGroupService(docParamsGroupPojo);
    }

    @GetMapping("/getParamGroupInfo")
    public TResponseVo getParamGroupInfo(DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        return fileOperationService.getParamGroupInfoService(docParamsGroupPojo);
    }

    @GetMapping("/getParamGroupList")
    public TResponseVo getParamGroupList(PagePojo pagePojo) throws Exception {
        return fileOperationService.getParamGroupListService(pagePojo);
    }

    @PostMapping("/orderParamGroup")
    public TResponseVo orderParamGroup(@RequestBody DocParamsGroupPojo docParamsGroupPojo) throws Exception {
        return fileOperationService.orderParamGroupService(docParamsGroupPojo);
    }

    @GetMapping("/delParam")
    public TResponseVo delParam(String paramsUuid, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        if (StringUtils.isBlank(userId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.delParamService(paramsUuid);
    }

    @PostMapping("/updateParam")
    public TResponseVo updateParam(@RequestBody DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        String paramsUuid = docParamsPojo.getParamsUuid();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
//        String userId = "110";
        if (StringUtils.isBlank(paramsUuid) || StringUtils.isBlank(userId)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        docParamsPojo.setUserId(userId);
        return fileOperationService.updateParamService(docParamsPojo);
    }

    @GetMapping("/getParamInfo")
    public TResponseVo getParamInfo(String paramsUuid, HttpSession session) throws Exception {
        return fileOperationService.getParamInfoService(paramsUuid);
    }

    @PostMapping("/getParam")
    public TResponseVo getParam(@RequestBody PagePojo pagePojo, HttpSession session, @RequestHeader("platform") String platform) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return fileOperationService.getParamService(pagePojo, userId, platform);
    }

    @GetMapping("/getParamTypeStyleList")
    public TResponseVo getParamTypeStyleList(String paramsTypeId) throws Exception {
        return fileOperationService.getParamTypeStyleListService(paramsTypeId);
    }

//    @GetMapping("/selectParamList")
//    public TResponseVo selectParamList(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
////        System.out.println(JSON.toJSONString(currencyForm));
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(user.getUserId())) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.getParamListService(fileUuid, fileVersionId);
//    }
//
//    @PostMapping("/commitParamList")
//    public TResponseVo commitParamList(@RequestBody CommitParamListForm commitParamListForm, HttpSession session) throws Exception {
////        System.out.println(JSON.toJSONString(currencyForm));
//        String fileUuid = commitParamListForm.getFileUuid();
//        String fileVersionId = commitParamListForm.getFileVersionId();
//        List<CommitParamListItemForm> lists = commitParamListForm.getLists();
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        System.out.println(lists);
//        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(user.getUserId())) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.commitParamListService(fileUuid, fileVersionId, lists);
//    }

    @GetMapping("/getVersionList")
    public TResponseVo getVersionList(String fileUuid, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getVersionListService(fileUuid);
    }

    @GetMapping("/getVersionDetail")
    public TResponseVo getVersionDetail(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getVersionDetailService(fileUuid, fileVersionId);
    }

    @GetMapping("/getVersionPhoto")
    public TResponseVo getVersionPhoto(DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(user.getUserId())) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getVersionPhotoService(fileUuid, fileVersionId);
    }

    @GetMapping("/getVersionTree")
    public TResponseVo getVersionTree(DocFileVerIndexPojo docFileVerIndexPojo, HttpSession session) throws Exception {
//        System.out.println(fileUuid);
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String fileUuid = docFileVerIndexPojo.getFileUuid();
        String mode = docFileVerIndexPojo.getMode();
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        if (mode != null && mode.equals("part")) {
            return fileOperationService.getVersionTreeService(fileUuid);
        } else {
            return fileOperationService.getVersionCompleteTreeService(fileUuid);
        }
    }

//    @GetMapping("/getVersionCompleteTree")
//    public TResponseVo getVersionCompleteTree(String fileUuid, HttpSession session) throws Exception {
////        System.out.println(fileUuid);
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        if (StringUtils.isBlank(fileUuid)||StringUtils.isBlank(user.getUserId())) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.getVersionCompleteTreeService(fileUuid);
//    }

    /**
     * 获取父文件变更信息
     *
     * @param fileUuid
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/getUpdateInfo")
    public TResponseVo getUpdateInfo(String fileUuid, HttpSession session) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getUpdateInfoService(fileUuid);
    }

    /**
     * 确认更新信息
     *
     * @param updateUuid,status
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/confirmUpdateInfo")
    public TResponseVo confirmUpdateInfo(String updateUuid, String status, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        if (StringUtils.isBlank(updateUuid) || StringUtils.isBlank(status)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.confirmUpdateInfoService(updateUuid, status, userId);
    }


    /**
     * 派生次数
     *
     * @param fileUuid
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/getDerivationList")
    public TResponseVo getDerivationList(String fileUuid, HttpSession session) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getDerivationListService(fileUuid);
    }

    /**
     * 应用次数
     *
     * @param fileUuid
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/getUseList")
    public TResponseVo getUseList(String fileUuid, HttpSession session) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getUseListService(fileUuid);
    }


//    @PostMapping("/saveDraft")
//    public TResponseVo saveDraft(@RequestBody DocFileVerIndexPojo docFileVerIndexPojo, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
////        String userId = "110";
//        return fileOperationService.saveDraftService(docFileVerIndexPojo, userId);
//    }


    @GetMapping("/getBoardData")
    public TResponseVo getBoardData(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getBoardDataService(fileUuid, fileVersionId, session);
    }

    @GetMapping("/getBoardSecondData")
    public TResponseVo getBoardSecondData(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        if (StringUtils.isBlank(fileUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.getBoardSecondDataService(fileUuid, fileVersionId, session);
    }

    /**
     * 法律法规库
     */

    /**
     * 新增章节大纲
     */
    @GetMapping("/addOutline")
    public TResponseVo addOutline(OutLinePojo outLinePojo) throws Exception {
        return fileOperationService.addOutlineService(outLinePojo);
    }

    /**
     * 调整章节大纲
     */
    @PostMapping("/updateOutline")
    public TResponseVo updateOutline(@RequestBody OutLinePojo outLinePojo) throws Exception {
        return fileOperationService.updateOutlineService(outLinePojo);
    }

    /**
     * 删除章节大纲
     */
    @GetMapping("/delOutline")
    public TResponseVo delOutline(String outlineId, String fileUuid, String fileVersionId) throws Exception {
        return fileOperationService.delOutlineService(outlineId, fileUuid, fileVersionId);
    }

    /**
     * 修改章节题目
     */
    @GetMapping("/updateOutlineName")
    public TResponseVo updateOutlineName(String outlineId, String fileUuid, String fileVersionId, String outlineText) throws Exception {
        return fileOperationService.updateOutlineNameService(outlineId, fileUuid, fileVersionId, outlineText);
    }

    /**
     * 编辑替换章节-获取可编辑章节列表
     */
    @GetMapping("/getReplaceOutlineList")
    public TResponseVo getReplaceOutlineList(String outlineId, String fileUuid, String fileVersionId) throws Exception {
        return fileOperationService.getReplaceOutlineListService(outlineId, fileUuid, fileVersionId);
    }

    /**
     * 编辑替换章节-确认
     */
    @PostMapping("/confirmReplaceOutline")
    public TResponseVo confirmReplaceOutline(@RequestBody OutLinePojo outLinePojo) throws Exception {
        String outlineId = outLinePojo.getOutlineId();
        List<String> outlineList = outLinePojo.getOutlineList();
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        String color = outLinePojo.getColor();
        return fileOperationService.confirmReplaceOutlineService(outlineId, outlineList, fileUuid, fileVersionId, color);
    }

    /**
     * 编辑非必选章节-确认
     */
    @PostMapping("/confirmNecessaryOutline")
    public TResponseVo confirmNecessaryOutline(@RequestBody OutLinePojo outLinePojo) throws Exception {
        String outlineId = outLinePojo.getOutlineId();
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        String isNecessary = outLinePojo.getIsNecessary();
        return fileOperationService.confirmNecessaryOutlineService(outlineId, fileUuid, fileVersionId, isNecessary);
    }


    /**
     * 新增影子版本
     */
    @GetMapping("/newShadowVersion")
    public TResponseVo newShadowVersion(DocFileIndexPojo docFileIndexPojo) throws Exception {
        return fileOperationService.newShadowVersionService(docFileIndexPojo);
    }

    /**
     * 根据标签获取相似度
     */
    @PostMapping("/getSimilarDoc")
    public TResponseVo getSimilarDoc(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        List<String> fileLabelIds = docFileIndexPojo.getFileLabelIds();
        String searchLike = docFileIndexPojo.getSearchLike();
        String fileTypeId = docFileIndexPojo.getFileTypeId();
        return fileOperationService.getSimilarDocService(fileLabelIds, userId, searchLike, fileTypeId);
    }

    /**
     * 自动保存
     */
    @PostMapping("/autoSave")
    public TResponseVo autoSave(@RequestBody ContentPojo contentPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        String contentId = contentPojo.getContentId();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        contentPojo.setUserId(userId);
        contentPojo.setSearchUuid(pageId);
        return fileOperationService.autoSaveService(contentPojo);
    }

//    /**
//     * 编辑页参数清单-新增
//     */
//    @PostMapping("/addContentParam")
//    public TResponseVo addContentParam(@RequestBody DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
//        return fileOperationService.addContentParamService(docParamsPojo);
//    }
//

    /**
     * 编辑页参数清单-删除
     */
    @GetMapping("/delContentParam")
    public TResponseVo delContentParam(DocParamsPojo docParamsPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        docParamsPojo.setSearchUuid(pageId);
        return fileOperationService.delContentParamService(docParamsPojo);
    }

    /**
     * 编辑页参数清单-查询
     */
    @GetMapping("/getContentParam")
    public TResponseVo getContentParam(DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        return fileOperationService.getContentParamService(docParamsPojo);
    }

    /**
     * 编辑页参数清单-修改文内参数
     */
    @PostMapping("/updateContentParam")
    public TResponseVo updateContentParam(@RequestBody DocParamsPojo docParamsPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        docParamsPojo.setSearchUuid(pageId);
        return fileOperationService.updateContentParamService(docParamsPojo);
    }

    /**
     * 编辑页参数清单-修改展示效果
     */
    @GetMapping("/updateContentParamShow")
    public TResponseVo updateContentParamShow(DocParamsPojo docParamsPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        docParamsPojo.setSearchUuid(pageId);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        return fileOperationService.updateContentParamShowService(docParamsPojo);
    }

    /**
     * 编辑页参数清单-修改提示词
     */
    @PostMapping("/updateContentParamPrompt")
    public TResponseVo updateContentParamPrompt(@RequestBody DocParamsForm docParamsPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        docParamsPojo.setSearchUuid(pageId);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        return fileOperationService.updateContentParamPromptService(docParamsPojo);
    }


    /**
     * 编辑页参数清单-新增和删除
     */
    @PostMapping("/addAndDelContentParam")
    public TResponseVo addAndDelContentParam(@RequestBody DocParamsPojo docParamsPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        docParamsPojo.setSearchUuid(pageId);
        return fileOperationService.addAndDelContentParamService(docParamsPojo);
    }

    /**
     * 编辑页搜索-搜索列表
     */
    @GetMapping("/search")
    public TResponseVo search(DocFileIndexPojo docFileIndexPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setSearchUserId(userId);
        docFileIndexPojo.setSearchUuid(pageId);
        return fileOperationService.searchService(docFileIndexPojo);
    }


    /**
     * 右侧看板-文件属性-查看属性
     */
    @GetMapping("/getFileProperty")
    public TResponseVo getFileProperty(String fileUuid, HttpSession session) throws Exception {
        return fileOperationService.getFilePropertyService(fileUuid);
    }

    /**
     * 右侧看板-文件属性-获取参与人下拉列表
     */
    @GetMapping("/getFilePropertyUserList")
    public TResponseVo getFilePropertyUserList(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return fileOperationService.getFilePropertyUserListService(userId);
    }

    /**
     * 右侧看板-文件属性-获取文件类型下拉列表
     */
    @GetMapping("/getFileTypeList")
    public TResponseVo getFileTypeList(String fileTypeGroupId) throws Exception {
        if (fileTypeGroupId == null) {
            return fileOperationService.getFileTypeListService();
        } else {
            return workingTableServiceImpl.getFileTypeListService(fileTypeGroupId);
        }
    }

    /**
     * 右侧看板-文件属性-获取适用范围下拉列表
     */
    @GetMapping("/getFileUseRangeList")
    public TResponseVo getFileUseRangeList() throws Exception {
        return fileOperationService.getFileUseRangeListService();
    }


    /**
     * 右侧看板-文件属性-修改属性
     */
    @PostMapping("/updateFileProperty")
    public TResponseVo updateFileProperty(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        return fileOperationService.updateFilePropertyService(docFileIndexPojo);
    }

    /**
     * 拉取附属文件类型维表
     */
    @GetMapping("/getTemplateTypeDi")
    public TResponseVo getTemplateTypeDi(String templateTypeId) throws Exception {
        return fileOperationService.getTemplateTypeDiService(templateTypeId);
    }


    /**
     * 模板组-新增附属文件
     */
    @PostMapping("/newSubsidiaryFile")
    public TResponseVo newSubsidiaryFile(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return fileOperationService.newSubsidiaryFileService(docFileIndexPojo);
    }

    /**
     * 模板组-上传附属文件
     *
     * @param
     */
    @PostMapping("/uploadSubsidiaryFile")
    @Transactional(rollbackFor = Exception.class)
    public TResponseVo uploadSubsidiaryFile(@RequestParam(value = "file") MultipartFile file,
                                            @RequestParam(value = "fileUuid", required = false) String fileUuid,
//                                            @RequestParam(value = "fileVersionId", required = false) String fileVersionId,
                                            @RequestParam(value = "fileName", required = false) String fileName,
                                            HttpSession session) throws Exception {
        long start = System.currentTimeMillis();  // 接口开始执行时间
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();

        String mainFileUuid = fileUuid;
        fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (file.isEmpty()) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传失败");
        }

        // 获取文件格式 doc或docx
        String oriFileName = file.getOriginalFilename();
        String extension = oriFileName.substring(oriFileName.lastIndexOf("."));
        if (!extension.equals(".doc") && !extension.equals(".docx")) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, "文件上传错误,目前只支持.doc和.docx的文档");
        }

        // 是否重新命名
        if (StringUtils.isBlank(fileName)) {
            // 使用文档原名
            fileName = file.getOriginalFilename();
        } else {
            // 重新命名  (如果新名字长度小于等于5 或者后缀不是.docx 就给他加一个)
            if (fileName.length() < 6 || !fileName.substring(fileName.length() - 5).equals(".docx") || !fileName.substring(fileName.length() - 4).equals(".doc")) {
                fileName = fileName + extension;
            }
        }
        File filePathNew = new File(this.filePath + "/" + fileUuid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFileNew = new File(this.filePath + "/" + fileUuid + "/" + fileUuid + extension);
        try {
            file.transferTo(docxFileNew);
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传完成");
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传报错");
        }
        long start2 = System.currentTimeMillis();  // 上传完成 开始解析
        System.out.println("程序上传运行时间：" + (start2 - start) + "ms");


        //TODO 解压docx文件获取style

        System.out.println("开始解析");
        //TODO 提前清空目标文件夹
        try {
            new Doc2html().wordToHtml(this.filePath + "/" + fileUuid + "/" + fileUuid + extension,
                    htmlPath + "/",
                    fileUuid + ".html");
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件解析完成");
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件解析报错");
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, file.getOriginalFilename() + "解析失败");
        }
        long start3 = System.currentTimeMillis();  // 解析完成 开始入库
        System.out.println("程序解析运行时间：" + (start3 - start2) + "ms");
        // 主表索引信息
        DocFileIndexPojo docFileIndexPojo = new DocFileIndexPojo();
        docFileIndexPojo.setFileUuid(fileUuid);
        docFileIndexPojo.setCreateUserId(userId);
        docFileIndexPojo.setUpdateUserId(userId);
        docFileIndexPojo.setCreateTime(new Date());
        docFileIndexPojo.setFileName(fileName.replaceAll("\\.docx", "").replaceAll("\\.doc", ""));
        docFileIndexPojo.setFileParentId(null);
        docFileIndexPojo.setIsRootFile("1");
        docFileIndexPojo.setFileStatus("1");
        docFileIndexPojo.setAncestorsFileUuid(fileUuid);
        docFileIndexPojo.setFileClass("1");

        String versionId = UUID.randomUUID().toString().replaceAll("-", "");
        docFileIndexPojo.setFileVersionId(versionId);
        docFileIndexPojo.setMainFileUuid(mainFileUuid);  // 附属文件标识
        // 版本表版本信息
        DocFileVerIndexPojo docFileVerIndexPojo = new DocFileVerIndexPojo();
        docFileVerIndexPojo.setFileVersionId(versionId);
        docFileVerIndexPojo.setFileVersionName(DocFrameUtils.versionCount(null));
        docFileVerIndexPojo.setFileUuid(fileUuid);
        docFileVerIndexPojo.setIsRootVersion("1");
        docFileVerIndexPojo.setParentsVersionId(null);
        docFileVerIndexPojo.setCreateUserId(user.getUserId());
        docFileVerIndexPojo.setUpdateUserId(user.getUserId());
        docFileVerIndexPojo.setCreateTime(new Date());
        docFileVerIndexPojo.setIsDraft("0");

        docFileIndexPojo.setFileTypeId("MB-ZBWJ");  // 附属文件随便给一个模板的枚举类型
        docFileIndexPojo.setFileVersionId(versionId);
        docFileIndexPojo.setVersions(docFileVerIndexPojo);
        fileOperationService.uploadSubsidiaryFileService(docFileIndexPojo);


        // 给html打H标签
        String htmlStr = new HtmlUtils().htmlAddHTag(htmlPath + "/" + fileUuid + ".html", docUnzipPath + "/" + fileUuid + "/word/styles.xml");
        // 拆分返回
        List<Map<String, String>> titleMapList = new HtmlUtils().splitHtmlByTag(htmlStr);
        String sql = "";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(new Date());
        // 拼装大纲sql
        for (Map<String, String> titleMap : titleMapList) {
            sql = sql + "(\"" + titleMap.get("id") + "\"," + JSON.toJSONString(titleMap.get("title")) + ",\"" + fileUuid + "\",\"" + versionId + "\",\"" + titleMap.get("fatherId") + "\",\"" + titleMap.get("id") + "\",\"" + titleMap.get("level") + "\",\"" + "0" + "\",\"" + dateString + "\",\"" + userId + "\",\"" + userId + "\"),";
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileOperationService.insertOutlineService(sql);
        }
        sql = "";
        // 拼装大纲内容sql
        for (Map<String, String> titleMap : titleMapList) {
            sql = sql + "(\"" + UUID.randomUUID().toString().replaceAll("-", "") + "\",\"" + titleMap.get("content").replaceAll("\\\"", "\\\\\"").replaceAll("\\\n", "") + "\",\"" + titleMap.get("id") + "\",\"" + fileUuid + "\",\"" + versionId + "\",\"" + dateString + "\"),";
        }
        if (sql.length() != 0) {
            sql = sql.substring(0, sql.length() - 1);
            fileOperationService.insertContentService(sql);
        }
        long end = System.currentTimeMillis();  // 入库完成
        System.out.println("程序入库运行时间：" + (end - start3) + "ms");
        HashMap<String, String> ret = new HashMap<>();
        ret.put("info", "上传成功");
        ret.put("fileUuid", fileUuid);
        ret.put("fileVersionId", versionId);
        return TResponseVo.success(ret);
    }

    /**
     * 模板组-获取附属文件列表
     */
    @GetMapping("/getSubsidiaryFileList")
    public TResponseVo getSubsidiaryFileList(String fileUuid, String fileVersionId, HttpSession session) throws Exception {
        return fileOperationService.getSubsidiaryFileListService(fileUuid, fileVersionId, session);
    }

    /**
     * 模板组-修改附属文件名
     */
    @GetMapping("/updateSubsidiaryFileName")
    public TResponseVo updateSubsidiaryFileName(String fileUuid, String fileName, String templateTypeId) throws Exception {
        return fileOperationService.updateSubsidiaryFileNameService(fileUuid, fileName, templateTypeId);
    }

    /**
     * 模板组-删除附属文件
     */
    @GetMapping("/delSubsidiaryFile")
    public TResponseVo delSubsidiaryFile(String fileUuid) throws Exception {
        return fileOperationService.delSubsidiaryFileService(fileUuid);
    }


    /**
     * 标注内容
     */
    /**
     * 词条库
     */
    @PostMapping("/addWords")
    public TResponseVo addWords(@RequestBody DocWordsPojo docWordsPojo, HttpSession session) throws Exception {
//        System.out.println(JSON.toJSONString(currencyForm));
        String wordsName = docWordsPojo.getWordsName();
        String wordsDesc = docWordsPojo.getWordsDesc();
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        if (StringUtils.isBlank(wordsName) || StringUtils.isBlank(wordsDesc) || user == null) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        docWordsPojo.setCreateUserId(user.getUserId());
        return fileOperationService.addWordsService(docWordsPojo);
    }

    @GetMapping("/delWords")
    public TResponseVo delWords(String wordsUuid) throws Exception {
        if (StringUtils.isBlank(wordsUuid)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.delWordsService(wordsUuid);
    }


    @PostMapping("/updateWords")
    public TResponseVo updateWords(@RequestBody DocWordsPojo docWordsPojo) throws Exception {
        String wordsUuid = docWordsPojo.getWordsUuid();
        String wordsName = docWordsPojo.getWordsName();
        String wordsDesc = docWordsPojo.getWordsDesc();
        if (StringUtils.isBlank(wordsUuid) || StringUtils.isBlank(wordsName) || StringUtils.isBlank(wordsDesc)) {
            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
        }
        return fileOperationService.updateWordsService(docWordsPojo);
    }

    @GetMapping("/getWordsInfo")
    public TResponseVo getWordsInfo(String wordsUuid) throws Exception {
        return fileOperationService.getWordsInfoService(wordsUuid);
    }

    @GetMapping("/getWordsList")
    public TResponseVo getWordsList(PagePojo pagePojo) throws Exception {
        return fileOperationService.getWordsListService(pagePojo);
    }

    @GetMapping("/getWordsLabelValueList")
    public TResponseVo getWordsLabelValueList() throws Exception {
        return fileOperationService.getWordsLabelValueListService();
    }

//    /**
//     * 摘编库
//     */
//    @GetMapping("/addTips")
//    public TResponseVo addTips(DocTipsPojo docTipsPojo, HttpSession session) throws Exception {
////        System.out.println(JSON.toJSONString(currencyForm));
//        String tipsName = docTipsPojo.getTipsName();
//        String tipsDesc = docTipsPojo.getTipsDesc();
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        if (StringUtils.isBlank(tipsName) || StringUtils.isBlank(tipsDesc)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.addTipsService(user.getUserId(), tipsName, tipsDesc);
//    }
//
//    @GetMapping("/delTips")
//    public TResponseVo delTips(String tipsUuid, HttpSession session) throws Exception {
//        if (StringUtils.isBlank(tipsUuid)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.delTipsService(tipsUuid);
//    }
//
//    @GetMapping("/updateTips")
//    public TResponseVo updateTips(DocTipsPojo docTipsPojo) throws Exception {
////        System.out.println(JSON.toJSONString(currencyForm));
//        String tipsUuid = docTipsPojo.getTipsUuid();
//        String tipsName = docTipsPojo.getTipsName();
//        String tipsDesc = docTipsPojo.getTipsDesc();
//        if (StringUtils.isBlank(tipsUuid) || StringUtils.isBlank(tipsName) || StringUtils.isBlank(tipsDesc)) {
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR);
//        }
//        return fileOperationService.updateTipsService(docTipsPojo);
//    }
//
//    @GetMapping("/getTips")
//    public TResponseVo getTips(Integer pageNum, Integer pageSize, String tipsNameLike, String isPaged, HttpSession session) throws Exception {
////        System.out.println(JSON.toJSONString(currencyForm));
////        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        if (pageNum == null) {
//            pageNum = 1;
//        }
//        if (pageSize == null) {
//            pageSize = 10;
//        }
//        return fileOperationService.getTipsService(pageNum, pageSize, tipsNameLike, isPaged);
//    }


//    /**
//     * 标注库
//     * @param tagPojo
//     * @param session
//     * @return
//     * @throws Exception
//     */
//    @GetMapping("/addTag")
//    public TResponseVo addTag(TagPojo tagPojo, HttpSession session) throws Exception {
//        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
//        String userId = user.getUserId();
//        tagPojo.setCreateUserId(userId);
//        return fileOperationService.addTagService(tagPojo);
//    }

    @GetMapping("/delTag")
    public TResponseVo delTag(TagPojo tagPojo, @RequestHeader("pageId") String pageId) throws Exception {
        tagPojo.setSearchUuid(pageId);
        return fileOperationService.delTagService(tagPojo);
    }

    /**
     * 标注-新增和删除
     */
    @PostMapping("/addAndDelContentTag")
    public TResponseVo addAndDelContentTag(@RequestBody TagPojo tagPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        tagPojo.setUserId(userId);
        tagPojo.setSearchUuid(pageId);
        return fileOperationService.addAndDelContentTagService(tagPojo);
    }

    @PostMapping("/updateTag")
    public TResponseVo updateTag(@RequestBody TagPojo tagPojo, @RequestHeader("pageId") String pageId) throws Exception {
        tagPojo.setSearchUuid(pageId);
        return fileOperationService.updateTagService(tagPojo);
    }

    @GetMapping("/getTagInfo")
    public TResponseVo getTagInfo(String tagId, String fileUuid, String fileVersionId) throws Exception {
        return fileOperationService.getTagInfoService(tagId, fileUuid, fileVersionId);
    }

    @GetMapping("/getTagList")
    public TResponseVo getTagList(PagePojo pagePojo) throws Exception {
        return fileOperationService.getTagListService(pagePojo);
    }

    @GetMapping("/getContentTag")
    public TResponseVo getContentTag(TagPojo tagPojo) throws Exception {
        return fileOperationService.getContentTagService(tagPojo);
    }

    /**
     * 编辑摘编-获取摘编内容
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @PostMapping("/getLawContent")
    public TResponseVo getLawContent(@RequestBody TagPojo tagPojo) throws Exception {
        String lawId = tagPojo.getLawId();
        return fileOperationService.getLawContentService(lawId);
    }

    /**
     * 编辑摘编-提交
     *
     * @param tagPojo
     * @return
     * @throws Exception
     */
    @PostMapping("/commitLaw")
    public TResponseVo commitLaw(@RequestBody TagPojo tagPojo) throws Exception {
        return fileOperationService.commitLawService(tagPojo);
    }

    /**
     * 书签库
     *
     * @param bookmarkPojo
     * @param session
     * @return
     * @throws Exception
     */
    @GetMapping("/addBookmark")
    public TResponseVo addBookmark(BookmarkPojo bookmarkPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        bookmarkPojo.setCreateUserId(userId);
        return fileOperationService.addBookmarkService(bookmarkPojo);
    }

    @GetMapping("/delBookmark")
    public TResponseVo delBookmark(BookmarkPojo bookmarkPojo, HttpSession session) throws Exception {
        return fileOperationService.delBookmarkService(bookmarkPojo);
    }

    @PostMapping("/addAndDelBookmark")
    public TResponseVo addAndDelBookmark(@RequestBody BookmarkPojo bookmarkPojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        bookmarkPojo.setCreateUserId(userId);
        bookmarkPojo.setSearchUuid(pageId);
        return fileOperationService.addAndDelBookmarkService(bookmarkPojo);
    }

    @GetMapping("/updateBookmark")
    public TResponseVo updateBookmark(BookmarkPojo bookmarkPojo, HttpSession session) throws Exception {
        return fileOperationService.updateBookmarkService(bookmarkPojo);
    }

    @GetMapping("/getBookmarkInfo")
    public TResponseVo getBookmarkInfo(BookmarkPojo bookmarkPojo, HttpSession session) throws Exception {
        return fileOperationService.getBookmarkInfoService(bookmarkPojo);
    }

    @GetMapping("/getBookmarkList")
    public TResponseVo getBookmarkList(PagePojo pagePojo, HttpSession session) throws Exception {
        return fileOperationService.getBookmarkListService(pagePojo);
    }

    @GetMapping("/getBookmarkLabelValueList")
    public TResponseVo getBookmarkLabelValueList(String fileUuid, String fileVersionId) throws Exception {
        return fileOperationService.getBookmarkLabelValueListService(fileUuid, fileVersionId);
    }


    @PostMapping("/addDerive")
    public TResponseVo addDerive(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return fileOperationService.addDeriveService(docFileIndexPojo, session);
    }

    @PostMapping("/addVersion")
    public TResponseVo addVersion(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return fileOperationService.addVersionService(docFileIndexPojo);
    }

    @GetMapping("/addLabelGroup")
    public TResponseVo addLabelGroup(DocLabelPojo docLabelPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docLabelPojo.setCreateUserId(userId);
        return fileOperationService.addLabelGroupService(docLabelPojo);
    }

    @GetMapping("/delLabelGroup")
    public TResponseVo delLabelGroup(DocLabelPojo docLabelPojo, HttpSession session) throws Exception {
        return fileOperationService.delLabelGroupService(docLabelPojo);
    }

    @GetMapping("/getLabelGroupList")
    public TResponseVo getLabelGroupList(PagePojo pagePojo) throws Exception {
        return fileOperationService.getLabelGroupListService(pagePojo);
    }

    @GetMapping("/getLabelGroupLVList")
    public TResponseVo getLabelGroupLVList() throws Exception {
        return fileOperationService.getLabelGroupLVListService();
    }

    @PostMapping("/addLabel")
    public TResponseVo addLabel(@RequestBody DocLabelPojo docLabelPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docLabelPojo.setCreateUserId(userId);
        return fileOperationService.addLabelService(docLabelPojo);
    }


    @GetMapping("/delLabel")
    public TResponseVo delLabel(String labelUuid) throws Exception {
        return fileOperationService.delLabelService(labelUuid);
    }

    @PostMapping("/updateLabel")
    public TResponseVo updateLabel(@RequestBody DocLabelPojo docLabelPojo, HttpSession session) throws Exception {
        return fileOperationService.updateLabelService(docLabelPojo);
    }

    @GetMapping("/getLabelInfo")
    public TResponseVo getLabelInfo(String labelUuid) throws Exception {
        return fileOperationService.getLabelInfoService(labelUuid);
    }

    @GetMapping("/getLabelList")
    public TResponseVo getLabelList(PagePojo pagePojo, String labelGroupId, String fileTypeId) throws Exception {
        return fileOperationService.getLabelListService(pagePojo, labelGroupId, fileTypeId);
    }

    @GetMapping("/getLabelLVList")
    public TResponseVo getLabelLVList(String labelGroupId) throws Exception {
        return fileOperationService.getLabelLVListService(labelGroupId);
    }

    @PostMapping("/uploadNotePic")
    public HashMap uploadNotePic(@RequestParam(value = "file") MultipartFile file, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("userPojo");
        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (file.isEmpty()) {
            HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("errno", 1);
            objectObjectHashMap.put("status","fail");
            objectObjectHashMap.put("message","文件为空");
            response.setStatus(403,"403");
            return objectObjectHashMap;
        }

//        文件名
        String fileName = file.getOriginalFilename();

//        后缀名
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        List<String> strings = Arrays.asList(".jpg", ".png", ".bmp", ".jpeg", ".gif");
        if (!strings.contains(extension)) {
            HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
            objectObjectHashMap.put("errno", 2);
            objectObjectHashMap.put("status","fail");
            objectObjectHashMap.put("message","文件上传错误，格式不支持");
            response.setStatus(403,"403");
            return objectObjectHashMap;
//            return new WorkTableException("文件上传错误,格式不支持");
//            return TResponseVo.error(ResponseEnum.WEB_INPARAM_ERROR, );
        }

        File filePathNew = new File(this.picPath);

        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFile = new File(this.picPath + "/" + fileUuid + extension);

        try {
            file.transferTo(docxFile);
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
        }
//        String path = docxFile.getPath();

        String absolutePath = docxFile.getAbsolutePath();
        // 判断是否需要压缩
        Map<String, Integer> sizeInfo = new ImageInfoUtils(docxFile).getSizeInfo();
        if (sizeInfo.get("Width") > 1600 || sizeInfo.get("Height") > 900) {
            Thumbnails.of(absolutePath).size(1600, 900).toFile(absolutePath);
        }
        //TODO 返回表头信息
        HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
        ArrayList<HashMap<Object, Object>> List1 = new ArrayList<>();
        HashMap<Object, Object> objectObjectHashMap1 = new HashMap<>();
        objectObjectHashMap1.put("url", absolutePath);
        objectObjectHashMap1.put("alt", fileName);
        objectObjectHashMap1.put("href",absolutePath);
        List1.add(objectObjectHashMap1);
        objectObjectHashMap.put("errno", 0);
        objectObjectHashMap.put("data", List1);
        objectObjectHashMap.put("status","success");
        return objectObjectHashMap;
    }

    @PostMapping("/uploadNoteAnnex")
    public TResponseVo uploadNoteAnnex(@RequestParam(value = "file") MultipartFile file, HttpSession session, HttpServletResponse response) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("userPojo");
        String fileUuid = UUID.randomUUID().toString().replaceAll("-", "");
        if (file.isEmpty()) {
            throw new WorkTableException("上传文件为空");
        }

//        文件名
        String fileName = file.getOriginalFilename();
//        后缀名
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        File filePathNew = new File(this.picPath);

        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFile = new File(this.picPath + "/" + fileUuid + extension);

        try {
            file.transferTo(docxFile);
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "fileupload", "文件上传Controller", "文件上传报错");
        }

        String absolutePath = docxFile.getAbsolutePath();

        //TODO 返回表头信息
        HashMap<String, Object> retMap = new HashMap<>();
        retMap.put("name",fileName);
        retMap.put("url",absolutePath);
        return TResponseVo.success(retMap);
    }

    @GetMapping("/getTrueVersionId")
    public TResponseVo getTrueVersionId(String fileUuid, HttpSession session) throws Exception {
        return fileOperationService.getTrueVersionIdService(fileUuid, session);
    }

    @PostMapping("/writeParam")
    public TResponseVo writeParam(@RequestBody DocParamsPojo docParamsPojo, @RequestHeader("pageId") String pageId, HttpSession session) throws Exception {
        docParamsPojo.setSearchUuid(pageId);
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        return fileOperationService.writeParamService(docParamsPojo);
    }

    @PostMapping("/saveWriteParam")
    public TResponseVo saveWriteParam(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return fileOperationService.saveWriteParamService(docFileIndexPojo, session);
    }

    @GetMapping("/resetParam")
    public TResponseVo resetParam(DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        return fileOperationService.resetParamService(docParamsPojo, session);
    }

    // 附件参数
    @PostMapping("/uploadAnnexParam")
    public TResponseVo uploadAnnexParam(@RequestParam(value = "file") MultipartFile file,
//                                        @RequestParam(value = "paramUuid", required = false) String paramUuid,
//                                        @RequestParam(value = "fileUuid", required = false) String fileUuid,
//                                        @RequestParam(value = "fileVersionId", required = false) String fileVersionId,
                                        HttpSession session) throws Exception {
        long start = System.currentTimeMillis();  // 接口开始执行时间
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();  // 操作人id

//        if (fileList==null || fileList.length==0) {
//            // 如果附件列表为空 返回错误
//            return TResponseVo.error(ResponseEnum.ERROR,"");
//        }
//        List<Map<String, String>> fileURLList = new ArrayList<>();
//        for (MultipartFile file : fileList) {
//            if (file.isEmpty()) {
//                break;
//            }

        // 自定义uuid
        String uid = UUID.randomUUID().toString().replaceAll("-", "");
        // 获取文件格式
        String oriFileName = file.getOriginalFilename();
        String extension = oriFileName.substring(oriFileName.lastIndexOf("."));
        // 创建文件夹
        File filePathNew = new File(this.annexPath + "/" + uid);
        if (!filePathNew.exists()) {
            filePathNew.mkdir();
        }
        File docxFileNew = new File(this.annexPath + "/" + uid + "/" + oriFileName);
        try {
            file.transferTo(docxFileNew);
            LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传完成");
        } catch (IOException e) {
            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}", "uploadSubsidiaryFile", "文件上传Controller", "文件上传报错");
        }
        long start2 = System.currentTimeMillis();  // 上传完成 开始解析
        System.out.println("程序上传运行时间：" + (start2 - start) + "ms");

        HashMap<String, String> map = new HashMap<>();
        map.put("name", oriFileName);
        map.put("uid", uid);
        map.put("url", this.annexPath + "/" + uid + "/" + oriFileName);

        HashMap<String, Object> ret = new HashMap<>();
        ret.put("info", "上传成功");
        ret.put("paramsText", map);//127.0.0.1:9999/
        return TResponseVo.success(ret);
    }

    @GetMapping("/deleteAnnexParam")
    public TResponseVo deleteAnnexParam(String fileUuid, String fileVersionId, String paramsUuid, String uid, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return fileOperationService.deleteAnnexParamService(fileUuid, fileVersionId, paramsUuid, uid, userId);
    }

    @GetMapping("/getBidLeftInfo")
    public TResponseVo getBidLeftInfo(String fileUuid) throws Exception {
        return fileOperationService.getBidLeftInfoService(fileUuid);
    }

    @PostMapping("/updateBidLeftInfo")
    public TResponseVo updateBidLeftInfo(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        return fileOperationService.updateBidLeftInfoService(docFileIndexPojo);
    }

    @GetMapping("/getBidKey")
    public TResponseVo getBidKey(String fileUuid, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String companyId = user.getCompanyId();
        return fileOperationService.getBidKeyService(fileUuid, userId, companyId);
    }

    @PostMapping("/getLawExtractTextLists")
    public TResponseVo getLawExtractTextLists(@RequestParam(value = "pageSize", required = false) Integer pageSize,
                                              @RequestParam(value = "refresh", required = false) Boolean refresh) throws Exception {
        String lawExtractTextListStr = "";
        if (refresh == null || refresh == false) {
            // 先读取缓存
            if (redisUtils.hasKey("getLawExtractTextLists" + envName)) {
                // 如果缓存里已经存在 则无需查库
                lawExtractTextListStr = String.valueOf(redisUtils.get("getLawExtractTextLists" + envName));
            } else {
                lawExtractTextListStr = fileOperationService.getLawExtractTextListsService(pageSize);
                // 写入缓存  5分钟更新一次
                redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 300);
            }
        } else {
            // 直接读取接口  结果存入缓存
            lawExtractTextListStr = fileOperationService.getLawExtractTextListsService(pageSize);
            // 写入缓存  5分钟更新一次
            redisUtils.set("getLawExtractTextLists" + envName, lawExtractTextListStr, 300);
        }
        if (StringUtils.isBlank(lawExtractTextListStr)) { // 防止摘编接口未采集到数据报错
            return null;
        }

        Map<String, Object> map = JSON.parseObject(lawExtractTextListStr, Map.class);
        Object data = map.get("data");
        return TResponseVo.success(data);
    }


//    /**
//     * 对比法律法规
//     */
//    @PostMapping("/compareLawExtractText")
//    public void compareLawExtractText(@RequestParam(value = "pageSize", required = false) Integer pageSize, HttpSession session) throws Exception {
//        fileOperationService.compareLawExtractTextService(pageSize);
//    }

    /**
     * 获取法律法规变化清单
     */
    @PostMapping("/getLawChangeList")
    public TResponseVo getLawChangeList(@RequestBody HfLawPojo hfLawPojo) throws Exception {
        String fileUuid = hfLawPojo.getFileUuid();
        String fileVersionId = hfLawPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            throw new WorkTableException("必填参数为空");
        }
        return fileOperationService.getLawChangeListService(fileUuid, fileVersionId);
    }

    /**
     * 已读
     */
    @PostMapping("/readLawChange")
    public TResponseVo readLawChange(@RequestBody HfLawPojo hfLawPojo) throws Exception {
        return fileOperationService.readLawChangeService(hfLawPojo);
    }


    /**
     * 评标阶段
     */
    @PostMapping("/newJudgmentMethod")
    public TResponseVo newJudgmentMethod(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.newJudgmentMethodService(hfJudgmentDetailPojo);
    }

    @PostMapping("/delJudgmentMethod")
    public TResponseVo delJudgmentMethod(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.delJudgmentMethodService(hfJudgmentDetailPojo);
    }

    @PostMapping("/updateJudgmentMethod")
    public TResponseVo updateJudgmentMethod(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.updateJudgmentMethodService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentMethodInfo")
    public TResponseVo getJudgmentMethodInfo(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentMethodInfoService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentMethodOutline")
    public TResponseVo getJudgmentMethodOutline(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentMethodOutlineService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentMethodList")
    public TResponseVo getJudgmentMethodList(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentMethodListService(hfJudgmentDetailPojo);
    }

    @PostMapping("/newJudgmentModule")
    public TResponseVo newJudgmentModule(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.newJudgmentModuleService(hfJudgmentDetailPojo);
    }

    @PostMapping("/delJudgmentModule")
    public TResponseVo delJudgmentModule(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.delJudgmentModuleService(hfJudgmentDetailPojo);
    }

    @PostMapping("/updateJudgmentModule")
    public TResponseVo updateJudgmentModule(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.updateJudgmentModuleService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentModuleInfo")
    public TResponseVo getJudgmentModuleInfo(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentModuleInfoService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentModuleList")
    public TResponseVo getJudgmentModuleList(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentModuleListService(hfJudgmentDetailPojo);
    }

    @PostMapping("/newJudgmentDetail")
    public TResponseVo newJudgmentDetail(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.newJudgmentDetailService(hfJudgmentDetailPojo);
    }

    @PostMapping("/delJudgmentDetail")
    public TResponseVo delJudgmentDetail(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.delJudgmentDetailService(hfJudgmentDetailPojo);
    }

    @PostMapping("/updateJudgmentDetail")
    public TResponseVo updateJudgmentDetail(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.updateJudgmentDetailService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentDetailInfo")
    public TResponseVo getJudgmentDetailInfo(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentDetailInfoService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentDetailList")
    public TResponseVo getJudgmentDetailList(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentDetailListService(hfJudgmentDetailPojo);
    }

    @PostMapping("/getJudgmentMethodModuleTree")
    public TResponseVo getJudgmentMethodModuleTree(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfJudgmentDetailPojo.setCreateUserId(userId);
        return fileOperationService.getJudgmentMethodModuleTreeService(hfJudgmentDetailPojo);
    }


    @PostMapping("/getTendParamsList")
    public TResponseVo getTendParamsList(@RequestBody HfJudgmentDetailPojo hfJudgmentDetailPojo, HttpSession session) throws Exception {
        String fileUuid = hfJudgmentDetailPojo.getFileUuid();
        String fileVersionId = hfJudgmentDetailPojo.getFileVersionId();
        return fileOperationService.getTendParamsListService(fileUuid, fileVersionId);
    }

    @PostMapping("/unitTrans")
    public TResponseVo unitTrans(@RequestBody DocframeParamsUnitDiPojo docframeParamsUnitDiPojo) throws Exception {
        String value = docframeParamsUnitDiPojo.getValue();
        String unitId = docframeParamsUnitDiPojo.getUnitId();
        String tagUnitId = docframeParamsUnitDiPojo.getTagUnitId();
        return fileOperationService.unitTransService(value, unitId, tagUnitId);
    }

    @PostMapping("/getUnitList")
    public TResponseVo getUnitList(@RequestBody(required = false) DocframeParamsUnitDiPojo docframeParamsUnitDiPojo) throws Exception {
        List<String> nodeIds = null;
        if (docframeParamsUnitDiPojo != null) {
            nodeIds = docframeParamsUnitDiPojo.getNodeIds();
        }
        return fileOperationService.getUnitListService(nodeIds);
    }

    @PostMapping("/getParamDetail")
    public TResponseVo getParamDetail(@RequestBody DocParamsPojo docParamsPojo) throws Exception {
        String fileUuid = docParamsPojo.getFileUuid();
        String fileVersionId = docParamsPojo.getFileVersionId();
        String uuid = docParamsPojo.getUuid();
        return fileOperationService.getParamDetailService(fileUuid, fileVersionId, uuid);
    }

    /**
     * 获取同项目下的文件列表
     */
    @GetMapping("/getProjectMateFile")
    public TResponseVo getProjectMateFile(String projectId) throws Exception {
        return fileOperationService.getProjectMateFileService(projectId);
    }

    /**
     * 文件复合
     */
    @PostMapping("/newCompoundFile")
    public TResponseVo newCompoundFile(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        return fileOperationService.newCompoundFileService(docFileIndexPojo);
    }

    /**
     * 项目投递
     */
    @PostMapping("/newDeliverFile")
    public TResponseVo newDeliverFile(@RequestBody DocFileIndexPojo docFileIndexPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docFileIndexPojo.setCreateUserId(userId);
        return fileOperationService.newDeliverFileService(docFileIndexPojo);
    }

    /**
     * 查看投递记录
     */
    @GetMapping("/getDeliverRec")
    public TResponseVo getDeliverRec(String fileUuid, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return fileOperationService.getDeliverRecService(fileUuid, userId);
    }

    /**
     * 获取数据源-辅助填写预备数据
     */
    @GetMapping("/getParamSourceDi")
    public TResponseVo getParamSourceDi(HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        return fileOperationService.getParamSourceDiService(userId);
    }

    /**
     * 辅助工具填写-获取预填空数量
     */
    @GetMapping("/getAutoInputList")
    public TResponseVo getAutoInputList(DocParamsPojo docParamsPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        docParamsPojo.setUserId(userId);
        return fileOperationService.getAutoInputListService(docParamsPojo);
    }

    /**
     * 辅助工具-自动填写
     */
    @PostMapping("/autoInput")
    public TResponseVo autoInput(@RequestBody HfSupToolFormPojo hfSupToolFormPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfSupToolFormPojo.setUserId(userId);
        return fileOperationService.autoInputService(hfSupToolFormPojo);
    }

    /**
     * 获取参数使用场景维表
     */
    @GetMapping("/getParamSaturationList")
    public TResponseVo autoInput() throws Exception {
        return fileOperationService.getParamSaturationListService();
    }

    /**
     * 获取参数填写记录
     */
    @PostMapping("/getParamChangeHistory")
    public TResponseVo getParamChangeHistory(@RequestBody HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception {
        return fileOperationService.getParamChangeHistoryService(hfParamChangeHistoryPojo);
    }

    /**
     * 清除参数填写记录
     */
    @PostMapping("/clearParamChangeHistory")
    public TResponseVo clearParamChangeHistory(@RequestBody HfParamChangeHistoryPojo hfParamChangeHistoryPojo) throws Exception {
        return fileOperationService.clearParamChangeHistoryService(hfParamChangeHistoryPojo);
    }


    /**
     * 添加批注
     */
    @PostMapping("/addAnnotate")
    public TResponseVo addAnnotate(@RequestBody OutLinePojo outLinePojo) throws Exception {
        return fileOperationService.addAnnotateService(outLinePojo);
    }


    /**
     * 获取变更记录
     */
    @PostMapping("/getCompareInfoList")
    public TResponseVo getCompareInfoList(@RequestBody OutLinePojo outLinePojo) throws Exception {
        return fileOperationService.getCompareInfoListService(outLinePojo);
    }


    /**
     * 查看变更信息
     */
    @GetMapping("/getCompareInfoDetail")
    public void getCompareInfoDetail(OutLinePojo outLinePojo, HttpServletResponse response) throws Exception {
        String fileUuid = outLinePojo.getFileUuid();
        String fileVersionId = outLinePojo.getFileVersionId();
        String compare0 = outLinePojo.getCompare0();  // compare0与 fileUuid,fileVersionId互斥
        String compare1 = outLinePojo.getCompare1();  // compare1与 fileUuid,fileVersionId互斥
        String uuid = outLinePojo.getUuid();
        fileOperationService.getCompareInfoDetailService(fileUuid, fileVersionId, compare0, compare1, uuid, response);
    }

    /**
     * 获取可对比记录
     */
    @PostMapping("/getCompareVersionList")
    public TResponseVo getCompareVersionList(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String fileVersionId = docFileIndexPojo.getFileVersionId();
        return fileOperationService.getCompareVersionListService(fileUuid, fileVersionId);
    }

    /**
     * 手动对比结果
     */
    @PostMapping("/handCompare")
    public TResponseVo handCompare(@RequestBody DocFileIndexPojo docFileIndexPojo) throws Exception {
        String fileUuid = docFileIndexPojo.getFileUuid();
        String[] split = fileUuid.split("-");
        fileUuid = split[0];
        String fileVersionId = split[1];
        String fileUuid2 = docFileIndexPojo.getFileUuid2();
        String[] split2 = fileUuid2.split("-");
        fileUuid2 = split2[0];
        String fileVersionId2 = split2[1];
        return fileOperationService.handCompareService(fileUuid, fileVersionId, fileUuid2, fileVersionId2);
    }

    /**
     * 模型定义评标要素-总体方案
     */
    @PostMapping("/confirmAssessTotalPlan")
    public TResponseVo confirmAssessTotalPlan(@RequestBody HfContentAssessPojo hfContentAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfContentAssessPojo.setCreateUserId(userId);
        return fileOperationService.confirmAssessTotalPlanService(hfContentAssessPojo);
    }

    /**
     * 模型定义评标要素-获取总体方案内容
     */
    @PostMapping("/getAssessTotalPlan")
    public TResponseVo getAssessTotalPlan(@RequestBody HfContentAssessPojo hfContentAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfContentAssessPojo.setCreateUserId(userId);
        return fileOperationService.getAssessTotalPlanService(hfContentAssessPojo);
    }

    /**
     * 模型定义评标要素-获取备选方案下拉表
     */
    @PostMapping("/getAssessPlanList")
    public TResponseVo getAssessPlanList(@RequestBody HfContentAssessPojo hfContentAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfContentAssessPojo.setCreateUserId(userId);
        return fileOperationService.getAssessPlanListService(hfContentAssessPojo);
    }

    /**
     * 模型定义评标要素-获取总体方案中的方案列表
     */
    @PostMapping("/getAssessElementListInPlan")
    public TResponseVo getAssessElementListInPlan(@RequestBody HfContentAssessPojo hfContentAssessPojo, HttpSession session) throws Exception {
        return fileOperationService.getAssessElementListInPlanService(hfContentAssessPojo);
    }


    /**
     * 模型定义评标要素-查询具体方案内容
     */
    @PostMapping("/getAssessElementList")
    public TResponseVo getAssessElementList(@RequestBody HfContentAssessPojo hfContentAssessPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfContentAssessPojo.setCreateUserId(userId);
        return fileOperationService.getAssessElementListService(hfContentAssessPojo);
    }


    /**
     * 模型定义评标要素-获取元素信息
     */
    @PostMapping("/getAssessElementInfo")
    public TResponseVo getAssessElementInfo(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        return fileOperationService.getAssessElementInfoService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-修改方案中某个元素
     */
    @PostMapping("/updateAssessElement")
    public TResponseVo updateAssessElement(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        return fileOperationService.updateAssessElementService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-删除方案中某个元素
     */
    @PostMapping("/delAssessElement")
    public TResponseVo delAssessElement(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        return fileOperationService.delAssessElementService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-在方案中新增元素
     */
    @PostMapping("/addAssessElement")
    public TResponseVo addAssessElement(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfContentAssessElementPojo.setCreateUserId(userId);
        return fileOperationService.addAssessElementService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-元素排序
     */
    @PostMapping("/orderAssessElement")
    public TResponseVo orderAssessElement(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        return fileOperationService.orderAssessElementService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-向左边插入表格
     */
    @PostMapping("/useAssessTotalPlan")
    public TResponseVo useAssessTotalPlan(@RequestBody HfContentAssessElementPojo hfContentAssessElementPojo) throws Exception {
        return fileOperationService.useAssessTotalPlanService(hfContentAssessElementPojo);
    }

    /**
     * 模型定义评标要素-获取当前文章已用参数
     */
    @PostMapping("/getContentAssessParam")
    public TResponseVo getContentAssessParam(@RequestBody HfContentAssessPojo hfContentAssessPojo) throws Exception {
        return fileOperationService.getContentAssessParamService(hfContentAssessPojo);
    }

    /**
     * 模型定义评标要素-提交前检查合规性
     */
    @PostMapping("/checkBeforeConfirm")
    public TResponseVo checkBeforeConfirm(@RequestBody HfContentAssessPojo hfContentAssessPojo) throws Exception {
        return fileOperationService.checkBeforeConfirmService(hfContentAssessPojo);
    }

    /**
     * 模型定义评标要素-获取文内标注列表
     */
    @PostMapping("/getContentAssessTag")
    public TResponseVo getContentAssessTag(@RequestBody HfContentAssessPojo hfContentAssessPojo) throws Exception {
        return fileOperationService.getContentAssessTagService(hfContentAssessPojo);
    }


    /**
     * 模型定义评标要素-获取文内标注列表
     */
    @GetMapping("/getFileParamsWriteList")
    public TResponseVo getFileParamsWriteList(DocParamsPojo docParamsPojo) throws Exception {
        return fileOperationService.getFileParamsWriteListService(docParamsPojo);
    }


    /**
     * 文内使用数据库-新建
     */
    @PostMapping("/addAndDelContentDb")
    public TResponseVo addAndDelContentDb(@RequestBody HfDmContentUseTablePojo hfDmContentUseTablePojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmContentUseTablePojo.setCreateUserId(userId);
        return fileOperationService.addAndDelContentDbService(hfDmContentUseTablePojo);
    }


    /**
     * 文内使用数据库-新建
     */
    @PostMapping("/getContentDb")
    public TResponseVo getContentDb(@RequestBody HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        return fileOperationService.getContentDbService(hfDmContentUseTablePojo);
    }

    /**
     * 右侧列表中删除数据表
     */
    @PostMapping("/delContentDb")
    public TResponseVo delContentDb(@RequestBody HfDmContentUseTablePojo hfDmContentUseTablePojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmContentUseTablePojo.setUserId(userId);
        hfDmContentUseTablePojo.setSearchUuid(pageId);
        return fileOperationService.delContentDbService(hfDmContentUseTablePojo);
    }

    /**
     * 编辑右侧数据表列表-修改
     */
    @PostMapping("/updateContentDb")
    public TResponseVo updateContentDb(@RequestBody HfDmContentUseTablePojo hfDmContentUseTablePojo, HttpSession session, @RequestHeader("pageId") String pageId) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        hfDmContentUseTablePojo.setUserId(userId);
        hfDmContentUseTablePojo.setSearchUuid(pageId);
        return fileOperationService.updateContentDbService(hfDmContentUseTablePojo);
    }
    /**
     * 编辑右侧数据表列表-获取卷出来的表
     */
    @PostMapping("/getRollTableList")
    public TResponseVo getRollTableList(@RequestBody HfDmContentUseTablePojo hfDmContentUseTablePojo) throws Exception {
        return fileOperationService.getRollTableListService(hfDmContentUseTablePojo);
    }

}
