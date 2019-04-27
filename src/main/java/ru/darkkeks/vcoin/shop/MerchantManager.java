package ru.darkkeks.vcoin.shop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.http.client.HttpClient;

import java.sql.SQLException;

public class MerchantManager {

    private static final String MERCHANT_URL = "http://shop.digiseller.ru/xml/check_unique_code.asp";

    private static final JsonParser parser = new JsonParser();

    private int id;
    private String password;
    private ShopDao shopDao;
    private HttpClient httpClient;

    public MerchantManager(int id, String password, ShopDao shopDao, HttpClient httpClient) {
        this.id = id;
        this.password = password;
        this.shopDao = shopDao;
        this.httpClient = httpClient;
    }

    public CodeInfo useCode(String code, int vkId, String referrer) throws Exception {
        if(shopDao.isUsed(code)) {
            return new CodeInfo(-1, false, true, 0);
        } else {
            JsonObject data = new JsonObject();
            data.add("id_seller", new JsonPrimitive(id));
            data.add("unique_code", new JsonPrimitive(code));
            data.add("sign", new JsonPrimitive(getSign(code)));

            String response = Util.post(httpClient, MERCHANT_URL, data.toString());
            JsonObject result = parser.parse(response).getAsJsonObject();

            shopDao.insertMerchantInfo(code, response);

            if(Integer.parseInt(result.get("retval").getAsString()) == 0) {
                shopDao.insertCode(code, vkId, referrer);
                String amountString = result.get("cnt_goods").getAsString().replace(",", ".");
                String profitString = result.get("profit").getAsString().replace(",", ".");
                long amount = Math.round(Double.parseDouble(amountString) * 1e6);
                double profit = Double.parseDouble(profitString);
                return new CodeInfo(amount, true, false, profit);
            } else {
                return new CodeInfo(-1, false, false, 0);
            }
        }
    }

    public void confirmTransfer(String code) {
        try {
            shopDao.confirmTransfer(code);
        } catch (SQLException ignored) {}
    }

    private String getSign(String code) {
        return Util.md5(String.format("%s:%s:%s", id, code, password));
    }

    public static class CodeInfo {
        private boolean isUsed;
        private boolean valid;
        private long amount;
        private double profit;

        public CodeInfo(long amount, boolean valid, boolean isUsed, double profit) {
            this.amount = amount;
            this.valid = valid;
            this.isUsed = isUsed;
            this.profit = profit;
        }

        public boolean isUsed() {
            return isUsed;
        }

        public boolean isValid() {
            return valid;
        }

        public long getAmount() {
            return amount;
        }

        public double getProfit() {
            return profit;
        }
    }

}
