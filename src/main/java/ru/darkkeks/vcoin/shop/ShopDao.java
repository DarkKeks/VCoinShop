package ru.darkkeks.vcoin.shop;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShopDao {

    private final static Logger logger = LoggerFactory.getLogger(ShopDao.class);

    private final static String SELECT_COUNT = "SELECT transfered FROM used_codes WHERE code = ?";
    private final static String INSERT = "INSERT INTO used_codes (code, user_id, referrer) VALUES (?, ?, ?)" +
            "ON CONFLICT (code) DO UPDATE SET user_id = excluded.user_id, time = now(), referrer = excluded.referrer";
    private final static String INSERT_MERCHANT = "INSERT INTO merchant_info (code, data) VALUES (?, ?) " +
            "ON CONFLICT DO NOTHING";
    private final static String CONFIRM = "UPDATE used_codes SET transfered = True WHERE code = ?";

    private final HikariDataSource dataSource;

    public ShopDao(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void insertMerchantInfo(String code, String info) throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_MERCHANT)) {
            statement.setString(1, code);
            statement.setString(2, info);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Insert merchant", e);
            throw e;
        }
    }

    public boolean isUsed(String code) throws SQLException {
        try(Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(SELECT_COUNT)) {
            statement.setString(1, code);
            ResultSet result = statement.executeQuery();
            if(result.next()) {
                return result.getBoolean("transfered");
            } else {
                return false;
            }
        } catch (SQLException e) {
            logger.error("Select count", e);
            throw e;
        }
    }

    public void insertCode(String code, int userId, String referrer) throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, code);
            statement.setInt(2, userId);
            statement.setString(3, referrer);
            statement.execute();
        } catch (SQLException e) {
            logger.error("Insert", e);
            throw e;
        }
    }

    public void confirmTransfer(String code) throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(CONFIRM)) {
            statement.setString(1, code);
            statement.execute();
        } catch (SQLException e) {
            logger.error("Confirm", e);
            throw e;
        }
    }
}
