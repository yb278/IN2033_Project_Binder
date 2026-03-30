-- =============================================================
-- IPOS-CA Test Data
-- Source: IPOS_SampleData_2026 specification
-- Subsystem: Cosymed Ltd pharmacy (IPOS-CA instance)
-- Last revised: 20 March 2026  |  Version 1.0
-- =============================================================
-- IMPORTANT: Run against a clean database (all tables empty)
-- except for the single default admin login required by the schema.
-- =============================================================

USE ipos_ca;

-- =============================================================
-- 1. MERCHANT SETTINGS
-- Cosymed Ltd — the pharmacy running this IPOS-CA instance
-- =============================================================

UPDATE merchant_settings SET
                             pharmacy_name  = 'Cosymed Ltd',
                             address_line1  = '25 Bond Street',
                             city           = 'London',
                             postcode       = 'WC1V 8LS',
                             phone          = '0207 321 8001',
                             fax            = NULL,
                             email          = 'info@cosymed.co.uk',
                             vat_rate       = 0.00    -- 0% VAT applied to all retail items
WHERE id = 1;

-- =============================================================
-- 2. USERS — IPOS-CA staff accounts (p.7 of spec)
-- NOTE: role ENUM values ('ADMIN', 'MANAGER', 'ACCOUNTANT', 'CLERK')
--       must match those declared in your schema DDL. Adjust if needed.
-- Passwords stored as plain text for prototype only.
-- In production these must be BCrypt hashes.
-- =============================================================

-- Updated the schema's default admin account to sysdba
UPDATE users SET
                 username      = 'sysdba',
                 password_hash = 'masterkey',
                 first_name    =  'System',
                 last_name     = 'Administrator',
                 role          = 'ADMIN',
                 is_active     = TRUE
WHERE id = 1;

-- Remaining staff accounts
INSERT INTO users (username, password_hash, first_name, last_name, role, is_active) VALUES
                                                                                        ('manager',    'Get_it_done', 'Alex',   'Wright', 'MANAGER',    TRUE),  -- Director of Operations / Manager
                                                                                        ('accountant', 'Count_money', 'Claire', 'Stone',  'ACCOUNTANT', TRUE),  -- Senior Accountant
                                                                                        ('clerk',      'Paperwork',   'Tom',    'Baker',  'CLERK',      TRUE);  -- Accountant (clerk level)

-- =============================================================
-- 3. DISCOUNT PLANS
-- plan_id 1 = No Discount          (default — already inserted by schema)
-- plan_id 2 = Fixed 3%             (Eva Bauyer — ACC0001)
-- plan_id 3 = Variable Volume      (Glynne Morrison — ACC0002)
--             Tiers: < £100 → 0%,  £100–£300 → 1%,  £300+ → 2%
--             Tier thresholds and rates are enforced by application logic.
-- =============================================================

INSERT INTO discount_plans (plan_name, plan_type, fixed_rate) VALUES
                                                                  ('Fixed 3%',        'FIXED',  3.00),
                                                                  ('Variable Volume', 'VOLUME', 0.00);

-- =============================================================
-- 4. STOCK ITEMS — Cosymed Ltd local stock (spec p.8)
-- Retail price = bulk_cost × 2  (100% markup).  VAT = 0%.
-- stock_item_id values 1–14 are assigned by INSERT order below;
-- they are referenced explicitly in sections 8 and 10.
--
-- Low stock alert: Rhynol (id=10) has 14 packs available < limit of 15.
-- =============================================================

INSERT INTO stock_items
(sa_item_id, description, package_type, unit, units_per_pack,
 bulk_cost, markup_rate, quantity_available, min_stock_level)
