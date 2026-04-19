from transformers import AutoTokenizer
import os
from dotenv import load_dotenv

load_dotenv()

class TokenCounter:
    def __init__(self, model_name="meta-llama/Meta-Llama-3.1-8B", hf_token=None):
        """
        Khởi tạo tokenizer để đếm token
        Chỉ load tokenizer, không load model để tiết kiệm tài nguyên
        
        Args:
            model_name: Tên model trên HuggingFace
            hf_token: HuggingFace token (optional, lấy từ env nếu không truyền)
        """
        print(f"Loading tokenizer from {model_name}...")
        
        # Lấy token từ parameter hoặc environment variable
        if hf_token is None:
            hf_token = os.getenv("HF_TOKEN")
        print(hf_token)
        try:
            # Load tokenizer với token
            if hf_token:
                self.tokenizer = AutoTokenizer.from_pretrained(
                    model_name, 
                    token=hf_token
                )
            
            print(f"Tokenizer loaded successfully from {model_name}")
        except Exception as e:
            print(f"✗ Error loading tokenizer from {model_name}: {e}")
            self.tokenizer = None
    
    def count_tokens(self, text):
        """
        Đếm số token trong text
        
        Args:
            text (str): Text cần đếm token
            
        Returns:
            int: Số lượng token
        """
        if not text:
            return 0
        
        try:
            # Tokenize text
            tokens = self.tokenizer.tokenize(text)
            return len(tokens)
        except Exception as e:
            print(f"Error counting tokens: {e}")
            # Fallback: ước lượng dựa trên số từ (rough estimation)
            return len(text.split()) * 1.3  # Ước lượng 1 từ ~= 1.3 token
    
    def count_tokens_from_ids(self, token_ids):
        """
        Đếm token từ list token IDs
        
        Args:
            token_ids (list): List of token IDs
            
        Returns:
            int: Số lượng token
        """
        return len(token_ids) if token_ids else 0
    
    def encode(self, text):
        """
        Encode text thành token IDs
        
        Args:
            text (str): Text cần encode
            
        Returns:
            list: List of token IDs
        """
        return self.tokenizer.encode(text, add_special_tokens=True)
    
    def decode(self, token_ids):
        """
        Decode token IDs thành text
        
        Args:
            token_ids (list): List of token IDs
            
        Returns:
            str: Decoded text
        """
        return self.tokenizer.decode(token_ids, skip_special_tokens=True)