DROP TABLE IF EXISTS accounts CASCADE;
CREATE TABLE IF NOT EXISTS accounts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    branch_code VARCHAR(3) NOT NULL CHECK (branch_code ~ '^[0-9]{3}$'),
    account_number VARCHAR(7) NOT NULL CHECK (account_number ~ '^[0-9]{7}$'),
    max_balance NUMERIC(18, 2) NOT NULL CHECK (max_balance >= 0),
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS transactions CASCADE;
CREATE TABLE IF NOT EXISTS transactions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    new_balance NUMERIC(19, 2) NOT NULL,
    PRIMARY KEY (id)
); 