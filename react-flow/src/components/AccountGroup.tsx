import { useEffect, useState } from "react";
import {
  Box,
  Spinner,
  Tooltip,
  useColorModeValue,
  VStack,
  Text,
  useBreakpointValue,
} from "@chakra-ui/react";

interface AccountGroupProps {
  onlineUsernames?: string[];
}

export const AccountGroup: React.FC<AccountGroupProps> = ({
  onlineUsernames = [],
}) => {
  const [onlineUsers, setOnlineUsers] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);

  const bgColor = useColorModeValue("white", "#333");
  const borderColor = useColorModeValue("#d0d7de", "#444");

  // Determine whether to show the text based on screen size
  const showText = useBreakpointValue({ base: false, sm: true });

  useEffect(() => {
    const usersWithEmail = onlineUsernames.map(
      (username) => `${username}@gmail.com`
    );
    console.log("hiep email: ", usersWithEmail);
    setOnlineUsers(usersWithEmail);
    setLoading(false);
  }, [onlineUsernames]);

  if (loading) return <Spinner size="sm" />;
  if (onlineUsers.length === 0) return null;

  const tooltipContent = (
    <VStack align="start" spacing={1}>
      {onlineUsers.slice(0, 3).map((user) => (
        <Text key={user} fontSize="sm">
          {user}
        </Text>
      ))}
      {onlineUsers.length > 3 && (
        <Text fontSize="sm">and {onlineUsers.length - 3} more</Text>
      )}
    </VStack>
  );

  return (
    <Tooltip label={tooltipContent} placement="bottom" hasArrow>
      <Box
        bg={bgColor}
        h="40px"
        px={showText ? 3 : 2} // nếu ẩn chữ thì padding nhỏ hơn
        border={`1px solid ${borderColor}`}
        fontSize="sm"
        borderRadius="20px"
        display="flex"
        alignItems="center"
        gap={1.5}
        cursor="pointer"
        userSelect={"none"}
      >
        <Box w={1.5} h={1.5} bg="green.400" borderRadius="50%" />
        {onlineUsers.length}
        {showText && ` online`}
      </Box>
    </Tooltip>
  );
};
