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
package com.amazon.aws.partners.saasfactory.api;

import com.amazon.aws.partners.saasfactory.domain.Category;
import com.amazon.aws.partners.saasfactory.domain.Product;
import com.amazon.aws.partners.saasfactory.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping(path="/api")
public class Products {

	private static final Logger logger = LoggerFactory.getLogger(Products.class);

	@Autowired
	private ProductService productService;

	@CrossOrigin
	@GetMapping(path = "/products/{id}")
	public Product getProduct(@PathVariable Integer id) throws Exception {
		logger.info("Products::getProduct id = " + id);
		return productService.getProduct(id);
	}

	@CrossOrigin
	@GetMapping(path = "/products")
	public List<Product> getProducts() throws Exception {
		logger.info("Products::getProducts");
		return productService.getProducts();
	}

	@CrossOrigin
	@PutMapping(path = "/products/{id}")
	public Product updateProduct(@PathVariable Integer id, @RequestBody Product product) throws Exception {
		logger.info("Products::updateProduct id = " + id + ", product = " + product);
		if (id == null || product == null || !id.equals(product.getId())) {
			logger.error("Products::updateProduct path variable doesn't equal request body. Throwing HTTP 400.");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		return productService.saveProduct(product);
	}

	@CrossOrigin
	@PostMapping(path = "/products")
	public Product insertProduct(@RequestBody Product product) throws Exception {
		logger.info("Products::insertProduct product = " + product);
		if (product == null) {
			logger.error("Products::insertProduct request body is empty. Throwing HTTP 400.");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		return productService.saveProduct(product);
	}

	@CrossOrigin
	@DeleteMapping(path = "/products/{id}")
	public Product deleteProduct(@PathVariable Integer id, @RequestBody Product product) throws Exception {
		logger.info("Products::deleteProduct id = " + id + ", product = " + product);
		if (id == null || product == null || !id.equals(product.getId())) {
			logger.error("Products::deleteProduct path variable doesn't equal request body. Throwing HTTP 400.");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		return productService.deleteProduct(product);
	}

    @CrossOrigin
    @GetMapping(path = "/categories")
    public List<Category> getCategories() throws Exception {
        logger.info("Products::getCategories");
        return productService.getCategories();
    }
}
