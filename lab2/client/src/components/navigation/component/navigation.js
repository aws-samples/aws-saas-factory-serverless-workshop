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
import { Link } from 'react-router-dom';
import {
  signInModal,
  signUpModal,
} from '../../modals/actions';
import {
  signOutUser,
} from '../../user/actions';
import { AuthenticatedLink } from '../';
import Navbar from 'react-bootstrap/Navbar';
import Nav from 'react-bootstrap/Nav';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';

const Navigation = (props) => {
  const {
    signInModal,
    signUpModal,
    signOutUser,
    currentUser, } = props;

  return (
    <Navbar expand="lg">
      <Navbar.Toggle aria-controls="basic-navbar-nav" />
      <Navbar.Collapse id="basic-navbar-nav">
        <Nav className="mr-auto">
          <Link className="text-success nav-link" to="/">Home</Link>
          <AuthenticatedLink isAuthenticated={currentUser.isAuthenticated} linkHref="dashboard" linkText="Dashboard" />
          <AuthenticatedLink isAuthenticated={currentUser.isAuthenticated} linkHref="products" linkText="Products" />
          <AuthenticatedLink isAuthenticated={currentUser.isAuthenticated} linkHref="orders" linkText="Orders" />
        </Nav>
        {currentUser.isAuthenticated ? (
          <Form inline>
              <span>Welcome back, {currentUser.email}!</span>
              <Button variant="link" onClick={signOutUser}>Sign Out</Button>
          </Form>
        ) :
        <Form inline>
              <Button variant="link" onClick={signInModal}>Sign In</Button>
              <Button variant="success" onClick={signUpModal}>Sign Up</Button>
            </Form>
        }
      </Navbar.Collapse>
    </Navbar>
  );
}

const mapDispatchToProps = {
  signInModal,
  signUpModal,
  signOutUser,
};

const mapStateToProps = state => ({
  currentUser: state.user,
});

export default connect(mapStateToProps, mapDispatchToProps)(Navigation);
