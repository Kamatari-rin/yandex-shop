package org.example.service;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Service
public class CartMigrationService implements MigrationService  {

    @Value("${db.migrations.cart.url}")
    private String url;

    @Value("${db.migrations.cart.username}")
    private String username;

    @Value("${db.migrations.cart.password}")
    private String password;

    @Value("${db.migrations.cart.changelog}")
    private String changelogFile;

    @Override
    public void runMigrations() {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changelogFile, new ClassLoaderResourceAccessor(), database);
            liquibase.update("");
            System.out.println("Carts DB migrations applied successfully.");
        } catch (SQLException | LiquibaseException e) {
            e.printStackTrace();
            throw new RuntimeException("Error applying Carts DB migrations", e);
        }
    }
}
