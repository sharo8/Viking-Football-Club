CREATE TABLE finance_expenses (
    id ${identity} PRIMARY KEY,
    category VARCHAR(80) NOT NULL,
    subcategory VARCHAR(80),
    description VARCHAR(400) NOT NULL,
    amount DECIMAL(14, 0) NOT NULL,
    expense_date DATE NOT NULL,
    vendor_name VARCHAR(160),
    vendor_phone VARCHAR(40),
    method VARCHAR(30),
    status VARCHAR(20) NOT NULL,
    note VARCHAR(500),
    linked_label VARCHAR(160),
    reversed BOOLEAN NOT NULL DEFAULT FALSE,
    reverse_reason VARCHAR(400),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE finance_receipts (
    id ${identity} PRIMARY KEY,
    receipt_number VARCHAR(40) NOT NULL,
    person_type VARCHAR(20) NOT NULL,
    person_id BIGINT NOT NULL,
    person_name VARCHAR(160) NOT NULL,
    payment_id BIGINT,
    amount DECIMAL(14, 0) NOT NULL,
    status VARCHAR(30) NOT NULL,
    finance_signer VARCHAR(160),
    recipient_signer VARCHAR(160),
    signed_finance_at TIMESTAMP,
    signed_recipient_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_finance_receipt_number UNIQUE (receipt_number)
);

CREATE TABLE finance_report_runs (
    id ${identity} PRIMARY KEY,
    report_what VARCHAR(40) NOT NULL,
    report_who VARCHAR(40) NOT NULL,
    period_label VARCHAR(80) NOT NULL,
    split_mode VARCHAR(20) NOT NULL,
    detail_level VARCHAR(20) NOT NULL,
    format VARCHAR(10) NOT NULL,
    byte_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    version BIGINT NOT NULL DEFAULT 0
);
