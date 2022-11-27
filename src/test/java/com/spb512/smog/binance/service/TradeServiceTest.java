package com.spb512.smog.binance.service;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TradeServiceTest {
    @Resource
    private TradeService tradeService;

    @Test
    void openPosition() {
        tradeService.openPosition();
    }

    @Test
    void closePosition() {
        tradeService.closePosition();
    }

    @Test
    void checkPosition() {
        tradeService.checkPosition();
    }
}