package com.spb512.smog.binance.task;

import com.binance.client.exception.BinanceApiException;
import com.spb512.smog.binance.service.TradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 交易定时任务
 *
 * @author spb512
 * @date 2022/09/21
 */
@Component
@EnableScheduling
@EnableAsync
@Async
public class TradeTimedTask {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private TradeService tradeService;
    /**
     * 开仓任务,每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void openPositionTask() {
        tradeService.openPosition();
        try {
        } catch (BinanceApiException e) {
            logger.info("开仓任务捕获api异常:{}", e.getMessage());
        }
    }

    /**
     * 平仓任务,每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void closePositionTask() {
        tradeService.closePosition();
        try {
        } catch (BinanceApiException e) {
            logger.info("平仓任务捕获api异常:{}", e.getMessage());
        }
    }

    /**
     * 检查持仓任务,每1秒执行一次
     */
    @Scheduled(cron = "0/1 * * * * ?")
    public void checkPositionTask() {
        tradeService.checkPosition();
        try {
        } catch (BinanceApiException e) {
            logger.info("止损任务捕获api异常:{}", e.getMessage());
        }
    }
}
