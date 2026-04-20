"""
Unit Tests cho module: app.py
Routes: /generate, /generate-stream, /set-kaggle-url,
        /get-kaggle-url, /metrics, /metrics/<id>, /

Test framework: PyTest + Flask test client + unittest.mock
Mục tiêu: Kiểm thử toàn bộ các route với branch coverage cấp 2.
          Mock toàn bộ DB, external HTTP calls và token_counter.
"""

import pytest
import json
from unittest.mock import patch, MagicMock
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


# --------------- FIXTURE: khởi tạo Flask test client ---------------

@pytest.fixture
def client():
    """
    Khởi tạo Flask test client với mock DB và TokenCounter.
    Mọi test trong file này đều tự động dùng fixture này.
    """
    mock_db = MagicMock()
    mock_tc = MagicMock()
    mock_tc.count_tokens.return_value = 10

    with patch('app.Database', return_value=mock_db), \
         patch('app.TokenCounter', return_value=mock_tc):
        import importlib
        import app as app_module
        importlib.reload(app_module)  # reload để áp dụng mock
        app_module.app.config['TESTING'] = True
        app_module.token_counter = mock_tc
        app_module.db = mock_db
        with app_module.app.test_client() as c:
            yield c, mock_db, mock_tc


# ===========================================================
# TC-APP-001: GET / — health check trả về status ok
# ===========================================================
def test_health_check_returns_ok(client):
    """
    TC-APP-001
    Objective : GET / trả về HTTP 200 và status='ok'
    Input     : GET request đến /
    Expected  : {"status": "ok", ...}, HTTP 200
    """
    c, _, _ = client
    response = c.get('/')
    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['status'] == 'ok'
    assert 'message' in data


# ===========================================================
# TC-APP-002: POST /generate — request hợp lệ, AI trả về response
# ===========================================================
def test_generate_success(client):
    """
    TC-APP-002
    Objective : POST /generate với payload hợp lệ trả về HTTP 200
                và response body chứa 'response' và 'metrics'
    Input     : {"diagram": {"tables": []}, "question": "Tạo table User", "history": "None"}
    Expected  : HTTP 200, response có trường 'metrics'
    """
    c, mock_db, mock_tc = client

    mock_ai_response = MagicMock()
    mock_ai_response.json.return_value = {
        "response": "Đã tạo table User với id, name, email"
    }
    mock_ai_response.status_code = 200

    with patch('app.requests.post', return_value=mock_ai_response):
        response = c.post('/generate',
                          json={
                              "diagram": {"tables": [{"name": "Product"}]},
                              "question": "Tạo table User với các field cơ bản",
                              "history": "None"
                          })

    assert response.status_code == 200
    data = json.loads(response.data)
    assert 'metrics' in data
    assert data['metrics']['input_tokens'] == 10
    assert data['metrics']['output_tokens'] == 10
    assert data['metrics']['total_tokens'] == 20


# ===========================================================
# TC-APP-003: POST /generate — AI service timeout → HTTP 504
# ===========================================================
def test_generate_timeout_returns_504(client):
    """
    TC-APP-003
    Objective : POST /generate trả về 504 khi AI service timeout
    Input     : requests.post raise requests.Timeout
    Expected  : HTTP 504, {"error": "Request timeout"}
    """
    import requests as req_lib
    c, mock_db, _ = client

    with patch('app.requests.post', side_effect=req_lib.Timeout()):
        response = c.post('/generate', json={"question": "test"})

    assert response.status_code == 504
    data = json.loads(response.data)
    assert data['error'] == 'Request timeout'
    # Đảm bảo metrics vẫn được lưu dù timeout
    mock_db.insert_metrics.assert_called_once()
    call_kwargs = mock_db.insert_metrics.call_args
    assert call_kwargs[1]['status'] == 'timeout' or \
           (call_kwargs[0] and 'timeout' in str(call_kwargs))


