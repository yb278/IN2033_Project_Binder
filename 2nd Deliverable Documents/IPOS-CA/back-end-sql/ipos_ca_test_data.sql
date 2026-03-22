-- =============================================================
-- Test data for main table
-- =============================================================

USE ipos_ca;

-- =============================================================
-- 1. MERCHANT SETTINGS (already inserted by schema, just update)
-- =============================================================
UPDATE merchant_settings SET
    pharmacy_name  = 'Cosymed Ltd.',
    address_line1  = '3 High Level Drive',
    city           = 'Sydenham',
    postcode       = 'SE26 3ET',
    phone          = '0208 778 0124',
    fax            = '0208 778 0125',
    email          = 'orders@cosymed.co.uk',
    vat_rate       = 20.00
WHERE id = 1;

-- =============================================================
-- 2. USERS — staff accounts (CA-01, CA-03, CA-08)
-- Passwords are plain text for the prototype
-- In production these would be BCrypt hashes
-- =============================================================

-- Already have: admin / admin123
-- Add a pharmacist and a manager for testing

INSERT INTO users (username, password_hash, first_name, last_name, role, is_active) VALUES
('jfaith',    'pharma123',   'Jane',    'Faith',    'PHARMACIST', TRUE),
('apetite',   'manager123',  'Alice',   'Petite',   'MANAGER',    TRUE),
('bsmith',    'pharma123',   'Bob',     'Smith',    'PHARMACIST', TRUE),
('inactive1', 'old123',      'Old',     'Staff',    'PHARMACIST', FALSE);  -- deactivated account for testing CA-04

-- =============================================================
-- 3. DISCOUNT PLANS (already inserted by schema, add one more)
-- plan_id 1 = No Discount
-- plan_id 2 = 5% Flat
-- plan_id 3 = Monthly Volume (1%/2%/3%)
-- =============================================================

INSERT INTO discount_plans (plan_name, plan_type, fixed_rate) VALUES
('10% Loyal Customer', 'FIXED', 10.00);
-- plan_id 4 = 10% loyal customer discount

-- =============================================================
-- 4. STOCK ITEMS — from Appendix 1 (Catalogue Layout)
-- bulk_cost = the Package Cost from InfoPharma catalogue
-- markup_rate = 40% retail markup (reasonable pharmacy rate)
-- min_stock_level = Stock limit from Appendix 1
-- =============================================================

INSERT INTO stock_items (sa_item_id, description, package_type, unit, units_per_pack, bulk_cost, markup_rate, quantity_available, min_stock_level) VALUES
('100 00001', 'Paracetamol',           'box',    'Caps', 20,  0.10,  40.00, 10345, 300),
('100 00002', 'Aspirin',               'box',    'Caps', 20,  0.50,  40.00, 12453, 500),
('100 00003', 'Analgin',               'box',    'Caps', 10,  1.20,  40.00,  4235, 200),
('100 00004', 'Celebrex caps 100mg',   'box',    'Caps', 10, 10.00,  35.00,  3420, 200),
('100 00005', 'Celebrex caps 200mg',   'box',    'Caps', 10, 18.50,  35.00,  1450, 150),
('100 00006', 'Retin-A Tretin 30g',    'box',    'Caps', 20, 25.00,  30.00,  2013, 200),
('100 00007', 'Lipitor TB 20mg',       'box',    'Caps', 30, 15.50,  30.00,  1562, 200),
('100 00008', 'Claritin CR 60g',       'box',    'Caps', 20, 19.50,  30.00,  2540, 200),
('200 00004', 'Iodine Tincture',       'bottle', 'ml',  100,  0.30,  50.00,  2213, 200),
('200 00005', 'Rhynol',                'bottle', 'ml',  200,  2.50,  45.00,  1908, 300),
('300 00001', 'Ospen',                 'box',    'Caps', 20, 10.50,  35.00,   809, 200),
('300 00002', 'Amopen',                'box',    'Caps', 30, 15.00,  35.00,  1340, 300),
('400 00001', 'Vitamin C',             'box',    'Caps', 30,  1.20,  40.00,  3258, 300),
('400 00002', 'Vitamin B12',           'box',    'Caps', 30,  1.30,  40.00,  2673, 300);

