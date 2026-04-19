// src/types/collaboration.types.ts
export interface CollaborationMember {
  id: number;
  username: string;
  picture: string;
  type: "OWNER" | "PARTICIPANTS";
  permission: "VIEW" | "FULL_ACCESS";
  isActive: boolean;
  createdAt: string;
}

export interface AccountDTO {
  id?: number;
  username: string;
  picture: string;
  name: string;
  role?: string;
}

export interface CollaborationResponse {
  collaborations: CollaborationMember[];
}
