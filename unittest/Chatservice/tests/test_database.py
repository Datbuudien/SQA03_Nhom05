"""
Unit Tests cho module: database.py
Class: Database

Test framework: PyTest + unittest.mock
Mục tiêu: Kiểm thử toàn bộ các method trong Database class
          với branch coverage cấp 2 (phủ hết các nhánh).

Lưu ý: Tất cả các test sử dụng mock để không cần kết nối DB thật.
       Không có side-effect ra ngoài (rollback-safe).
"""

import pytest
from unittest.mock import patch, MagicMock, call
from mysql.connector import Error as MySQLError

# Import module cần test
import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
from database import Database


# ===========================================================
# TC-DB-001: get_connection — kết nối thành công
# ===========================================================
def test_get_connection_success():
    """
    TC-DB-001
    Objective : get_connection trả về connection object khi DB sẵn sàng
    Input     : config hợp lệ (mock mysql.connector.connect)
    Expected  : trả về mock connection object (không None)
    """
    db = Database()
    mock_conn = MagicMock()

    with patch('database.mysql.connector.connect', return_value=mock_conn) as mock_connect:
        result = db.get_connection()

    assert result is mock_conn
    mock_connect.assert_called_once_with(**db.config)


# ===========================================================
# TC-DB-002: get_connection — kết nối thất bại (MySQL Error)
# ===========================================================
def test_get_connection_failure_returns_none():
    """
    TC-DB-002
    Objective : get_connection trả về None khi MySQL raise Error
    Input     : mysql.connector.connect raise MySQLError
    Expected  : trả về None
    """
    db = Database()

    with patch('database.mysql.connector.connect', side_effect=MySQLError("Connection refused")):
        result = db.get_connection()

    assert result is None


# ===========================================================
# TC-DB-003: init_database — thành công, bảng được tạo
# ===========================================================
def test_init_database_success():
    """
    TC-DB-003
    Objective : init_database trả về True khi tạo bảng thành công
    Input     : mock connection + cursor hoạt động bình thường
    Expected  : True, cursor.execute và connection.commit được gọi
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        result = db.init_database()

    assert result is True
    mock_cursor.execute.assert_called_once()
    mock_conn.commit.assert_called_once()


# ===========================================================
# TC-DB-004: init_database — get_connection trả về None
# ===========================================================
def test_init_database_no_connection():
    """
    TC-DB-004
    Objective : init_database trả về False khi không lấy được connection
    Input     : get_connection trả về None
    Expected  : False
    """
    db = Database()

    with patch.object(db, 'get_connection', return_value=None):
        result = db.init_database()

    assert result is False


# ===========================================================
# TC-DB-005: init_database — cursor.execute raise Error
# ===========================================================
def test_init_database_execute_error():
    """
    TC-DB-005
    Objective : init_database trả về False khi cursor raise MySQLError
    Input     : cursor.execute raise MySQLError
    Expected  : False
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.execute.side_effect = MySQLError("Syntax error")
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        result = db.init_database()

    assert result is False


# ===========================================================
# TC-DB-006: insert_metrics — thành công
# ===========================================================
def test_insert_metrics_success():
    """
    TC-DB-006
    Objective : insert_metrics trả về True và gọi execute đúng tham số
    Input     : tất cả tham số hợp lệ, mock connection OK
    Expected  : True, cursor.execute được gọi với đúng values
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        result = db.insert_metrics(
            input_text="Tạo table User",
            output_text="Đã tạo table User với 3 fields",
            input_tokens=10,
            output_tokens=15,
            ttft_ms=120.5,
            total_time_ms=500.0,
            status='success',
            error_message=None
        )

    assert result is True
    mock_conn.commit.assert_called_once()
    # Kiểm tra total_tokens được tính đúng (10 + 15 = 25)
    call_args = mock_cursor.execute.call_args[0][1]
    assert call_args[4] == 25  # total_tokens index


# ===========================================================
# TC-DB-007: insert_metrics — get_connection trả về None
# ===========================================================
def test_insert_metrics_no_connection():
    """
    TC-DB-007
    Objective : insert_metrics trả về False khi không có connection
    Input     : get_connection trả về None
    Expected  : False
    """
    db = Database()

    with patch.object(db, 'get_connection', return_value=None):
        result = db.insert_metrics("input", "output", 5, 5, 100, 200)

    assert result is False


# ===========================================================
# TC-DB-008: insert_metrics — cursor.execute raise Error
# ===========================================================
def test_insert_metrics_execute_error():
    """
    TC-DB-008
    Objective : insert_metrics trả về False khi execute raise MySQLError
    Input     : cursor.execute raise MySQLError("Duplicate entry")
    Expected  : False
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.execute.side_effect = MySQLError("Duplicate entry")
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        result = db.insert_metrics("input", "output", 5, 5, 100, 200, status='error')

    assert result is False