VALUES
-- id=1
('100 00001', 'Paracetamol',         'box',    'Caps', 20,  0.10, 100.00,  121, 10),
-- id=2
('100 00002', 'Aspirin',             'box',    'Caps', 20,  0.50, 100.00,  201, 15),
-- id=3
('100 00003', 'Analgin',             'box',    'Caps', 10,  1.20, 100.00,   25, 10),
-- id=4
('100 00004', 'Celebrex caps 100mg', 'box',    'Caps', 10, 10.00, 100.00,   43, 10),
-- id=5
('100 00005', 'Celebrex caps 200mg', 'box',    'Caps', 10, 18.50, 100.00,   35,  5),
-- id=6
('100 00006', 'Retin-A Tretin 30g',  'box',    'Caps', 20, 25.00, 100.00,   28, 10),
-- id=7
('100 00007', 'Lipitor TB 20mg',     'box',    'Caps', 30, 15.50, 100.00,   10, 10),
-- id=8
('100 00008', 'Claritin CR 60g',     'box',    'Caps', 20, 19.50, 100.00,   21, 10),
-- id=9
('200 00004', 'Iodine Tincture',     'bottle', 'ml',  100,  0.30, 100.00,   35, 10),
-- id=10  *** BELOW MIN STOCK LEVEL: 14 < 15 ***
('200 00005', 'Rhynol',              'bottle', 'ml',  200,  2.50, 100.00,   14, 15),
-- id=11
('300 00001', 'Ospen',               'box',    'Caps', 20, 10.50, 100.00,   78, 10),
-- id=12
('300 00002', 'Amopen',              'box',    'Caps', 30, 15.00, 100.00,   90, 15),
-- id=13
('400 00001', 'Vitamin C',           'box',    'Caps', 30,  1.20, 100.00,   22, 15),
-- id=14
('400 00002', 'Vitamin B12',         'box',    'Caps', 30,  1.30, 100.00,   43, 15);

-- =============================================================
-- 5. ACCOUNT HOLDERS — customers registered with Cosymed Ltd
-- =============================================================
-- Retail price reference (bulk_cost × 2):
--   Ospen £21.00 | Vitamin C £2.40 | Amopen £30.00 | Vitamin B12 £2.60
--   Analgin £2.40 | Celebrex 100mg £20.00 | Aspirin £1.00 | Retin-A £50.00
--
-- ACC0001 Eva Bauyer:
--   1 Mar purchase (Scenario 10): subtotal £91.00 − 3% = £88.27
--   1 Apr purchase (Scenario 13): subtotal £73.40 − 3% = £71.20
--   Total outstanding: £159.47
--   Last payment received: 28 Feb 2026 (spec states "30 February", treated as 28 Feb)
--
-- ACC0002 Glynne Morrison:
--   5 Mar purchase (Scenario 12): subtotal £149.20 − 1% = £147.71
--   Paid in full 29 Mar 2026 by credit card (Scenario 14)
--   Outstanding balance: £0.00
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

-- ACC0001 — Eva Bauyer  (Fixed 3% discount, plan_id=2)
('Eva',    'Bauyer',   '1980-01-01', 'evabauyer@example.com',       '0207 321 8001',
 '1 Liverpool Street', 'London', 'EC2V 8NS',
 'NORMAL', 500.00, 159.47,
 2, 71.20,
 'due', 'no_need', NULL, NULL, '2026-03-31'),

-- ACC0002 — Glynne Morrison  (Variable Volume discount, plan_id=3)
('Glynne', 'Morrison', '1975-06-15', 'glynne.morrison@example.com', '0207 321 8001',
 '1 Liverpool Street', 'London', 'EC2V 8NS',
 'NORMAL', 500.00, 0.00,
 3, 0.00,
 'no_need', 'no_need', NULL, NULL, NULL);

-- =============================================================
-- 6. ACCOUNT HOLDER PAYMENTS
-- Scenario 14: Glynne Morrison paid full balance 29 Mar 2026, credit card
-- Scenario 15: Eva Bauyer's last payment was 28 Feb 2026 (balance then cleared)
-- =============================================================

INSERT INTO account_holder_payments (holder_id, amount, payment_date, notes) VALUES
-- Eva Bauyer — prior balance cleared before March purchases (amount not specified in spec)
(1,   0.00, '2026-02-28 10:00:00', 'Balance cleared; last payment before current outstanding period'),
-- Glynne Morrison — full balance cleared by credit card
(2, 147.71, '2026-03-29 14:00:00', 'Full balance of £147.71 cleared by credit card');

