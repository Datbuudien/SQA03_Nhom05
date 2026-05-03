// =========================================================================
// TEST CASE 1: POST /account/logout (Đăng xuất thành công)
// Yêu cầu: Bật Header X-Username = "Tên user"
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về thông báo đăng xuất và xóa cookie thành công", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('message', 'Logout successful. Cookie cleared.');
});

// Kiểm tra xem Server có gửi lệnh xóa Cookie 'jwt' về cho Client không
pm.test("Server đã gửi lệnh xóa Cookie 'jwt'", function () {
    // Khi set maxAge(0) và value(null), trình duyệt/Postman sẽ nhận được cookie rỗng
    var jwtCookie = pm.cookies.get('jwt');
    if (jwtCookie !== undefined) {
        pm.expect(jwtCookie).to.be.oneOf(['', null]);
    }
});


// =========================================================================
// TEST CASE 2: POST /account/logout (Thất bại do quên truyền Header)
// Cách test: Vào tab Headers của Postman, TẮT (uncheck) biến X-Username đi
// =========================================================================

pm.test("Status code là 400 Bad Request", function () {
    pm.response.to.have.status(400);
});

pm.test("Trả về báo lỗi thiếu Header X-Username", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('error', 'Missing X-Username header');
});