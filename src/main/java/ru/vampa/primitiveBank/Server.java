package ru.vampa.primitiveBank;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import ru.vampa.primitiveBank.netty.RequestFilterHandler;
import ru.vampa.primitiveBank.netty.WorkerHandler;

import java.nio.charset.StandardCharsets;

/**
 * Запуск http-сервера
 *
 * @author vbelyashov
 */

public class Server implements Runnable {
    // заголовки, используемые для передачи параметров в запросе между хендлерами
    public static final String HEAD_OPERATION = "requested-operaion";
    public static final String HEAD_ACCOUNT_ID = "account-id";
    public static final String HEAD_DESTINATION_ID = "destination-id";
    public static final String HEAD_AMOUNT = "amount";

    // коды операций, передаются в заголовке operation и, по совместительству, являются названиями ресурсов,
    // обращение к которым, вызывают соответствующую операцию
    public static final String OPER_BALANCE = "balance";
    public static final String OPER_DEPOSIT = "deposit";
    public static final String OPER_TRANSFER = "transfer";
    public static final String OPER_WITHDRAW = "withdraw";

    private final int port;

    private Server(int port) {
        this.port = port;
    }

    public void run() {
        final EventLoopGroup workerGroup = new NioEventLoopGroup();
        final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        ChannelFuture channelFuture = null;
        try {
            final ServerBootstrap server = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(Integer.MAX_VALUE))
                                    .addLast(new RequestFilterHandler()) // извлекаем данные из запроса, проверяем
                                    .addLast(new WorkerHandler()); // производим операцию
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 500)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            channelFuture = server.bind("localhost", port).sync();
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            if (channelFuture != null)
                channelFuture.channel().close().awaitUninterruptibly();
        }
    }

    /**
     * Генерирует и отправляет ответ с кодом ошибки status и сообщением errorMessage
     * @param errorMessage сообение об ошибке, будет в теле ответа
     * @param status код ошибки
     */
    public static void sendError(ChannelHandlerContext ctx, String errorMessage, HttpResponseStatus status) {
        final ByteBuf content = Unpooled.copiedBuffer(errorMessage, StandardCharsets.UTF_8);
        final FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);

        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());

        final ChannelFuture channelFuture = ctx.writeAndFlush(response);
        channelFuture.addListener(ChannelFutureListener.CLOSE);
    }

    public static void main(String[] args) {
        new Server(8080).run();
    }
}
