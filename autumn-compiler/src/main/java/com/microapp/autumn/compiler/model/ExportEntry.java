package com.microapp.autumn.compiler.model;

import java.lang.reflect.Method;
import java.util.List;

import lombok.Data;

@Data
public class ExportEntry {
    private String interfaceName;
    private String beanName;
    private List<Method> methods;
}
