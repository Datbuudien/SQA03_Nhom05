// src/services/chatbotService.ts

interface ChatbotRequestModel {
  id: string;
  name: string;
  attributes: Array<{
    id: string;
    name: string;
    dataType: string;
    isPrimaryKey: boolean;
    isForeignKey: boolean;
  }>;
}

export interface ChatbotRequest {
  diagram: {
    models: ChatbotRequestModel[];
  };
  question: string;
  history: string;
  max_new_tokens: number;
  do_sample: boolean;
}

// Delete action structure from API
export interface DeleteAction {
  name: string;
  drop_table: boolean;
  attrs_to_delete: string[];
  fks_to_delete: string[];
}

// Response được parse từ string
export interface ChatbotResponse {
  action: "REFRESH" | "UPDATE" | "REFLECTIVE";
  message: string;
  create: Array<{
    name: string;
    is_new: boolean;
    attrs: Array<{
      name: string;
      type: string;
      pk?: boolean;
    }>;
    fks: Array<{
      column: string;
      references: string;
    }>;
  }>;
  delete: DeleteAction[];
  tomtat: string;
}

class ChatbotService {
  private readonly API_URL = "http://localhost:8080/generate";

  /**
   * Parse string response từ API thành object ChatbotResponse
   */
  parseResponse(responseString: string): ChatbotResponse {
    console.log("📥 Raw response string:", responseString);

    const response: ChatbotResponse = {
      action: "UPDATE",
      message: "",
      create: [],
      delete: [],
      tomtat: "",
    };

    try {
      // Check action type
      if (responseString.startsWith("_REFRESH:")) {
        response.action = "REFRESH";
        responseString = responseString.substring(9); // Remove "_REFRESH:"
      } else if (responseString.startsWith("_REFLECTIVE")) {
        response.action = "REFLECTIVE";
        // Remove "_REFLECTIVE" and any trailing whitespace/colon
        responseString = responseString.substring(11).replace(/^[\s:]+/, "");
      }

      // Extract message (text before "create:")
      const createIndex = responseString.indexOf("create:");
      if (createIndex > 0) {
        response.message = responseString.substring(0, createIndex).trim();
      }

      // Extract create array
      const createMatch = this.extractCreateArray(responseString);
      console.log(createMatch);
      if (createMatch) {
        const createJson = `[${createMatch}]`;
        response.create = JSON.parse(createJson);
        console.log("✅ Parsed create array:", response.create);
      }

      // Extract delete array
      const deleteMatch = this.extractDeleteArray(responseString);
      if (deleteMatch) {
        const deleteJson = `[${deleteMatch}]`;
        response.delete = JSON.parse(deleteJson);
        console.log("✅ Parsed delete array:", response.delete);
      }

      // Extract tomtat
      const tomtatMatch = responseString.match(/tomtat:(.*?)$/s);
      if (tomtatMatch && tomtatMatch[1]) {
        response.tomtat = tomtatMatch[1].trim();
      }
    } catch (error) {
      console.error("❌ Error parsing response:", error);
      throw new Error("Failed to parse chatbot response");
    }

    return response;
  }

  private extractCreateArray(str: string): string | null {
    const start = str.indexOf("create: [");
    if (start === -1) return null;

    let i = start + "create: [".length;
    let depth = 1;

    while (i < str.length) {
      if (str[i] === "[") depth++;
      else if (str[i] === "]") depth--;

      if (depth === 0) {
        // vị trí kết thúc mảng
        return str.slice(start + "create: [".length, i);
      }

      i++;
    }

    return null; // không tìm thấy đóng
  }

  private extractDeleteArray(str: string): string | null {
    const start = str.indexOf("delete: [");
    if (start === -1) return null;

    let i = start + "delete: [".length;
    let depth = 1;

    while (i < str.length) {
      if (str[i] === "[") depth++;
      else if (str[i] === "]") depth--;

      if (depth === 0) {
        // vị trí kết thúc mảng
        return str.slice(start + "delete: [".length, i);
      }

      i++;
    }

    return null; // không tìm thấy đóng
  }

  /**
   * Extract response after "### Response:" marker
   */
  private extractResponseContent(output: string): string {
    const marker = "### Response:";
    const markerIndex = output.indexOf(marker);

    if (markerIndex === -1) {
      console.warn("⚠️ Response marker not found, using full output");
      return output;
    }

    // Lấy phần sau "### Response:"
    const content = output.substring(markerIndex + marker.length).trim();
    console.log("📝 Extracted content after marker:", content);

    return content;
  }

  /**
   * Send request to chatbot API
   */
  async sendMessage(request: ChatbotRequest): Promise<ChatbotResponse> {
    console.log("🤖 Chatbot request:", request);
    console.log("📊 Current models in diagram:", request.diagram.models);
    console.log("string req: ", JSON.stringify(request));
    try {
      const response = await fetch(this.API_URL, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`API request failed with status ${response.status}`);
      }

      const data = await response.json();
      console.log("📦 API response:", data);

      // Extract output từ response
      if (!data.output) {
        throw new Error("No output field in API response");
      }

      // Lấy phần sau "### Response:"
      const responseContent = this.extractResponseContent(data.output);

      // Parse response string thành ChatbotResponse object
      const parsedResponse = this.parseResponse(responseContent);
      console.log("✅ Parsed response:", parsedResponse);

      return parsedResponse;
    } catch (error) {
      console.error("❌ Error calling chatbot API:", error);

      // Fallback error response
      return {
        action: "UPDATE",
        message: "Xin lỗi, đã xảy ra lỗi khi xử lý yêu cầu của bạn.",
        create: [],
        delete: [],
        tomtat: "Không thể kết nối đến server AI. Vui lòng thử lại sau.",
      };
    }
  }
}

export const chatbotService = new ChatbotService();
