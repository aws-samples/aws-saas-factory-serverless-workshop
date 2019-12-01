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
import Axios from 'axios';
import jwt from 'jsonwebtoken';
import config from '../../../shared/config';
import {
    RECEIVE_AUTHENTICATE_USER,
    RECEIVE_REGISTER_USER,
} from '../actionTypes';
import {
    closeModal,
} from '../../modals/actions';

Axios.defaults.baseURL = config.api.base_url;

const registerUserFinished = user => {
    return {
        type: RECEIVE_REGISTER_USER,
        user,
    };
};

const receiveUserAuthentication = user => {
    return {
        type: RECEIVE_AUTHENTICATE_USER,
        user,
    };
};

export const authenticateUser = (userChallenge) => {
    return function(dispatch) {
        const url = '/auth';

        Axios.post(url, userChallenge)
            .then(response => {
                if(response.data && response.data.idToken) {
                    const idToken = response.data.idToken;

                    sessionStorage.setItem('isAuthenticated', 'true');
                    sessionStorage.setItem('idToken', idToken);
                    
                    const decoded = jwt.decode(idToken);
                    const user = {
                        firstName: decoded.given_name,
                        lastName: decoded.family_name,
                        email: decoded.email,
                        company: decoded['custom:company'],
                        tenantId: decoded['custom:tenant_id'],
                        idToken,
                        isAuthenticated: true,
                    }

                    dispatch(receiveUserAuthentication(user));
                } else {
                    throw new Error("User authentication failed.");
                }
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    }
};

export const registerUser = (user) => {
    return function(dispatch) {
        const url = '/registration';

        Axios.post(url, user)
            .then(response => {
                dispatch(registerUserFinished(response.data));
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    }
};

export const signOutUser = () => {
    return function(dispatch) {     
        sessionStorage.removeItem('isAuthenticated');
        sessionStorage.removeItem('idToken');

        const user = {
            isAuthenticated: false,
        };

        dispatch(receiveUserAuthentication(user));
    };
};
