"""
conftest.py — Cấu hình chung cho toàn bộ pytest session.

Cung cấp:
- autouse fixture để reset trạng thái module giữa các test
- Đảm bảo không có side-effect giữa các test cases
"""

import pytest
import sys
import os

# Thêm thư mục cha vào sys.path để import được các module
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


@pytest.fixture(autouse=True)
def reset_app_module():
    """
    Reset module app giữa các test để tránh state leak.
    Chạy tự động cho mọi test trong session.
    """
    yield
    # Cleanup: remove cached modules để reload sạch ở test tiếp theo
    modules_to_remove = [key for key in sys.modules
                         if key in ('app', 'database', 'token_counter')]
    for mod in modules_to_remove:
        sys.modules.pop(mod, None)
