//package cn.nebulaedata.listener;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.RabbitHandler;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// * @author 徐衍旭
// * @date 2021/8/24 16:15
// * @note
// */
//public class FileListener {
//    private static Logger LOG = LoggerFactory.getLogger(FileListener.class);
//
//    @Value("${doc-frame-service-log.file-pdf-save-path}")
//    private String filePdfSavePath;
//    @Value("${doc-frame-service-log.pdf-main-page-img-local-path}")
//    private String pdfMainPageImgLocalPath;
//    @Value("${doc-frame-service-log.file-search-index-del-mq}")
//    private String fileSearchIndexDelMq;
//
//    @Autowired
//    private DocLogUtils docLogUtils;
//    @Autowired
//    private WordParseUtils wordParseUtils;
//    @Autowired
//    private LogMapper logMapper;
//
//    @RabbitListener(queues = "${doc-frame-service-log.file_search_index_mq}")
//    @RabbitHandler
//    @Transactional(rollbackFor = Exception.class)
//    public void fileSearchInsert(Object msgBytes) throws Exception {
//        String msg = "";
//        try {
//            if ((((Message) msgBytes).getBody()) instanceof byte[]) {
//                msg = new String((byte[])(((Message) msgBytes).getBody()));
//            }
//            Boolean aBoolean = docLogUtils.insertFlleIndexSearchTask(msg);
//            if (aBoolean == true) {
//                LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}","fileSearchInsert",msg,"搜索索引插入成功");
//            } else {
//                LOG.info("[Class]:{}-[BusinessId]:{}-[Msg]:{}","fileSearchInsert",msg,"搜索索引插入失败");
//            }
//        } catch (Exception e) {
//            LOG.error("[Class]:{}-[BusinessId]:{}-[Msg]:{}","fileSearchInsert",msg,"接收报错");
//        }
//    }
//}
