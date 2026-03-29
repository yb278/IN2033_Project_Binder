-- =============================================================
-- IPOS-CA Database Schema
-- Module: IN2033 Team Project 2025-2026
-- Team B (IPOS-CA), Group 2
-- =============================================================

CREATE DATABASE IF NOT EXISTS ipos_ca;
USE ipos_ca;

-- -------------------------------------------------------------
-- MERCHANT SETTINGS
-- Stores pharmacy identity details and global config (VAT, etc.)
-- Used by IPOS-CA-Templates and IPOS-CA-Stock (CA-21, CA-37)
-- -------------------------------------------------------------
CREATE TABLE merchant_settings (
    id              INT PRIMARY KEY DEFAULT 1,   -- single-row config table
    pharmacy_name   VARCHAR(100)    NOT NULL,
    address_line1   VARCHAR(100),
    address_line2   VARCHAR(100),
    city            VARCHAR(50),
    postcode        VARCHAR(10),
    phone           VARCHAR(20),
    fax             VARCHAR(20),
    email           VARCHAR(100),
    logo_path       VARCHAR(255),                -- path to logo image file
    vat_rate        DECIMAL(5,2)    NOT NULL DEFAULT 20.00,  -- configurable (CA-21)
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Default merchant row
INSERT INTO merchant_settings (id, pharmacy_name, address_line1, city, postcode, phone, vat_rate)
VALUES (1, 'Cosymed Ltd.', '3 High Level Drive', 'Sydenham', 'SE26 3ET', '0208 778 0124', 20.00);

-- -------------------------------------------------------------
-- USERS (Staff accounts for IPOS-CA login)
-- Roles: ADMIN, PHARMACIST, MANAGER  (CA-01, CA-03, CA-08)
-- ADMIN    -> full access + manage user accounts
-- PHARMACIST -> catalogue, sales, stock, orders, customer accounts
-- MANAGER  -> reports, credit limits, templates
-- -------------------------------------------------------------
CREATE TABLE users (
    user_id         INT             PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,               -- store hashed passwords
    first_name      VARCHAR(50)     NOT NULL,
    last_name       VARCHAR(50)     NOT NULL,
    role            ENUM('ADMIN', 'PHARMACIST', 'MANAGER') NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,  -- CA-04 (remove = deactivate)
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Default admin account (password: admin123 – change before demo)
INSERT INTO users (username, password_hash, first_name, last_name, role)
VALUES ('admin', 'admin123', 'System', 'Admin', 'ADMIN');

-- -------------------------------------------------------------
-- DISCOUNT PLANS
-- Supports fixed and flexible plans (Student Brief §8.2 IPOS-CA-CUST)
-- CA-34: Apply Discount Plan to an account holder
-- -------------------------------------------------------------
CREATE TABLE discount_plans (
    plan_id         INT             PRIMARY KEY AUTO_INCREMENT,
    plan_name       VARCHAR(100)    NOT NULL,
    plan_type       ENUM('FIXED', 'FLEXIBLE') NOT NULL,
    -- FIXED: single flat rate applied to every order
    fixed_rate      DECIMAL(5,2),                           -- e.g. 5.00 = 5%
    -- FLEXIBLE: JSON-encoded tiers stored as a text field
    -- Format: [{"min":0,"max":1000,"rate":1.0}, {"min":1000,"max":2000,"rate":2.0}, ...]
    flexible_tiers  TEXT,
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- Example plans
INSERT INTO discount_plans (plan_name, plan_type, fixed_rate) VALUES ('No Discount', 'FIXED', 0.00);
INSERT INTO discount_plans (plan_name, plan_type, fixed_rate) VALUES ('5% Flat Discount', 'FIXED', 5.00);
INSERT INTO discount_plans (plan_name, plan_type, flexible_tiers)
VALUES ('Monthly Volume Discount', 'FLEXIBLE',
        '[{"min":0,"max":1000,"rate":1.0},{"min":1000,"max":2000,"rate":2.0},{"min":2000,"max":999999,"rate":3.0}]');

-- -------------------------------------------------------------
-- ACCOUNT HOLDERS (consumers with a merchant account)
-- CA-03 Create, CA-04 Remove, CA-05 Update, CA-06 View Status
-- CA-09 Update Status, CA-10 Record Payment, CA-33 Set Credit Limit
-- CA-34 Apply Discount Plan
-- Status lifecycle per Student Brief Fig.1:
--   NORMAL -> SUSPENDED (15th of next month if unpaid) -> IN_DEFAULT (end of that month)
-- -------------------------------------------------------------
CREATE TABLE account_holders (
    holder_id           INT             PRIMARY KEY AUTO_INCREMENT,
    first_name          VARCHAR(50)     NOT NULL,
    last_name           VARCHAR(50)     NOT NULL,
    date_of_birth       DATE,
    email               VARCHAR(100)    NOT NULL,
    phone               VARCHAR(20),
    address_line1       VARCHAR(100),
    address_line2       VARCHAR(100),
    city                VARCHAR(50),
    postcode            VARCHAR(10),
    -- Account state
    account_status      ENUM('NORMAL','SUSPENDED','IN_DEFAULT') NOT NULL DEFAULT 'NORMAL',
    credit_limit        DECIMAL(10,2)   NOT NULL DEFAULT 500.00,
    outstanding_balance DECIMAL(10,2)   NOT NULL DEFAULT 0.00,  -- running total owed
    discount_plan_id    INT             REFERENCES discount_plans(plan_id),
    -- Monthly flexible discount tracking
    monthly_order_total DECIMAL(10,2)   NOT NULL DEFAULT 0.00,  -- resets each calendar month
    -- Reminder state (CA-11, CA-12; pseudo-code in Student Brief)
    status_1st_reminder ENUM('no_need','due','sent') NOT NULL DEFAULT 'no_need',
    status_2nd_reminder ENUM('no_need','due','sent') NOT NULL DEFAULT 'no_need',
    date_1st_reminder   DATE,
    date_2nd_reminder   DATE,
    -- Tracking when account status last changed (for automated status updates CA-09)
    payment_due_date    DATE,
    last_status_change  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------
-- ACCOUNT HOLDER PAYMENTS
-- CA-10: Record payment from an account holder; updates balance
-- -------------------------------------------------------------
CREATE TABLE account_holder_payments (
    payment_id      INT             PRIMARY KEY AUTO_INCREMENT,
    holder_id       INT             NOT NULL REFERENCES account_holders(holder_id),
    amount          DECIMAL(10,2)   NOT NULL,
    payment_date    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes           VARCHAR(255)
);

-- -------------------------------------------------------------
-- STOCK ITEMS (local pharmacy inventory)
-- Mirrors IPOS-SA catalogue but adds markup_rate and VAT reference
-- CA-18 Maintain Stock, CA-19 View Stock, CA-20 Check Low Stock
-- CA-21 Configure VAT (via merchant_settings.vat_rate)
-- CA-22 Update Retail Mark-up Rate
-- -------------------------------------------------------------
CREATE TABLE stock_items (
    stock_item_id       INT             PRIMARY KEY AUTO_INCREMENT,
    -- Links to IPOS-SA catalogue (shared DB access approach)
    sa_item_id          VARCHAR(20),                        -- e.g. "100 00001"
    description         VARCHAR(200)    NOT NULL,
    package_type        VARCHAR(50),
    unit                VARCHAR(20),
    units_per_pack      INT,
    bulk_cost           DECIMAL(10,2)   NOT NULL,           -- cost from InfoPharma (IPOS-SA price)
    markup_rate         DECIMAL(5,2)    NOT NULL DEFAULT 0.00, -- retail mark-up % (CA-22)
    quantity_available  INT             NOT NULL DEFAULT 0,
    min_stock_level     INT             NOT NULL DEFAULT 0,  -- low stock threshold
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- -------------------------------------------------------------
-- ORDERS TO IPOS-SA (orders the merchant places with InfoPharma)
-- CA-23 Order More Stock, CA-29 Place Order, CA-30 Track Status
-- CA-31 View Previous Orders, CA-32 Query Outstanding Balance
-- -------------------------------------------------------------
CREATE TABLE orders_to_sa (
    order_id        INT             PRIMARY KEY AUTO_INCREMENT,
    ipos_sa_order_ref VARCHAR(20),                         -- reference returned by IPOS-SA
    placed_by       INT             NOT NULL REFERENCES users(user_id),
    order_date      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount    DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    status          ENUM('PENDING','ACCEPTED','PROCESSING','DISPATCHED','DELIVERED') NOT NULL DEFAULT 'PENDING',
    dispatch_date   TIMESTAMP,
    delivery_date   TIMESTAMP,
    courier         VARCHAR(100),
    courier_ref     VARCHAR(100),
    expected_delivery DATE,
    payment_status  ENUM('PENDING','PAID')   NOT NULL DEFAULT 'PENDING',
    payment_date    TIMESTAMP
);

-- -------------------------------------------------------------
-- ORDER LINE ITEMS (items within each order to IPOS-SA)
-- -------------------------------------------------------------
CREATE TABLE order_items (
    order_item_id   INT             PRIMARY KEY AUTO_INCREMENT,
    order_id        INT             NOT NULL REFERENCES orders_to_sa(order_id),
    stock_item_id   INT             NOT NULL REFERENCES stock_items(stock_item_id),
    quantity        INT             NOT NULL,
    unit_cost       DECIMAL(10,2)   NOT NULL,               -- price at time of order
    line_total      DECIMAL(10,2)   GENERATED ALWAYS AS (quantity * unit_cost) STORED
);

-- -------------------------------------------------------------
-- SALES TRANSACTIONS (sales to customers in the pharmacy shop)
-- CA-13 Record Sale, CA-14 Accept Payment, CA-15 Cash Payment
-- CA-16 Card Payment, CA-17 Generate Receipt/Invoice
-- -------------------------------------------------------------
CREATE TABLE sales (
    sale_id             INT             PRIMARY KEY AUTO_INCREMENT,
    served_by           INT             NOT NULL REFERENCES users(user_id),
    holder_id           INT             REFERENCES account_holders(holder_id), -- NULL = occasional customer
    -- Customer details for occasional customers (no account)
    occasional_name     VARCHAR(100),
    sale_timestamp      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal            DECIMAL(10,2)   NOT NULL DEFAULT 0.00,  -- before VAT
    vat_amount          DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    discount_amount     DECIMAL(10,2)   NOT NULL DEFAULT 0.00,
    total_amount        DECIMAL(10,2)   NOT NULL DEFAULT 0.00,  -- final amount due
    -- Payment details (CA-14, CA-15, CA-16)
    payment_method      ENUM('CASH','CARD','ACCOUNT') NOT NULL,  -- ACCOUNT = account holder on credit
    payment_received    DECIMAL(10,2),                           -- amount tendered (cash)
    change_given        DECIMAL(10,2),
    -- Card details (CA-16; store first 4 + last 4 digits only, not full number)
    card_type           VARCHAR(20),                             -- e.g. VISA, MASTERCARD
    card_first_four     CHAR(4),
    card_last_four      CHAR(4),
    card_expiry         CHAR(5),                                 -- MM/YY
    invoice_number      VARCHAR(20)     UNIQUE,                  -- generated invoice ref
    pu_reconciled       BOOLEAN         DEFAULT FALSE            -- TRUE once IPOS-PU has cleared the card payment
);

-- -------------------------------------------------------------
-- SALE LINE ITEMS
-- -------------------------------------------------------------
CREATE TABLE sale_items (
    sale_item_id    INT             PRIMARY KEY AUTO_INCREMENT,
    sale_id         INT             NOT NULL REFERENCES sales(sale_id),
    stock_item_id   INT             NOT NULL REFERENCES stock_items(stock_item_id),
    quantity        INT             NOT NULL,
    unit_price      DECIMAL(10,2)   NOT NULL,   -- retail price (bulk_cost * markup) at time of sale
    line_total      DECIMAL(10,2)   GENERATED ALWAYS AS (quantity * unit_price) STORED
);

-- -------------------------------------------------------------
-- REMINDERS (CA-11 1st Reminder, CA-12 2nd Reminder)
-- Tracks which reminders have been generated per account holder
-- -------------------------------------------------------------
CREATE TABLE reminders (
    reminder_id     INT             PRIMARY KEY AUTO_INCREMENT,
    holder_id       INT             NOT NULL REFERENCES account_holders(holder_id),
    reminder_type   ENUM('FIRST','SECOND') NOT NULL,
    generated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_due_by  DATE            NOT NULL,   -- current_date + 7 days as per spec
    amount_owed     DECIMAL(10,2)   NOT NULL,
    -- Link to the specific sale/invoice this reminder concerns
    sale_id         INT             REFERENCES sales(sale_id),
    sent            BOOLEAN         NOT NULL DEFAULT FALSE
);

-- -------------------------------------------------------------
-- TEMPLATES (CA-35 Update Reminder Template, CA-36 Update Receipt/Invoice Template)
-- Stores editable text templates used when generating reminders and receipts
-- -------------------------------------------------------------
CREATE TABLE templates (
    template_id     INT             PRIMARY KEY AUTO_INCREMENT,
    template_type   ENUM('FIRST_REMINDER','SECOND_REMINDER','RECEIPT') NOT NULL UNIQUE,
    template_body   TEXT            NOT NULL,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Default reminder templates (matching layout in Student Brief Appendix 6)
INSERT INTO templates (template_type, template_body) VALUES
('FIRST_REMINDER',
 'Dear {HOLDER_NAME},\n\nREMINDER - INVOICE NO.: {INVOICE_NO}\nIPOS Account: {ACCOUNT_NO}  Total Amount: {AMOUNT}\n\nAccording to our records, it appears that we have not yet received payment of the above invoice, which was raised against {HOLDER_NAME} on {INVOICE_DATE}.\n\nWe would appreciate payment at your earliest convenience.\nPayment is due by: {PAYMENT_DUE_DATE}\n\nIf you have already sent a payment to us recently, please accept our apologies.\n\nYours sincerely,\n{PHARMACIST_NAME}'),
('SECOND_REMINDER',
 'Dear {HOLDER_NAME},\n\nSECOND REMINDER - INVOICE NO.: {INVOICE_NO}\nIPOS Account: {ACCOUNT_NO}  Total Amount: {AMOUNT}\n\nIt appears that we still have not yet received payment of the above invoice, despite the reminder sent to you on {FIRST_REMINDER_DATE}.\n\nWe would appreciate it if you would settle this invoice in full by return.\nPayment is due by: {PAYMENT_DUE_DATE}\n\nIf you have already sent a payment to us recently, please accept our apologies.\n\nYours sincerely,\n{PHARMACIST_NAME}'),
('RECEIPT',
 'INVOICE NO.: {INVOICE_NO}\nDate: {DATE}\n\nCustomer: {CUSTOMER_NAME}\nAccount No: {ACCOUNT_NO}\n\n{ITEMS_TABLE}\n\nSubtotal:        £{SUBTOTAL}\nVAT @ {VAT_RATE}%:  £{VAT_AMOUNT}\nAmount Due:      £{TOTAL}\n\nThank you for your valued custom.\n\n{PHARMACIST_NAME}');

-- -------------------------------------------------------------
-- INDEXES for performance on common queries
-- -------------------------------------------------------------
CREATE INDEX idx_account_holders_status   ON account_holders(account_status);
CREATE INDEX idx_sales_timestamp          ON sales(sale_timestamp);
CREATE INDEX idx_sales_holder             ON sales(holder_id);
CREATE INDEX idx_stock_sa_item_id         ON stock_items(sa_item_id);
CREATE INDEX idx_orders_status            ON orders_to_sa(status);
CREATE INDEX idx_reminders_holder         ON reminders(holder_id);
