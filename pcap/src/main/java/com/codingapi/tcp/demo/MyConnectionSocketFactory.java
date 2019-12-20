package com.codingapi.tcp.demo;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author lorne
 * @date 2019/12/18
 * @description
 */
@Slf4j
public class MyConnectionSocketFactory implements ConnectionSocketFactory {

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        log.info("socket create ! ");
        return new Socket();
    }

    @Override
    public Socket connectSocket(int connectTimeout,
                                Socket socket, HttpHost host, InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        final Socket sock = socket != null ? socket : createSocket(context);
        log.info("socket=>{}",socket);
        if (localAddress != null) {
            sock.bind(localAddress);
        }
        try {
            sock.connect(remoteAddress, connectTimeout);
        } catch (final IOException ex) {
            try {
                sock.close();
            } catch (final IOException ignore) {
            }
            throw ex;
        }
        return sock;
    }
}
