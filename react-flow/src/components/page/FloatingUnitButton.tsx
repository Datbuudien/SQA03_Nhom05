import { AiOutlineDeploymentUnit } from "react-icons/ai";
import {
  IconButton,
  Box,
  useColorModeValue,
  Tooltip,
  useBreakpointValue,
} from "@chakra-ui/react";
import { keyframes } from "@emotion/react";

export const FloatingUnitButton = ({ onClick }: { onClick?: () => void }) => {
  const bgColor = useColorModeValue("#fff", "#333");
  const iconColor = useColorModeValue("black", "white");
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const isTooltipActive = useBreakpointValue({ base: false, md: true });

  const spin = keyframes`
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  `;
  const spinAnimation = `${spin} 3s linear infinite`;

  return (
    <Tooltip
      label="Ask Sherpa"
      placement="bottom"
      hasArrow
      gutter={10}
      isDisabled={!isTooltipActive}
    >
      <IconButton
        aria-label="Deployment Unit"
        icon={<AiOutlineDeploymentUnit size={22} />}
        boxSize={"40px"}
        onClick={onClick}
        color={iconColor}
        borderRadius="full"
        border={"1px"}
        bg={bgColor}
        borderColor={borderColor}
        transition="all 0.3s ease-in-out"
        _hover={{
          transform: "scale(1.1)",
          bgGradient: "linear(to-br, blue.400, teal.400)",
          color: "white",
          boxShadow: "0 10px 25px rgba(88, 106, 167, 0.5)",
          svg: {
            animation: spinAnimation,
          },
        }}
      />
    </Tooltip>
  );
};

export default FloatingUnitButton;
