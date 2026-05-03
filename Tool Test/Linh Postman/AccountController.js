// =========================================================================
// API 1: POST /account 
// (Tạo tài khoản mới)
// Yêu cầu Header: X-Username="Tên admin", Content-Type="application/json"
// Body: raw JSON chứa firstName, lastName, cccd, role, email...
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về tài khoản đã tạo (có id và username tự sinh)", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('id');
    pm.expect(json).to.have.property('username');
    pm.expect(json).to.have.property('cccd');
    
    // Lưu lại thông tin vào biến môi trường để test các API Get/Update/Delete phía dưới
    pm.collectionVariables.set("testAccountId", json.id);
    pm.collectionVariables.set("testAccountUsername", json.username);
    pm.collectionVariables.set("testAccountCccd", json.cccd);
    console.log("Đã lưu Account ID mới:", json.id);
});


// =========================================================================
// API 2: GET /account/search 
// (Tìm kiếm phân trang)
// Yêu cầu Header: X-Username="Tên admin"
// Query Params: keyword, role, page, size...
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về định dạng Page của Spring Data (chứa content array)", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('content');
    pm.expect(json).to.have.property('totalElements');
    pm.expect(json).to.have.property('totalPages');
    pm.expect(json.content).to.be.an('array');
});


// =========================================================================
// API 3: GET /account/{username} 
// (Lấy chi tiết theo Username)
// Yêu cầu Header: X-Username="Tên admin hoặc teacher"
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về đúng account cần tìm", function () {
    var json = pm.response.json();
    // Lấy biến username đã lưu từ API POST để so sánh
    var expectedUsername = pm.collectionVariables.get("testAccountUsername");
    if(expectedUsername) {
        pm.expect(json).to.have.property('username', expectedUsername);
    } else {
        pm.expect(json).to.have.property('username');
    }
});


// =========================================================================
// API 4: GET /account/me 
// (Lấy profile user đang đăng nhập)
// Yêu cầu Header: X-Username="Tên user"
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về thông tin của user hiện tại", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('username');
});


// =========================================================================
// API 5: PUT /account/{id} 
// (Cập nhật tài khoản)
// Yêu cầu Header: X-Username="Tên admin", Content-Type="application/json"
// Body: raw JSON chứa thông tin cần cập nhật
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về account đã được cập nhật thành công", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('id');
    // Có thể bổ sung test các trường cụ thể dựa trên body bạn truyền lên
});


// =========================================================================
// API 6: GET /account/bulk 
// (Lấy list account theo danh sách ID)
// Yêu cầu Header: X-Username="Tên user"
// Query Params: ids=1&ids=2&ids=3...
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về mảng danh sách account", function () {
    var json = pm.response.json();
    pm.expect(json).to.be.an('array');
});


// =========================================================================
// API 7: GET /account/student/{cccd} VÀ /account/teacher/{cccd}
// (Lấy thông tin theo số CCCD)
// Yêu cầu Header: X-Username="Tên admin hoặc teacher"
// =========================================================================

pm.test("Status code là 200 (nếu tìm thấy) hoặc 404 (nếu không)", function () {
    pm.expect(pm.response.code).to.be.oneOf([200, 404]);
});

if (pm.response.code === 200) {
    pm.test("Trả về đúng account có CCCD tương ứng", function () {
        var json = pm.response.json();
        pm.expect(json).to.have.property('cccd');
    });
}


// =========================================================================
// API 8: GET /account/email/{email} 
// (Lấy thông tin thu gọn (DTO) theo email)
// Yêu cầu Header: X-Username="Tên admin hoặc student"
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về AccountDTO với các trường thu gọn", function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('username');
    pm.expect(json).to.have.property('name');
    pm.expect(json).to.have.property('picture');
});


// =========================================================================
// API 9: DELETE /account/{id} 
// (Xóa mềm tài khoản)
// Yêu cầu Header: X-Username="Tên admin"
// =========================================================================

pm.test("Status code là 200", function () {
    pm.response.to.have.status(200);
});

pm.test("Trả về thông báo xóa thành công", function () {
    var text = pm.response.text();
    pm.expect(text).to.eql("Account Deleted Successfully!");
});

// Có thể dọn dẹp biến môi trường sau khi test xong luồng
pm.test("Dọn dẹp biến môi trường", function () {
    pm.collectionVariables.unset("testAccountId");
    pm.collectionVariables.unset("testAccountUsername");
    pm.collectionVariables.unset("testAccountCccd");
});