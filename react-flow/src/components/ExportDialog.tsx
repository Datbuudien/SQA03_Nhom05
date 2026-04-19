// src/components/ExportDialog.tsx - WITH LIGHT MODE SUPPORT
import React, { useState } from "react";
import {
  Modal,
  ModalOverlay,
  ModalContent,
  ModalHeader,
  ModalBody,
  ModalFooter,
  ModalCloseButton,
  Button,
  VStack,
  HStack,
  Radio,
  RadioGroup,
  Text,
  Box,
  Select,
  useToast,
  Divider,
  Badge,
  useColorModeValue,
} from "@chakra-ui/react";
import { FileJson, Database, Image as ImageIcon, Download } from "lucide-react";
import { toPng } from "html-to-image";
import {
  Panel,
  useReactFlow,
  getNodesBounds,
  getViewportForBounds,
} from "reactflow";

interface ExportDialogProps {
  isOpen: boolean;
  onClose: () => void;
  schemaData: any;
}

type ExportType = "json" | "sql" | "image";
type DatabaseType = "mysql" | "postgresql" | "sqlite" | "sqlserver" | "oracle";

export const ExportDialog: React.FC<ExportDialogProps> = ({
  isOpen,
  onClose,
  schemaData,
}) => {
  const [exportType, setExportType] = useState<ExportType>("json");
  const [databaseType, setDatabaseType] = useState<DatabaseType>("mysql");
  const [isExporting, setIsExporting] = useState(false);
  const toast = useToast();
  const { getNodes } = useReactFlow();

  // 🌟 THEME COLORS
  const bgColor = useColorModeValue("white", "gray.900");
  const textColor = useColorModeValue("#24292f", "white");
  const mutedText = useColorModeValue("#57606a", "gray.400");
  const borderColor = useColorModeValue("#d0d7de", "gray.700");
  const hoverBorderColor = useColorModeValue("#0969da", "blue.400");
  const selectedBorderColor = useColorModeValue("#0969da", "blue.500");
  const boxBg = useColorModeValue("#f6f8fa", "gray.800");
  const selectBg = useColorModeValue("white", "gray.800");
  const selectBorderColor = useColorModeValue("#d0d7de", "gray.600");
  const dividerColor = useColorModeValue("#d0d7de", "gray.700");

  // Icon colors
  const jsonIconColor = useColorModeValue("#0969da", "blue.400");
  const sqlIconColor = useColorModeValue("#1a7f37", "green.400");
  const imageIconColor = useColorModeValue("#8250df", "purple.400");

  // ============= JSON EXPORT =============
  const exportAsJSON = () => {
    try {
      const nodes = getNodes();
      const exportData = {
        metadata: {
          exportedAt: new Date().toISOString(),
          version: "1.0.0",
        },
        schema: {
          name: schemaData?.name || "Untitled Diagram",
          description: schemaData?.description || "",
        },
        tables: nodes.map((node) => ({
          id: node.id,
          name: node.data.name,
          position: node.position,
          attributes: node.data.attributes.map((attr: any) => ({
            id: attr.id,
            name: attr.name,
            dataType: attr.dataType,
            isNullable: attr.isNullable,
            isPrimaryKey: attr.isPrimaryKey,
            isForeignKey: attr.isForeignKey,
            isUnique: attr.isUnique,
            isAutoIncrement: attr.isAutoIncrement,
            defaultValue: attr.defaultValue,
            comment: attr.comment,
            connection: attr.connection,
          })),
        })),
      };

      const jsonString = JSON.stringify(exportData, null, 2);
      const blob = new Blob([jsonString], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${schemaData?.name || "diagram"}_${
        new Date().toISOString().split("T")[0]
      }.json`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast({
        title: "JSON Exported",
        description: "Diagram exported successfully as JSON",
        status: "success",
        duration: 3000,
      });
    } catch (error) {
      console.error("Error exporting JSON:", error);
      toast({
        title: "Export Failed",
        description: "Failed to export as JSON",
        status: "error",
        duration: 3000,
      });
    }
  };

  // ============= SQL EXPORT =============
  const generateSQL = (dbType: DatabaseType): string => {
    const nodes = getNodes();
    let sql = "";

    sql += `-- Database Schema Export\n`;
    sql += `-- Generated: ${new Date().toISOString()}\n`;
    sql += `-- Database Type: ${dbType.toUpperCase()}\n`;
    sql += `-- Schema: ${schemaData?.name || "Untitled"}\n\n`;

    switch (dbType) {
      case "mysql":
        sql += `SET FOREIGN_KEY_CHECKS = 0;\n\n`;
        break;
      case "postgresql":
        sql += `-- PostgreSQL Schema\n\n`;
        break;
      case "sqlite":
        sql += `PRAGMA foreign_keys = OFF;\n\n`;
        break;
    }

    nodes.forEach((node) => {
      const tableName = node.data.name;
      const attributes = node.data.attributes || [];

      sql += `-- Table: ${tableName}\n`;
      sql += `CREATE TABLE ${quoteIdentifier(tableName, dbType)} (\n`;

      const columnDefinitions: string[] = [];
      const primaryKeys: string[] = [];
      const foreignKeys: string[] = [];

      attributes.forEach((attr: any) => {
        let columnDef = `  ${quoteIdentifier(attr.name, dbType)} `;

        columnDef += convertDataType(attr.dataType, dbType);

        if (!attr.isNullable) {
          columnDef += " NOT NULL";
        }

        if (attr.isAutoIncrement) {
          columnDef += getAutoIncrementSyntax(dbType);
        }

        if (attr.defaultValue) {
          columnDef += ` DEFAULT ${attr.defaultValue}`;
        }

        if (attr.isUnique && !attr.isPrimaryKey) {
          columnDef += " UNIQUE";
        }

        if (attr.comment && (dbType === "mysql" || dbType === "postgresql")) {
          columnDef += ` COMMENT '${attr.comment.replace(/'/g, "''")}'`;
        }

        columnDefinitions.push(columnDef);

        if (attr.isPrimaryKey) {
          primaryKeys.push(quoteIdentifier(attr.name, dbType));
        }

        if (attr.connection) {
          foreignKeys.push({
            column: attr.name,
            refTable: attr.connection.targetModelId,
            refColumn: attr.connection.targetAttributeId,
            name:
              attr.connection.foreignKeyName || `fk_${tableName}_${attr.name}`,
          } as any);
        }
      });

      if (primaryKeys.length > 0) {
        columnDefinitions.push(`  PRIMARY KEY (${primaryKeys.join(", ")})`);
      }

      foreignKeys.forEach((fk: any) => {
        const fkDef = `  CONSTRAINT ${quoteIdentifier(
          fk.name,
          dbType
        )} FOREIGN KEY (${quoteIdentifier(
          fk.column,
          dbType
        )}) REFERENCES ${quoteIdentifier(
          fk.refTable,
          dbType
        )}(${quoteIdentifier(fk.refColumn, dbType)})`;
        columnDefinitions.push(fkDef);
      });

      sql += columnDefinitions.join(",\n");
      sql += `\n)`;

      if (dbType === "mysql") {
        sql += ` ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`;
      }

      sql += `;\n\n`;
    });

    switch (dbType) {
      case "mysql":
        sql += `SET FOREIGN_KEY_CHECKS = 1;\n`;
        break;
      case "sqlite":
        sql += `PRAGMA foreign_keys = ON;\n`;
        break;
    }

    return sql;
  };

  const quoteIdentifier = (name: string, dbType: DatabaseType): string => {
    switch (dbType) {
      case "mysql":
        return `\`${name}\``;
      case "postgresql":
        return `"${name}"`;
      case "sqlserver":
        return `[${name}]`;
      default:
        return name;
    }
  };

  const convertDataType = (dataType: string, dbType: DatabaseType): string => {
    const type = dataType.toUpperCase();

    const typeMap: Record<DatabaseType, Record<string, string>> = {
      mysql: {
        VARCHAR: "VARCHAR",
        INT: "INT",
        INTEGER: "INT",
        BIGINT: "BIGINT",
        TEXT: "TEXT",
        DATE: "DATE",
        DATETIME: "DATETIME",
        TIMESTAMP: "TIMESTAMP",
        BOOLEAN: "TINYINT(1)",
        DECIMAL: "DECIMAL",
        FLOAT: "FLOAT",
      },
      postgresql: {
        VARCHAR: "VARCHAR",
        INT: "INTEGER",
        INTEGER: "INTEGER",
        BIGINT: "BIGINT",
        TEXT: "TEXT",
        DATE: "DATE",
        DATETIME: "TIMESTAMP",
        TIMESTAMP: "TIMESTAMP",
        BOOLEAN: "BOOLEAN",
        DECIMAL: "DECIMAL",
        FLOAT: "REAL",
      },
      sqlite: {
        VARCHAR: "TEXT",
        INT: "INTEGER",
        INTEGER: "INTEGER",
        BIGINT: "INTEGER",
        TEXT: "TEXT",
        DATE: "TEXT",
        DATETIME: "TEXT",
        TIMESTAMP: "TEXT",
        BOOLEAN: "INTEGER",
        DECIMAL: "REAL",
        FLOAT: "REAL",
      },
      sqlserver: {
        VARCHAR: "VARCHAR",
        INT: "INT",
        INTEGER: "INT",
        BIGINT: "BIGINT",
        TEXT: "TEXT",
        DATE: "DATE",
        DATETIME: "DATETIME2",
        TIMESTAMP: "DATETIME2",
        BOOLEAN: "BIT",
        DECIMAL: "DECIMAL",
        FLOAT: "FLOAT",
      },
      oracle: {
        VARCHAR: "VARCHAR2",
        INT: "NUMBER(10)",
        INTEGER: "NUMBER(10)",
        BIGINT: "NUMBER(19)",
        TEXT: "CLOB",
        DATE: "DATE",
        DATETIME: "TIMESTAMP",
        TIMESTAMP: "TIMESTAMP",
        BOOLEAN: "NUMBER(1)",
        DECIMAL: "NUMBER",
        FLOAT: "FLOAT",
      },
    };

    const baseType = type.split("(")[0].trim();
    const mapped = typeMap[dbType][baseType];

    if (mapped) {
      if (type.includes("(")) {
        const params = type.substring(type.indexOf("("));
        return mapped + params;
      }
      return mapped;
    }

    return dataType;
  };

  const getAutoIncrementSyntax = (dbType: DatabaseType): string => {
    switch (dbType) {
      case "mysql":
        return " AUTO_INCREMENT";
      case "postgresql":
        return " GENERATED ALWAYS AS IDENTITY";
      case "sqlserver":
        return " IDENTITY(1,1)";
      case "sqlite":
        return " AUTOINCREMENT";
      default:
        return "";
    }
  };

  const exportAsSQL = () => {
    try {
      setIsExporting(true);
      const sql = generateSQL(databaseType);

      const blob = new Blob([sql], { type: "text/plain" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${schemaData?.name || "diagram"}_${databaseType}_${
        new Date().toISOString().split("T")[0]
      }.sql`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);

      toast({
        title: "SQL Exported",
        description: `Schema exported successfully as ${databaseType.toUpperCase()} SQL`,
        status: "success",
        duration: 3000,
      });
    } catch (error) {
      console.error("Error exporting SQL:", error);
      toast({
        title: "Export Failed",
        description: "Failed to export as SQL",
        status: "error",
        duration: 3000,
      });
    } finally {
      setIsExporting(false);
    }
  };

  // ============= IMAGE EXPORT =============
  function downloadImage(dataUrl: string) {
    const a = document.createElement("a");
    a.setAttribute("download", "reactflow.png");
    a.setAttribute("href", dataUrl);
    a.click();
  }

  const imageWidth = 1920 * 2;
  const imageHeight = 1080 * 2;

  const exportAsImage = async () => {
    const nodesBounds = getNodesBounds(getNodes());
    const viewport = getViewportForBounds(
      nodesBounds,
      imageWidth,
      imageHeight,
      0.5,
      2
    );
    const view = document.querySelector(".react-flow__viewport") as HTMLElement;
    if (!view) return;
    toPng(view, {
      backgroundColor: "transparent",
      width: imageWidth,
      height: imageHeight,
      style: {
        width: `${imageWidth}px`,
        height: `${imageHeight}px`,
        transform: `translate(${viewport.x}px, ${viewport.y}px) scale(${viewport.zoom})`,
      },
    }).then(downloadImage);
  };

  const handleExport = () => {
    switch (exportType) {
      case "json":
        exportAsJSON();
        break;
      case "sql":
        exportAsSQL();
        break;
      case "image":
        exportAsImage();
        break;
    }
    onClose();
  };

  const getExportIcon = (type: ExportType) => {
    switch (type) {
      case "json":
        return <FileJson size={20} />;
      case "sql":
        return <Database size={20} />;
      case "image":
        return <ImageIcon size={20} />;
    }
  };

  const getExportDescription = (type: ExportType) => {
    switch (type) {
      case "json":
        return "Export complete diagram data as JSON format";
      case "sql":
        return "Generate SQL CREATE statements for your database";
      case "image":
        return "Export diagram as PNG image (1920x1080)";
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg" isCentered>
      <ModalOverlay />
      <ModalContent
        border={"1px solid"}
        borderColor={borderColor}
        bg={bgColor}
        color={textColor}
      >
        <ModalHeader pb={0}>Export Diagram</ModalHeader>
        <ModalCloseButton />

        <ModalBody>
          <VStack spacing={4} align="stretch">
            <Text fontSize="sm" color={mutedText}>
              Choose export format for your diagram
            </Text>

            <RadioGroup
              value={exportType}
              onChange={(v) => setExportType(v as ExportType)}
            >
              <VStack spacing={3} align="stretch">
                {/* JSON Option */}
                <Box
                  p={4}
                  border="2px solid"
                  borderColor={
                    exportType === "json" ? selectedBorderColor : borderColor
                  }
                  borderRadius="md"
                  cursor="pointer"
                  _hover={{ borderColor: hoverBorderColor }}
                  onClick={() => setExportType("json")}
                  bg={exportType === "json" ? boxBg : "transparent"}
                >
                  <HStack justify="space-between">
                    <HStack spacing={3}>
                      <Box color={jsonIconColor}>{getExportIcon("json")}</Box>
                      <VStack align="start" spacing={0}>
                        <HStack>
                          <Text fontWeight="bold">JSON Format</Text>
                          <Badge colorScheme="blue" fontSize="xs">
                            .json
                          </Badge>
                        </HStack>
                        <Text fontSize="xs" color={mutedText}>
                          {getExportDescription("json")}
                        </Text>
                      </VStack>
                    </HStack>
                    <Radio value="json" />
                  </HStack>
                </Box>
                {/* SQL Option
                <Box
                  p={4}
                  border="2px solid"
                  borderColor={
                    exportType === "sql" ? selectedBorderColor : borderColor
                  }
                  borderRadius="md"
                  cursor="pointer"
                  _hover={{ borderColor: hoverBorderColor }}
                  onClick={() => setExportType("sql")}
                  bg={exportType === "sql" ? boxBg : "transparent"}
                >
                  <HStack
                    justify="space-between"
                    mb={exportType === "sql" ? 3 : 0}
                  >
                    <HStack spacing={3}>
                      <Box color={sqlIconColor}>{getExportIcon("sql")}</Box>
                      <VStack align="start" spacing={0}>
                        <HStack>
                          <Text fontWeight="bold">SQL Script</Text>
                          <Badge colorScheme="green" fontSize="xs">
                            .sql
                          </Badge>
                        </HStack>
                        <Text fontSize="xs" color={mutedText}>
                          {getExportDescription("sql")}
                        </Text>
                      </VStack>
                    </HStack>
                    <Radio value="sql" />
                  </HStack>

                  {exportType === "sql" && (
                    <>
                      <Divider my={2} borderColor={dividerColor} />
                      <VStack align="start" spacing={2}>
                        <Text fontSize="sm" fontWeight="medium">
                          Select Database Type:
                        </Text>
                        <Select
                          value={databaseType}
                          onChange={(e) =>
                            setDatabaseType(e.target.value as DatabaseType)
                          }
                          size="sm"
                          bg={selectBg}
                          borderColor={selectBorderColor}
                        >
                          <option value="mysql">MySQL / MariaDB</option>
                          <option value="postgresql">PostgreSQL</option>
                          <option value="sqlite">SQLite</option>
                          <option value="sqlserver">SQL Server</option>
                          <option value="oracle">Oracle</option>
                        </Select>
                      </VStack>
                    </>
                  )}
                </Box> */}
                {/* Image Option */}
                <Box
                  p={4}
                  border="2px solid"
                  borderColor={
                    exportType === "image" ? imageIconColor : borderColor
                  }
                  borderRadius="md"
                  cursor="pointer"
                  _hover={{ borderColor: imageIconColor }}
                  onClick={() => setExportType("image")}
                  bg={exportType === "image" ? boxBg : "transparent"}
                >
                  <HStack justify="space-between">
                    <HStack spacing={3}>
                      <Box color={imageIconColor}>{getExportIcon("image")}</Box>
                      <VStack align="start" spacing={0}>
                        <HStack>
                          <Text fontWeight="bold">PNG Image</Text>
                          <Badge colorScheme="purple" fontSize="xs">
                            .png
                          </Badge>
                        </HStack>
                        <Text fontSize="xs" color={mutedText}>
                          {getExportDescription("image")}
                        </Text>
                      </VStack>
                    </HStack>
                    <Radio value="image" colorScheme="purple" />
                  </HStack>
                </Box>
              </VStack>
            </RadioGroup>
          </VStack>
        </ModalBody>

        <ModalFooter>
          <HStack spacing={3}>
            <Button variant="ghost" onClick={onClose}>
              Cancel
            </Button>
            <Button
              colorScheme="blue"
              leftIcon={<Download size={16} />}
              onClick={handleExport}
              isLoading={isExporting}
              loadingText="Exporting..."
            >
              Export
            </Button>
          </HStack>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};
