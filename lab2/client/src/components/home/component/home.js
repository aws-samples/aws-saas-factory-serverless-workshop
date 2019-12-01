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
import React from 'react';
import { connect } from 'react-redux';
import { signUpModal } from '../../modals/actions';
import Container from 'react-bootstrap/Container';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faArrowRight } from '@fortawesome/free-solid-svg-icons';
import bgImage from '../../../assets/green-02@2x.png';

function Home(props) {
    const { signUpModal } = props;

    return (
        <Container style={{ backgroundImage: `url(${bgImage})`, backgroundSize: 'cover' }}>
            <Container fluid style={{height: '40vh'}}>
                <Row>
                    <Col>
                        <h1 className="display-2 text-light"><strong>SaaS Commerce</strong> </h1>
                        <p className="lead text-light mb-5">Your SaaS commerce solution</p>
                        <p>
                            <Button variant='danger' onClick={signUpModal}>Get Started Now <FontAwesomeIcon icon={faArrowRight}/></Button>
                        </p>
                    </Col>
                </Row>
            </Container>
        </Container>
    )
}

export default connect(() => ({}), { signUpModal })(Home);
