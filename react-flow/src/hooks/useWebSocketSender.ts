// src/hooks/useWebSocketSender.ts
import { useCallback } from "react";
import { websocketService } from "../services/websocketService";
import { useWebSocketContext } from "../contexts/WebSocketContext";
import {
  NodePositionUpdate,
  FieldNameUpdate,
  FieldTypeUpdate,
  ToggleKeyTypeUpdate,
  AddAttributeUpdate,
  DeleteAttributeUpdate,
  ForeignKeyConnectionUpdate,
  ForeignKeyDisconnectUpdate,
  AddModelUpdate,
  UpdateModelNameUpdate,
  DeleteModelUpdate,
  UpdateDiagramNameUpdate,
} from "../types/websocket.types";

/**
 * Hook để gửi WebSocket messages
 * Sử dụng global state để check connection
 */
export const useWebSocketSender = () => {
  // const { isConnected } = useWebSocketContext();

  const sendNodePositionUpdate = useCallback((update: NodePositionUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send node position update: not connected");
    //   return;
    // }
    websocketService.sendNodePositionUpdate(update);
  }, []);

  const sendFieldNameUpdate = useCallback(async (update: FieldNameUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send field name update: not connected");
    //   return;
    // }
    websocketService.sendFieldNameUpdate(update);
  }, []);

  const sendFieldTypeUpdate = useCallback(async (update: FieldTypeUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send field type update: not connected");
    //   return;
    // }
    websocketService.sendFieldTypeUpdate(update);
  }, []);

  const sendToggleKeyType = useCallback(async (update: ToggleKeyTypeUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send toggle key type: not connected");
    //   return;
    // }
    websocketService.sendToggleKeyType(update);
  }, []);

  const sendAddAttribute = useCallback(async (update: AddAttributeUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send add attribute: not connected");
    //   return;
    // }
    websocketService.sendAddAttribute(update);
  }, []);

  const sendDeleteAttribute = useCallback(
    async (update: DeleteAttributeUpdate) => {
      // if (!isConnected) {
      //   console.warn("⚠️ Cannot send delete attribute: not connected");
      //   return;
      // }
      websocketService.sendDeleteAttribute(update);
    },
    []
  );

  const sendForeignKeyConnect = useCallback(
    async (update: ForeignKeyConnectionUpdate) => {
      // if (!isConnected) {
      //   console.warn("⚠️ Cannot send foreign key connect: not connected");
      //   return;
      // }
      websocketService.sendForeignKeyConnect(update);
    },
    []
  );

  const sendForeignKeyDisconnect = useCallback(
    async (update: ForeignKeyDisconnectUpdate) => {
      // if (!isConnected) {
      //   console.warn("⚠️ Cannot send foreign key disconnect: not connected");
      //   return;
      // }
      websocketService.sendForeignKeyDisconnect(update);
    },
    []
  );

  const sendAddModel = useCallback(async (update: AddModelUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send add model: not connected");
    //   return;
    // }
    websocketService.sendAddModel(update);
  }, []);

  const sendUpdateModelName = useCallback(
    async (update: UpdateModelNameUpdate) => {
      // if (!isConnected) {
      //   console.warn("⚠️ Cannot send update model name: not connected");
      //   return;
      // }
      websocketService.sendUpdateModelName(update);
    },
    []
  );

  const sendDeleteModel = useCallback(async (update: DeleteModelUpdate) => {
    // if (!isConnected) {
    //   console.warn("⚠️ Cannot send delete model: not connected");
    //   return;
    // }
    websocketService.sendDeleteModel(update);
  }, []);

  const sendUpdateDiagramName = useCallback(
    async (update: UpdateDiagramNameUpdate) => {
      // if (!isConnected) {
      //   console.warn("⚠️ Cannot send update diagram name: not connected");
      //   return;
      // }
      websocketService.sendUpdateDiagramName(update);
    },
    []
  );

  return {
    // isConnected,
    sendNodePositionUpdate,
    sendFieldNameUpdate,
    sendFieldTypeUpdate,
    sendToggleKeyType,
    sendAddAttribute,
    sendDeleteAttribute,
    sendForeignKeyConnect,
    sendForeignKeyDisconnect,
    sendAddModel,
    sendUpdateModelName,
    sendDeleteModel,
    sendUpdateDiagramName,
  };
};
