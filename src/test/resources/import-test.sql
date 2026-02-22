INSERT INTO customers (customer_id, first_name, last_name, document_type, document_id, email, phone, date_of_birth, status, created_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 'John', 'Doe', 'DNI', '12345678', 'john@test.com', '+1234567890', '1990-05-15', 'ACTIVE', CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, last_name, document_type, document_id, email, phone, status, created_at)
VALUES ('550e8400-e29b-41d4-a716-446655440001', 'Jane', 'Smith', 'PASSPORT', 'AB123456', 'jane@test.com', '+0987654321', 'PENDING', CURRENT_TIMESTAMP);

INSERT INTO customers (customer_id, first_name, last_name, document_type, document_id, email, status, created_at)
VALUES ('550e8400-e29b-41d4-a716-446655440002', 'Bob', 'Wilson', 'CEDULA', '87654321', 'bob@test.com', 'INACTIVE', CURRENT_TIMESTAMP);
