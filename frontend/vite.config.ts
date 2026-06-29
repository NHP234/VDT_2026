import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import type { UserConfig } from 'vite';

type VitestUserConfig = UserConfig & {
  test: {
    globals: boolean;
    environment: string;
    setupFiles: string;
  };
};

const config = {
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/setup.ts',
  },
} satisfies VitestUserConfig;

export default defineConfig(config);
