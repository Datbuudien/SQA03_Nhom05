import {
  IconButton,
  Tooltip,
  useBreakpointValue,
  useColorMode,
  useColorModeValue,
} from "@chakra-ui/react";
import { SunIcon, MoonIcon } from "@chakra-ui/icons";

export function ThemeToggle() {
  const { colorMode, toggleColorMode } = useColorMode();
  const borderColor = useColorModeValue("#d0d7de", "#444");
  const bgColor = useColorModeValue("#fff", "#333");
  const isTooltipActive = useBreakpointValue({ base: false, md: true });

  return (
    <Tooltip
      label="Theme"
      hasArrow
      placement="bottom"
      isDisabled={!isTooltipActive}
    >
      <IconButton
        aria-label="Toggle theme"
        icon={
          colorMode === "light" ? (
            <MoonIcon boxSize={"17px"} />
          ) : (
            <SunIcon boxSize={"20px"} />
          )
        }
        onClick={toggleColorMode}
        // variant="ghost"
        // mr={2}
        // ml={1.5}
        bg={bgColor}
        size="md"
        borderRadius={"50%"}
        border={"1px"}
        borderColor={borderColor}
      />
    </Tooltip>
  );
}