-- =============================================================
-- 7. ORDERS TO IPOS-SA — Cosymed Ltd orders placed with InfoPharma
-- Scenario 2:  25 Feb 2026, £376.00, delivered 26 Feb, DHL
-- Scenario 4:  10 Mar 2026, £430.00, delivered 12 Mar, InfoPharma courier
-- Scenario 9:  Both orders paid in full on 15 Mar 2026 by company credit card
-- placed_by = 2 (manager); adjust user ID if schema auto-increments differently.
-- =============================================================

INSERT INTO orders_to_sa
(ipos_sa_order_ref, placed_by, order_date, total_amount,
 status, dispatch_date, delivery_date,
 courier, courier_ref, expected_delivery,
 payment_status, payment_date)
VALUES

-- order_id=1 — Scenario 2
('SA-2026-0042', 2, '2026-02-25 09:00:00', 376.00,
 'DELIVERED', '2026-02-25 17:00:00', '2026-02-26 17:00:00',
 'DHL', 'DHL100042GB', '2026-02-26',
 'PAID', '2026-03-15 11:00:00'),

-- order_id=2 — Scenario 4
('SA-2026-0067', 2, '2026-03-10 10:00:00', 430.00,
 'DELIVERED', '2026-03-11 09:00:00', '2026-03-12 11:00:00',
 'InfoPharma Courier', 'IP-COURIER-067', '2026-03-12',
 'PAID', '2026-03-15 11:00:00');

-- =============================================================
-- 8. ORDER ITEMS — line items for Cosymed's IPOS-SA orders above
-- unit_cost = InfoPharma catalogue package cost (same as bulk_cost)
-- =============================================================

-- Order 1 items — Scenario 2 (total £376.00)
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
                                                                           (1,  1, 10,  0.10),   -- Paracetamol    10 × £0.10  =   £1.00
                                                                           (1,  3, 20,  1.20),   -- Analgin        20 × £1.20  =  £24.00
                                                                           (1, 10, 10,  2.50),   -- Rhynol         10 × £2.50  =  £25.00
                                                                           (1, 12, 20, 15.00),   -- Amopen         20 × £15.00 = £300.00
                                                                           (1, 14, 20,  1.30);   -- Vitamin B12    20 × £1.30  =  £26.00
--                                            Total:   £376.00

-- Order 2 items — Scenario 4 (total £430.00)
INSERT INTO order_items (order_id, stock_item_id, quantity, unit_cost) VALUES
                                                                           (2, 10, 10,  2.50),   -- Rhynol         10 × £2.50  =  £25.00
                                                                           (2, 11, 10, 10.50),   -- Ospen          10 × £10.50 = £105.00
                                                                           (2, 12, 20, 15.00);   -- Amopen         20 × £15.00 = £300.00
--                                            Total:   £430.00

-- =============================================================
-- 9. SALES — retail sales processed at Cosymed Ltd
-- Retail price = bulk_cost × 2.  VAT = 0% on all items.
-- served_by = 2 (manager); adjust user ID if schema increments differ.
--
-- Retail price quick-reference (bulk_cost × 2):
--   Paracetamol £0.20 | Aspirin £1.00    | Analgin £2.40
--   Celebrex 100mg £20.00 | Celebrex 200mg £37.00 | Retin-A £50.00
--   Lipitor £31.00 | Claritin £39.00 | Iodine Tincture £0.60
--   Rhynol £5.00   | Ospen £21.00    | Amopen £30.00
--   Vitamin C £2.40 | Vitamin B12 £2.60
--
-- sale_id  Scenario  Customer           Date    Method    Total
--       1     10     Eva Bauyer         1 Mar   ACCOUNT   £88.27
--       2     11a    Walk-in            3 Mar   CASH       £9.20
--       3     11b    Walk-in            3 Mar   CARD     £140.00
--       4     11c    Walk-in            3 Mar   CASH      £70.00
--       5     11d    Walk-in            3 Mar   CASH      £48.20
--       6     11e    Walk-in            3 Mar   CARD      £46.80
--       7     11f    Walk-in            3 Mar   CASH      £95.20
--       8     12     Glynne Morrison    5 Mar   ACCOUNT  £147.71
--       9     13     Eva Bauyer         1 Apr   ACCOUNT   £71.20
-- =============================================================

