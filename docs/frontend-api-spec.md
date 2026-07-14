# 會議室預約系統 — 前端串接規格書

> 版本：v1.0　更新日期：2026-06-15　後端技術：Spring Boot 3.4.5 / Java 17

---

## 目錄

1. [整體架構](#1-整體架構)
2. [通用規範](#2-通用規範)
3. [認證機制](#3-認證機制)
4. [API 列表速覽](#4-api-列表速覽)
5. [認證模組 `/auth`](#5-認證模組-auth)
6. [用戶模組 `/users`](#6-用戶模組-users)
7. [會議室模組 `/rooms`](#7-會議室模組-rooms)
8. [預約模組 `/records`](#8-預約模組-records)
9. [AI 聊天模組 `/chat`](#9-ai-聊天模組-chat)
10. [錯誤處理](#10-錯誤處理)
11. [OAuth2 流程](#11-oauth2-流程)
12. [資料型別參考](#12-資料型別參考)

---

## 1. 整體架構

```
前端 (Vue 3 + Vite)
  │
  ├── JWT Bearer Token（所有需驗證的 API）
  │
後端 (Spring Boot)  http://localhost:8080
  ├── /auth        → 認證（註冊/登入/OAuth2/Token刷新）
  ├── /users       → 用戶管理
  ├── /rooms       → 會議室管理
  ├── /records     → 預約管理（單筆/批次/週期）
  └── /chat        → AI 聊天室預約
```

**開發環境 Base URL：** `http://localhost:8080`

**Swagger UI：** `http://localhost:8080/swagger-ui.html`（開發環境可直接測試所有 API）

---

## 2. 通用規範

### 2.1 請求標頭

| 標頭 | 必填 | 說明 |
|------|------|------|
| `Content-Type` | 是（POST/PUT） | `application/json` |
| `Authorization` | 視 API 而定 | `Bearer {accessToken}` |

### 2.2 統一回應格式

所有 API 均回傳以下結構：

```json
{
  "code": 200,
  "msg": "success",
  "data": { ... }
}
```

| 欄位 | 型別 | 說明 |
|------|------|------|
| `code` | `number` | 業務狀態碼（非 HTTP 狀態碼） |
| `msg` | `string` | 訊息（成功時為 `"success"`） |
| `data` | `any \| null` | 回應資料；錯誤時為 `null` |

**注意：** HTTP 狀態碼一律為 200，業務是否成功看 `code` 欄位。

### 2.3 時間格式

所有時間欄位使用 **ISO 8601 with Offset** 格式，台灣時區為 `+08:00`：

```
2024-01-15T09:00:00+08:00
```

前端發送時間時請帶時區偏移，例如：

```json
{
  "startedTime": "2024-01-15T09:00:00+08:00",
  "endedTime":   "2024-01-15T10:00:00+08:00"
}
```

### 2.4 UUID 格式

所有 ID 欄位均為 UUID 字串，例如：`"550e8400-e29b-41d4-a716-446655440000"`

---

## 3. 認證機制

### 3.1 JWT Token

登入成功後取得兩個 Token：

| Token | 說明 | 建議存放 |
|-------|------|---------|
| `accessToken` | 呼叫 API 使用，有效期較短 | `localStorage` 或 `sessionStorage` |
| `refreshToken` | 刷新 accessToken 用，有效期較長 | `localStorage`（HttpOnly Cookie 更安全） |

**使用方式：**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**JWT Payload 包含：**
```json
{
  "sub": "userId (UUID)",
  "email": "user@example.com",
  "role": 0,
  "iat": 1700000000,
  "exp": 1700003600
}
```

### 3.2 Token 刷新流程

```
1. 收到 code 401 → accessToken 過期
2. 呼叫 POST /auth/refresh?refreshToken={refreshToken}
3. 取得新的 accessToken + refreshToken
4. 更新本地儲存，重試原始請求
5. 若 refresh 也失敗 → 導向登入頁
```

### 3.3 Role 權限

| `role` 值 | 身份 | 說明 |
|-----------|------|------|
| `0` | 一般用戶 | 可預約、查詢、取消自己的預約 |
| `1` | 管理員 | 額外可管理會議室、取消任何人的預約 |

---

## 4. API 列表速覽

| 方法 | 路徑 | 說明 | 需要登入 | 需要管理員 |
|------|------|------|---------|-----------|
| POST | `/auth/register` | 註冊帳號 | 否 | 否 |
| POST | `/auth/login` | 登入 | 否 | 否 |
| GET  | `/auth/verify-email` | Email 驗證 | 否 | 否 |
| POST | `/auth/refresh` | 刷新 Token | 否 | 否 |
| GET  | `/users` | 所有用戶 | 是 | 否 |
| GET  | `/users/{id}` | 用戶詳情 | 是 | 否 |
| GET  | `/users/check-email` | 檢查 Email | 否 | 否 |
| POST | `/users` | 新增用戶 | 是 | 否 |
| PUT  | `/users` | 更新用戶 | 是 | 否 |
| GET  | `/rooms` | 所有會議室 | 是 | 否 |
| GET  | `/rooms/{id}` | 單一會議室 | 是 | 否 |
| GET  | `/rooms/availability` | 當日全覽 | 是 | 否 |
| GET  | `/rooms/{id}/slots` | 房間時段查詢 | 是 | 否 |
| POST | `/rooms` | 新增會議室 | 是 | 是 |
| PUT  | `/rooms/{id}` | 更新會議室 | 是 | 是 |
| DELETE | `/rooms/{id}` | 停用會議室 | 是 | 是 |
| POST | `/records` | 建立單筆預約 | 是 | 否 |
| POST | `/records/batch` | 批次預約 | 是 | 否 |
| POST | `/records/recurring` | 週期預約 | 是 | 否 |
| GET  | `/records/my` | 我的預約 | 是 | 否 |
| GET  | `/records/{id}` | 預約詳情 | 是 | 否 |
| PUT  | `/records/{id}/cancel` | 取消單筆 | 是 | 否 |
| PUT  | `/records/{id}/cancel-series` | 取消整個週期系列 | 是 | 否 |
| POST | `/chat/send` | 傳訊息給 AI | 是 | 否 |
| DELETE | `/chat/history` | 清除對話歷史 | 是 | 否 |

---

## 5. 認證模組 `/auth`

### 5.1 POST `/auth/register` — 註冊帳號

**需要登入：** 否

**Request Body：**

```json
{
  "userName": "王小明",
  "email": "ming@example.com",
  "password": "Abc12345",
  "company": "某某公司"
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `userName` | `string` | 是 | 不可空白 |
| `email` | `string` | 是 | Email 格式 |
| `password` | `string` | 是 | 8–20 字元 |
| `company` | `string` | 否 | 公司名稱 |

**成功回應 201：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Registration successful. Please check your email to verify your account."
}
```

**前端須知：**
- 註冊後系統自動寄驗證信到填寫的 Email
- **用戶須點擊信中連結驗證後才可登入**
- 可顯示「請前往信箱確認驗證信」提示

---

### 5.2 POST `/auth/login` — 登入

**需要登入：** 否

**Request Body：**

```json
{
  "email": "ming@example.com",
  "password": "Abc12345"
}
```

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "ming@example.com",
    "userName": "王小明",
    "role": 0
  }
}
```

**失敗情境：**

| `code` | `msg` | 原因 |
|--------|-------|------|
| 401 | Invalid email or password | 帳號或密碼錯誤 |
| 403 | Account is inactive | 帳號已停用 |
| 403 | Email not verified. Please check your inbox. | Email 尚未驗證 |

---

### 5.3 GET `/auth/verify-email` — 驗證 Email

**需要登入：** 否

**用途：** 用戶點擊驗證信中的連結後，前端頁面呼叫此 API（或直接由後端 redirect）

**Query Params：**

| 參數 | 必填 | 說明 |
|------|------|------|
| `token` | 是 | 驗證信中的 token（UUID） |

**範例：** `GET /auth/verify-email?token=abc123...`

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Email verified successfully. You can now log in."
}
```

**失敗回應（token 無效或過期）：**

```json
{
  "code": 400,
  "msg": "Invalid or expired verification token",
  "data": null
}
```

**前端須知：**
- 驗證信連結格式：`http://localhost:5173/verify-email?token={token}`
- 前端頁面載入後呼叫此 API，再依結果顯示成功/失敗訊息
- Token 有效期 24 小時

---

### 5.4 POST `/auth/refresh` — 刷新 Token

**需要登入：** 否

**Query Params：**

| 參數 | 必填 | 說明 |
|------|------|------|
| `refreshToken` | 是 | 當前的 refreshToken |

**範例：** `POST /auth/refresh?refreshToken=eyJhbGciOiJIUzI1NiJ9...`

**成功回應：** 同 `/auth/login` 的 `data` 結構

**失敗回應：**

```json
{
  "code": 401,
  "msg": "Invalid or expired refresh token",
  "data": null
}
```

---

## 6. 用戶模組 `/users`

> 所有 `/users` API 皆需帶 `Authorization` Header。

### 6.1 GET `/users` — 查詢所有用戶

**回應 `data`：** `UserVO[]`（陣列）

### 6.2 GET `/users/{id}` — 依 ID 查詢用戶

**Path Variable：** `id` = UUID

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "userName": "王小明",
    "email": "ming@example.com",
    "company": "某某公司",
    "role": 0,
    "status": 1,
    "authProvider": "LOCAL",
    "emailVerified": true,
    "createdTime": "2024-01-10T10:30:00+08:00",
    "updatedTime": "2024-01-10T10:30:00+08:00"
  }
}
```

**注意：** `pwd` 欄位不回傳（已遮蔽）

### 6.3 GET `/users/check-email` — 檢查 Email 是否存在

**用途：** 註冊表單即時驗證

**Query Params：** `email=ming@example.com`

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

`data: true` 表示 Email 已被使用，`false` 表示可以註冊。

---

## 7. 會議室模組 `/rooms`

> 所有 `/rooms` API 皆需帶 `Authorization` Header。

### 7.1 GET `/rooms` — 查詢所有啟用中的會議室

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "roomId": "aaaa-...",
      "roomName": "A 會議室",
      "capacity": 10,
      "location": "3F 左側",
      "status": 1,
      "createdTime": "2024-01-01T00:00:00+08:00",
      "updatedTime": "2024-01-01T00:00:00+08:00"
    }
  ]
}
```

**`status` 說明：** `1` = 啟用，`0` = 停用（此 API 只回傳 status=1 的房間）

### 7.2 GET `/rooms/{id}` — 查詢單一會議室

**Path Variable：** `id` = UUID

回傳單一 `RoomVO` 物件，結構同上。

### 7.3 GET `/rooms/availability` — 當日全部房間可用性（行事曆）

**用途：** 首頁行事曆全覽，顯示當天所有房間的預約狀況

**Query Params：**

| 參數 | 必填 | 格式 | 範例 |
|------|------|------|------|
| `date` | 是 | `yyyy-MM-dd` | `2024-01-15` |

**範例：** `GET /rooms/availability?date=2024-01-15`

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "date": "2024-01-15",
    "rooms": [
      {
        "roomId": "aaaa-...",
        "roomName": "A 會議室",
        "capacity": 10,
        "bookedSlots": [
          {
            "recordId": "bbbb-...",
            "title": "週會",
            "createdBy":"預約者A",
            "startedTime": "2024-01-15T09:00:00+08:00",
            "endedTime": "2024-01-15T10:00:00+08:00"
          },
          {
            "recordId": "cccc-...",
            "title": "專案討論",
            "createdBy":"預約者A",
            "startedTime": "2024-01-15T14:00:00+08:00",
            "endedTime": "2024-01-15T15:30:00+08:00"
          }
        ]
      }
    ]
  }
}
```

**`bookedSlots` 為空陣列** 表示該房間當天完全沒有預約（全天可用）。

### 7.4 GET `/rooms/{id}/slots` — 指定房間某日的預約時段

**Query Params：**

| 參數 | 必填 | 格式 | 範例 |
|------|------|------|------|
| `date` | 是 | `yyyy-MM-dd` | `2024-01-15` |

**範例：** `GET /rooms/aaaa-.../slots?date=2024-01-15`

**回應 `data`：** `RecordVO[]`（完整預約記錄陣列）

---

### 7.5 POST `/rooms` — 新增會議室（管理員）

**需要管理員：** 是（`role = 1`）

**Request Body：**

```json
{
  "roomName": "B 會議室",
  "capacity": 20,
  "location": "4F 右側"
}
```

| 欄位 | 型別 | 必填 | 規則 |
|------|------|------|------|
| `roomName` | `string` | 是 | 不可空白 |
| `capacity` | `number` | 是 | 最小值 1 |
| `location` | `string` | 否 | 地點描述 |

**回應 `data`：** 建立後的 `RoomVO`

### 7.6 PUT `/rooms/{id}` — 更新會議室（管理員）

**需要管理員：** 是

**Request Body：** 同 7.5

**回應 `data`：** 更新後的 `RoomVO`

### 7.7 DELETE `/rooms/{id}` — 停用會議室（管理員，軟刪除）

**需要管理員：** 是

**說明：** 僅設定 `status = 0`，不真正刪除資料

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Room disabled successfully"
}
```

---

## 8. 預約模組 `/records`

> 所有 `/records` API 皆需帶 `Authorization` Header。  
> 預約的 `userId` 由後端從 JWT 自動取得，前端不需傳送。

### 8.1 POST `/records` — 建立單筆預約

**Request Body：**

```json
{
  "roomId": "aaaa-...",
  "title": "週會",
  "reason": "每週例行會議",
  "startedTime": "2024-01-15T09:00:00+08:00",
  "endedTime": "2024-01-15T10:00:00+08:00",
  "reminderTime": "2024-01-15T08:45:00+08:00"
}
```

| 欄位 | 型別 | 必填 | 說明 |
|------|------|------|------|
| `roomId` | `string (UUID)` | 是 | 目標會議室 ID |
| `title` | `string` | 否 | 會議標題 |
| `reason` | `string` | 否 | 使用原因 |
| `startedTime` | `string (OffsetDateTime)` | 是 | 開始時間（含時區） |
| `endedTime` | `string (OffsetDateTime)` | 是 | 結束時間（含時區） |
| `reminderTime` | `string (OffsetDateTime)` | 否 | 提醒時間；系統於此時寄 Email |
| `rrule` | `string` | 否 | **週期預約專用**，單筆請勿填寫 |

**成功回應 `data`：** `RecordVO`（完整預約記錄）

**衝突回應（409）：**

```json
{
  "code": 409,
  "msg": "Room is already booked during this time slot",
  "data": null
}
```

---

### 8.2 POST `/records/batch` — 批次預約

**用途：** 一次預約多個時段（可跨房間）

**規則：** 全部時段通過衝突檢查才會寫入；任一筆衝突則**全部取消**（All-or-Nothing）

**Request Body：** `RecordDTO[]`（陣列）

```json
[
  {
    "roomId": "aaaa-...",
    "title": "研討會 Day 1",
    "startedTime": "2024-01-15T09:00:00+08:00",
    "endedTime": "2024-01-15T17:00:00+08:00"
  },
  {
    "roomId": "bbbb-...",
    "title": "研討會 Day 2",
    "startedTime": "2024-01-16T09:00:00+08:00",
    "endedTime": "2024-01-16T17:00:00+08:00"
  }
]
```

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "count": 2,
    "parentRecordId": null,
    "recordIds": [
      "cccc-...",
      "dddd-..."
    ]
  }
}
```

| `data` 欄位 | 型別 | 說明 |
|------------|------|------|
| `count` | `number` | 成功建立的筆數 |
| `parentRecordId` | `null` | 批次預約此欄永遠為 null |
| `recordIds` | `string[]` | 所有建立的預約 ID |

---

### 8.3 POST `/records/recurring` — 週期預約

**用途：** 依 RRULE 規則自動展開多筆預約，共用同一個 `parentRecordId`

**規則：** 最多展開 **52 筆**，搜尋時間上限 3 年

**Request Body：** 單一 `RecordDTO`，帶 `rrule` 欄位

```json
{
  "roomId": "aaaa-...",
  "title": "每週一站立會議",
  "startedTime": "2024-01-15T09:00:00+08:00",
  "endedTime": "2024-01-15T09:30:00+08:00",
  "reminderTime": "2024-01-15T08:50:00+08:00",
  "rrule": "FREQ=WEEKLY;BYDAY=MO;COUNT=10"
}
```

**RRULE 格式範例：**

| 需求 | `rrule` 值 |
|------|-----------|
| 每週一，共 10 次 | `FREQ=WEEKLY;BYDAY=MO;COUNT=10` |
| 每日，共 5 天 | `FREQ=DAILY;COUNT=5` |
| 每月，共 6 次 | `FREQ=MONTHLY;COUNT=6` |
| 每週一三五，直到年底 | `FREQ=WEEKLY;BYDAY=MO,WE,FR;UNTIL=20241231T000000Z` |
| 每週二四，共 8 次 | `FREQ=WEEKLY;BYDAY=TU,TH;COUNT=8` |

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "count": 10,
    "parentRecordId": "eeee-...",
    "recordIds": [
      "ffff-...",
      "gggg-...",
      "..."
    ]
  }
}
```

| `data` 欄位 | 型別 | 說明 |
|------------|------|------|
| `count` | `number` | 展開的總筆數 |
| `parentRecordId` | `string (UUID)` | 系列共用 ID（取消系列時使用） |
| `recordIds` | `string[]` | 所有展開的預約 ID |

---

### 8.4 GET `/records/my` — 查詢我的預約

**說明：** 依登入 JWT 自動篩選當前用戶的預約，依建立時間降冪排序

**回應 `data`：** `RecordVO[]`

---

### 8.5 GET `/records/{id}` — 查詢預約詳情

**Path Variable：** `id` = UUID

**回應 `data`：** 單一 `RecordVO`

**`RecordVO` 欄位說明：**

```json
{
  "recordId": "bbbb-...",
  "roomId": "aaaa-...",
  "userId": "550e-...",
  "title": "週會",
  "reason": "每週例行會議",
  "commentText": null,
  "status": 1,
  "parentRecordId": null,
  "rrule": null,
  "startedTime": "2024-01-15T09:00:00+08:00",
  "endedTime": "2024-01-15T10:00:00+08:00",
  "reminderTime": "2024-01-15T08:45:00+08:00",
  "isNotified": 0,
  "createdBy": "ming@example.com",
  "createdTime": "2024-01-10T10:00:00+08:00",
  "updatedBy": "ming@example.com",
  "updatedTime": "2024-01-10T10:00:00+08:00"
}
```

| 欄位 | 說明 |
|------|------|
| `status` | `1` = 預約中，`0` = 已取消 |
| `parentRecordId` | 週期預約的系列 ID；單筆為 `null` |
| `rrule` | 若為週期預約則有值；單筆為 `null` |
| `isNotified` | `0` = 未發提醒信，`1` = 已發提醒信 |

---

### 8.6 PUT `/records/{id}/cancel` — 取消單筆預約

**說明：** 本人或管理員可取消

**Path Variable：** `id` = UUID

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Booking cancelled successfully"
}
```

---

### 8.7 PUT `/records/{id}/cancel-series` — 取消整個週期系列

**說明：** 提供系列中**任一筆** `recordId`，即可取消同一 `parentRecordId` 下所有「尚未取消」的預約

**Path Variable：** `id` = 系列中任一 UUID

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Cancelled 8 bookings in the series"
}
```

---

## 9. AI 聊天模組 `/chat`

> 需帶 `Authorization` Header。  
> 對話歷史存於 Redis，**30 分鐘無操作後自動清除**。

### 9.1 POST `/chat/send` — 傳送訊息給 AI 助理

**Request Body：**

```json
{
  "message": "幫我預約明天早上九點到十點的 A 會議室，主題是週會"
}
```

**成功回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "reply": "已幫您預約 A 會議室，2024-01-16 09:00–10:00，主題：週會。預約 ID 為 bbbb-..."
  }
}
```

**AI 可理解的指令範例：**

| 用途 | 範例訊息 |
|------|---------|
| 查詢會議室 | "有哪些會議室？" |
| 查可用時段 | "明天 A 會議室有哪些時段可用？" |
| 建立預約 | "幫我預約後天下午兩點到三點的 B 會議室，主題是客戶訪談" |
| 查我的預約 | "我有哪些預約？" |
| 取消預約 | "幫我取消預約 {recordId}" |

**前端須知：**
- `reply` 可能包含 `\n` 換行，建議用 `white-space: pre-wrap` 或 markdown renderer 顯示
- 建立對話歷史：將每次 `message` 和 `reply` 顯示為聊天泡泡

---

### 9.2 DELETE `/chat/history` — 清除對話歷史

**說明：** 手動清除當前用戶的對話記憶，下次對話重新開始

**回應：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "Conversation history cleared"
}
```

---

## 10. 錯誤處理

### 10.1 業務錯誤碼對照

| `code` | 原因 | 前端處理建議 |
|--------|------|------------|
| 200 | 成功 | 正常處理 `data` |
| 400 | 請求格式錯誤 / 參數驗證失敗 | 顯示 `msg` 內容 |
| 401 | 未登入 / Token 過期 | 導向登入頁或刷新 Token |
| 403 | 無權限（帳號停用、Email 未驗證） | 顯示 `msg` 內容 |
| 404 | 資源不存在 | 顯示「找不到資料」 |
| 409 | 時間衝突（預約重疊） | 顯示「此時段已被預約」 |
| 500 | 伺服器錯誤 | 顯示通用錯誤訊息 |

### 10.2 驗證錯誤格式

當 Request Body 的欄位驗證失敗時，回傳：

```json
{
  "code": 400,
  "msg": "password length must be between 8 and 20",
  "data": null
}
```

### 10.3 Axios 攔截器參考實作（Vue 3）

```javascript
// api/axios.js
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
})

// 請求攔截：自動帶 Token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 回應攔截：自動刷新 Token
api.interceptors.response.use(
  response => {
    const { code, msg, data } = response.data
    if (code === 401) {
      return handleTokenRefresh(response.config)
    }
    if (code !== 200) {
      return Promise.reject(new Error(msg))
    }
    return data
  },
  error => Promise.reject(error)
)

async function handleTokenRefresh(failedConfig) {
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) {
    router.push('/login')
    return
  }
  try {
    const res = await axios.post(
      `/auth/refresh?refreshToken=${refreshToken}`,
      null,
      { baseURL: 'http://localhost:8080' }
    )
    const { accessToken, refreshToken: newRefresh } = res.data.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', newRefresh)
    failedConfig.headers.Authorization = `Bearer ${accessToken}`
    return api(failedConfig)
  } catch {
    router.push('/login')
  }
}

export default api
```

---

## 11. OAuth2 流程

### 11.1 Microsoft Azure AD 登入

```
1. 前端顯示「使用 Microsoft 登入」按鈕
2. 點擊後開啟（或 redirect 至）：
   GET http://localhost:8080/oauth2/authorization/azure
3. 用戶完成 Azure AD 認證
4. 後端 redirect 至前端：
   http://localhost:5173/oauth2/callback?accessToken=xxx&refreshToken=yyy
5. 前端 /oauth2/callback 頁面擷取 URL 參數，存入 localStorage，導向首頁
```

### 11.2 Google 登入

```
1. 前端顯示「使用 Google 登入」按鈕
2. 點擊後開啟：
   GET http://localhost:8080/oauth2/authorization/google
3. 流程同 Azure AD（步驟 3-5）
```

### 11.3 前端 `/oauth2/callback` 頁面範例（Vue Router）

```javascript
// pages/OAuthCallback.vue
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

export default {
  setup() {
    const router = useRouter()
    onMounted(() => {
      const params = new URLSearchParams(window.location.search)
      const accessToken = params.get('accessToken')
      const refreshToken = params.get('refreshToken')
      if (accessToken) {
        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        router.push('/')
      } else {
        router.push('/login?error=oauth_failed')
      }
    })
  }
}
```

---

## 12. 資料型別參考

### UserVO

| 欄位 | 型別 | 說明 |
|------|------|------|
| `userId` | `string (UUID)` | 用戶 ID |
| `userName` | `string` | 顯示名稱 |
| `email` | `string` | 登入 Email |
| `company` | `string \| null` | 公司 |
| `role` | `0 \| 1` | `0`=一般用戶, `1`=管理員 |
| `status` | `0 \| 1` | `0`=停用, `1`=啟用 |
| `authProvider` | `"LOCAL" \| "AZURE_AD" \| "GOOGLE"` | 登入方式 |
| `emailVerified` | `boolean` | Email 是否已驗證 |
| `createdTime` | `string (OffsetDateTime)` | 建立時間 |
| `updatedTime` | `string (OffsetDateTime)` | 更新時間 |

### RoomVO

| 欄位 | 型別 | 說明 |
|------|------|------|
| `roomId` | `string (UUID)` | 房間 ID |
| `roomName` | `string` | 房間名稱 |
| `capacity` | `number` | 容納人數 |
| `location` | `string \| null` | 地點描述 |
| `status` | `0 \| 1` | `0`=停用, `1`=啟用 |
| `createdTime` | `string (OffsetDateTime)` | 建立時間 |
| `updatedTime` | `string (OffsetDateTime)` | 更新時間 |

### RecordVO

| 欄位 | 型別 | 說明 |
|------|------|------|
| `recordId` | `string (UUID)` | 預約 ID |
| `roomId` | `string (UUID)` | 會議室 ID |
| `userId` | `string (UUID)` | 預約人 ID |
| `title` | `string \| null` | 會議標題 |
| `reason` | `string \| null` | 使用原因 |
| `commentText` | `string \| null` | 備註 |
| `status` | `0 \| 1` | `0`=已取消, `1`=預約中 |
| `parentRecordId` | `string (UUID) \| null` | 週期系列 ID |
| `rrule` | `string \| null` | RRULE 規則（週期預約有值） |
| `startedTime` | `string (OffsetDateTime)` | 開始時間 |
| `endedTime` | `string (OffsetDateTime)` | 結束時間 |
| `reminderTime` | `string (OffsetDateTime) \| null` | 提醒時間 |
| `isNotified` | `0 \| 1` | `0`=未發提醒, `1`=已發提醒 |
| `createdBy` | `string` | 建立者 Email |
| `createdTime` | `string (OffsetDateTime)` | 建立時間 |
| `updatedBy` | `string` | 最後更新者 Email |
| `updatedTime` | `string (OffsetDateTime)` | 更新時間 |

### BatchBookingResponse

| 欄位 | 型別 | 說明 |
|------|------|------|
| `count` | `number` | 建立的預約筆數 |
| `parentRecordId` | `string (UUID) \| null` | 週期系列 ID（批次為 null） |
| `recordIds` | `string[]` | 所有建立的預約 ID 清單 |

---

*規格書由後端程式碼自動推導產生，如有異動請通知前端同步更新。*
