package cn.nebulaedata.service;

import cn.nebulaedata.pojo.EditToolFolderPojo;
import cn.nebulaedata.pojo.HfAuditingResultNoticePojo;
import cn.nebulaedata.pojo.HfRoomPojo;
import cn.nebulaedata.vo.TResponseVo;

import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2023/11/30 15:35
 * @note 协同编辑接口
 */
public interface CollaborateService {

    /**
     * 文件协同编辑房间
     */
    /**
     * 创建协同编辑房间
     *
     * @return
     * @throws Exception
     */
    public TResponseVo createHfRoomService(HfRoomPojo hfRoomPojo) throws Exception;


    /**
     * 获取房间
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getHfRoomInfoService(String fileUuid, String fileVersionId) throws Exception;


    /**
     * 获取房间
     *
     * @return
     * @throws Exception
     */
    public TResponseVo updateHfRoomService(String fileUuid, String fileVersionId, Object settings, String userId) throws Exception;

    /**
     * 关闭房间
     *
     * @return
     * @throws Exception
     */
    public TResponseVo closeHfRoomService(String fileUuid, String fileVersionId, String userId) throws Exception;



    /**
     * 获取用户登录状态
     *
     * @return
     * @throws Exception
     */
    public TResponseVo getUserStatusService(String userId) throws Exception;


    /**
     * 邀请加入房间
     */
    /**
     * 邀请用户
     */
    public TResponseVo inviteUserService(List<Map<String, Object>> inviteUserList, String fileUuid, String fileVersionId, String userId) throws Exception;

    /**
     * 修改用户权限
     */
    public TResponseVo changeUserPermissionService(String noticeUserId, String fileUuid, String fileVersionId, String permission, Boolean admin, String userId) throws Exception;

    /**
     * 移除用户
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo removeUserService(String noticeUserId, String fileUuid, String fileVersionId, String userId) throws Exception;


    /**
     * 用户申请加入
     *
     * @param
     * @return
     * @throws Exception
     */
    public TResponseVo userJoinApplicationService(String fileUuid, String fileVersionId, String userId) throws Exception;

    /**
     * 处理用户申请
     * @param fileUuid
     * @param fileVersionId
     * @param result
     * @param applyUserId
     * @param userId
     * @return
     * @throws Exception
     */
    public TResponseVo handleJoinApplicationService(String fileUuid, String fileVersionId, Boolean result, String permission, String applyUserId, String userId) throws Exception;

}
