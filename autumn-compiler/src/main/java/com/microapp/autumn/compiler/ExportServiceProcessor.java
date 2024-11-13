package com.microapp.autumn.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microapp.autumn.compiler.model.TargetAnnotation;
import com.microapp.autumn.compiler.model.TargetAnnotationAttribute;
import com.microapp.autumn.compiler.model.TargetClass;
import com.microapp.autumn.compiler.util.ClassNameUtil;

@SupportedAnnotationTypes(value = {"com.microapp.autumn.api.annotation.Export"})
@SupportedSourceVersion(value = SourceVersion.RELEASE_17)
public class ExportServiceProcessor extends AbstractProcessor {
    private Logger log = LoggerFactory.getLogger(ExportServiceProcessor.class);

    private Filer mFilerUtils;
    private Types mTypesUtils;
    private Elements mElementsUtils;
    private Messager mMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        log.info("begin handle export annotation process init.");
        super.init(processingEnvironment);
        mFilerUtils = processingEnv.getFiler();
        mTypesUtils = processingEnv.getTypeUtils();
        mElementsUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotationElement: annotations) {
            Set<? extends Element> annotatedClasses = roundEnv.getElementsAnnotatedWith(annotationElement);
            for(Element annotatedClass: annotatedClasses) {
                handleAnnotationClass(annotationElement, annotatedClass);
            }
        }
        return true;
    }

    private void handleAnnotationClass(TypeElement annotationElement, Element annotatedClass) {
        log.info("begin handle annotation Export:{}", annotatedClass.getSimpleName().toString());
        String fullName = annotatedClass.asType().toString();
        TargetClass targetClass = new TargetClass();
        targetClass.setName(annotatedClass.getSimpleName().toString());
        targetClass.setFullName(fullName);
        targetClass.setPackageName(ClassNameUtil.getPackageName(fullName));

        List<TargetAnnotation> annotations = new ArrayList<>();
        targetClass.setAnnotations(annotations);

        List<? extends AnnotationMirror> mirrors = annotatedClass.getAnnotationMirrors();
        for(AnnotationMirror mirror: mirrors) {
            TargetAnnotation annotation = new TargetAnnotation();
            annotation.setType(mirror.getAnnotationType().toString());
            annotations.add(annotation);
            List<TargetAnnotationAttribute> attributes = new ArrayList<>();
            annotation.setAttributes(attributes);
            //log.info("===========:type:{}, value:{}", mirror.getAnnotationType(), mirror.getElementValues());

            Map<? extends ExecutableElement, ? extends AnnotationValue> mapping = mirror.getElementValues();
            mapping.forEach((k, v) -> {
                TargetAnnotationAttribute attribute = new TargetAnnotationAttribute();
                String defaultValue = k.getDefaultValue().toString();
                String name = k.getSimpleName().toString();
                attribute.setName(name);
                attribute.setDefaultValue(defaultValue);
                String value = v.toString();
                attribute.setValue(value);
                attribute.setReturnType(k.getReturnType().toString());
                attributes.add(attribute);
            });
        }

        annotatedClass.getEnclosedElements();
        log.info("====================={}", targetClass);
    }





}
