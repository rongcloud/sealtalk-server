package com.rcloud.server.sealtalk.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Slf4j
public class DatabaseCreatorApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment environment = applicationContext.getEnvironment();

        String dbHost = environment.getProperty("sealtalk-config.db_host");
        String dbPort = environment.getProperty("sealtalk-config.db_port");
        String dbName = environment.getProperty("sealtalk-config.db_name");
        String dbUser = environment.getProperty("sealtalk-config.db_user");
        String dbPassword = environment.getProperty("sealtalk-config.db_password");
        String driverClassName = environment.getProperty("spring.datasource.driverClassName");

        if (StringUtils.isAnyBlank(dbHost, dbPort, dbName, dbUser, driverClassName)) {
            log.error("Database Initializer Properties: Host=[{}], Port=[{}], DBName=[{}], User=[{}]", dbHost, dbPort, dbName, dbUser);
            throw new IllegalStateException("Database initialization failed. One or more required database properties are missing or blank. Please check your configuration (sealtalk-config.db_host, db_port, db_name, db_user, spring.datasource.driverClassName).");
        }
        String url = "jdbc:mysql://" + dbHost + ":" + dbPort + "/?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=utf8&useCompress=true&serverTimezone=UTC";
        try {
            Class.forName(driverClassName);
            log.info("Attempting to connect to database server.");
            try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
                 Statement statement = connection.createStatement()) {
                log.info("Creating database [{}] if it does not exist...", dbName);
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin");
                log.info("Database [{}] created or already exists.", dbName);
            }
        } catch (Exception e) {
            log.error("Failed to create database '{}'. Please check database connection settings and user privileges.", dbName, e);
            throw new RuntimeException("Failed to initialize database: " + dbName, e);
        }
    }
}
