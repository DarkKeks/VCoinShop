package ru.darkkeks.vcoin.shop;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.Optional;

public class Launcher {

    private static final String DATABASE_URL = getEnv("DATABASE_URL");
    private static final String DATABASE_USERNAME = getEnv("DATABASE_USERNAME");
    private static final String DATABASE_PASSWORD = getEnv("DATABASE_PASSWORD");

    private static final int MERCHANT_ID = Integer.valueOf(getEnv("MERCHANT_ID"));
    private static final String MERCHANT_PASSWORD = getEnv("MERCHANT_PASSWORD");

    private static final int VCOIN_ID = Integer.valueOf(getEnv("VCOIN_ID"));
    private static final String VCOIN_KEY = getEnv("VCOIN_KEY");

    private static final int PORT = Integer.valueOf(getEnv("PORT"));

    public static void main(String[] args) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DATABASE_URL);
        config.setUsername(DATABASE_USERNAME);
        config.setPassword(DATABASE_PASSWORD);
        HikariDataSource dataSource = new HikariDataSource(config);

        HttpClient client = HttpClientBuilder.create().build();

        VCoinApi vCoinApi = new VCoinApi(VCOIN_ID, VCOIN_KEY, client);
        MerchantManager manager = new MerchantManager(MERCHANT_ID, MERCHANT_PASSWORD, new ShopDao(dataSource), client);

        new Server(manager, vCoinApi).start(PORT);
    }

    private static String getEnv(String name) {
        return Optional.ofNullable(System.getenv(name)).orElseThrow(() -> new IllegalStateException("Env " + name));
    }
}
