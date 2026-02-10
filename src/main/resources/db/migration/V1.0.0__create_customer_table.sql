CREATE TABLE customers (
    customer_id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    document_type VARCHAR(10)  NOT NULL,
    document_id   VARCHAR(20)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    date_of_birth DATE,
    address       VARCHAR(500),
    status        VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP,

    CONSTRAINT uk_customers_document UNIQUE (document_type, document_id),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT chk_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING')),
    CONSTRAINT chk_customers_document_type CHECK (document_type IN ('DNI', 'PASSPORT', 'CEDULA', 'RUC'))
);

CREATE INDEX idx_customers_status ON customers (status);
CREATE INDEX idx_customers_document_id ON customers (document_id);
CREATE INDEX idx_customers_email ON customers (email);
