package com.spb512.smog.binance.talib;

/**
 * 鳍strat实体
 *
 * @author spb512
 * @date 2022/09/22
 */
public class FinStratEntity {
    private int retCode;
    private double[] rsiReal;

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public double[] getRsiReal() {
        return rsiReal;
    }

    public void setRsiReal(double[] rsiReal) {
        this.rsiReal = rsiReal;
    }
}
