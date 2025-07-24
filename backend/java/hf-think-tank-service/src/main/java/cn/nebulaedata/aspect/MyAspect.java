package cn.nebulaedata.aspect;

/**
 * @author 徐衍旭
 * @date 2023/11/27 14:05
 * @note
 */
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

//@Aspect
//@Component
public class MyAspect {

//    @After("execution(* com.example.myapp.controller.*.*(..)) && target(myTarget)")
//    public void afterAdvice(JoinPoint joinPoint, MyTarget myTarget) {
//        // 在这里编写需要在目标方法执行前运行的代码
//        System.out.println("Before executing method: " + joinPoint.getSignature().getName());
//        // 额外执行的方法
//        myTarget.extraMethod();
//    }
}