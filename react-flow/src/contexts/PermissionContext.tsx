// src/contexts/PermissionContext.tsx
import React, { createContext, useMemo, ReactNode } from "react";

type Permission = "VIEW" | "FULL_ACCESS" | null;

interface PermissionContextType {
  hasFullAccess: boolean;
  hasViewAccess: boolean;
  canEdit: boolean;
  canView: boolean;
  permission: Permission;
}

export const PermissionContext = createContext<PermissionContextType | null>(
  null
);

interface PermissionProviderProps {
  permission: Permission;
  children: ReactNode;
}

export const PermissionProvider: React.FC<PermissionProviderProps> = ({
  permission,
  children,
}) => {
  const hasFullAccess = useMemo(() => {
    return permission === "FULL_ACCESS";
  }, [permission]);

  const hasViewAccess = useMemo(() => {
    return permission === "VIEW";
  }, [permission]);

  const canEdit = useMemo(() => {
    return hasFullAccess;
  }, [hasFullAccess]);

  const canView = useMemo(() => {
    return hasFullAccess || hasViewAccess;
  }, [hasFullAccess, hasViewAccess]);

  const value = useMemo(
    () => ({
      hasFullAccess,
      hasViewAccess,
      canEdit,
      canView,
      permission,
    }),
    [hasFullAccess, hasViewAccess, canEdit, canView, permission]
  );

  return (
    <PermissionContext.Provider value={value}>
      {children}
    </PermissionContext.Provider>
  );
};
