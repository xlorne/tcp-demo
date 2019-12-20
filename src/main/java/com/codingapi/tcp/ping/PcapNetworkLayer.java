package com.codingapi.tcp.ping;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.util.MacAddress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author lorne
 * @date 2019/12/20
 * @description
 */
@Slf4j
public class PcapNetworkLayer {

    private static final String COUNT_KEY = PcapNetworkLayer.class.getName() + ".count";
    private static final int COUNT = Integer.getInteger(COUNT_KEY, -1);

    private static final String READ_TIMEOUT_KEY = PcapNetworkLayer.class.getName() + ".readTimeout";
    private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]

    private static final String SNAPLEN_KEY = PcapNetworkLayer.class.getName() + ".snaplen";
    private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]


    private String deviceName = "en0";
    private String localIp = "10.0.0.160";
    private String deviceMacAddress = "78:4f:43:90:44:e8";

    private PcapHandle handle;

    public PcapNetworkLayer() {

    }

    public PcapNetworkLayer(String deviceName, String localIp, String deviceMacAddress) {
        this.deviceName = deviceName;
        this.localIp = localIp;
        this.deviceMacAddress = deviceMacAddress;
    }

    public void start() throws Exception{
        PcapNetworkInterface nif= Pcaps.getDevByName(deviceName);
        MacAddress SRC_MAC_ADDR = MacAddress.getByName(deviceMacAddress);


        //网卡监听Handle
        handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);

        //设置监听拦截
        handle.setFilter(
                "",
                BpfProgram.BpfCompileMode.OPTIMIZE);

        PacketListener listener =
                packet -> {
                    if (packet.contains(ArpPacket.class)) {
                        ArpPacket arp = packet.get(ArpPacket.class);
                    }
                    log.info("packet:{}",packet);
                };
        //启动线程拦截数据包
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Task t = new Task(handle, listener,COUNT);
        pool.execute(t);
    }


    public void close(){
        if (handle != null && handle.isOpen()) {
            handle.close();
        }
    }

}
