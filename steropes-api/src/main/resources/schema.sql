DROP TABLE IF EXISTS accounts CASCADE;

CREATE TABLE IF NOT EXISTS accounts (
    account_id UUID NOT NULL DEFAULT gen_random_uuid(),
    branch_number VARCHAR(3) NOT NULL CHECK (branch_number ~ '^[0-9]{3}$'),
    account_number VARCHAR(7) NOT NULL CHECK (account_number ~ '^[0-9]{7}$'),
    max_balance NUMERIC(18, 2) NOT NULL CHECK (max_balance >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id)
);

DROP TABLE IF EXISTS account_transactions CASCADE;

CREATE TABLE IF NOT EXISTS account_transactions (
    transaction_id UUID NOT NULL DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL CHECK (sequence_number >= 0),
    amount NUMERIC(19, 2) NOT NULL,
    new_balance NUMERIC(19, 2) NOT NULL CHECK (new_balance >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_id, sequence_number),
    PRIMARY KEY (transaction_id)
);