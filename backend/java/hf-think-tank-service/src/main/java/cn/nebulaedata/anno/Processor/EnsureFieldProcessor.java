package cn.nebulaedata.anno.Processor;

import cn.nebulaedata.anno.EnsureField;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 徐衍旭
 * @date 2023/11/28 16:47
 * @note
 */
public class EnsureFieldProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(EnsureField.class)) {
            EnsureField ensureField = element.getAnnotation(EnsureField.class);
            String fieldName = ensureField.value();
            System.out.println(fieldName);
            boolean fieldExists = element.getEnclosedElements().stream()
                    .anyMatch(e -> e.getSimpleName().toString().equals(fieldName));

            if (!fieldExists) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Field '" + fieldName + "' not found in " + element.getSimpleName());
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(){{addAll(Collections.singleton(EnsureField.class.getName()));}};
//        return Set.of(EnsureField.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
