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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class Order  implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private LocalDate orderDate;
    private LocalDate shipDate;
    private Purchaser purchaser;
    private Address shipAddress;
    private Address billAddress;
    private List<OrderLineItem> lineItems = new ArrayList<>();

    public Order() {
        this(null, null, null, null, null, null, null);
    }

    public Order(UUID id, LocalDate orderDate, LocalDate shipDate, Purchaser purchaser, Address shipAddress, Address billAddress, List<OrderLineItem> lineItems) {
        this.id = id;
        this.orderDate = orderDate;
        this.shipDate = shipDate;
        this.purchaser = purchaser;
        this.shipAddress = shipAddress;
        this.billAddress = billAddress;
        this.lineItems = lineItems != null ? lineItems : new ArrayList<>();
    }

    public BigDecimal getTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for(OrderLineItem lineItem : getLineItems()) {
            total = total.add(lineItem.getExtendedPurchasePrice());
        }
        return total;
    }

    public Order(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
        if (getLineItems() != null && !getLineItems().isEmpty()) {
            getLineItems().forEach(lineItem -> lineItem.setOrderId(id));
        }
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public LocalDate getShipDate() {
        return shipDate;
    }

    public void setShipDate(LocalDate shipDate) {
        this.shipDate = shipDate;
    }

    public Purchaser getPurchaser() {
        return purchaser;
    }

    public void setPurchaser(Purchaser purchaser) {
        this.purchaser = purchaser;
    }

    public Address getShipAddress() {
        return shipAddress;
    }

    public void setShipAddress(Address shipAddress) {
        this.shipAddress = shipAddress;
    }

    public Address getBillAddress() {
        return billAddress;
    }

    public void setBillAddress(Address billAddress) {
        this.billAddress = billAddress;
    }

    public List<OrderLineItem> getLineItems() {
        return lineItems;
    }

    public void setLineItems(List<OrderLineItem> lineItems) {
        this.lineItems = lineItems != null ? lineItems : new ArrayList<OrderLineItem>();
    }
}