INSERT INTO sales
(served_by, holder_id, occasional_name, sale_timestamp,
 subtotal, vat_amount, discount_amount, total_amount,
 payment_method, payment_received, change_given,
 card_type, card_first_four, card_last_four, card_expiry,
 invoice_number)
VALUES

-- sale_id=1 — Scenario 10: Eva Bauyer, 1 Mar 2026, on account, 3% discount
-- Items: Ospen×1 (£21.00) + Vitamin C×2 (£4.80) + Amopen×2 (£60.00) + Vitamin B12×2 (£5.20)
-- Subtotal: £91.00  |  Discount 3%: £2.73  |  Total: £88.27
(2, 1, NULL, '2026-03-01 11:00:00',
 91.00, 0.00, 2.73, 88.27,
 'ACCOUNT', NULL, NULL,
 NULL, NULL, NULL, NULL,
 'INV-000001'),

-- sale_id=2 — Scenario 11a: Walk-in, 3 Mar 2026, cash
-- Items: Aspirin×2 (£2.00) + Analgin×3 (£7.20)
-- Subtotal: £9.20  |  Total: £9.20
(2, NULL, 'Walk-in Customer', '2026-03-03 10:00:00',
 9.20, 0.00, 0.00, 9.20,
 'CASH', 10.00, 0.80,
 NULL, NULL, NULL, NULL,
 'INV-000002'),

-- sale_id=3 — Scenario 11b: Walk-in, 3 Mar 2026, Visa credit card
-- Items: Celebrex 100mg×2 (£40.00) + Retin-A Tretin×2 (£100.00)
-- Subtotal: £140.00  |  Total: £140.00
(2, NULL, 'Walk-in Customer', '2026-03-03 10:30:00',
 140.00, 0.00, 0.00, 140.00,
 'CARD', NULL, NULL,
 'VISA', NULL, NULL, NULL,
 'INV-000003'),

-- sale_id=4 — Scenario 11c: Walk-in, 3 Mar 2026, cash
-- Items: Lipitor TB×1 (£31.00) + Claritin CR×1 (£39.00)
-- Subtotal: £70.00  |  Total: £70.00
(2, NULL, 'Walk-in Customer', '2026-03-03 11:00:00',
 70.00, 0.00, 0.00, 70.00,
 'CASH', 70.00, 0.00,
 NULL, NULL, NULL, NULL,
 'INV-000004'),

-- sale_id=5 — Scenario 11d: Walk-in, 3 Mar 2026, cash
-- Items: Celebrex 200mg×1 (£37.00) + Iodine Tincture×2 (£1.20) + Rhynol×2 (£10.00)
-- Subtotal: £48.20  |  Total: £48.20
(2, NULL, 'Walk-in Customer', '2026-03-03 11:30:00',
 48.20, 0.00, 0.00, 48.20,
 'CASH', 50.00, 1.80,
 NULL, NULL, NULL, NULL,
 'INV-000005'),

-- sale_id=6 — Scenario 11e: Walk-in, 3 Mar 2026, debit card
-- Items: Ospen×2 (£42.00) + Vitamin C×2 (£4.80)
-- Subtotal: £46.80  |  Total: £46.80
(2, NULL, 'Walk-in Customer', '2026-03-03 12:00:00',
 46.80, 0.00, 0.00, 46.80,
 'CARD', NULL, NULL,
 'DEBIT', NULL, NULL, NULL,
 'INV-000006'),

