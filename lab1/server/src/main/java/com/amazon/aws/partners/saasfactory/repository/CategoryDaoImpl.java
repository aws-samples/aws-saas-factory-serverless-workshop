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
public class CategoryDaoImpl implements CategoryDao {

    private final static Logger logger = LoggerFactory.getLogger(CategoryDaoImpl.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Override
    public Category getCategory(Integer categoryId) throws Exception {
        logger.info("CategoryDao::getCategory " + categoryId);
        return jdbc.queryForObject("SELECT category_id, category FROM category WHERE category_id = ?", new Object[]{categoryId}, new CategoryRowMapper());
    }

    @Override
    public Category getCategoryByName(String name) throws Exception {
        logger.info("CategoryDao::getCategoryByName " + name);
        return jdbc.queryForObject("SELECT category_id, category FROM category WHERE category = ?", new Object[]{name}, new CategoryRowMapper());
    }

    @Override
    public List<Category> getCategories() throws Exception {
        logger.info("CategoryDao::getCategories");
        List<Category> categories = jdbc.query("SELECT category_id, category FROM category", new CategoryRowMapper());
        if (categories == null) {
            categories = Collections.emptyList();
        }
        logger.info("CategoryDao::getCategories returning " + categories.size() + " categories");
        return categories;
    }

    @Override
    public Category saveCategory(Category category) throws Exception {
        logger.info("CategoryDao::saveCategory " + category);
        if (category.getId() != null && category.getId() > 0) {
            return updateCategory(category);
        } else {
            return insertCategory(category);
        }
    }

    private Category insertCategory(Category category) throws Exception {
        logger.info("CategoryDao::insertCategory " + category);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO category (category) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            ps.setString(1, category.getName());
            return ps;
        }, keyHolder);
        if (!keyHolder.getKeys().isEmpty()) {
            category.setId((Integer) keyHolder.getKeys().get("category_id"));
        } else {
            category.setId(keyHolder.getKey().intValue());
        }
        return category;
    }

    private Category updateCategory(Category category) throws Exception {
        logger.info("CategoryDao::updateCategory " + category);
        jdbc.update("UPDATE category SET category = ? WHERE category_id = ?", new Object[]{category.getName(), category.getId()});
        return category;
    }

    @Override
    public Category deleteCategory(Category category) throws Exception {
        logger.info("CategoryDao::deleteCategory " + category);
        int affectedRows = jdbc.update("DELETE FROM category WHERE category_id = ?", new Object[]{category.getId()});
        if (affectedRows != 1) {
            throw new RuntimeException("Delete failed for category " + category.getId());
        }
        return category;
    }

    class CategoryRowMapper implements RowMapper<Category> {
        @Override
        public Category mapRow(ResultSet result, int rowMapper) throws SQLException {
            return new Category(result.getInt("category_id"), result.getString("category"));
        }
    }
}
