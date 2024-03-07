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
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Provider, connect } from 'react-redux';
import Container from 'react-bootstrap/Container';

import {
  Navigation,
  PrivateRoute,
} from './components/navigation';
import { Home } from './components/home';
import { Dashboard } from './components/dashboard';
import { Products } from './components/products';
import { Orders } from './components/orders';
import { Footer } from './components/layout';
import { ModalWrapper as Modal } from './components/modals';

const App = ({ store, currentUser }) => {
  const { isAuthenticated } = currentUser;

  return (
    <Provider store={store}>
      <Router>
        <Container>
          <Container>
            <Navigation />
          </Container>
          <Container>
            <div class="jumbotron">
              <Routes>
                <Route
                  exact={true}
                  path="/"
                  element={
                    isAuthenticated ? <Navigate to="/dashboard" /> : <Home />
                  }
                />
                <Route
                  path="/dashboard"
                  element={
                    <PrivateRoute currentUser={currentUser}>
                      <Dashboard />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/products"
                  element={
                    <PrivateRoute currentUser={currentUser}>
                      <Products />
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/orders"
                  element={
                    <PrivateRoute currentUser={currentUser}>
                      <Orders />
                    </PrivateRoute>
                  }
                />
              </Routes>
            </div>
          </Container>
          <Container>
            <Footer />
          </Container>
        </Container>
        <Modal />
      </Router>
    </Provider>
  );
}

const mapStateToProps = state => ({
    currentUser: state.user,
});

export default connect(mapStateToProps)(App);
