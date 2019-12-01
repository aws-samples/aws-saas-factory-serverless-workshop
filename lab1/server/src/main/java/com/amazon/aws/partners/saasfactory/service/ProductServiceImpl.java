package com.amazon.aws.partners.saasfactory.service;

import com.amazon.aws.partners.saasfactory.domain.Category;
import com.amazon.aws.partners.saasfactory.domain.Product;
import com.amazon.aws.partners.saasfactory.repository.CategoryDao;
import com.amazon.aws.partners.saasfactory.repository.ProductDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductServiceImpl.class);

    @Autowired
    private ProductDao productDao;
    @Autowired
    private CategoryDao categoryDao;

    @Override
    public List<Product> getProducts() throws Exception {
        logger.info("ProductService::getProducts");
        StopWatch timer = new StopWatch();
        timer.start();
        List<Product> products = productDao.getProducts();
        timer.stop();
        logger.info("ProductService::getProducts exec " + timer.getTotalTimeMillis());
        return products;
    }

    @Override
    public Product getProduct(Integer productId) throws Exception {
        logger.info("ProductService::getProduct " + productId);
        StopWatch timer = new StopWatch();
        timer.start();
        Product product = productDao.getProduct(productId);
        timer.stop();
        logger.info("ProductService::getProduct " + productId + " exec " + timer.getTotalTimeMillis());
        return product;
    }

    @Override
    public Product saveProduct(Product product) throws Exception {
        Integer productId = product != null ? product.getId() : null;
        logger.info("ProductService::saveProduct " + productId);
        StopWatch timer = new StopWatch();
        timer.start();
        product = productDao.saveProduct(product);
        timer.stop();
        logger.info("ProductService::saveProduct exec " + timer.getTotalTimeMillis());
        return product;
    }

    @Override
    public Product deleteProduct(Product product) throws Exception {
        Integer productId = product != null ? product.getId() : null;
        logger.info("ProductService::deleteProduct " + productId);
        StopWatch timer = new StopWatch();
        timer.start();
        product = productDao.deleteProduct(product);
        timer.stop();
        logger.info("ProductService::deleteProduct exec " + timer.getTotalTimeMillis());
        return product;
    }

    @Override
    public List<Category> getCategories() throws Exception {
        logger.info("ProductService::getCategories");
        StopWatch timer = new StopWatch();
        timer.start();
        List<Category> categories = categoryDao.getCategories();
        timer.stop();
        logger.info("ProductService::getCategories exec " + timer.getTotalTimeMillis());
        return categories;
    }

    @Override
    public Category getCategory(Integer categoryId) throws Exception {
        logger.info("ProductService::getCategory");
        StopWatch timer = new StopWatch();
        timer.start();
        Category category = categoryDao.getCategory(categoryId);
        timer.stop();
        logger.info("ProductService::getCategory exec " + timer.getTotalTimeMillis());
        return category;
    }

    @Override
    public Category getCategoryByName(String name) throws Exception {
        logger.info("ProductService::getCategoryByName");
        StopWatch timer = new StopWatch();
        timer.start();
        Category category = categoryDao.getCategoryByName(name);
        timer.stop();
        logger.info("ProductService::getCategoryByName exec " + timer.getTotalTimeMillis());
        return category;
    }
}
