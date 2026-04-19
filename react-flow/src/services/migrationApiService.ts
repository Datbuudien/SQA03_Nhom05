// src/services/migrationApiService.ts
export interface MigrationHistoryDto {
  id: number;
  username: string;
  createdAt: string;
  snapshotHash: string;
  diagramId: number;
  diagramName: string;
}

export interface MigrationDetailDto {
  id: number;
  username: string;
  createdAt: string;
  snapshotHash: string;
  snapshotJson: string;
  diagramId: number;
  diagramName: string;
}

const API_BASE_URL = "http://localhost:8080/api/migration";

export const migrationApiService = {
  /**
   * Lấy danh sách history của một diagram
   */
  async getDiagramHistory(diagramId: string): Promise<MigrationHistoryDto[]> {
    try {
      const response = await fetch(
        `${API_BASE_URL}/diagram/${diagramId}/history`,
        {
          credentials: "include",
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error("Error fetching diagram history:", error);
      throw error;
    }
  },

  /**
   * Lấy chi tiết một migration snapshot
   */
  async getMigrationDetail(migrationId: number): Promise<MigrationDetailDto> {
    try {
      const response = await fetch(`${API_BASE_URL}/${migrationId}`, {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      return await response.json();
    } catch (error) {
      console.error("Error fetching migration detail:", error);
      throw error;
    }
  },
};
