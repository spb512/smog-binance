package com.spb512.smog.binance.client;

import com.binance.client.RequestOptions;
import com.binance.client.SyncRequestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取客户端
 *
 * @author spb512
 * @date 2022/09/21
 */
@Component
public class GetClient {
    @Value("${binance.api.key:}")
    private String apiKey;
    @Value("${binance.secret.key:}")
    private String secretKey;
    @Value("${binance.api.base.url:https://fapi.binance.com}")
    private String apiBaseUrl;

    private SyncRequestClient publicClient;
    private SyncRequestClient privateClient;

    public SyncRequestClient getPublicClient() {
        if (publicClient == null){
            RequestOptions options = new RequestOptions();
            options.setUrl(apiBaseUrl);
            publicClient =SyncRequestClient.create(null, null, options);
        }
        return publicClient;
    }

    public SyncRequestClient getPrivateClient() {
        if (privateClient == null){
            RequestOptions options = new RequestOptions();
            options.setUrl(apiBaseUrl);
            privateClient =SyncRequestClient.create(apiKey, secretKey, options);
        }
        return privateClient;
    }
}
