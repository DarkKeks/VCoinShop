package ru.darkkeks.vcoin.shop;

import com.google.gson.Gson;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.util.Headers;
import io.undertow.util.MimeMappings;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public class Server {

    private final static Logger logger = LoggerFactory.getLogger(Server.class);

    private Gson gson;
    private Undertow server;
    private MerchantManager merchantManager;
    private VCoinApi vCoinApi;

    public Server(MerchantManager merchantManager, VCoinApi vCoinApi) {
        this.vCoinApi = vCoinApi;
        this.merchantManager = merchantManager;
        this.gson = new Gson();
    }

    public void start(int port) {
        server = Undertow.builder()
                .addHttpListener(port, "0.0.0.0")
                .setIoThreads(2)
                .setWorkerThreads(2)
                .setHandler(Handlers.path()
                        .addExactPath("/verify", this::handleTransfer)
                        .addPrefixPath("/", Handlers
                                .resource(new ClassPathResourceManager(getClass().getClassLoader(), "public/"))
                                .setMimeMappings(MimeMappings.builder(true)
                                        .addMapping("html", "text/html;charset=utf-8")
                                        .build())
                        ))
                .build();
        server.start();
    }

    private void handleTransfer(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseHeaders().add(Headers.CONTENT_ENCODING, "UTF-8");
        exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "application/json");

        Optional<Integer> vkIdOptional = queryParam(exchange, "vk_id").map(Integer::parseInt);
        Optional<String> codeOptional = queryParam(exchange, "uniquecode").map(String::toLowerCase);
        String referrer = queryParam(exchange, "referrer").orElse(null);

        logger.info("ProcessCode(vkId={}, code={}, referrer={})",
                vkIdOptional.orElse(null), codeOptional.orElse(null), referrer);
        if(vkIdOptional.isPresent() && codeOptional.isPresent()) {
            exchange.getResponseSender().send(gson.toJson(processCode(codeOptional.get(), vkIdOptional.get(),
                    referrer)));
        } else {
            exchange.getResponseSender().send(gson.toJson(Result.error(ResultError.HORSE)));
        }
        exchange.endExchange();
    }

    private Result processCode(String code, Integer vkId, String referrer) {
        try {
            MerchantManager.CodeInfo info = merchantManager.useCode(code, vkId, referrer);
            logger.info("CodeInfo(valid={}, used={}, amount={})", info.isValid(), info.isUsed(), info.getAmount());
            if(!info.isUsed() && info.isValid()) {
                try {
                    logger.info("Transferring(to={}, amount={})", vkId, info.getAmount());
                    vCoinApi.transfer(vkId, info.getAmount()).join();
                    merchantManager.confirmTransfer(code);
                    return Result.success(info.getAmount(), code, vkId);
                } catch (CompletionException e) {
                    logger.error("Can't transfer", e);
                    return Result.error(ResultError.ERROR, e.getMessage(), code, vkId);
                }
            } else {
                return Result.error(ResultError.INVALID, code, vkId);
            }
        } catch (Exception e) {
            logger.error("IO error", e);
            return Result.error(ResultError.ERROR, e.getMessage(), code, vkId);
        }
    }

    private Optional<String> queryParam(HttpServerExchange exchange, String name) {
        return Optional.ofNullable(exchange.getQueryParameters().get(name))
                .map(Deque::getFirst);
    }

    private static class Result {
        private boolean success;
        private ResultError error;
        private String message;

        private long amount;
        private String code;
        private int vkId;

        public Result(boolean success, ResultError error, String message, long amount, String code, int vkId) {
            this.success = success;
            this.error = error;
            this.message = message;
            this.amount = amount;
            this.code = code;
            this.vkId = vkId;
        }

        public static Result success(long amount, String code, int vkId) {
            return new Result(true, ResultError.NONE, null, amount, code, vkId);
        }

        public static Result error(ResultError error, String message, String code, int vkId) {
            return new Result(false, error, message, 0, code, vkId);
        }

        public static Result error(ResultError error, String code, int vkId) {
            return new Result(false, error, null, 0, code, vkId);
        }

        public static Result error(ResultError error) {
            return new Result(false, error, null, 0, null, -1);
        }
    }

    private enum ResultError {
        HORSE,
        ERROR,
        INVALID,
        NONE
    }
}
