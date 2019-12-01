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
import React, { useState } from 'react';
import { connect } from 'react-redux';
import moment from 'moment';
import {
    closeModal,
} from '../actions';
import {
    editOrder,
} from '../../orders/actions';
import Modal from 'react-bootstrap/Modal';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';

function EditProductModal(props) {
    const {
        show,
        order,
        orderId = order.id,
        purchaserId = order.purchaser.id,
        products,
        title = order ? `Edit Order ${order.id}` : 'Edit Order',
        buttonText = 'Edit Order',
        editOrder,
        closeModal,
    } = props;

    const useInput = initialValue => {
        const [value, setValue] = useState(initialValue);

        return {
            value,
            setValue,
            reset: () => setValue(''),
            bind: {
                value,
                onChange: event => {
                    setValue(event.target.value);
                }
            },
        };
    };

    let initialQuantity1;
    if(products[0]) {
        initialQuantity1 = order.lineItems.find(lineItem => products[0].id === lineItem.product.id);
    }

    let initialQuantity2;
    if(products[1]) {
        initialQuantity2 = order.lineItems.find(lineItem => products[1].id === lineItem.product.id);
    }

    let initialQuantity3;
    if(products[2]) {
        initialQuantity3 = order.lineItems.find(lineItem => products[2].id === lineItem.product.id);
    }

    const { value: firstName, bind: bindFirstName } = useInput(order.purchaser.firstName);
    const { value: lastName, bind: bindLastName } = useInput(order.purchaser.lastName);
    const { value: address1, bind: bindAddress1 } = useInput(order.shipAddress.line1);
    const { value: address2, bind: bindAddress2 } = useInput(order.shipAddress.line2);
    const { value: city, bind: bindCity } = useInput(order.shipAddress.city);
    const { value: state, bind: bindState } = useInput(order.shipAddress.state);
    const { value: zipCode, bind: bindZipCode } = useInput(order.shipAddress.postalCode);
    const { value: quantity1, bind: bindQuantity1 } = useInput(initialQuantity1 ? initialQuantity1.quantity : '');
    const { value: quantity2, bind: bindQuantity2 } = useInput(initialQuantity2 ? initialQuantity2.quantity : '');
    const { value: quantity3, bind: bindQuantity3 } = useInput(initialQuantity3 ? initialQuantity3.quantity : '');

    const submitOrderUpdate = () => {
        const address = {
            line1: address1,
            line2: address2,
            city: city,
            state: state,
            postalCode: zipCode,
        };
        const lineItems = [];

        if(quantity1) {
            lineItems.push(
                {
                    product: {
                        id: products[0].id,
                        sku: products[0].sku,
                        name: products[0].name,
                        price: products[0].price,
                        category: {
                            id: products[0].category.id,
                            name: products[0].category.name,
                        }
                    },
                    quantity: quantity1,
                    unitPurchasePrice: products[0].price,
                    extendedPurchasePrice: products[0].price * quantity1,
                }
            );
        }

        if(quantity2) {
            lineItems.push(
                {
                    product: {
                        id: products[1].id,
                        sku: products[1].sku,
                        name: products[1].name,
                        price: products[1].price,
                        category: {
                            id: products[1].category.id,
                            name: products[1].category.name,
                        }
                    },
                    quantity: quantity2,
                    unitPurchasePrice: products[1].price,
                    extendedPurchasePrice: products[1].price * quantity2,
                }
            );
        }

        if(quantity3) {
            lineItems.push(
                {
                    product: {
                        id: products[2].id,
                        sku: products[2].sku,
                        name: products[2].name,
                        price: products[2].price,
                        category: {
                            id: products[2].category.id,
                            name: products[2].category.name,
                        }
                    },
                    quantity: quantity3,
                    unitPurchasePrice: products[2].price,
                    extendedPurchasePrice: products[2].price * quantity3,
                }
            );
        }

        const order = {
            id: orderId,
            orderDate: moment().format('YYYY-MM-DD'),
            purchaser: {
                firstName,
                lastName,
                id: purchaserId,
            },
            shipAddress: address,
            billAddress: address,
            lineItems,
            total: lineItems.reduce((acc, cur) => acc + cur.extendedPurchasePrice, 0).toFixed(2),
        };

        editOrder(order);
    };
    
    return (
        <>
            <Modal
                show={show}
                onHide={closeModal}
                centered
            >
                <Modal.Header closeButton>
                    <Modal.Title>{title}</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                <Form>
                        <h4>Purchaser</h4>
                        <Form.Row>
                            <Form.Group as={Col} controlId="purchaserFirstName">
                                <Form.Label>First Name</Form.Label>
                                <Form.Control type="text" {...bindFirstName} />
                            </Form.Group>
                            <Form.Group as={Col} controlId="purchaserLastName">
                                <Form.Label>Last Name</Form.Label>
                                <Form.Control type="text" placeholder="Enter Last Name" {...bindLastName} />
                            </Form.Group>
                        </Form.Row>
                        <h4>Shipping Information</h4>
                        <Form.Row>
                            <Form.Group as={Col} controlId="shippingAddress1">
                                <Form.Label>Address</Form.Label>
                                <Form.Control type="text" placeholder="Enter Address" {...bindAddress1} />
                            </Form.Group>
                        </Form.Row>
                        <Form.Row>
                            <Form.Group as={Col} controlId="shippingAddress2">
                                <Form.Label>Address 2</Form.Label>
                                <Form.Control type="text" placeholder="Enter Address 2" {...bindAddress2} />
                            </Form.Group>
                        </Form.Row>
                        <Form.Row>
                            <Form.Group as={Col} controlId="shippingAddressCity">
                                <Form.Label>City</Form.Label>
                                <Form.Control type="text" placeholder="Enter City" {...bindCity} />
                            </Form.Group>
                            <Form.Group as={Col} controlId="shippingAddressState">
                                <Form.Label>State</Form.Label>
                                <Form.Control as="select" {...bindState}>
                                    {
                                        ['CA', 'GA', 'NV', 'NY', 'OR', 'TX', 'FL', 'WA'].map(state => <option key={state} value={state}>{state}</option>)
                                    }
                                </Form.Control>
                            </Form.Group>
                            <Form.Group as={Col} controlId="shippingZipCode">
                                <Form.Label>Zip Code</Form.Label>
                                <Form.Control type="text" placeholder="Enter Zip Code" {...bindZipCode} />
                            </Form.Group>
                        </Form.Row>
                        <h4>Available Products</h4>
                            {
                                products[0] ?
                                (
                                    <Form.Row className='mb-2'>
                                        <Col md={{offset: 1}}>
                                            <Form.Label>{`${products[0].name} (${products[0].sku}):`}</Form.Label>
                                        </Col>
                                        <Col>
                                            <Form.Control type="text" placeholder="Enter Quantity" {...bindQuantity1} />
                                        </Col>
                                    </Form.Row>
                                ) : 
                                    null
                            }
                            {
                                products[1] ?
                                (
                                    <Form.Row className='mb-2'>
                                        <Col md={{offset: 1}}>
                                            <Form.Label>{`${products[1].name} (${products[1].sku}):`}</Form.Label>
                                        </Col>
                                        <Col>
                                            <Form.Control type="text" placeholder="Enter Quantity" {...bindQuantity2} />
                                        </Col>
                                    </Form.Row>
                                ) : 
                                null
                            }
                            {
                                products[2] ?
                                (
                                    <Form.Row>
                                        <Col md={{offset: 1}}>
                                            <Form.Label>{`${products[2].name} (${products[2].sku}):`}</Form.Label>
                                        </Col>
                                        <Col>
                                            <Form.Control type="text" placeholder="Enter Quantity" {...bindQuantity3} />
                                        </Col>
                                    </Form.Row>
                                ) : 
                                null
                            }
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={closeModal}>
                        Close
                    </Button>
                    <Button variant="success" onClick={submitOrderUpdate}>
                        {buttonText}
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
}

const mapDispatchToProps = {
    closeModal,
    editOrder,
}

const mapStateToProps = state => {
    return {
        currentModal: state.modal.currentModal,
        show: state.modal.currentModal !== null,
        order: state.modal.order,
        products: state.products.products,
    };
}

export default connect(mapStateToProps, mapDispatchToProps)(EditProductModal);
