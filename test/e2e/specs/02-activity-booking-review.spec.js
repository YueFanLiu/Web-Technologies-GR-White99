import { test, expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { authState } from '../fixtures/auth.js'
import { activityData } from '../fixtures/data.js'
import { ActivityPage } from '../pages/ActivityPage.js'
import { ManagerPage } from '../pages/ManagerPage.js'

test.describe.serial('activity booking, confirmation, and reviews', () => {
  const activity = activityData()

  test('organizer creates a cross-day started activity', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('organizer') })
    const page = await context.newPage()
    const manager = new ManagerPage(page)

    await manager.createActivity(activity)
    await context.close()
  })

  test('attendee books activity but cannot review before confirmation', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const activities = new ActivityPage(page)

    await activities.gotoList()
    await page.getByPlaceholder(/search/i).fill(activity.title)
    await page.keyboard.press('Enter')
    await expect(page.getByRole('button', { name: /favorite/i }).first()).toBeVisible()
    await expect(page.getByRole('button', { name: /^save$/i })).toHaveCount(0)
    await activities.openActivityByTitle(activity.title)
    await activities.bookCurrentActivity({
      fullName: 'E2E Attendee',
      email: process.env.E2E_GMAIL_EMAIL || process.env.E2E_ATTENDEE_EMAIL,
      phone: '+33 100000000'
    })

    await page.goto('/my-events/joined')
    await expect(page.getByText(activity.title)).toBeVisible()
    await context.close()
  })

  test('organizer confirms attendee registration', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('organizer') })
    const page = await context.newPage()
    const manager = new ManagerPage(page)

    await manager.openAttendeeListForActivity(activity.title)
    await manager.confirmFirstRegisteredAttendee()
    await context.close()
  })

  test('confirmed attendee can leave multiple reviews after activity started', async ({ browser }) => {
    const context = await browser.newContext({ storageState: authState('attendee') })
    const page = await context.newPage()
    const activities = new ActivityPage(page)
    const firstReview = `E2E review after confirmation ${Date.now()}`
    const secondReview = `E2E second review after confirmation ${Date.now()}`

    await page.goto('/product/mainEvent')
    await page.getByPlaceholder(/search/i).fill(activity.title)
    await page.keyboard.press('Enter')
    await activities.openActivityByTitle(activity.title)

    await activities.submitReview(firstReview)
    await activities.submitReview(secondReview)
    await context.close()
  })
})


