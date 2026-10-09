INSERT INTO categories (id, name, system_key, sort_order, created_at, updated_at, created_by)
SELECT gen_random_uuid(), v.name, v.sys_key, v.ord, now(), now(), 'system'
FROM (VALUES
  ('Travel',             'TRAVEL',             1),
  ('Food & Dining',      'FOOD_DINING',        2),
  ('EMIs',               'EMI',                3),
  ('Party',              'PARTY',              4),
  ('Festivals',          'FESTIVALS',          5),
  ('Pooja Items',        'POOJA',              6),
  ('Salary',             'SALARY',             7),
  ('Investment Returns', 'INVESTMENT_RETURNS', 8),
  ('Other',              'OTHER',              99)
) AS v(name, sys_key, ord);

INSERT INTO sub_categories (id, name, system_key, sort_order, created_at, updated_at, created_by)
SELECT gen_random_uuid(), v.name, v.sys_key, v.ord, now(), now(), 'system'
FROM (VALUES
  ('Flights',            'FLIGHTS',          1),
  ('Train & Bus',        'TRAIN_BUS',        2),
  ('Cab & Fuel',         'CAB_FUEL',         3),
  ('Hotels & Stay',      'HOTEL_STAY',       4),
  ('Groceries',          'GROCERIES',        5),
  ('Restaurants',        'RESTAURANTS',      6),
  ('Online Delivery',    'ONLINE_DELIVERY',  7),
  ('Coffee & Snacks',    'COFFEE_SNACKS',    8),
  ('Home Loan',          'HOME_LOAN',        9),
  ('Vehicle Loan',       'VEHICLE_LOAN',    10),
  ('Personal Loan',      'PERSONAL_LOAN',   11),
  ('Credit Card EMI',    'CARD_EMI',        12),
  ('Birthday',           'BIRTHDAY',        13),
  ('Get-togethers',      'GET_TOGETHER',    14),
  ('Gifts',              'GIFTS',           15),
  ('Decorations',        'DECORATIONS',     16),
  ('Diwali',             'DIWALI',          17),
  ('Eid',                'EID',             18),
  ('Christmas',          'CHRISTMAS',       19),
  ('Flowers & Garlands', 'FLOWERS',         20),
  ('Pooja Samagri',      'POOJA_SAMAGRI',   21),
  ('Temple Offerings',   'TEMPLE_OFFERINGS',22),
  ('Interest',           'INTEREST',        23),
  ('Dividends',          'DIVIDENDS',       24)
) AS v(name, sys_key, ord);

INSERT INTO category_sub_categories (category_id, sub_category_id)
SELECT c.id, s.id
FROM (VALUES
  ('TRAVEL','FLIGHTS'), ('TRAVEL','TRAIN_BUS'), ('TRAVEL','CAB_FUEL'), ('TRAVEL','HOTEL_STAY'),
  ('FOOD_DINING','GROCERIES'), ('FOOD_DINING','RESTAURANTS'),
  ('FOOD_DINING','ONLINE_DELIVERY'), ('FOOD_DINING','COFFEE_SNACKS'),
  ('EMI','HOME_LOAN'), ('EMI','VEHICLE_LOAN'), ('EMI','PERSONAL_LOAN'), ('EMI','CARD_EMI'),
  ('PARTY','BIRTHDAY'), ('PARTY','GET_TOGETHER'), ('PARTY','GIFTS'), ('PARTY','DECORATIONS'),
  ('FESTIVALS','DIWALI'), ('FESTIVALS','EID'), ('FESTIVALS','CHRISTMAS'),
  ('FESTIVALS','GIFTS'), ('FESTIVALS','DECORATIONS'), ('FESTIVALS','FLOWERS'),
  ('POOJA','FLOWERS'), ('POOJA','POOJA_SAMAGRI'), ('POOJA','TEMPLE_OFFERINGS'),
  ('INVESTMENT_RETURNS','INTEREST'), ('INVESTMENT_RETURNS','DIVIDENDS')
) AS v(cat_key, sub_key)
JOIN categories c     ON c.system_key = v.cat_key
JOIN sub_categories s ON s.system_key = v.sub_key;
