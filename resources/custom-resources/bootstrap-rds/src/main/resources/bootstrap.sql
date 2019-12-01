-- Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
-- 
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this
-- software and associated documentation files (the "Software"), to deal in the Software
-- without restriction, including without limitation the rights to use, copy, modify,
-- merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so.
-- 
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
-- INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
-- PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
-- HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
-- OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

-- Load up the UUID data type
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE category (
	category_id SERIAL PRIMARY KEY,
	category VARCHAR(255) NOT NULL UNIQUE CHECK (category <> '')
);

CREATE TABLE product (
	product_id SERIAL PRIMARY KEY,
	sku VARCHAR(32) NOT NULL UNIQUE CHECK (sku <> ''),
	product VARCHAR(255) NOT NULL UNIQUE CHECK (product <> ''),
	price DECIMAL(9,2) NOT NULL
);

CREATE TABLE product_categories (
	product_id INT NOT NULL REFERENCES product (product_id) ON DELETE CASCADE ON UPDATE CASCADE,
	category_id INT NOT NULL REFERENCES category (category_id) ON DELETE RESTRICT ON UPDATE CASCADE,
	CONSTRAINT product_categories_pk PRIMARY KEY (product_id, category_id)
);

CREATE TABLE purchaser (
	purchaser_id SERIAL PRIMARY KEY,
	first_name VARCHAR(64),
	last_name VARCHAR(64),
	UNIQUE(first_name, last_name)
);

CREATE TABLE order_fulfillment (
	order_fulfillment_id SERIAL PRIMARY KEY,
	order_date DATE NOT NULL,
	ship_date DATE,
	purchaser_id INTEGER NOT NULL REFERENCES purchaser (purchaser_id) ON DELETE RESTRICT ON UPDATE CASCADE,
	ship_to_line1 VARCHAR(128),
	ship_to_line2 VARCHAR(128),
	ship_to_city VARCHAR(128),
	ship_to_state VARCHAR(128),
	ship_to_postal_code VARCHAR(128),
	bill_to_line1 VARCHAR(128),
	bill_to_line2 VARCHAR(128),
	bill_to_city VARCHAR(128),
	bill_to_state VARCHAR(128),
	bill_to_postal_code VARCHAR(128)
);

CREATE TABLE order_line_item (
	order_line_item_id SERIAL PRIMARY KEY,
	order_fulfillment_id INT NOT NULL REFERENCES order_fulfillment (order_fulfillment_id) ON DELETE RESTRICT ON UPDATE CASCADE,
	product_id INT NOT NULL REFERENCES product (product_id) ON DELETE RESTRICT ON UPDATE CASCADE,
	quantity INT NOT NULL CHECK (quantity > 0),
	unit_purchase_price DECIMAL(9, 2) NOT NULL
);

-- UI currently blows up if there are no categories, so add some static entries even if we're not
-- entering fake product or order data
INSERT INTO category (category) VALUES ('JavaScript'), ('Python'), ('Java'), ('C#'), ('PHP'), ('Swift'), ('Ruby'), ('Golang');