# ===========================================================
# TC-APP-004: POST /generate — lỗi không xác định → HTTP 500
# ===========================================================
def test_generate_generic_exception_returns_500(client):
    """
    TC-APP-004
    Objective : POST /generate trả về 500 khi có lỗi không xác định
    Input     : requests.post raise Exception("Unknown error")
    Expected  : HTTP 500, {"error": "Unknown error"}
    """
    c, mock_db, _ = client

    with patch('app.requests.post', side_effect=Exception("Unknown error")):
        response = c.post('/generate', json={"question": "test"})

    assert response.status_code == 500
    data = json.loads(response.data)
    assert 'error' in data
    mock_db.insert_metrics.assert_called_once()


# ===========================================================
# TC-APP-005: POST /generate — diagram rỗng vẫn xử lý được
# ===========================================================
def test_generate_empty_diagram(client):
    """
    TC-APP-005
    Objective : POST /generate hoạt động bình thường khi diagram={}
    Input     : {"diagram": {}, "question": "Liệt kê các table"}
    Expected  : HTTP 200, có 'metrics' trong response
    """
    c, _, _ = client

    mock_ai_resp = MagicMock()
    mock_ai_resp.json.return_value = {"response": "Không có table nào"}

    with patch('app.requests.post', return_value=mock_ai_resp):
        response = c.post('/generate', json={"diagram": {}, "question": "Liệt kê các table"})

    assert response.status_code == 200


# ===========================================================
# TC-APP-006: POST /generate — không có question field
# ===========================================================
def test_generate_missing_question_uses_empty_string(client):
    """
    TC-APP-006
    Objective : POST /generate vẫn xử lý khi thiếu field 'question'
                (sử dụng empty string làm default)
    Input     : {"diagram": {}}  — không có 'question'
    Expected  : HTTP 200, không crash
    """
    c, _, _ = client

    mock_ai_resp = MagicMock()
    mock_ai_resp.json.return_value = {"response": "OK"}

    with patch('app.requests.post', return_value=mock_ai_resp):
        response = c.post('/generate', json={"diagram": {}})

    assert response.status_code == 200


# ===========================================================
# TC-APP-007: POST /generate — AI response có trường 'output' thay vì 'response'
# ===========================================================
def test_generate_ai_response_with_output_field(client):
    """
    TC-APP-007
    Objective : extract output_text từ trường 'output' nếu 'response' không có
    Input     : AI trả về {"output": "Result from AI"}
    Expected  : HTTP 200, metrics được tính dựa trên output_text
    """
    c, _, mock_tc = client
    mock_tc.count_tokens.side_effect = [5, 8]  # input=5, output=8

    mock_ai_resp = MagicMock()
    mock_ai_resp.json.return_value = {"output": "Result from AI"}

    with patch('app.requests.post', return_value=mock_ai_resp):
        response = c.post('/generate', json={"question": "test"})

    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['metrics']['total_tokens'] == 13  # 5 + 8


# ===========================================================
# TC-APP-008: POST /set-kaggle-url — URL hợp lệ
# ===========================================================
def test_set_kaggle_url_success(client):
    """
    TC-APP-008
    Objective : POST /set-kaggle-url cập nhật KAGGLE_API_URL thành công
    Input     : {"url": "https://new-ngrok-url.ngrok-free.app"}
    Expected  : HTTP 200, message xác nhận URL mới
    """
    c, _, _ = client
    response = c.post('/set-kaggle-url',
                      json={"url": "https://new-ngrok-url.ngrok-free.app"})

    assert response.status_code == 200
    data = json.loads(response.data)
    assert 'message' in data
    assert 'new-ngrok-url' in data['message']


# ===========================================================
# TC-APP-009: POST /set-kaggle-url — không có URL → HTTP 400
# ===========================================================
def test_set_kaggle_url_missing_url_returns_400(client):
    """
    TC-APP-009
    Objective : POST /set-kaggle-url trả về 400 khi không có 'url' trong body
    Input     : {} (không có url)
    Expected  : HTTP 400, {"error": "No URL provided"}
    """
    c, _, _ = client
    response = c.post('/set-kaggle-url', json={})

    assert response.status_code == 400
    data = json.loads(response.data)
    assert data['error'] == 'No URL provided'


