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
package com.amazon.aws.partners.saasfactory.controller;

import com.amazon.aws.partners.saasfactory.domain.Category;
import com.amazon.aws.partners.saasfactory.domain.Product;
import com.amazon.aws.partners.saasfactory.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.beans.PropertyEditorSupport;
import java.util.List;

@Controller
public class ProductsController {

	private final static Logger LOGGER = LoggerFactory.getLogger(ProductsController.class);

	@Autowired
	private ProductService productService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Category.class, new CategoryEditor());
	}

	@GetMapping("/products")
	public String getProducts(Model model) throws Exception {
		List<Category> categories = productService.getCategories();
		List<Product> products = productService.getProducts();
		model.addAttribute("categories", categories);
		model.addAttribute("products", products);
		return "products";
	}

	@PostMapping("/products")
	public String insertProduct(@ModelAttribute Product product, Model model) throws Exception {
		LOGGER.info("ProductsController::insertProduct " + product);
		productService.saveProduct(product);
		return "redirect:/products";
	}

	@PostMapping("/updateProduct")
	public String updateProduct(@ModelAttribute Product product, Model model) throws Exception {
		LOGGER.info("ProductsController::updateProduct " + product);
		productService.saveProduct(product);
		return "redirect:/products";
	}

	public String deleteProduct(@ModelAttribute Product product) throws Exception {
		LOGGER.info("ProductsController::deleteProduct " + product.getId());
		productService.deleteProduct(product);
		return "redirect:/products";
	}

	class CategoryEditor extends PropertyEditorSupport {
		@Override
		public String getAsText() {
			Category category = (Category) getValue();
			return category == null ? "" : category.getId().toString();
		}

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			Category category = null;
			try {
				//category = productService.getCategoryByName(text);
				category = productService.getCategory(Integer.valueOf(text));
			} catch (Exception e) {
				//LOGGER.error("Can't look up category by name " + text);
				LOGGER.error("Can't look up category by id " + text);
			}
			setValue(category);
		}
	}
}
