-- V3: Прайс-лист аренды оборудования — категории, шаблоны, инструменты
-- Маппинг: daily_price = Сутки, deposit = Депозит
-- inventory_number: PREFIX-NNNN, глобально уникален через sequence

-- ── Вспомогательная последовательность ──────────────────────────────────────
CREATE SEQUENCE _v3_seq START 1;

-- ── Вспомогательная функция вставки ─────────────────────────────────────────
CREATE FUNCTION _v3_ins(
    p_branch_id BIGINT,
    p_cat_id    BIGINT,
    p_name      TEXT,
    p_daily     NUMERIC,
    p_deposit   NUMERIC
) RETURNS VOID LANGUAGE plpgsql AS $func$
DECLARE
    v_tid  BIGINT;
    v_pfx  TEXT;
    v_seq  TEXT;
BEGIN
    INSERT INTO tool_templates (name, category_id)
    VALUES (p_name, p_cat_id)
    RETURNING id INTO v_tid;

    v_pfx := LEFT(
        UPPER(REGEXP_REPLACE(p_name, '[^A-Za-zА-ЯЁа-яё0-9]', '', 'g')),
        4
    );
    v_seq := LPAD(nextval('_v3_seq')::TEXT, 4, '0');

    INSERT INTO tools (
        name, instance_number, inventory_number, article,
        deposit, daily_price, purchase_price,
        status, template_id, branch_id, created_at
    ) VALUES (
        p_name, 1,
        v_pfx || '-' || v_seq,
        v_pfx || '-A' || v_seq,
        p_deposit,
        p_daily,
        CASE WHEN p_deposit > 0 THEN p_deposit * 1.1 ELSE 0 END,
        'AVAILABLE',
        v_tid,
        p_branch_id,
        NOW()
    );
END;
$func$;

-- ── Основной блок ────────────────────────────────────────────────────────────
DO $do$
DECLARE
    bid BIGINT;
    cid BIGINT;
