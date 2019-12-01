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
import { closeModal } from '../actions';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import {
    authenticateUser,
} from '../../user/actions';

function SignInModal(props) {
    const {
        show,
        title = 'Sign In',
        buttonText = 'Sign In',
        closeModal,
        authenticateUser,
    } = props;

    const [validated, setValidated] = useState(false);

    const useInput = initialValue => {
        const [value, setValue] = useState(initialValue);

        return {
            value,
            setValue,
            reset: () => setValue(''),
            bind: {
                value,
                onChange: event => {
                    setValue(event.target.value)
                }
            },
        };
    };

    const { value: email, bind: bindEmail } = useInput('');
    const { value: password, bind: bindPassword } = useInput('');

    const submitSignInUser = event => {
        const form = event.currentTarget;
        const userChallenge = {
            username: email,
            password,
        }

        event.preventDefault();

        if(form.checkValidity() === false) {
            form.reportValidity();
            event.stopPropagation();
        } else {
            setValidated(true);
            authenticateUser(userChallenge);
        }
    };

    return (
        <>
            <Modal
                show={show}
                onHide={closeModal}
                centered
            >
                <Form id="signInForm" noValidate validated={validated} onSubmit={submitSignInUser} >
                    <Modal.Header closeButton>
                        <Modal.Title>{title}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                            <Form.Group controlId="signInformEmail">
                                <Form.Label>Email address</Form.Label>
                                <Form.Control type="email" required placeholder="Enter email" {...bindEmail} />
                                <Form.Control.Feedback type="invalid">
                                    Please provide a valid email address.
                                </Form.Control.Feedback>
                            </Form.Group>
                            <Form.Group controlId="signInformPassword">
                                <Form.Label>Password</Form.Label>
                                <Form.Control type="password" required placeholder="Password" {...bindPassword} />
                                <Form.Control.Feedback type="invalid">
                                    Please provide your password.
                                </Form.Control.Feedback>
                            </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={closeModal} >
                            Close
                        </Button>
                        <Button type="submit" variant="success" >
                            {buttonText}
                        </Button>
                    </Modal.Footer>
                </Form>
            </Modal>
        </>
    );
}

const mapDispatchToProps = {
  closeModal,
  authenticateUser,  
};

const mapStateToProps = state => {
    return {
        currentModal: state.modal.currentModal,
        show: state.modal.currentModal !== null,
    };
}

export default connect(mapStateToProps, mapDispatchToProps)(SignInModal);
