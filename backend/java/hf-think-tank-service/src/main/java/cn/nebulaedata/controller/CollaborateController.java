package cn.nebulaedata.controller;

import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.DocUserPojo;
import cn.nebulaedata.pojo.EditToolFolderPojo;
import cn.nebulaedata.pojo.HfRoomPojo;
import cn.nebulaedata.service.impl.CollaborateServiceImpl;
import cn.nebulaedata.service.impl.EditToolServiceImpl;
import cn.nebulaedata.service.impl.FileIndexServiceImpl;
import cn.nebulaedata.service.impl.FileOperationServiceImpl;
import cn.nebulaedata.vo.TResponseVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2023/12/4 09:57
 * @note
 */

@RestController
@RequestMapping("/web/teamWork")
public class CollaborateController {
    private static final Logger LOG = LoggerFactory.getLogger(EditToolController.class);

    @Autowired
    private EditToolServiceImpl editToolServiceImpl;
    @Autowired
    private CollaborateServiceImpl collaborateServiceImpl;

    @PostMapping("/createHfRoom")
    public TResponseVo createHfRoom(@RequestBody HfRoomPojo hfRoomPojo) throws Exception {
        return collaborateServiceImpl.createHfRoomService(hfRoomPojo);
    }

    @GetMapping("/getUserStatus")
    public TResponseVo getUserStatus(String userId) throws Exception {
//        Object userId = map.get("userId");
        if (userId != null) {
            return collaborateServiceImpl.getUserStatusService(String.valueOf(userId));
        } else {
            throw new WorkTableException("必填参数为空");
        }
    }

    @PostMapping("/getHfRoomInfo")
    public TResponseVo getHfRoomInfo(@RequestBody HfRoomPojo hfRoomPojo) throws Exception {
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        return collaborateServiceImpl.getHfRoomInfoService(fileUuid, fileVersionId);
    }

    @PostMapping("/updateHfRoom")
    public TResponseVo updateHfRoom(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        Object settings = hfRoomPojo.getSettings();
        return collaborateServiceImpl.updateHfRoomService(fileUuid, fileVersionId, settings, userId);
    }

    @PostMapping("/closeHfRoom")
    public TResponseVo closeHfRoom(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        return collaborateServiceImpl.closeHfRoomService(fileUuid, fileVersionId, userId);
    }

    @PostMapping("/inviteUser")
    public TResponseVo inviteUser(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        List<Map<String, Object>> inviteUserList = hfRoomPojo.getInviteUserList();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        return collaborateServiceImpl.inviteUserService(inviteUserList, fileUuid, fileVersionId, userId);
    }

    @PostMapping("/changeUserPermission")
    public TResponseVo changeUserPermission(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String noticeUserId = hfRoomPojo.getNoticeUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        String permission = hfRoomPojo.getPermission();
        Boolean admin = hfRoomPojo.getAdmin();
        return collaborateServiceImpl.changeUserPermissionService(noticeUserId, fileUuid, fileVersionId, permission, admin, userId);
    }

    @PostMapping("/removeUser")
    public TResponseVo removeUser(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String noticeUserId = hfRoomPojo.getNoticeUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        return collaborateServiceImpl.removeUserService(noticeUserId, fileUuid, fileVersionId, userId);
    }

    @PostMapping("/userJoinApplication")
    public TResponseVo userJoinApplication(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        return collaborateServiceImpl.userJoinApplicationService(fileUuid, fileVersionId, userId);
    }

    @PostMapping("/handleJoinApplication")
    public TResponseVo handleJoinApplication(@RequestBody HfRoomPojo hfRoomPojo, HttpSession session) throws Exception {
        DocUserPojo user = (DocUserPojo) session.getAttribute("user");
        String userId = user.getUserId();
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        String applyUserId = hfRoomPojo.getApplyUserId();
        Boolean result = hfRoomPojo.getResult();
        String permission = hfRoomPojo.getPermission();
        return collaborateServiceImpl.handleJoinApplicationService(fileUuid, fileVersionId, result, permission, applyUserId, userId);
    }


}
