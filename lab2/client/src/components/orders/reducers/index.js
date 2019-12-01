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
    REQUEST_ALL_ORDERS,
    RECEIVE_ALL_ORDERS,
    REQUEST_ADD_ORDER,
    RECEIVE_ADD_ORDER,
    REQUEST_EDIT_ORDER,
    RECEIVE_EDIT_ORDER,
    REQUEST_DELETE_ORDER,
    RECEIVE_DELETE_ORDER,
} from '../actionTypes';

const initialState = {
    orders: [],
    orderCount: 0,
}

function addOrder(orders, order) {
    const ordersCopy = orders.slice();
    ordersCopy.push(order);

    return ordersCopy;
}

function updateOrder(orders, updatedOrder) {
    const ordersCopy = orders.slice();
    return ordersCopy.map(order => order.id === updatedOrder.id ?  updatedOrder : order);
}

function deleteOrder(orders, deletedOrder) {
    const ordersCopy = orders.slice();
    return ordersCopy.filter(order => order.id !== deletedOrder.id);
}

export const orders = (state = initialState, action) => {
    switch (action.type) {
        case REQUEST_ALL_ORDERS:
            return null;
        case RECEIVE_ALL_ORDERS:
            return {
                ...state,
                orders: action.orders,
                orderCount: action.orders.length,
            }
        case REQUEST_ADD_ORDER:
                return null;
        case RECEIVE_ADD_ORDER: 
            const ordersWithAdd = addOrder(state.orders, action.order);

            return {
                ...state,
                orders: ordersWithAdd,
                orderCount: ordersWithAdd.length,
            }
        case REQUEST_EDIT_ORDER:
                return null;
        case RECEIVE_EDIT_ORDER: 
            const ordersWithUpdate = updateOrder(state.orders, action.order);

            return {
                ...state,
                orders: ordersWithUpdate,
                orderCount: ordersWithUpdate.length,
            }
        case REQUEST_DELETE_ORDER:
                return null;
        case RECEIVE_DELETE_ORDER: 
            const ordersWithDelete = deleteOrder(state.orders, action.order);

            return {
                ...state,
                orders: ordersWithDelete,
                orderCount: ordersWithDelete.length,
            }
        default:
            return state;
    }    
};