# ===========================================================
# TC-DB-009: insert_metrics — total_tokens được tính đúng
# ===========================================================
def test_insert_metrics_total_tokens_calculation():
    """
    TC-DB-009
    Objective : total_tokens = input_tokens + output_tokens được tính chính xác
    Input     : input_tokens=100, output_tokens=200
    Expected  : total_tokens=300 trong câu query
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        db.insert_metrics("input", "output", 100, 200, 50.0, 300.0)

    values_passed = mock_cursor.execute.call_args[0][1]
    assert values_passed[4] == 300  # total_tokens = 100 + 200


# ===========================================================
# TC-DB-010: insert_metrics — status='error' với error_message
# ===========================================================
def test_insert_metrics_with_error_status():
    """
    TC-DB-010
    Objective : insert_metrics lưu đúng khi status='error' và có error_message
    Input     : status='error', error_message='Request timeout'
    Expected  : True, values chứa đúng status và error_message
    """
    db = Database()
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.is_connected.return_value = True

    with patch.object(db, 'get_connection', return_value=mock_conn):
        result = db.insert_metrics(
            input_text="query",
            output_text="",
            input_tokens=5,
            output_tokens=0,
            ttft_ms=0,
            total_time_ms=30000,
            status='error',
            error_message='Request timeout'
        )

    assert result is True
    values_passed = mock_cursor.execute.call_args[0][1]
    assert values_passed[7] == 'error'
    assert values_passed[8] == 'Request timeout'


# ===========================================================
# TC-DB-011: Database.__init__ — đọc config từ environment
# ===========================================================
def test_database_init_reads_env_vars():
    """
    TC-DB-011
    Objective : Database.__init__ đọc đúng config từ biến môi trường
    Input     : set env DB_HOST='testhost', DB_PORT='3308', DB_NAME='testdb'
    Expected  : db.config phản ánh đúng các giá trị env
    """
    env_vars = {
        'DB_HOST': 'testhost',
        'DB_PORT': '3308',
        'DB_NAME': 'testdb',
        'DB_USER': 'admin',
        'DB_PASSWORD': 'secret'
    }
    with patch.dict(os.environ, env_vars):
        db = Database()

    assert db.config['host'] == 'testhost'
    assert db.config['port'] == 3308
    assert db.config['database'] == 'testdb'
    assert db.config['user'] == 'admin'
    assert db.config['password'] == 'secret'


# ===========================================================
# TC-DB-012: Database.__init__ — dùng default config khi không có env
# ===========================================================
def test_database_init_default_config():
    """
    TC-DB-012
    Objective : Database dùng giá trị mặc định khi không set env vars
    Input     : không có env vars DB_*
    Expected  : host='localhost', port=3307, database='schema_chatbot'
    """
    # Xóa các env vars liên quan nếu có
    env_clean = {k: v for k, v in os.environ.items()
                 if not k.startswith('DB_')}
    with patch.dict(os.environ, env_clean, clear=True):
        db = Database()

    assert db.config['host'] == 'localhost'
    assert db.config['port'] == 3307
    assert db.config['database'] == 'schema_chatbot'
