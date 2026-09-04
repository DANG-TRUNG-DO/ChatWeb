# 📋 Realtime Chat Web - Implementation Plan

> Kế hoạch triển khai ứng dụng Web Chat Realtime theo kiến trúc **Modular Monolith**.
> Dựa trên [requirement.txt](./requirement.txt).

---

## Tổng quan các Phase

| Phase | Tên | Mục tiêu | Ước lượng |
|-------|-----|-----------|-----------|
| 1 | Project Setup & Infrastructure | Khởi tạo dự án, cấu hình Docker, CI cơ bản | 2-3 ngày |
| 2 | Authentication Module | Register, Login, JWT, Security | 3-4 ngày |
| 3 | User Module | Profile, Search, Avatar | 2-3 ngày |
| 4 | Conversation & Messaging (Direct Chat) | Direct chat, gửi/nhận message, lịch sử | 4-5 ngày |
| 5 | Realtime & WebSocket | STOMP WebSocket, realtime messaging | 3-4 ngày |
| 6 | Frontend - MVP | React UI cho toàn bộ MVP features | 5-7 ngày |
| 7 | Deployment & CI/CD | Deploy lên cloud, pipeline CI/CD | 2-3 ngày |
| **Bonus** | Post-MVP Features | Group Chat, Typing, Online/Offline, File Upload, Redis, Scaling | Mở rộng |

**Tổng ước lượng MVP: ~21-29 ngày**

---

## Phase 1: Project Setup & Infrastructure

> **Mục tiêu:** Thiết lập nền tảng dự án, cấu trúc module, Docker, database migration.

### 1.1 Backend - Spring Boot Project

- [x] Khởi tạo Spring Boot project (Java 21, Maven/Gradle)
- [x] Cấu hình `application.yml` với các profile: `dev`, `prod`
- [x] Thiết lập cấu trúc **Modular Monolith** với các package:
  ```
  com.chatweb
  ├── auth/          # Authentication module
  ├── user/          # User module
  ├── conversation/  # Conversation module
  ├── message/       # Message module
  ├── realtime/      # WebSocket/Realtime module
  └── common/        # Shared utilities, exceptions, config
  ```
- [x] Mỗi module có cấu trúc internal:
  ```
  module/
  ├── controller/
  ├── service/
  ├── repository/
  ├── entity/
  ├── dto/
  └── exception/
  ```

### 1.2 Database & Migration

- [x] Cấu hình PostgreSQL connection
- [x] Tích hợp **Flyway** migration
- [x] Tạo migration script ban đầu:
  - `V1__create_users_table.sql`
  - `V2__create_refresh_tokens_table.sql`
  - `V3__create_conversations_table.sql`
  - `V4__create_conversation_members_table.sql`
  - `V5__create_messages_table.sql`
- [x] Thêm index cho `messages` (conversation_id, created_at) phục vụ cursor pagination

### 1.3 Docker & Docker Compose

- [x] Tạo `Dockerfile` cho backend (multi-stage build)
- [x] Tạo `docker-compose.yml` với:
  - PostgreSQL container
  - Redis container (chuẩn bị sẵn cho Phase sau)
  - Backend container
- [x] Tạo `.env.example` cho environment variables

### 1.4 Frontend - React Project

- [x] Khởi tạo React + TypeScript + Vite
- [x] Cài đặt dependencies:
  - Tailwind CSS + shadcn/ui
  - React Router
  - TanStack Query
  - Zustand
  - STOMP.js + SockJS
- [x] Thiết lập cấu trúc thư mục frontend:
  ```
  src/
  ├── components/     # UI components
  ├── pages/          # Route pages
  ├── hooks/          # Custom hooks
  ├── stores/         # Zustand stores
  ├── services/       # API service layer
  ├── types/          # TypeScript types
  └── lib/            # Utilities
  ```

### 1.5 Swagger / OpenAPI

- [x] Tích hợp SpringDoc OpenAPI
- [x] Cấu hình Swagger UI tại `/swagger-ui.html`
- [x] Thêm JWT Bearer authentication vào Swagger

