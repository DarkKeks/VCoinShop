package ru.darkkeks.vcoin.shop;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Util {

    private final static Logger logger = LoggerFactory.getLogger(Util.class);

    public static String post(HttpClient client, String url, String body) throws IOException {
        HttpPost request = new HttpPost(url);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(body, Charsets.UTF_8));
        try {
            HttpResponse response = client.execute(request);

            try(InputStream content = response.getEntity().getContent()) {
                return IOUtils.toString(content, Charsets.UTF_8);
            }
        } catch (SocketException e) {
            logger.error("Network error", e);
            throw e;
        }
    }


    public static String md5(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(string.getBytes());
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            logger.error("Kek", e);
            return null;
        }
    }
}
