package com.codingapi.tcp.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

/**
 * @author lorne
 * @date 2019/12/20
 * @description
 */
@SpringBootApplication
public class PingApplicationTest {

    public static void main(String[] args) {
        SpringApplication.run(PingApplicationTest.class,args);
    }

    @PostConstruct
    public void start()throws Exception{
        PcapNetworkLayer pcapNetworkLayer = new PcapNetworkLayer();
        pcapNetworkLayer.start();
    }
}
