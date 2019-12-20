package com.codingapi.tcp.ping;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.IpPacket;
import org.pcap4j.packet.Packet;

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


    private String deviceName;

    private PcapHandle handle;

    private PcapHandle sendHandle;

    private ExecutorService pool = Executors.newSingleThreadExecutor();

    public PcapNetworkLayer() {
        deviceName = "en0";
    }

    public PcapNetworkLayer(String deviceName) {
        this.deviceName = deviceName;
    }

    public void start() throws Exception{
        PcapNetworkInterface nif= Pcaps.getDevByName(deviceName);
        //网卡监听Handle
        handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
        //创建发送数据包Handle
        sendHandle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);

        //设置监听拦截
        handle.setFilter(
                "",
                BpfProgram.BpfCompileMode.OPTIMIZE);

        PacketListener listener =
                packet -> {
                    if (packet.contains(IpPacket.class)) {
                        IpPacket ipPacket = packet.get(IpPacket.class);
                        log.info("header:\n{}",ipPacket.getHeader());
                    }
                };
        //启动线程拦截数据包
        Task t = new Task(handle, listener,COUNT);
        pool.execute(t);
    }


    public void send(Packet packet) throws Exception{
        sendHandle.sendPacket(packet);
    }

    public void close(){
        if (handle != null && handle.isOpen()) {
            handle.close();
        }
        if (sendHandle != null && sendHandle.isOpen()) {
            sendHandle.close();
        }
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
    }

}
