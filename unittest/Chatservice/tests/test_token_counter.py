"""
Unit Tests cho module: token_counter.py
Class: TokenCounter

Test framework: PyTest + unittest.mock
Mục tiêu: Kiểm thử các method count_tokens, count_tokens_from_ids,
          encode, decode và xử lý các nhánh lỗi.
"""

import pytest
from unittest.mock import patch, MagicMock
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


# ===========================================================
# TC-TC-001: TokenCounter.__init__ — load tokenizer thành công
# ===========================================================
def test_token_counter_init_success():
    """
    TC-TC-001
    Objective : TokenCounter khởi tạo thành công khi có HF token hợp lệ
    Input     : hf_token='fake_token', mock AutoTokenizer.from_pretrained OK
    Expected  : self.tokenizer không phải None
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake_token')

    assert tc.tokenizer is mock_tokenizer


# ===========================================================
# TC-TC-002: TokenCounter.__init__ — load thất bại, tokenizer = None
# ===========================================================
def test_token_counter_init_failure_sets_none():
    """
    TC-TC-002
    Objective : TokenCounter.tokenizer = None khi from_pretrained raise Exception
    Input     : hf_token='bad_token', from_pretrained raise Exception
    Expected  : tc.tokenizer is None (không crash)
    """
    with patch('token_counter.AutoTokenizer.from_pretrained',
               side_effect=Exception("Model not found")):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='bad_token')

    assert tc.tokenizer is None


# ===========================================================
# TC-TC-003: count_tokens — text bình thường
# ===========================================================
def test_count_tokens_normal_text():
    """
    TC-TC-003
    Objective : count_tokens trả về số token đúng
    Input     : text="Tạo table User với các field cơ bản"
    Expected  : số nguyên bằng len(tokenizer.tokenize(text))
    """
    mock_tokenizer = MagicMock()
    mock_tokenizer.tokenize.return_value = ['Tạo', '▁table', '▁User', '▁với', '▁các', '▁field', '▁cơ', '▁bản']

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.count_tokens("Tạo table User với các field cơ bản")
    assert result == 8


# ===========================================================
# TC-TC-004: count_tokens — text rỗng trả về 0
# ===========================================================
def test_count_tokens_empty_string():
    """
    TC-TC-004
    Objective : count_tokens trả về 0 khi text rỗng ""
    Input     : text=""
    Expected  : 0
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.count_tokens("")
    assert result == 0
    mock_tokenizer.tokenize.assert_not_called()


# ===========================================================
# TC-TC-005: count_tokens — text None trả về 0
# ===========================================================
def test_count_tokens_none_input():
    """
    TC-TC-005
    Objective : count_tokens trả về 0 khi text là None
    Input     : text=None
    Expected  : 0
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.count_tokens(None)
    assert result == 0


# ===========================================================
# TC-TC-006: count_tokens — tokenizer raise Exception → fallback
# ===========================================================
def test_count_tokens_fallback_on_error():
    """
    TC-TC-006
    Objective : count_tokens dùng fallback estimation khi tokenize raise Exception
    Input     : text="hello world test", tokenizer.tokenize raise Exception
    Expected  : trả về len("hello world test".split()) * 1.3 = 3.9
    """
    mock_tokenizer = MagicMock()
    mock_tokenizer.tokenize.side_effect = Exception("Tokenizer error")

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.count_tokens("hello world test")
    assert result == pytest.approx(3 * 1.3)


# ===========================================================
# TC-TC-007: count_tokens_from_ids — list bình thường
# ===========================================================
def test_count_tokens_from_ids_normal():
    """
    TC-TC-007
    Objective : count_tokens_from_ids trả về đúng số lượng token IDs
    Input     : token_ids=[1, 2, 3, 4, 5]
    Expected  : 5
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    assert tc.count_tokens_from_ids([1, 2, 3, 4, 5]) == 5


# ===========================================================
# TC-TC-008: count_tokens_from_ids — list rỗng trả về 0
# ===========================================================
def test_count_tokens_from_ids_empty():
    """
    TC-TC-008
    Objective : count_tokens_from_ids trả về 0 khi list rỗng
    Input     : token_ids=[]
    Expected  : 0
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    assert tc.count_tokens_from_ids([]) == 0


# ===========================================================
# TC-TC-009: count_tokens_from_ids — None trả về 0
# ===========================================================
def test_count_tokens_from_ids_none():
    """
    TC-TC-009
    Objective : count_tokens_from_ids trả về 0 khi input là None
    Input     : token_ids=None
    Expected  : 0
    """
    mock_tokenizer = MagicMock()

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    assert tc.count_tokens_from_ids(None) == 0


# ===========================================================
# TC-TC-010: encode — trả về token IDs đúng
# ===========================================================
def test_encode_returns_token_ids():
    """
    TC-TC-010
    Objective : encode trả về list token IDs từ tokenizer.encode
    Input     : text="Hello chatbot"
    Expected  : list token IDs giống mock tokenizer.encode trả về
    """
    mock_tokenizer = MagicMock()
    mock_tokenizer.encode.return_value = [128000, 9906, 6369, 6465]

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.encode("Hello chatbot")
    assert result == [128000, 9906, 6369, 6465]
    mock_tokenizer.encode.assert_called_once_with("Hello chatbot", add_special_tokens=True)


# ===========================================================
# TC-TC-011: decode — trả về text đúng
# ===========================================================
def test_decode_returns_text():
    """
    TC-TC-011
    Objective : decode trả về text từ list token IDs
    Input     : token_ids=[128000, 9906, 6369, 6465]
    Expected  : "Hello chatbot"
    """
    mock_tokenizer = MagicMock()
    mock_tokenizer.decode.return_value = "Hello chatbot"

    with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer):
        from token_counter import TokenCounter
        tc = TokenCounter(hf_token='fake')

    result = tc.decode([128000, 9906, 6369, 6465])
    assert result == "Hello chatbot"
    mock_tokenizer.decode.assert_called_once_with([128000, 9906, 6369, 6465], skip_special_tokens=True)


# ===========================================================
# TC-TC-012: TokenCounter.__init__ — lấy HF_TOKEN từ env khi không truyền
# ===========================================================
def test_token_counter_init_reads_hf_token_from_env():
    """
    TC-TC-012
    Objective : TokenCounter lấy hf_token từ biến môi trường HF_TOKEN
                khi không truyền tham số hf_token
    Input     : HF_TOKEN='env_token' trong env, hf_token=None
    Expected  : from_pretrained được gọi với token='env_token'
    """
    mock_tokenizer = MagicMock()

    with patch.dict(os.environ, {'HF_TOKEN': 'env_token'}):
        with patch('token_counter.AutoTokenizer.from_pretrained', return_value=mock_tokenizer) as mock_ft:
            from token_counter import TokenCounter
            tc = TokenCounter()  # không truyền hf_token

    mock_ft.assert_called_once_with(
        'meta-llama/Meta-Llama-3.1-8B',
        token='env_token'
    )
