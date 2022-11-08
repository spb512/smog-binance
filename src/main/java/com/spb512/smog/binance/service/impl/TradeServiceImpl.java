package com.spb512.smog.binance.service.impl;

import com.binance.client.SyncRequestClient;
import com.binance.client.exception.BinanceApiException;
import com.binance.client.model.ResponseResult;
import com.binance.client.model.enums.*;
import com.binance.client.model.market.Candlestick;
import com.binance.client.model.market.ExchangeInfoEntry;
import com.binance.client.model.market.ExchangeInformation;
import com.binance.client.model.trade.AccountBalance;
import com.binance.client.model.trade.Leverage;
import com.binance.client.model.trade.Order;
import com.binance.client.model.trade.PositionRisk;
import com.spb512.smog.binance.client.GetClient;
import com.spb512.smog.binance.dto.IndicatorDto;
import com.spb512.smog.binance.service.TradeService;
import com.spb512.smog.binance.talib.FinStratEntity;
import com.spb512.smog.binance.talib.FinStratModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 交易服务impl
 *
 * @author spb512
 * @date 2022/09/21
 */
@Service
public class TradeServiceImpl implements TradeService {
    Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 时间间隔
     */
    CandlestickInterval interval = CandlestickInterval.THREE_MINUTES;
    /**
     * 限制
     */
    Integer limit = 250;
    /**
     * 持仓模式："true": 双向持仓模式；"false": 单向持仓模式
     */
    String dual = "false";
    @Resource
    private GetClient getClient;
    @Resource
    private FinStratModel finModel;
    private SyncRequestClient publicClient;
    private SyncRequestClient privateClient;
    /**
     * 交易对
     */
    private String symbolNam = "BTCUSDT";
    /**
     * 杠杆倍数
     */
    private Integer leverage = 10;
    /**
     * 保证金模式：逐仓或全仓
     */
    private MarginType marginType = MarginType.ISOLATED;
    /**
     * 收盘价数组
     */
    private double[] dClose = new double[limit];
    /**
     * rsi12做空激活点
     */
    private double activateHighRsi12 = 83;
    /**
     * rsi12做多激活点
     */
    private double activateLowRsi12 = 17;
//    /**
//     * 最高做空点
//     */
//    private double highestHighRsi = 0;
//    /**
//     * 最低做空点
//     */
//    private double lowestLowRsi = 100;
//    /**
//     * 回调幅度
//     */
//    private double pullbackRsi = 0.01;
    /**
     * 做多
     */
    private boolean doBuy = false;
    /**
     * 做空
     */
    private boolean doSell = false;
    /**
     * 是否有持仓
     */
    private boolean isPosition = false;
    /**
     * 最小开仓资金
     */
    private BigDecimal minStartup = BigDecimal.valueOf(10);
    /**
     * 可用余额
     */
    private BigDecimal availableBalance;
    /**
     * 最大数量
     */
    private String maxQty;
    /**
     * 最高盈利率
     */
    private BigDecimal highestUplRatio = BigDecimal.ZERO;
    /**
     * 收益率激活
     */
    private final double activateRatio = 0.0512;
    /**
     * 回调收益率
     */
    private final double pullbackRatio = 0.001;
    /**
     * 强制止损线
     */
    private final double stopLossLine = -0.05;
    /**
     * 跳过num
     */
    private int skipNum = 0;

    @NotNull
    private static BigDecimal getUplRatio(PositionRisk positionRisk) {
        BigDecimal unrealizedProfit = positionRisk.getUnrealizedProfit();
        BigDecimal isolatedMargin = new BigDecimal(positionRisk.getIsolatedMargin());
        BigDecimal isolated = isolatedMargin.subtract(unrealizedProfit);
        BigDecimal uplRatio = unrealizedProfit.divide(isolated, 16, RoundingMode.HALF_UP);
        return uplRatio;
    }

    @PostConstruct
    public void init() {
        publicClient = getClient.getPublicClient();
        privateClient = getClient.getPrivateClient();
        //初始化持仓模式
        try {
            ResponseResult result = privateClient.changePositionSide(dual);
            if (result.getCode() == 200) {
                logger.info("初始化持仓模式为单向持仓成功");
            }
        } catch (BinanceApiException e) {
            logger.info(e.getMessage());
        }
        //初始化倍数
        Leverage leverageResult = privateClient.changeInitialLeverage(symbolNam, leverage);
        logger.info("初始化杠杆倍数为{}成功", leverageResult.getLeverage());
        //初始化保证金模式
        try {
            ResponseResult marginTypeResult = privateClient.changeMarginType(symbolNam, marginType);
            if (marginTypeResult.getCode() == 200) {
                logger.info("初始化保证金模式为逐仓成功");
            }
        } catch (BinanceApiException e) {
            logger.info(e.getMessage());
        }
        //初始化市价交易单次最大买和卖的数量
        ExchangeInformation exchangeInformation = publicClient.getExchangeInformation();
        List<ExchangeInfoEntry> symbols = exchangeInformation.getSymbols();
        for (ExchangeInfoEntry exchangeInfoEntry : symbols) {
            if (symbolNam.equals(exchangeInfoEntry.getSymbol())) {
                List<List<Map<String, String>>> filters = exchangeInfoEntry.getFilters();
                List<Map<String, String>> mapList = filters.get(2);
                maxQty = mapList.get(2).get("maxQty");
                logger.info("初始化市价交易单次最大买和卖的数量为{}成功", maxQty);
            }
        }
        //初始化持仓标记
        List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
        if (positionRiskList.get(0).getPositionAmt().doubleValue() > 0) {
            isPosition = true;
            logger.info("当前有持仓，初始化持仓标记isPosition为true成功");
        }
    }

