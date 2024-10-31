package com.microapp.core.config;

import com.microapp.core.extension.AttachableProcessor;
import lombok.Data;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/8
 */
@Data
public class ServiceConfig<T> {
    private Class<?> interfaceClass;
    private transient AttachableProcessor ref;
}