-- sale_id=7 — Scenario 11f: Walk-in, 3 Mar 2026, cash
-- Items: Amopen×3 (£90.00) + Vitamin B12×2 (£5.20)
-- Subtotal: £95.20  |  Total: £95.20
(2, NULL, 'Walk-in Customer', '2026-03-03 12:30:00',
 95.20, 0.00, 0.00, 95.20,
 'CASH', 100.00, 4.80,
 NULL, NULL, NULL, NULL,
 'INV-000007'),

-- sale_id=8 — Scenario 12: Glynne Morrison, 5 Mar 2026, on account
-- Items: Aspirin×2 (£2.00) + Analgin×3 (£7.20) + Celebrex 100mg×2 (£40.00) + Retin-A×2 (£100.00)
-- Subtotal: £149.20  |  Variable discount (£100–£300 bracket → 1%): £1.49  |  Total: £147.71
(2, 2, NULL, '2026-03-05 14:00:00',
 149.20, 0.00, 1.49, 147.71,
 'ACCOUNT', NULL, NULL,
 NULL, NULL, NULL, NULL,
 'INV-000008'),

-- sale_id=9 — Scenario 13: Eva Bauyer, 1 Apr 2026, on account, 3% discount
-- Items: Ospen×1 (£21.00) + Analgin×3 (£7.20) + Celebrex 100mg×2 (£40.00) + Vitamin B12×2 (£5.20)
-- Subtotal: £73.40  |  Discount 3%: £2.20  |  Total: £71.20
(2, 1, NULL, '2026-04-01 10:00:00',
 73.40, 0.00, 2.20, 71.20,
 'ACCOUNT', NULL, NULL,
 NULL, NULL, NULL, NULL,
 'INV-000009');

-- =============================================================
-- 10. SALE ITEMS — line items for each sale above
-- unit_price = retail price (bulk_cost × 2)
-- =============================================================

-- Sale 1 — Eva Bauyer, Scenario 10
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (1, 11, 1, 21.00),   -- Ospen           1 × £21.00 =  £21.00
                                                                          (1, 13, 2,  2.40),   -- Vitamin C       2 × £2.40  =   £4.80
                                                                          (1, 12, 2, 30.00),   -- Amopen          2 × £30.00 =  £60.00
                                                                          (1, 14, 2,  2.60);   -- Vitamin B12     2 × £2.60  =   £5.20
--                                           Total:    £91.00 (before discount)

-- Sale 2 — Walk-in cash, Scenario 11a
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (2, 2, 2, 1.00),     -- Aspirin         2 × £1.00  =   £2.00
                                                                          (2, 3, 3, 2.40);     -- Analgin         3 × £2.40  =   £7.20

-- Sale 3 — Walk-in Visa, Scenario 11b
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (3, 4, 2, 20.00),    -- Celebrex 100mg  2 × £20.00 =  £40.00
                                                                          (3, 6, 2, 50.00);    -- Retin-A Tretin  2 × £50.00 = £100.00

-- Sale 4 — Walk-in cash, Scenario 11c
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (4, 7, 1, 31.00),    -- Lipitor TB      1 × £31.00 =  £31.00
                                                                          (4, 8, 1, 39.00);    -- Claritin CR     1 × £39.00 =  £39.00

-- Sale 5 — Walk-in cash, Scenario 11d
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (5,  5, 1, 37.00),   -- Celebrex 200mg  1 × £37.00 =  £37.00
                                                                          (5,  9, 2,  0.60),   -- Iodine Tincture 2 × £0.60  =   £1.20
                                                                          (5, 10, 2,  5.00);   -- Rhynol          2 × £5.00  =  £10.00

-- Sale 6 — Walk-in debit card, Scenario 11e
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (6, 11, 2, 21.00),   -- Ospen           2 × £21.00 =  £42.00
                                                                          (6, 13, 2,  2.40);   -- Vitamin C       2 × £2.40  =   £4.80

-- Sale 7 — Walk-in cash, Scenario 11f
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (7, 12, 3, 30.00),   -- Amopen          3 × £30.00 =  £90.00
                                                                          (7, 14, 2,  2.60);   -- Vitamin B12     2 × £2.60  =   £5.20

