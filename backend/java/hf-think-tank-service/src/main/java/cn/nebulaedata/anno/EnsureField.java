package cn.nebulaedata.anno;

/**
 * @author 徐衍旭
 * @date 2023/11/28 16:46
 * @note
 */
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE) // 类或接口
@Retention(RetentionPolicy.SOURCE) // 只在源码级别保留，编译时不包含在类文件中
public @interface EnsureField {
    String value(); // 要检查的字段名称
}