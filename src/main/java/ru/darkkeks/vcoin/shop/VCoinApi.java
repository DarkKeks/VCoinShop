package ru.darkkeks.vcoin.shop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class VCoinApi {

    private static final Logger logger = LoggerFactory.getLogger(VCoinApi.class);

    private static final String TRANSFER = "https://coin-without-bugs.vkforms.ru/merchant/send/";

    private static final JsonParser jsonParser = new JsonParser();

    private ExecutorService transferExecutor;
    private HttpClient httpClient;

    private int userId;
    private String apiKey;

    public VCoinApi(int userId, String apiKey, HttpClient httpClient) {
        this.userId = userId;
        this.apiKey = apiKey;
        this.httpClient = httpClient;

        // Has to be one thread, so we never do concurrent transfers
        this.transferExecutor = new ScheduledThreadPoolExecutor(1);
    }

    private String doRequest(String url, String data) {
        try {
            return Util.post(httpClient, url, data);
        } catch (IOException e) {
            return null;
        }
    }

    public CompletableFuture<TransferResult> transfer(int to, long amount) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject data = new JsonObject();
            data.add("merchantId", new JsonPrimitive(userId));
            data.add("key", new JsonPrimitive(apiKey));
            data.add("toId", new JsonPrimitive(to));
            data.add("amount", new JsonPrimitive(amount));
            logger.info(data.toString());
            return doRequest(TRANSFER, data.toString());
        }, transferExecutor).thenApply(message -> {
            JsonObject result = jsonParser.parse(message).getAsJsonObject();
            if(result.has("error")) {
                throw new IllegalStateException(result.get("error").getAsJsonObject()
                        .get("message").getAsString());
            }
            return new TransferResult(result.get("response").getAsJsonObject());
        });
    }

    public int getUserId() {
        return userId;
    }

    public ExecutorService getTransferExecutor() {
        return transferExecutor;
    }
}
