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
package com.amazon.aws.partners.saasfactory.repository;

import com.amazon.aws.partners.saasfactory.domain.Category;
import com.amazon.aws.partners.saasfactory.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

@Repository
public class ProductDaoImpl implements ProductDao {

    private final static Logger logger = LoggerFactory.getLogger(ProductDaoImpl.class);
    private final static String SELECT_PRODUCT_SQL = "SELECT p.product_id, p.sku, p.product, p.price, c.category_id, c.category " +
            "FROM product p LEFT OUTER JOIN ( " +
            "SELECT x.product_id, MAX(x.category_id) AS category_id " +
            "FROM product_categories x INNER JOIN product y ON x.product_id = y.product_id " +
            "GROUP BY x.product_id) AS pc " +
            "ON p.product_id = pc.product_id " +
            "LEFT OUTER JOIN category AS c ON pc.category_id = c.category_id";
    private final static String INSERT_PRODUCT_SQL = "INSERT INTO product (sku, product, price) VALUES (?, ?, ?)";
    private final static String UPDATE_PRODUCT_SQL = "UPDATE product SET sku = ?, product = ?, price = ? WHERE product_id = ?";
    private final static String DELETE_PRODUCT_SQL = "DELETE FROM product WHERE product_id = ?";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private CategoryDao categoryDao;

    @Override
    public Product getProduct(Integer productId) throws Exception {
        logger.info("ProductDao::getProduct " + productId);
        //String sql = "SELECT p.product_id, p.sku, p.product, p.price FROM product p WHERE p.product_id = ?";
        String sql = SELECT_PRODUCT_SQL.concat(" WHERE p.product_id = ?");
        return jdbc.queryForObject(sql, new Object[]{productId}, new ProductRowMapper());
    }

    @Override
    public List<Product> getProducts() throws Exception {
        logger.info("ProductDao::getProducts");
        List<Product> products = jdbc.query(SELECT_PRODUCT_SQL, new ProductRowMapper());
        if (products == null) {
            products = Collections.emptyList();
        }
        logger.info("ProductDao::getProducts returning " + products.size() + " products");
        return products;
    }

    @Override
    public Product saveProduct(Product product) throws Exception {
        logger.info("ProductDao::saveProduct " + product);
        if (product.getId() != null && product.getId() > 0) {
            return updateProduct(product);
        } else {
            return insertProduct(product);
        }
    }

    private Product insertProduct(Product product) throws Exception {
        logger.info("ProductDao::insertProduct " + product);
        Category category = product.getCategory();
        if (category != null) {
            category = categoryDao.saveCategory(category);
            product.setCategory(category);
        }
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_PRODUCT_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, product.getSku());
            ps.setString(2, product.getName());
            ps.setBigDecimal(3, product.getPrice());
            return ps;
        }, keyHolder);
        if (!keyHolder.getKeys().isEmpty()) {
            product.setId((Integer) keyHolder.getKeys().get("product_id"));
        } else {
            product.setId(keyHolder.getKey().intValue());
        }
        if (category != null) {
            jdbc.update("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)", new Object[]{product.getId(), product.getCategory().getId()});
        }
        return product;
    }

    private Product updateProduct(Product product) throws Exception {
        logger.info("ProductDao::updateProduct " + product);
        if (product.getCategory() != null) {
            Category category = categoryDao.saveCategory(product.getCategory());
            product.setCategory(category);
            Boolean insertProductCategory = jdbc.queryForObject("SELECT NOT EXISTS (SELECT * FROM product_categories WHERE product_id = ? AND category_id = ?)", new Object[]{product.getId(), category.getId()}, Boolean.class);
            if (insertProductCategory) {
                logger.info("ProductDao::updateProduct inserting product category mapping " + product.getId() + ", " + category.getId());
                jdbc.update("INSERT INTO product_categories (product_id, category_id) VALUES (?, ?)", new Object[]{product.getId(), category.getId()});
            }
        }
        jdbc.update(UPDATE_PRODUCT_SQL, new Object[]{product.getSku(), product.getName(), product.getPrice(), product.getId()});
        return product;
    }

    @Override
    public Product deleteProduct(Product product) throws Exception {
        logger.info("ProductDao::deleteProduct " + product);
        int affectedRows = jdbc.update(DELETE_PRODUCT_SQL, new Object[]{product.getId()});
        if (affectedRows != 1) {
            throw new RuntimeException("Delete failed for product " + product.getId());
        }
        return product;
    }

    class ProductRowMapper  implements RowMapper<Product> {
        @Override
        public Product mapRow(ResultSet result, int rowNumber) throws SQLException {
            Product product = new Product(result.getInt("product_id"), result.getString("sku"), result.getString("product"), result.getBigDecimal("price"));
            Category category = new Category(result.getInt("category_id"), result.getString("category"));
            product.setCategory(category);
            return product;
        }
    }
}
