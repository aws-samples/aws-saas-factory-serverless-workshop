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
import { BrowserRouter as Router, Route, Redirect } from 'react-router-dom';
import { Provider, connect } from 'react-redux';
import Container from 'react-bootstrap/Container';
import Jumbotron from 'react-bootstrap/Jumbotron';

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
            <Jumbotron>
              <Route exact={true} path='/'>
                 {isAuthenticated ? <Redirect to="/dashboard" /> : <Home /> }
              </Route>
              <PrivateRoute path='/dashboard' component={Dashboard} />
              <PrivateRoute path='/products' component={Products} />
              <PrivateRoute path='/orders' component={Orders} />
            </Jumbotron>
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
