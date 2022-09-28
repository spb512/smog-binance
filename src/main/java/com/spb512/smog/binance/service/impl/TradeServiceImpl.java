package com.spb512.smog.binance.service.impl;

import com.alibaba.fastjson2.JSONObject;
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
     * 时间间隔
     */
    CandlestickInterval interval = CandlestickInterval.THREE_MINUTES;
    /**
     * 限制
     */
    Integer limit = 250;
    /**
     * 收盘价数组
     */
    private double[] dClose = new double[limit];
    /**
     * rsi12做空激活点
     */
    private double activateHighRsi12 = 80;
    /**
     * rsi12做多激活点
     */
    private double activateLowRsi12 = 20;
    /**
     * 最高做空点
     */
    private double highestHighRsi = 0;
    /**
     * 最低做空点
     */
    private double lowestLowRsi = 100;
    /**
     * 回调幅度
     */
    private double pullbackRsi = 0.01;
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
    private double activateRatio = 0.05;
    /**
     * 回调收益率
     */
    private double pullbackRatio = 0.001;
    /**
     * 强制止损线
     */
    private double stopLossLine = -0.10;
    /**
     * 持仓模式："true": 双向持仓模式；"false": 单向持仓模式
     */
    String dual = "false";
    @PostConstruct
    public void init(){
        publicClient = getClient.getPublicClient();
        privateClient = getClient.getPrivateClient();
        //初始化持仓模式
        try {
            ResponseResult result = privateClient.changePositionSide(dual);
            if (result.getCode() == 200){
                logger.info("初始化持仓模式为{}成功", dual);
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
            if (marginTypeResult.getCode() == 200){
                logger.info("初始化保证金模式为{}成功", marginType);
            }
        } catch (BinanceApiException e) {
            logger.info(e.getMessage());
        }
        //初始化市价交易单次最大买和卖的数量
        ExchangeInformation exchangeInformation = publicClient.getExchangeInformation();
        List<ExchangeInfoEntry> symbols = exchangeInformation.getSymbols();
        for(ExchangeInfoEntry exchangeInfoEntry : symbols){
            if(symbolNam.equals(exchangeInfoEntry.getSymbol())){
                List<List<Map<String, String>>> filters = exchangeInfoEntry.getFilters();
                List<Map<String, String>> mapList = filters.get(2);
                maxQty = mapList.get(2).get("maxQty");
                logger.info("最大数量maxQty：" + maxQty);
            }
        }
    }

    @Override
    public synchronized void openPosition() {
        // 当前是否有持仓
        if(isPosition){
            return;
        }
        //查询k线数据
        List<Candlestick> candlestickList = publicClient.getMarkPriceCandlesticks(symbolNam, interval, null, null, limit);
        IndicatorDto indicatorDto = getIndicators(candlestickList);
        double rsi12 = indicatorDto.getRsi12();
//        logger.info("currentPrice:{}", candlestickList.get(candlestickList.size() - 1).getClose());
//        logger.info("rsi12:{},highestHighRsi:{},lowestLowRsi{}", rsi12,highestHighRsi,lowestLowRsi);
        if ((rsi12 > activateHighRsi12) && (rsi12 > highestHighRsi)) {
            highestHighRsi = rsi12;
            logger.info("highestHighRsi更新，当前为:{}", highestHighRsi);
        }
//        logger.info("(highestHighRsi > activateHighRsi12):{} && (highestHighRsi - rsi12 > pullbackRsi){},总的：{}",highestHighRsi > activateHighRsi12,(highestHighRsi - rsi12 > pullbackRsi),(highestHighRsi > activateHighRsi12) && (highestHighRsi - rsi12 > pullbackRsi));
        if ((highestHighRsi > activateHighRsi12) && (highestHighRsi - rsi12 > pullbackRsi)) {
            doSell = true;
        }

        if ((rsi12 < activateLowRsi12) && (rsi12 < lowestLowRsi)) {
            lowestLowRsi = rsi12;
            logger.info("lowestLowRsi更新，当前为:{}", lowestLowRsi);
        }
        if ((lowestLowRsi < activateLowRsi12) && (rsi12 - lowestLowRsi > pullbackRsi)) {
            doBuy = true;
        }
        if (doBuy || doSell) {
//        if (false) {
            //再次确认是否有持仓
            List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
            if(positionRiskList.get(0).getPositionAmt().doubleValue() > 0){
                return;
            }
            //查询余额
            List<AccountBalance> balanceList = privateClient.getBalance();
            for (AccountBalance accountBalance : balanceList) {
                if("USDT".equals(accountBalance.getAsset())){
                    availableBalance = accountBalance.getAvailableBalance();
                    if (availableBalance.compareTo(minStartup) < 0){
                        doBuy = false;
                        doSell = false;
                        logger.info("账号余额:{},余额过低小于{}", availableBalance, minStartup);
                        return;
                    }
                }
            }
            //计算开仓数量
            BigDecimal currentPrice = candlestickList.get( candlestickList.size() - 1).getClose();
            logger.info("当前余额:{}",availableBalance);
            logger.info("当前价格:{}",currentPrice);
            BigDecimal quantity = availableBalance.divide(currentPrice, 3, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(leverage)).multiply(BigDecimal.valueOf(0.8));
            logger.info("下单数量{}", quantity);
            //下单
            OrderSide sid = OrderSide.BUY;
            String direction = "做多";
            if (doSell){
                sid =OrderSide.SELL;
                direction = "做空";
            }
            Order order = privateClient.postOrder(symbolNam, sid, null, OrderType.MARKET, null, String.valueOf(quantity), null, null, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
            if (order.getOrderId() != null){
                isPosition = true;
                lowestLowRsi = 100;
                highestHighRsi = 0;
                doBuy = false;
                doSell = false;
            }
            logger.info("开{}仓,订单号ordId:{}=======>当前余额:{}", direction, order.getOrderId(), availableBalance);
        }
    }

    @NotNull
    private IndicatorDto getIndicators(List<Candlestick> candlestickList) {
        for (int i = 0; i < candlestickList.size(); i++){
            dClose[i] = candlestickList.get(i).getClose().doubleValue();
        }
        FinStratEntity rsi12FinEntity = finModel.calRsi(dClose, 12);
        double[] dRsi12 = rsi12FinEntity.getRsiReal();
        IndicatorDto indicatorDto = new IndicatorDto();
        indicatorDto.setRsi12(dRsi12[dRsi12.length - 1]);
        return indicatorDto;
    }

    @Override
    public synchronized void  closePosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
        if(positionRiskList.get(0).getPositionAmt().doubleValue() == 0){
            return;
        }
        //查询k线数据
        List<Candlestick> candlestickList = publicClient.getMarkPriceCandlesticks(symbolNam, interval, null, null, limit);
        IndicatorDto indicatorDto = getIndicators(candlestickList);
        double rsi12 = indicatorDto.getRsi12();
        PositionRisk positionRisk = positionRiskList.get(0);
        BigDecimal unrealizedProfit = positionRisk.getUnrealizedProfit();
        BigDecimal isolatedMargin = new BigDecimal(positionRisk.getIsolatedMargin());
        BigDecimal isolated = isolatedMargin.subtract(unrealizedProfit);
        BigDecimal uplRatio = unrealizedProfit.divide(isolated, 2, RoundingMode.HALF_UP);
//        logger.info("UnrealizedProfit:{};uplRatio:{}",positionRisk.getUnrealizedProfit(), uplRatio);
        if ((uplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio) > 0)) {
            highestUplRatio = uplRatio;
            logger.info("highestUplRatio更新，当前为:{};rsi12指数为:{}", highestUplRatio, rsi12);
        }
        if ((highestUplRatio.compareTo(BigDecimal.valueOf(activateRatio)) > -1) && (uplRatio.compareTo(highestUplRatio.subtract(BigDecimal.valueOf(pullbackRatio))) < 1)) {
            Order order = sell(positionRisk);
            logger.info("平仓成功,订单号ordId:{}=======>当前余额:{}",order.getOrderId(), availableBalance);
            logger.info("平仓收益率为:{};rsi12指数为:{}", uplRatio, rsi12);
        }
    }

    @Override
    public synchronized void checkPosition() {
        // 当前是否有持仓
        if (!isPosition) {
            return;
        }
        List<PositionRisk> positionRiskList = privateClient.getPositionRisk(symbolNam);
        if(positionRiskList.get(0).getPositionAmt().doubleValue() == 0){
            return;
        }
        PositionRisk positionRisk = positionRiskList.get(0);
        BigDecimal unrealizedProfit = positionRisk.getUnrealizedProfit();
        BigDecimal isolatedMargin = new BigDecimal(positionRisk.getIsolatedMargin());
        BigDecimal isolated = isolatedMargin.subtract(unrealizedProfit);
        BigDecimal uplRatio = unrealizedProfit.divide(isolated, 2, RoundingMode.HALF_UP);
        if (uplRatio.compareTo(BigDecimal.valueOf(stopLossLine)) < 0) {
            logger.info("达到强制止损线{}%", stopLossLine * 100);
            Order order = sell(positionRisk);
            logger.info("当前收益率{}", uplRatio);
            logger.info("平仓成功,订单号ordId:{}=======>当前余额:{}",order.getOrderId(), availableBalance);
        }
    }

    private Order sell(PositionRisk positionRisk) {
        BigDecimal positionAmt = positionRisk.getPositionAmt();
        OrderSide side;
        if (positionAmt.doubleValue() > 0){
            side = OrderSide.SELL;
        } else {
            side = OrderSide.BUY;
        }
        Order order = privateClient.postOrder(symbolNam, side, null, OrderType.MARKET, null, positionAmt.abs().toString(), null, null, null, null, null, null, null, null, null, NewOrderRespType.RESULT);
        highestUplRatio = BigDecimal.ZERO;
        if (order.getOrderId() != null){
            isPosition = false;
        }
        return order;
    }
}