    @Override
    public synchronized void openPosition() {
        // 当前是否有持仓
        if (isPosition) {
            return;
        }
        //是否跳过
        if(skipNum > 0){
            skipNum--;
            return;
        }
        //查询k线数据
        List<Candlestick> candlestickList = publicClient.getMarkPriceCandlesticks(symbolNam, interval, null, null, limit);
        IndicatorDto indicatorDto = getIndicators(candlestickList);
        double rsi12 = indicatorDto.getRsi12();
//        if ((rsi12 > activateHighRsi12) && (rsi12 > highestHighRsi)) {
//            highestHighRsi = rsi12;
//            logger.info("highestHighRsi更新，当前为:{}", highestHighRsi);
//        }
        if (rsi12 > activateHighRsi12) {
            doSell = true;
            doBuy = false;
        }

//        if ((rsi12 < activateLowRsi12) && (rsi12 < lowestLowRsi)) {
//            lowestLowRsi = rsi12;
//            logger.info("lowestLowRsi更新，当前为:{}", lowestLowRsi);
//        }
        if (rsi12 < activateLowRsi12) {
            doBuy = true;
            doSell = false;
        }
        if (doBuy || doSell) {
            //再次确认是否有持仓
            List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
            if (positionRiskList.get(0).getPositionAmt().doubleValue() > 0) {
                return;
            }
            //查询更新余额变量
            getBalance();
            if (availableBalance.compareTo(minStartup) < 0) {
                doBuy = false;
                doSell = false;
                logger.info("账号余额:{},余额过低小于{}", availableBalance, minStartup);
            }
            //计算开仓数量
            BigDecimal currentPrice = candlestickList.get(candlestickList.size() - 1).getClose();
            BigDecimal quantity = availableBalance.divide(currentPrice, 3, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(leverage)).multiply(BigDecimal.valueOf(0.5));
            logger.info("当前价格:{};下单数量{};当前rsi12指标:{}", currentPrice, quantity, rsi12);
            //下单
            OrderSide sid = OrderSide.BUY;
            String direction = "做多";
            if (doSell) {
                sid = OrderSide.SELL;
                direction = "做空";
            }
            Order order = privateClient.postOrder(symbolNam, sid, null, OrderType.MARKET, null, String.valueOf(quantity), null, null, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
            if (order.getOrderId() != null) {
                isPosition = true;
//                lowestLowRsi = 100;
//                highestHighRsi = 0;
                doBuy = false;
                doSell = false;
            }
            logger.info("开{}仓成功,订单号ordId:{};当前余额:{}", direction, order.getOrderId(), availableBalance);
        }
    }

    @Override
    public synchronized void closePosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
        if (positionRiskList.get(0).getPositionAmt().doubleValue() == 0) {
            return;
        }
        PositionRisk positionRisk = positionRiskList.get(0);
        BigDecimal uplRatio = getUplRatio(positionRisk);
        if ((uplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio) > 0)) {
            highestUplRatio = uplRatio;
            logger.info("highestUplRatio更新，当前为:{}", highestUplRatio);
        }
        if ((highestUplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio.subtract(BigDecimal.valueOf(pullbackRatio))) < 1)) {
            sell(positionRisk, uplRatio);
        }
    }

    @Override
    public synchronized void checkPosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
        if (positionRiskList.get(0).getPositionAmt().doubleValue() == 0) {
            return;
        }
        PositionRisk positionRisk = positionRiskList.get(0);
        BigDecimal uplRatio = getUplRatio(positionRisk);
        if (uplRatio.compareTo(BigDecimal.valueOf(stopLossLine)) < 0) {
            logger.info("达到强制止损线{}%", stopLossLine * 100);
            sell(positionRisk, uplRatio);
            //暂停
            skipNum = 300;
            logger.info("暂停{}秒", skipNum);
        }
    }

    private void sell(@NotNull PositionRisk positionRisk, BigDecimal uplRatio) {
        BigDecimal positionAmt = positionRisk.getPositionAmt();
        OrderSide side;
        String direction;
        if (positionAmt.doubleValue() > 0) {
            side = OrderSide.SELL;
            direction = "做多";
        } else {
            side = OrderSide.BUY;
            direction = "做空";
        }
        Order order = privateClient.postOrder(symbolNam, side, null, OrderType.MARKET, null, positionAmt.abs().toString(), null, null, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
        highestUplRatio = BigDecimal.ZERO;
        if (order.getOrderId() != null) {
            isPosition = false;
            //查询更新余额变量
            getBalance();
            logger.info("开仓均价:{};当前价格:{};当前收益率:{}", positionRisk.getEntryPrice(), positionRisk.getMarkPrice(), uplRatio);
            logger.info("平{}仓成功,订单号ordId:{};当前余额:{}", direction, order.getOrderId(), availableBalance);
            //暂停
            skipNum = 300;
            logger.info("暂停{}秒<=====================分隔符=======================>", skipNum);
        }
    }

    @NotNull
    private IndicatorDto getIndicators(List<Candlestick> candlestickList) {
        for (int i = 0; i < candlestickList.size(); i++) {
            dClose[i] = candlestickList.get(i).getClose().doubleValue();
        }
        FinStratEntity rsi12FinEntity = finModel.calRsi(dClose, 12);
        double[] dRsi12 = rsi12FinEntity.getRsiReal();
        IndicatorDto indicatorDto = new IndicatorDto();
        indicatorDto.setRsi12(dRsi12[dRsi12.length - 1]);
        return indicatorDto;
    }

    private void getBalance() {
        List<AccountBalance> balanceList = privateClient.getBalance();
        for (AccountBalance accountBalance : balanceList) {
            if ("USDT".equals(accountBalance.getAsset())) {
                availableBalance = accountBalance.getAvailableBalance();
                return;
            }
        }
    }
}
