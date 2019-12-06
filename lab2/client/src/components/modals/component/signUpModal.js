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
import { registerUser } from '../../user/actions';
import Modal from 'react-bootstrap/Modal';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faAward } from '@fortawesome/free-solid-svg-icons';

function SignUpModal(props) {
    const {
        show,
        title = 'Sign Up',
        buttonText = 'Sign Up',
        privacyMessage = 'Your email will never be shared.',
        closeModal,
        registerUser,
    } = props;

    const plans = [
        {
            id: 1,
            name: 'Standard Tier',
        },
        {
            id: 2,
            name: 'Professional Tier',
        },
        {
            id: 3,
            name: 'Advanced Tier',
        }
    ];

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

    const { value: firstName, bind: bindFirstName } = useInput('');
    const { value: lastName, bind: bindLastName } = useInput('');
    const { value: email, bind: bindEmail } = useInput('');
    const { value: password, bind: bindPassword } = useInput('');
    const { value: verifyPassword, bind: bindVerifyPassword } = useInput('');
    const { value: company, bind: bindCompany } = useInput('');
    const { value: plan, bind: bindPlan } = useInput(plans[0].name);

    const submitRegisterUser = event => {
        const form = event.currentTarget;
        const verifyPasswordControl = document.getElementById('signUpFormPasswordVerify');
        const submitButton = document.getElementById('signUpFormSubmitButton');
        const user = {
            firstName,
            lastName,
            email,
            password,
            company,
            plan,
        };

        event.preventDefault();

        if(password !== verifyPassword) {
            verifyPasswordControl.setCustomValidity('The passwords you entered do not match. Please verify the passwords are the same.');
            event.stopPropagation();
        } else {
            verifyPasswordControl.setCustomValidity('');
        }

        if(form.checkValidity() === false) {
            form.reportValidity();
            event.stopPropagation();
        } else {
            submitButton.disabled = true;
            setValidated(true);
            registerUser(user);
        }
    };

    return (
        <>
            <Modal
                show={show}
                onHide={closeModal}
                centered
            >
                <Form id="signUpForm" noValidate validated={validated} onSubmit={submitRegisterUser}>
                    <Modal.Header closeButton>
                        <Modal.Title>{title}</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                            <Form.Group controlId="signUpFormFirstName">
                                <Form.Label>First Name</Form.Label>
                                <Form.Control type="text" placeholder="FirstName" required {...bindFirstName} />
                            </Form.Group>
                            <Form.Group controlId="signUpFormLastName">
                                <Form.Label>Last Name</Form.Label>
                                <Form.Control type="text" placeholder="LastName" required {...bindLastName} />
                            </Form.Group>
                            <Form.Group controlId="signUpFormEmail">
                                <Form.Label>Email Address</Form.Label>
                                <Form.Control type="email" placeholder="EmailAddress" required {...bindEmail} />
                                <Form.Text className="text-muted">
                                    {privacyMessage} <FontAwesomeIcon icon={faAward} />
                                </Form.Text>
                            </Form.Group>
                            <Form.Group controlId="signUpFormPassword">
                                <Form.Label>Password</Form.Label>
                                <Form.Control type="password" placeholder="Password" required pattern="(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])\S{8,}" {...bindPassword} />
                                <Form.Text className="text-muted">
                                    At least 8 characters containing 1 uppercase, 1 lowercase, and 1 number
                                </Form.Text>
                            </Form.Group>
                            <Form.Group controlId="signUpFormPasswordVerify">
                                <Form.Label>Verify Password</Form.Label>
                                <Form.Control type="password" placeholder="Password" required pattern="(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])\S{8,}" {...bindVerifyPassword} />
                                <Form.Control.Feedback type="invalid">
                                    The passwords you entered do not match. Please verify the passwords are the same.
                                </Form.Control.Feedback>
                            </Form.Group>
                            <Form.Group controlId="signUpFormCompany">
                                <Form.Label>Company</Form.Label>
                                <Form.Control type="text" placeholder="CompanyName" required {...bindCompany} />
                            </Form.Group>
                            <Form.Group controlId="signUpFormPlan">
                                <Form.Label>Plan</Form.Label>
                                <Form.Control as="select" {...bindPlan} >
                                    {
                                        plans.map(plan => <option key={plan.id} value={plan.name}>{plan.name}</option>)
                                    }
                                </Form.Control>
                            </Form.Group>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button variant="secondary" onClick={closeModal}>
                            Close
                        </Button>
                        <Button type="submit" id="signUpFormSubmitButton" variant="success">
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
    registerUser,
};

const mapStateToProps = state => {
    return {
        currentModal: state.modal.currentModal,
        show: state.modal.currentModal !== null,
    };
}

export default connect(mapStateToProps, mapDispatchToProps)(SignUpModal);
