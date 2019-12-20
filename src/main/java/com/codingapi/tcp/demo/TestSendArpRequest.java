package com.codingapi.tcp.demo;

import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.*;
import org.pcap4j.packet.ArpPacket;
import org.pcap4j.packet.EthernetPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.namednumber.ArpHardwareType;
import org.pcap4j.packet.namednumber.ArpOperation;
import org.pcap4j.packet.namednumber.EtherType;
import org.pcap4j.util.ByteArrays;
import org.pcap4j.util.MacAddress;

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ARP 协议发送数据包测试
 * 地址解析协议，即ARP（Address Resolution Protocol）
 *
 */
@Slf4j
public class TestSendArpRequest {

    private static final int SNAPLEN =  65536; // [bytes]
    private static final int READ_TIMEOUT = 10; // [ms]
    private static MacAddress resolvedAddr;

    public static void main(String[] args) throws Exception{
        //本机的网卡设备名称
//        PcapNetworkInterface nif=Pcaps.getDevByName("\\Device\\NPF_{C949EE1C-9B37-48B3-9652-9C35609521C4}");
        PcapNetworkInterface nif=Pcaps.getDevByName("en0");
        //本机的IP
        String localIp= "10.0.0.161";
        //本机的MAC地址
//      String deviceMacAddress = "48-8A-D2-43-6A-33";
        String deviceMacAddress = "78:4f:43:90:44:e8";


        MacAddress SRC_MAC_ADDR = MacAddress.getByName(deviceMacAddress);

        //目标IP
        String strDstIpAddress ="10.0.0.245";
        //本机IP
        String strSrcIpAddress =  localIp;

        //网卡监听Handle
        PcapHandle handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);

        //设置监听拦截
        handle.setFilter(
                "arp and src host "
                        + strDstIpAddress
                        + " and dst host "
                        + strSrcIpAddress
                        + " and ether dst "
                        + Pcaps.toBpfString(SRC_MAC_ADDR),
                BpfProgram.BpfCompileMode.OPTIMIZE);

        PacketListener listener =
                packet -> {
                    if (packet.contains(ArpPacket.class)) {
                        ArpPacket arp = packet.get(ArpPacket.class);
                        if (arp.getHeader().getOperation().equals(ArpOperation.REPLY)) {
                            resolvedAddr = arp.getHeader().getSrcHardwareAddr();
                        }
                    }
                    log.info("packet:{}",packet);
                };
        //启动线程拦截数据包
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Task t = new Task(handle, listener);
        pool.execute(t);

        //创建发送数据包Handle
        PcapHandle sendHandle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
        //设置Arp请求
        ArpPacket.Builder arpBuilder = new ArpPacket.Builder();
        arpBuilder
                .hardwareType(ArpHardwareType.ETHERNET)
                .protocolType(EtherType.IPV4)
                .hardwareAddrLength((byte) MacAddress.SIZE_IN_BYTES)
                .protocolAddrLength((byte) ByteArrays.INET4_ADDRESS_SIZE_IN_BYTES)
                .operation(ArpOperation.REQUEST)
                .srcHardwareAddr(SRC_MAC_ADDR)
                .srcProtocolAddr(InetAddress.getByName(strSrcIpAddress))
                .dstHardwareAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
                .dstProtocolAddr(InetAddress.getByName(strDstIpAddress));

        EthernetPacket.Builder etherBuilder = new EthernetPacket.Builder();
        etherBuilder
                .dstAddr(MacAddress.ETHER_BROADCAST_ADDRESS)
                .srcAddr(SRC_MAC_ADDR)
                .type(EtherType.ARP)
                .payloadBuilder(arpBuilder)
                .paddingAtBuild(true);
        //构建并发送
        Packet p = etherBuilder.build();
        log.info("send:{}",p);
        sendHandle.sendPacket(p);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (handle != null && handle.isOpen()) {
            handle.close();
        }
        if (sendHandle != null && sendHandle.isOpen()) {
            sendHandle.close();
        }
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown();
        }
        //打印接受到的反馈MAC信息
        System.out.println(strDstIpAddress + " was resolved to " + resolvedAddr);

    }


    private static class Task implements Runnable {

        private PcapHandle handle;
        private PacketListener listener;

        public Task(PcapHandle handle, PacketListener listener) {
            this.handle = handle;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                handle.loop(1, listener);
            } catch (PcapNativeException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NotOpenException e) {
                e.printStackTrace();
            }
        }
    }
}
