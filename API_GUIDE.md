# 📚 Полный гайд по HTTP API для фронтенд-разработчика

## 🎯 Введение

Этот гайд содержит **ВСЕ** HTTP запросы, которые можно делать к бэкенду. Каждый запрос описан максимально подробно с примерами.

### Базовый URL
```
http://localhost:8080
```

### Важная информация об аутентификации

1. **Токены доступа (Access Token)**: Используются для авторизации запросов
2. **Refresh Token**: Используется для обновления access token
3. **Где брать токены**: После успешного логина или регистрации они приходят в теле ответа
4. **Как отправлять токен**: В заголовке `Authorization` в формате `Bearer {accessToken}`

### Формат заголовков

Для запросов, требующих аутентификации:
```
Authorization: Bearer {ваш_access_token}
Content-Type: application/json
```

---

## 🔐 1. АУТЕНТИФИКАЦИЯ

Все эндпоинты аутентификации находятся по пути `/api/auth`

### 1.1. Регистрация нового пользователя

**Эндпоинт:** `POST /api/auth/register`

**Требует аутентификации:** ❌ НЕТ (публичный эндпоинт)

**Что отправляем:**
```json
{
  "fullName": "Иван Иванов",        // опционально
  "email": "ivan@example.com",      // ОБЯЗАТЕЛЬНО: либо email, либо phone
  "phone": "+996555123456",          // ОБЯЗАТЕЛЬНО: либо email, либо phone
  "password": "мой_пароль123"        // ОБЯЗАТЕЛЬНО
}
```

**Важно:** 
- Должен быть указан **хотя бы один** из: `email` или `phone`
- `password` обязателен
- `fullName` можно не указывать

**Пример запроса (JavaScript/Fetch):**
```javascript
const response = await fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'ivan@example.com',
    password: 'мой_пароль123',
    fullName: 'Иван Иванов'
  })
});

const data = await response.json();
```

**Что получаем в ответе (200 OK):**

**В теле ответа:**
```json
{
  "userId": 1,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**В заголовках ответа:**
- `Authorization: Bearer {opaque_token}` - непрозрачный токен (можно игнорировать)
- `Access-Token: {opaque_token}` - непрозрачный токен (можно игнорировать)
- `Token-Type: Bearer`
- `Uid: {email или phone}`
- `Client: {client_id}`

**Что делать с токенами:**
1. Сохраните `accessToken` - он нужен для всех защищенных запросов
2. Сохраните `refreshToken` - он нужен для обновления access token
3. Используйте `accessToken` в заголовке `Authorization: Bearer {accessToken}`

**Ошибки:**
- **409 Conflict**: Пользователь с таким email/phone уже существует
  ```json
  {
    "error": "User already exists"
  }
  ```

---

### 1.2. Вход в систему (Login)

**Эндпоинт:** `POST /api/auth/login`

**Требует аутентификации:** ❌ НЕТ (публичный эндпоинт)

**Что отправляем:**
```json
{
  "email": "ivan@example.com",      // либо email, либо phone
  "phone": "+996555123456",          // либо email, либо phone
  "password": "мой_пароль123"        // ОБЯЗАТЕЛЬНО
}
```

**Важно:** 
- Должен быть указан **хотя бы один** из: `email` или `phone`
- `password` обязателен

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    email: 'ivan@example.com',
    password: 'мой_пароль123'
  })
});

const data = await response.json();
```

**Что получаем в ответе (200 OK):**

**В теле ответа:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**В заголовках ответа:**
- Те же заголовки, что и при регистрации

**Ошибки:**
- **401 Unauthorized**: Неверный email/phone или пароль

---

### 1.3. Обновление токена (Refresh Token)

**Эндпоинт:** `POST /api/auth/refresh` или `GET /api/auth/refresh`

**Требует аутентификации:** ❌ НЕТ (публичный эндпоинт)

**Когда использовать:** Когда ваш `accessToken` истек (обычно через 15 минут)

