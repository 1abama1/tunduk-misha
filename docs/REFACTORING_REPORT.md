# Engineering audit and refactoring report — 17.10.2026

## A. Executive Summary

В ветке `audit/backend-hardening-20261017` выполнены проверяемые исправления backend на основе commit `8897c8ba48a45626fe5942ecee8a9ff9a0b8c9d5`. Полный `clean verify` проходит на Java 17.0.12 и Java 21.0.12: **138 тестов, 0 failures, 0 errors, 0 skipped**. Собран исполняемый JAR и проверен через реальный HTTP на Java 17 с отдельной тестовой БД.

Исправлены потеря истории инструментов при soft delete договора, обход защиты исторического клиента, ложный дефицит доступности при нескольких бронях одного экземпляра, устаревшие возвращаемые ревизии, пропуск инструментов в Excel, циклическая сериализация категорий. Усилены JWT/refresh/API-key проверки, безопасная выдача загруженных файлов и обработка входных ошибок. POI обновлён 5.2.5 → 5.5.1. Добавлены CI, документация и реестр 82 HTTP-маршрутов.

**Инцидент выполнения:** исходный контекстный тест без изоляции автоматически прочитал локальный `.env` и 17.10.2026 в 15:45 применил существующую V17 к `localhost:5432/kyrgyzCrm`, schema public (до этого V16). Запуск был прерван. Это ошибка выполнения baseline, а не запланированная миграция. Откат/clean/удаление данных не выполнялись; соответствие фактических данных исходному состоянию не проверялось. Владелец этой БД должен проверить применение V17. Все дальнейшие интеграционные проверки выполнялись в новом кластере `127.0.0.1:55439/tunduk_audit_test` с отдельными схемами, без использования пароля рабочей БД. Дефект изоляции тестов исправлен.

Работа не объявляется полной production-сертификацией или исчерпывающим завершением всех фаз мастер-задания. Нет полного SCA, Docker runtime/build, production-нагрузки, проверки всех внешних потребителей и визуального согласования печатного договора. Остаточные риски и границы проверок перечислены ниже и в [реестре](audit/RISK_REGISTER.md).

## B. Architecture Before / After

До и после — Spring Boot монолит с пакетами controller/service/repository, PostgreSQL/Flyway, JWT и POI. ContractService уже делегировал CRUD/reporting/Excel; эта структура сохранена. Массового перемещения пакетов, смены технологий или микросервисов нет.

Реальные изменения границ: общий LoginIdentifiers для регистрации/входа, явный soft UPDATE в репозитории договоров, единое использование RentalDocumentDto.fromEntity для legacy rent, предварительная проверка всех ревизий batch и контролируемая сериализация каталога. PostgreSQL advisory lock сохранён. Историческая таблица и существующие API остаются основой модели. Карта: [architecture](audit/ARCHITECTURE.md).

## C. Issues Found

Полный [реестр R01–R23](audit/RISK_REGISTER.md) содержит severity, confidence, evidence, root cause, impact, solution/alternatives, compatibility, tests и status.

| Приоритет | Основные подтверждённые проблемы | Результат |
| --- | --- | --- |
| P1 | Неизолированный context test; ORM удаляет историческую коллекцию | Тестовая среда изолирована; поддержанные пути soft delete сохраняют историю |
| P1 | Удаление клиента после скрытия его договоров | Native EXISTS проверяет всю историю |
| P1 | Несколько броней/ремонт уменьшают доступность несколько раз | Считаются пригодные экземпляры через NOT EXISTS |
| P1 | Пропуск 6–10 инструментов и формульные кеши Excel | Дополнительные листы; значения записываются как литералы |
| P1 | Возврат старой ревизии; рекурсивный JSON категорий | Flush перед ответом; preflight batch; ограничение графа каталога |
| P1 | Уязвимая версия POI; inline выдача активного контента | POI 5.5.1; allowlist изображений и attachment fallback |
| P2 | Refresh/API-key проверки, ввод, страницы sync, wrapper, статистика | Исправления с регрессионными тестами |
| P2 / открыто | Durable audit, branch ownership, деньги, нагрузка, полный SCA, Docker | Зафиксированы требования и release gates |

