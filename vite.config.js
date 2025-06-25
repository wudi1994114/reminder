import { defineConfig } from 'vite';
import uni from '@dcloudio/uni-app';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
}); 