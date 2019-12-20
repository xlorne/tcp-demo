package com.codingapi.tcp.demo;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author lorne
 * @date 2019/12/18
 * @description
 */
public class HttpSocketTest {

    public static void main(String[] args) {

        Registry<ConnectionSocketFactory> connectionSocketFactoryRegistry =  RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new MyConnectionSocketFactory())
//                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        CloseableHttpClient httpClient =  HttpClients.custom()
                .setConnectionManager(new PoolingHttpClientConnectionManager(connectionSocketFactoryRegistry))
                .build();

        ClientHttpRequestFactory httpClientRequestFactory=  new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate =new RestTemplate(httpClientRequestFactory);

        String res = restTemplate.getForObject("http://www.baidu.com",String.class);
        System.out.println(res);
    }
}
