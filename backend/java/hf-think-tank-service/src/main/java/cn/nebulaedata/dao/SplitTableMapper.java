package cn.nebulaedata.dao;

import cn.nebulaedata.pojo.*;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author 徐衍旭
 * @date 2021/8/12 15:09
 * @note
 */
public interface SplitTableMapper {
    /**
     * 创建招标文件-数据管理
     */
    public Integer newBidDocumentContentDmDataDao(@Param("bidFileIndex") DocFileIndexPojo bidFileIndex, @Param("DbNo1") String DbNo1, @Param("DbNo2") String DbNo2,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 创建数据管理
     */
    public Integer newTendContentDmDataDao(@Param("docFileIndexPojo") DocFileIndexPojo docFileIndexPojo, @Param("DbNo1") String DbNo1, @Param("DbNo2") String DbNo2,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 新增数据
     *
     * @param
     * @return
     */
    public Integer addDmDataInFileDao(@Param("hfDmData") HfDmData hfDmData, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取当前最大顺序
     *
     * @param
     * @return
     */
    public Integer getNowDataOrderInFileDao(@Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 删除数据
     *
     * @param
     * @return
     */
    public Integer delDmDataInFileDao(@Param("tableId") String tableId, @Param("dataId") String dataId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);


    /**
     * 变更数据
     *
     * @param
     * @return
     */
    public Integer chgDmDataInFileDao(@Param("hfDmData") HfDmData hfDmData, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取数据清单
     *
     * @param
     * @return
     */
    public List<HfDmData> getDmDataListInFileDao(@Param("createUserId") String createUserId, @Param("tableId") String tableId, @Param("paramNameLike") String paramNameLike, @Param("sql") String sql, @Param("sortBySql") String sortBySql, @Param("limit") Integer limit, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    public List<Map<String, Object>> getDmDataListInFileGroupByDao(@Param("createUserId") String createUserId, @Param("tableId") String tableId, @Param("paramNameLike") String paramNameLike, @Param("sql") String sql, @Param("sortBySql") String sortBySql, @Param("groupBySql") String groupBySql, @Param("groupBySqlWithName") String groupBySqlWithName, @Param("groupBySqlNotNull") String groupBySqlNotNull, @Param("groupByFunctionsSql") String groupByFunctionsSql, @Param("limit") Integer limit, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
    public List<String> getTotalKeysDao(@Param("createUserId") String createUserId, @Param("tableId") String tableId, @Param("paramNameLike") String paramNameLike, @Param("sql") String sql, @Param("sortBySql") String sortBySql, @Param("limit") Integer limit, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
    public List<HfDmData> getNoSortDataInFileDao(@Param("tableId") String tableId, @Param("keyList") List<String> keyList, @Param("limit") Integer limit, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取数据信息
     *
     * @param
     * @return
     */
    public Integer getDmDataTotalInFileDao(@Param("createUserId") String createUserId, @Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 上传表数据
     *
     * @param
     * @return
     */
    public Integer uploadHfDmDataInFileDao(@Param("lists") List<HfDmData> lists, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);


    /**
     * 从数据管理导入数据
     *
     * @param
     * @return
     */
    public Integer importDmDataDao(@Param("tableId") String tableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("newTableId") String newTableId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 清空目标表数据
     * @param tableId
     * @param fileUuid
     * @param fileVersionId
     * @param DbNo
     * @param dmContentDataDatabase
     * @return
     */
    public Integer clearTableDataDao(@Param("tableId") String tableId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 从数据管理单纯导入数据 包含表头 包含data
     *
     * @param
     * @return
     */
    public Integer importOnlyDataDao(@Param("fromTableId") String fromTableId, @Param("toTableId") String toTableId,@Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);




    public Integer newSubsidiaryContentDmDataDao(@Param("docFileIndexPojo")DocFileIndexPojo docFileIndexPojo, @Param("DbNo1") String DbNo1, @Param("DbNo2") String DbNo2,@Param("dmContentDataDatabase") String dmContentDataDatabase);
    public Integer addDeriveContentDmDataDao(@Param("docFileIndexPojo")DocFileIndexPojo docFileIndexPojo, @Param("DbNo1") String DbNo1, @Param("DbNo2") String DbNo2,@Param("dmContentDataDatabase") String dmContentDataDatabase);
    public Integer addVersionContentDmDataDao(@Param("docFileIndexPojo")DocFileIndexPojo docFileIndexPojo, @Param("DbNo1") String DbNo1, @Param("DbNo2") String DbNo2,@Param("dmContentDataDatabase") String dmContentDataDatabase);
    /**
     * 获取文内数据表信息
     *
     * @param
     * @return
     */
    public List<HfDmContentData> getDmDataInFileDao(@Param("rows") List rows,@Param("tableId") String tableId,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 获取数据dataId清单
     * @param tableId
     * @param sortBySql
     * @param fileUuid
     * @param fileVersionId
     * @param DbNo
     * @param dmContentDataDatabase
     * @return
     */
    public List<String> getDmDataIdListInFileDao(@Param("tableId") String tableId,@Param("sortBySql") String sortBySql,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 文内表数据排序
     * @param sql
     * @return
     */
    public Integer orderDmDataInFileDao(@Param("sql") String sql);

    /**
     * 清空文内表数据
     * @param tableId
     * @param fileUuid
     * @param fileVersionId
     * @param DbNo
     * @param dmContentDataDatabase
     * @return
     */
    public Integer clearDmDataInFileDao(@Param("tableId") String tableId,@Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 复制文内数据表数据
     * @param tableId
     * @param newTableId
     * @param userId
     * @param fileUuid
     * @param fileVersionId
     * @return
     */
    public Integer copyDmDataInFileDao(@Param("tableId") String tableId, @Param("newTableId") String newTableId, @Param("userId") String userId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 根据tableId获取data_content字段内容
     * @param tableId
     * @param dataId
     * @param fileUuid
     * @param fileVersionId
     * @param DbNo
     * @param dmContentDataDatabase
     * @return
     */
    public List<HfDmData> getDmDataListByTableIdInFileDao(@Param("tableId") String tableId, @Param("dataId") String dataId, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

    /**
     * 根据tableId获取data_content
     * @param importTableIdList
     * @param fileUuid
     * @param fileVersionId
     * @param DbNo
     * @param dmContentDataDatabase
     * @return
     */
    public List<HfDmData> getDmTableAllDataInFileDao(@Param("importTableIdList") List<String> importTableIdList, @Param("fileUuid") String fileUuid, @Param("fileVersionId") String fileVersionId, @Param("DbNo") String DbNo,@Param("dmContentDataDatabase") String dmContentDataDatabase);

}
