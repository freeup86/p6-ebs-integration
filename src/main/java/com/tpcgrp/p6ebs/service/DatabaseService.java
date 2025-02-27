package com.tpcgrp.p6ebs.service;

import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class DatabaseService {

    public boolean testP6Connection(String server, String database, String username, String password) {
        String url = String.format("jdbc:sqlserver://%s;databaseName=%s;encrypt=true;trustServerCertificate=true",
                server, database);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean testEbsConnection(String server, String sid, String username, String password) {
        String url = String.format("jdbc:oracle:thin:@%s:1521:%s", server, sid);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}