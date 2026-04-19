// src/api/diagramApi.ts

export interface DiagramListFilters {
  lastDiagramId?: number;
  pageSize?: number;
  nameStartsWith?: string;
  searchQuery?: string;
  ownerFilter?: "me" | "team";
  dateRange?: "today" | "last7days" | "last30days" | "alltime";
  isDeleted?: boolean;
  sharedWithMe?: boolean;
  sortBy?: "name" | "createdAt" | "updatedAt";
  sortDirection?: "ASC" | "DESC";
}

export interface DiagramListItem {
  id: number;
  name: string;
  ownerUsername: string;
  ownerFullName: string;
  ownerAvatar?: string;
  updatedByUsername: string;
  updatedByFullName: string;
  updatedByAvatar?: string;
  updatedAt: string;
  createdAt: string;
  createdByUsername: string;
  createdByFullName: string;
  isStarred: boolean;
  hasCollaborators: boolean;
  lastMigrationUsername?: string;
  lastMigrationDate?: string;
}

export interface DiagramListResponse {
  diagrams: DiagramListItem[];
  lastDiagramId: number | null;
  hasMore: boolean;
  totalCount: number;
}

export const diagramApi = {
  /**
   * Get paginated list of diagrams
   */
  async getList(
    filters: DiagramListFilters = {}
  ): Promise<DiagramListResponse> {
    const params = new URLSearchParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        params.append(key, value.toString());
      }
    });

    const response = await fetch(
      `http://localhost:8080/api/diagrams/list?${params.toString()}`,
      {
        method: "GET",
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to fetch diagrams");
    }

    return response.json();
  },

  /**
   * Soft delete diagram (move to trash)
   */
  async deleteDiagram(diagramId: number): Promise<void> {
    const response = await fetch(
      `http://localhost:8080/api/diagrams/${diagramId}`,
      {
        method: "DELETE",
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to delete diagram");
    }
  },

  /**
   * Restore diagram from trash
   */
  async restoreDiagram(diagramId: number): Promise<void> {
    const response = await fetch(
      `http://localhost:8080/api/diagrams/${diagramId}/restore`,
      {
        method: "POST",
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to restore diagram");
    }
  },

  /**
   * Permanently delete diagram
   */
  async permanentlyDelete(diagramId: number): Promise<void> {
    const response = await fetch(
      `http://localhost:8080/api/diagrams/${diagramId}/permanent`,
      {
        method: "DELETE",
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to permanently delete diagram");
    }
  },
};
