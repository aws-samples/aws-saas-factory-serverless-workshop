/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.GetParametersResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

import java.sql.*;
import java.util.*;

public class ProductServiceDAL {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProductServiceDAL.class);

    private SsmClient ssm;
    private String dbHost;
    private String dbDatabase;
    private String dbUsername;
    private String dbPassword;
    private String dbPasswordParam;
    private Connection connection;

    private final static String SELECT_PRODUCT_SQL = "SELECT p.product_id, p.sku, p.product, p.price, c.category_id, c.category " +
            "FROM product p LEFT OUTER JOIN ( " +
            "SELECT x.product_id, MAX(x.category_id) AS category_id " +
            "FROM product_categories x INNER JOIN product y ON x.product_id = y.product_id " +
            "GROUP BY x.product_id) AS pc " +
            "ON p.product_id = pc.product_id " +
            "LEFT OUTER JOIN category AS c ON pc.category_id = c.category_id " +
            "WHERE p.tenant_id = ?";
    private final static String INSERT_PRODUCT_SQL = "INSERT INTO product (tenant_id, sku, product, price) VALUES (?, ?, ?, ?)";
    private final static String UPDATE_PRODUCT_SQL = "UPDATE product SET sku = ?, product = ?, price = ? WHERE tenant_id = ? AND product_id = ?";
    private final static String DELETE_PRODUCT_SQL = "DELETE FROM product WHERE tenant_id = ? AND product_id = ?";

    private final static String SELECT_CATEGORY_SQL = "SELECT category_id, category FROM category WHERE tenant_id = ?";
    private final static String INSERT_CATEGORY_SQL = "INSERT INTO category (tenant_id, category) VALUES (?, ?)";
    private final static String UPDATE_CATEGORY_SQL = "UPDATE category SET category = ? WHERE tenant_id = ? AND category_id = ?";
    private final static String DELETE_CATEGORY_SQL = "DELETE FROM category WHERE tenant_id = ? AND category_id = ?";

    public ProductServiceDAL() {
        this.ssm = SsmClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();

        LOGGER.info("ProductServiceDAL fetching parameters from SSM");
        GetParametersResponse response = ssm.getParameters(request -> request
                .names("DB_HOST_POOL", "DB_NAME", "DB_USER", "DB_PASS")
                .build()
        );
        for (Parameter parameter : response.parameters()) {
            LOGGER.info("Processing parameter " + parameter.name());
            switch (parameter.name()) {
                case "DB_HOST_POOL":
                    this.dbHost = parameter.value();
                    break;
                case "DB_NAME":
                    this.dbDatabase = parameter.value();
                    break;
                case "DB_USER":
                    this.dbUsername = parameter.value();
                    break;
                case "DB_PASS":
                    this.dbPasswordParam = parameter.value();
                    break;
            }
        }

        // Now go fetch the encrypted database password
        LOGGER.info("ProductServiceDAL fetching password secret from SSM");
        GetParameterResponse passwordResponse = ssm.getParameter(request -> request
            .withDecryption(Boolean.TRUE)
            .name(this.dbPasswordParam)
            .build()
        );
        this.dbPassword = passwordResponse.parameter().value();

        Properties connectionProperties = new Properties();
        connectionProperties.put("user", this.dbUsername);
        connectionProperties.put("password", this.dbPassword);
        String jdbcUrl = "jdbc:postgresql://" + this.dbHost + ":5432/" + this.dbDatabase;
        LOGGER.info("JDBC Connection URL = " + jdbcUrl);
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, connectionProperties);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Product> getProducts(Map<String, Object> event) {
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        categoriesWorkaroundHack(event);

        List<Product> products = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_PRODUCT_SQL)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product product = new Product();
                product.setId(rs.getInt(1));
                product.setSku(rs.getString(2));
                product.setName(rs.getString(3));
                product.setPrice(rs.getBigDecimal(4));
                product.setCategory(new Category(rs.getInt(5), rs.getString(6)));
                products.add(product);
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return products;
    }

    public Product getProduct(Map<String, Object> event, Integer productId) {
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        Product product = null;
        String sql = SELECT_PRODUCT_SQL.concat(" AND p.product_id = ?");
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            product = new Product();
            stmt.setObject(1, tenantId);
            stmt.setInt(2, productId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                product.setId(rs.getInt(1));
                product.setSku(rs.getString(2));
                product.setName(rs.getString(3));
                product.setPrice(rs.getBigDecimal(4));
                product.setCategory(new Category(rs.getInt(5), rs.getString(6)));
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return product;
    }

    public Product updateProduct(Map<String, Object> event, Product product) {
        LoggingManager.log(event, "ProductServiceDAL::updateProduct " + product);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        Category category = product.getCategory();
        if (category != null && (category.getId() == null || category.getId() < 1)) {
            LoggingManager.log(event, "ProductServiceDAL::updateProduct inserting new category " + category);
            category = insertCategory(event, product.getCategory());
            product.setCategory(category);
        }
        if (category != null && category.getId() > 0) {
            try (PreparedStatement stmt1 = connection.prepareStatement("SELECT NOT EXISTS (SELECT * FROM product_categories WHERE product_id = ? AND category_id = ?)")) {
                stmt1.setInt(1, product.getId());
                stmt1.setInt(2, category.getId());
                boolean insertProductCategory = false;
                ResultSet rs = stmt1.executeQuery();
                if (rs.next()) {
                    insertProductCategory = rs.getBoolean(1);
                }
                if (insertProductCategory) {
                    LoggingManager.log(event, "ProductServiceDAL::updateProduct inserting product category mapping " + product.getId() + ", " + category.getId());
                    try (PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)")) {
                        stmt2.setInt(1, product.getId());
                        stmt2.setInt(2, category.getId());
                        int insertedRows = stmt2.executeUpdate();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_PRODUCT_SQL)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, product.getSku());
            stmt.setString(3, product.getName());
            stmt.setBigDecimal(4, product.getPrice());
            stmt.setInt(5, product.getId());
            int affected = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return product;
    }

    public Product insertProduct(Map<String, Object> event, Product product) {
        LoggingManager.log(event, "ProductServiceDAL::insertProduct " + product);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        Category category = product.getCategory();
        if (category != null && category.getName() != null && !category.getName().isEmpty()) {
            if (category.getId() == null || category.getId() < 1) {
                category = insertCategory(event, category);
            } else {
                category = updateCategory(event, category);
            }
            product.setCategory(category);
        }
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_PRODUCT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, product.getSku());
            stmt.setString(3, product.getName());
            stmt.setBigDecimal(4, product.getPrice());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                product.setId(rs.getInt("product_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (category != null && category.getName() != null && !category.getName().isEmpty()) {
            LoggingManager.log(event, "ProductServiceDAL::insertProduct inserting product category mapping " + product.getId() + ", " + category.getId());
            try (PreparedStatement stmt2 = connection.prepareStatement("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)")) {
                stmt2.setInt(1, product.getId());
                stmt2.setInt(2, category.getId());
                int insertedRows = stmt2.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        return product;
    }

    public Product deleteProduct(Map<String, Object> event, Product product) {
        LoggingManager.log(event, "ProductServiceDAL::deleteProduct " + product);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_PRODUCT_SQL)) {
            stmt.setObject(1, tenantId);
            stmt.setInt(2, product.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows != 1) {
                throw new RuntimeException("Delete failed for product " + product.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return product;
    }

    public List<Category> getCategories(Map<String, Object> event) {
        categoriesWorkaroundHack(event);

        LoggingManager.log(event, "ProductServiceDAL::getCategories");
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));

        List<Category> categories = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_CATEGORY_SQL)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Category category = new Category();
                category.setId(rs.getInt(1));
                category.setName(rs.getString(2));
                categories.add(category);
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return categories;
    }

    public Category getCategory(Map<String, Object> event, Integer categoryId) {
        LoggingManager.log(event, "ProductServiceDAL::getCategory");
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));

        Category category = null;
        String sql = SELECT_CATEGORY_SQL.concat(" AND category_id = ?");
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            category = new Category();
            stmt.setObject(1, tenantId);
            stmt.setInt(2, categoryId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                category.setId(rs.getInt(1));
                category.setName(rs.getString(2));
            }
            rs.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return category;
    }

    public Category updateCategory(Map<String, Object> event, Category category) {
        LoggingManager.log(event, "ProductServiceDAL::updateCategory " + category);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));

        try (PreparedStatement stmt = connection.prepareStatement(UPDATE_CATEGORY_SQL)) {
            stmt.setString(1, category.getName());
            stmt.setObject(2, tenantId);
            stmt.setInt(3, category.getId());
            int affected = stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return category;
    }

    public Category insertCategory(Map<String, Object> event, Category category) {
        LoggingManager.log(event, "ProductServiceDAL::insertCategory " + category);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));

        try (PreparedStatement stmt = connection.prepareStatement(INSERT_CATEGORY_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, category.getName());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            while (rs.next()) {
                category.setId(rs.getInt("category_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return category;
    }

    public Category deleteCategory(Map<String, Object> event, Category category) {
        LoggingManager.log(event, "ProductServiceDAL::deleteCategory " + category);
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_CATEGORY_SQL)) {
            stmt.setObject(1, tenantId);
            stmt.setInt(2, category.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows != 1) {
                throw new RuntimeException("Delete failed for category " + category.getId());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return category;
    }

    public void categoriesWorkaroundHack(Map<String, Object> event) {
        UUID tenantId = UUID.fromString(new TokenManager().getTenantId(event));
        try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM category WHERE tenant_id = ?")) {
            stmt.setObject(1, tenantId);
            int tenantCategories = 0;
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                tenantCategories = rs.getInt(1);
            }
            LOGGER.info("Tenant " + tenantId + " has " + tenantCategories + " categories");
            if (tenantCategories < 1) {
                LOGGER.info("Adding default categories for tenant " + tenantId);
                PreparedStatement defaultCategories = connection.prepareStatement(INSERT_CATEGORY_SQL);
                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "JavaScript");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "Python");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "Java");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "C#");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "PHP");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "Swift");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "Ruby");
                defaultCategories.addBatch();

                defaultCategories.setObject(1, tenantId);
                defaultCategories.setString(2, "Golang");
                defaultCategories.addBatch();

                defaultCategories.executeBatch();
                defaultCategories.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