-- Sale 8 — Glynne Morrison, Scenario 12
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (8,  2, 2,  1.00),   -- Aspirin         2 × £1.00  =   £2.00
                                                                          (8,  3, 3,  2.40),   -- Analgin         3 × £2.40  =   £7.20
                                                                          (8,  4, 2, 20.00),   -- Celebrex 100mg  2 × £20.00 =  £40.00
                                                                          (8,  6, 2, 50.00);   -- Retin-A Tretin  2 × £50.00 = £100.00
--                                           Total:   £149.20 (before discount)

-- Sale 9 — Eva Bauyer, Scenario 13
INSERT INTO sale_items (sale_id, stock_item_id, quantity, unit_price) VALUES
                                                                          (9, 11, 1, 21.00),   -- Ospen           1 × £21.00 =  £21.00
                                                                          (9,  3, 3,  2.40),   -- Analgin         3 × £2.40  =   £7.20
                                                                          (9,  4, 2, 20.00),   -- Celebrex 100mg  2 × £20.00 =  £40.00
                                                                          (9, 14, 2,  2.60);   -- Vitamin B12     2 × £2.60  =   £5.20
--                                           Total:    £73.40 (before discount)

-- =============================================================
-- 11. REMINDERS — Scenario 16
-- On 15 April 2026, reminding letters generated for all debtors.
-- On 29 April 2026, the same exercise is repeated (2nd reminder).
-- Only Eva Bauyer (holder_id=1) carries an outstanding balance (£159.47).
-- Glynne Morrison has cleared his balance (no reminder needed).
-- =============================================================

INSERT INTO reminders
(holder_id, reminder_type, generated_at, payment_due_by, amount_owed, sale_id, sent)
VALUES
-- Eva Bauyer — 1st reminder, 15 April 2026
(1, 'FIRST',  '2026-04-15 09:00:00', '2026-04-22', 159.47, NULL, TRUE),
-- Eva Bauyer — 2nd reminder, 29 April 2026
(1, 'SECOND', '2026-04-29 09:00:00', '2026-05-06', 159.47, NULL, TRUE);

-- =============================================================
-- 12. QUICK VERIFICATION QUERIES
-- Uncomment the block below and run after loading to confirm counts.
-- =============================================================

-- SELECT 'users'                 AS tbl, COUNT(*) AS count FROM users
-- UNION SELECT 'discount_plans',          COUNT(*) FROM discount_plans
-- UNION SELECT 'stock_items',             COUNT(*) FROM stock_items
-- UNION SELECT 'account_holders',         COUNT(*) FROM account_holders
-- UNION SELECT 'account_holder_payments', COUNT(*) FROM account_holder_payments
-- UNION SELECT 'orders_to_sa',            COUNT(*) FROM orders_to_sa
-- UNION SELECT 'order_items',             COUNT(*) FROM order_items
-- UNION SELECT 'sales',                   COUNT(*) FROM sales
-- UNION SELECT 'sale_items',              COUNT(*) FROM sale_items
-- UNION SELECT 'reminders',               COUNT(*) FROM reminders;

-- Expected row counts:
-- users:                  4  (sysdba, manager, accountant, clerk)
-- discount_plans:         3  (No Discount + Fixed 3% + Variable Volume)
-- stock_items:           14  (full CA catalogue from spec p.8)
-- account_holders:        2  (Eva Bauyer, Glynne Morrison)
-- account_holder_payments:2  (Eva Feb clearance, Glynne Mar payment)
-- orders_to_sa:           2  (Scenario 2: Feb, Scenario 4: Mar)
-- order_items:            8  (5 for order 1, 3 for order 2)
-- sales:                  9  (Scenarios 10, 11a–f, 12, 13)
-- sale_items:            25  (4+2+2+2+3+2+2+4+4)
-- reminders:              2  (Eva Bauyer: 15 Apr + 29 Apr)