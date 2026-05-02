// =========================================================================
// API 1: POST /api/diagrams/create (Tạo blank diagram - KHÔNG đính kèm file)
// Yêu cầu Body (form-data): name="My Diagram"
// Yêu cầu Header: X-Username="Tên user"
// =========================================================================

// PM_DI_001 : Tạo blank diagram thành công
pm.test('Status code là 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Trả về success = true và có diagramId', function () {
    var json = pm.response.json();
    pm.expect(json).to.have.property('success', true);
    pm.expect(json).to.have.property('diagramId');
    pm.expect(json.diagramId).to.be.a('number');
    pm.expect(json).to.have.property('message', 'Diagram created successfully');
});

// Lưu diagramId vào biến môi trường nếu cần dùng cho các API sau
pm.test('Lưu diagramId vào biến môi trường', function () {
    var json = pm.response.json();
    if(json.diagramId) {
        pm.collectionVariables.set('testImportedDiagramId', json.diagramId);
        console.log('Đã tạo Diagram ID:', json.diagramId);
    }
});


// =========================================================================
// API 2: POST /api/diagrams/create (Import từ file JSON hợp lệ)
// Yêu cầu Body (form-data): name="My Diagram", jsonFile=[Chọn file .json]
// =========================================================================

// PM_DI_002 : Import diagram thành công
pm.test('Status code là 200', function () {
    pm.response.to.have.status(200);
});

pm.test('Import diagram thành công (success = true)', function () {
    var json = pm.response.json();
    pm.expect(json.success).to.be.true;
    pm.expect(json.diagramId).to.be.a('number');
    pm.expect(json.message).to.eql('Diagram created successfully');
});


// =========================================================================
// API 3: POST /api/diagrams/create (Import file sai định dạng)
// Yêu cầu Body (form-data): name="My Diagram", jsonFile=[Chọn file .txt hoặc .png]
// =========================================================================

// PM_DI_003 : Chặn file không phải .json
pm.test('Status code là 400 Bad Request', function () {
    pm.response.to.have.status(400);
});

pm.test('Trả về báo lỗi sai định dạng file', function () {
    var json = pm.response.json();
    pm.expect(json.success).to.be.false;
    pm.expect(json.diagramId).to.be.null;
    pm.expect(json.message).to.eql('File must be a JSON file');
});


// =========================================================================
// API 4: POST /api/diagrams/create (Thiếu tham số bắt buộc)
// Yêu cầu Body (form-data): Bỏ trống không điền biến "name"
// =========================================================================

// PM_DI_004 : Bắt lỗi thiếu @RequestParam("name")
pm.test('Status code là 400 Bad Request do thiếu name', function () {
    // Spring Boot mặc định ném lỗi 400 khi thiếu tham số bắt buộc trong @RequestParam
    pm.response.to.have.status(400);
});