-- Low stock items for testing CA-20 (quantity below min_stock_level)
INSERT INTO stock_items (sa_item_id, description, package_type, unit, units_per_pack, bulk_cost, markup_rate, quantity_available, min_stock_level) VALUES
('100 00009', 'Ibuprofen 200mg',       'box',    'Caps', 24,  0.80,  40.00,   150, 300),  -- LOW: 150 < 300
('200 00006', 'Cough Syrup 100ml',     'bottle', 'ml',  100,  3.20,  45.00,    80, 200);  -- LOW: 80 < 200

-- =============================================================
-- 5. ACCOUNT HOLDERS — customers with credit accounts
-- Based on Cosymed merchant client from Appendix 4/5/6/7
-- =============================================================

INSERT INTO account_holders
    (first_name, last_name, date_of_birth, email, phone,
     address_line1, city, postcode,
     account_status, credit_limit, outstanding_balance,
     discount_plan_id, monthly_order_total,
     status_1st_reminder, status_2nd_reminder,
     date_1st_reminder, date_2nd_reminder,
     payment_due_date)
VALUES

-- Normal account, no outstanding balance
('James',   'Smith',   '1985-03-15', 'j.smith@email.com',    '07700 900123',
 '27 Sainsbury Close', 'Stratford', 'E15 1AB',
 'NORMAL', 500.00, 0.00,
 2, 0.00,
 'no_need', 'no_need', NULL, NULL, NULL),

-- Normal account with outstanding balance — reminder due soon
('Sarah',   'Johnson', '1990-07-22', 'sarah.j@email.com',    '07700 900456',
 '14 Park Road', 'London', 'SE1 4TW',
 'NORMAL', 750.00, 125.49,
 3, 350.00,
 'due', 'no_need', NULL, NULL, '2026-03-15'),

-- Suspended account — overdue by 15+ days
('Michael', 'Brown',   '1978-11-08', 'm.brown@email.com',    '07700 900789',
 '8 Church Lane', 'Peckham', 'SE15 3HJ',
 'SUSPENDED', 600.00, 294.00,
 1, 0.00,
 'sent', 'due', '2026-02-20', '2026-03-07', '2026-02-28'),

-- In default — overdue by 30+ days
('Emily',   'Davis',   '1995-04-30', 'e.davis@email.com',    '07700 900321',
 '3 Station Road', 'Lewisham', 'SE13 7HQ',
 'IN_DEFAULT', 400.00, 506.80,
 1, 0.00,
 'sent', 'sent', '2026-01-20', '2026-02-04', '2026-01-31'),

-- Normal account — loyal customer with 10% discount
('Robert',  'Wilson',  '1965-09-12', 'r.wilson@email.com',   '07700 900654',
 '52 High Street', 'Forest Hill', 'SE23 1PX',
 'NORMAL', 1000.00, 0.00,
 4, 1250.00,
 'no_need', 'no_need', NULL, NULL, NULL),

-- Normal account — flexible discount, high spender
('Lisa',    'Taylor',  '1988-12-01', 'l.taylor@email.com',   '07700 900987',
 '19 Elm Avenue', 'Dulwich', 'SE21 7BN',
 'NORMAL', 800.00, 75.00,
 3, 1800.00,
 'no_need', 'no_need', NULL, NULL, NULL);

-- =============================================================
-- 6. ACCOUNT HOLDER PAYMENTS — payment history (CA-10)
-- =============================================================

INSERT INTO account_holder_payments (holder_id, amount, payment_date, notes) VALUES
(1, 125.49, '2026-02-15 10:30:00', 'Full payment for January balance'),
(1, 250.00, '2026-01-20 14:15:00', 'Partial payment'),
(2,  50.00, '2026-03-01 09:00:00', 'Partial payment'),
(5, 300.00, '2026-02-28 11:45:00', 'Monthly clearance'),
(6,  75.00, '2026-03-10 16:00:00', 'Card payment');

