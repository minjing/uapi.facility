/*
 * Copyright (C) 2017. The UAPI Authors
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at the LICENSE file.
 *
 * You must gained the permission from the authors if you want to
 * use the project into a commercial product
 */

package uapi.net.telnet.internal;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import uapi.net.INetChannelHandler;
import uapi.net.NetException;
import uapi.net.telnet.ITelnetListener;
import uapi.net.telnet.TelnetAttributes;
import uapi.service.ServiceType;
import uapi.service.annotation.Attribute;
import uapi.service.annotation.Service;

import java.net.InetAddress;
import java.util.Date;

@Service(
        value = ITelnetListener.class,
        type = ServiceType.Prototype
)
public class TelnetListener implements ITelnetListener {

    private static final String DEFAULT_HOST    = "localhost";
    private static final int DEFAULT_PORT       = 23;

    private EventLoopGroup _bossGroup;
    private EventLoopGroup _workerGroup;

    @Attribute(TelnetAttributes.HOST)
    protected String _host = DEFAULT_HOST;

    @Attribute(TelnetAttributes.PORT)
    protected int _port = DEFAULT_PORT;

    @Attribute(TelnetAttributes.HANDLER)
    protected INetChannelHandler _handler;

    @Override
    public void startUp() throws NetException {
        this._bossGroup = new NioEventLoopGroup(1);
        this._workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(this._bossGroup, this._workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new TelNetInitializer());
            Channel ch = bootstrap.bind(this._host, this._port).sync().channel();
            ch.closeFuture().sync();
        } catch (Exception ex) {
            // do nothing
        } finally {
            this._bossGroup.shutdownGracefully();
            this._workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void shutDown() throws NetException {
        if (this._bossGroup != null) {
            this._bossGroup.shutdownGracefully();
        }
        if (this._workerGroup != null) {
            this._workerGroup.shutdownGracefully();
        }
    }

    private final class TelNetInitializer extends ChannelInitializer<SocketChannel> {

        private final StringDecoder DECODER = new StringDecoder();
        private final StringEncoder ENCODER = new StringEncoder();

        @Override
        protected void initChannel(SocketChannel channel) throws Exception {
            ChannelPipeline pipeline = channel.pipeline();
            
            pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
            pipeline.addLast(DECODER);
            pipeline.addLast(ENCODER);
            pipeline.addLast(new TelNetHandler());
        }
    }

    private final class TelNetHandler extends SimpleChannelInboundHandler<String> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
            ctx.write("It is " + new Date() + " now.\r\n");
            ctx.flush();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String request) throws Exception {
            String response;
            boolean close = false;
            if (request.isEmpty()) {
                response = "Please input command.\r\n";
            } else if ("bye".equals(request.toLowerCase())) {
                response = "Good bye!\r\n";
                close = true;
            } else {
                response = "Did you say '" + request + "'?\r\n";
            }

            ChannelFuture future = ctx.write(response);

            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
                TelnetListener.this.shutDown();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
