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
    RECEIVE_ALL_ORDERS,
    RECEIVE_ADD_ORDER,
    RECEIVE_EDIT_ORDER,
    RECEIVE_DELETE_ORDER,
} from '../actionTypes';
import {
    closeModal,
    errorModal,
 } from '../../modals/actions';

Axios.defaults.baseURL = config.api.base_url;

export const addOrderFinished = (order) => {
    return {
        type: RECEIVE_ADD_ORDER,
        order,
    };
};

export const editOrderFinished = (order) => {
    return {
        type: RECEIVE_EDIT_ORDER,
        order,
    };
};

export const deleteOrderFinished = (order) => {
    return {
        type: RECEIVE_DELETE_ORDER,
        order,
    };
};

export const receiveAllOrders = orders => {
    return {
        type: RECEIVE_ALL_ORDERS,
        orders,
    };
};

export const fetchOrders = () => {
    return function(dispatch) {
        const url = '/orders';
    
        const instance = createAxiosInstance();

        instance.get(url)
            .then(response => {
                dispatch(receiveAllOrders(response.data))
            }, error => console.error(error));
    };
};

export const addOrder = (order) => {
    return function(dispatch) {
        const url = '/orders';
        const instance = createAxiosInstance();
        
        dispatch(closeModal());

        instance.post(url, order)
            .then(response => {
                dispatch(addOrderFinished(response.data));
            })
            .catch(error => {
                const errorText = `The order was not added. (${error.message})`;

                dispatch(errorModal(errorText));
            });
    };
};

export const editOrder = (order) => {
    return function(dispatch) {
        const url = `/orders/${order.id}`;
        const instance = createAxiosInstance();

        instance.put(url, order)
            .then(response => {
                dispatch(editOrderFinished(response.data));
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    }
};

export const deleteOrder = (order) => {
    return function(dispatch) {
        const url = `/orders/${order.id}`;
        const instance = createAxiosInstance();

        instance.delete(url, { data: order })
            .then(response => {
                dispatch(deleteOrderFinished(order));
            }, error => console.error(error))
            .then(() => {
                dispatch(closeModal());
            }, error => console.error(error));
    };
};

function createAxiosInstance () {
    const token = sessionStorage.getItem('idToken');

        const instance = Axios.create({
            headers: {'Authorization': `Bearer ${token}`},
          });

          return instance;
}
