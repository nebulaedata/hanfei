package cn.nebulaedata.service.impl;

import cn.nebulaedata.dao.CollaborateMapper;
import cn.nebulaedata.dao.EditToolMapper;
import cn.nebulaedata.dao.FileOperationMapper;
import cn.nebulaedata.dao.WorkingTableMapper;
import cn.nebulaedata.exception.EditToolException;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.pojo.*;
import cn.nebulaedata.service.CollaborateService;
import cn.nebulaedata.service.EditToolService;
import cn.nebulaedata.socket.RoomServer;
import cn.nebulaedata.utils.AmqUtils;
import cn.nebulaedata.utils.JsonKeyUtils;
import cn.nebulaedata.utils.RedisUtils;
import cn.nebulaedata.vo.TResponseVo;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.*;

import static cn.nebulaedata.utils.AmqUtils.buildQueue;
import static cn.nebulaedata.utils.AmqUtils.destroyQueue;

/**
 * @author 徐衍旭
 * @date 2023/3/15 14:11
 * @note
 */
@Service
public class CollaborateServiceImpl implements CollaborateService {

    private static final Logger LOG = LoggerFactory.getLogger(CollaborateServiceImpl.class);

    @Autowired
    private RoomServer roomServer;
    @Autowired
    private CollaborateMapper collaborateMapper;
    @Autowired
    private FileOperationMapper fileOperationMapper;
    @Autowired
    private WorkingTableMapper workingTableMapper;
    @Autowired
    private FileOperationServiceImpl fileOperationServiceImpl;
    @Autowired
    private RedisUtils redisUtils;
    @Value("${doc-frame-service.env-name}")
    private String envName;

