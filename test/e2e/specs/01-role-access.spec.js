import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { CommonPage } from '../pages/CommonPage.js'

test.describe('role access and display', () => {
  test('attendee role is displayed as ATTENDEE, not PARENT', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const common = new CommonPage(page)

    await common.expectRoleDisplayedAsAttendee()
    await context.close()
  })

  test('organizer can access activity management', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('organizer') })
    const page = await context.newPage()
    const common = new CommonPage(page)

    await common.warmAuthenticatedSession()
    await page.goto('/manager/manageEvent')
    await expect(page).not.toHaveURL(/\/401|\/login/)
    await expect(page.locator('body')).toContainText(/activity|event|manage/i)
    await context.close()
  })

  test('admin can access organizer management routes', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('admin') })
    const page = await context.newPage()
    const common = new CommonPage(page)

    await common.warmAuthenticatedSession()
    await page.goto('/manager/manageEvent')
    await expect(page).not.toHaveURL(/\/401|\/login/)
    await expect(page.locator('body')).toContainText(/activity|event|manage/i)
    await context.close()
  })
})


