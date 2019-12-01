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
package com.amazon.aws.partners.saasfactory.service;

import com.amazon.aws.partners.saasfactory.domain.Order;
import com.amazon.aws.partners.saasfactory.repository.OrderDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired
    private OrderDao orderDao;

    @Override
    public List<Order> getOrders() throws Exception {
        logger.info("OrderService::getOrders");
        StopWatch timer = new StopWatch();
        timer.start();
        List<Order> orders = orderDao.getOrders();
        timer.stop();
        logger.info("OrderService::getOrders exec " + timer.getTotalTimeMillis());
        return orders;
    }

    @Override
    public Order getOrder(Integer orderId) throws Exception {
        logger.info("OrderService::getOrder " + orderId);
        StopWatch timer = new StopWatch();
        timer.start();
        Order order = orderDao.getOrder(orderId);
        timer.stop();
        logger.info("OrderService::getOrder exec " + timer.getTotalTimeMillis());
        return order;
    }

    @Override
    public Order saveOrder(Order order) throws Exception {
        Integer orderId = order != null ? order.getId() : null;
        logger.info("OrderService::saveOrder " + orderId);
        StopWatch timer = new StopWatch();
        timer.start();
        order = orderDao.saveOrder(order);
        timer.stop();
        logger.info("OrderService::saveOrder exec " + timer.getTotalTimeMillis());
        return order;
    }

    @Override
    public Order deleteOrder(Order order) throws Exception {
        Integer orderId = order != null ? order.getId() : null;
        logger.info("OrderService::deleteOrder " + orderId);
        StopWatch timer = new StopWatch();
        timer.start();
        order = orderDao.deleteOrder(order);
        timer.stop();
        logger.info("OrderService::deleteOrder exec " + timer.getTotalTimeMillis());
        return order;
    }
}
