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
    SET_CURRENT_MODAL,
    SIGN_IN_MODAL,
    SIGN_UP_MODAL,
    ADD_PRODUCT_MODAL,
    EDIT_PRODUCT_MODAL,
    DELETE_PRODUCT_MODAL,
    ADD_ORDER_MODAL,
    EDIT_ORDER_MODAL,
    DELETE_ORDER_MODAL,
    CLOSE_MODAL,
    ERROR_MODAL,
} from '../actionTypes';

export const signInModal = () => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: SIGN_IN_MODAL,
    };
};

export const signUpModal = () => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: SIGN_UP_MODAL,
    };
};

export const addProductModal = () => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: ADD_PRODUCT_MODAL,
    };
};

export const editProductModal = (product) => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: EDIT_PRODUCT_MODAL,
        product,
    };
};

export const deleteProductModal = (product) => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: DELETE_PRODUCT_MODAL,
        product,
    };
};

export const addOrderModal = () => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: ADD_ORDER_MODAL,
    };
};

export const editOrderModal = (order) => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: EDIT_ORDER_MODAL,
        order,
    };
};

export const deleteOrderModal = (order) => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: DELETE_ORDER_MODAL,
        order,
    };
};

export const closeModal = () => {
    return {
        type: CLOSE_MODAL,
    };
};

export const errorModal = error => {
    return {
        type: SET_CURRENT_MODAL,
        currentModal: ERROR_MODAL,
        error,
    };
};
