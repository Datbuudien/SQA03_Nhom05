// =========================================================================
// API 1: GET /api/diagram/{diagramId}/collaborations (Lấy danh sách)
// =========================================================================

// PM_DC_001 : Lấy danh sách collaborations thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

// Kiểm tra danh sách trả về
pm.test('Trả về mảng danh sách các collaborations', function () {
    var json = pm.response.json();
    pm.expect(json).to.be.an('array');
    if (json.length > 0) {
        pm.expect(json[0]).to.have.property('id');
        pm.expect(json[0]).to.have.property('username');
        pm.expect(json[0]).to.have.property('permission');
    }
});


// =========================================================================
// API 2: POST /api/diagram/{diagramId}/collaborations (Thêm collaborator)
// =========================================================================

// PM_DC_002 : Thêm collaborator thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

// Kiểm tra data trả về
pm.test('Trả về thông tin collaboration vừa tạo', function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('id');
    pm.expect(json).to.have.property('username');
    pm.expect(json).to.have.property('permission');
});

// Lưu thông tin vào biến môi trường để dùng cho API xóa/sửa sau này
pm.test('Lưu collaborationId vào biến môi trường', function () {
    var json = pm.response.json();
    pm.collectionVariables.set('testCollaborationId', json.id);
    console.log('Collaboration ID đã lưu:', json.id);
});

// PM_DC_003 : Thêm collaborator thất bại (Ví dụ: sai data type hoặc thiếu quyền)
pm.test('Status 400 hoặc 403 khi thêm thất bại', function () {
    pm.expect(pm.response.code).to.be.oneOf([400, 403, 422]);
});


// =========================================================================
// API 3: PATCH /api/diagram/{diagramId}/collaborations/{collaborationId} (Cập nhật quyền)
// =========================================================================

// PM_DC_004 : Cập nhật quyền thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Response trả về rỗng theo thiết kế Controller', function () {
    pm.expect(pm.response.text()).to.be.empty;
});


// =========================================================================
// API 4: GET /api/diagram/{diagramId}/collaborations/check-access?username=xxx (Kiểm tra quyền)
// =========================================================================

// PM_DC_005 : Kiểm tra quyền truy cập thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Trả về kết quả boolean (true/false)', function () {
    var json = pm.response.json();
    pm.expect(json).to.be.a('boolean');
});


// =========================================================================
// API 5: GET /api/diagram/{diagramId}/collaborations/owner (Lấy thông tin owner)
// =========================================================================

// PM_DC_006 : Lấy thông tin owner thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Trả về thông tin user có quyền OWNER', function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('id');
    pm.expect(json).to.have.property('username');
    pm.expect(json).to.have.property('permission');
    pm.expect(json.permission).to.eql('OWNER');
});


// =========================================================================
// API 6: GET /api/diagram/{diagramId}/online-users (Lấy danh sách user online)
// =========================================================================

// PM_DC_007 : Lấy danh sách online users thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Trả về danh sách (Set/Array) username đang online', function () {
    var json = pm.response.json();
    pm.expect(json).to.be.an('array'); 
});


// =========================================================================
// API 7: DELETE /api/diagram/{diagramId}/collaborations/{collaborationId} (Xóa)
// =========================================================================

// PM_DC_008 : Xóa collaborator thành công
pm.test('Status 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Response trả về rỗng sau khi xóa', function () {
    pm.expect(pm.response.text()).to.be.empty;
});

// Xóa biến môi trường sau khi test xong (Rollback lại Workspace)
pm.test('Dọn dẹp biến testCollaborationId', function () {
    pm.collectionVariables.unset('testCollaborationId');
});