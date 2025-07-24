package cn.nebulaedata.dao;

import cn.nebulaedata.pojo.HfAuditingResultNoticePojo;
import cn.nebulaedata.pojo.HfRoomPojo;

/**
 * @author 徐衍旭
 * @date 2023/3/15 14:32
 * @note
 */
public interface CollaborateMapper {

    /**
     * 创建房间
     *
     * @param hfRoomPojo
     * @return
     */
    public Integer createHfRoomDao(HfRoomPojo hfRoomPojo);

    /**
     * 查询房间信息
     *
     * @param
     * @return
     */
    public HfRoomPojo getRoomInfoDao(String fileUuid, String fileVersionId);
    public HfRoomPojo getHfRoomInfoDao(String roomId);


    /**
     * 通知被邀请用户
     * @param hfAuditingResultNoticePojo
     * @return
     */
    public Integer noticeUserDao(HfAuditingResultNoticePojo hfAuditingResultNoticePojo);


}
