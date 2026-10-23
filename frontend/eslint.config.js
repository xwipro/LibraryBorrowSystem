import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'
import globals from 'globals'

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-ignore',
    ignores: [
      '**/dist/**',
      '**/dist-ssr/**',
      '**/coverage/**',
      '**/node_modules/**',
      'src/auto-imports.d.ts',
      'src/components.d.ts',
    ],
  },
  js.configs.recommended,
  vueTsConfigs.recommended,
  ...pluginVue.configs['flat/essential'],
  skipFormatting,
  {
    name: 'app/node-config',
    files: ['vite.config.ts', 'eslint.config.js'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
  {
    name: 'app/custom-rules',
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-v-html': 'off',
      'no-console': ['warn', { allow: ['warn', 'error'] }],
    },
  },
)
