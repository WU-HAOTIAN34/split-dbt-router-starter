package com.wht.sdt.context;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StrategyContext {

    private int tbCount;

    private int dbCount;

    private String keyValue;

}
