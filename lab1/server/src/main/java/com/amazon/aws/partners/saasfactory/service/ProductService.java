package com.amazon.aws.partners.saasfactory.service;

import com.amazon.aws.partners.saasfactory.domain.Category;
import com.amazon.aws.partners.saasfactory.domain.Product;

import java.util.List;

public interface ProductService {

    public List<Product> getProducts() throws Exception;

    public Product getProduct(Integer productId) throws Exception;

    public Product saveProduct(Product product) throws Exception;

    public Product deleteProduct(Product product) throws Exception;

    public List<Category> getCategories() throws Exception;

    public Category getCategory(Integer categoryId) throws Exception;

    public Category getCategoryByName(String name) throws Exception;
}