BEGIN
    SELECT id INTO bid FROM branches ORDER BY id LIMIT 1;

    -- 1. Алмазные установки и коронки
    INSERT INTO tool_categories (name) VALUES ('Алмазные установки и коронки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Алмазная установка (ручная)',              8000,  2000);
    PERFORM _v3_ins(bid, cid, 'Алмазная установка (стандарт, комплект)',  8000, 45000);
    PERFORM _v3_ins(bid, cid, 'Аренда алмазной коронки по кирпичу',       1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Алмазные коронки 40-63мм (за 1мм зуба)',   1000,  2500);
    PERFORM _v3_ins(bid, cid, 'Алмазные коронки 76-100мм (за 1мм зуба)',  1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Алмазные коронки 102-120мм (за 1мм зуба)', 1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Алмазные коронки 150-200мм (за 1мм зуба)', 1000,  6000);

    -- 2. Сварочное оборудование
    INSERT INTO tool_categories (name) VALUES ('Сварочное оборудование') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки пласт. труб HYNDAI',           1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки пласт. труб INGCO малый',      1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки пласт. труб Патриот большой',  1000,  6000);
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки ПНД труб диам 180мм',          2000, 45000);
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки ПНД труб диам 250мм (китай)',  5000, 75000);
    PERFORM _v3_ins(bid, cid, 'Аппарат для сварки ПНД труб диам 250мм',          5000, 75000);
    PERFORM _v3_ins(bid, cid, 'Сварка RILAND ARC 200 CT',                        1000, 11000);
    PERFORM _v3_ins(bid, cid, 'Сварка CAMEO MMA 250',                            1000, 11000);
    PERFORM _v3_ins(bid, cid, 'Сварка РЕОСТАТ черная 180А',                      1000, 11000);
    PERFORM _v3_ins(bid, cid, 'Сварка TCH MMA 300А черный',                      1000, 15000);
    PERFORM _v3_ins(bid, cid, 'Сварка полуавтомат ROLF',                         2000, 28000);
    PERFORM _v3_ins(bid, cid, 'Сварка полуавтомат Патриот',                      2000, 28000);
    PERFORM _v3_ins(bid, cid, 'Сварка полуавтомат Bolian NBC-270s',               2000, 28000);
    PERFORM _v3_ins(bid, cid, 'Сварка RILAND MMA 500G 380В 25кВт',               5000, 50000);
    PERFORM _v3_ins(bid, cid, 'Сварка RILAND MMA 500G 380В 25кВт PRO',           5000, 55000);

    -- 3. Асфальторезы
    INSERT INTO tool_categories (name) VALUES ('Асфальторезы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Асфальторез WAKER NEUSON 300мм',  5000,  80000);
    PERFORM _v3_ins(bid, cid, 'Асфальторез CUTTER 500мм',        5000,  80000);
    PERFORM _v3_ins(bid, cid, 'Асфальторез LONCIN 500мм',        5000,  80000);
    PERFORM _v3_ins(bid, cid, 'Асфальторез TOLSEN 500мм',        5000,  95000);
    PERFORM _v3_ins(bid, cid, 'Асфальторез диск 500мм (за 1мм)', 1000,   6000);

    -- 4. Мотоблоки
    INSERT INTO tool_categories (name) VALUES ('Мотоблоки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Набор для окучивания (мотоблок)',  1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Бензиновый мотоблок SHINEREAY',   4000, 50000);
    PERFORM _v3_ins(bid, cid, 'Бензиновый мотоблок Калуга',      4000, 50000);
    PERFORM _v3_ins(bid, cid, 'Бензиновый мотоблок Победа',      4000, 50000);
    PERFORM _v3_ins(bid, cid, 'Бензиновый мотоблок Урал',        4000, 50000);

    -- 5. Бетономешалки и миксеры
    INSERT INTO tool_categories (name) VALUES ('Бетономешалки и миксеры') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Бетономешалка 220 литров', 4000, 25000);
    PERFORM _v3_ins(bid, cid, 'Миксер BODA',              1000,  5500);

    -- 6. Болгарки (УШМ)
    INSERT INTO tool_categories (name) VALUES ('Болгарки (УШМ)') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Болгарка SAFUN 125',        1000, 8000);
    PERFORM _v3_ins(bid, cid, 'Болгарка TENIR аккум. 125', 1000, 8000);
    PERFORM _v3_ins(bid, cid, 'Болгарка ATEK 180',         1000, 5000);
    PERFORM _v3_ins(bid, cid, 'Болгарка SAFUN 230',        1000, 8000);

    -- 7. Буры и сверла
    INSERT INTO tool_categories (name) VALUES ('Буры и сверла') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Буры SDS MAX 10-18мм',  500,  1500);
    PERFORM _v3_ins(bid, cid, 'Буры SDS MAX 20-30мм', 2000,  4000);
    PERFORM _v3_ins(bid, cid, 'Буры SDS MAX 32-40мм', 2000,  6500);
    PERFORM _v3_ins(bid, cid, 'Буры на АНКОР',          500,  1000);

    -- 8. Вибраторы
    INSERT INTO tool_categories (name) VALUES ('Вибраторы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Вибратор 1.0-1.5м',        1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Вибратор глубинный 6м',    2000, 12000);
    PERFORM _v3_ins(bid, cid, 'Вибратор глубинный 3-4м',  2000, 12000);
    PERFORM _v3_ins(bid, cid, 'Вибратор Чемпион 4м',      2000, 12000);
    PERFORM _v3_ins(bid, cid, 'Вибробулова 1.5м',            0,  2500);
    PERFORM _v3_ins(bid, cid, 'Вибробулова 4.0м',            0,  5500);
    PERFORM _v3_ins(bid, cid, 'Вибробулова 6.0м',            0,  6500);
    PERFORM _v3_ins(bid, cid, 'Виброрейка бензиновая 2м',  4000, 38000);

    -- 9. Пилы
    INSERT INTO tool_categories (name) VALUES ('Пилы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Возвратно-поступательная пила Сабельная',   1000,  9000);
    PERFORM _v3_ins(bid, cid, 'Пила пчелка 110мм',                         1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Пила диск по алюминию',                     1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Пила отрезная по металлу BOSCH',            2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Пила отрезная по металлу KEN',              2000,  8500);
    PERFORM _v3_ins(bid, cid, 'Пила отрезная по металлу TOLSEN',           2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная KEHANG 255',               1000,  6500);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная INCCO малый асинхроник',   1000, 20000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная BODA 255',                 1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная EBI MAX 255',              1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная LANZAN 255',               1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная ROLF 255',                 1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная SUZUKI 255',               1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная WUJIE красный',            1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная по алюминию',              2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная SUZUKI 255 рельсовый',     2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная MAKITA LN1040F',           2000, 35000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная INCCO большой',            2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная Metabo 305 рельсовый',     2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная ALEM 305',                 2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная BODA 355',                 2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная HOTECHE 305 рельсовый',    2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Пила торцовочная Озчелик BETA плюс',        4000, 50000);
    PERFORM _v3_ins(bid, cid, 'Пила цепная STIHL 260',                     5000, 49000);
    PERFORM _v3_ins(bid, cid, 'Пила цепная STIHL 382',                     5000, 56000);
    PERFORM _v3_ins(bid, cid, 'Пила цепная STIHL 382 оригинальная цепь',   5000, 56000);
    PERFORM _v3_ins(bid, cid, 'Пила цепная электр. Китай',                 1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Пила циркулярная KEN HEANG 190мм',          1000,  8000);
    PERFORM _v3_ins(bid, cid, 'Лобзик электрический',                      1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Камнерезный станок',                        4000, 60000);

    -- 10. Воздуходувки
    INSERT INTO tool_categories (name) VALUES ('Воздуходувки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Воздуходувка электрическая',                    1000,  4500);
    PERFORM _v3_ins(bid, cid, 'Воздуходувка ранцевая бензиновая TOLSEN',       2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Воздуходувка ранцевая бензиновая 4-х тактная',  2000, 25000);

    -- 11. Газовые пушки и обогреватели
    INSERT INTO tool_categories (name) VALUES ('Газовые пушки и обогреватели') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Газовая пушка ROLF натяжные потолки',           1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Газовая пушка TYQ 15кВт',                       4000, 15000);
    PERFORM _v3_ins(bid, cid, 'Газовая пушка TYQ 30кВт',                       4000, 15000);
    PERFORM _v3_ins(bid, cid, 'Газовая пушка ПрофТепло 30кВт',                 4000, 20000);
    PERFORM _v3_ins(bid, cid, 'Газовая пушка ПрофТепло 57кВт',                 4000, 25000);
    PERFORM _v3_ins(bid, cid, 'Газовая пушка ПрофТепло 81кВт',                 4000, 30000);
    PERFORM _v3_ins(bid, cid, 'Газовый баллон без пушки или с горелкой',       1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Газовый инфракрасный обогреватель',             1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Горелка газовая большая комплект с баллоном',   1000,  8700);
    PERFORM _v3_ins(bid, cid, 'Горелка газовая малая комплект с баллоном',     1000,  8700);
    PERFORM _v3_ins(bid, cid, 'Электротепловентилятор 1-фазный 3кВт',          1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Электротепловентилятор 3-фазный 11кВт',         2000, 12000);
    PERFORM _v3_ins(bid, cid, 'Осушитель воздуха YAKE 80кв.м.',                5000, 70000);

    -- 12. Дизельные тепловые пушки
    INSERT INTO tool_categories (name) VALUES ('Дизельные тепловые пушки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 13кВт бак 18л',    4000, 20352);
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 30кВт бак 18л',    4000, 20352);
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 30кВт бак 18.5л',  4000, 20352);
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 40кВт бак 26л',    4000, 25000);
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 43кВт бак 55.5л',  4000, 25500);
    PERFORM _v3_ins(bid, cid, 'Диз. тепловая пушка 65кВт бак 55.5л',  4000, 45000);

    -- 13. Газонокосилки и триммеры
    INSERT INTO tool_categories (name) VALUES ('Газонокосилки и триммеры') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Газонокосилка бенз. 4-тактный триммер', 2000, 11000);
    PERFORM _v3_ins(bid, cid, 'Газонокосилка электр. ПРОРАБ',          2000,  8000);
    PERFORM _v3_ins(bid, cid, 'Газонокосилка электр. YWTT',            2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Газонокосилка электр. INGCO',           2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Кусторез STIHL',                        2000, 35000);
    PERFORM _v3_ins(bid, cid, 'Скарификатор аэратор HOTECHE',          2000, 11000);

    -- 14. Генераторы
    INSERT INTO tool_categories (name) VALUES ('Генераторы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Генератор 3.0 TOLSEN',       4000,  35000);
    PERFORM _v3_ins(bid, cid, 'Генератор 3.0 KEMAGE',       4000,  35000);
    PERFORM _v3_ins(bid, cid, 'Генератор 3.5',              4000,  35000);
    PERFORM _v3_ins(bid, cid, 'Генератор 3.5 HONDA',        4000,  35000);
    PERFORM _v3_ins(bid, cid, 'Генератор 5.5 KEMAGE',       5000,  50000);
    PERFORM _v3_ins(bid, cid, 'Генератор 7.0 KEMAGE',       5000,  55000);
    PERFORM _v3_ins(bid, cid, 'Генератор 8.0 TOLSEN',       5000,  65000);
    PERFORM _v3_ins(bid, cid, 'Генератор 8.5 KEMAGE',       5000,  65000);
    PERFORM _v3_ins(bid, cid, 'Генератор 3Ф 7.0 STILWELL',  5000,  70000);
    PERFORM _v3_ins(bid, cid, 'Генератор 3Ф 8.5 Kipor',     5000, 150000);

    -- 15. Компрессоры
    INSERT INTO tool_categories (name) VALUES ('Компрессоры') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Компрессор 50л',                          2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Компрессор 50л медицинский тихий',        2000, 20000);
    PERFORM _v3_ins(bid, cid, 'Компрессор 80л двухпоршневой',            4000, 40000);
    PERFORM _v3_ins(bid, cid, 'Компрессор MZB VA 70л двухпоршневой',     4000, 40000);
    PERFORM _v3_ins(bid, cid, 'Компрессор 90л трехпоршневой 10АТМ',      4000, 50000);
    PERFORM _v3_ins(bid, cid, 'Компрессор 90л трехпоршневой 10АТМ бенз.',4000, 50000);

    -- 16. Краскопульты
    INSERT INTO tool_categories (name) VALUES ('Краскопульты') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Краскопульт ручной известь опрыск.',  3000,  8000);
    PERFORM _v3_ins(bid, cid, 'Краскопульт безвоздушный JSPERFECT',  5000, 60000);
    PERFORM _v3_ins(bid, cid, 'Краскопульт безвоздушный PROFESSIONAL SPRAY', 5000, 60000);
    PERFORM _v3_ins(bid, cid, 'Краскопульт безвоздушный для эмали',  5000, 60000);
    PERFORM _v3_ins(bid, cid, 'Краскопульт безвоздушный мембранный', 5000, 40000);
    PERFORM _v3_ins(bid, cid, 'Хопер ковш',                          1000,  4000);

    -- 17. Пневматические пистолеты
    INSERT INTO tool_categories (name) VALUES ('Пневматические пистолеты') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Пистолет пневмо гвоздезабивной большой',    1000, 8000);
    PERFORM _v3_ins(bid, cid, 'Пистолет пневмо гвоздезабивной малый',      1000, 4500);
    PERFORM _v3_ins(bid, cid, 'Пистолет пневмо скобозабивной большой',     1000, 8000);
    PERFORM _v3_ins(bid, cid, 'Пистолет пневмо скобозабивной малый',       1000, 4500);
    PERFORM _v3_ins(bid, cid, 'Пистолет покрасочный',                       500, 1000);
    PERFORM _v3_ins(bid, cid, 'Пистолет покрасочный полупрофессиональный',  500, 1000);
    PERFORM _v3_ins(bid, cid, 'Пистолет покрасочный профессиональный',      500, 2500);
    PERFORM _v3_ins(bid, cid, 'Пистолет покрасочный большой водоэмульсия',  500, 2500);
    PERFORM _v3_ins(bid, cid, 'Пистолет продувочный',                       500, 4500);
    PERFORM _v3_ins(bid, cid, 'Пистолет текстурный',                        500, 3500);
    PERFORM _v3_ins(bid, cid, 'Набор пневмоинструмента',                   1000, 4000);
    PERFORM _v3_ins(bid, cid, 'Заклепачник пневматический',                1000, 9500);
    PERFORM _v3_ins(bid, cid, 'Пистолет для антикоррозийной обработки',    1000, 4500);

    -- 18. Плазморезы
    INSERT INTO tool_categories (name) VALUES ('Плазморезы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Плазморез LGK ABM 160А 380В до 55мм', 5000,  90000);
    PERFORM _v3_ins(bid, cid, 'Плазморез LGK 130А 380В до 55мм',     5000,  75000);
    PERFORM _v3_ins(bid, cid, 'Плазморез LGK 120А 380В до 45мм',     5000,  60000);
    PERFORM _v3_ins(bid, cid, 'Плазморез RILAND 100А 380В до 35мм',  5000,  75000);
    PERFORM _v3_ins(bid, cid, 'Плазморез 40А 220В до 10мм',          2000,  17000);
    PERFORM _v3_ins(bid, cid, 'Насадки для плазмореза 1Ф комплект',     0,      0);
    PERFORM _v3_ins(bid, cid, 'Насадки для плазмореза 3Ф RILAND',       0,      0);

    -- 19. Отбойники
    INSERT INTO tool_categories (name) VALUES ('Отбойники') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Отбойник КИТАЙ малый 250 тип',              2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник КИТАЙ малый НОВЫЕ 250 тип',        2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник ROLF 250 тип',                     2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TEN 250 тип',                      2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник PIT 250 тип',                      2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник Профессионал 250 тип',             2000,  12000);
    PERFORM _v3_ins(bid, cid, 'Отбойник РЕОСТАТ 45Дж 300 тип',            2000,  15000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TOLSEN 7кг 300 тип',               2000,  15000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TOLSEN 11кг 300 тип',              2000,  25000);
    PERFORM _v3_ins(bid, cid, 'Отбойник HOTECHE 11кг 300 тип',             2000,  25000);
    PERFORM _v3_ins(bid, cid, 'Отбойник HOTECHE 15кг 300 тип',             2000,  20000);
    PERFORM _v3_ins(bid, cid, 'Отбойник PIT 45Дж большой 300 тип',        2000,  20000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TOLSEN 23кг 1600W 300 тип',        2000,  20000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TEN 60Дж 350 тип',                2000,  20000);
    PERFORM _v3_ins(bid, cid, 'Отбойник INCCO 18кг 300 тип',               2000,  20000);
    PERFORM _v3_ins(bid, cid, 'Отбойник HOTECHE 18кг зеленый 350 тип',    2000,  30000);
    PERFORM _v3_ins(bid, cid, 'Отбойник MAKITA HM1203C 9.7кг 25.5Дж',     5000,  41000);
    PERFORM _v3_ins(bid, cid, 'Отбойник MAKITA HM1317C 17кг 33.8Дж',      5000,  63000);
    PERFORM _v3_ins(bid, cid, 'Отбойник TOLSEN 18кг 60Дж 350 тип',        2000,  32000);
    PERFORM _v3_ins(bid, cid, 'Отбойник HILTI TE-1000 500 тип',            5000,  90000);
    PERFORM _v3_ins(bid, cid, 'Отбойник BOSCH 16 18кг 45Дж 400 тип',      5000,  90000);
    PERFORM _v3_ins(bid, cid, 'Отбойник BOSCH 27 30кг 63Дж 500 тип',      7000, 125000);

    -- 20. Перфораторы
    INSERT INTO tool_categories (name) VALUES ('Перфораторы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Перфоратор BODA 1.9Дж',                 1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор BOSCH 1.9Дж',                1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор ROLF 1.9Дж',                 1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор аккум. MAKITA 1.9Дж',        2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор ANCHOR 9Дж квадр. бур',      1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор BODA 9Дж квадр. бур',        1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор CROWN 9Дж',                  2000,  8000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор KEN 9Дж',                    2000, 12000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор CROWN 14Дж SDS MAX',         2000, 20000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор KEN 14Дж SDS MAX',           2000, 20000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор MAKITA 4001 9.5Дж SDS MAX',  3000, 37000);
    PERFORM _v3_ins(bid, cid, 'Перфоратор MAKITA 5201 19.7Дж SDS MAX', 4500, 48000);

    -- 21. Шуруповерты и дрели
    INSERT INTO tool_categories (name) VALUES ('Шуруповерты и дрели') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Шуруповерт Фенг Бао набор 13 патрон',  1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт BOSCH набор',               1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт JIONGJIE набор',            1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт MAKITA 456 набор',          2000, 36000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт MAKONA набор',              1000, 10000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт-дрель INCCO 16 патрон',     1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Шуруповерт-дрель Агресс',              1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Бурмашинка электрическая',             1000,  3000);
    PERFORM _v3_ins(bid, cid, 'Сверлильный станок',                   2000, 25000);

    -- 22. Шлифовальные машинки
    INSERT INTO tool_categories (name) VALUES ('Шлифовальные машинки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка эксцентриковая 120мм',     1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка плоскошлифовальная TOLSEN',1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка вибрационная MAKITA',      1000,   800);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка BODA',                     1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка Пит дерево',               1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка Жираф шпаклевка',          2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка Жираф TOLSEN',             2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка Жираф бесщеточный',        2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка дерево пол СО-206',        5000, 50000);
    PERFORM _v3_ins(bid, cid, 'Шлиф. машинка дерево пол СО-401',        5000, 50000);
    PERFORM _v3_ins(bid, cid, 'Полировочная машинка',                    1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Реноватор',                               1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Оберфрейзер 12мм цанга',                 1000,  8000);

    -- 23. Штробрезы и стенорезы
    INSERT INTO tool_categories (name) VALUES ('Штробрезы и стенорезы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Штробрез INGCO',                              2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Штробрез HOTECHE ПРОФЕССИОНАЛ. 156мм',        2000, 17000);
    PERFORM _v3_ins(bid, cid, 'Стенорезная машина ручная 400мм рез 14 см',   5000, 30000);

    -- 24. Трамбовки
    INSERT INTO tool_categories (name) VALUES ('Трамбовки') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Трамбовка электр. 1-фазная 250 тип',          3000,  35000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка электр. 3-фазная 250 тип',          3000,  35000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка электр. 1-фазная большая',          3000,  35000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка бензиновая',                        5000,  35000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка бензиновая Кузнечик ПИШПЕК',        5000, 100000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка площадочная электро 250 тип',       3000,  32000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка площадочная 350 тип',               3000,  32000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка площадочная ELITECH 350 тип',       5000,  50000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка площадочная оранжевая 350 тип',     3000,  50000);
    PERFORM _v3_ins(bid, cid, 'Трамбовка площадочная TOLSEN 125кг 500 тип',  5000,  93840);

    -- 25. Трубогибы
    INSERT INTO tool_categories (name) VALUES ('Трубогибы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Трубогиб квадрат 15-40мм',              2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Трубогиб круглые трубы 15-30мм',        2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Трубогиб круглый пруток до 16мм',       2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Трубогиб 6-10мм медь алюминий малый',   1000,  1500);
    PERFORM _v3_ins(bid, cid, 'Трубогиб 6-22мм медь алюминий большой', 1000,  6500);
    PERFORM _v3_ins(bid, cid, 'Трубогиб полоса 40x10мм',               2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Рычажный трубогиб 6-10мм',              1000,  3000);

    -- 26. Насосы и мотопомпы
    INSERT INTO tool_categories (name) VALUES ('Насосы и мотопомпы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Насос для закачки систем отопления 1200л/ч', 1000,  8000);
    PERFORM _v3_ins(bid, cid, 'Насос погружной 250л/мин 50 диам',           1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Мотопомпа вода грязь',                       4000, 30000);

    -- 27. Мотобуры
    INSERT INTO tool_categories (name) VALUES ('Мотобуры') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Мотобур Патриот 4-х тактный', 4000, 25000);
    PERFORM _v3_ins(bid, cid, 'Мотобур Патриот двуручный',    4000, 25000);
    PERFORM _v3_ins(bid, cid, 'Мотобур Патриот одноручный',   4000, 25000);

    -- 28. Лазерные и оптические приборы
    INSERT INTO tool_categories (name) VALUES ('Лазерные и оптические приборы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Лазерный нивелир зеленый 360',        1000,  5500);
    PERFORM _v3_ins(bid, cid, 'Лазерная рулетка HILTI PD 38',        2000, 38000);
    PERFORM _v3_ins(bid, cid, 'Нивелир ADA оптический',              2000, 36000);
    PERFORM _v3_ins(bid, cid, 'Нивелир Spectra AL-20 оптический',    2000, 28000);
    PERFORM _v3_ins(bid, cid, 'Нивелир АТ-32 оптический',            2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Лазерный термометр пирометр',         1000,  1500);
    PERFORM _v3_ins(bid, cid, 'Детектор скрытой проводки',           1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Детектор скрытой проводки АДА',       1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Тепловизор ADA',                      2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Тепловизор UNI-T PRO UTi260B',        2000, 30000);
    PERFORM _v3_ins(bid, cid, 'Уровни STABILA',                      1000,  8000);
    PERFORM _v3_ins(bid, cid, 'Мегометр',                            1000,  2000);

    -- 29. Пылесосы
    INSERT INTO tool_categories (name) VALUES ('Пылесосы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Пылесос моющий Karcher Puzzi 10/1', 2000, 130000);
    PERFORM _v3_ins(bid, cid, 'Пылесос моющий Karcher Puzzi 8/1',  2000,  80000);
    PERFORM _v3_ins(bid, cid, 'Пылесос моющий Karcher SE 6.100',   2000,  35000);
    PERFORM _v3_ins(bid, cid, 'Пылесос строит. HOTECHE 75л',       2000,  30000);
    PERFORM _v3_ins(bid, cid, 'Пылесос строит. INCCO 75л',         2000,  30000);
    PERFORM _v3_ins(bid, cid, 'Пылесос строит. китай малый',       1000,  10000);
    PERFORM _v3_ins(bid, cid, 'Пароочиститель Karcher SC 4',        2000,  35000);

    -- 30. Подъемники и такелаж
    INSERT INTO tool_categories (name) VALUES ('Подъемники и такелаж') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Козловой подъемник',                   5000, 25000);
    PERFORM _v3_ins(bid, cid, 'Козловой подъемник тележка таль',      5000, 30000);
    PERFORM _v3_ins(bid, cid, 'Тельфер 300-600кг',                    2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Тельфер 400-800кг',                    2000, 25000);
    PERFORM _v3_ins(bid, cid, 'Таль 1.5т 3м',                         1000,  9000);
    PERFORM _v3_ins(bid, cid, 'Присоска аккумуляторная 150кг',        1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Рохля 2500кг',                         4000, 30000);
    PERFORM _v3_ins(bid, cid, 'Ходули',                               2000, 35000);
    PERFORM _v3_ins(bid, cid, 'Пресс гидравлический',                 1000, 25000);

    -- 31. Строительные леса и лестницы
    INSERT INTO tool_categories (name) VALUES ('Строительные леса и лестницы') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Леса строительные одна секция',         500,  6500);
    PERFORM _v3_ins(bid, cid, 'Колеса на леса большие за 1 шт',        500,  1750);
    PERFORM _v3_ins(bid, cid, 'Колеса на леса маленькие за 1 шт',      500,  1750);
    PERFORM _v3_ins(bid, cid, 'Комплект колес на леса 4 шт',          4000,  7000);
    PERFORM _v3_ins(bid, cid, 'Лестница-трансформер 3.55м',           1000, 11000);
    PERFORM _v3_ins(bid, cid, 'Лестница-трансформер 4.65м',           1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Лестница-трансформер 6.0м',            2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Лестница-стремянка 3.6м',              1000, 15000);
    PERFORM _v3_ins(bid, cid, 'Лестница телескопическая 4.4м',        1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Стремянки с широкими ножками',          1000,  5000);

    -- 32. Разное
    INSERT INTO tool_categories (name) VALUES ('Разное') RETURNING id INTO cid;
    PERFORM _v3_ins(bid, cid, 'Выпрямитель проволоки арматуры',         4000, 40000);
    PERFORM _v3_ins(bid, cid, 'Гайковерт',                              1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Головки FORCE',                          1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Динамометрический ключ 1/2 40-210Нм',    1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Динамометрический ключ 1/4 5-25Нм',      1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Динамометрический ключ 3/8 19-110Нм',    1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Домкрат 20 тонн',                        1000,  5000);
    PERFORM _v3_ins(bid, cid, 'Домкрат 32 тонн',                        1000,  8500);
    PERFORM _v3_ins(bid, cid, 'Домкрат 50 тонн',                        1000,  8500);
    PERFORM _v3_ins(bid, cid, 'Каток садовый',                          1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Кафельный станок ручной 60см',           1000,  7000);
    PERFORM _v3_ins(bid, cid, 'Кафельный станок ручной 120см',          2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Кафельный станок 100см ИТАЛИЯ',          4000, 65000);
    PERFORM _v3_ins(bid, cid, 'Ковролинорез',                           1000,  8500);
    PERFORM _v3_ins(bid, cid, 'Кондуктор для врезки замков',            1000,  8500);
    PERFORM _v3_ins(bid, cid, 'Кувалда',                                 500,  2500);
    PERFORM _v3_ins(bid, cid, 'Кабелерез',                              1000,  2000);
    PERFORM _v3_ins(bid, cid, 'Лерки сантехнические',                   1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Набор для установки кондиционера',        1000,  8000);
    PERFORM _v3_ins(bid, cid, 'Набор звездочек',                         1000, 12000);
    PERFORM _v3_ins(bid, cid, 'Опрессовщик электрических кабелей',       1000,  9500);
    PERFORM _v3_ins(bid, cid, 'Опрессометр сантехнический',              2000, 15000);
    PERFORM _v3_ins(bid, cid, 'Паяльная лампа',                         1000,  2000);
    PERFORM _v3_ins(bid, cid, 'Прожектор',                              1000,  2500);
    PERFORM _v3_ins(bid, cid, 'Станок БЕЛМАШ УНИВЕРСАЛ 2000Е',          2000, 20000);
    PERFORM _v3_ins(bid, cid, 'Станок для гибки арматуры до 12мм',       500,  2000);
    PERFORM _v3_ins(bid, cid, 'Станок комбинированный JET JKM 300',      2000, 65000);
    PERFORM _v3_ins(bid, cid, 'Строгательная машина СО-306',             5000, 50000);
    PERFORM _v3_ins(bid, cid, 'Съемник гидравлический трехлапый',        1000,  6000);
    PERFORM _v3_ins(bid, cid, 'Съемники подшипников внутренних набор',   1000,  8000);
    PERFORM _v3_ins(bid, cid, 'Съемники подшипников трехлапые',           500,  2000);
    PERFORM _v3_ins(bid, cid, 'Тачки',                                   1000,  6500);
    PERFORM _v3_ins(bid, cid, 'Трос сантехнический',                     1000,  2000);
    PERFORM _v3_ins(bid, cid, 'Удлинитель 1Ф 20м',                        500,  3000);
    PERFORM _v3_ins(bid, cid, 'Удлинитель 1Ф 30м',                        500,  3000);
    PERFORM _v3_ins(bid, cid, 'Удлинитель 1Ф 50м',                        500,  5000);
    PERFORM _v3_ins(bid, cid, 'Удлинитель 3Ф 17м Сварка',               2000,  8000);
    PERFORM _v3_ins(bid, cid, 'Удлинитель 3Ф 24м Пушки',                2000,  8000);
    PERFORM _v3_ins(bid, cid, 'Фен строительный TOLSEN 600 град',        1000,  4800);
    PERFORM _v3_ins(bid, cid, 'Фен строительный BOSCH 600 град',         1000,  4800);
    PERFORM _v3_ins(bid, cid, 'Формы для садовой дорожки',               1000,  1500);
    PERFORM _v3_ins(bid, cid, 'Газовый ключ',                            1000,  4000);
    PERFORM _v3_ins(bid, cid, 'Мойка высокого давления Китай 200 бар',   2000, 30000);
END;
$do$;

-- ── Очистка вспомогательных объектов ────────────────────────────────────────
DROP FUNCTION _v3_ins;
DROP SEQUENCE _v3_seq;