    /**
     * 创建协同编辑房间
     *
     * @param hfRoomPojo
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo createHfRoomService(HfRoomPojo hfRoomPojo) throws Exception {
        String fileUuid = hfRoomPojo.getFileUuid();
        String fileVersionId = hfRoomPojo.getFileVersionId();
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            throw new WorkTableException("必填参数为空");
        }
        List<Map<String, Object>> userList = hfRoomPojo.getUserList();// 需要通知 且 拥有房间直接进入权(无需管理员确认) 的人员名单
        if (userList == null || userList.size() == 0) {
            throw new WorkTableException("房间未指定人员名单");
        } else {
            for (Map<String, Object> map : userList) {
                map.put("leaveTime", new Date());
            }
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间已经存在,请刷新状态查看");
        }

        // 创建房间消息队列mq
        String queueName = "hf-" + envName + "-" + fileUuid + fileVersionId;
        Channel channel = buildQueue(queueName); // 如果队列已经存在 不会重复创建
        // 获取队列的消费者数量
        int consumerCount = (channel.queueDeclarePassive(queueName)).getMessageCount();
        if (consumerCount == 0) {
            // 创建消费者
            /**
             * 1.queue:队列名称
             * 2.aotoACK：是否自动确认
             * 3.callback：回调对象
             */
            Consumer consumer = new DefaultConsumer(channel) {
                /**
                 * 回调方法。当收到消息之后会自动执行该方法
                 *
                 * @param consumerTag 标识
                 * @param envelope    获取一些信息，交换机，路由Key
                 * @param properties  配置信息
                 * @param body        数据
                 * @throws IOException
                 */
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String msg = new String(body);
                    DocParamsPojo paramsPojo = JSON.parseObject(msg, DocParamsPojo.class);
                    try {
                        paramsPojo.setWriteWay("team");
                        fileOperationServiceImpl.writeParamService(paramsPojo);
                        // 查询当前文件的房间是否存在
                        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
                        if (!redisUtils.hasKey(roomKey)) {
                            throw new WorkTableException("协同编辑房间未创建");
                        }
                        // 获取房间信息
                        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
                        String roomId = (String) roomMap.get("roomId");

                        // ws 填参完成后通知
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("role", "feedback");
                        map.put("data", new HashMap<String, Object>());
                        map.put("action", "completeWriteParam");
                        roomServer.sendInfo(map, roomId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            channel.basicConsume(queueName, true, consumer);
        }

        // redis创建房间信息
        HashMap<String, Object> roomInfoMap = new HashMap<>();
        roomInfoMap.put("userList", userList);
        String roomId = Base64.getEncoder().encodeToString((fileUuid + "/" + fileVersionId + "/" + UUID.randomUUID().toString().replaceAll("-", "")).getBytes());
        roomInfoMap.put("roomId", roomId);
        roomInfoMap.put("settings", hfRoomPojo.getSettings());
        redisUtils.set(roomKey, roomInfoMap);
        // 启动定时任务消费roomServerVariables消息

        // 获取管理员信息
        DocUserPojo userInfoDao = new DocUserPojo();
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                userInfoDao = workingTableMapper.getUserInfoDao(String.valueOf(userMap.get("userId")));
                break;
            }
        }
        // 获取文件信息
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        // 通知组员(排除管理员自己)
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin == null || !(Boolean) admin) {
                // 通知
                HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
                hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
                hfAuditingResultNoticePojo.setAuditingContent(userInfoDao.getUserName() + "邀请您共同编辑文件《" + docAllInfoDao.getFileName() + "》");
                hfAuditingResultNoticePojo.setFileUuid(fileUuid);
                hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
                hfAuditingResultNoticePojo.setNoticeUserId(String.valueOf(userMap.get("userId")));
                hfAuditingResultNoticePojo.setNoticeType("room_invite");
                hfAuditingResultNoticePojo.setIsRead(false);
                collaborateMapper.noticeUserDao(hfAuditingResultNoticePojo);
            }
        }
        return TResponseVo.success(roomInfoMap);
    }

    /**
     * 获取用户登录状态
     *
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getUserStatusService(String userId) throws Exception {
        String[] strings = redisUtils.dimSearch("login_" + envName + "_" + userId + "_*");
        if (strings.length != 0) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", true);
            return TResponseVo.success(map);
        } else {
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", false);
            return TResponseVo.success(map);
        }
    }

    /**
     * 获取房间信息
     *
     * @param fileUuid
     * @param fileVersionId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo getHfRoomInfoService(String fileUuid, String fileVersionId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (redisUtils.hasKey(roomKey)) {
            HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
            return TResponseVo.success(roomMap);
        } else {
            return TResponseVo.error("协同编辑房间未创建");
//            throw new WorkTableException("协同编辑房间未创建");
        }
    }

    /**
     * 获取房间
     *
     * @param fileUuid
     * @param fileVersionId
     * @param settings
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo updateHfRoomService(String fileUuid, String fileVersionId, Object settings, String userId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || settings == null || StringUtils.isBlank(userId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            return TResponseVo.success("协同编辑空间不存在或已关闭");
        }

        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");

        // 获取管理员信息
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                if (!userId.equals(String.valueOf(userMap.get("userId")))) {
                    throw new WorkTableException("您不是房间管理员,不能设置房间信息");
                }
                break;
            }
        }

        roomMap.put("settings", settings);
        redisUtils.set(roomKey, roomMap);

        return TResponseVo.success("修改成功");
    }

    /**
     * 关闭房间
     *
     * @param fileUuid
     * @param fileVersionId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo closeHfRoomService(String fileUuid, String fileVersionId, String userId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(userId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            return TResponseVo.success("协同编辑空间不存在或已关闭");
        }

        try {
            // 获取房间信息
            HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
            List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");
            // 获取管理员信息
            for (Map<String, Object> userMap : userList) {
                Object admin = userMap.get("admin");
                if (admin != null && (Boolean) admin) {
                    if (!userId.equals(String.valueOf(userMap.get("userId")))) {
                        throw new WorkTableException("您不是房间管理员,不能关闭房间");
                    }
                    break;
                }
            }

            redisUtils.del(roomKey);

            // 根据redis房间信息 维护queue
            // 1.创建房间消息队列mq (已存在不会重建)
            String queueName = "hf-" + envName + "-" + fileUuid + fileVersionId;
            destroyQueue(queueName);

        } catch (Exception e) {
            return TResponseVo.error("协同编辑空间关闭失败");
        }

        return TResponseVo.success("协同编辑空间关闭成功");
    }


    /**
     * 邀请用户
     *
     * @param inviteUserList
     * @param fileUuid
     * @param fileVersionId
     * @param adminUserId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo inviteUserService(List<Map<String, Object>> inviteUserList, String fileUuid, String fileVersionId, String adminUserId) throws Exception {
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间未创建");
        }
        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");
        String roomId = (String) roomMap.get("roomId");

        // 记录redis userIdList
        ArrayList<String> userIdList = new ArrayList<>();

        // 获取管理员信息
        DocUserPojo userInfoDao = new DocUserPojo();
        for (Map<String, Object> userMap : userList) {
            userIdList.add((String) userMap.get("userId"));
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                if (!adminUserId.equals(String.valueOf(userMap.get("userId")))) {
                    throw new WorkTableException("您不是房间管理员,不能邀请用户");
                }
                userInfoDao = workingTableMapper.getUserInfoDao(String.valueOf(userMap.get("userId")));
            }
        }

        // 判断邀请用户重复 重复的不邀请
        inviteUserList.removeIf(k -> userIdList.contains(k.get("userId")));
        for (Map<String, Object> map : inviteUserList) {
            map.put("leaveTime", new Date());
        }
        // 更新用户清单
        userList.addAll(inviteUserList);
        // 更新房间用户信息
        redisUtils.set(roomKey, roomMap);

        // 获取文件信息
        DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
        for (Map<String, Object> noticeUser : inviteUserList) {
            // 通知
            HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
            hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
            hfAuditingResultNoticePojo.setAuditingContent(userInfoDao.getUserName() + "邀请您共同编辑文件《" + docAllInfoDao.getFileName() + "》");
            hfAuditingResultNoticePojo.setFileUuid(fileUuid);
            hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
            hfAuditingResultNoticePojo.setNoticeUserId((String) noticeUser.get("userId"));
            hfAuditingResultNoticePojo.setNoticeType("room_invite");
            hfAuditingResultNoticePojo.setIsRead(false);
            collaborateMapper.noticeUserDao(hfAuditingResultNoticePojo);
        }

        // ws
        HashMap<String, Object> map = new HashMap<>();
        map.put("role", "system");
        map.put("data", new HashMap<String, Object>() {{
            put("allValues", JSON.parseObject(JSON.toJSONString(roomMap), Map.class));
            put("changedValues", JSON.parseObject(JSON.toJSONString(inviteUserList), List.class));
        }});
        map.put("action", "inviteUser");
        roomServer.sendInfoExceptMe(map, roomId, adminUserId);

        return TResponseVo.success("邀请成功");
    }

    /**
     * 修改用户权限
     *
     * @param noticeUserId
     * @param fileUuid
     * @param fileVersionId
     * @param permission
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo changeUserPermissionService(String noticeUserId, String fileUuid, String fileVersionId, String permission, Boolean newAdmin, String adminUserId) throws Exception {
        if (StringUtils.isBlank(noticeUserId) || StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(adminUserId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间未创建");
        }
        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");
        String roomId = (String) roomMap.get("roomId");

        // 获取管理员信息
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                if (!adminUserId.equals(String.valueOf(userMap.get("userId")))) {
                    throw new WorkTableException("您不是房间管理员,不能修改用户权限");
                }
                break;
            }
        }
        // 获取被修改人员信息
        Map changeUserMap = new HashMap<String, Object>();
        for (Map<String, Object> userMap : userList) {
            if (noticeUserId.equals(String.valueOf(userMap.get("userId")))) {  // 被修改的人
                if (permission != null) userMap.put("permission", permission);
                if (newAdmin != null && newAdmin) {  // 转管理员
                    userMap.put("permission", "rw");
                    userMap.put("admin", true);
                }
                changeUserMap = userMap;
            }
            if (adminUserId.equals(String.valueOf(userMap.get("userId")))) {  // 自己
                if (newAdmin != null && newAdmin) {  // 转管理员
                    userMap.put("admin", false);
                }
            }
        }
        // 更新房间用户信息
        redisUtils.set(roomKey, roomMap);

        // ws
        HashMap<String, Object> map = new HashMap<>();
        map.put("role", "system");
        Map finalChangeUserMap = changeUserMap;
        map.put("data", new HashMap<String, Object>() {{
            put("allValues", JSON.parseObject(JSON.toJSONString(roomMap), Map.class));
            put("changedValues", JSON.parseObject(JSON.toJSONString(finalChangeUserMap), Map.class));
        }});
        if (newAdmin != null && newAdmin) {
            map.put("action", "changeAdmin");
        } else {
            map.put("action", "changeUserPermission");
        }
        roomServer.sendInfoExceptMe(map, roomId, adminUserId);
        return TResponseVo.success("修改完成");
    }

    /**
     * 移除用户
     *
     * @param noticeUserId
     * @param fileUuid
     * @param fileVersionId
     * @param adminUserId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo removeUserService(String noticeUserId, String fileUuid, String fileVersionId, String adminUserId) throws Exception {
        if (StringUtils.isBlank(noticeUserId) || StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(adminUserId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间未创建");
        }
        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");
        String roomId = (String) roomMap.get("roomId");

        // 判断是否为自己退出
        if (!adminUserId.equals(noticeUserId)) {
            // 获取管理员信息
            for (Map<String, Object> userMap : userList) {
                Object admin = userMap.get("admin");
                if (admin != null && (Boolean) admin) {
                    if (!adminUserId.equals(String.valueOf(userMap.get("userId")))) {
                        throw new WorkTableException("您不是房间管理员,不能移除用户");
                    }
                    break;
                }
            }
        }

        // 从userList中剔除noticeUserId用户
        final HashMap[] changeUserMap = {new HashMap()};
        userList.removeIf(k -> {
            HashMap hashMap = JSON.parseObject(JSON.toJSONString(k), HashMap.class);
            changeUserMap[0] = hashMap;
            return noticeUserId.equals((String) k.get("userId"));
        });

        // 判断房间是否还有管理员 没有就重新指定,如果无法指定,就是没有人了,就销毁房间
        Boolean hasAdmin = false;
        for (Map<String, Object> map : userList) {
            if (map.get("admin") != null && (Boolean) map.get("admin")) {
                hasAdmin = true;
                break;
            }
        }
        if (!hasAdmin) {
            if (userList.size() == 0) {
                // 销毁房间
                redisUtils.del(roomKey);
                // ws  通知关闭房间
                HashMap<String, Object> map = new HashMap<>();
                map.put("role", "system");
                Map finalChangeUserMap = changeUserMap[0];
                map.put("data", new HashMap<String, Object>() {{
                    put("allValues", new HashMap<>());
                    put("changedValues", JSON.parseObject(JSON.toJSONString(finalChangeUserMap), Map.class));
                }});
                map.put("action", "closeRoom");
                roomServer.sendInfoExceptMe(map, roomId, adminUserId);

                // 构建返回信息
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("roomId", roomId);
                retMap.put("info", "移除成功,空间关闭");
                return TResponseVo.success(retMap);
            } else {
                // 指定第一个人为管理员
                userList.get(0).put("admin", true);
                userList.get(0).put("permission", "rw");

                // 更新房间用户信息
                redisUtils.set(roomKey, roomMap);

                // 构建返回信息
                HashMap<String, Object> retMap = new HashMap<>();
                retMap.put("roomId", roomId);
                retMap.put("userId", userList.get(0).get("userId"));
                retMap.put("info", "移除成功,管理员变化");
                return TResponseVo.success(retMap);
            }
        } else {
            // 更新房间用户信息
            redisUtils.set(roomKey, roomMap);

            // 构建返回信息
            HashMap<String, Object> retMap = new HashMap<>();
            retMap.put("roomId", roomId);
            retMap.put("userId", noticeUserId);
            retMap.put("info", "用户移除成功");
            return TResponseVo.success(retMap);
        }
    }


    /**
     * 用户申请加入
     *
     * @param fileUuid
     * @param fileVersionId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo userJoinApplicationService(String fileUuid, String fileVersionId, String userId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(userId)) {
            throw new WorkTableException("必填参数为空");
        }
        DocUserPojo userInfoDao = workingTableMapper.getUserInfoDao(userId);
        HashMap<String, Object> applyInfo = new HashMap<String, Object>();
        applyInfo.put("userId", userInfoDao.getUserId());
        applyInfo.put("userName", userInfoDao.getUserName());
        applyInfo.put("avatar", userInfoDao.getAvatar());
        applyInfo.put("permission", "ro");
        applyInfo.put("admin", false);
        applyInfo.put("join", false);  //
        applyInfo.put("applyTime", new Date());  // 申请加入用户

        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间未创建");
        }
        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList");
        // 判断用户是否已经是协同编辑的用户
        for (Map<String, Object> userMap : userList) {
            if (userMap.get("userId") != null && ((String) userMap.get("userId")).equals(userId)) {
                throw new WorkTableException("您已获得编辑权限,请在此页面刷新");
            }
        }

        // 判断用户是否已经在申请列表中
        List<Map<String, Object>> waitList = (List<Map<String, Object>>) roomMap.get("waitList"); // 申请列表
        if (waitList != null) {
            Boolean in = false;
            for (Map<String, Object> userMap : waitList) { // 更新申请时间
                if (userMap.get("userId") != null && ((String) userMap.get("userId")).equals(userId)) {
                    in = true;
                    userMap.put("applyTime", new Date());
                    break;
                }
            }
            if (!in) {
                // 创建申请队列
                waitList.add(applyInfo);
                roomMap.put("waitList", waitList);
            }
        } else {
            // 创建申请队列
            List<Map<String, Object>> newWaitList = new ArrayList<>();
            newWaitList.add(applyInfo);
            roomMap.put("waitList", newWaitList);
        }
        redisUtils.set(roomKey, roomMap);

        // 获取管理员信息
        String adminUserId = "";
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                if (userMap.get("userId") != null) adminUserId = String.valueOf(userMap.get("userId"));
                break;
            }
        }

        // 通知管理员消息盒子
        if (StringUtils.isNotBlank(adminUserId)) {
            // 获取文件信息
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            // 通知入库
            HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
            hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
            hfAuditingResultNoticePojo.setAuditingContent(userInfoDao.getUserName() + " 申请共同编辑文件《" + docAllInfoDao.getFileName() + "》");
            hfAuditingResultNoticePojo.setFileUuid(fileUuid);
            hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
            hfAuditingResultNoticePojo.setNoticeUserId(adminUserId);
            hfAuditingResultNoticePojo.setNoticeType("room_apply");
            hfAuditingResultNoticePojo.setIsRead(false);
            collaborateMapper.noticeUserDao(hfAuditingResultNoticePojo);
        }
        return TResponseVo.success("申请完成,请等待管理员审批");
    }


    /**
     * 处理用户申请
     *
     * @param fileUuid
     * @param fileVersionId
     * @param result
     * @param applyUserId
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public TResponseVo handleJoinApplicationService(String fileUuid, String fileVersionId, Boolean result, String permission, String applyUserId, String userId) throws Exception {
        if (StringUtils.isBlank(fileUuid) || StringUtils.isBlank(fileVersionId) || StringUtils.isBlank(applyUserId)) {
            throw new WorkTableException("必填参数为空");
        }
        // 查询当前文件的房间是否存在
        String roomKey = envName + "_" + "roomKey" + "_" + fileUuid + "_" + fileVersionId;
        if (!redisUtils.hasKey(roomKey)) {
            throw new WorkTableException("协同编辑房间未创建");
        }
        // 获取房间信息
        HashMap<String, Object> roomMap = (HashMap<String, Object>) redisUtils.get(roomKey);
        List<Map<String, Object>> userList = (List<Map<String, Object>>) roomMap.get("userList"); // 用户列表
        ArrayList<String> userIdList = new ArrayList<>();
        List<Map<String, Object>> waitList = (List<Map<String, Object>>) roomMap.get("waitList"); // 申请列表

        // 获取管理员信息
        String adminUserId = "";
        String adminUserName = "";
        for (Map<String, Object> userMap : userList) {
            Object admin = userMap.get("admin");
            if (admin != null && (Boolean) admin) {
                if (userMap.get("userId") != null) adminUserId = String.valueOf(userMap.get("userId"));
                if (userMap.get("userName") != null) adminUserName = String.valueOf(userMap.get("userName"));
            }
            userIdList.add((String) userMap.get("userId"));
        }

        for (Map<String, Object> map : waitList) {
            if (applyUserId.equals(map.get("userId"))) {
                if (result) {  // 如果同意
                    // 如果permission不为空 就重新赋值权限
                    if (StringUtils.isNotBlank(permission)) {
                        map.put("permission", permission);
                    }
                    if (!userIdList.contains(applyUserId)) {
                        userList.add(map);
                    }
                } else {  // 拒绝就移除
                    userList.removeIf(k -> applyUserId.equals((String) k.get("userId")));
                }
                waitList.remove(map);
                break;
            }
        }
        redisUtils.set(roomKey, roomMap);

        // 通知申请人消息盒子
        if (StringUtils.isNotBlank(applyUserId)) {
            // 获取文件信息
            DocFileIndexPojo docAllInfoDao = fileOperationMapper.getDocAllInfoDao(fileUuid);
            // 通知入库
            HfAuditingResultNoticePojo hfAuditingResultNoticePojo = new HfAuditingResultNoticePojo();
            hfAuditingResultNoticePojo.setAuditingUuid(UUID.randomUUID().toString().replaceAll("-", ""));
            hfAuditingResultNoticePojo.setAuditingContent(adminUserName + (result ? "同意" : "拒绝") + "您关于共同编辑文件《" + docAllInfoDao.getFileName() + "》的申请");
            hfAuditingResultNoticePojo.setFileUuid(fileUuid);
            hfAuditingResultNoticePojo.setFileVersionId(fileVersionId);
            hfAuditingResultNoticePojo.setNoticeUserId(applyUserId);
            hfAuditingResultNoticePojo.setNoticeType(result ? "room_apply_result_accept" : "room_apply_result_reject");
            hfAuditingResultNoticePojo.setIsRead(false);
            collaborateMapper.noticeUserDao(hfAuditingResultNoticePojo);
        }

        return TResponseVo.success("处理完成");
    }
}