# ===========================================================
# TC-APP-010: GET /get-kaggle-url — trả về URL hiện tại
# ===========================================================
def test_get_kaggle_url(client):
    """
    TC-APP-010
    Objective : GET /get-kaggle-url trả về kaggle_url hiện tại
    Input     : GET request
    Expected  : HTTP 200, {"kaggle_url": <current_url>}
    """
    c, _, _ = client
    response = c.get('/get-kaggle-url')

    assert response.status_code == 200
    data = json.loads(response.data)
    assert 'kaggle_url' in data


# ===========================================================
# TC-APP-011: GET /metrics — thành công, trả về metrics list
# ===========================================================
def test_get_metrics_success(client):
    """
    TC-APP-011
    Objective : GET /metrics trả về HTTP 200 và danh sách metrics
    Input     : mock DB trả về danh sách metrics mẫu
    Expected  : HTTP 200, response có 'metrics' và 'statistics'
    """
    c, mock_db, _ = client
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_cursor.fetchall.return_value = [
        {"id": 1, "status": "success", "input_tokens": 10, "output_tokens": 20}
    ]
    mock_cursor.fetchone.return_value = {
        "total_requests": 1,
        "successful_requests": 1,
        "avg_ttft_ms": 100.0,
        "avg_total_time_ms": 500.0,
        "avg_input_tokens": 10.0,
        "avg_output_tokens": 20.0,
        "total_tokens_processed": 30
    }
    mock_db.get_connection.return_value = mock_conn

    response = c.get('/metrics')

    assert response.status_code == 200
    data = json.loads(response.data)
    assert 'metrics' in data
    assert 'statistics' in data


# ===========================================================
# TC-APP-012: GET /metrics — DB connection None → HTTP 500
# ===========================================================
def test_get_metrics_no_db_connection(client):
    """
    TC-APP-012
    Objective : GET /metrics trả về 500 khi không lấy được DB connection
    Input     : db.get_connection() trả về None
    Expected  : HTTP 500, {"error": "Database connection failed"}
    """
    c, mock_db, _ = client
    mock_db.get_connection.return_value = None

    response = c.get('/metrics')

    assert response.status_code == 500
    data = json.loads(response.data)
    assert data['error'] == 'Database connection failed'


# ===========================================================
# TC-APP-013: GET /metrics?limit=5 — giới hạn kết quả
# ===========================================================
def test_get_metrics_with_limit_param(client):
    """
    TC-APP-013
    Objective : GET /metrics?limit=5 truyền đúng limit=5 vào query
    Input     : query param limit=5
    Expected  : HTTP 200, cursor.execute được gọi với tham số (5,)
    """
    c, mock_db, _ = client
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_cursor.fetchall.return_value = []
    mock_cursor.fetchone.return_value = {
        "total_requests": 0, "successful_requests": 0,
        "avg_ttft_ms": 0, "avg_total_time_ms": 0,
        "avg_input_tokens": 0, "avg_output_tokens": 0,
        "total_tokens_processed": 0
    }
    mock_db.get_connection.return_value = mock_conn

    response = c.get('/metrics?limit=5')

    assert response.status_code == 200
    # Kiểm tra execute được gọi với (5,) cho limit
    execute_calls = mock_cursor.execute.call_args_list
    limit_used = any(call[0][1] == (5,) for call in execute_calls
                     if len(call[0]) > 1)
    assert limit_used


# ===========================================================
# TC-APP-014: GET /metrics/<id> — tìm thấy metric
# ===========================================================
def test_get_metric_detail_found(client):
    """
    TC-APP-014
    Objective : GET /metrics/1 trả về đúng record
    Input     : metric_id=1, mock DB trả về record
    Expected  : HTTP 200, response chứa đúng metric data
    """
    c, mock_db, _ = client
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_cursor.fetchone.return_value = {
        "id": 1, "status": "success",
        "input_text": "Tạo table User",
        "output_text": "Đã tạo",
        "input_tokens": 10, "output_tokens": 15
    }
    mock_db.get_connection.return_value = mock_conn

    response = c.get('/metrics/1')

    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['id'] == 1