Эксплуатируемый P0 не установлен. Не все недостающие защитные проверки классифицированы как уязвимость: например, подмена владельца JTI не доказана как внешне достижимая при нормальном issuer; это усиление инвариантов, P2.

## D. Code Changes

| Модуль | Что и почему изменено | Проверка / риск |
| --- | --- | --- |
| Auth / security | Единая нормализация, UTF-8 ключ, положительные TTL, обязательные expiry/type/subject, один parse access claims, owner/expiry/JTI refresh, fail-closed API-key principal | Unit, реальный filter chain, конкурентный PostgreSQL refresh. Роль access остаётся действующей до expiry |
| Contracts / history / clients | Проверка состава до загрузки; первичный toolId из запроса; flush ревизии; native soft delete; проверка всей истории клиента | Multi-tool lifecycle, busy restore rollback, удалённая история/клиент. Bulk ORM remove остаётся неподдержанным путём |
| Sync | Preflight ревизий до update/close; проверка согласованности id/offlineId; ограничения null/size и устранение лишнего сохранения offlineId | Атомарный rollback и replay. Full snapshot/optional revision остаются текущим протоколом |
| Availability / stats | Счёт пригодных экземпляров вместо вычитания числа записей; валидация периода; реальный IN_REPAIR count | PostgreSQL booking race/adjacency/count; unit stats. Производительность под нагрузкой не измерена |
| Excel | Передаются все инструменты; листы по 5; очищается исходная формула перед значением; mutable fallback list | Реальный шаблон открывается POI, все 10 имён присутствуют. Печатный вид/юридический макет требуют проверки |
| JSON / uploads / errors | Ограничен граф каталога; proxy metadata не сериализуется; MIME/filename выдачи безопасны; 400/413 вместо случайного 500 | Сначала воспроизведена JSON depth 1001; затем regression и реальный HTTP categories 200. Вложенный граф/ошибки invalid since требуют проверки потребителей |
| Trader sync | Максимум 200, стабильный id sort, только parse-specific catch | Ошибки БД не маскируются полной выборкой. Timestamp paging всё ещё не commit-order протокол |
| Delivery / tests | Wrapper исправлен, POI обновлён, Testcontainers или явная `_test` БД, старые mock dependencies восстановлены, GitHub Actions и container hardening | Java17/21 verify и JAR smoke. Контейнер/CI не запускались |

Денежные Double/JSON-типы, URL и существующие Flyway-файлы не менялись. Изменения поведения: невалидный trader since → 400; page size ограничен 200; каталог не разворачивает рекурсивный граф; активные файлы скачиваются; PostgreSQL host publishing привязан к loopback. Для последнего есть [план rollout](DEPLOYMENT.md) с проверкой удалённых DB consumers.

## E. Security Review

SecurityRoutesTest проверяет фактический SecurityFilterChain для 13 CRM/admin семейств, trader sync, public login, profile и bearer failures. JwtSecurityTest покрывает тип/подпись/expiry/subject. Refresh race проверен двумя независимыми транзакциями: максимум одна успешная rotation, replay invalidation коммитится и новый refresh после replay не принимается. API-key unit tests проверяют владельца, malformed principal и null approval.

JSON категорий больше не обходит instance/rental/client graph; HTTP smoke подтвердил ответ 200 на заполненном каталоге без instances. Передача пользовательской строки `=HYPERLINK(...)` в Excel остаётся STRING. Не проверялись все возможные форматы загружаемых файлов и полная схема image decoding.

