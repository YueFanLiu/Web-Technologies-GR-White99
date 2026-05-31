import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { MessagesPage } from '../pages/MessagesPage.js'

test.describe('event contact chat reuse', () => {
  test('messages page never renders duplicate-key errors or zero unread badges', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()

    await new MessagesPage(page).openMessages()
    await expect(page.getByText(/duplicate key|chat_conversations_direct_pair_uidx/i)).toHaveCount(0)
    await new MessagesPage(page).expectNoZeroBadge()
    await context.close()
  })
})


