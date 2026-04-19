// src/utils/nodeHelpers.ts

/**
 * Helper functions để làm việc với ReactFlow nodes
 */

/**
 * Tìm attribute ID theo tên trong một model
 */
export const findAttributeIdByName = (
  allNodes: Map<string, any>,
  modelId: string,
  attrName: string
): string | null => {
  const node = allNodes.get(modelId);
  if (!node) {
    console.log("Không tìm thấy model ", node.id);
    return null;
  }

  const attr = node.data?.attributes?.find((a: any) => a.name === attrName);
  return attr?.id || null;
};

/**
 * Tìm model ID theo tên model
 */
export const findModelIdByName = (
  allNodes: Map<string, any>,
  modelName: string
): string | null => {
  console.log("namee: " + modelName);
  console.log("nodess: ", allNodes);
  for (const [modelId, node] of allNodes.entries()) {
    if (node.data?.name == modelName) {
      return node.id;
    }
  }
  return null;
};

/**
 * Tìm tất cả attributes trong một model
 */
export const getModelAttributes = (
  allNodes: Map<string, any>,
  modelId: string
): any[] => {
  const node = allNodes.get(modelId);
  if (!node || node.type !== "model") return [];

  return node.data?.attributes || [];
};

/**
 * Check xem model đã tồn tại chưa
 */
export const doesModelExist = (
  allNodes: Map<string, any>,
  modelName: string
): boolean => {
  return findModelIdByName(allNodes, modelName) !== null;
};

/**
 * Check xem attribute đã tồn tại trong model chưa
 */
export const doesAttributeExist = (
  allNodes: Map<string, any>,
  modelId: string,
  attrName: string
): boolean => {
  return findAttributeIdByName(allNodes, modelId, attrName) !== null;
};

/**
 * Lấy thông tin đầy đủ của một model
 */
export const getModelInfo = (
  allNodes: Map<string, any>,
  modelId: string
): {
  id: string;
  name: string;
  attributes: any[];
} | null => {
  const node = allNodes.get(modelId);
  if (!node || node.type !== "model") return null;

  return {
    id: node.id,
    name: node.data?.name || node.id,
    attributes: node.data?.attributes || [],
  };
};

/**
 * Tạo một delay Promise (helper cho async operations)
 */
export const delay = (ms: number) =>
  new Promise((resolve) => setTimeout(resolve, ms));
