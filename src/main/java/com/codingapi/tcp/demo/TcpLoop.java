package com.codingapi.tcp.demo;


import com.sun.jna.Platform;
import lombok.extern.slf4j.Slf4j;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.*;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.util.NifSelector;

import java.io.IOException;

@Slf4j
@SuppressWarnings("javadoc")
public class TcpLoop {

    private static final String COUNT_KEY = TcpLoop.class.getName() + ".count";
    private static final int COUNT = Integer.getInteger(COUNT_KEY, -1);

    private static final String READ_TIMEOUT_KEY = TcpLoop.class.getName() + ".readTimeout";
    private static final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]

    private static final String SNAPLEN_KEY = TcpLoop.class.getName() + ".snaplen";
    private static final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]

    private TcpLoop() {}

    public static void main(String[] args) throws PcapNativeException, NotOpenException {
        String filter = args.length != 0 ? args[0] : "";

        System.out.println(COUNT_KEY + ": " + COUNT);
        System.out.println(READ_TIMEOUT_KEY + ": " + READ_TIMEOUT);
        System.out.println(SNAPLEN_KEY + ": " + SNAPLEN);
        System.out.println("\n");

        PcapNetworkInterface nif=Pcaps.getDevByName("en0");

//        System.out.println(nif.getName() + "(" + nif.getDescription() + ")");

        final PcapHandle handle = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);

        if (filter.length() != 0) {
            handle.setFilter(filter, BpfCompileMode.OPTIMIZE);
        }

        PacketListener listener =
                new PacketListener() {
                    @Override
                    public void gotPacket(Packet packet) {
//                        System.out.println(handle.getTimestamp());
//                        System.out.println(packet);
//                        System.out.println(String.format("time:%s\ndata:%s",handle.getTimestamp(),new String(packet.getRawData(), StandardCharsets.UTF_8)));

                        if(packet.contains(TcpPacket.class)){
                            TcpPacket tcpPacket = packet.get(TcpPacket.class);
                            TcpPacket.TcpHeader tcpHeader = tcpPacket.getHeader();
                            if(tcpHeader.getDstPort().valueAsInt() == 80) {

                                log.info("data:{}", tcpPacket);
                                if(tcpHeader.getRawData()!=null&&tcpHeader.getRawData().length>0) {
                                    log.info("msg:{}", new String(tcpHeader.getRawData()));
                                }
                            }
                        }

                    }
                };

        try {
            handle.loop(COUNT, listener);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PcapStat ps = handle.getStats();
        System.out.println("ps_recv: " + ps.getNumPacketsReceived());
        System.out.println("ps_drop: " + ps.getNumPacketsDropped());
        System.out.println("ps_ifdrop: " + ps.getNumPacketsDroppedByIf());
        if (Platform.isWindows()) {
            System.out.println("bs_capt: " + ps.getNumPacketsCaptured());
        }

        handle.close();
    }
}