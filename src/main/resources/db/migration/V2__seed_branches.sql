-- V2: Начальные данные — филиалы
INSERT INTO branches (name)
VALUES
    ('Филиал Бишкек — Восток'),
    ('Филиал Бишкек — Юг'),
    ('Филиал Ош — Центр')
ON CONFLICT (name) DO NOTHING;
