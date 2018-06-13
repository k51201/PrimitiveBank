package ru.vampa.primitiveBank.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import ru.vampa.primitiveBank.Server;
import ru.vampa.primitiveBank.service.IAccountService;
import ru.vampa.primitiveBank.service.InsufficientFundsException;
import ru.vampa.primitiveBank.service.NoAccountRegisteredException;

import java.nio.charset.StandardCharsets;

import static ru.vampa.primitiveBank.Server.*;

/**
 * Хендлер для осуществления операций с полученными параметрами
 *
 * @author vbelyashov
 */

public class WorkerHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof NoAccountRegisteredException) {
            // Если не нашли счет с таким id
            final String accountId = cause.getMessage();
            Server.sendError(ctx, "account with id " + accountId + " not found", HttpResponseStatus.NOT_FOUND);
        }
        if (cause instanceof InsufficientFundsException) {
            // Если не хватило денег
            final String accountId = cause.getMessage();
            Server.sendError(ctx, "on account with id " + accountId + " not enough funds",
                    HttpResponseStatus.FORBIDDEN);
        } else {
            Server.sendError(ctx, "internal server error: " + cause.getMessage(),
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj)
            throws NoAccountRegisteredException, InsufficientFundsException {
        final FullHttpRequest request = (FullHttpRequest) obj;

        final String operation = request.headers().get(HEAD_OPERATION);
        final Long accountId = Long.parseUnsignedLong(request.headers().get(HEAD_ACCOUNT_ID));
        final Long amount;

        final IAccountService accountService = IAccountService.INSTANCE;
        final DefaultFullHttpResponse response;

        switch (operation) {
            case OPER_BALANCE: // Запрос баланса
                response = getBalanceResponse(accountId, accountService);
                break;
            case OPER_DEPOSIT: // Внесение
                amount = Long.parseUnsignedLong(request.headers().get(HEAD_AMOUNT));
                accountService.deposit(accountId, amount);
                response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
                break;
            case OPER_TRANSFER: // Перевод на другой счет
                amount = Long.parseUnsignedLong(request.headers().get(HEAD_AMOUNT));
                final Long destinationId = Long.parseUnsignedLong(request.headers().get(HEAD_DESTINATION_ID));
                accountService.transfer(accountId, destinationId, amount);
                response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
                break;
            case OPER_WITHDRAW: // Снятие
                amount = Long.parseUnsignedLong(request.headers().get(HEAD_AMOUNT));
                accountService.withdraw(accountId, amount);
                response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
                break;
            default:
                throw new IllegalStateException("unknown operation");
        }

        final ChannelFuture channelFuture = ctx.writeAndFlush(response);
        channelFuture.addListener(ChannelFutureListener.CLOSE);

        request.release();
    }

    /**
     * Возвращает http-ответ на запрос баланса
     * @param accountId id счета
     * @param accountService служба для работы со счетом
     * @throws NoAccountRegisteredException в случае, если счета с таким id не существует
     */
    private DefaultFullHttpResponse getBalanceResponse(Long accountId, IAccountService accountService)
            throws NoAccountRegisteredException {
        final long balance = accountService.getBalance(accountId);
        final ByteBuf responseContent = Unpooled.copiedBuffer(Long.toString(balance), StandardCharsets.UTF_8);

        final DefaultFullHttpResponse response =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, responseContent);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.ACCEPT_CHARSET, StandardCharsets.UTF_8.name());
        return response;
    }
}
