package cn.nebulaedata.Advice;


import cn.nebulaedata.enums.ResponseEnum;
import cn.nebulaedata.exception.EditToolException;
import cn.nebulaedata.exception.FileIndexException;
import cn.nebulaedata.exception.WorkTableException;
import cn.nebulaedata.vo.TResponseVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author 贾亦真
 * @date 2020/12/30 15:37
 * @note
 */
@ControllerAdvice
public class AdviceExceptionHandler {

//    private static final Logger LOG = LoggerFactory.getLogger(AdviceExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public TResponseVo handle(Exception e) {
        e.printStackTrace();
        return TResponseVo.error(ResponseEnum.ERROR);
    }

    @ExceptionHandler(FileIndexException.class)
    @ResponseBody
    public TResponseVo userHandle(FileIndexException e) throws Exception {
        Integer code = e.getCode();
        if (code == 600) {
            return TResponseVo.error(ResponseEnum.ERROR, "该段落已组合成可替换段落，请先取消组合再拖动。");
        } else if (code == 50) {
            return TResponseVo.error(ResponseEnum.ERROR, "执行sql命令时出错,请联系开发人员");
        } else if (code == 101) {
            return TResponseVo.error(ResponseEnum.ERROR, "该项目名已存在");
        } else if (code == 102) {
            return TResponseVo.error(ResponseEnum.ERROR, "新增项目失败");
        } else if (code == 103) {
            return TResponseVo.error(ResponseEnum.ERROR, "请勿在项目中重复添加同一文件");
        } else if (code == 121) {
            return TResponseVo.error(ResponseEnum.ERROR, "批次编码已存在");
        } else if (code == 141) {
            return TResponseVo.error(ResponseEnum.ERROR, "项目中已存在同名文件，请重新命名");
        } else if (code == 142) {
            return TResponseVo.error(ResponseEnum.ERROR, "更新项目文件属性失败");
        } else if (code == 321) {
            return TResponseVo.error(ResponseEnum.ERROR, "该文章已收藏，请勿重复收藏。");
        } else if (code == 322) {
            return TResponseVo.error(ResponseEnum.ERROR, "检查重复性错误。");
        } else if (code == 700) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，原文档索引不存在。");
        } else if (code == 701) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，原文档版本不存在。");
        } else if (code == 702) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，原文档大纲不存在。");
        } else if (code == 703) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，原文档内容不存在。");
        } else if (code == 704) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败");
        } else if (code == 710) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，创建索引失败。");
        } else if (code == 711) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，创建索引任务失败。");
        } else if (code == 712) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建附属文件失败，创建版本失败。");
        } else if (code == 720) {
            return TResponseVo.error(ResponseEnum.ERROR, "删除附属文件失败");
        } else if (code == 721) {
            return TResponseVo.error(ResponseEnum.ERROR, "保存参数内容失败");
        } else if (code == 722) {
            return TResponseVo.error(ResponseEnum.ERROR, "已加入至文库中，请勿重复添加");
        } else if (code == 723) {
            return TResponseVo.error(ResponseEnum.ERROR, "已加入至文库中，请勿重复添加");
        } else if (code == 801) {
            return TResponseVo.error(ResponseEnum.ERROR, "新增词条失败，该词条名已存在。");
        } else if (code == 802) {
            return TResponseVo.error(ResponseEnum.ERROR, "新增词条失败。");
        } else if (code == 804) {
            return TResponseVo.error(ResponseEnum.ERROR, "新增词条失败。");
        } else if (code == 805) {
            return TResponseVo.error(ResponseEnum.ERROR, "删除词条失败。");
        } else if (code == 806) {
            return TResponseVo.error(ResponseEnum.ERROR, "修改词条失败，该词条名已存在。");
        } else if (code == 807) {
            return TResponseVo.error(ResponseEnum.ERROR, "修改词条失败。");
        } else if (code == 851) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建评标办法失败");
        } else if (code == 852) {
            return TResponseVo.error(ResponseEnum.ERROR, "创建评标办法失败");
        } else if (code == 854) {
            return TResponseVo.error(ResponseEnum.ERROR, "修改评标办法失败");
        } else if (code == 871) {
            return TResponseVo.error(ResponseEnum.ERROR, "评标办法名称不能为空");
        } else if (code == 872) {
            return TResponseVo.error(ResponseEnum.ERROR, "必填参数为空");
        } else if (code == 873) {
            return TResponseVo.error(ResponseEnum.ERROR, "该办法名已存在,请重新命名");
        } else if (code == 881) {
            return TResponseVo.error(ResponseEnum.ERROR, "评标模块名称不能为空");
        } else if (code == 882) {
            return TResponseVo.error(ResponseEnum.ERROR, "必填参数为空");
        } else if (code == 883) {
            return TResponseVo.error(ResponseEnum.ERROR, "评标类型为'评分'时,权重不能为空");
        } else if (code == 884) {
            return TResponseVo.error(ResponseEnum.ERROR, "该模块名已存在,请重新命名");
        } else if (code == 890) {
            return TResponseVo.error(ResponseEnum.ERROR, "书签暂不支持复制。");
        } else if (code == 901) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签分组的名称重复。");
        } else if (code == 902) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签分组的名称重复");
        } else if (code == 903) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签的名称重复。");
        } else if (code == 904) {
            return TResponseVo.error(ResponseEnum.ERROR, "标签的名称重复");
        } else if (code == 905) {
            return TResponseVo.error(ResponseEnum.ERROR, "请检查标签内容是否包含特殊字符");
        } else if (code == 1000) {
            return TResponseVo.error(ResponseEnum.ERROR, "段落中存在同源参数,不可替换");
        } else if (code == 1001) {
            return TResponseVo.error(ResponseEnum.ERROR, "修改文件属性失败，请检查入参");
        } else if (code == 1002) {
            return TResponseVo.error(ResponseEnum.ERROR, "修改文件属性失败，本项目存在同名文件");
        } else if (code == 1101) {
            return TResponseVo.error(ResponseEnum.ERROR, "非段落【变更】，新增或删除段落无对比数据");
        } else if (code == 1102) {
            return TResponseVo.error(ResponseEnum.ERROR, "附属文件无对比数据");
        } else if (code == 2087) {
            return TResponseVo.error(ResponseEnum.ERROR, "xml数据有误");
        } else {
            throw new Exception();
        }
    }

    @ExceptionHandler(EditToolException.class)
    @ResponseBody
    public TResponseVo editToolHandle(EditToolException e) throws Exception {
        Integer code = e.getCode();
        String content = e.getContent();
        if (code != null) {
            if (code == 2001) {
                return TResponseVo.error(ResponseEnum.ERROR, "新增文件夹失败");
            } else if (code == 2002) {
                return TResponseVo.error(ResponseEnum.ERROR, "新增文件夹失败.");
            } else if (code == 2003) {
                return TResponseVo.error(ResponseEnum.ERROR, "当前路径下已存在同名文件夹");
            } else if (code == 2011) {
                return TResponseVo.error(ResponseEnum.ERROR, "该文件夹不为空,无法删除");
            } else if (code == 2013) {
                return TResponseVo.error(ResponseEnum.ERROR, "删除文件夹失败");
            } else if (code == 2014) {
                return TResponseVo.error(ResponseEnum.ERROR, "删除文件夹失败.");
            } else if (code == 2021) {
                return TResponseVo.error(ResponseEnum.ERROR, "重命名文件夹失败");
            } else if (code == 2022) {
                return TResponseVo.error(ResponseEnum.ERROR, "重命名文件夹失败.");
            } else if (code == 2032) {
                return TResponseVo.error(ResponseEnum.ERROR, "移动文件夹失败");
            } else if (code == 2033) {
                return TResponseVo.error(ResponseEnum.ERROR, "移动文件夹失败.");
            } else {
                throw new Exception();
            }
        } else if (StringUtils.isNotBlank(content)) {
            return TResponseVo.error(ResponseEnum.ERROR, content);
        } else {
            throw new Exception();
        }
    }

    @ExceptionHandler(WorkTableException.class)
    @ResponseBody
    public TResponseVo workTableHandle(WorkTableException e) throws Exception {
        e.printStackTrace();
        Integer code = e.getCode();
        String content = e.getContent();
        return TResponseVo.error(ResponseEnum.ERROR,content);
    }
}