import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { hasOptionalAccount, roles } from '../fixtures/users.js'
import { FriendsPage } from '../pages/FriendsPage.js'
import { MessagesPage } from '../pages/MessagesPage.js'

test.describe.serial('friends and direct messages', () => {
  test('blank friend search warns instead of returning all users', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const friends = new FriendsPage(page)

    await friends.expectBlankSearchWarning()
    await context.close()
  })

  test('attendee sends friend request and recipient accepts', async ({ browser }) => {
    test.skip(!hasOptionalAccount('attendee2'), 'Set E2E_ATTENDEE_2_EMAIL/PASSWORD to run full friend flow')

    const requesterContext = await browser.newContext({ storageState: authState('attendee') })
    const requesterPage = await requesterContext.newPage()
    await new FriendsPage(requesterPage).sendFriendRequest(roles.attendee2.email)
    await requesterContext.close()

    const recipientContext = await browser.newContext({ storageState: authState('attendee2') })
    const recipientPage = await recipientContext.newPage()
    await new FriendsPage(recipientPage).acceptFirstRequest()
    await recipientContext.close()
  })

  test('friends can chat and unread zero badge is hidden after reading', async ({ browser }) => {
    test.skip(!hasOptionalAccount('attendee2'), 'Set E2E_ATTENDEE_2_EMAIL/PASSWORD to run full message flow')

    const senderContext = await browser.newContext({ storageState: authState('attendee') })
    const senderPage = await senderContext.newPage()
    const messages = new MessagesPage(senderPage)
    const text = `E2E direct message ${Date.now()}`
    await messages.openFirstConversation()
    await messages.sendMessage(text)
    await senderContext.close()

    const recipientContext = await browser.newContext({ storageState: authState('attendee2') })
    const recipientPage = await recipientContext.newPage()
    const recipientMessages = new MessagesPage(recipientPage)
    await recipientMessages.openFirstConversation()
    await expect(recipientPage.getByText(text)).toBeVisible()
    await recipientMessages.expectNoZeroBadge()
    await recipientContext.close()
  })
})


