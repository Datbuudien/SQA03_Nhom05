import { useState, useEffect } from "react";
import {
  Box,
  Grid,
  GridItem,
  Heading,
  Text,
  VStack,
  HStack,
  Card,
  CardBody,
  useColorModeValue,
  Stat,
  StatLabel,
  StatNumber,
  StatHelpText,
  StatArrow,
  Spinner,
  Flex,
  Icon,
  SimpleGrid,
  Select,
  Table,
  Thead,
  Tbody,
  Tr,
  Th,
  Td,
  Badge,
  Tooltip as ChakraTooltip,
  Button,
} from "@chakra-ui/react";
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Area,
  AreaChart,
} from "recharts";
import {
  Users,
  Database,
  MessageSquare,
  Clock,
  TrendingUp,
  Activity,
  CheckCircle2,
  XCircle,
  Zap,
} from "lucide-react";

interface Statistics {
  totalAccounts: number;
  totalDiagrams: number;
  totalInputTokens: number;
  totalOutputTokens: number;
  totalTokens: number;
  avgTotalTime: number;
  successfulRequests: number;
  totalRequests: number;
  successRate: number;
}

interface MetricRecord {
  id: number;
  created_at: string;
  input_tokens: number;
  output_tokens: number;
  total_tokens: number;
  total_time_ms: number;
  status: string;
  input_preview: string;
  output_preview: string;
}

