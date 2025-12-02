package com.wht.sdt.enumeration;


import lombok.Getter;

@Getter
public enum StrategyType {

    CONSISTENT_HASH(1),
    HASH(2),
    TIME_BASED(3),
    CUSTOM_1(4),
    CUSTOM_2(5),
    CUSTOM_3(6),
    CUSTOM_4(7);

    private final Integer code;

    StrategyType(Integer code) {
        this.code = code;
    }

    public static StrategyType getInstance(Integer code) {
        for (StrategyType type : StrategyType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

}
