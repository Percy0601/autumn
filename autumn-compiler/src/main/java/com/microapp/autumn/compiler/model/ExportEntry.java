package com.microapp.autumn.compiler.model;

import java.util.List;

import lombok.Data;

@Data
public class ExportEntry {
    private String name;
    private String implementName;
    private String proxyTypeName;
    private String proxyBeanName;
    private List<TargetMethod> methods;

}
