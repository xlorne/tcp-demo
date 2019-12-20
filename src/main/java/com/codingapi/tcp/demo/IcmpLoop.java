package com.codingapi.tcp.demo;

import org.pcap4j.core.*;
import org.pcap4j.packet.IcmpV4CommonPacket;

/**
 * @author lorne
 * @date 2019/12/15
 * @description
 */
public class IcmpLoop {

    private static final int COUNT = -1;
    private static final int SNAPLEN =  65536; // [bytes]
    private static final int READ_TIMEOUT = 10;

    public static void main(String[] args) throws Exception{

        String filter = "";

        //本机的网卡设备名称
        PcapNetworkInterface nif= Pcaps.getDevByName("en0");

        final PcapHandle handle = nif.openLive(SNAPLEN, PcapNetworkInterface.PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);

        handle.setFilter(filter, BpfProgram.BpfCompileMode.OPTIMIZE);

        PacketListener listener =
                packet -> {
                    if(packet.contains(IcmpV4CommonPacket.class)){
                        IcmpV4CommonPacket commonPacket = packet.get(IcmpV4CommonPacket.class);
                        System.out.println("packet:\n"+commonPacket);
                    }
                };

        handle.loop(COUNT, listener);
        handle.close();

    }
}
