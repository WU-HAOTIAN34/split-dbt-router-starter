package com.wht.sdt.strategy;


import com.wht.sdt.enumeration.StrategyType;
import com.wht.sdt.strategy.impl.RouterStrategyConsistentHash;
import com.wht.sdt.strategy.impl.RouterStrategyHashCode;
import com.wht.sdt.strategy.impl.RouterStrategyTimeBased;

public class RouterStrategyFactory {


    public static RouterStrategy getInstance(StrategyType strategyType) {
        switch (strategyType) {
            case HASH:
                return new RouterStrategyHashCode();
            case CONSISTENT_HASH:
                return new RouterStrategyConsistentHash();
            case TIME_BASED:
                return new RouterStrategyTimeBased();
            default:
                return new RouterStrategyHashCode();
        }
    }

}
