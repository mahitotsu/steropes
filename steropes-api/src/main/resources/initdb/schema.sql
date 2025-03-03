DROP TABLE IF EXISTS account;
CREATE TABLE IF NOT EXISTS account (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    branch_number VARCHAR(3) NOT NULL CHECK (branch_number ~ '^[0-9]{3}$'),
    account_number VARCHAR(7) NOT NULL CHECK (account_number ~ '^[0-9]{7}$'),
    max_balance NUMERIC(15, 2) NOT NULL CHECK (max_balance >= 0),
    UNIQUE (branch_number, account_number),
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS account_transaction;
CREATE TABLE IF NOT EXISTS account_transaction (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    branch_number VARCHAR(3) NOT NULL CHECK (branch_number ~ '^[0-9]{3}$'),
    account_number VARCHAR(7) NOT NULL CHECK (account_number ~ '^[0-9]{7}$'),
    sequence_number INTEGER NOT NULL CHECK (sequence_number > 0),
    amount NUMERIC(15, 2) NOT NULL,
    new_balance NUMERIC(15, 2) NOT NULL CHECK (new_balance >= 0),
    UNIQUE (branch_number, account_number, sequence_number),
    PRIMARY KEY (id)
);

CREATE INDEX idx_branch_account ON account_transaction (branch_number, account_number);