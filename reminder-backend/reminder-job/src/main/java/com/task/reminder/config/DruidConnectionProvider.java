package com.task.reminder.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.quartz.utils.ConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quartz连接池的Druid实现
 * 实现ConnectionProvider接口，使Quartz能够使用Druid连接池
 */
public class DruidConnectionProvider implements ConnectionProvider {
    private static final Logger logger = LoggerFactory.getLogger(DruidConnectionProvider.class);
    
    private DruidDataSource dataSource;
    
    // JDBC参数
    private String driver;
    private String URL;
    private String user;
    private String password;
    private int maxConnections = 10;
    private String validationQuery;
    
    // Druid特有参数
    private long maxWaitMillis;
    private int idleConnectionTestPeriod;
    
    public void setDriver(String driver) {
        this.driver = driver;
    }
    
    public void setURL(String URL) {
        this.URL = URL;
    }
    
    public void setUser(String user) {
        this.user = user;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }
    
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }
    
    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }
    
    public void setIdleConnectionTestPeriod(int idleConnectionTestPeriod) {
        this.idleConnectionTestPeriod = idleConnectionTestPeriod;
    }

    @Override
    public void initialize() throws SQLException {
        try {
            dataSource = new DruidDataSource();
            
            // 设置JDBC基本连接参数
            dataSource.setDriverClassName(driver);
            dataSource.setUrl(URL);
            dataSource.setUsername(user);
            dataSource.setPassword(password);
            
            // 设置连接池参数
            dataSource.setMaxActive(maxConnections);
            if (validationQuery != null && !validationQuery.trim().isEmpty()) {
                dataSource.setValidationQuery(validationQuery);
            }
            dataSource.setInitialSize(3);
            dataSource.setMinIdle(3);
            dataSource.setTestWhileIdle(true);
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            
            // 设置连接池防护参数
            if (maxWaitMillis > 0) {
                dataSource.setMaxWait(maxWaitMillis);
            }
            
            // 配置监控统计拦截的filters
            dataSource.setFilters("stat,wall");
            
            // 初始化
            dataSource.init();
            
            logger.info("DruidConnectionProvider初始化成功");
        } catch (Exception e) {
            logger.error("DruidConnectionProvider初始化失败", e);
            throw new SQLException("初始化Druid数据源失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() throws SQLException {
        try {
            dataSource.close();
        } catch (Exception e) {
            logger.error("关闭Druid连接池失败", e);
            throw new SQLException("关闭Druid连接池失败: " + e.getMessage(), e);
        }
    }
}