### ✅ Deliverables Phase 1:
- Project chạy được local với Docker Compose
- Database migration chạy thành công
- Swagger UI accessible
- Frontend dev server chạy được

---

## Phase 2: Authentication Module

> **Mục tiêu:** Hoàn thiện hệ thống đăng ký, đăng nhập, JWT authentication.

### 2.1 Database Entities

- [x] `User` entity (id, email, username, password_hash, display_name, avatar_url, created_at, updated_at)
- [x] `RefreshToken` entity (id, user_id, token, expires_at, revoked)

### 2.2 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/auth/register` | Đăng ký tài khoản mới |
| POST | `/api/auth/login` | Đăng nhập, trả về access + refresh token |
| POST | `/api/auth/refresh` | Refresh access token |
| POST | `/api/auth/logout` | Revoke refresh token |

### 2.3 Security Implementation

- [x] **Password hashing** với BCrypt
- [x] **JWT Access Token** (short-lived, ~15 phút)
- [x] **JWT Refresh Token** (long-lived, ~7 ngày, lưu DB)
- [x] Tạo `JwtTokenProvider` utility class
- [x] Tạo `JwtAuthenticationFilter` (OncePerRequestFilter)
- [x] Cấu hình `SecurityFilterChain`:
  - Permit: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`
  - Authenticate: tất cả endpoint còn lại
- [x] **CORS** configuration
- [x] **Input validation** với Bean Validation (`@Valid`, `@NotBlank`, `@Email`, etc.)

### 2.4 Error Handling

- [x] Global exception handler (`@ControllerAdvice`)
- [x] Custom exceptions: `EmailAlreadyExistsException`, `InvalidCredentialsException`, `TokenExpiredException`
- [x] Chuẩn hóa error response format:
  ```json
  {
    "status": 400,
    "error": "BAD_REQUEST",
    "message": "Email already exists",
    "timestamp": "2024-01-01T00:00:00"
  }
  ```

### 2.5 Testing

- [x] Unit tests cho `AuthService` (Mockito)
- [x] Unit tests cho `JwtTokenProvider`
- [x] Integration tests với Testcontainers (PostgreSQL)
- [x] Test register, login, refresh, logout flows

### ✅ Deliverables Phase 2:
- User có thể register/login thành công
- JWT authentication hoạt động cho REST API
- Swagger hiển thị đầy đủ Auth endpoints
- Test coverage cho Auth module

---

## Phase 3: User Module

> **Mục tiêu:** Quản lý profile, tìm kiếm user, avatar.

### 3.1 API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| GET | `/api/users/me` | Lấy profile user hiện tại |
| PUT | `/api/users/me` | Cập nhật profile |
| GET | `/api/users/search?q=keyword` | Tìm kiếm user theo username/display_name |
| GET | `/api/users/{id}` | Lấy thông tin user theo ID |

### 3.2 Features

- [x] **View Profile**: trả về thông tin user (không bao gồm password)
- [x] **Update Profile**: cập nhật display_name, avatar_url
- [x] **Search User**: tìm kiếm theo username hoặc display_name (ILIKE query)
- [x] **Avatar**: lưu avatar dạng URL (MVP dùng URL external, sau này có thể upload)

### 3.3 DTOs & Mapping

- [x] `UserProfileResponse` DTO
- [x] `UpdateProfileRequest` DTO
- [x] `UserSearchResponse` DTO (phân biệt thông tin public vs private)
- [x] Sử dụng MapStruct hoặc manual mapping

### 3.4 Testing

- [x] Unit tests cho `UserService`
- [x] Integration tests cho search functionality

### ✅ Deliverables Phase 3:
- User xem/sửa được profile
- Search user hoạt động
- API documentation cập nhật

---

## Phase 4: Conversation & Messaging (Direct Chat)

> **Mục tiêu:** Tạo conversation, gửi/nhận message, lịch sử tin nhắn, read/unread.

### 4.1 Database Design

- [x] `Conversation` entity:
  - `id`, `type` (DIRECT/GROUP), `name` (nullable, dùng cho group), `created_at`, `updated_at`
- [x] `ConversationMember` entity:
  - `id`, `conversation_id`, `user_id`, `role` (OWNER/ADMIN/MEMBER), `last_read_message_id`, `joined_at`
- [x] `Message` entity:
  - `id`, `conversation_id`, `sender_id`, `content`, `type` (TEXT/IMAGE/FILE), `reply_to_id` (nullable), `edited`, `deleted`, `created_at`, `updated_at`

### 4.2 Conversation API

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/conversations` | Tạo conversation (direct chat) |
| GET | `/api/conversations` | Danh sách conversation của user |
| GET | `/api/conversations/{id}` | Chi tiết conversation |
| DELETE | `/api/conversations/{id}` | Xóa conversation |