Остались: немедленный access revoke отсутствует (TTL 15 минут), app-level brute-force limiting не реализован, production CORS/gateway неизвестны, durable audit отсутствует. [SECURITY.md](SECURITY.md) описывает точные гарантии. POI advisory проверен по [Apache](https://poi.apache.org/); [Spring advisories](https://spring.io/security/) просмотрены выборочно, полного заключения об отсутствии CVE нет.

## F. Database Review

Новые миграции не добавлены; `git diff --exit-code -- src/main/resources/db/migration` проходит. [Хеши](audit/MIGRATION_HASHES.csv). Реальный PostgreSQL 17.6: 13 миграций на пустой схеме, повтор без изменений, V16 → V17 с историческими несколькими инструментами, блокировка populated V1 без удаления строки, отказ при неверном checksum.

Soft delete сохраняет rental_document_tools; удалённые договоры не видны full snapshot, но блокируют удаление исторического клиента. Уникальный offlineId и транзакционный receipt replay/rollback проверены. Старые V6/V11/V12 не переписаны; callback блокирует опасные legacy-переходы. Production restore rehearsal не выполнялся. Нет оснований утверждать, что история всех фактических инсталляций проверена. Сведения об исходном непреднамеренном применении V17 — в разделе A.

## G. Performance Review

Production baseline, CPU/GC/heap, p95/p99, EXPLAIN ANALYZE, время ожидания lock, query-count load профили и замеры throughput **не выполнялись**. Нет заявления об ускорении на процент или устранении всех N+1.

Изменён алгоритм доступности: вместо вычитания количества rental/booking записей используется один count по кандидатам с NOT EXISTS. Это исправление корректности, подтверждённое данными, а не доказанный performance gain. Пагинация trader sync ограничена; global snapshot, history/search и глобальная сериализация записей требуют отдельной нагрузки и согласования протокола.

## H. Test Results

Команды выполнялись из корня проекта; локальный Maven cache указан через `-Dmaven.repo.local=C:\123321\!!misha\audit\.m2`. Интеграционные прогоны используют `TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:55439/tunduk_audit_test`, user audit_test, новые схемы.

| Проверка | Факт |
| --- | --- |
| Исходный `mvnw.cmd -B clean verify` | Wrapper падает до Maven, null Target |
| Direct Maven исходного кода | Компиляция успешна; остановлен из-за подключения к локальной business DB и применения V17 |
| Исходный `test` с datasource port 1/Flyway disabled | 84 tests, 1 failure, 6 errors, 0 skipped; baseline изолирован от рабочей БД |
| Новые Auth/API-key/contract unit tests до исправлений | 11 tests, 7 failures; после исправлений 11/11 |
| Новый PostgreSQL count regression до исправления | expected 1, actual 0; исправлен |
| Новый soft-delete regression до исправления | expected retained history row 1, actual 0; исправлен |
| Новый ten-tool XLSX до исправления | Missing tool 6; после правки roundtrip проходит |
| Новый category serialization до исправления | 7 tests, 1 error: JSON nesting depth 1001; после правки проходит |
| `mvnw.cmd -o -B ... clean verify`, Java 21.0.12 | **138 / 0 failures / 0 errors / 0 skipped**, BUILD SUCCESS, 44.539 s |
| Та же команда с JAVA_HOME=jdk-17, Java 17.0.12 | **138 / 0 / 0 / 0**, BUILD SUCCESS, 46.213 s |
| JAR artifact inspection | 227 application classes, Java class major 61, POI/JJWT/Flyway dependencies присутствуют |
| Java17 `java -jar` на port 18089 / новой тестовой схеме | Запуск успешен; HTTP login → catalog/client → contract create/close → XLSX/sync успешен |
| HTTP XLSX | 200, 419273 bytes; fullSnapshot=true, 1 test contract; categories 200 без instances |
| `git diff --check` / migration diff | Успешно / миграции неизменны |

Логи команд лежат локально в игнорируемых `docs/audit/*.log`. [HTTP summary](audit/HTTP_SMOKE.json), [resolved dependencies](audit/DEPENDENCIES.txt). Первая HTTP-проба с неполным legacy payload корректно получила 400; итоговая проба с полными DTO прошла и останавливалась при любой ошибке. Результаты тестов не заменяют coverage-метрику; требуемые 90% не измерены. Старые тесты не удалялись и не отключались.

Пересчёт XML подтвердил **138 уникальных testcase nodes**, без failure/error/skipped: [машинный итог](audit/TEST_RESULTS.json). Простая сумма атрибута tests у корневых XML даёт 122: два отчёта с nested-классами (AuthServiceTest и ClientDirectoryServiceTest) содержат по 8 testcase при корневом tests=0. Для итогового числа использованы фактические testcase и совпадающий Maven total, а не некорректная сумма metadata.

## I. CI / Docker

Добавлен `.github/workflows/verify.yml`: Java17, Maven cache, full clean verify с PostgreSQL Testcontainers, Compose template validation, Docker build, test-report artifact. Ошибки не подавляются; deploy/publish нет. Workflow в GitHub не исполнялся в этой сессии.

Docker CLI 29.1.3 установлен, engine недоступен. Compose syntax проверен; Docker build/start/non-root runtime не подтверждены. Добавлены non-root пользователь, .dockerignore, PostgreSQL healthcheck/dependency readiness, loopback publishing и согласование POSTGRES_DB/JDBC URL. Native PostgreSQL/JAR smoke не выдаётся за Docker-тест.

## J. Remaining Technical Debt

Приоритет: закрыть release gates SCA/container/frontend; определить правила branch ownership и access revocation; разработать committed durable audit/retention; согласовать денежные scale/rounding/price snapshots; измерить global lock, snapshots, history/search N+1; заменить неинформативные mock smoke tests поведением. Legacy bulk deletion/hard purge не использовать без политики архивации. Рабочие часы бронирования, import/DTO/card/report edge cases нуждаются в продолжении предметного аудита, без изменения бизнес-правил на предположениях.

Adversarial self-review выявил и исправил JSON recursion и формульные кеши после первоначальных исправлений. Проверена регрессия update+close в одном sync batch после добавления flush. Рассмотрены clear persistence context в soft delete, сохранность истории после удаления, совместимость дополнительных XLSX листов, отсутствие ветвевой изоляции и граница применимости тестов. Отдельного внешнего/человеческого review не было.

## K. Production Readiness

Это качественная инженерная оценка на имеющихся доказательствах, не результат формального стандарта или pentest.

| Направление | /10 | Обоснование |
| --- | --- | --- |
| Architecture | 6 | Слои и существующие делегаты понятны; legacy API/branch ownership не унифицированы |
| Code Quality | 6 | Минимальные проверенные исправления; DTO/import/search долг остаётся |
| Security | 6 | Реальная security/refresh проверка и POI update; нет полного SCA, throttling и немедленного revoke |
| Database | 7 | PostgreSQL migration/history tests, immutable SQL; production restore/data rehearsal отсутствует |
| Concurrency | 7 | Реальные независимые транзакции/lock/replay/rollback; только два конкурента и без нагрузочного профиля |
| Reliability | 6 | Atomic operations и regression evidence; durable audit/app readiness остаются |
| Testing | 6 | 138 зелёных на Java17/21 и HTTP smoke; много старых smoke tests и нет coverage/всех edge cases |
| Performance | 4 | Исправлена корректность count, ограничены trader pages; production измерений нет |
| Observability | 3 | Есть логи; durable audit, correlation/tracing/metrics не реализованы |
| Deployment | 5 | CI и container hardening подготовлены; Docker/GitHub runtime не проверен |
| Documentation | 8 | Архитектура, 82 endpoints, риски, запуск/тесты/sync; legacy guide явно помечен |
| Overall | 6 | Существенные подтверждённые исправления; release gates остаются открытыми |

## L. Release Checklist

- Проверить фактическое первоначальное применение V17 к локальной kyrgyzCrm/public; не выполнять автоматический откат/repair/clean.
- Провести внешний review diff, особенно native soft delete/история, JWT политики и JSON-контракт каталога.
- Выполнить GitHub CI и Docker build/start/health/shutdown на целевой платформе, проверить текущие Java17/base-image patches.
- Выполнить полный dependency/container vulnerability scan; принять решение о поддерживаемых maintenance updates без major-миграции вслепую.
- Восстановить backup в отдельную среду и проверить миграции с историческими данными, offlineId duplicates и реальными consumers.
- Проверить frontend/внешние trader consumers: JSON вложенность, 400 для invalid since, pagination cap, sync fullSnapshot replacement и refresh serialization.
- Согласовать печатный XLSX с 1/5/6/10 инструментами, даты/залог/стоимости; POI roundtrip не заменяет print review.
- Проверить CORS/TLS/auth throttling/readiness и потребителей PostgreSQL до перехода host binding на loopback.
- Не включать branch-limited accounts, incremental cursor или hard purge без соответствующей модели/протокола/политики.

Изменения не отправлялись в main/remote и не разворачивались в production. После проверок временный JAR и тестовый PostgreSQL остановлены; порты 18089/55439 не слушают. Данные тестового кластера сохранены для диагностики.
