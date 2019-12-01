package com.amazon.aws.partners.saasfactory.service;

import com.amazon.aws.partners.saasfactory.domain.Order;

import java.util.List;

public interface OrderService {

    public List<Order> getOrders() throws Exception;

    public Order getOrder(Integer orderId) throws Exception;

    public Order saveOrder(Order order) throws Exception;

    public Order deleteOrder(Order order) throws Exception;
}
