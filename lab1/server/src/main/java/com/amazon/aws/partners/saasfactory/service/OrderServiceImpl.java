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
