CREATE TABLE due_categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(80)  NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE due_subcategories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID         NOT NULL REFERENCES due_categories (id),
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(80)  NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_due_subcategory_category_code UNIQUE (category_id, code),
    CONSTRAINT uq_due_subcategories_id_category UNIQUE (id, category_id)
);

INSERT INTO due_categories (code, name, sort_order) VALUES
    ('SUBSCRIPTIONS', 'Subscriptions', 10),
    ('LENDING', 'Lending', 20),
    ('HOUSEHOLD', 'Household', 30),
    ('OTHER', 'Other', 999);

INSERT INTO due_subcategories (category_id, code, name, sort_order)
SELECT category.id, option.code, option.name, option.sort_order
FROM (VALUES
    ('SUBSCRIPTIONS', 'NETFLIX', 'Netflix', 10),
    ('SUBSCRIPTIONS', 'SPOTIFY', 'Spotify', 20),
    ('SUBSCRIPTIONS', 'YOUTUBE', 'YouTube', 30),
    ('SUBSCRIPTIONS', 'SIM_RECHARGE', 'SIM recharge', 40),
    ('SUBSCRIPTIONS', 'METRO', 'Metro', 50),
    ('SUBSCRIPTIONS', 'OTHER', 'Other subscription', 999),
    ('LENDING', 'PERSON', 'Person', 10),
    ('LENDING', 'FAMILY', 'Family', 20),
    ('LENDING', 'INVESTMENT', 'Investment', 30),
    ('LENDING', 'LOAN_EMI', 'Loan / EMI', 40),
    ('LENDING', 'CHIT', 'Chit', 50),
    ('LENDING', 'OTHER', 'Other lending', 999),
    ('HOUSEHOLD', 'GAS', 'Gas', 10),
    ('HOUSEHOLD', 'WATER', 'Water', 20),
    ('HOUSEHOLD', 'ELECTRICITY', 'Electricity', 30),
    ('HOUSEHOLD', 'RENT', 'Rent', 40),
    ('HOUSEHOLD', 'INTERNET', 'Internet', 50),
    ('HOUSEHOLD', 'MAINTENANCE', 'Maintenance', 60),
    ('HOUSEHOLD', 'OTHER', 'Other household', 999),
    ('OTHER', 'OTHER', 'Other', 10)
) AS option(category_code, code, name, sort_order)
JOIN due_categories category ON category.code = option.category_code;

ALTER TABLE dues DROP CONSTRAINT IF EXISTS dues_category_check;
ALTER TABLE dues ADD COLUMN category_id UUID;
ALTER TABLE dues ADD COLUMN sub_category_id UUID;

UPDATE dues due
SET category_id = category.id
FROM due_categories category
WHERE category.code = CASE due.category
    WHEN 'SUBSCRIPTIONS' THEN 'SUBSCRIPTIONS'
    WHEN 'HOUSING_UTILITIES' THEN 'HOUSEHOLD'
    WHEN 'FRIENDS_FAMILY' THEN 'LENDING'
    WHEN 'FAMILY_SUPPORT' THEN 'LENDING'
    WHEN 'DEBT_REPAYMENT' THEN 'LENDING'
    WHEN 'INVESTMENTS' THEN 'LENDING'
    WHEN 'CHIT_FUNDS' THEN 'LENDING'
    ELSE 'OTHER'
END;

ALTER TABLE dues ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE dues DROP COLUMN category;
ALTER TABLE dues
    ADD CONSTRAINT fk_dues_category
        FOREIGN KEY (category_id) REFERENCES due_categories (id),
    ADD CONSTRAINT fk_dues_subcategory_category
        FOREIGN KEY (sub_category_id, category_id)
        REFERENCES due_subcategories (id, category_id);

CREATE INDEX idx_due_subcategories_category
    ON due_subcategories (category_id, sort_order, name)
    WHERE active = TRUE;
CREATE INDEX idx_dues_user_category_subcategory_date
    ON dues (user_id, category_id, sub_category_id, due_date, id);
