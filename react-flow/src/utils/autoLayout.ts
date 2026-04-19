// src/utils/autoLayout.ts

interface LayoutNode {
  id: string;
  width: number;
  height: number;
  children: string[]; // IDs của các node con (được kết nối qua foreign key)
  parents: string[]; // IDs của các node cha
  level?: number; // Cấp độ trong cây (0 = root)
}

interface LayoutConfig {
  horizontalSpacing: number; // Khoảng cách giữa các cột
  verticalSpacing: number; // Khoảng cách giữa các node trong cùng cột
  startX: number; // Tọa độ X bắt đầu
  startY: number; // Tọa độ Y bắt đầu
}

const DEFAULT_CONFIG: LayoutConfig = {
  horizontalSpacing: 400,
  verticalSpacing: 50,
  startX: 100,
  startY: 100,
};

/**
 * Xây dựng graph từ danh sách nodes
 */
function buildGraph(nodes: Map<string, any>): Map<string, LayoutNode> {
  const graph = new Map<string, LayoutNode>();

  // Khởi tạo các node trong graph
  nodes.forEach((node, nodeId) => {
    graph.set(nodeId, {
      id: nodeId,
      width: node.width || 280,
      height: node.height || 200,
      children: [],
      parents: [],
    });
  });

  // Xây dựng quan hệ cha-con dựa trên foreign key
  nodes.forEach((node, nodeId) => {
    const attributes = node.data?.attributes || [];

    attributes.forEach((attr: any) => {
      if (attr.isForeignKey && attr.connection) {
        const targetNodeId = attr.connection.targetModelId;

        // Node hiện tại trỏ đến target => target là cha, current là con
        const currentNode = graph.get(nodeId);
        const targetNode = graph.get(targetNodeId);

        if (currentNode && targetNode) {
          // Thêm target vào parents của current
          if (!currentNode.parents.includes(targetNodeId)) {
            currentNode.parents.push(targetNodeId);
          }

          // Thêm current vào children của target
          if (!targetNode.children.includes(nodeId)) {
            targetNode.children.push(nodeId);
          }
        }
      }
    });
  });

  return graph;
}

/**
 * Tìm các root nodes (không có parent)
 */
function findRootNodes(graph: Map<string, LayoutNode>): string[] {
  const roots: string[] = [];

  graph.forEach((node, nodeId) => {
    if (node.parents.length === 0) {
      roots.push(nodeId);
    }
  });

  return roots;
}

/**
 * Gán level cho các nodes (BFS từ roots)
 */
function assignLevels(graph: Map<string, LayoutNode>, roots: string[]): void {
  const visited = new Set<string>();
  const queue: Array<{ nodeId: string; level: number }> = [];

  // Khởi tạo với các root nodes
  roots.forEach((rootId) => {
    queue.push({ nodeId: rootId, level: 0 });
  });

  while (queue.length > 0) {
    const { nodeId, level } = queue.shift()!;

    if (visited.has(nodeId)) continue;
    visited.add(nodeId);

    const node = graph.get(nodeId);
    if (!node) continue;

    // Gán level (lấy max nếu node có nhiều parents)
    node.level = Math.max(node.level || 0, level);

    // Thêm children vào queue
    node.children.forEach((childId) => {
      if (!visited.has(childId)) {
        queue.push({ nodeId: childId, level: level + 1 });
      }
    });
  }

  // Xử lý các node không được visit (isolated nodes)
  graph.forEach((node, nodeId) => {
    if (!visited.has(nodeId)) {
      node.level = 0;
    }
  });
}

/**
 * Nhóm các nodes theo level (cột)
 */
function groupByLevel(graph: Map<string, LayoutNode>): Map<number, string[]> {
  const levelGroups = new Map<number, string[]>();

  graph.forEach((node, nodeId) => {
    const level = node.level || 0;

    if (!levelGroups.has(level)) {
      levelGroups.set(level, []);
    }

    levelGroups.get(level)!.push(nodeId);
  });

  return levelGroups;
}

/**
 * Tính toán vị trí cho các nodes
 */
function calculatePositions(
  graph: Map<string, LayoutNode>,
  levelGroups: Map<number, string[]>,
  config: LayoutConfig = DEFAULT_CONFIG
): Map<string, { x: number; y: number }> {
  const positions = new Map<string, { x: number; y: number }>();

  // Sắp xếp các levels
  const sortedLevels = Array.from(levelGroups.keys()).sort((a, b) => a - b);

  sortedLevels.forEach((level) => {
    const nodesInLevel = levelGroups.get(level) || [];

    // Tính toán X dựa trên level (cột)
    const x = config.startX + level * config.horizontalSpacing;

    // Tính tổng chiều cao của tất cả nodes trong level này
    const totalHeight = nodesInLevel.reduce((sum, nodeId) => {
      const node = graph.get(nodeId);
      return sum + (node?.height || 200) + config.verticalSpacing;
    }, -config.verticalSpacing); // Trừ đi spacing cuối cùng

    // Bắt đầu từ vị trí căn giữa
    let currentY = config.startY - totalHeight / 2;

    // Sắp xếp nodes trong level theo tên để có thứ tự nhất quán
    nodesInLevel.sort((a, b) => {
      const nodeA = graph.get(a);
      const nodeB = graph.get(b);
      return (nodeA?.id || "").localeCompare(nodeB?.id || "");
    });

    nodesInLevel.forEach((nodeId) => {
      const node = graph.get(nodeId);
      if (!node) return;

      positions.set(nodeId, {
        x: x,
        y: currentY,
      });

      currentY += node.height + config.verticalSpacing;
    });
  });

  return positions;
}

/**
 * Hàm chính để tính toán layout tự động
 */
export function calculateAutoLayout(
  nodes: Map<string, any>,
  config: Partial<LayoutConfig> = {}
): Map<string, { x: number; y: number }> {
  const finalConfig = { ...DEFAULT_CONFIG, ...config };

  console.log("🎯 Starting auto layout calculation...");
  console.log("📊 Total nodes:", nodes.size);

  // 1. Xây dựng graph
  const graph = buildGraph(nodes);
  console.log("🔗 Graph built");

  // 2. Tìm root nodes
  const roots = findRootNodes(graph);
  console.log("🌳 Root nodes found:", roots.length);

  // 3. Gán levels
  assignLevels(graph, roots);
  console.log("📏 Levels assigned");

  // 4. Nhóm theo level
  const levelGroups = groupByLevel(graph);
  console.log("📦 Grouped into", levelGroups.size, "levels");

  // 5. Tính toán vị trí
  const positions = calculatePositions(graph, levelGroups, finalConfig);
  console.log("✅ Positions calculated for", positions.size, "nodes");

  return positions;
}
