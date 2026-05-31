import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { postData } from '../fixtures/data.js'
import { PostPage } from '../pages/PostPage.js'

test.describe.serial('posts and post details', () => {
  const post = postData()
  const relatedEventTitle = process.env.E2E_RELATED_EVENT_TITLE || ''

  test('attendee creates a post and views it in postDetails', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const posts = new PostPage(page)

    await posts.createPost(post, relatedEventTitle)
    await posts.openPostDetails(post.title)
    await expect(page.getByText(post.content)).toBeVisible()
    if (relatedEventTitle) {
      await posts.expectRelatedEventAndOpen(relatedEventTitle)
    }
    await context.close()
  })

  test('post associated with event displays the event and can jump to eventDetails', async ({ browser }) => {
    test.skip(!relatedEventTitle, 'Set E2E_RELATED_EVENT_TITLE to run related event display/jump coverage')

    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const posts = new PostPage(page)

    await posts.openPostDetails(post.title)
    await posts.expectRelatedEventAndOpen(relatedEventTitle)
    await context.close()
  })

  test('postDetails supports adding and deleting comments', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const posts = new PostPage(page)
    const comment = `E2E post comment ${Date.now()}`

    await posts.openPostDetails(post.title)
    await posts.commentOnPost(comment)
    await page.getByRole('button', { name: /delete/i }).last().click()
    await page.getByRole('dialog', { name: /delete comment/i }).getByRole('button', { name: /^delete$/i }).click()
    await expect(page.getByText(comment)).toHaveCount(0)
    await context.close()
  })
})


