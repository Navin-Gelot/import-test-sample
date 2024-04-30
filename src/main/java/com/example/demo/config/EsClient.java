package com.example.demo.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;

@Configuration
@AllArgsConstructor
@Slf4j
public class EsClient {
    private final EsConfig esConfig;

    @Bean
    public RestHighLevelClient elasticClient() {
        log.debug("initializing elasticsearch client from EsClient");
        int _2MinMillis = 120000;
        return prepareClient(15_000, _2MinMillis);
    }

    @Bean
    public RestHighLevelClient bulkElasticClient() {
        log.debug("initializing elasticsearch client from bulk EsClient");
        int _4MinMillis = 240000;
        return prepareClient(40_000, _4MinMillis);
    }

    private RestHighLevelClient prepareClient(int socketTimeout, int maxRetryTimeout) {
        HttpHost[] httpHosts = Arrays.stream(esConfig.getHosts().split(",")).map(hostPort -> {
            String[] hp = hostPort.split(":");
            return new HttpHost(hp[0], Integer.parseInt(hp[1]));
        }).toArray(HttpHost[]::new);

        RestClient client = RestClient.builder(httpHosts)
                .setMaxRetryTimeoutMillis(maxRetryTimeout)
                .setFailureListener(new RestClient.FailureListener() {
                    @Override
                    public void onFailure(HttpHost host) {
                        log.error("Failed elasticsearch request for : " + host);
                    }
                })
                .setRequestConfigCallback((requestConfigBuilder) -> requestConfigBuilder.setSocketTimeout(socketTimeout))
                .build();
        return new RestHighLevelClient(client);
    }

}
