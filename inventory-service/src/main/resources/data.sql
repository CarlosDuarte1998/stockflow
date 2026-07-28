-- Seed inicial: 12 productos distribuidos en 4 categorias.
-- Algunos productos arrancan con currentStock <= minStock a proposito
-- para poder validar el endpoint de alertas sin necesidad de registrar movimientos primero.

INSERT INTO products (id, sku, name, category, current_stock, min_stock, unit_price) VALUES
  (1,  'ELEC-001', 'Mouse inalambrico',           'ELECTRONICA', 45, 10, 12.99),
  (2,  'ELEC-002', 'Teclado mecanico',             'ELECTRONICA', 8,  10, 45.50),
  (3,  'ELEC-003', 'Monitor 24"',                  'ELECTRONICA', 15, 5,  159.00),
  (4,  'ELEC-004', 'Cable HDMI 2m',                'ELECTRONICA', 3,  15, 6.75),
  (5,  'ALIM-001', 'Cafe molido 500g',             'ALIMENTOS',   60, 20, 4.20),
  (6,  'ALIM-002', 'Azucar 1kg',                   'ALIMENTOS',   12, 12, 1.35),
  (7,  'ALIM-003', 'Aceite vegetal 1L',            'ALIMENTOS',   5,  10, 3.10),
  (8,  'FERR-001', 'Martillo carpintero',          'FERRETERIA',  22, 8,  14.00),
  (9,  'FERR-002', 'Caja de tornillos (100u)',     'FERRETERIA',  30, 15, 5.99),
  (10, 'FERR-003', 'Cinta metrica 5m',             'FERRETERIA',  0,  6,  3.50),
  (11, 'OFIC-001', 'Resma papel bond carta',       'OFICINA',     100, 25, 4.80),
  (12, 'OFIC-002', 'Caja de boligrafos (12u)',     'OFICINA',     18, 20, 6.25);
