ALTER TABLE transactions
    ADD COLUMN payment_mode VARCHAR(20);

UPDATE transactions
SET payment_mode = 'CASH';

ALTER TABLE transactions
    ALTER COLUMN payment_mode SET NOT NULL,
    ADD CONSTRAINT chk_transactions_payment_mode
        CHECK (payment_mode IN ('CASH', 'CREDIT', 'BANK_ACCOUNT'));
