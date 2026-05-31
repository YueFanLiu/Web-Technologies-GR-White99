import { test } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import fs from 'node:fs/promises'
import { LoginPage } from '../pages/LoginPage.js'
import { authDir, authState } from '../fixtures/auth.js'
import { hasOptionalAccount, requireAccount, roles } from '../fixtures/users.js'

async function loginAndSave(page, roleName) {
  const account = roles[roleName]?.optional && !hasOptionalAccount(roleName)
    ? null
    : requireAccount(roleName)
  if (!account) return

  const loginPage = new LoginPage(page)
  await loginPage.login(account.email, account.password)
  await page.context().storageState({ path: authState(roleName) })
}

test('login all configured roles', async ({ page }) => {
  await fs.mkdir(authDir, { recursive: true })
  await loginAndSave(page, 'attendee')
  await loginAndSave(page, 'organizer')
  await loginAndSave(page, 'admin')
  if (hasOptionalAccount('attendee2')) {
    await loginAndSave(page, 'attendee2')
  }
})


