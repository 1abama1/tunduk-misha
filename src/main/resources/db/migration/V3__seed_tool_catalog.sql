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
    p_deposit   NUMERIC,
    p_price     NUMERIC
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
        p_price,
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
    c_drill BIGINT;
    c_demo BIGINT;
    c_concrete BIGINT;
    c_weld BIGINT;
    c_garden BIGINT;
    c_power BIGINT;
    c_heat BIGINT;
    c_gen BIGINT;
    c_lift BIGINT;
    c_comp BIGINT;
    c_measure BIGINT;
    c_scaffold BIGINT;
    c_clean BIGINT;
    c_machine BIGINT;
    c_plumb BIGINT;
BEGIN
    SELECT id INTO bid FROM branches ORDER BY id LIMIT 1;

    -- ==========================================
    -- Создание категорий и вставка оборудования
    -- ==========================================

    -- 1. Алмазное и буровое оборудование
    INSERT INTO tool_categories (name) VALUES ('Алмазное и буровое оборудование') RETURNING id INTO c_drill;
    PERFORM _v3_ins(bid, c_drill, 'Алмазная установка  (ручная установка)', 700.0, 2000.0, 8000.0);
    PERFORM _v3_ins(bid, c_drill, 'Алмазная установка (стандарт) (комплект)', 1500.0, 5000.0, 45000.0);
    PERFORM _v3_ins(bid, c_drill, 'Аренда алмазной коронки по кирпичу (зависит от диаметра)', 300.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_drill, 'Алмазные коронки от 40мм до 63мм', 300.0, 1000.0, 2500.0);
    PERFORM _v3_ins(bid, c_drill, 'Алмазные коронки от 76мм до 100мм', 400.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_drill, 'Алмазные коронки от 102мм до 120мм', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_drill, 'Алмазные коронки от 150мм до 200мм', 800.0, 1000.0, 6000.0);
    PERFORM _v3_ins(bid, c_drill, 'Буры SDS MAX с 10-18 мм', 300.0, 500.0, 1500.0);
    PERFORM _v3_ins(bid, c_drill, 'Буры SDS MAX с 20-30 мм', 500.0, 2000.0, 4000.0);
    PERFORM _v3_ins(bid, c_drill, 'Буры SDS MAX с 32-40 мм', 800.0, 2000.0, 6500.0);
    PERFORM _v3_ins(bid, c_drill, 'Буры на АНКОР', 300.0, 500.0, 1000.0);
    PERFORM _v3_ins(bid, c_drill, 'Бурмашинка электрическая', 500.0, 1000.0, 3000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор BODA (1,9 Дж)', 400.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор BOSCH (1,9 Дж)', 400.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор ROLF (1,9 Дж)', 400.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор аккамулят.MAKITA 1,9 Дж', 500.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор ANCHOR (9 Дж) квадратный бур', 700.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор BODA (9 Дж) квадратный бур', 700.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор CROWN (9 Дж)', 800.0, 2000.0, 8000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор KEN (9 Дж)', 800.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор  CROWN  (14 Дж) SDS MAX', 800.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор  KEN  (14 Дж) SDS MAX', 800.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор MAKITA 4001 (9,5 Дж) SDS MAX', 1000.0, 3000.0, 37000.0);
    PERFORM _v3_ins(bid, c_drill, 'Перфоратор MAKITA 5201(19,7 Дж) SDS MAX', 1500.0, 4500.0, 48000.0);

    -- 2. Демонтажное оборудование
    INSERT INTO tool_categories (name) VALUES ('Демонтажное оборудование') RETURNING id INTO c_demo;
    PERFORM _v3_ins(bid, c_demo, 'Отбойник КИТАЙ Малые (ПРОФЕШ. РЕОСТАТ, ROLF и т.д.)', 900.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник КИТАЙ Малые НОВЫЕ', 1200.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник ROLF', 900.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TEN', 900.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник PIT', 900.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник Профешинал', 1200.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник РЕОСТАТ 45 Дж', 1200.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TOLSEN 7 кг', 1200.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TOLSEN 11 кг', 1200.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник HOTECHE 11 кг', 1200.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник HOTECHE  15 кг', 1400.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник PIT 45 Дж большой', 1400.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TOLSEN 23 кг 1600 W', 1400.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TEN 60 Дж большой', 2000.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник INCCO 18 кг', 1400.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник HOTECHE  18 кг зеленый', 2000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник MAKITA HM1203C вес 9,7кг 25,5Дж', 2000.0, 5000.0, 41000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник MAKITA HM1317C вес 17кг 33,8 Дж', 2000.0, 5000.0, 63000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник TOLSEN 18 кг 60 ДЖ', 2000.0, 2000.0, 32000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник HILTI TE-1000', 2500.0, 5000.0, 90000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник BOSCH 16 вес 18кг 45 Дж', 2500.0, 5000.0, 90000.0);
    PERFORM _v3_ins(bid, c_demo, 'Отбойник BOSCH 27 вес 30 кг 63 Дж', 3000.0, 7000.0, 125000.0);
    PERFORM _v3_ins(bid, c_demo, 'Штроборез INGCO', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_demo, 'Штроборез  HOTECHE ПРОФЕССИОНАЛ. 156 мм', 1000.0, 2000.0, 17000.0);
    PERFORM _v3_ins(bid, c_demo, 'Кувалда', 500.0, 500.0, 2500.0);

    -- 3. Бетонное и дорожное оборудование
    INSERT INTO tool_categories (name) VALUES ('Бетонное и дорожное оборудование') RETURNING id INTO c_concrete;
    PERFORM _v3_ins(bid, c_concrete, 'Асфальторез Бензиновый  WAKER NEUSON 300 мм', 3000.0, 5000.0, 80000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Асфальторез Бензиновый CUTTER 500 мм', 3000.0, 5000.0, 80000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Асфальторез Бензиновый LONCIN 500 мм', 3000.0, 5000.0, 80000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Асфальторез Бензиновый TOLSEN 500 мм', 3000.0, 5000.0, 95000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Асфальторез диск 500 мм', 600.0, 1000.0, 6000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Бетономешалка 220 литров', 800.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибратор 1,0м 1,5метра', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибратор глубинный  6 метров', 1000.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибратор глубинный 3м или 4м', 1000.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибратор Чемпион 4м', 1000.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибробулова 1,5 м стоимость оборудования', 0, 0, 2500.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибробулова 4,0 м стоимость оборудования', 0, 0, 5500.0);
    PERFORM _v3_ins(bid, c_concrete, 'Вибробулова 6,0 м стоимость оборудования', 0, 0, 6500.0);
    PERFORM _v3_ins(bid, c_concrete, 'Виброрейка бензиновая 2 м', 2500.0, 4000.0, 38000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Стенорезная машина ручная 400 мм глубина реза 14см', 3000.0, 5000.0, 30000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Строгательная Машина СО 306', 2500.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Электр.1 фазная (сдача мин 2часа)', 1700.0, 3000.0, 35000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Электр.3 фазная (сдача мин 2часа)', 1700.0, 3000.0, 35000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Электр.1 фазная большая', 2000.0, 3000.0, 35000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Бензин. (сдача мин 2часа)', 3000.0, 5000.0, 35000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Бензин. Кузнечик ПИШПЕК (мин сутки)', 3000.0, 5000.0, 100000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Площадочная (сдача мин 2часа) Электро', 1700.0, 3000.0, 32000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Площадочная (сдача мин 2часа)', 2400.0, 3000.0, 32000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Площадочная (сдача мин 2часа) ELITECH', 3000.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Площадочная Оранжевая', 3000.0, 3000.0, 50000.0);
    PERFORM _v3_ins(bid, c_concrete, 'Трамбовка Площадочная TOLSEN Большая 125 кг', 3500.0, 5000.0, 93840.0);
    PERFORM _v3_ins(bid, c_concrete, 'Хопер ковш', 500.0, 1000.0, 4000.0);

    -- 4. Сварочное и плазменное оборудование
    INSERT INTO tool_categories (name) VALUES ('Сварочное оборудование') RETURNING id INTO c_weld;
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки пласт. труб HYNDAI', 500.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки пласт. труб INGCO малый', 500.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки пласт. труб Патриот бол.', 500.0, 1000.0, 6000.0);
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки ПНД труб диам 180 мм', 1000.0, 2000.0, 45000.0);
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки ПНД труб диам 250 мм (китай)', 2000.0, 5000.0, 75000.0);
    PERFORM _v3_ins(bid, c_weld, 'Аппарат для сварки ПНД труб диам 250 мм', 2000.0, 5000.0, 75000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез 160 А 380 В до 55 мм LGK ABM PROFESSIONAL', 1500.0, 5000.0, 90000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез 130 А 380 В до 55 мм LGK', 1500.0, 5000.0, 75000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез 120 А 380 В до 45 мм LGK', 1500.0, 5000.0, 60000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез 100 А 380 В до 35 мм RILAND', 1500.0, 5000.0, 75000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез 40 А 220 В до 10 мм', 800.0, 2000.0, 17000.0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез насадки для плазмореза (по 100сом)', 400.0, 0, 0);
    PERFORM _v3_ins(bid, c_weld, 'Плазморез насадки для плазмореза (по 200сом)', 600.0, 0, 0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка RILAND ARC 200 CT', 500.0, 1000.0, 11000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка CAMEO MMA 250', 500.0, 1000.0, 11000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка РЕОСТАТ черная 180 А', 500.0, 1000.0, 11000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка TCH MMA 300 А черный', 500.0, 1000.0, 15000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка полуавтомат ROLF', 800.0, 2000.0, 28000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка полуавтомат Патриот', 800.0, 2000.0, 28000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка полуавтомат Bolian NBC-270s', 1000.0, 2000.0, 28000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка RILAND MMA 500G 380В 25кВт 40В', 1000.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_weld, 'Сварка RILAND MMA 500G 380В 25кВт 40В PRO', 1500.0, 5000.0, 55000.0);
    PERFORM _v3_ins(bid, c_weld, 'Горелка газовая большая (комплект с балон)', 500.0, 1000.0, 8700.0);
    PERFORM _v3_ins(bid, c_weld, 'Горелка газовая малая (комплект с балон)', 500.0, 1000.0, 8700.0);
    PERFORM _v3_ins(bid, c_weld, 'Паяльная лампа', 500.0, 1000.0, 2000.0);

    -- 5. Садовая техника и оборудование
    INSERT INTO tool_categories (name) VALUES ('Садовая техника') RETURNING id INTO c_garden;
    PERFORM _v3_ins(bid, c_garden, 'Бенз. Мотоблок Набор для окучивания', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_garden, 'Бензиновый мотоблок SHINEREAY', 1500.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_garden, 'Бензиновый мотоблок Калуга', 1500.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_garden, 'Бензиновый мотоблок Победа', 1500.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_garden, 'Бензиновый мотоблок Урал', 1500.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_garden, 'Воздуходувка', 500.0, 1000.0, 4500.0);
    PERFORM _v3_ins(bid, c_garden, 'Воздуходувка ранцевая бензиновая TOLSEN', 1000.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_garden, 'Воздуходувка ранцевая бензиновая 4-х тактная', 1000.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_garden, 'Газонокосилка бенз. 4 тактный триммер', 800.0, 2000.0, 11000.0);
    PERFORM _v3_ins(bid, c_garden, 'Газонокосилка электр.триммер ПРОРАБ', 800.0, 2000.0, 8000.0);
    PERFORM _v3_ins(bid, c_garden, 'Газонокосилка электр.YWTT', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_garden, 'Газонокосилка электр.INGCO', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_garden, 'Кусторез STIHL', 1000.0, 2000.0, 35000.0);
    PERFORM _v3_ins(bid, c_garden, 'Мотобур Патриот 4-х тактный', 1500.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_garden, 'Мотобур Патриот двуручный', 1500.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_garden, 'Мотобур Патриот одноручный', 1500.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_garden, 'Пила ЦЕПНАЯ Бензо STIHL 260', 1500.0, 5000.0, 49000.0);
    PERFORM _v3_ins(bid, c_garden, 'Пила ЦЕПНАЯ Бензо STIHL 382', 1500.0, 5000.0, 56000.0);
    PERFORM _v3_ins(bid, c_garden, 'Пила ЦЕПНАЯ Бензо STIHL 382 оригинал цепь', 2000.0, 5000.0, 56000.0);
    PERFORM _v3_ins(bid, c_garden, 'Пила ЦЕПНАЯ Электр. КИТАЙ', 800.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_garden, 'Каток садовый', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_garden, 'Скарификатор+ аэратор  HOTECHE', 1500.0, 2000.0, 11000.0);
    PERFORM _v3_ins(bid, c_garden, 'Формы для садовой дорожки', 100.0, 1000.0, 1500.0);

    -- 6. Электроинструмент
    INSERT INTO tool_categories (name) VALUES ('Электроинструмент') RETURNING id INTO c_power;
    PERFORM _v3_ins(bid, c_power, 'Болгарка (УШМ) SAFUN 125', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_power, 'Болгарка (УШМ) TENIR аккамуляторная 125', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_power, 'Болгарка (УШМ) ATEK  180', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Болгарка (УШМ) SAFUN 230', 600.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_power, 'Возвратно поступ. пила Пилка по дереву', 350.0, 0, 0);
    PERFORM _v3_ins(bid, c_power, 'Возвратно поступательная пила (Сабельная пила)', 800.0, 1000.0, 9000.0);
    PERFORM _v3_ins(bid, c_power, 'Гайковерт', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_power, 'Лобзик Электр.', 400.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Миксер  BODA', 500.0, 1000.0, 5500.0);
    PERFORM _v3_ins(bid, c_power, 'Оберфрейзер 12мм цанга', 800.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила (пчелка) 110мм', 400.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила ДИСК по алюминию (диск 100$)', 300.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила отрезная по металу BOSCH', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила отрезная по металу KEN', 800.0, 2000.0, 8500.0);
    PERFORM _v3_ins(bid, c_power, 'Пила отрезная по металу TOLSEN', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) KEHANG корич.', 500.0, 1000.0, 6500.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торц.(углорез)INCCO малый асинхроник', 500.0, 1000.0, 20000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) BODA 255', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) EBI MAX 255', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) LANZAN 255', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) ROLF 255', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) SUZUKI 255', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) WUJIE красный', 500.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез по алюминию)', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) SUZUKI 255 рельсовый', 800.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) MAKITA LN1040F', 800.0, 2000.0, 35000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез)INCCO большой', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) Metabo 305 рельсовый', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) ALEM 305', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) BODA 355', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила торцовочная(углорез) HOTECHE 305 рельсовый', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_power, 'Пила Циркулярная KEN  HEANG (190мм.)', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_power, 'Реноватор', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Фен строительный  TOLSEN 600 град', 300.0, 1000.0, 4800.0);
    PERFORM _v3_ins(bid, c_power, 'Фен строительный BOSCH 600 град', 300.0, 1000.0, 4800.0);
    PERFORM _v3_ins(bid, c_power, 'Полировачная машинка', 500.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф Машинка эксцентриковая 120 мм', 500.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф. Машинка (ручная) TOLSEN плоскошлифовальная', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф. Машинка вибрационная  (ручная)MAKITA', 500.0, 1000.0, 800.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка (ручная)BODA', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка Пит (дерево)', 800.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка (шпаклевка) Жираф', 1000.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка (шпаклевка) Жираф TOLSEN', 1500.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка (шпаклевка) Жираф бесщеточный', 1500.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф.машинка (дерево)пол СО-206', 2000.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_power, 'Шлиф. Машинка (дерево)пол СО-401', 1500.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт Фенг Бао (набор) 13 патрон', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт BOSCH  (набор)', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт JIONGJIE  (набор)', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт MAKITA 456(набор)', 800.0, 2000.0, 36000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт MAKONA  (набор)', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт-дрель INCCO 16 патрон', 500.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_power, 'Шуруповерт-дрель Агресс', 500.0, 1000.0, 5000.0);

    -- 7. Тепловое и климатическое оборудование
    INSERT INTO tool_categories (name) VALUES ('Тепловое и климатическое оборудование') RETURNING id INTO c_heat;
    PERFORM _v3_ins(bid, c_heat, 'Газовая пушка ROLF только для натяжных (1 кг\час)', 500.0, 1000.0, 10000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовая пушка TYQ 15кВт (1 кг\час) Голубая', 800.0, 4000.0, 15000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовая пушка TYQ 30кВт (1 кг\час) Голубая', 800.0, 4000.0, 15000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газ. пушка ПрофТепло 30 кВт (1,5-3кг\час)', 800.0, 4000.0, 20000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовая пушка ПрофТепло 57кВт (2,9-4,1 кг)', 1000.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовая пушка ПрофТепло 81 кВт (3,9-5,9 кг)', 1500.0, 4000.0, 30000.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 13 кВТ (2,51л\час) бак 18 л', 1000.0, 4000.0, 20352.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 30 кВТ (2,51л\час) бак 18 л', 1000.0, 4000.0, 20352.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 30 кВТ (2,51л\час) бак 18,5л', 1000.0, 4000.0, 20352.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 40 кВТ (3,9л\час) бак 26л', 1000.0, 4000.0, 25000.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 43 кВТ ( 4,0л\час) бак 55,5', 1500.0, 4000.0, 25500.0);
    PERFORM _v3_ins(bid, c_heat, 'Диз. Тепл. пушка 65 кВТ (5,95л\час) бак 55,5л', 2000.0, 4000.0, 45000.0);
    PERFORM _v3_ins(bid, c_heat, 'Электротепловентиляторы ( 1 фазный) 3кВт', 300.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_heat, 'Электротепловентиляторы ( 3 фазный) 11кВт', 600.0, 2000.0, 12000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовый инфракрасный обогреватель', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_heat, 'Газовый баллон (без пушки или с горелкой)', 300.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_heat, 'Осушитель воздуха YAKE', 1500.0, 5000.0, 70000.0);

    -- 8. Генераторы и электропитание
    INSERT INTO tool_categories (name) VALUES ('Генераторы и электропитание') RETURNING id INTO c_gen;
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3,0 TOLSEN', 1300.0, 4000.0, 35000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3,0 KEMAGE', 1300.0, 4000.0, 35000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3,5', 1300.0, 4000.0, 35000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3,5 HONDA', 1300.0, 4000.0, 35000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 5,5 KEMAGE', 1600.0, 5000.0, 50000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 7,0 KEMAGE', 2100.0, 5000.0, 55000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 8.0 TOLSEN', 2100.0, 5000.0, 65000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 8,5 KEMAGE', 2500.0, 5000.0, 65000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3Ф 7,0 JL 9000 STILWELL', 2100.0, 5000.0, 70000.0);
    PERFORM _v3_ins(bid, c_gen, 'Генератор 3Ф 8,5 Kipor', 3000.0, 5000.0, 150000.0);
    PERFORM _v3_ins(bid, c_gen, 'Удлинители электрические 1 Ф 20 м.пог', 200.0, 500.0, 3000.0);
    PERFORM _v3_ins(bid, c_gen, 'Удлинители электрические 1 Ф 30 м.пог', 300.0, 500.0, 3000.0);
    PERFORM _v3_ins(bid, c_gen, 'Удлинители электрические 1 Ф 50 м.пог', 400.0, 500.0, 5000.0);
    PERFORM _v3_ins(bid, c_gen, 'Удлинители электрические 3 Ф 17 М.пог. 5х6 мм.кв 63 А', 500.0, 2000.0, 8000.0);
    PERFORM _v3_ins(bid, c_gen, 'Удлинители электрические 3 Ф 24 м.пог на пушки', 500.0, 2000.0, 8000.0);

    -- 9. Подъемное и складское оборудование
    INSERT INTO tool_categories (name) VALUES ('Подъемное и складское оборудование') RETURNING id INTO c_lift;
    PERFORM _v3_ins(bid, c_lift, 'Домкрат 20 тонн', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_lift, 'Домкрат 32 тонн', 500.0, 1000.0, 8500.0);
    PERFORM _v3_ins(bid, c_lift, 'Домкрат 50 тонн', 500.0, 1000.0, 8500.0);
    PERFORM _v3_ins(bid, c_lift, 'Козловой подьемник', 2000.0, 5000.0, 25000.0);
    PERFORM _v3_ins(bid, c_lift, 'Козловой подъемник +тележка+таль', 2500.0, 5000.0, 30000.0);
    PERFORM _v3_ins(bid, c_lift, 'Рохля 2500 кг', 1500.0, 4000.0, 30000.0);
    PERFORM _v3_ins(bid, c_lift, 'Таль 1,5т, 3м', 800.0, 1000.0, 9000.0);
    PERFORM _v3_ins(bid, c_lift, 'Тельфер 300-600 кг', 1000.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_lift, 'Тельфер 400-800 кг', 1000.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_lift, 'Тачки', 500.0, 1000.0, 6500.0);
    PERFORM _v3_ins(bid, c_lift, 'Присоска аккумуляторная 150 кг', 500.0, 1000.0, 5000.0);

    -- 10. Компрессорное и покрасочное оборудование
    INSERT INTO tool_categories (name) VALUES ('Компрессорное и покрасочное оборудование') RETURNING id INTO c_comp;
    PERFORM _v3_ins(bid, c_comp, 'Компрессор 50л.', 500.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_comp, 'Компрессор 50л. медицинский тихий', 800.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_comp, 'Компрессор 80л.двухпоршневой', 1000.0, 4000.0, 40000.0);
    PERFORM _v3_ins(bid, c_comp, 'Компрессор MZB VA 70л.двухпоршневой', 1000.0, 4000.0, 40000.0);
    PERFORM _v3_ins(bid, c_comp, 'Компрессор 90л.трехпоршневой 10 АТМ', 1300.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_comp, 'Компрессор 90л.трехпоршневой 10 АТМ бензиновый', 1600.0, 4000.0, 50000.0);
    PERFORM _v3_ins(bid, c_comp, 'Краскопульт (ручной для извести и опрыск.)', 1000.0, 3000.0, 8000.0);
    PERFORM _v3_ins(bid, c_comp, 'Краскопульт безвоздушный JSPERFECT', 2000.0, 5000.0, 60000.0);
    PERFORM _v3_ins(bid, c_comp, 'Краскопульт безвоздушный PROFESSIONAL SPRAY', 2000.0, 5000.0, 60000.0);
    PERFORM _v3_ins(bid, c_comp, 'Краскопульт безвоздушный JSPERFECT (для эмали)', 2000.0, 5000.0, 60000.0);
    PERFORM _v3_ins(bid, c_comp, 'Краскопульт безвоздушный мембранный', 1500.0, 5000.0, 40000.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет для нанесения антикоррозийной обработки', 300.0, 1000.0, 4500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет пневмо гвоздезабивной большой', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет пневмо гвоздезабивной малый', 300.0, 1000.0, 4500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет пневмо скобозабивной большой', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет пневмо скобозабивной малый', 300.0, 1000.0, 4500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет Покрасочный', 200.0, 500.0, 1000.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет Покрасочный (полу-профессиональный)', 300.0, 500.0, 1000.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет Покрасочный (профессиональный)', 300.0, 500.0, 2500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет Покрасочный большой водоэмульсия', 500.0, 500.0, 2500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет продувочный', 200.0, 500.0, 4500.0);
    PERFORM _v3_ins(bid, c_comp, 'Пистолет Текстурный', 300.0, 500.0, 3500.0);
    PERFORM _v3_ins(bid, c_comp, 'Заклепачник пневматический', 500.0, 1000.0, 9500.0);
    PERFORM _v3_ins(bid, c_comp, 'Набор пневмоинструмента', 500.0, 1000.0, 4000.0);

    -- 11. Измерительная техника
    INSERT INTO tool_categories (name) VALUES ('Измерительная техника') RETURNING id INTO c_measure;
    PERFORM _v3_ins(bid, c_measure, 'Детектор скрытой проводки', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_measure, 'Детектор скрытой проводки АДА', 500.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_measure, 'Динам.метр ключ 1\2, 40-210Hm, 460 мм TOLSEN', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_measure, 'Динам.метр ключ 1\4, 5-25Hm, 460 мм HOTECHE', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_measure, 'Динам.метр ключ 3/8, 19-110Hm, 365мм FORCE', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_measure, 'Лазерная рулетка HILTI PD 38', 500.0, 2000.0, 38000.0);
    PERFORM _v3_ins(bid, c_measure, 'Лазерный нивелир зеленый 360', 500.0, 1000.0, 5500.0);
    PERFORM _v3_ins(bid, c_measure, 'Лазерный термометр пирометр', 300.0, 1000.0, 1500.0);
    PERFORM _v3_ins(bid, c_measure, 'Мегометр', 500.0, 1000.0, 2000.0);
    PERFORM _v3_ins(bid, c_measure, 'Нивелир ADA (оптический)', 600.0, 2000.0, 36000.0);
    PERFORM _v3_ins(bid, c_measure, 'Нивелир Spectra AL-20 (оптический)', 600.0, 2000.0, 28000.0);
    PERFORM _v3_ins(bid, c_measure, 'Нивелир АТ-32 (оптический)', 600.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_measure, 'Тепловизор ADA', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_measure, 'Тепловизор UNI-T PRO UTi260B Professional', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_measure, 'Уровни STABILA', 500.0, 1000.0, 8000.0);

    -- 12. Леса, лестницы и высотное оборудование
    INSERT INTO tool_categories (name) VALUES ('Леса и лестницы') RETURNING id INTO c_scaffold;
    PERFORM _v3_ins(bid, c_scaffold, 'Леса строительные (одна секция)', 100.0, 500.0, 6500.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Колеса на леса большие за 1 шт', 75.0, 500.0, 1750.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Колеса на леса маленькие за 1 шт', 75.0, 500.0, 1750.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Комплект колес на леса 4 колеса', 300.0, 4000.0, 7000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Лестница  трансформер 3,55 м.пог', 500.0, 1000.0, 11000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Лестница  трансформер 4,65 м.пог', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Лестница  трансформер 6,0 м.пог', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Лестница стремянка 3,6 м', 500.0, 1000.0, 15000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Лестница телескопическая 4,4 м.пог', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Стремянки с широкими ножками', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_scaffold, 'Ходули', 1000.0, 2000.0, 35000.0);

    -- 13. Клининговое оборудование
    INSERT INTO tool_categories (name) VALUES ('Клининговое оборудование') RETURNING id INTO c_clean;
    PERFORM _v3_ins(bid, c_clean, 'Мойка китай 200 бар', 1000.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пароотчиститель Karcher SC 4', 1000.0, 2000.0, 35000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос  моющий Karcher Puzzi 10\1', 1500.0, 2000.0, 130000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос  моющий Karcher Puzzi 8\1', 1200.0, 2000.0, 80000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос моющий Karcher SE 6.100', 1200.0, 2000.0, 35000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос строит.HOTECHE (болш.) 75 литров', 800.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос строит.INCCO(болш.) 75 литров', 800.0, 2000.0, 30000.0);
    PERFORM _v3_ins(bid, c_clean, 'Пылесос строит.китай (мал.)', 500.0, 1000.0, 10000.0);

    -- 14. Станки и плиткорезы
    INSERT INTO tool_categories (name) VALUES ('Станки и плиткорезы') RETURNING id INTO c_machine;
    PERFORM _v3_ins(bid, c_machine, 'Камнерезный станок', 1500.0, 4000.0, 60000.0);
    PERFORM _v3_ins(bid, c_machine, 'Кафельный станок ручной 60 см', 500.0, 1000.0, 7000.0);
    PERFORM _v3_ins(bid, c_machine, 'Кафельный станок ручной 120 см', 800.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_machine, 'Кафельный станок 100 см ИТАЛИЯ', 1000.0, 4000.0, 65000.0);
    PERFORM _v3_ins(bid, c_machine, 'Станок БЕЛМАШ УНИВЕРСАЛ 2000Е', 800.0, 2000.0, 20000.0);
    PERFORM _v3_ins(bid, c_machine, 'Станок для гибки арматуры до 12мм', 100.0, 500.0, 2000.0);
    PERFORM _v3_ins(bid, c_machine, 'Станок комбинированный JET JKM 300', 1000.0, 2000.0, 65000.0);
    PERFORM _v3_ins(bid, c_machine, 'Сверлильный станок', 1000.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_machine, 'Пила торцовочная(углорез)Озчелик BETA +', 1000.0, 4000.0, 50000.0);

    -- 15. Сантехническое и ручное оборудование
    INSERT INTO tool_categories (name) VALUES ('Сантехническое и ручное оборудование') RETURNING id INTO c_plumb;
    PERFORM _v3_ins(bid, c_plumb, 'Выпрямитель проволки арматуры', 1500.0, 4000.0, 40000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Газовый ключ', 500.0, 1000.0, 4000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Головки FORCE', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Кабелерез', 500.0, 1000.0, 2000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Ковролинорез', 500.0, 1000.0, 8500.0);
    PERFORM _v3_ins(bid, c_plumb, 'Кондуктор для врезки замков', 800.0, 1000.0, 8500.0);
    PERFORM _v3_ins(bid, c_plumb, 'Лерки сантехнические', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Мотопомпа (вода,грязь)', 1500.0, 4000.0, 30000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Набор для установки кондиционера', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Набор Звездочек', 500.0, 1000.0, 12000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Насос для закачки систем отопления 1200 литр\Час', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Насос погружной 250л \мин 50 диам', 500.0, 1000.0, 5000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Опресовщик (электрических кабелей)', 500.0, 1000.0, 9500.0);
    PERFORM _v3_ins(bid, c_plumb, 'Опресометр (сантехнический)', 500.0, 2000.0, 15000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Пресс гидравлический', 800.0, 1000.0, 25000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Рычажный трубогиб    6, 8, 10 мм', 400.0, 1000.0, 3000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Съемники гидравлический трехлапый', 800.0, 1000.0, 6000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Съемники подшибников внутренних набор', 500.0, 1000.0, 8000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Съемники подшибников трехлапые', 300.0, 500.0, 2000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трос сантехнический', 500.0, 1000.0, 2000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб квадрат 15х15 20х20 25х25 30х30 40х40х2', 800.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб круглые трубы 15мм 20мм 25мм 30мм', 800.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб круглый пруток до 16 мм', 800.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб от 6 до 10 мм медь алюминий малый', 500.0, 1000.0, 1500.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб от 6 до 22 мм медь алюминий большой', 800.0, 1000.0, 6500.0);
    PERFORM _v3_ins(bid, c_plumb, 'Трубогиб полоса 40х10мм', 800.0, 2000.0, 25000.0);
    PERFORM _v3_ins(bid, c_plumb, 'Прожектор', 300.0, 1000.0, 2500.0);

END;
$do$;

-- ── Очистка вспомогательных объектов ────────────────────────────────────────
DROP FUNCTION _v3_ins;
DROP SEQUENCE _v3_seq;
