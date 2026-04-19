# Hướng Dẫn Chạy Dự Án Bằng Docker

> **LƯU Ý QUAN TRỌNG VỀ LỆNH MAVEN (mvn):**
> Trước đây, project yêu cầu bạn phải tự chạy lệnh `mvn clean package` hoặc `mvnw package` trên máy tính để tạo ra các file `.jar` trong thư mục `target/` trước khi thao tác lệnh Docker.
> 
> Tuy nhiên, **tôi đã cấu hình nâng cấp toàn bộ các Dockerfile Java lên Multi-Stage Build**. Nghĩa là bây giờ, tiến trình Docker sẽ **tự động tải Maven ảo và tự động chạy `mvn package` ngay bên trong môi trường Docker** khi bạn gọi lệnh `docker compose build`. 
> 
> Bạn **KHÔNG CẦN CHẠY LỆNH `mvn`** bằng tay nữa, cũng không cần cài sẵn Maven/Java trên máy cá nhân nữa! 🚀

---

## 1. Yêu Cầu Cài Đặt
- Máy tính chỉ cần cài **Docker Desktop** (bật sẵn).

## 2. Thứ Tự Khởi Động (Trình Tự Bắt Buộc)

Mở Terminal (hoặc PowerShell) tại thư mục gốc của dự án (`f:\SQA\doan`).

### Bước 2.1: Chạy Nhóm Cốt Lõi (Tự động chạy cả Maven bên trong)
Lệnh này sẽ tự động chạy Maven package ngầm và sau đó bật API Gateway, Account Service, React-Flow BE, FE, DB...
```bash
docker compose -f infrastructure/docker-compose.yml up -d --build
```
> *(Chờ Docker làm hết việc từ A-Z. Cà phê một lúc vì lần build đầu tiên tự động kéo thư viện khá lâu).*

### Bước 2.2: Chạy Nhóm Chatbot AI
Lệnh này bật Chatbot lên để kết hợp cùng với Gateway:
```bash
docker compose -f chat-service/docker-compose.yml up -d --build
```

---

## 3. Các Đường Dẫn Hệ Thống
* 🌎 **Giao diện Web (Frontend):** [http://localhost:5173](http://localhost:5173)
* 🛡️ **API Gateway (Cổng chính):** [http://localhost:8080](http://localhost:8080)
* 🤖 **Dịch vụ Chatbot (trực tiếp):** [http://localhost:8000](http://localhost:8000)

## 4. Cập Nhật Ngrok Cho Chatbot (Không cần restart server)
Do AI Model đang chạy Kaggle và nhận link Ngrok thay đổi liên tục. Bạn chỉ cần chạy lệnh sau để thay Ngrok mới vào chatbot đang chạy (thay URL tương ứng):
```bash
curl -X POST http://localhost:8000/set-kaggle-url -H "Content-Type: application/json" -d "{\"url\":\"https://<dia-chi-ngrok-moi>.ngrok-free.app\"}"
```

## 5. Dọn Dẹp Khi Dùng Xong
Bật 2 lệnh sau để tắt triệt để server Docker, nhả lại RAM:
```bash
docker compose -f infrastructure/docker-compose.yml down
docker compose -f chat-service/docker-compose.yml down
```
