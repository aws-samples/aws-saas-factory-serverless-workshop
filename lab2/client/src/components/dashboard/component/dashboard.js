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
    fetchOrders,
 } from '../../orders/actions';
 import {
    fetchProducts,
 } from '../../products/actions';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import { Link } from 'react-router-dom';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import {
    faArrowCircleRight,
    faChartLine,
    faLongArrowAltRight,
    faLongArrowAltUp,
    faLongArrowAltDown,
    faListOl,
    faTags,
} from '@fortawesome/free-solid-svg-icons';

function Dashboard(props) {
    const { productCount,
        orderCount,
        fetchProducts,
        fetchOrders,
    } = props;
    
    useEffect(() => {
            fetchProducts();
    }, [fetchProducts]);

    useEffect(() => {
        fetchOrders();
    }, [fetchOrders]);

    const trend = [faLongArrowAltRight, faLongArrowAltUp, faLongArrowAltDown][Math.floor(Math.random() * 3)];

    return (
        <Container>
            <Row>
                <h2>Tenant Name</h2>
            </Row>
            <Row>
                <Col>
                    <Card bg="light" text="black" border="success" style={{ width: '18rem' }}>
                        <Card.Body>
                            <Row>
                                <Col>
                                    <FontAwesomeIcon icon={faListOl} size="5x" />
                                </Col>
                                <Col>
                                    <p>Orders: { orderCount }</p>
                                </Col>
                            </Row>
                        </Card.Body>
                        <Card.Footer bg="gray">
                            <Row>
                                <Col>
                                    <Link style={{ textDecoration: 'none' }} to="/orders">Details <FontAwesomeIcon icon={faArrowCircleRight} /></Link>
                                </Col>
                            </Row>
                        </Card.Footer>
                    </Card>
                </Col>
                <Col>
                    <Card bg="light" text="black" border="success" style={{ width: '18rem' }}>
                        <Card.Body>
                            <Row>
                                <Col>
                                    <FontAwesomeIcon icon={faTags} size="5x" />
                                </Col>
                                <Col>
                                    <p>Products: { productCount }</p>
                                </Col>
                            </Row>
                        </Card.Body>
                        <Card.Footer bg="gray">
                            <Row>
                                <Col>
                                <Link style={{ textDecoration: 'none' }} to="/products">Details <FontAwesomeIcon icon={faArrowCircleRight} /></Link>
                                </Col>
                            </Row>
                        </Card.Footer>
                    </Card>
                </Col>
                <Col>
                    <Card bg="light" text="black" border="success" style={{ width: '18rem' }}>
                        <Card.Body>
                            <Row>
                                <Col>
                                    <FontAwesomeIcon icon={faChartLine} size="5x" />
                                </Col>
                                <Col>
                                    <p>Trend: <FontAwesomeIcon icon={trend} /></p>
                                </Col>
                            </Row>
                        </Card.Body>
                        <Card.Footer bg="gray">
                            <Row>
                                <Col>
                                <Link style={{ textDecoration: 'none' }} to="/dashboard">Details <FontAwesomeIcon icon={faArrowCircleRight} /></Link>
                                </Col>
                            </Row>
                        </Card.Footer>
                    </Card>
                </Col>
            </Row>
        </Container>
    )
}

const mapDispatchToProps = {
    fetchProducts,
    fetchOrders,
};

const mapStateToProps = state => {
    return {
        productCount: state.products ? state.products.productCount : 0,
        orderCount: state.orders ? state.orders.orderCount : 0,
    }
}

export default connect(mapStateToProps, mapDispatchToProps)(Dashboard);
