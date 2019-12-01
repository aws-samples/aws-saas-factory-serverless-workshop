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
package com.amazon.aws.partners.saasfactory;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class DynamoDbHelper {

    public static Map<String, AttributeValue> toAttributeValueMap(Address address) {
        Map<String, AttributeValue> map = new HashMap<>();
        if (address.getLine1() != null && !address.getLine1().isEmpty()) {
            map.put("line1", AttributeValue.builder().s(address.getLine1().toString()).build());
        }
        if (address.getLine2() != null && !address.getLine2().isEmpty()) {
            map.put("line2", AttributeValue.builder().s(address.getLine2()).build());
        }
        if (address.getCity() != null && !address.getCity().isEmpty()) {
            map.put("city", AttributeValue.builder().s(address.getCity()).build());
        }
        if (address.getState() != null && !address.getState().isEmpty()) {
            map.put("state", AttributeValue.builder().s(address.getState()).build());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isEmpty()) {
            map.put("postalCode", AttributeValue.builder().s(address.getPostalCode()).build());
        }
        return map;
    }

    public static Map<String, AttributeValue> toAttributeValueMap(Category category) {
        Map<String, AttributeValue> map = new HashMap<>();
        if (category.getId() != null) {
            map.put("id", AttributeValue.builder().s(category.getId().toString()).build());
        }
        if (category.getName() != null && !category.getName().isEmpty()) {
            map.put("name", AttributeValue.builder().s(category.getName()).build());
        }
        return map;
    }

    public static Map<String, AttributeValue> toAttributeValueMap(Product product) {
        Map<String, AttributeValue> map = new HashMap<>();
        if (product.getId() != null) {
            map.put("id", AttributeValue.builder().s(product.getId().toString()).build());
        }
        if (product.getSku() != null && !product.getSku().isEmpty()) {
            map.put("sku", AttributeValue.builder().s(product.getSku()).build());
        }
        if (product.getName() != null && !product.getName().isEmpty()) {
            map.put("name", AttributeValue.builder().s(product.getName()).build());
        }
        if (product.getPrice() != null) {
            map.put("price", AttributeValue.builder().n(product.getPrice().toString()).build());
        }
        if (product.getCategory() != null) {
            map.put("category", AttributeValue.builder().m(toAttributeValueMap(product.getCategory())).build());
        }
        return map;
    }

    public static Map<String, AttributeValue> toAttributeValueMap(Purchaser purchaser) {
        Map<String, AttributeValue> map = new HashMap<>();
        if (purchaser.getId() != null) {
            map.put("id", AttributeValue.builder().s(purchaser.getId().toString()).build());
        }
        if (purchaser.getFirstName() != null && !purchaser.getFirstName().isEmpty()) {
            map.put("firstName", AttributeValue.builder().s(purchaser.getFirstName()).build());
        }
        if (purchaser.getLastName() != null && !purchaser.getLastName().isEmpty()) {
            map.put("lastName", AttributeValue.builder().s(purchaser.getLastName()).build());
        }
        return map;
    }

    public static Map<String, AttributeValue> toAttributeValueMap(OrderLineItem lineItem) {
        Map<String, AttributeValue> map = new HashMap<>();
        if (lineItem.getId() != null) {
            map.put("id", AttributeValue.builder().s(lineItem.getId().toString()).build());
        }
        if (lineItem.getOrderId() != null) {
            map.put("orderId", AttributeValue.builder().s(lineItem.getOrderId().toString()).build());
        }
        if (lineItem.getProduct() != null) {
            map.put("product", AttributeValue.builder().m(toAttributeValueMap(lineItem.getProduct())).build());
        }
        if (lineItem.getQuantity() != null) {
            map.put("quantity", AttributeValue.builder().n(lineItem.getQuantity().toString()).build());
        }
        if (lineItem.getUnitPurchasePrice() != null) {
            map.put("unitPurchasePrice", AttributeValue.builder().n(lineItem.getUnitPurchasePrice().toPlainString()).build());
        }
        if (lineItem.getExtendedPurchasePrice() != null) {
            map.put("extendedPurchasePrice", AttributeValue.builder().n(lineItem.getExtendedPurchasePrice().toPlainString()).build());
        }
        return map;
    }

    public static Map<String, AttributeValue> toAttributeValueMap(Order order) {
        Map<String, AttributeValue> map = new HashMap<>();
        map.put("id", AttributeValue.builder().s(order.getId().toString()).build());
        LocalDate orderDate = order.getOrderDate();
        if (orderDate != null) {
            map.put("orderDate", AttributeValue.builder().s(orderDate.toString()).build());
        }
        LocalDate shipDate = order.getShipDate();
        if (shipDate != null) {
            map.put("shipDate", AttributeValue.builder().s(shipDate.toString()).build());
        }
        Purchaser purchaser = order.getPurchaser();
        if (purchaser != null) {
            map.put("purchaser", AttributeValue.builder().m(toAttributeValueMap(purchaser)).build());
        }
        Address shipAddress = order.getShipAddress();
        if (shipAddress != null) {
            map.put("shipAddress", AttributeValue.builder().m(toAttributeValueMap(shipAddress)).build());
        }
        Address billAddress = order.getBillAddress();
        if (billAddress != null) {
            map.put("billAddress", AttributeValue.builder().m(toAttributeValueMap(billAddress)).build());
        }
        List<AttributeValue> lineItems = new ArrayList<>();
        order.getLineItems().forEach(lineItem -> {
            lineItems.add(AttributeValue.builder().m(toAttributeValueMap(lineItem)).build());
        });
        if (!lineItems.isEmpty()) {
            map.put("lineItems", AttributeValue.builder().l(lineItems).build());
        }
        map.put("total", AttributeValue.builder().n(order.getTotal().toPlainString()).build());
        return map;
    }

    public static Purchaser purchaserFromAttributeValueMap(Map<String, AttributeValue> item) {
        Purchaser purchaser = null;
        if (item != null) {
            purchaser = new Purchaser();
            if (item.containsKey("id")) {
                purchaser.setId(Integer.valueOf(item.get("id").s()));
            }
            if (item.containsKey("firstName")) {
                purchaser.setFirstName(item.get("firstName").s());
            }
            if (item.containsKey("lastName")) {
                purchaser.setLastName(item.get("lastName").s());
            }
        }
        return purchaser;
    }

    public static Address addressFromAttributeValueMap(Map<String, AttributeValue> item) {
        Address address = null;
        if (item != null) {
            address = new Address();
            if (item.containsKey("line1")) {
                address.setLine1(item.get("line1").s());
            }
            if (item.containsKey("line2")) {
                address.setLine2(item.get("line2").s());
            }
            if (item.containsKey("city")) {
                address.setCity(item.get("city").s());
            }
            if (item.containsKey("state")) {
                address.setState(item.get("state").s());
            }
            if (item.containsKey("postalCode")) {
                address.setPostalCode(item.get("postalCode").s());
            }
        }
        return address;
    }

    public static Category categoryFromAttrbuteValueMap(Map<String, AttributeValue> item) {
        Category category = null;
        if (item != null) {
            category = new Category();
            if (item.containsKey("id")) {
                category.setId(Integer.valueOf(item.get("id").s()));
            }
            if (item.containsKey("name")) {
                category.setName(item.get("name").s());
            }
        }
        return category;
    }

    public static Product productFromAttributeValueMap(Map<String, AttributeValue> item) {
        Product product = null;
        if (item != null) {
            product = new Product();
            if (item.containsKey("id")) {
                product.setId(Integer.valueOf(item.get("id").s()));
            }
            if (item.containsKey("sku")) {
                product.setSku(item.get("sku").s());
            }
            if (item.containsKey("name")) {
                product.setName(item.get("name").s());
            }
            if (item.containsKey("price")) {
                product.setPrice(new BigDecimal(item.get("price").n()));
            }
            if (item.containsKey("category")) {
                product.setCategory(categoryFromAttrbuteValueMap(item.get("category").m()));
            }
        }
        return product;
    }

    public static OrderLineItem lineItemFromAttributeValueMap(Map<String, AttributeValue> item) {
        OrderLineItem lineItem = null;
        if (item != null) {
            lineItem = new OrderLineItem();
            if (item.containsKey("id")) {
                lineItem.setId(Integer.valueOf(item.get("id").s()));
            }
            if (item.containsKey("orderId")) {
                lineItem.setOrderId(UUID.fromString(item.get("orderId").s()));
            }
            if (item.containsKey("product")) {
                lineItem.setProduct(productFromAttributeValueMap(item.get("product").m()));
            }
            if (item.containsKey("quantity")) {
                lineItem.setQuantity(Integer.valueOf(item.get("quantity").n()));
            }
            if (item.containsKey("unitPurchasePrice")) {
                lineItem.setUnitPurchasePrice(new BigDecimal(item.get("unitPurchasePrice").n()));
            }
        }
        return lineItem;
    }

    public static Order orderFromAttributeValueMap(Map<String, AttributeValue> item) {
        Order order = null;
        if (item != null) {
            order = new Order();
            if (item.containsKey("id")) {
                order.setId(UUID.fromString(item.get("id").s()));
            }
            if (item.containsKey("orderDate")) {
                LocalDate orderDate = LocalDate.parse(item.get("orderDate").s());
                order.setOrderDate(orderDate);
            }
            if (item.containsKey("shipDate")) {
                LocalDate shipDate = LocalDate.parse(item.get("shipDate").s());
                order.setShipDate(shipDate);
            }
            if (item.containsKey("purchaser")) {
                order.setPurchaser(purchaserFromAttributeValueMap(item.get("purchaser").m()));
            }
            if (item.containsKey("shipAddress")) {
                order.setShipAddress(addressFromAttributeValueMap(item.get("shipAddress").m()));
            }
            if (item.containsKey("billAddress")) {
                order.setBillAddress(addressFromAttributeValueMap(item.get("billAddress").m()));
            }
            if (item.containsKey("lineItems")) {
                ArrayList<OrderLineItem> lineItems = new ArrayList<>();
                item.get("lineItems").l().forEach(attributeValue ->
                        lineItems.add(lineItemFromAttributeValueMap(attributeValue.m()))
                );
                order.setLineItems(lineItems);
            }
        }
        return order;
    }
}