# ===========================================================
# TC-APP-015: GET /metrics/<id> — không tìm thấy → HTTP 404
# ===========================================================
def test_get_metric_detail_not_found(client):
    """
    TC-APP-015
    Objective : GET /metrics/999 trả về 404 khi không tìm thấy record
    Input     : metric_id=999, mock DB trả về None
    Expected  : HTTP 404, {"error": "Metric not found"}
    """
    c, mock_db, _ = client
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.fetchone.return_value = None
    mock_conn.cursor.return_value = mock_cursor
    mock_db.get_connection.return_value = mock_conn

    response = c.get('/metrics/999')

    assert response.status_code == 404
    data = json.loads(response.data)
    assert data['error'] == 'Metric not found'


# ===========================================================
# TC-APP-016: GET /metrics/<id> — DB connection None → HTTP 500
# ===========================================================
def test_get_metric_detail_no_connection(client):
    """
    TC-APP-016
    Objective : GET /metrics/<id> trả về 500 khi không có DB connection
    Input     : db.get_connection() trả về None
    Expected  : HTTP 500, {"error": "Database connection failed"}
    """
    c, mock_db, _ = client
    mock_db.get_connection.return_value = None

    response = c.get('/metrics/1')

    assert response.status_code == 500
    data = json.loads(response.data)
    assert data['error'] == 'Database connection failed'


# ===========================================================
# TC-APP-017: POST /generate — token_counter = None (không đếm tokens)
# ===========================================================
def test_generate_without_token_counter(client):
    """
    TC-APP-017
    Objective : POST /generate hoạt động bình thường khi token_counter=None
    Input     : app.token_counter = None
    Expected  : HTTP 200, metrics có input_tokens=0, output_tokens=0
    """
    import app as app_module
    c, _, _ = client
    app_module.token_counter = None  # Simulate failed token counter init

    mock_ai_resp = MagicMock()
    mock_ai_resp.json.return_value = {"response": "OK response"}

    with patch('app.requests.post', return_value=mock_ai_resp):
        response = c.post('/generate', json={"question": "test", "diagram": {}})

    assert response.status_code == 200
    data = json.loads(response.data)
    assert data['metrics']['input_tokens'] == 0
    assert data['metrics']['output_tokens'] == 0


# ===========================================================
# TC-APP-018: POST /generate — AI response trả về 'text' field
# ===========================================================
def test_generate_ai_response_text_field(client):
    """
    TC-APP-018
    Objective : extract output_text từ trường 'text' nếu các trường khác rỗng
    Input     : AI trả về {"text": "AI output from text field"}
    Expected  : HTTP 200, metrics được tính dựa trên 'text' field
    """
    c, _, _ = client

    mock_ai_resp = MagicMock()
    mock_ai_resp.json.return_value = {"text": "AI output from text field"}

    with patch('app.requests.post', return_value=mock_ai_resp):
        response = c.post('/generate', json={"question": "test"})

    assert response.status_code == 200


# ===========================================================
# TC-APP-019: GET /metrics — exception từ cursor → HTTP 500
# ===========================================================
def test_get_metrics_exception_returns_500(client):
    """
    TC-APP-019
    Objective : GET /metrics trả về 500 khi cursor.execute raise Exception
    Input     : cursor.execute raise Exception("DB Error")
    Expected  : HTTP 500, có 'error' trong response
    """
    c, mock_db, _ = client
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.execute.side_effect = Exception("DB Error")
    mock_conn.cursor.return_value = mock_cursor
    mock_db.get_connection.return_value = mock_conn

    response = c.get('/metrics')

    assert response.status_code == 500
    data = json.loads(response.data)
    assert 'error' in data


# ===========================================================
# TC-APP-020: POST /set-kaggle-url — URL bị trim trailing slash
# ===========================================================
def test_set_kaggle_url_trims_trailing_slash(client):
    """
    TC-APP-020
    Objective : set-kaggle-url tự động xóa trailing slash khỏi URL
    Input     : {"url": "https://example.ngrok.app/"}
    Expected  : HTTP 200, KAGGLE_API_URL = "https://example.ngrok.app" (không có /)
    """
    import app as app_module
    c, _, _ = client

    c.post('/set-kaggle-url', json={"url": "https://example.ngrok.app/"})

    assert not app_module.KAGGLE_API_URL.endswith('/')
