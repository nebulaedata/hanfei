package cn.nebulaedata.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author 徐衍旭
 * @date 2022/5/23 17:50
 * @note
 */
@Target(value= ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface NotNull {
    String message() default "";
}