-- =============================================================
-- 7. ORDERS TO IPOS-SA — from Appendix 4 (Merchant's Orders Summary)
-- Report Period: 01/01/2026 – 31/01/2026
-- =============================================================

INSERT INTO orders_to_sa
    (ipos_sa_order_ref, placed_by, order_date, total_amount,
     status, dispatch_date, delivery_date,
     courier, courier_ref, expected_delivery,
     payment_status, payment_date)
VALUES

-- IP2034 — delivered and paid (Appendix 4)
('IP2034', 2, '2026-01-12 09:00:00', 302.50,
 'DELIVERED', '2026-01-14 08:00:00', '2026-01-15 14:00:00',
 'Royal Mail', 'RM123456789GB', '2026-01-16',
 'PAID', '2026-01-20 10:00:00'),

-- IP2780 — delivered, payment pending
('IP2780', 2, '2026-01-17 10:30:00', 525.00,
 'DELIVERED', '2026-01-18 09:00:00', '2026-01-19 13:00:00',
 'DHL', 'DHL987654321', '2026-01-20',
 'PENDING', NULL),

-- IP3021 — still pending dispatch
('IP3021', 2, '2026-01-29 11:00:00', 750.30,
 'PENDING', NULL, NULL,
 NULL, NULL, NULL,
 'PENDING', NULL),

-- A recent order for demo day
('IP3105', 2, '2026-03-10 09:30:00', 508.60,
 'ACCEPTED', NULL, NULL,
 NULL, NULL, '2026-03-14',
 'PENDING', NULL);

-- =============================================================
-- 8. ORDER ITEMS — from Appendix 2 (Order Form) and Appendix 5
-- Linking to stock_items via stock_item_id
-- =============================================================

-- IP2034 items (from Appendix 5)
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
(1,  1, 10, 0.05),   -- Paracetamol
(1,  3, 20, 5.10),   -- Analgin (using catalogue price)
(1,  9, 10, 2.60),   -- Ibuprofen
(1, 10, 10, 17.40);  -- Rhynol

-- IP2780 items (from Appendix 5)
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
(2,  2, 10,  0.30),  -- Aspirin
(2,  4, 20, 10.20),  -- Celebrex 100mg
(2, 11, 30, 10.60);  -- Ospen

-- IP3021 items (from Appendix 5)
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
(3, 10, 30, 15.00),  -- Rhynol
(3, 11, 25, 10.00),  -- Ospen
(3, 13,  2, 25.15);  -- Vitamin C

-- IP3105 — the order form from Appendix 2
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
(4,  1, 10, 0.10),   -- Paracetamol      10 x 0.10 = 1.00
(4,  3, 20, 1.20),   -- Analgin          20 x 1.20 = 24.00
(4,  9, 20, 0.30),   -- Iodine Tincture  20 x 0.30 = 3.60  (mapped to stock_item_id 9)
(4, 10, 10, 2.50),   -- Rhynol           10 x 2.50 = 25.00
(4, 11, 10, 10.50),  -- Ospen            10 x 10.50 = 105.00
(4, 12, 20, 15.00),  -- Amopen           20 x 15.00 = 300.00
(4, 13, 20, 1.20),   -- Vitamin C        20 x 1.20 = 24.00
(4, 14, 20, 1.30);   -- Vitamin B12      20 x 1.30 = 26.00

-- =============================================================
-- 9. SALES — from Appendix 7 (Retail Invoice)
-- Invoice 197362 — James Smith, 18th December 2002 (using 2026 dates)
-- =============================================================

INSERT INTO sales
    (served_by, holder_id, occasional_name, sale_timestamp,
     subtotal, vat_amount, discount_amount, total_amount,
     payment_method, payment_received, change_given,
     card_type, card_first_four, card_last_four, card_expiry,
     invoice_number)
VALUES

-- Appendix 7: James Smith (account holder 1) — card payment
(2, 1, NULL, '2026-01-18 14:30:00',
 106.80, 18.69, 0.00, 125.49,
 'CARD', NULL, NULL,
 'VISA', '4532', '1234', '06/28',
 'INV-000001'),

-- Cash sale to an occasional customer
(2, NULL, 'Walk-in Customer', '2026-02-05 10:15:00',
 15.60, 3.12, 0.00, 18.72,
 'CASH', 20.00, 1.28,
 NULL, NULL, NULL, NULL,
 'INV-000002'),

-- Account sale to Sarah Johnson (holder 2) — goes on her account
(3, 2, NULL, '2026-02-20 16:00:00',
 104.57, 20.92, 5.23, 125.49,
 'ACCOUNT', NULL, NULL,
 NULL, NULL, NULL, NULL,
 'INV-000003'),

-- Cash sale — Robert Wilson (holder 5) — with 10% loyalty discount
(2, 5, NULL, '2026-03-01 11:30:00',
 84.00, 16.80, 8.40, 92.40,
 'CARD', NULL, NULL,
 'MASTERCARD', '5412', '9876', '11/27',
 'INV-000004'),

-- Recent sale for demo day testing
(2, 1, NULL, '2026-03-12 09:45:00',
 46.00, 9.20, 0.00, 55.20,
 'CASH', 60.00, 4.80,
 NULL, NULL, NULL, NULL,
 'INV-000005');

-- =============================================================
-- 10. SALE ITEMS — line items for each sale above
-- =============================================================

-- INV-000001 (Appendix 7 — James Smith)
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
(1,  1, 1, 0.50),   -- Paracetamol     1 pack  @ £0.50 retail
(1,  3, 2, 3.20),   -- Analgin         2 packs @ £3.20
(1,  9, 2, 0.80),   -- Iodine Tincture 2 packs @ £0.80
(1, 10, 5, 6.50),   -- Rhynol          5 packs @ £6.50
(1, 12, 2,30.00),   -- Amopen          2 packs @ £30.00
(1, 14, 2, 2.90);   -- Vitamin B12     2 packs @ £2.90

-- INV-000002 (Walk-in — Paracetamol + Aspirin)
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
(2, 1, 3, 0.14),    -- Paracetamol 3 packs
(2, 2, 2, 0.70);    -- Aspirin 2 packs

-- INV-000003 (Sarah Johnson — on account)
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
(3,  4, 2, 13.50),  -- Celebrex 100mg
(3, 13, 5,  1.68),  -- Vitamin C
(3,  7, 3, 20.15);  -- Lipitor

-- INV-000004 (Robert Wilson — loyalty discount)
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
(4,  5, 2, 24.98),  -- Celebrex 200mg
(4,  8, 2, 25.35);  -- Claritin

-- INV-000005 (James Smith — recent)
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
(5,  2, 4, 0.70),   -- Aspirin
(5, 13, 6, 1.68),   -- Vitamin C
(5, 14, 8, 1.82);   -- Vitamin B12

-- =============================================================
-- 11. REMINDERS — from Appendix 6
-- Michael Brown (holder 3) — 1st reminder sent, 2nd due
-- Emily Davis (holder 4)   — both reminders sent
-- =============================================================

INSERT INTO reminders (holder_id, reminder_type, generated_at, payment_due_by, amount_owed, sale_id, sent) VALUES
-- Michael Brown — 1st reminder (Appendix 6, first letter)
(3, 'FIRST',  '2026-02-20 09:00:00', '2026-02-27', 294.00, 3, TRUE),
-- Michael Brown — 2nd reminder (due but not yet generated — will be created by ReminderService)
(3, 'SECOND', '2026-03-07 09:00:00', '2026-03-14', 294.00, 3, FALSE),

-- Emily Davis — both reminders sent (Appendix 6, second letter layout)
(4, 'FIRST',  '2026-01-20 09:00:00', '2026-01-27', 506.80, NULL, TRUE),
(4, 'SECOND', '2026-02-04 09:00:00', '2026-02-11', 506.80, NULL, TRUE);

-- =============================================================
-- 12. QUICK VERIFICATION QUERIES
-- Run these to confirm data loaded correctly
-- =============================================================

-- Uncomment to run after loading:

-- SELECT 'users' as tbl, COUNT(*) as count FROM users
-- UNION SELECT 'account_holders', COUNT(*) FROM account_holders
-- UNION SELECT 'stock_items', COUNT(*) FROM stock_items
-- UNION SELECT 'orders_to_sa', COUNT(*) FROM orders_to_sa
-- UNION SELECT 'order_items', COUNT(*) FROM order_items
-- UNION SELECT 'sales', COUNT(*) FROM sales
-- UNION SELECT 'sale_items', COUNT(*) FROM sale_items
-- UNION SELECT 'reminders', COUNT(*) FROM reminders
-- UNION SELECT 'account_holder_payments', COUNT(*) FROM account_holder_payments;
