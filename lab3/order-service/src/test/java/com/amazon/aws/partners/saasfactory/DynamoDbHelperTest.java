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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class DynamoDbHelperTest {

    @Test
    public void toAttributeValueMapAddress() {

    }

    @Test
    public void testToAttributeValueMapCategory() {
        Category category = new Category(1, "My Test Category");
        Map<String, AttributeValue> map = DynamoDbHelper.toAttributeValueMap(category);
        map.forEach((k, v) -> {
            System.out.println(k + " => " + v);
        });
    }

    @Test
    public void testToAttributeValueMapProduct() {
    }

    @Test
    public void testToAttributeValueMapPurchaser() {
    }

    @Test
    public void testToAttributeValueMapOrderLineItem() {
    }

    @Test
    public void testToAttributeValueMapOrder() {
    }

    @Test
    public void testOrderFromAttributeValueMapOrder() {
        System.out.println("testOrderFromAttributeValueMapOrder");
//        InputStream streamDDB = DynamoDbHelperTest.class.getResourceAsStream("/order-dynamodb.json");
//        String ddbJson = new BufferedReader(new InputStreamReader(streamDDB)).lines().collect(Collectors.joining("\n"));
//
//        InputStream stream = DynamoDbHelperTest.class.getResourceAsStream("/order.json");
//        String json = new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
//
//        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        try {
//            Order order = mapper.readValue(json, Order.class);
//            System.out.println(order);
//        } catch (IOException ioe) {
//            System.out.println(ioe.getMessage());
//        }
//
//        Order orderDDB = DynamoDbHelper.orderFromAttributeValueMap(ddbJson);

        UUID id = UUID.fromString("fe53ed46-8888-4f12-be8f-4d5ebb38553b");
        LocalDate orderDate = LocalDate.parse("2000-01-01");
        LocalDate shipDate = LocalDate.parse("2000-01-02");
        Purchaser purchaser = new Purchaser(1, "Firstname", "Lastname");
        Address shipAddress = new Address("line1", "line2", "city", "state", "zip");
        Address billAddress = new Address("bill1", "bill2", "billCity", "billState", "billZip");
        Product product = new Product(1, "sku", "productName", BigDecimal.valueOf(10.00d), new Category(1, "categoryName"));
        OrderLineItem lineItem = new OrderLineItem(1, id, product, 1, product.getPrice());
        List<OrderLineItem> lineItems = new ArrayList<>();
        lineItems.add(lineItem);

        Order expected = new Order();
        expected.setId(id);
        expected.setOrderDate(orderDate);
        expected.setShipDate(shipDate);
        expected.setPurchaser(purchaser);
        expected.setShipAddress(shipAddress);
        expected.setBillAddress(billAddress);
        expected.setLineItems(lineItems);

        Map<String, AttributeValue> item = DynamoDbHelper.toAttributeValueMap(expected);
        Order order = DynamoDbHelper.orderFromAttributeValueMap(item);

        assertEquals(expected.getId(), order.getId());
        assertEquals(expected.getOrderDate(), order.getOrderDate());
        assertEquals(expected.getShipDate(), order.getShipDate());
        assertEquals(expected.getPurchaser().getId(), order.getPurchaser().getId());
        assertEquals(expected.getPurchaser().getFirstName(), order.getPurchaser().getFirstName());
        assertEquals(expected.getPurchaser().getLastName(), order.getPurchaser().getLastName());
        assertEquals(expected.getShipAddress().getLine1(), order.getShipAddress().getLine1());
        assertEquals(expected.getShipAddress().getLine2(), order.getShipAddress().getLine2());
        assertEquals(expected.getShipAddress().getCity(), order.getShipAddress().getCity());
        assertEquals(expected.getShipAddress().getState(), order.getShipAddress().getState());
        assertEquals(expected.getShipAddress().getPostalCode(), order.getShipAddress().getPostalCode());
        assertEquals(expected.getBillAddress().getLine1(), order.getBillAddress().getLine1());
        assertEquals(expected.getBillAddress().getLine2(), order.getBillAddress().getLine2());
        assertEquals(expected.getBillAddress().getCity(), order.getBillAddress().getCity());
        assertEquals(expected.getBillAddress().getState(), order.getBillAddress().getState());
        assertEquals(expected.getBillAddress().getPostalCode(), order.getBillAddress().getPostalCode());
        assertEquals(expected.getLineItems().size(), order.getLineItems().size());
        assertEquals(expected.getLineItems().get(0).getId(), order.getLineItems().get(0).getId());
        assertEquals(expected.getLineItems().get(0).getProduct().getName(), order.getLineItems().get(0).getProduct().getName());
        assertEquals(expected.getLineItems().get(0).getQuantity(), order.getLineItems().get(0).getQuantity());
        assertEquals(expected.getLineItems().get(0).getUnitPurchasePrice(), order.getLineItems().get(0).getUnitPurchasePrice());
    }

    @Test
    public void testFromJson() {
        System.out.println("testFromJson");
        InputStream is = DynamoDbHelperTest.class.getResourceAsStream("/order.json");
        String json = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper()
                .findAndRegisterModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        try {
            Order order = mapper.readValue(json, Order.class);
            System.out.println(order);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }
}