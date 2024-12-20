package com.microapp.autumn.compiler;

import com.microapp.autumn.api.annotation.Reference;
import com.microapp.autumn.compiler.model.MethodElement;
import com.microapp.autumn.compiler.model.ReferenceEntry;
import com.microapp.autumn.compiler.model.TargetClass;
import com.microapp.autumn.compiler.util.ClassNameUtil;
import com.microapp.autumn.compiler.util.ClassResolverUtil;
import com.microapp.autumn.compiler.util.FreemarkerUtil;
import com.microapp.autumn.compiler.util.MetaHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@SupportedAnnotationTypes(value = {"com.microapp.autumn.api.annotation.Reference"})
@SupportedSourceVersion(value = SourceVersion.RELEASE_17)
public class ReferenceServiceProcessor extends AbstractProcessor {
    private Logger log = LoggerFactory.getLogger(ReferenceServiceProcessor.class);
    private Map<String, ReferenceEntry> mapping = new ConcurrentHashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        log.info("begin handle reference annotation.");
        for (TypeElement annotationElement: annotations) {
            Set<? extends Element> annotatedClasses = roundEnv.getElementsAnnotatedWith(annotationElement);
            for(Element annotatedClass: annotatedClasses) {
                TargetClass targetClass = ClassResolverUtil.handleReference(annotatedClass);
            }
        }


        return true;
    }

    private void handleWrite() {
        ConcurrentHashMap<String, ReferenceEntry> referenceEntries =  MetaHolder.getReferService();
        referenceEntries.forEach((k, v) -> {
            try {
                String sourceName = "";
                int lastDot = k.lastIndexOf('.');
                if (lastDot > 0) {
                    sourceName = k.substring(0, lastDot);
                }
                String packageName = ClassNameUtil.getPackageName(sourceName);
                String simpleClassName = ClassNameUtil.getSimpleClassName(sourceName);
                JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(packageName + "._" + simpleClassName + "Proxy");

//                FreemarkerUtil.handleProxy(k, v, builderFile.openWriter());
                JavaFileObject builderFactoryFile = processingEnv.getFiler().createSourceFile(packageName + "._" + simpleClassName + "PoolFactory");
//                FreemarkerUtil.handlePoolFactory(k, v, builderFactoryFile.openWriter());
            } catch (Exception e) {
                log.warn("handleWrite Proxy Exception:", e);
            }
        });
    }

    private void handleAnnotationClass(TypeElement annotationElement, Element annotatedClass) {
        if(!annotationElement.toString().equals("autumn.core.annotation.Reference")) {
            return;
        }
        Reference reference = annotatedClass.getAnnotation(Reference.class);
        ExecutableElement executableElement = (ExecutableElement) annotatedClass;

        DeclaredType interfaceClass = (DeclaredType)executableElement.getReturnType();
        List<? extends Element> elements = interfaceClass.asElement().getEnclosedElements();
        if(elements.isEmpty()) {
            return;
        }
        List<MethodElement> methodElements = new ArrayList<>();
        for(Element ele: elements) {
            MethodElement method = new MethodElement();
            ExecutableElement ee = (ExecutableElement)ele;
            method.setName(ee.getSimpleName().toString());
            TypeMirror returnType = ee.getReturnType();
            method.setReturnType(returnType.toString());
            methodElements.add(method);
            List<? extends VariableElement> ves = ee.getParameters();
            if(ves.isEmpty()) {
                continue;
            }
            List<String> params = new ArrayList<>();
            for(VariableElement ve: ves) {
                params.add(ve.asType().toString());
            }
            method.setParamTypes(params);
        }

        ReferenceEntry entry = new ReferenceEntry();
        entry.setInterfaceName(interfaceClass.toString());
        entry.setMethodElements(methodElements);
//        log.info("entry:{}", JSON.toJSONString(entry));
        MetaHolder.addReferService(interfaceClass.toString(), entry);
    }


}
