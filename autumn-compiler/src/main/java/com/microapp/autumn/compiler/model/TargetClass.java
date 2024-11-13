package com.microapp.autumn.compiler.model;

import java.util.List;

import javax.lang.model.element.AnnotationMirror;

import lombok.Data;

/**
 * @author: baoxin.zhao
 * @date: 2024/11/13
 */
@Data
public class TargetClass {
    private String name;
    private String fullName;
    private String packageName;
    private List<TargetAnnotation> annotations;

}