### 4.3 Message API

| Method | Endpoint | Mô tả |
|--------|----------|--------|
| POST | `/api/conversations/{id}/messages` | Gửi message |
| GET | `/api/conversations/{id}/messages?cursor=xxx&limit=20` | Lịch sử message (cursor pagination) |
| PUT | `/api/messages/{id}` | Edit message |
| DELETE | `/api/messages/{id}` | Soft delete message |
| POST | `/api/conversations/{id}/read` | Đánh dấu đã đọc |

### 4.4 Business Logic

- [x] **Tạo Direct Conversation**: Kiểm tra đã tồn tại conversation giữa 2 user chưa
- [x] **Cursor-based Pagination**: Sử dụng `message_id` hoặc `created_at` làm cursor
  ```sql
  SELECT * FROM messages
  WHERE conversation_id = ? AND id < ?
  ORDER BY id DESC
  LIMIT 20
  ```
- [x] **Read/Unread**: Cập nhật `last_read_message_id` trong `conversation_members`
- [x] **Unread Count**: Đếm messages có id > last_read_message_id
- [x] **Edit Message**: Chỉ sender mới được edit, đánh dấu `edited = true`
- [x] **Delete Message**: Soft delete, đánh dấu `deleted = true`
- [x] **Reply Message**: Lưu `reply_to_id`, trả về referenced message

### 4.5 Authorization

- [x] Chỉ member của conversation mới xem/gửi message
- [x] Chỉ sender mới edit/delete message của mình
- [x] Chỉ OWNER mới xóa conversation

### 4.6 Testing

- [x] Unit tests cho `ConversationService`, `MessageService`
- [x] Integration tests cho pagination, read/unread
- [x] Test authorization rules

### ✅ Deliverables Phase 4:
- Direct chat hoạt động qua REST API
- Message CRUD đầy đủ
- Cursor pagination cho message history
- Read/Unread tracking

---

## Phase 5: Realtime & WebSocket

> **Mục tiêu:** Tích hợp WebSocket STOMP, realtime messaging, events.

### 5.1 WebSocket Configuration

- [x] Cấu hình `WebSocketMessageBrokerConfigurer`:
  - Endpoint: `/ws` (với SockJS fallback)
  - Application destination prefix: `/app`
  - Broker prefix: `/topic`, `/queue`
- [x] **JWT Authentication cho WebSocket**:
  - Interceptor xác thực token khi CONNECT
  - Gắn user principal vào WebSocket session

### 5.2 Message Flow

```
Client A                    Server                     Client B
   |                          |                          |
   |--- SEND /app/chat ------>|                          |
   |                          |-- Save to PostgreSQL     |
   |                          |-- Broadcast              |
   |                          |--- /topic/conv/{id} ---->|
   |<--- /topic/conv/{id} ----|                          |
```

### 5.3 WebSocket Controller

- [x] `ChatController` (`@MessageMapping`):
  ```java
  @MessageMapping("/chat.send")        // Gửi message
  @MessageMapping("/chat.typing")      // Typing indicator (Post-MVP)
  @MessageMapping("/chat.read")        // Đánh dấu đã đọc
  ```

### 5.4 Realtime Events

