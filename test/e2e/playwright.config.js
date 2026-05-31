import { defineConfig, devices } from '../../frontend/node_modules/@playwright/test/index.mjs'
import dotenv from '../../frontend/node_modules/dotenv/lib/main.js'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
dotenv.config({ path: path.resolve(__dirname, '.env.e2e') })

const baseURL = process.env.E2E_BASE_URL || 'http://localhost:5173'

export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  reporter: [
    ['list'],
    ['html', { outputFolder: path.resolve(__dirname, 'playwright-report'), open: 'never' }]
  ],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'setup',
      testMatch: /00-auth\.setup\.spec\.js/
    },
    {
      name: 'chromium',
      testIgnore: /00-auth\.setup\.spec\.js/,
      use: { ...devices['Desktop Chrome'] },
      dependencies: ['setup']
    }
  ],
  outputDir: path.resolve(__dirname, 'test-results')
})


