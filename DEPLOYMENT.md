# 🚀 Hướng Dẫn Triển Khai Hệ Thống ChatWeb (Deployment Guide)

Tài liệu hướng dẫn chi tiết quy trình triển khai ứng dụng Realtime Chat Web lên môi trường Cloud và thiết lập tự động hóa CI/CD qua GitHub Actions.

---

## 🏗️ Kiến Trúc Triển Khai (Cloud Architecture)

```
                            +--------------------------+
                            |     Cloudflare Pages     |
                            |   (React Frontend SPA)   |
                            |  https://chatweb.pages.dev
                            +------------+-------------+
                                         |
                       HTTPS REST API / WSS WebSocket
                                         |
                                         v
                            +--------------------------+
                            |       Render.com         |
                            | (Spring Boot Docker App) |
                            | /actuator/health         |
                            +----+----------------+----+
                                 |                |
                       JDBC SSL  |                | Redis SSL
                                 v                v
                     +--------------------+  +--------------------+
                     |  Neon PostgreSQL   |  |   Upstash Redis    |
                     | (Serverless DB +   |  |   (PubSub & Cache) |
                     |  Flyway Migration) |  +--------------------+
                     +--------------------+
```

| Thành phần | Nền tảng | Gói miễn phí / Khuyến nghị | Ghi chú |
|------------|----------|---------------------------|---------|
| **Frontend** | Cloudflare Pages | Free tier (Unlimited bandwidth) | Hỗ trợ CDN toàn cầu, SPA routing fallback |
| **Backend** | Render.com | Free / Starter Web Service | Docker multi-stage build, health checks |
| **Database** | Neon.tech | Free tier (PostgreSQL 16) | Serverless Postgres, tự động auto-scale & pooler |
| **Redis** | Upstash | Free tier (Serverless Redis) | Hỗ trợ URL `rediss://...` bảo mật SSL |
| **CI/CD** | GitHub Actions | 2,000 mins/month free | Tự động build, test và deploy |

---

## 📋 Bước 1: Khởi tạo Database trên Neon PostgreSQL

