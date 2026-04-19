const API_BASE_URL = "http://localhost:8080";
const CHAT_API_URL = "http://localhost:8080";

export interface StatisticsData {
  totalAccounts: number;
  totalDiagrams: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  avgTTFT: number;
  avgTotalTime: number;
  successfulRequests: number;
  totalRequests: number;
  accountGrowth?: number;
  diagramGrowth?: number;
}

export const statisticsApi = {
  /**
   * Get comprehensive statistics
   */
  async getStatistics(timeRange: string = "7days"): Promise<StatisticsData> {
    try {
      // Fetch account statistics
      const accountResponse = await fetch(
        `${API_BASE_URL}/account/search?size=1`,
        {
          credentials: "include",
        }
      );
      const accountData = await accountResponse.json();

      // Fetch diagram statistics
      const diagramResponse = await fetch(
        `${API_BASE_URL}/api/diagrams/list?pageSize=1`,
        {
          credentials: "include",
        }
      );
      const diagramData = await diagramResponse.json();

      // Fetch chat metrics
      const metricsResponse = await fetch(`${CHAT_API_URL}/metrics?limit=100`);
      const metricsData = await metricsResponse.json();

      return {
        totalAccounts: accountData.totalElements || 0,
        totalDiagrams: diagramData.totalElements || 0,
        totalInputTokens: metricsData.statistics?.total_tokens_processed
          ? Math.floor(metricsData.statistics.total_tokens_processed * 0.4)
          : 0,
        totalOutputTokens: metricsData.statistics?.total_tokens_processed
          ? Math.floor(metricsData.statistics.total_tokens_processed * 0.6)
          : 0,
        totalTokens: metricsData.statistics?.total_tokens_processed || 0,
        avgTTFT: metricsData.statistics?.avg_ttft_ms || 0,
        avgTotalTime: metricsData.statistics?.avg_total_time_ms || 0,
        successfulRequests: metricsData.statistics?.successful_requests || 0,
        totalRequests: metricsData.statistics?.total_requests || 0,
      };
    } catch (error) {
      console.error("Error fetching statistics:", error);
      throw error;
    }
  },

  /**
   * Get chat metrics details
   */
  async getChatMetrics(limit: number = 100) {
    try {
      const response = await fetch(`${CHAT_API_URL}/metrics?limit=${limit}`, {
        credentials: "include",
      });
      if (!response.ok) {
        throw new Error("Failed to fetch chat metrics");
      }
      return await response.json();
    } catch (error) {
      console.error("Error fetching chat metrics:", error);
      throw error;
    }
  },

  /**
   * Get specific metric detail
   */
  async getMetricDetail(metricId: number) {
    try {
      const response = await fetch(`${CHAT_API_URL}/metrics/${metricId}`, {
        credentials: "include",
      });
      if (!response.ok) {
        throw new Error("Failed to fetch metric detail");
      }
      return await response.json();
    } catch (error) {
      console.error("Error fetching metric detail:", error);
      throw error;
    }
  },
};
