package com.spb512.smog.binance.service;

/**
 * 交易服务
 *
 * @author spb512
 * @date 2022/09/21
 */
public interface TradeService {
    /**
     * 开仓
     */
    void openPosition();

    /**
     * 平仓
     */
    void closePosition();

    /**
     * 检查持仓
     */
    void checkPosition();
}
