package com.microapp.autumn.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microapp.autumn.compiler.model.TargetAnnotation;
import com.microapp.autumn.compiler.model.TargetAnnotationAttribute;
import com.microapp.autumn.compiler.model.TargetClass;
import com.microapp.autumn.compiler.model.TargetMethod;
import com.microapp.autumn.compiler.model.TargetVariable;
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
                handleAnnotationClass(annotatedClass);
            }
        }
        return true;
    }

    private void handleAnnotationClass(Element annotatedClass) {
        log.info("begin handle annotation Export:{}", annotatedClass.getSimpleName().toString());
        // ==========================begin handle basic==========================
        String fullName = annotatedClass.asType().toString();
        TargetClass targetClass = new TargetClass();
        targetClass.setName(annotatedClass.getSimpleName().toString());
        targetClass.setFullName(fullName);
        targetClass.setPackageName(ClassNameUtil.getPackageName(fullName));
        // ==========================end handle basic==========================

        // ==========================end handle basic==========================
        List<String> interfaces = new ArrayList<>();
        targetClass.setImplementList(interfaces);
        if(annotatedClass instanceof TypeElement) {
            TypeElement typeElement = (TypeElement)annotatedClass;
            List<? extends TypeMirror> interfaceMirrors = typeElement.getInterfaces();
            interfaceMirrors.forEach(it -> {
                if(it instanceof DeclaredType) {
                    DeclaredType dt = (DeclaredType)it;
                    String interfaceName = dt.asElement().toString();
                    interfaces.add(interfaceName);
                }
            });
        }

        // ==========================begin handle annotation==========================
        List<TargetAnnotation> annotations = new ArrayList<>();
        targetClass.setAnnotations(annotations);
        List<? extends AnnotationMirror> mirrors = annotatedClass.getAnnotationMirrors();
        annotatedClass.getEnclosingElement();
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
        // ==========================end handle annotation==========================

        // ==========================begin handle field and method==========================
        List<TargetMethod> methods = new ArrayList<>();
        targetClass.setMethods(methods);
        List<? extends Element> elements = annotatedClass.getEnclosedElements();
        elements.forEach(it -> {
            if(it instanceof ExecutableElement) {

                TargetMethod method = new TargetMethod();
                ExecutableElement ee = (ExecutableElement)it;
                String name = ee.getSimpleName().toString();
                if(name.equals("<init>")) {
                    return;
                }
                method.setName(name);
                List<TargetVariable> variables = new ArrayList<>();
                method.setVariables(variables);
                List<? extends VariableElement> vars = ee.getParameters();
                vars.forEach(v -> {
                    TargetVariable variable = new TargetVariable();
                    String varName = v.getSimpleName().toString();
                    variable.setName(varName);
                    String typeName = v.asType().toString();
                    variable.setType(typeName);
                    variables.add(variable);
                });
                TypeMirror returnType = ee.getReturnType();
                method.setReturnType(returnType.toString());

                List<? extends TypeMirror> thrownTypes = ee.getThrownTypes();

                List<String> exceptions = thrownTypes.stream()
                        .map(TypeMirror::toString)
                        .collect(Collectors.toList());
                method.setExceptions(exceptions);
                methods.add(method);
            }
        });
        // ==========================end handle field and method==========================

        log.info("====================={}", targetClass);
    }

}
