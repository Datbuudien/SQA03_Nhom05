import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    global: "globalThis",
  },
  optimizeDeps: {
    include: ["sockjs-client"],
  },
  server: {
    host: true, // 🌟 listen trên 0.0.0.0
    port: 5173, // mặc định
  },
});
