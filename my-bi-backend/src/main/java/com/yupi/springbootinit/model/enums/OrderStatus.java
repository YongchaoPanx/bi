package com.yupi.springbootinit.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum OrderStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    @EnumValue
    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
