import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { CommonPage } from '../pages/CommonPage.js'

test.describe('notifications', () => {
  test('notifications can be read and zero badges are hidden', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()

    await page.goto('/notifications')
    const firstUnread = page.getByRole('button', { name: /mark.*read|read/i }).first()
    if (await firstUnread.count()) {
      await firstUnread.click()
    }
    await new CommonPage(page).unreadBadgeShouldNotShowZero()
    await expect(page.locator('body')).toContainText(/notification|updates|read/i)
    await context.close()
  })
})