- [x] Định nghĩa event types:
  ```java
  enum WebSocketEventType {
    MESSAGE_SENT,
    MESSAGE_UPDATED,
    MESSAGE_DELETED,
    MESSAGE_READ,
    TYPING,           // Post-MVP
    USER_ONLINE,      // Post-MVP
    USER_OFFLINE       // Post-MVP
  }
  ```
- [x] `WebSocketEvent` wrapper DTO:
  ```json
  {
    "type": "MESSAGE_SENT",
    "payload": { ... },
    "timestamp": "2024-01-01T00:00:00"
  }
  ```

### 5.5 Integration với Message Module

- [x] Khi gửi message qua WebSocket → lưu DB trước → broadcast sau
- [x] Khi edit/delete message → cập nhật DB → broadcast event
- [x] Subscribe theo conversation: `/topic/conversation/{conversationId}`

### 5.6 Testing

- [x] Unit tests cho WebSocket controller
- [x] Integration test: kết nối WebSocket, gửi/nhận message
- [x] Test JWT authentication cho WebSocket

### ✅ Deliverables Phase 5:
- Client kết nối WebSocket thành công
- Gửi/nhận message realtime
- Events broadcast đúng conversation
- Message luôn persist trước khi broadcast

---

## Phase 6: Frontend - MVP

> **Mục tiêu:** Xây dựng giao diện React hoàn chỉnh cho MVP.

### 6.1 Authentication Pages

- [x] **Login Page**: Form email + password, validation, error handling
- [x] **Register Page**: Form đăng ký, validation
- [x] **Auth Guard**: Redirect về login nếu chưa đăng nhập
- [x] **Token Management**:
  - Lưu access token trong memory (Zustand store)
  - Lưu refresh token trong httpOnly cookie hoặc localStorage
  - Auto refresh token khi hết hạn (Axios interceptor)

### 6.2 Layout & Navigation

- [x] **Main Layout**:
  ```
  ┌──────────────────────────────────┐
  │  Sidebar    │   Chat Area        │
  │             │                    │
  │ Search      │  Header (user info)│
  │ ──────────  │  ──────────────── │
  │ Conv List   │  Message List      │
  │             │                    │
  │             │                    │
  │             │  ──────────────── │
  │             │  Message Input     │
  └──────────────────────────────────┘
  ```
- [x] Responsive design (mobile: sidebar ẩn, hiện khi toggle)

### 6.3 Conversation List (Sidebar)

- [x] Hiển thị danh sách conversations
- [x] Mỗi item hiển thị: avatar, tên, last message, thời gian, unread count
- [x] Search user → tạo conversation mới
- [x] Sort theo thời gian tin nhắn gần nhất

### 6.4 Chat Area

- [x] **Message List**:
  - Hiển thị tin nhắn theo thời gian
  - Phân biệt tin nhắn gửi (phải) vs nhận (trái)
  - Avatar + tên sender
  - Timestamp
  - Reply indicator
  - Edited badge
  - Infinite scroll (load thêm messages khi scroll lên)
- [x] **Message Input**:
  - Text input với Enter để gửi
  - Reply mode (hiển thị message đang reply)
- [x] **Message Actions** (right-click hoặc hover menu):
  - Reply
  - Edit (chỉ message của mình)
  - Delete (chỉ message của mình)

### 6.5 WebSocket Integration

- [x] Tạo `WebSocketService`:
  - Connect/disconnect management
  - Auto reconnect
  - Subscribe/unsubscribe conversations
- [x] Tạo `useWebSocket` hook
- [x] Real-time updates:
  - Tin nhắn mới xuất hiện ngay
  - Edit/delete cập nhật ngay
  - Unread count cập nhật

### 6.6 State Management (Zustand)

- [x] `useAuthStore`: user, tokens, login/logout actions
- [x] `useConversationStore`: conversations list, active conversation
- [x] `useMessageStore`: messages by conversation
- [x] `useWebSocketStore`: connection state

### 6.7 API Service Layer

- [x] Tạo Axios instance với:
  - Base URL configuration
  - JWT interceptor (attach token)
  - Refresh token interceptor (auto refresh)
  - Error handling interceptor
- [x] TanStack Query hooks cho mỗi API endpoint

