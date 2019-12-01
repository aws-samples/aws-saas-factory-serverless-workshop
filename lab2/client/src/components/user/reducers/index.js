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
import {
    REQUEST_AUTHENTICATE_USER,
    RECEIVE_AUTHENTICATE_USER,
    REQUEST_REGISTER_USER,
    RECEIVE_REGISTER_USER,
} from '../actionTypes';
import jwt from 'jsonwebtoken';

const isAuthenticated = sessionStorage.getItem('isAuthenticated');
const idToken = sessionStorage.getItem('idToken');
const decoded = jwt.decode(idToken);

const initialState = {
    firstName: decoded ? decoded.firstName : null,
    lastName: decoded ? decoded.lastName : null,
    email: decoded ? decoded.email : null,
    isAuthenticated: isAuthenticated ? Boolean(isAuthenticated) : false,
    idToken: null,
    tenantId: decoded ? decoded['custom:tenant_id'] : null,
    company: decoded ? decoded['custom:company'] : null,
};

export const user = (state = initialState, action) => {
    switch (action.type) {
        case REQUEST_AUTHENTICATE_USER:
            return null;
        case RECEIVE_AUTHENTICATE_USER:
            return {
                ...state,
                ...action.user,
            }
        case REQUEST_REGISTER_USER:
            return null;
        case RECEIVE_REGISTER_USER:
            return {
                ...state,
                ...action.user,
            }
        default:
            return state;
    };
};
