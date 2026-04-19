Lưu ý:

- Mặc định account-service dùng cấu hình codespace-dev khi không có env. Khi dùng cấu hình codespace-dev thì phải cập nhật api-gateway-url theo url codespace mới tạo.
- Khi thực hiện Unit Test bằng codespace cần bỏ comment và cập nhật api-gateway-url theo url codespace mới tạo.
- Các thành phần trong hệ thống được tạo bởi docker-compose ở folder infrastructure.
- Mặc định có eureka-server, api-gateway, redis, postgres-account. Ngoài ra, trong docker-compose còn có các image của kafka và account-service, learn-service, stats-service kèm các image database. Khi dev tính năng cần sử dụng các thành phần nào thì mở comment của các thành phần đó ra, ngược lại nếu không sử dụng thì nên comment lại.
