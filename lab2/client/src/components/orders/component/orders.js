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
import React, { useEffect } from 'react';
import { connect } from 'react-redux';
import {
    addOrderModal,
    editOrderModal,
    deleteOrderModal,
} from '../../modals/actions';
import {
    fetchOrders,
} from '../actions';
import {
    fetchProducts,
} from '../../products/actions';
import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Table from 'react-bootstrap/Table';
import Tooltip from 'react-bootstrap/Tooltip';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';
import moment from 'moment';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faTimesCircle,
    faCheckCircle,
    faPlus,
    faEdit,
    faTrash,
} from '@fortawesome/free-solid-svg-icons';

function Orders(props) {
    const {
        orders,
        fetchOrders,
        fetchProducts,
        addOrderModal,
        editOrderModal,
        deleteOrderModal,
     } = props;

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);

    useEffect(() => {
        fetchProducts();
    }, [fetchProducts]);

    orders.sort((a, b) => b.id - a.id);

    return (
        <Container>
            <h2>Orders</h2>
            <Container>
                <Row className='mb-2'>
                    <Col>
                        <Button onClick={addOrderModal} variant='success' className="float-right"> Add Order <FontAwesomeIcon icon={faPlus} /></Button>
                    </Col>
                </Row>
                <Row>
                    <Table>
                        <thead>
                            <tr>
                                <th>Order Number</th>
                                <th>Order Total</th>
                                <th>Order Date</th>
                                <th>Total Items</th>
                                <th>Purchaser</th>
                                <th className="text-center">Shipped</th>
                                <th className="text-center">Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            {
                                orders ?
                                    orders.map((order) => {
                                        const { id,
                                            orderDate,
                                            lineItems,
                                            total,
                                            totalItems = lineItems ? lineItems.length : 0,
                                            purchaser,
                                            purchaserId = purchaser.id,
                                            shipDate,
                                        } = order;

                                        return (
                                            <tr key={id}>
                                                <td>{id}</td>
                                                <td>${total}</td>
                                                <td>{moment(orderDate).format('L')}</td>
                                                <td>{totalItems}</td>
                                                <td>{purchaserId}</td>
                                                <td className="text-center">{shipDate === null ? <FontAwesomeIcon className="text-danger" icon={faTimesCircle} /> : <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip-ship-date">{shipDate}</Tooltip>}><FontAwesomeIcon className="text-success" icon={faCheckCircle} /></OverlayTrigger>}</td>
                                                <td className="text-center"><Button variant='secondary' onClick={() => editOrderModal(order)}> Edit <FontAwesomeIcon icon={faEdit} /></Button> <Button onClick={() => deleteOrderModal(order)} variant='danger'> Del <FontAwesomeIcon icon={faTrash} /></Button></td>
                                            </tr>
                                        );
                                    }
                                    )
                                    :
                                    null
                            }
                        </tbody>
                    </Table>
                </Row>
            </Container>
        </Container>
    )
}

const mapDispatchToProps = {
    fetchOrders,
    fetchProducts,
    addOrderModal,
    editOrderModal,
    deleteOrderModal,
};

const mapStateToProps = state => {
    return {
        orders: state.orders.orders,
    };
};

export default connect(mapStateToProps, mapDispatchToProps)(Orders);
