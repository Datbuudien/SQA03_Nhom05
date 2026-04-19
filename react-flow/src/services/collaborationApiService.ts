// src/services/collaborationApiService.ts
import { CollaborationMember, AccountDTO } from "../types/collaboration.types";

class CollaborationApiService {
  private baseUrl = "http://localhost:8080";

  async getCollaborations(diagramId: string): Promise<CollaborationMember[]> {
    const response = await fetch(
      `${this.baseUrl}/api/diagram/${diagramId}/collaborations`,
      {
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to fetch collaborations");
    }

    return response.json();
  }

  async searchAccountByEmail(email: string): Promise<AccountDTO> {
    const response = await fetch(`${this.baseUrl}/account/email/${email}`, {
      credentials: "include",
    });

    if (!response.ok) {
      if (response.status === 404) {
        throw new Error("Account not found");
      }
      throw new Error("Failed to search account");
    }

    return response.json();
  }

  async addCollaborator(
    diagramId: string,
    username: string,
    permission: "VIEW" | "FULL_ACCESS"
  ): Promise<CollaborationMember> {
    const response = await fetch(
      `${this.baseUrl}/api/diagram/${diagramId}/collaborations`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, permission }),
      }
    );

    if (!response.ok) {
      return response.json().then((data) => {
        throw new Error(data.message || "Unknown error");
      });
    }

    return response.json();
  }

  async updatePermission(
    diagramId: string,
    collaborationId: number,
    permission: "VIEW" | "FULL_ACCESS"
  ): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/api/diagram/${diagramId}/collaborations/${collaborationId}`,
      {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ permission }),
      }
    );

    if (!response.ok) {
      throw new Error("Failed to update permission");
    }
  }

  async removeCollaborator(
    diagramId: string,
    collaborationId: number
  ): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/api/diagram/${diagramId}/collaborations/${collaborationId}`,
      {
        method: "DELETE",
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to remove collaborator");
    }
  }

  async getOnlineUsers(diagramId: string): Promise<Set<string>> {
    const response = await fetch(
      `${this.baseUrl}/api/diagram/${diagramId}/online-users`,
      {
        credentials: "include",
      }
    );

    if (!response.ok) {
      throw new Error("Failed to fetch online users");
    }

    return response.json();
  }
}

export const collaborationApiService = new CollaborationApiService();
