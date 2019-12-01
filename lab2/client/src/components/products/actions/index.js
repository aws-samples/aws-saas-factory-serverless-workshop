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
import config from '../../../shared/config';
import {
    REQUEST_ALL_PRODUCTS,
    RECEIVE_ALL_PRODUCTS,
    RECEIVE_PRODUCT_CATEGORIES,
    RECEIVE_ADD_PRODUCT,
    RECEIVE_EDIT_PRODUCT,
    RECEIVE_DELETE_PRODUCT,
 } from '../actionTypes';
import {
    closeModal,
    errorModal,
 } from '../../modals/actions';

Axios.defaults.baseURL = config.api.base_url;

export const requestAllProducts = () => {
    return {
        type: REQUEST_ALL_PRODUCTS,
    };
};

export const receiveAllProducts = products => {
    return {
        type: RECEIVE_ALL_PRODUCTS,
        products,
    };
};

export const receiveProductCategories = categories => {
    return {
        type: RECEIVE_PRODUCT_CATEGORIES,
        categories,
    };
};

export const addProductFinished = (product) => {
    return {
        type: RECEIVE_ADD_PRODUCT,
        product,
    };
};

export const editProductFinished = (product) => {
    return {
        type: RECEIVE_EDIT_PRODUCT,
        product,
    };
};

export const deleteProductFinished = (product) => {
    return {
        type: RECEIVE_DELETE_PRODUCT,
        product,
    };
};

export const fetchProducts = () => {
    return function(dispatch) {
        const url = '/products';
        const instance = createAxiosInstance();

        instance.get(url)
            .then(response => {
                dispatch(receiveAllProducts(response.data))
            }, error => console.error(error));
    };
};

export const fetchProductCategories = () => {
    return function(dispatch) {
        const url = '/categories';
        const instance = createAxiosInstance();

        instance.get(url)
        .then(response => {
            dispatch(receiveProductCategories(response.data))
        }, error => console.error(error));
    };
};

export const addProduct = (product) => {
    return function(dispatch) {
        const url = '/products';
        const instance = createAxiosInstance();

        instance.post(url, product)
            .then(response => {
                dispatch(addProductFinished(response.data));
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    };
};

export const editProduct = (product) => {
    return function(dispatch) {
        const url = `/products/${product.id}`;
        const instance = createAxiosInstance();

        instance.put(url, product)
            .then(() => {
                dispatch(editProductFinished(product));
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    }
};

export const deleteProduct = (product) => {
   return function(dispatch) {
       const url = `/products/${product.id}`;
       const instance = createAxiosInstance();

        dispatch(closeModal());

        /*
        instance.delete(url, { data: product })
            .then(response => {
                const deletedProduct = response.data;
                if (deletedProduct && deletedProduct.id) {
                    dispatch(deleteProductFinished(deletedProduct));
                } else {
                    dispatch(errorModal("The product was not deleted."));
                }
            }, error => console.error(error))
            .then(() => {
            }, error => console.error(error));
        */
   };
};

function createAxiosInstance () {
    const token = sessionStorage.getItem('idToken');

        const instance = Axios.create({
            headers: {'Authorization': `Bearer ${token}`},
          });

          return instance;
}
