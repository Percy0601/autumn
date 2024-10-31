package com.microapp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: baoxin.zhao
 * @date: 2024/10/15
 */
@AllArgsConstructor
@Getter
public enum MulticastEventEnum {
    REGISTRY("registry", "multicast registry event"),
    SHUTDOWN("shutdown", "multicast shutdown event"),
    ;
    private String code;
    private String desc;

    public static MulticastEventEnum getByCode(String code) {
        for (MulticastEventEnum e: MulticastEventEnum.values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