export function StatisticsPage() {
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [recentMetrics, setRecentMetrics] = useState<MetricRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState("all");

  const bgColor = useColorModeValue("#faf9f9ff", "#0d1117");
  const cardBg = useColorModeValue("white", "#161b22");
  const borderColor = useColorModeValue("#d0d7de", "#30363d");
  const textColor = useColorModeValue("#24292f", "#e6edf3");
  const mutedText = useColorModeValue("#57606a", "#8b949e");
  const accentColor = useColorModeValue("#0969da", "#58a6ff");

  useEffect(() => {
    fetchStatistics();
  }, [timeRange]);

  const fetchStatistics = async () => {
    setLoading(true);
    try {
      // Fetch account statistics
      const accountResponse = await fetch(
        "http://localhost:8080/account/search?size=1",
        {
          credentials: "include",
        }
      );
      const accountData = await accountResponse.json();

      // Fetch diagram statistics
      const diagramResponse = await fetch(
        "http://localhost:8080/api/diagrams/all?pageSize=1",
        {
          credentials: "include",
        }
      );
      const diagramData = await diagramResponse.json();

      // Fetch chat metrics
      const metricsResponse = await fetch(
        "http://localhost:8080/metrics?limit=100",
        {
          credentials: "include",
        }
      );
      const metricsData = await metricsResponse.json();

      const stats = metricsData.statistics;
      const totalTokens = parseInt(stats?.total_tokens_processed || "0");
      const avgInputTokens = parseFloat(stats?.avg_input_tokens || "0");
      const avgOutputTokens = parseFloat(stats?.avg_output_tokens || "0");

      // Calculate input/output ratio
      const inputRatio =
        avgInputTokens / (avgInputTokens + avgOutputTokens) || 0.4;
      const outputRatio =
        avgOutputTokens / (avgInputTokens + avgOutputTokens) || 0.6;

      setStatistics({
        totalAccounts: accountData.totalElements || 0,
        totalDiagrams: diagramData.totalCount || 0,
        totalInputTokens: Math.floor(totalTokens * inputRatio),
        totalOutputTokens: Math.floor(totalTokens * outputRatio),
        totalTokens: totalTokens,
        avgTotalTime: parseFloat(stats?.avg_total_time_ms || "0"),
        successfulRequests: parseInt(stats?.successful_requests || "0"),
        totalRequests: stats?.total_requests || 0,
        successRate: stats?.total_requests
          ? (parseInt(stats.successful_requests) / stats.total_requests) * 100
          : 0,
      });

      // Set recent metrics for table
      setRecentMetrics(metricsData.metrics || []);
    } catch (error) {
      console.error("Error fetching statistics:", error);
    } finally {
      setLoading(false);
    }
  };

  // Prepare chart data from recent metrics
  const getChartData = () => {
    if (!recentMetrics.length) return [];

    // Group by date and aggregate
    const grouped: {
      [key: string]: {
        requests: number;
        totalTime: number;
        tokens: number;
        count: number;
      };
    } = {};

    recentMetrics.forEach((metric) => {
      const date = new Date(metric.created_at);
      const dateKey = date.toLocaleDateString("en-US", { weekday: "short" });

      if (!grouped[dateKey]) {
        grouped[dateKey] = { requests: 0, totalTime: 0, tokens: 0, count: 0 };
      }

      grouped[dateKey].requests += 1;
      grouped[dateKey].totalTime += metric.total_time_ms;
      grouped[dateKey].tokens += metric.total_tokens;
      grouped[dateKey].count += 1;
    });

    return Object.entries(grouped).map(([date, data]) => ({
      date,
      requests: data.requests,
      avgTime: data.count > 0 ? data.totalTime / data.count : 0,
      tokens: data.tokens,
    }));
  };

  const timeSeriesData = getChartData();

  const tokenDistributionData = [
    { name: "Input Tokens", value: statistics?.totalInputTokens || 0 },
    { name: "Output Tokens", value: statistics?.totalOutputTokens || 0 },
  ];

  const requestStatusData = [
    { name: "Success", value: statistics?.successfulRequests || 0 },
    {
      name: "Failed",
      value:
        (statistics?.totalRequests || 0) -
        (statistics?.successfulRequests || 0),
    },
  ];

  const COLORS = ["#0969da", "#1f883d", "#d1242f", "#9a6700"];

  const StatCard = ({
    icon,
    label,
    value,
    unit,
    subValue,
    color,
  }: {
    icon: any;
    label: string;
    value: number | string;
    unit?: string;
    subValue?: string;
    color?: string;
  }) => (
    <Card bg={cardBg} border="1px" borderColor={borderColor} shadow="sm">
      <CardBody>
        <HStack justify="space-between" mb={2}>
          <Box
            p={2}
            bg={useColorModeValue(
              color === "green"
                ? "#dafbe1"
                : color === "red"
                ? "#ffebe9"
                : color === "yellow"
                ? "#fff8c5"
                : "#ddf4ff",
              color === "green"
                ? "#0f3d20"
                : color === "red"
                ? "#5c1011"
                : color === "yellow"
                ? "#4d4106"
                : "#0d419d"
            )}
            borderRadius="md"
          >
            <Icon
              as={icon}
              boxSize={5}
              color={
                color === "green"
                  ? "#1f883d"
                  : color === "red"
                  ? "#d1242f"
                  : color === "yellow"
                  ? "#9a6700"
                  : accentColor
              }
            />
          </Box>
        </HStack>
        <VStack align="start" spacing={1}>
          <Text fontSize="sm" color={mutedText}>
            {label}
          </Text>
          <HStack align="baseline">
            <Text fontSize="2xl" fontWeight="600" color={textColor}>
              {typeof value === "number" ? value.toLocaleString() : value}
            </Text>
            {unit && (
              <Text fontSize="sm" color={mutedText}>
                {unit}
              </Text>
            )}
          </HStack>
          {subValue && (
            <Text fontSize="xs" color={mutedText}>
              {subValue}
            </Text>
          )}
        </VStack>
      </CardBody>
    </Card>
  );

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  if (loading) {
    return (
      <Flex h="60vh" align="center" justify="center">
        <Spinner size="xl" />
      </Flex>
    );
  }

  return (
    <Box w="full" maxW="100%">
      <Flex justify="space-between" align="center" mb={6}>
        <Box>
          <Heading size="lg" color={textColor} fontWeight="400" mb={2}>
            Statistics Dashboard
          </Heading>
          <Text color={mutedText} fontSize="sm">
            Overview of system performance and usage
          </Text>
        </Box>
        <HStack>
          <Button
            size="sm"
            variant="ghost"
            onClick={fetchStatistics}
            leftIcon={<Icon as={Activity} boxSize={4} />}
          >
            Refresh
          </Button>
        </HStack>
      </Flex>

      {/* Key Metrics */}
      <SimpleGrid columns={{ base: 1, md: 2, lg: 4 }} spacing={4} mb={6}>
        <StatCard
          icon={Users}
          label="Total Accounts"
          value={statistics?.totalAccounts || 0}
          color="blue"
        />
        <StatCard
          icon={Database}
          label="Total Diagrams"
          value={statistics?.totalDiagrams || 0}
          color="blue"
        />
        <StatCard
          icon={MessageSquare}
          label="Chat Requests"
          value={statistics?.totalRequests || 0}
          color="blue"
        />
        <StatCard
          icon={Activity}
          label="Total Tokens"
          value={statistics?.totalTokens || 0}
          color="blue"
        />
      </SimpleGrid>

      {/* Performance Metrics Row */}
      <SimpleGrid columns={{ base: 1, md: 2 }} spacing={4} mb={6}>
        <StatCard
          icon={Clock}
          label="Avg Response Time"
          value={statistics?.avgTotalTime.toFixed(2) || "0"}
          unit="ms"
          color="yellow"
        />

        <StatCard
          icon={CheckCircle2}
          label="Success Rate"
          value={statistics?.successRate.toFixed(1) || "0"}
          unit="%"
          subValue={`${statistics?.successfulRequests || 0} / ${
            statistics?.totalRequests || 0
          } requests`}
          color="green"
        />
      </SimpleGrid>

      {/* Charts Grid */}
      <Grid
        templateColumns={{ base: "1fr", lg: "repeat(2, 1fr)" }}
        gap={4}
        mb={6}
      >
        {/* Token Distribution */}
        <GridItem>
          <Card bg={cardBg} border="1px" borderColor={borderColor}>
            <CardBody>
              <Heading size="sm" mb={4} color={textColor} fontWeight="600">
                Token Distribution
              </Heading>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={tokenDistributionData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }: any) =>
                      `${name.split(" ")[0]}: ${(percent * 100).toFixed(0)}%`
                    }
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {tokenDistributionData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={COLORS[index % COLORS.length]}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </CardBody>
          </Card>
        </GridItem>

        {/* Request Status */}
        <GridItem>
          <Card bg={cardBg} border="1px" borderColor={borderColor}>
            <CardBody>
              <Heading size="sm" mb={4} color={textColor} fontWeight="600">
                Request Success Rate
              </Heading>
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={requestStatusData}
                    cx="50%"
                    cy="50%"
                    labelLine={false}
                    label={({ name, percent }: any) =>
                      `${name}: ${(percent * 100).toFixed(0)}%`
                    }
                    outerRadius={80}
                    fill="#8884d8"
                    dataKey="value"
                  >
                    {requestStatusData.map((entry, index) => (
                      <Cell
                        key={`cell-${index}`}
                        fill={index === 0 ? "#1f883d" : "#d1242f"}
                      />
                    ))}
                  </Pie>
                  <Tooltip />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            </CardBody>
          </Card>
        </GridItem>
      </Grid>

      {/* Time Series Charts */}
      {timeSeriesData.length > 0 && (
        <Grid
          templateColumns={{ base: "1fr", lg: "repeat(2, 1fr)" }}
          gap={4}
          mb={6}
        >
          {/* Requests Over Time */}
          <GridItem>
            <Card bg={cardBg} border="1px" borderColor={borderColor}>
              <CardBody>
                <Heading size="sm" mb={4} color={textColor} fontWeight="600">
                  Requests Over Time
                </Heading>
                <ResponsiveContainer width="100%" height={250}>
                  <AreaChart data={timeSeriesData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip />
                    <Area
                      type="monotone"
                      dataKey="requests"
                      stroke="#0969da"
                      fill="#ddf4ff"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </CardBody>
            </Card>
          </GridItem>

          {/* Response Time */}
          <GridItem>
            <Card bg={cardBg} border="1px" borderColor={borderColor}>
              <CardBody>
                <Heading size="sm" mb={4} color={textColor} fontWeight="600">
                  Average Response Time (ms)
                </Heading>
                <ResponsiveContainer width="100%" height={250}>
                  <LineChart data={timeSeriesData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey="avgTime"
                      stroke="#1f883d"
                      strokeWidth={2}
                      name="Avg Time (ms)"
                    />
                  </LineChart>
                </ResponsiveContainer>
              </CardBody>
            </Card>
          </GridItem>
        </Grid>
      )}

      {/* Token Usage Over Time */}
      {timeSeriesData.length > 0 && (
        <Card bg={cardBg} border="1px" borderColor={borderColor} mb={6}>
          <CardBody>
            <Heading size="sm" mb={4} color={textColor} fontWeight="600">
              Token Usage Trend
            </Heading>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={timeSeriesData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis />
                <Tooltip />
                <Legend />
                <Bar dataKey="tokens" fill="#0969da" name="Total Tokens" />
              </BarChart>
            </ResponsiveContainer>
          </CardBody>
        </Card>
      )}

      {/* Recent Requests Table */}
      <Card bg={cardBg} border="1px" borderColor={borderColor}>
        <CardBody>
          <Heading size="sm" mb={4} color={textColor} fontWeight="600">
            Recent Requests (Last 10)
          </Heading>
          <Box overflowX="auto">
            <Table variant="simple" size="sm">
              <Thead>
                <Tr>
                  <Th>Time</Th>
                  <Th>Status</Th>
                  <Th isNumeric>Input Tokens</Th>
                  <Th isNumeric>Output Tokens</Th>
                  <Th isNumeric>Total Tokens</Th>
                  <Th isNumeric>Time (ms)</Th>
                </Tr>
              </Thead>
              <Tbody>
                {recentMetrics.slice(0, 10).map((metric) => (
                  <Tr key={metric.id}>
                    <Td>
                      <Text fontSize="xs" color={mutedText}>
                        {formatDate(metric.created_at)}
                      </Text>
                    </Td>
                    <Td>
                      <Badge
                        colorScheme={
                          metric.status === "success" ? "green" : "red"
                        }
                        fontSize="xs"
                      >
                        {metric.status}
                      </Badge>
                    </Td>
                    <Td isNumeric>{metric.input_tokens}</Td>
                    <Td isNumeric>{metric.output_tokens}</Td>
                    <Td isNumeric fontWeight="600">
                      {metric.total_tokens}
                    </Td>
                    <Td isNumeric>
                      <Text
                        color={
                          metric.total_time_ms < 5
                            ? "green.500"
                            : metric.total_time_ms < 10
                            ? "yellow.500"
                            : "red.500"
                        }
                        fontWeight="500"
                      >
                        {metric.total_time_ms.toFixed(2)}
                      </Text>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </Box>
        </CardBody>
      </Card>
    </Box>
  );
}
