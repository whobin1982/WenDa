// ESLint v9 flat config（基线：修复 #4 frontend-ci.yml 假绿）。
// 真实门禁：lint 失败即 workflow 失败（不再 || true）。
// 当前规则集：仅启用 TS + Vue3 关键规则；不引入未审查规则。
import js from '@eslint/js'
import tseslint from 'typescript-eslint'
import pluginVue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

export default [
    {
        ignores: [
            'node_modules/**',
            'dist/**',
            'coverage/**',
            '.vite/**',
            '*.config.ts',
            '*.config.js'
        ]
    },
    js.configs.recommended,
    ...tseslint.configs.recommended,
    ...pluginVue.configs['flat/recommended'],
    {
        files: ['**/*.{ts,vue}'],
        languageOptions: {
            parser: vueParser,
            parserOptions: {
                parser: tseslint.parser,
                sourceType: 'module',
                ecmaVersion: 'latest'
            }
        },
        rules: {
            // 关闭对未使用变量 / 空对象类型的强警告；后续按需收紧
            '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
            'no-unused-vars': 'off',
            'no-empty': ['error', { allowEmptyCatch: true }],
            'vue/multi-word-component-names': 'off',
            'vue/no-v-html': 'off',
            'vue/html-self-closing': 'off',
            'vue/singleline-html-element-content-newline': 'off',
            'vue/max-attributes-per-line': 'off',
            'vue/html-indent': 'off',
            'vue/html-closing-bracket-newline': 'off',
            'vue/first-attribute-linebreak': 'off',
            'vue/attribute-hyphenation': 'off',
            'vue/v-on-event-hyphenation': 'off'
        }
    }
]
