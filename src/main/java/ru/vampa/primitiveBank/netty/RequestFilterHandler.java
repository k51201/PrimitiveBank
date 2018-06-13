package ru.vampa.primitiveBank.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import ru.vampa.primitiveBank.Server;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static ru.vampa.primitiveBank.Server.*;

/**
 * Хендлер для получения параметров запроса.
 * Делает валидацию и, если всё хорошо - складывает параметры в заголовки
 *
 * @author vbelyashov
 */

public class RequestFilterHandler extends MessageToMessageDecoder<FullHttpRequest> {
    private static final String ACCOUNTS_URL = "accounts";

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Server.sendError(ctx, "internal server error: " + cause.getMessage(),
                HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) {
        final HttpMethod method = request.method();

        // Поддерживается только GET для запроса баланса и POST для всего остального
        if (method == HttpMethod.GET) handleGet(ctx, request, out);
        else if (method == HttpMethod.POST) handlePost(ctx, request, out);
        else Server.sendError(ctx, "method is not acceptable", HttpResponseStatus.NOT_ACCEPTABLE);
    }

    /**
     * Обрабатывает запросы типа 'GET /accounts/123', где 123 - id счета. Проверяет, что id парсится и больше 0.
     * Складывает в заголовки только код операции (balance) и id счета.
     */
    private void handleGet(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) {
        final List<String> urlParts = getUrlParts(request);
        if (urlParts == null || urlParts.size() != 2 || !ACCOUNTS_URL.equals(urlParts.get(0))) {
            send404(ctx);
            return;
        }

        final String accountId = urlParts.get(1);
        if (parsesIncorrect(accountId)) { // Проверяет id только на соответствие формата (Long)
            Server.sendError(ctx, "invalid account id", HttpResponseStatus.BAD_REQUEST);
            return;
        }

        request.headers().add(HEAD_OPERATION, OPER_BALANCE);
        request.headers().add(HEAD_ACCOUNT_ID, accountId);
        out.add(request);
        request.retain();
    }

    /**
     * Обрабатывает запросы типа 'POST /accounts/123/withdraw', где 123 - id счета, а withdraw - операция.
     * Складывает в заголовки код операции, сумму и счет назначения (если это перевод). Проверяет парсятся ли параметры
     * перед отправкой.
     */
    private void handlePost(ChannelHandlerContext ctx, FullHttpRequest request, List<Object> out) {
        // Достаем сумму операции из тела запроса
        final ByteBuf content = request.content();
        if (content.isReadable()) {
            final String amount = content.readCharSequence(content.readableBytes(), CharsetUtil.UTF_8).toString();
            if (parsesIncorrect(amount)) {
                Server.sendError(ctx, "invalid amount", HttpResponseStatus.BAD_REQUEST);
                return;
            }
            request.headers().add(HEAD_AMOUNT, amount);
        }

        final List<String> urlParts = getUrlParts(request);
        if (urlParts == null || urlParts.size() < 3 || !ACCOUNTS_URL.equals(urlParts.get(0))) {
            send404(ctx);
            return;
        }

        final String operation = urlParts.get(2);
        switch (operation) {
            case OPER_TRANSFER:
                // Для перевода требуется id счета назначения, проверяем, что он есть и парсится
                if (urlParts.size() > 3) {
                    final String destinationId = urlParts.get(3);
                    if (parsesIncorrect(destinationId)) { // Проверяет id только на соответствие формата (Long)
                        Server.sendError(ctx, "invalid destination id", HttpResponseStatus.BAD_REQUEST);
                        return;
                    }
                    request.headers().add(HEAD_DESTINATION_ID, destinationId);
                }
                else {
                    Server.sendError(ctx, "no destination for transfer", HttpResponseStatus.BAD_REQUEST);
                    return;
                }
            case OPER_DEPOSIT:
            case OPER_WITHDRAW:
                final String accountId = urlParts.get(1);
                if (parsesIncorrect(accountId)) {
                    Server.sendError(ctx, "invalid account id", HttpResponseStatus.BAD_REQUEST);
                    return;
                }
                request.headers().add(HEAD_ACCOUNT_ID, accountId);
                request.headers().add(HEAD_OPERATION, operation);
                out.add(request);
                request.retain();
                break;
            default:
                // Если такой операции нет для POST - 404
                send404(ctx);
        }
    }

    /**
     * Отправляет код 404
     */
    private void send404(ChannelHandlerContext ctx) {
        Server.sendError(ctx, "resource is not found", HttpResponseStatus.NOT_FOUND);
    }

    /**
     * Добывает из запроса путь к ресурсу в виде списка
     */
    private List<String> getUrlParts(FullHttpRequest request) {
        final String url = request.uri();
        return url == null ? null :
                Arrays.stream(url.toLowerCase().split("/"))
                        .filter(part -> part != null && !part.isEmpty())
                        .collect(Collectors.toList());
    }

    /**
     * Проверка строки на возможность распарсить её в UnsignedLong, получившееся число должно быть больше нуля
     */
    private static boolean parsesIncorrect(String suspiciousString) {
        try {
            final long parsedLong = Long.parseUnsignedLong(suspiciousString);
            return parsedLong < 1;
        } catch (NumberFormatException e) {
            return true;
        }
    }
}