### 6.8 User Profile

- [x] **Profile Page/Modal**: xem và chỉnh sửa profile
- [x] Hiển thị avatar, display name

### ✅ Deliverables Phase 6:
- Giao diện chat hoàn chỉnh
- Đăng ký/đăng nhập hoạt động
- Chat realtime qua WebSocket
- Message history với infinite scroll
- Read/Unread hiển thị đúng

---

## Phase 7: Deployment & CI/CD

> **Mục tiêu:** Deploy ứng dụng lên cloud, thiết lập CI/CD pipeline.

### 7.1 Backend Deployment (Render)

- [x] Cấu hình `application-prod.yml`
- [x] Environment variables trên Render:
  - `DATABASE_URL` (Neon PostgreSQL)
  - `REDIS_URL` (Upstash) — chuẩn bị sẵn
  - `JWT_SECRET`
  - `CORS_ALLOWED_ORIGINS`
- [x] Health check endpoint: `/actuator/health`
- [x] Cấu hình **HTTPS** và **WSS**

### 7.2 Database (Neon PostgreSQL)

- [x] Tạo database trên Neon
- [x] Cấu hình connection pooling
- [x] Flyway migration chạy tự động khi deploy
- [x] Backup strategy

### 7.3 Redis (Upstash) — Chuẩn bị

- [x] Tạo Redis instance trên Upstash
- [x] Cấu hình connection (sử dụng ở Post-MVP phase)

### 7.4 Frontend Deployment (Cloudflare Pages)

- [x] Cấu hình build command: `npm run build`
- [x] Environment variable: `VITE_API_URL`, `VITE_WS_URL`
- [x] Cấu hình SPA fallback (redirect tất cả routes về `index.html`)
- [x] Custom domain (nếu có)

### 7.5 CI/CD Pipeline (GitHub Actions)

- [x] **Backend workflow** (`.github/workflows/backend.yml`):
  ```yaml
  on: push (main branch)
  jobs:
    - Checkout
    - Setup Java 21 / 17
    - Run tests
    - Build JAR
    - Deploy to Render (via webhook hoặc API)
  ```
- [x] **Frontend workflow** (`.github/workflows/frontend.yml`):
  ```yaml
  on: push (main branch)
  jobs:
    - Checkout
    - Setup Node.js
    - Install dependencies
    - Run lint + tests
    - Build
    - Deploy to Cloudflare Pages
  ```
- [x] **PR workflow**: chạy tests trên mọi Pull Request

### ✅ Deliverables Phase 7:
- Ứng dụng live trên internet
- CI/CD tự động khi push code
- HTTPS/WSS hoạt động
- Database migration tự động

---

## Bonus Phase: Post-MVP Features

> **Mục tiêu:** Mở rộng tính năng sau khi MVP hoàn thiện.

### B.1 Group Chat
- [ ] Tạo group conversation (name, avatar)
- [ ] Thêm/xóa member
- [ ] Phân quyền: OWNER, ADMIN, MEMBER
- [ ] UI: group info panel, member list, manage members

### B.2 Typing Indicator
- [ ] Client gửi event `TYPING` qua WebSocket
- [ ] Server broadcast cho các member khác
- [ ] Frontend hiển thị "User đang nhập..."
- [ ] Debounce typing event + auto hide sau 3s

### B.3 Online/Offline Status
- [ ] Redis lưu user presence
- [ ] WebSocket connect/disconnect → set online/offline
- [ ] Broadcast `USER_ONLINE`/`USER_OFFLINE` events
- [ ] Frontend hiển thị green dot

### B.4 File Upload
- [ ] Message type: IMAGE, FILE
- [ ] Upload endpoint + Storage (Cloudflare R2 / S3)
- [ ] File size limit, allowed types validation
- [ ] Image preview + file download

### B.5 Redis Integration
- [ ] User Presence, Cache, Rate Limiting
- [ ] WebSocket Distributed State

### B.6 Horizontal Scaling
- [ ] Redis Pub/Sub cho WebSocket distribution
- [ ] Load balancer + connection pooling
