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

    private final static String INSERT = "INSERT INTO used_codes (code, user_id, referrer, merchant_info, coins) " +
            "VALUES (?, ?, ?, ?::jsonb, ?) ON CONFLICT (code) DO UPDATE SET " +
            "user_id = excluded.user_id, " +
            "time = now(), " +
            "referrer = excluded.referrer, " +
            "merchant_info = excluded.merchant_info, " +
            "coins = excluded.coins";

    private final static String CONFIRM = "UPDATE used_codes SET transfered = True WHERE code = ?";

    private final HikariDataSource dataSource;

    public ShopDao(HikariDataSource dataSource) {
        this.dataSource = dataSource;
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

    public void insertCode(String code, int userId, String referrer, String merchant, long coins) throws SQLException {
        try(Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT)) {
            statement.setString(1, code);
            statement.setInt(2, userId);
            statement.setString(3, referrer);
            statement.setString(4, merchant);
            statement.setLong(5, coins);
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
