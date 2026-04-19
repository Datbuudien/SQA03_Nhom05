from flask import Flask, request, jsonify, Response
import requests
import json
import time
from database import Database
from token_counter import TokenCounter

app = Flask(__name__)

# URL của Kaggle API (thay bằng ngrok URL của bạn)
KAGGLE_API_URL = "https://YOUR_NGROK_URL.ngrok-free.app"

# Khởi tạo database và token counter
db = Database()
token_counter = None

def init_services():
    """Khởi tạo các service cần thiết"""
    global token_counter
    
    print("="*50)
    print("Initializing services...")
    print("="*50)
    
    # Khởi tạo database
    print("\n1. Initializing database...")
    db.init_database()
    
    # Khởi tạo token counter
    print("\n2. Loading tokenizer...")
    try:
        token_counter = TokenCounter()
    except Exception as e:
        print(f"Warning: Could not initialize token counter: {e}")
        token_counter = None
    
    print("\n" + "="*50)
    print("Initialization complete!")
    print("="*50 + "\n")

@app.route("/", methods=["GET"])
def health_check():
    return jsonify({
        "status": "ok", 
        "message": "Proxy service is running",
        "tokenizer_loaded": token_counter is not None,
        "kaggle_url": KAGGLE_API_URL
    })

@app.route("/generate", methods=["POST"])
def generate():
    """Forward request đến Kaggle API (non-streaming) với tracking metrics"""
    start_time = time.time()
    ttft = 0
    input_text = ""
    output_text = ""
    input_tokens = 0
    output_tokens = 0
    status = "success"
    error_message = None
    
    try:
        data = request.get_json()
        # Tạo input text từ diagram, question, history (giống bên Kaggle)
        diagram = data.get("diagram", {})
        question = data.get("question", "")
        history = data.get("history", "None")
        
        json_compact = json.dumps(diagram, ensure_ascii=False, separators=(', ', ': '))
        input_text = f"""database hiện tại: {json_compact}
        
        câu hỏi: {question} 
        
        lịch sử: {history}"""
        
        # Đếm input tokens
        if token_counter and input_text:
            input_tokens = token_counter.count_tokens(input_text)
            print(f"Input tokens: {input_tokens}")
        
        # Forward request đến Kaggle
        response = requests.post(
            f"{KAGGLE_API_URL}/generate",
            json=data,
            timeout=500
        )
        print(response)
        response_data = response.json()
        # response_data = data.copy()
        # output_text = "This is a mock chatbot response for testing purposes."
        # response_data["response"] = output_text
        
        # Lấy output text từ response
        output_text = response_data.get("response", "") or response_data.get("output", "") or response_data.get("text", "")
        print(output_text)
        # Đếm output tokens
        if token_counter and output_text:
            output_tokens = token_counter.count_tokens(output_text)
            print(f"Output tokens: {output_tokens}")
        
        # Tính total time
        total_time = (time.time() - start_time) * 1000  # Convert to milliseconds
        
        # Lưu metrics vào database
        db.insert_metrics(
            input_text=input_text,
            output_text=output_text,
            input_tokens=input_tokens,
            output_tokens=output_tokens,
            ttft_ms=0,
            total_time_ms=total_time,
            status=status,
            error_message=error_message
        )
        
        # Thêm metrics vào response
        response_data["metrics"] = {
            "input_tokens": input_tokens,
            "output_tokens": output_tokens,
            "total_tokens": input_tokens + output_tokens,
            "ttft_ms": round(ttft, 2),
            "total_time_ms": round(total_time, 2)
        }
        
        print(f"Request completed - Total time: {total_time:.2f}ms")
        
        # return jsonify(response_data), response.status_code
        return jsonify(response_data), 200
    
    except requests.Timeout:
        status = "timeout"
        error_message = "Request timeout"
        total_time = (time.time() - start_time) * 1000
        
        # Lưu metrics ngay cả khi có lỗi
        db.insert_metrics(
            input_text=input_text,
            output_text="",
            input_tokens=input_tokens,
            output_tokens=0,
            ttft_ms=ttft or 0,
            total_time_ms=total_time,
            status=status,
            error_message=error_message
        )
        
        return jsonify({"error": error_message}), 504
    
    except Exception as e:
        status = "error"
        error_message = str(e)
        total_time = (time.time() - start_time) * 1000
        
        # Lưu metrics ngay cả khi có lỗi
        db.insert_metrics(
            input_text=input_text,
            output_text="",
            input_tokens=input_tokens,
            output_tokens=0,
            ttft_ms=ttft or 0,
            total_time_ms=total_time,
            status=status,
            error_message=error_message
        )
        
        return jsonify({"error": error_message}), 500

