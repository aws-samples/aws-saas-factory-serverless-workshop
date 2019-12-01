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

import java.util.Date;
import java.util.List;

import com.amazon.aws.partners.saasfactory.domain.Order;
import com.amazon.aws.partners.saasfactory.domain.OrderLineItem;
import com.amazon.aws.partners.saasfactory.domain.Product;
import com.amazon.aws.partners.saasfactory.service.OrderService;
import com.amazon.aws.partners.saasfactory.service.ProductService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class OrdersController {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(OrdersController.class);
	
	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductService productService;

	@GetMapping("/orders")
	public String orders(Model model) throws Exception {
		List<Order> orders = orderService.getOrders();
		List<Product> products = productService.getProducts();

		model.addAttribute("orders", orders);
		model.addAttribute("products", products);

		return "orders";
	}
	
	@PostMapping("/orders")
	public String insertOrder(@ModelAttribute Order order, Model model) throws Exception {
		LOGGER.info("OrdersController::insertOrder " + order);

		order.setOrderDate(new Date());
		order.setBillAddress(order.getShipAddress());
		
		order.getLineItems().removeIf(item -> item == null || 0 == item.getQuantity());
		order.getLineItems().forEach(item -> {
			try {
				item.setUnitPurchasePrice(productService.getProduct(item.getProduct().getId()).getPrice());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		orderService.saveOrder(order);
		
		return "redirect:/orders";
	}
	
	@PostMapping("/updateOrder")
	public String updateOrder(@ModelAttribute Order order, Model model) throws Exception {
		LOGGER.info("OrdersController::updateOrder " + order);

		order.setBillAddress(order.getShipAddress());

		order.getLineItems().removeIf(item -> item == null || 0 == item.getQuantity());
		order.getLineItems().forEach(item -> {
			try {
				item.setUnitPurchasePrice(productService.getProduct(item.getProduct().getId()).getPrice());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		orderService.saveOrder(order);
		
		return "redirect:/orders";
	}
	
	@PostMapping("/deleteOrder")
	public String deleteOrder(@ModelAttribute Order order) throws Exception {
		LOGGER.info("OrdersController::deleteOrder " + order.getId());
		
		orderService.deleteOrder(order);
		
		return "redirect:/orders";
	}
}