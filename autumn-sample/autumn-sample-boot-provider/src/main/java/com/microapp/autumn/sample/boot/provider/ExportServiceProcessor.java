package com.microapp.autumn.sample.boot.provider;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SupportedAnnotationTypes(value = {"com.microapp.autumn.api.annotation.Export"})
@SupportedSourceVersion(value = SourceVersion.RELEASE_11)
public class ExportServiceProcessor extends AbstractProcessor {
    private Logger log = LoggerFactory.getLogger(ExportServiceProcessor.class);

    private Filer mFilerUtils;
    private Types mTypesUtils;
    private Elements mElementsUtils;
    private Messager mMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        System.out.println("######################################################");
        super.init(processingEnvironment);
        mFilerUtils = processingEnv.getFiler();
        mTypesUtils = processingEnv.getTypeUtils();
        mElementsUtils = processingEnv.getElementUtils();
        mMessager = processingEnv.getMessager();
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        System.out.println("######################################################");

        log.info("begin handle export annotation process");
        for (TypeElement annotationElement: annotations) {
            Set<? extends Element> annotatedClasses = roundEnv.getElementsAnnotatedWith(annotationElement);
            for(Element annotatedClass: annotatedClasses) {
                handleAnnotationClass(annotationElement, annotatedClass);
            }
        }
        return true;
    }

    private void handleAnnotationClass(TypeElement annotationElement, Element annotatedClass) {
        log.info("begin handle Export:{}", annotatedClass);
        if(!annotationElement.toString().equals("autumn.core.annotation.Export")) {
            return;
        }
        TypeElement te = (TypeElement) annotatedClass;
        List<? extends TypeMirror> interfaces = te.getInterfaces();
        //MetaHolder.addExportService(annotatedClass.toString(), interfaces);
    }


}