@app.route("/generate-stream", methods=["POST"])
def generate_stream():
    """Forward streaming request đến Kaggle API với tracking metrics"""
    start_time = time.time()
    ttft = None
    first_token_received = False
    input_text = ""
    output_text = ""
    input_tokens = 0
    output_tokens = 0
    status = "success"
    error_message = None
    
    try:
        data = request.get_json()
        input_text = data.get("prompt", "") or data.get("input", "") or data.get("message", "")
        
        # Đếm input tokens
        if token_counter and input_text:
            input_tokens = token_counter.count_tokens(input_text)
            print(f"Input tokens: {input_tokens}")
        
        def stream():
            nonlocal ttft, first_token_received, output_text, output_tokens
            
            try:
                # Stream từ Kaggle API
                with requests.post(
                    f"{KAGGLE_API_URL}/generate-stream",
                    json=data,
                    stream=True,
                    timeout=500
                ) as response:
                    for line in response.iter_lines():
                        if line:
                            # Đánh dấu TTFT khi nhận token đầu tiên
                            if not first_token_received:
                                ttft = (time.time() - start_time) * 1000
                                first_token_received = True
                                print(f"TTFT: {ttft:.2f}ms")
                            
                            decoded_line = line.decode('utf-8')
                            
                            # Thu thập output text từ stream
                            try:
                                # Parse JSON nếu có thể
                                if decoded_line.startswith('data: '):
                                    json_str = decoded_line[6:]  # Remove 'data: ' prefix
                                    chunk_data = json.loads(json_str)
                                    chunk_text = chunk_data.get("text", "") or chunk_data.get("response", "")
                                    output_text += chunk_text
                            except:
                                pass
                            
                            yield decoded_line + '\n'
                
                # Đếm output tokens sau khi stream xong
                if token_counter and output_text:
                    output_tokens = token_counter.count_tokens(output_text)
                    print(f"Output tokens: {output_tokens}")
                
                # Tính total time
                total_time = (time.time() - start_time) * 1000
                
                # Lưu metrics vào database
                db.insert_metrics(
                    input_text=input_text,
                    output_text=output_text,
                    input_tokens=input_tokens,
                    output_tokens=output_tokens,
                    ttft_ms=ttft or 0,
                    total_time_ms=total_time,
                    status=status,
                    error_message=error_message
                )
                
                print(f"Stream completed - Total time: {total_time:.2f}ms, TTFT: {ttft:.2f}ms")
                
            except Exception as e:
                error_msg = str(e)
                print(f"Stream error: {error_msg}")
                
                # Lưu metrics khi có lỗi
                total_time = (time.time() - start_time) * 1000
                db.insert_metrics(
                    input_text=input_text,
                    output_text=output_text,
                    input_tokens=input_tokens,
                    output_tokens=output_tokens,
                    ttft_ms=ttft or 0,
                    total_time_ms=total_time,
                    status="error",
                    error_message=error_msg
                )
        
        return Response(stream(), mimetype="text/event-stream")
    
    except Exception as e:
        status = "error"
        error_message = str(e)
        total_time = (time.time() - start_time) * 1000
        
        # Lưu metrics ngay cả khi có lỗi
        db.insert_metrics(
            input_text=input_text,
            output_text="",
            input_tokens=input_tokens,
            output_tokens=0,
            ttft_ms=0,
            total_time_ms=total_time,
            status=status,
            error_message=error_message
        )
        
        return jsonify({"error": error_message}), 500

@app.route("/set-kaggle-url", methods=["POST"])
def set_kaggle_url():
    """Cập nhật Kaggle API URL"""
    global KAGGLE_API_URL
    data = request.get_json()
    new_url = data.get("url", "")
    
    if new_url:
        KAGGLE_API_URL = new_url.rstrip('/')
        return jsonify({"message": f"Kaggle URL updated to: {KAGGLE_API_URL}"})
    
    return jsonify({"error": "No URL provided"}), 400

@app.route("/get-kaggle-url", methods=["GET"])
def get_kaggle_url():
    """Lấy Kaggle API URL hiện tại"""
    return jsonify({"kaggle_url": KAGGLE_API_URL})

@app.route("/metrics", methods=["GET"])
def get_metrics():
    """Lấy metrics từ database"""
    try:
        connection = db.get_connection()
        if connection is None:
            return jsonify({"error": "Database connection failed"}), 500
        
        cursor = connection.cursor(dictionary=True)
        
        # Lấy limit từ query params (default: 10)
        limit = request.args.get('limit', 10, type=int)
        
        # Query latest metrics
        query = """
        SELECT id, created_at, input_tokens, output_tokens, total_tokens, 
               ttft_ms, total_time_ms, status,
               SUBSTRING(input_text, 1, 100) as input_preview,
               SUBSTRING(output_text, 1, 100) as output_preview
        FROM chat_metrics 
        ORDER BY created_at DESC 
        LIMIT %s
        """
        
        cursor.execute(query, (limit,))
        metrics = cursor.fetchall()
        
        # Query statistics
        stats_query = """
        SELECT 
            COUNT(*) as total_requests,
            SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END) as successful_requests,
            AVG(ttft_ms) as avg_ttft_ms,
            AVG(total_time_ms) as avg_total_time_ms,
            AVG(input_tokens) as avg_input_tokens,
            AVG(output_tokens) as avg_output_tokens,
            SUM(total_tokens) as total_tokens_processed
        FROM chat_metrics
        """
        
        cursor.execute(stats_query)
        stats = cursor.fetchone()
        
        cursor.close()
        connection.close()
        
        return jsonify({
            "metrics": metrics,
            "statistics": stats
        })
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/metrics/<int:metric_id>", methods=["GET"])
def get_metric_detail(metric_id):
    """Lấy chi tiết một metric record"""
    try:
        connection = db.get_connection()
        if connection is None:
            return jsonify({"error": "Database connection failed"}), 500
        
        cursor = connection.cursor(dictionary=True)
        
        query = """
        SELECT * FROM chat_metrics WHERE id = %s
        """
        
        cursor.execute(query, (metric_id,))
        metric = cursor.fetchone()
        
        cursor.close()
        connection.close()
        
        if metric:
            return jsonify(metric)
        else:
            return jsonify({"error": "Metric not found"}), 404
        
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == "__main__":
    print("="*50)
    print("Local Proxy Service with Metrics Tracking")
    print("="*50)
    print(f"Running on: http://localhost:8000")
    print(f"Forwarding to: {KAGGLE_API_URL}")
    print(f"Database: MySQL localhost:3307/schema_chatbot")
    print("="*50)
    
    # Initialize services
    init_services()
    
    app.run(host="0.0.0.0", port=8000, debug=False, threaded=True)