1. Đăng ký/đăng nhập tài khoản tại [neon.tech](https://neon.tech).
2. Nhấp **Create Project**, đặt tên `chatweb-db`, chọn region gần nhất (ví dụ: `Singapore` - `ap-southeast-1`).
3. Sau khi tạo xong, vào **Dashboard** -> copy chuỗi kết nối **Connection String**:
   - Chọn kiểu kết nối **Pooled connection** (có chứa đuôi `-pooler`):
     ```text
     postgresql://<username>:<password>@<endpoint>-pooler.ap-southeast-1.aws.neon.tech/chatweb?sslmode=require
     ```
   - Chuyển sang định dạng JDBC chuẩn cho Spring Boot:
     ```text
     jdbc:postgresql://<endpoint>-pooler.ap-southeast-1.aws.neon.tech/chatweb?sslmode=require
     ```
4. **Flyway Migration**: Khi backend khởi động với profile `prod`, Flyway sẽ tự động chạy tất cả migration scripts (`V1` -> `V5`), tự khởi tạo đầy đủ tables và indexes mà không cần chạy SQL thủ công.

---

## 📋 Bước 2: Khởi tạo Redis trên Upstash

1. Đăng ký/đăng nhập tài khoản tại [upstash.com](https://upstash.com).
2. Nhấp **Create Database**, đặt tên `chatweb-redis`, chọn region gần với database (ví dụ: `ap-southeast-1`).
3. Sau khi khởi tạo, cuộn xuống mục **REST API / Node / Java** hoặc tìm **`UPSTASH_REDIS_REST_URL` / `redis://...`**:
   - Copy đường dẫn kết nối chuẩn **`rediss://...`** (bao gồm mật khẩu và SSL):
     ```text
     rediss://default:<password>@<endpoint>.upstash.io:6379
     ```

---

## 📋 Bước 3: Triển khai Backend lên Render.com

### Cách 1: Sử dụng Render Blueprint (`render.yaml`) - Khuyến nghị
1. Đăng nhập [render.com](https://render.com).
2. Chọn **Blueprints** -> **New Blueprint Instance**.
3. Kết nối với GitHub repository `ChatWeb`.
4. Render sẽ tự động phát hiện file `render.yaml` và nạp cấu hình web service:
   - Name: `chatweb-backend`
   - Runtime: `Docker`
   - Health Check Path: `/actuator/health`
5. Điền giá trị cho các biến môi trường được đánh dấu chưa có:
   - `DATABASE_URL`: JDBC URL từ Bước 1
   - `REDIS_URL`: Redis URL từ Bước 2
   - `JWT_SECRET`: Chuỗi bảo mật ngẫu nhiên ít nhất 256-bit (32+ ký tự)
   - `CORS_ALLOWED_ORIGINS`: Địa chỉ domain của Frontend (ví dụ: `https://chatweb.pages.dev`)
6. Nhấp **Apply** để bắt đầu build và deploy.

### Cách 2: Tạo Web Service thủ công
1. Trên Render Dashboard, nhấp **New +** -> **Web Service**.
2. Kết nối GitHub repository của bạn.
3. Cấu hình các thông số:
   - **Name:** `chatweb-backend`
   - **Language:** `Docker`
   - **Dockerfile Path:** `./backend/Dockerfile`
   - **Docker Context:** `./backend`
   - **Region:** `Singapore`
   - **Health Check Path:** `/actuator/health`
4. Khai báo **Environment Variables**:
   | Key | Value | Mô tả |
   |-----|-------|-------|
   | `SPRING_PROFILES_ACTIVE` | `prod` | Kích hoạt cấu hình production |
   | `DATABASE_URL` | `jdbc:postgresql://...` | Connection string PostgreSQL (Neon) |
   | `REDIS_URL` | `rediss://...` | Connection string Redis (Upstash) |
   | `JWT_SECRET` | `<chuỗi_bí_mật_256bit>` | Key mã hóa HS256 JWT tokens |
   | `CORS_ALLOWED_ORIGINS` | `https://chatweb.pages.dev,http://localhost:5173` | Danh sách domain được phép gọi API |
5. Nhấp **Create Web Service**. Sau khi deploy xong, bạn sẽ có URL dạng `https://chatweb-backend.onrender.com`.

---

## 📋 Bước 4: Triển khai Frontend lên Cloudflare Pages

1. Đăng nhập [dash.cloudflare.com](https://dash.cloudflare.com).
2. Vào **Workers & Pages** -> **Create application** -> Chọn tab **Pages** -> **Connect to Git**.
3. Chọn repository `ChatWeb`.
4. Cấu hình build settings:
   - **Framework preset:** `Vite`
   - **Root directory:** `frontend`
   - **Build command:** `npm run build`
   - **Build output directory:** `dist`
5. Khai báo **Environment Variables**:
   | Variable | Value ví dụ | Mô tả |
   |----------|-------------|-------|
   | `VITE_API_URL` | `https://chatweb-backend.onrender.com/api` | Base URL của Backend REST API |
   | `VITE_WS_URL` | `https://chatweb-backend.onrender.com/ws` | Endpoint WebSocket (SockJS sẽ dùng WSS) |
6. Nhấp **Save and Deploy**.
7. Cloudflare Pages sẽ tự động nhận diện file `frontend/public/_redirects` để điều hướng toàn bộ route về `index.html` (SPA fallback), giải quyết triệt để lỗi 404 khi người dùng refresh trình duyệt.

---

## 📋 Bước 5: Cấu hình Tự Động Hóa CI/CD (GitHub Actions)

Kho lưu trữ đã được cấu hình sẵn 3 workflows trong `.github/workflows/`:
- `backend.yml`: Chạy tests, đóng gói JAR và tự động deploy sang Render khi có commit mới ở nhánh `main` tác động tới `backend/`.
- `frontend.yml`: Typecheck, build và deploy sang Cloudflare Pages khi có commit mới ở nhánh `main` tác động tới `frontend/`.
- `pr.yml`: Tự động kiểm tra chất lượng code trên cả backend và frontend cho mỗi Pull Request.

### Thiết lập GitHub Secrets:
Truy cập GitHub repository của bạn: **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**:

1. **RENDER_DEPLOY_HOOK_URL**:
   - Lấy tại: Render Dashboard -> Chọn `chatweb-backend` -> **Settings** -> Cuộn đến **Deploy Hook** -> Copy URL.
2. **CLOUDFLARE_API_TOKEN**:
   - Lấy tại: Cloudflare Dashboard -> **My Profile** -> **API Tokens** -> **Create Token** -> Sử dụng template **Edit Cloudflare Workers/Pages**.
3. **CLOUDFLARE_ACCOUNT_ID**:
   - Lấy tại: Cloudflare Dashboard -> Trang chủ của Account -> Copy **Account ID** ở thanh bên phải.
4. **VITE_API_URL**: `https://chatweb-backend.onrender.com/api`
5. **VITE_WS_URL**: `https://chatweb-backend.onrender.com/ws`

---

## 🔍 Kiểm Tra & Xác Minh Sau Khi Triển Khai

1. **Kiểm tra Health Check Backend:**
   ```bash
   curl -I https://chatweb-backend.onrender.com/actuator/health
   # Kết quả mong muốn: HTTP/2 200 OK, {"status":"UP"}
   ```
2. **Kiểm tra Truy Cập Giao Diện:**
   - Truy cập `https://chatweb.pages.dev` trên trình duyệt.
   - Thử đăng ký tài khoản mới và đăng nhập.
   - Thử tải lại trang (F5) ở các màn hình `/chat` để xác nhận SPA routing hoạt động.
3. **Kiểm tra Kết Nối Realtime (WebSocket):**
   - Mở 2 cửa sổ trình duyệt (hoặc 1 cửa sổ ẩn danh).
   - Đăng nhập 2 tài khoản khác nhau.
   - Tìm kiếm người dùng, gửi tin nhắn và xác nhận tin nhắn xuất hiện tức thời qua WSS.
