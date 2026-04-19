// src/components/chat/DeleteActionsDisplay.tsx
import {
  Box,
  Text,
  VStack,
  HStack,
  Code,
  IconButton,
  Tooltip,
  Badge,
  useColorModeValue,
  useToast,
  Button,
} from "@chakra-ui/react";
import { FC, useState, useCallback } from "react";
import { delay, findModelIdByName } from "../../utils/nodeHelpers";
import { FaMinus, FaLink, FaCheck } from "react-icons/fa";
import { usePermission } from "../../hooks/usePermission";
import { ReactFlowState, useStore } from "reactflow";

// Define the delete action structure from API
interface DeleteAction {
  name: string;
  drop_table: boolean;
  attrs_to_delete: string[];
  fks_to_delete: string[];
}

interface DeleteActionsDisplayProps {
  deleteActions: DeleteAction[];
  // allNodes: Map<string, any>;
  onDeleteModel?: (modelId: string) => Promise<void>;
  onDeleteAttribute?: (modelId: string, attributeId: string) => Promise<void>;
}

export const DeleteActionsDisplay: FC<DeleteActionsDisplayProps> = ({
  deleteActions,
  // allNodes,
  onDeleteModel,
  onDeleteAttribute,
}) => {
  const allNodes = useStore((state: ReactFlowState) => state.nodeInternals);
  const { canEdit } = usePermission();
  const deleteBg = useColorModeValue("red.50", "red.900");
  const itemBg = useColorModeValue("white", "gray.700");
  const toast = useToast();
  const [processingModels, setProcessingModels] = useState<Set<string>>(
    new Set()
  );
  const [processingAttrs, setProcessingAttrs] = useState<Set<string>>(
    new Set()
  );
  const [isAcceptingAll, setIsAcceptingAll] = useState(false);
  const [completedItems, setCompletedItems] = useState<Set<string>>(new Set());

  // Handler xóa model
  const handleDeleteModel = useCallback(
    async (modelName: string) => {
      if (!onDeleteModel) {
        toast({
          title: "Lỗi",
          description: "Chức năng xóa model chưa được kích hoạt",
          status: "error",
          duration: 2000,
        });
        return;
      }

      const modelId = findModelIdByName(allNodes, modelName);
      if (!modelId) {
        toast({
          title: "Lỗi",
          description: `Không tìm thấy bảng ${modelName}`,
          status: "error",
          duration: 2000,
        });
        return;
      }

      setProcessingModels((prev) => new Set(prev).add(modelName));

      try {
        await onDeleteModel(modelId);

        toast({
          title: "Thành công",
          description: `Đã xóa bảng ${modelName}`,
          status: "success",
          duration: 2000,
        });
      } catch (error) {
        console.error("Error deleting model:", error);
        toast({
          title: "Lỗi",
          description: "Không thể xóa bảng",
          status: "error",
          duration: 2000,
        });
      } finally {
        setProcessingModels((prev) => {
          const next = new Set(prev);
          next.delete(modelName);
          return next;
        });
      }
    },
    [onDeleteModel, allNodes, toast]
  );

  // Handler xóa attribute
  const handleDeleteAttribute = useCallback(
    async (modelName: string, attributeName: string) => {
      if (!onDeleteAttribute) {
        toast({
          title: "Lỗi",
          description: "Chức năng xóa thuộc tính chưa được kích hoạt",
          status: "error",
          duration: 2000,
        });
        return;
      }

      const modelId = findModelIdByName(allNodes, modelName);
      if (!modelId) {
        toast({
          title: "Lỗi",
          description: `Không tìm thấy bảng ${modelName}`,
          status: "error",
          duration: 2000,
        });
        return;
      }

      // Find attribute ID by name
      const model = allNodes.get(modelId);
      const attribute = model?.data?.attributes?.find(
        (attr: any) => attr.name === attributeName
      );

      if (!attribute) {
        toast({
          title: "Lỗi",
          description: `Không tìm thấy thuộc tính ${attributeName} trong bảng ${modelName}`,
          status: "error",
          duration: 2000,
        });
        return;
      }

      const key = `${modelName}.${attributeName}`;
      setProcessingAttrs((prev) => new Set(prev).add(key));

      try {
        await onDeleteAttribute(modelId, attribute.id);

        toast({
          title: "Thành công",
          description: `Đã xóa thuộc tính ${attributeName} khỏi bảng ${modelName}`,
          status: "success",
          duration: 2000,
        });
      } catch (error) {
        console.error("Error deleting attribute:", error);
        toast({
          title: "Lỗi",
          description: "Không thể xóa thuộc tính",
          status: "error",
          duration: 2000,
        });
      } finally {
        setProcessingAttrs((prev) => {
          const next = new Set(prev);
          next.delete(key);
          return next;
        });
      }
    },
    [onDeleteAttribute, allNodes, toast]
  );

  const handleAcceptAll = useCallback(async () => {
    setIsAcceptingAll(true);

    try {
      for (const action of deleteActions) {
        // Delete model if drop_table
        if (action.drop_table && !completedItems.has(`model-${action.name}`)) {
          await handleDeleteModel(action.name);
          await delay(300);
        }

        // Delete attributes
        for (const attrName of action.attrs_to_delete || []) {
          const attrKey = `${action.name}.${attrName}`;
          if (!completedItems.has(`attr-${attrKey}`)) {
            try {
              await handleDeleteAttribute(action.name, attrName);
              await delay(300);
            } catch (error) {
              console.error(`Failed to delete attribute ${attrName}:`, error);
            }
          }
        }

        // Delete foreign keys
        for (const fkName of action.fks_to_delete || []) {
          const fkKey = `${action.name}.fk.${fkName}`;
          if (!completedItems.has(`fk-${fkKey}`)) {
            try {
              await handleDeleteAttribute(action.name, fkName);
              await delay(300);
            } catch (error) {
              console.error(`Failed to delete FK ${fkName}:`, error);
            }
          }
        }
      }

      toast({
        title: "Hoàn tất",
        description: "Đã xóa tất cả các mục",
        status: "success",
        duration: 3000,
      });
    } catch (error) {
      console.error("Error in Accept All:", error);
    } finally {
      setIsAcceptingAll(false);
    }
  }, [
    deleteActions,
    completedItems,
    handleDeleteModel,
    handleDeleteAttribute,
    toast,
  ]);

  if (deleteActions.length === 0) return null;

  return (
    <VStack align="stretch" spacing={2}>
      {deleteActions.map((action, idx) => {
        const isProcessingModel = processingModels.has(action.name);

        return (
          <Box
            key={idx}
            p={3}
            bg={deleteBg}
            borderRadius="md"
            borderLeft="3px solid"
            borderColor="red.500"
          >
            {/* Model Header */}
            <HStack justify="space-between" mb={2}>
              <HStack>
                <Text fontSize="sm" fontWeight="bold" color="red.600">
                  Table
                </Text>
                <Badge colorScheme="red">{action.name}</Badge>
              </HStack>
              {action.drop_table && canEdit && (
                <Tooltip label="Xóa bảng">
                  <IconButton
                    aria-label="Delete table"
                    icon={<FaMinus />}
                    size="xs"
                    colorScheme="red"
                    isLoading={isProcessingModel}
                    onClick={() => handleDeleteModel(action.name)}
                  />
                </Tooltip>
              )}
            </HStack>

            {/* Attributes to delete */}
            {action.attrs_to_delete.length > 0 && (
              <Box mt={2} pl={2}>
                <Text
                  fontSize="xs"
                  fontWeight="semibold"
                  color="gray.600"
                  mb={1}
                >
                  Attribute ({action.attrs_to_delete.length}):
                </Text>
                <VStack align="stretch" spacing={1}>
                  {action.attrs_to_delete.map((attrName, attrIdx) => {
                    const attrKey = `${action.name}.${attrName}`;
                    const isProcessing = processingAttrs.has(attrKey);

                    return (
                      <HStack
                        key={attrIdx}
                        fontSize="xs"
                        p={1.5}
                        bg={itemBg}
                        borderRadius="sm"
                        justify="space-between"
                      >
                        <HStack spacing={2}>
                          <Code fontSize="10px" colorScheme="red">
                            {attrName}
                          </Code>
                        </HStack>
                        {canEdit && (
                          <Tooltip label="Xóa thuộc tính">
                            <IconButton
                              aria-label="Delete attribute"
                              icon={<FaMinus />}
                              size="xs"
                              variant="ghost"
                              colorScheme="red"
                              isLoading={isProcessing}
                              onClick={() =>
                                handleDeleteAttribute(action.name, attrName)
                              }
                            />
                          </Tooltip>
                        )}
                      </HStack>
                    );
                  })}
                </VStack>
              </Box>
            )}

            {/* Foreign keys to delete */}
            {action.fks_to_delete.length > 0 && (
              <Box mt={2} pl={2}>
                <Text
                  fontSize="xs"
                  fontWeight="semibold"
                  color="gray.600"
                  mb={1}
                >
                  🔗 Foreign key ({action.fks_to_delete.length}):
                </Text>
                <VStack align="stretch" spacing={1}>
                  {action.fks_to_delete.map((fkName, fkIdx) => {
                    const fkKey = `${action.name}.fk.${fkName}`;
                    const isProcessing = processingAttrs.has(fkKey);

                    return (
                      <HStack
                        key={fkIdx}
                        fontSize="xs"
                        p={1.5}
                        bg={useColorModeValue("red.100", "red.800")}
                        borderRadius="sm"
                        justify="space-between"
                      >
                        <HStack spacing={1}>
                          <Code fontSize="10px" colorScheme="red">
                            {fkName}
                          </Code>
                        </HStack>
                        {canEdit && (
                          <Tooltip label="Xóa khóa ngoại">
                            <IconButton
                              aria-label="Delete foreign key"
                              icon={<FaMinus />}
                              size="xs"
                              variant="ghost"
                              colorScheme="red"
                              isLoading={isProcessing}
                              onClick={() =>
                                handleDeleteAttribute(action.name, fkName)
                              }
                            />
                          </Tooltip>
                        )}
                      </HStack>
                    );
                  })}
                </VStack>
              </Box>
            )}
          </Box>
        );
      })}
      {canEdit && (
        <Button
          size="sm"
          colorScheme="red"
          leftIcon={<FaCheck />}
          onClick={handleAcceptAll}
          isLoading={isAcceptingAll}
          loadingText="Đang xử lý..."
        >
          Accept All
        </Button>
      )}
    </VStack>
  );
};
