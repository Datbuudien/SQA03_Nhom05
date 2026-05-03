/**
 * Postman test scripts - copy từng block vào tab Tests của request tương ứng.
 * Format giữ nguyên kiểu:
 * // [TC_xxx] Mục tiêu...
 * pm.test(...)
 */

// =========================================================
// TC_COL_001 - GET {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_001] Mục tiêu: Đảm bảo API lấy danh sách collaborators hoạt động
pm.test("[TC_COL_001] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

// [TC_COL_001] Danh sách collaborators phải là mảng JSON
pm.test("[TC_COL_001] Response là array", () => {
  const data = pm.response.json();
  pm.expect(Array.isArray(data)).to.be.true;
});

// =========================================================
// TC_COL_002 - GET {{baseUrl}}/account/email/{{targetEmail}}
// =========================================================
// [TC_COL_002] Mục tiêu: Tìm account bằng email để lấy username phục vụ add collaborator
pm.test("[TC_COL_002] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const data = pm.response.json();

// [TC_COL_002] API phải trả về username hợp lệ
pm.test("[TC_COL_002] Có trường username", () => {
  pm.expect(data.username).to.exist;
});

// [TC_COL_002] Lưu username vào environment để dùng cho TC add collaborator
pm.environment.set("targetUsername", data.username);

// =========================================================
// TC_COL_003 - POST {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_003] Mục tiêu: Thêm collaborator mới với quyền VIEW
pm.test("[TC_COL_003] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const c = pm.response.json();

// [TC_COL_003] Kiểm tra username add vào đúng user đã search
pm.test("[TC_COL_003] Username đúng", () => {
  pm.expect(c.username).to.eql(pm.environment.get("targetUsername"));
});

// [TC_COL_003] Kiểm tra quyền ban đầu là VIEW
pm.test("[TC_COL_003] Permission = VIEW", () => {
  pm.expect(c.permission).to.eql("VIEW");
});

// [TC_COL_003] Lưu collaborationId để update/remove ở các test sau
pm.environment.set("collaborationId", c.id);

// =========================================================
// TC_COL_004 - GET {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_004] Mục tiêu: Xác nhận collaborator vừa add xuất hiện trong danh sách
pm.test("[TC_COL_004] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const list = pm.response.json();
const targetUsername = pm.environment.get("targetUsername");
const found = list.find(x => x.username === targetUsername);

// [TC_COL_004] Phải tìm thấy collaborator mới
pm.test("[TC_COL_004] Có collaborator mới trong list", () => {
  pm.expect(found).to.exist;
});

// [TC_COL_004] Nếu có id thì cập nhật lại collaborationId
if (found && found.id) {
  pm.environment.set("collaborationId", found.id);
}

// =========================================================
// TC_COL_005 - PATCH {{baseUrl}}/api/diagram/{{diagramId}}/collaborations/{{collaborationId}}
// =========================================================
// [TC_COL_005] Mục tiêu: Cập nhật quyền collaborator sang FULL_ACCESS
pm.test("[TC_COL_005] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

// =========================================================
// TC_COL_006 - GET {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_006] Mục tiêu: Xác nhận quyền collaborator sau update là FULL_ACCESS
pm.test("[TC_COL_006] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const list2 = pm.response.json();
const collabId = String(pm.environment.get("collaborationId"));
const found2 = list2.find(x => String(x.id) === collabId);

// [TC_COL_006] Bản ghi collaborator phải tồn tại
pm.test("[TC_COL_006] Tìm thấy collaborator theo id", () => {
  pm.expect(found2).to.exist;
});

// [TC_COL_006] Permission phải là FULL_ACCESS
pm.test("[TC_COL_006] Permission = FULL_ACCESS", () => {
  pm.expect(found2.permission).to.eql("FULL_ACCESS");
});

// =========================================================
// TC_COL_007 - PATCH {{baseUrl}}/api/diagram/{{diagramId}}/collaborations/{{collaborationId}}
// =========================================================
// [TC_COL_007] Mục tiêu: Hạ quyền từ FULL_ACCESS về VIEW (alternative flow)
pm.test("[TC_COL_007] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

// =========================================================
// TC_COL_008 - GET {{baseUrl}}/api/diagram/{{diagramId}}/online-users
// =========================================================
// [TC_COL_008] Mục tiêu: Chụp trạng thái online users trước thao tác remove
pm.test("[TC_COL_008] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const users = pm.response.json();

// [TC_COL_008] Lưu snapshot để đối chiếu (nếu cần)
pm.environment.set("onlineUsersSnapshot", JSON.stringify(users));

// =========================================================
// TC_COL_009 - DELETE {{baseUrl}}/api/diagram/{{diagramId}}/collaborations/{{collaborationId}}
// =========================================================
// [TC_COL_009] Mục tiêu: Xóa collaborator khỏi diagram
pm.test("[TC_COL_009] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

// =========================================================
// TC_COL_010 - GET {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_010] Mục tiêu: Đảm bảo collaborator không còn trong danh sách
pm.test("[TC_COL_010] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const list3 = pm.response.json();
const collabId2 = String(pm.environment.get("collaborationId"));
const found3 = list3.find(x => String(x.id) === collabId2);

// [TC_COL_010] Không được còn bản ghi collaborator vừa xóa
pm.test("[TC_COL_010] Collaborator đã bị xóa khỏi list", () => {
  pm.expect(found3).to.not.exist;
});

// =========================================================
// TC_COL_011 - GET {{baseUrl}}/api/diagram/{{diagramId}}/online-users
// =========================================================
// [TC_COL_011] Mục tiêu: Kiểm tra user mục tiêu không còn online sau remove/hạ quyền
pm.test("[TC_COL_011] Status phải là 200", () => {
  pm.response.to.have.status(200);
});

const users2 = pm.response.json();
const target = pm.environment.get("targetUsername");

// [TC_COL_011] targetUsername không được tồn tại trong danh sách online users
pm.test("[TC_COL_011] Target user không còn online", () => {
  const isOnline = Array.isArray(users2) ? users2.includes(target) : false;
  pm.expect(isOnline).to.be.false;
});

// =========================================================
// TC_COL_012 - GET {{baseUrl}}/account/email/{{invalidEmail}}
// =========================================================
// [TC_COL_012] Mục tiêu: Tìm email không tồn tại phải trả về lỗi not found
pm.test("[TC_COL_012] Status phải là 404", () => {
  pm.response.to.have.status(404);
});

// =========================================================
// TC_COL_013 - POST {{baseUrl}}/api/diagram/{{diagramId}}/collaborations
// =========================================================
// [TC_COL_013] Mục tiêu: Add user đã là collaborator phải bị từ chối
pm.test("[TC_COL_013] Status phải là 400 hoặc 409", () => {
  pm.expect([400, 409]).to.include(pm.response.code);
});

// =========================================================
// TC_COL_014 - PATCH {{baseUrl}}/api/diagram/{{diagramId}}/collaborations/99999999
// =========================================================
// [TC_COL_014] Mục tiêu: Update permission cho collaborationId không tồn tại
pm.test("[TC_COL_014] Status phải là 404", () => {
  pm.response.to.have.status(404);
});

// =========================================================
// TC_COL_015 - DELETE {{baseUrl}}/api/diagram/{{diagramId}}/collaborations/99999999
// =========================================================
// [TC_COL_015] Mục tiêu: Xóa collaborationId không tồn tại phải trả về 404
pm.test("[TC_COL_015] Status phải là 404", () => {
  pm.response.to.have.status(404);
});