**POST запрос - что отправляем:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**GET запрос - параметры URL:**
```
GET /api/auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Пример POST запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/auth/refresh', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    refreshToken: 'ваш_refresh_token'
  })
});

const data = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "accessToken": "новый_access_token...",
  "refreshToken": "новый_refresh_token..."
}
```

**Важно:** 
- Старый refresh token становится недействительным
- Сохраните новые токены и используйте их дальше

**Ошибки:**
- **400 Bad Request**: Не указан refreshToken
  ```json
  {
    "error": "refreshToken is required"
  }
  ```
- **401 Unauthorized**: Неверный или истекший refresh token
  ```json
  {
    "error": "invalid refresh token"
  }
  ```

---

## 👤 2. ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ

### 2.1. Получить информацию о текущем пользователе

**Эндпоинт:** `GET /api/profile/me`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Что отправляем:** Ничего (только заголовок с токеном)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/profile/me', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const userData = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "fullName": "Иван Иванов",
  "birthDate": "1990-01-15",
  "residentialAddress": "г. Бишкек, ул. Ленина, д. 1",
  "registeredAddress": "г. Бишкек, ул. Ленина, д. 1",
  "email": "ivan@example.com",
  "phone": "+996555123456",
  "consentPersonalData": true,
  "consentPrivacyPolicy": true,
  "consentUserAgreement": true,
  "simpleMode": false
}
```

**Ошибки:**
- **401 Unauthorized**: Неверный или отсутствующий токен
- **404 Not Found**: Пользователь не найден

---

## 👥 3. АДМИН - УПРАВЛЕНИЕ ПОЛЬЗОВАТЕЛЯМИ

**Важно:** Все эндпоинты в разделе `/api/admin/**` требуют роль **ADMIN**

### 3.1. Поиск пользователей

**Эндпоинт:** `GET /api/admin/users`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры запроса (все опциональны):**
- `query` - поисковый запрос (поиск по имени, email, phone)
- `tags` - фильтр по тегам (через запятую)
- `hasDocuments` - есть ли документы (true/false)
- `minDocs` - минимальное количество документов
- `maxDocs` - максимальное количество документов
- `simpleMode` - простой режим (true/false)
- `consentPersonalData` - согласие на обработку данных (true/false)
- `contractNumber` - номер договора
- `sort` - поле для сортировки (по умолчанию: "id")
- `direction` - направление сортировки: "asc" или "desc" (по умолчанию: "asc")

**Пример запроса:**
```javascript
// Простой поиск
const response = await fetch('http://localhost:8080/api/admin/users?query=Иван', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

// Сложный поиск с фильтрами
const response2 = await fetch(
  'http://localhost:8080/api/admin/users?query=Иван&hasDocuments=true&minDocs=1&sort=fullName&direction=asc',
  {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ваш_access_token',
      'Content-Type': 'application/json'
    }
  }
);

const users = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "fullName": "Иван Иванов",
    "email": "ivan@example.com",
    "phone": "+996555123456",
    // ... другие поля
  },
  {
    "id": 2,
    "fullName": "Петр Петров",
    // ...
  }
]
```

**Ошибки:**
- **401 Unauthorized**: Неверный токен
- **403 Forbidden**: У вас нет прав администратора

---

## 🏢 4. АДМИН - УПРАВЛЕНИЕ КЛИЕНТАМИ

**Важно:** Все эндпоинты требуют роль **ADMIN**

### 4.1. Поиск клиентов

**Эндпоинт:** `GET /api/admin/clients/search`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры запроса (все опциональны):**
- `query` - поисковый запрос
- `tag` - фильтр по тегу (один тег)
- `hasDocuments` - есть ли документы (true/false)
- `minDocs` - минимальное количество документов
- `maxDocs` - максимальное количество документов
- `contractNumber` - номер договора
- `sort` - поле для сортировки (по умолчанию: "id")
- `direction` - направление сортировки: "asc" или "desc" (по умолчанию: "asc")

**Пример запроса:**
```javascript
const response = await fetch(
  'http://localhost:8080/api/admin/clients/search?query=Иван&hasDocuments=true',
  {
    method: 'GET',
    headers: {
      'Authorization': 'Bearer ваш_access_token',
      'Content-Type': 'application/json'
    }
  }
);

const clients = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "fullName": "Иван Иванов",
    "phone": "+996555123456",
    "email": "ivan@example.com",
    "tag": "ПОСТОЯННЫЙ",
    // ... другие поля
  }
]
```

---

### 4.2. Получить всех клиентов

**Эндпоинт:** `GET /api/admin/clients`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/admin/clients', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const clients = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "fullName": "Иван Иванов",
    "phone": "+996555123456",
    "email": "ivan@example.com",
    "tag": "ПОСТОЯННЫЙ"
  },
  // ... другие клиенты
]
```

---

### 4.3. Получить клиента по ID

**Эндпоинт:** `GET /api/admin/clients/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID клиента (в URL пути)

**Пример запроса:**
```javascript
const clientId = 1;
const response = await fetch(`http://localhost:8080/api/admin/clients/${clientId}`, {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const client = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "fullName": "Иван Иванов",
  "phone": "+996555123456",
  "email": "ivan@example.com",
  "tag": "ПОСТОЯННЫЙ",
  "documents": [
    {
      "id": 1,
      "contractNumber": "ДОГ-001",
      // ... другие поля документа
    }
  ]
}
```

**Ошибки:**
- **404 Not Found**: Клиент не найден

---

### 4.4. Создать нового клиента

**Эндпоинт:** `POST /api/admin/clients/create`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Что отправляем:**
```json
{
  "fullName": "Иван Иванов",
  "phone": "+996555123456",
  "email": "ivan@example.com",
  "tag": "ОБЫЧНЫЙ"
}
```

**Доступные значения для `tag`:**
- `"УСТАРЕВШИЙ"`
- `"В_АРЕНДЕ"`
- `"ДОЛЖНИК"`
- `"ПОСТОЯННЫЙ"`
- `"ОБЫЧНЫЙ"`

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/admin/clients/create', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    fullName: 'Иван Иванов',
    phone: '+996555123456',
    email: 'ivan@example.com',
    tag: 'ОБЫЧНЫЙ'
  })
});

const newClient = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "fullName": "Иван Иванов",
  "phone": "+996555123456",
  "email": "ivan@example.com",
  "tag": "ОБЫЧНЫЙ"
}
```

---

### 4.5. Обновить клиента

**Эндпоинт:** `PUT /api/admin/clients/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID клиента (в URL пути)

**Что отправляем:**
```json
{
  "fullName": "Иван Иванов (обновлено)",
  "phone": "+996555123456",
  "email": "ivan_new@example.com",
  "tag": "ПОСТОЯННЫЙ"
}
```

**Важно:** Все поля опциональны, но если отправляете объект, лучше указать все поля

**Пример запроса:**
```javascript
const clientId = 1;
const response = await fetch(`http://localhost:8080/api/admin/clients/${clientId}`, {
  method: 'PUT',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    fullName: 'Иван Иванов (обновлено)',
    phone: '+996555123456',
    email: 'ivan_new@example.com',
    tag: 'ПОСТОЯННЫЙ'
  })
});

const updatedClient = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "fullName": "Иван Иванов (обновлено)",
  "phone": "+996555123456",
  "email": "ivan_new@example.com",
  "tag": "ПОСТОЯННЫЙ"
}
```

---

### 4.6. Удалить клиента

**Эндпоинт:** `DELETE /api/admin/clients/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID клиента (в URL пути)

**Пример запроса:**
```javascript
const clientId = 1;
const response = await fetch(`http://localhost:8080/api/admin/clients/${clientId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const result = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "message": "Клиент удалён"
}
```

---

### 4.7. Получить документы клиента

**Эндпоинт:** `GET /api/admin/clients/{clientId}/documents`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{clientId}` - ID клиента (в URL пути)

**Пример запроса:**
```javascript
const clientId = 1;
const response = await fetch(`http://localhost:8080/api/admin/clients/${clientId}/documents`, {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const documents = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "contractNumber": "ДОГ-001",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "amount": 5000.0,
    "createdAt": "2024-01-01T10:00:00",
    "clientId": 1
  },
  // ... другие документы
]
```

**Ошибки:**
- **404 Not Found**: Клиент не найден

---

## 📄 5. АДМИН - УПРАВЛЕНИЕ ДОКУМЕНТАМИ АРЕНДЫ

**Важно:** Все эндпоинты требуют роль **ADMIN**

### 5.1. Создать документ аренды

**Эндпоинт:** `POST /api/admin/documents/create`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Что отправляем:**
```json
{
  "clientId": 1,
  "contractNumber": "ДОГ-001",
  "categoryId": 1,
  "toolId": 5
}
```

**Поля:**
- `clientId` - ID клиента (ОБЯЗАТЕЛЬНО)
- `contractNumber` - номер договора (ОБЯЗАТЕЛЬНО)
- `categoryId` - ID категории инструмента (ОБЯЗАТЕЛЬНО)
- `toolId` - ID физического инструмента (ОБЯЗАТЕЛЬНО)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/admin/documents/create', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    clientId: 1,
    contractNumber: 'ДОГ-001',
    categoryId: 1,
    toolId: 5
  })
});

const document = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "contractNumber": "ДОГ-001",
  "category": "Электроинструменты",
  "toolName": "Дрель Bosch",
  "serialNumber": "SN123456",
  "startDate": "2024-01-01",
  "endDate": null,
  "amount": null,
  "toolId": 5
}
```

---

### 5.2. Получить все документы

**Эндпоинт:** `GET /api/admin/documents`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/admin/documents', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const documents = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "contractNumber": "ДОГ-001",
    "category": "Электроинструменты",
    "toolName": "Дрель Bosch",
    "serialNumber": "SN123456",
    "startDate": "2024-01-01",
    "endDate": "2024-01-31",
    "amount": 5000.0,
    "toolId": 5
  },
  // ... другие документы
]
```

---

### 5.3. Получить документ по ID

**Эндпоинт:** `GET /api/admin/documents/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID документа (в URL пути)

**Пример запроса:**
```javascript
const documentId = 1;
const response = await fetch(`http://localhost:8080/api/admin/documents/${documentId}`, {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const document = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "contractNumber": "ДОГ-001",
  "category": "Электроинструменты",
  "toolName": "Дрель Bosch",
  "serialNumber": "SN123456",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "amount": 5000.0,
  "toolId": 5
}
```

---

### 5.4. Обновить документ

**Эндпоинт:** `PUT /api/admin/documents/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID документа (в URL пути)

**Что отправляем:**
```json
{
  "contractNumber": "ДОГ-001-обновлено",
  "startDate": "2024-01-01",
  "endDate": "2024-02-01",
  "amount": 6000.0,
  "categoryId": 1,
  "toolId": 5
}
```

**Поля (все опциональны):**
- `contractNumber` - номер договора
- `startDate` - дата начала (формат: "YYYY-MM-DD")
- `endDate` - дата окончания (формат: "YYYY-MM-DD")
- `amount` - сумма аренды (число)
- `categoryId` - ID категории
- `toolId` - ID инструмента

**Пример запроса:**
```javascript
const documentId = 1;
const response = await fetch(`http://localhost:8080/api/admin/documents/${documentId}`, {
  method: 'PUT',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    contractNumber: 'ДОГ-001-обновлено',
    startDate: '2024-01-01',
    endDate: '2024-02-01',
    amount: 6000.0,
    categoryId: 1,
    toolId: 5
  })
});

const updatedDocument = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "contractNumber": "ДОГ-001-обновлено",
  "category": "Электроинструменты",
  "toolName": "Дрель Bosch",
  "serialNumber": "SN123456",
  "startDate": "2024-01-01",
  "endDate": "2024-02-01",
  "amount": 6000.0,
  "toolId": 5
}
```

---

### 5.5. Закрыть документ (возврат инструмента)

**Эндпоинт:** `POST /api/admin/documents/{id}/close`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID документа (в URL пути)

**Что отправляем:** Ничего (только заголовок с токеном)

**Пример запроса:**
```javascript
const documentId = 1;
const response = await fetch(`http://localhost:8080/api/admin/documents/${documentId}/close`, {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const closedDocument = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "id": 1,
  "contractNumber": "ДОГ-001",
  "category": "Электроинструменты",
  "toolName": "Дрель Bosch",
  "serialNumber": "SN123456",
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "amount": 5000.0,
  "toolId": 5
}
```

**Что делает:** Закрывает документ аренды (возвращает инструмент)

---

### 5.6. Удалить документ

**Эндпоинт:** `DELETE /api/admin/documents/{id}`

**Требует аутентификации:** ✅ ДА + роль ADMIN

**Параметры:**
- `{id}` - ID документа (в URL пути)

**Пример запроса:**
```javascript
const documentId = 1;
const response = await fetch(`http://localhost:8080/api/admin/documents/${documentId}`, {
  method: 'DELETE',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const result = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
{
  "status": "deleted"
}
```

---

## 🔧 6. ИНСТРУМЕНТЫ

### 6.1. Получить все инструменты

**Эндпоинт:** `GET /api/tools/all`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/all', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const tools = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "serialNumber": "SN123456",
    "template": {
      "id": 1,
      "name": "Дрель Bosch",
      "categoryName": "Электроинструменты",
      "description": "Мощная дрель для работы",
      "available": true
    }
  },
  // ... другие инструменты
]
```

---

### 6.2. Получить инструменты на сегодня

**Эндпоинт:** `GET /api/tools/today`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/today', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const tools = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "serialNumber": "SN123456",
    "template": {
      "id": 1,
      "name": "Дрель Bosch",
      "categoryName": "Электроинструменты",
      "description": "Мощная дрель для работы",
      "available": true
    }
  },
  // ... инструменты на сегодня
]
```

---

### 6.3. Создать инструмент

**Эндпоинт:** `POST /api/tools/create`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Что отправляем:**
```json
{
  "templateId": 1,
  "serialNumber": "SN123456",
  "contractId": 5
}
```

**Поля:**
- `templateId` - ID шаблона инструмента (ОБЯЗАТЕЛЬНО)
- `serialNumber` - серийный номер (ОБЯЗАТЕЛЬНО)
- `contractId` - ID договора (опционально)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/create', {
  method: 'POST',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    templateId: 1,
    serialNumber: 'SN123456',
    contractId: 5
  })
});

const newTool = await response.json();
```

**Что получаем в ответе (201 Created):**
```json
{
  "id": 1,
  "serialNumber": "SN123456",
  "template": {
    "id": 1,
    "name": "Дрель Bosch",
    "categoryName": "Электроинструменты",
    "description": "Мощная дрель для работы",
    "available": true
  }
}
```

---

## 📁 7. КАТЕГОРИИ ИНСТРУМЕНТОВ

### 7.1. Получить все категории

**Эндпоинт:** `GET /api/tools/categories`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/categories', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const categories = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Электроинструменты"
  },
  {
    "id": 2,
    "name": "Ручной инструмент"
  },
  // ... другие категории
]
```

---

## 📋 8. ШАБЛОНЫ ИНСТРУМЕНТОВ

### 8.1. Получить все шаблоны

**Эндпоинт:** `GET /api/tools/templates`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/templates', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const templates = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Дрель Bosch",
    "categoryName": "Электроинструменты",
    "description": "Мощная дрель для работы",
    "available": true
  },
  // ... другие шаблоны
]
```

---

### 8.2. Получить шаблоны по категории

**Эндпоинт:** `GET /api/tools/templates/category/{categoryId}`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Параметры:**
- `{categoryId}` - ID категории (в URL пути)

**Пример запроса:**
```javascript
const categoryId = 1;
const response = await fetch(`http://localhost:8080/api/tools/templates/category/${categoryId}`, {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const templates = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Дрель Bosch",
    "categoryName": "Электроинструменты",
    "description": "Мощная дрель для работы",
    "available": true
  },
  // ... шаблоны этой категории
]
```

---

### 8.3. Получить доступные шаблоны

**Эндпоинт:** `GET /api/tools/templates/available`

**Требует аутентификации:** ✅ ДА (нужен access token)

**Пример запроса:**
```javascript
const response = await fetch('http://localhost:8080/api/tools/templates/available', {
  method: 'GET',
  headers: {
    'Authorization': 'Bearer ваш_access_token',
    'Content-Type': 'application/json'
  }
});

const templates = await response.json();
```

**Что получаем в ответе (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Дрель Bosch",
    "categoryName": "Электроинструменты",
    "description": "Мощная дрель для работы",
    "available": true
  },
  // ... только доступные шаблоны (available: true)
]
```

---

## 🎯 КРАТКАЯ ШПАРГАЛКА

### Коды ответов HTTP

- **200 OK** - запрос успешен
- **201 Created** - ресурс создан
- **400 Bad Request** - неверный запрос (проверьте данные)
- **401 Unauthorized** - не авторизован (проверьте токен)
- **403 Forbidden** - нет доступа (нужна роль ADMIN)
- **404 Not Found** - ресурс не найден
- **409 Conflict** - конфликт (например, пользователь уже существует)

### Где брать данные для запросов

1. **Access Token**: После логина/регистрации в поле `accessToken` ответа
2. **Refresh Token**: После логина/регистрации в поле `refreshToken` ответа
3. **ID клиента**: Из ответа при создании клиента или из списка клиентов
4. **ID документа**: Из ответа при создании документа или из списка документов
5. **ID категории**: Из списка категорий (`/api/tools/categories`)
6. **ID шаблона**: Из списка шаблонов (`/api/tools/templates`)
7. **ID инструмента**: Из списка инструментов (`/api/tools/all`)

### Типичный workflow

1. **Регистрация/Логин** → получаете токены
2. **Сохраняете токены** (localStorage/sessionStorage)
3. **Используете accessToken** в заголовке `Authorization: Bearer {token}`
4. **Когда токен истек** → используете refreshToken для получения новых токенов
5. **Для админ-операций** → нужна роль ADMIN

### Пример полного цикла работы с API

```javascript
// 1. Регистрация
const registerResponse = await fetch('http://localhost:8080/api/auth/register', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
});
const { accessToken, refreshToken } = await registerResponse.json();

// Сохраняем токены
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// 2. Получаем профиль
const profileResponse = await fetch('http://localhost:8080/api/profile/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
});
const profile = await profileResponse.json();

// 3. Если токен истек, обновляем
if (profileResponse.status === 401) {
  const refreshResponse = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  const { accessToken: newAccessToken, refreshToken: newRefreshToken } = await refreshResponse.json();
  localStorage.setItem('accessToken', newAccessToken);
  localStorage.setItem('refreshToken', newRefreshToken);
}
```

---

## ❓ Часто задаваемые вопросы

**Q: Как понять, что токен истек?**  
A: Сервер вернет статус 401 Unauthorized. Тогда используйте refresh token.

**Q: Как долго действует access token?**  
A: 15 минут (900000 миллисекунд)

**Q: Как долго действует refresh token?**  
A: 7 дней (604800000 миллисекунд)

**Q: Можно ли использовать один токен для нескольких запросов?**  
A: Да, пока он не истек.

**Q: Что делать, если забыл токен?**  
A: Нужно заново войти через `/api/auth/login`

**Q: Как получить роль ADMIN?**  
A: Роль назначается на бэкенде, обратитесь к администратору.

---

**Удачи в разработке! 🚀**

