CREATE TABLE dues (
    id                  UUID PRIMARY KEY,
    user_id             UUID           NOT NULL REFERENCES users (id),
    title               VARCHAR(120)   NOT NULL,
    description         VARCHAR(1000),
    category            VARCHAR(30)    NOT NULL CHECK (category IN (
                            'HOUSING_UTILITIES', 'TRANSPORTATION', 'FOOD_DINING',
                            'HEALTHCARE', 'PERSONAL_CARE', 'SHOPPING',
                            'EDUCATION_CHILDCARE', 'ENTERTAINMENT', 'TRAVEL',
                            'INSURANCE', 'DEBT_REPAYMENT', 'TAXES_FEES',
                            'FAMILY_SUPPORT', 'FRIENDS_FAMILY', 'PETS',
                            'GIFTS_DONATIONS', 'BUSINESS_EXPENSES', 'SUBSCRIPTIONS',
                            'INVESTMENTS', 'CHIT_FUNDS', 'OTHER')),
    amount              NUMERIC(19, 4) NOT NULL CHECK (amount > 0),
    currency            VARCHAR(3)     NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    due_date            DATE           NOT NULL,
    frequency           VARCHAR(15)    NOT NULL CHECK (frequency IN (
                            'ONCE', 'DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY')),
    frequency_interval  INTEGER        NOT NULL CHECK (frequency_interval BETWEEN 1 AND 120),
    reminder_days_before INTEGER[]     NOT NULL DEFAULT ARRAY[1, 3, 7],
    payee_name          VARCHAR(120),
    reference           VARCHAR(100),
    auto_pay            BOOLEAN        NOT NULL DEFAULT FALSE,
    status              VARCHAR(15)    NOT NULL CHECK (status IN (
                            'ACTIVE', 'PAUSED', 'COMPLETED')),
    created_at          TIMESTAMPTZ    NOT NULL,
    updated_at          TIMESTAMPTZ    NOT NULL,
    created_by          VARCHAR(255),
    updated_by          VARCHAR(255),
    version             BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_dues_once_interval CHECK (frequency <> 'ONCE' OR frequency_interval = 1),
    CONSTRAINT ck_dues_reminder_days CHECK (
        array_position(reminder_days_before, NULL) IS NULL
        AND 0 <= ALL (reminder_days_before)
        AND 365 >= ALL (reminder_days_before)
    )
);

CREATE INDEX idx_dues_user_date_id ON dues (user_id, due_date, id);
CREATE INDEX idx_dues_user_status_date_id ON dues (user_id, status, due_date, id);
CREATE INDEX idx_dues_user_category_date_id ON dues (user_id, category, due_date, id);
