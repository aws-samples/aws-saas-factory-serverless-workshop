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

INSERT INTO product (sku, product, price) VALUES ('054138', 'SaaS Architectures in JavaScript', 49.99), ('345629', 'Go SaaS', 37.99), ('582460', 'Modern C# Multi-Tenancy', 59.99), ('680425', 'iOS SaaS Identity', 79.99);
INSERT INTO product_categories (product_id, category_id)
SELECT product_id, category_id FROM product CROSS JOIN category WHERE sku = '054138' AND category = 'JavaScript'
UNION
SELECT product_id, category_id FROM product CROSS JOIN category WHERE sku = '345629' AND category = 'Golang'
UNION
SELECT product_id, category_id FROM product CROSS JOIN category WHERE sku = '582460' AND category = 'C#'
UNION
SELECT product_id, category_id FROM product CROSS JOIN category WHERE sku = '680425' AND category = 'Swift'
;

INSERT INTO purchaser (first_name, last_name) VALUES ('Melissa', 'Jones'), ('Robin', 'Wrangler'), ('Thomas', 'Smith');

INSERT INTO order_fulfillment (purchaser_id, order_date, ship_to_line1, ship_to_city, ship_to_state, ship_to_postal_code, bill_to_line1, bill_to_city, bill_to_state, bill_to_postal_code)
VALUES
(1, CURRENT_DATE - INTERVAL '30 day', '347 Main St', 'SaaS City', 'NV', '12345', '347 Main St', 'SaaS City', 'NV', '12345'),
(2, CURRENT_DATE - INTERVAL '10 day', '63 Multi-Tenant Circle', 'Cloud City', 'NY', '12345', '63 Multi-Tenant Circle', 'Cloud City', 'NY', '12345'),
(3, CURRENT_DATE - INTERVAL '1 day', '53 Cloud Route', 'Consumption Pricing Town', 'CA', '12345', '53 Cloud Route', 'Consumption Pricing Town', 'CA', '12345');

INSERT INTO order_line_item (order_fulfillment_id, product_id, quantity, unit_purchase_price)
SELECT 1, product_id, 2, price FROM product WHERE sku = '345629'
UNION
SELECT 1, product_id, 1, price FROM product WHERE sku = '582460'
UNION
SELECT 2, product_id, 1, price FROM product WHERE sku = '054138'
UNION
SELECT 3, product_id, 10, price FROM product WHERE sku = '582460'
;