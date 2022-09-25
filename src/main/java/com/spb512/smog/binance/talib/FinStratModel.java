package com.spb512.smog.binance.talib;

import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.springframework.stereotype.Component;

/**
 * 翅片参数模型
 *
 * @author spb512
 * @date 2022/09/22
 */
@Component
public class FinStratModel {
    /**
     * Talib核心
     */
    private Core finLib = new Core();

    /**
     * @param inClose      收盘价
     * @param inTimePeriod 时间周期
     * @return RSI计算结果数据
     */
    public FinStratEntity calRsi(double[] inClose, int inTimePeriod) {

        // 指标计算结果
        FinStratEntity fiResult = new FinStratEntity();

        int startIdx = 0;
        int endIdx = inClose.length - 1;
        double[] sriReal = new double[inClose.length - inTimePeriod];

        RetCode retCode = this.finLib.rsi(startIdx, endIdx, inClose, inTimePeriod, new MInteger(), new MInteger(),
                sriReal);
        if (retCode == RetCode.Success) {
            fiResult.setRetCode(0);
            fiResult.setRsiReal(sriReal);
        }
        return fiResult;
    }

}
