// src/hooks/useChatActions.ts
import { useCallback } from "react";
import {
  findAttributeIdByName,
  findModelIdByName,
  delay,
} from "../utils/nodeHelpers";
import { useStoreApi } from "reactflow";

interface UseChatActionsProps {
  allNodes: Map<string, any>;

  // Handlers từ useSchemaVisualizer (async)
  onAddModel?: () => Promise<string | null>;
  onAddAttribute?: (modelId: string) => Promise<string | null>;
  onFieldNameUpdate?: (
    attributeId: string,
    attributeName: string
  ) => Promise<void>;
  onFieldTypeUpdate?: (
    attributeId: string,
    attributeType: string
  ) => Promise<void>;
  onToggleKeyType?: (
    modelId: string,
    attributeId: string,
    keyType: "NORMAL" | "PRIMARY" | "FOREIGN"
  ) => Promise<void>;
  onForeignKeyConnect?: (
    attributeId: string,
    targetModelId: string,
    targetAttributeId: string
  ) => Promise<void>;
  onModelNameUpdate?: (
    modelId: string,
    oldName: string,
    newName: string
  ) => Promise<void>;
  onDeleteModel?: (modelId: string) => Promise<void>;
  onDeleteAttribute?: (modelId: string, attributeId: string) => Promise<void>;
}

/**
 * Custom hook để wrap các handlers thành các functions dễ sử dụng hơn
 * cho AI chat actions
 */
export const useChatActions = ({
  allNodes,
  onAddModel,
  onAddAttribute,
  onFieldNameUpdate,
  onFieldTypeUpdate,
  onToggleKeyType,
  onForeignKeyConnect,
  onModelNameUpdate,
  onDeleteModel,
  onDeleteAttribute,
}: UseChatActionsProps) => {
  const storeApi = useStoreApi();
  const getNodeInternals = () => storeApi.getState().nodeInternals;
  /**
   * Tạo model mới với tên cụ thể
   */
  const handleCreateModel = useCallback(
    async (modelName: string): Promise<void> => {
      if (!onAddModel || !onModelNameUpdate) {
        throw new Error("Required handlers not provided");
      }

      // 1. Tạo model mới
      const id = await onAddModel();
      console.log("vjp pro: ", id);
      await delay(100);

      // 4. Rename model
      await onModelNameUpdate(id, "Model", modelName);

      // 5. Đợi rename hoàn tất
      await delay(100);
    },
    [onAddModel, onModelNameUpdate, allNodes]
  );

  /**
   * Thêm attribute mới với tên, type và PK flag
   */
  const handleCreateAttribute = useCallback(
    async (
      modelId: string,
      attributeName: string,
      dataType: string,
      isPrimaryKey: boolean = false
    ): Promise<void> => {
      if (
        !onAddAttribute ||
        !onFieldNameUpdate ||
        !onFieldTypeUpdate ||
        !onToggleKeyType
      ) {
        throw new Error("Required handlers not provided");
      }
      console.log("bucvc: ", attributeName);

      // 1. Tạo attribute mới
      const id = await onAddAttribute(modelId);
      await delay(100);

      console.log("bucvc: ", id);

      // 4. Update name
      await onFieldNameUpdate(id, attributeName);
      await delay(100);

      console.log("bucvc: ", attributeName);

      // 5. Update type
      await onFieldTypeUpdate(id, dataType);
      await delay(100);

      // 6. Set primary key nếu cần
      if (isPrimaryKey) {
        await onToggleKeyType(modelId, id, "PRIMARY");
        await delay(100);
      }
    },
    [
      onAddAttribute,
      onFieldNameUpdate,
      onFieldTypeUpdate,
      onToggleKeyType,
      allNodes,
    ]
  );

  /**
   * Tạo foreign key connection
   */
  const handleCreateForeignKey = useCallback(
    async (
      sourceModelId: string,
      sourceColumnName: string,
      targetModelName: string,
      targetColumnName: string
    ): Promise<void> => {
      if (!onForeignKeyConnect || !onToggleKeyType) {
        throw new Error("Required handlers not provided");
      }

      const nodes = getNodeInternals();

      // 1. Tìm source attribute ID
      const sourceAttributeId = findAttributeIdByName(
        nodes,
        sourceModelId,
        sourceColumnName
      );
      if (!sourceAttributeId) {
        throw new Error(
          `Không tìm thấy attribute ${sourceColumnName} trong model ${sourceModelId}`
        );
      }

      // 2. Tìm target model ID
      const targetModelId = findModelIdByName(nodes, targetModelName);
      if (!targetModelId) {
        throw new Error(`Không tìm thấy model ${targetModelName}`);
      }

      // 3. Tìm target attribute ID
      const targetAttributeId = findAttributeIdByName(
        nodes,
        targetModelId,
        targetColumnName
      );
      if (!targetAttributeId) {
        throw new Error(
          `Không tìm thấy attribute ${targetColumnName} trong model ${targetModelName}`
        );
      }

      // 4. Set source attribute as foreign key
      await onToggleKeyType(sourceModelId, sourceAttributeId, "FOREIGN");
      await delay(100);

      // 5. Create connection
      await onForeignKeyConnect(
        sourceAttributeId,
        targetModelId,
        targetAttributeId
      );
      await delay(100);
    },
    [onForeignKeyConnect, onToggleKeyType, allNodes]
  );

  /**
   * Xóa model
   */
  const handleDeleteModel = useCallback(
    async (modelId: string): Promise<void> => {
      if (!onDeleteModel) {
        throw new Error("Delete model handler not provided");
      }

      await onDeleteModel(modelId);
      await delay(100);
    },
    [onDeleteModel]
  );

  /**
   * Xóa attribute
   */
  const handleDeleteAttribute = useCallback(
    async (modelId: string, attributeId: string): Promise<void> => {
      if (!onDeleteAttribute) {
        throw new Error("Delete attribute handler not provided");
      }

      await onDeleteAttribute(modelId, attributeId);
      await delay(100);
    },
    [onDeleteAttribute]
  );

  return {
    handleCreateModel,
    handleCreateAttribute,
    handleCreateForeignKey,
    handleDeleteModel,
    handleDeleteAttribute,
  };
};
