import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'
import { CommonPage } from './CommonPage.js'

export class ManagerPage {
  constructor(page) {
    this.page = page
  }

  async createActivity(data) {
    await new CommonPage(this.page).warmAuthenticatedSession()
    await this.page.goto('/manager/createActivity')
    await this.page.getByLabel(/title/i).fill(data.title)
    await this.page.getByLabel(/description/i).fill(data.description)
    await this.page.getByLabel(/category/i).fill(data.category)
    await this.page.getByLabel(/capacity/i).fill(data.capacity)
    await this.page.getByLabel(/price/i).fill(data.price)
    await this.page.getByLabel(/venue name/i).fill(data.venueName)
    await this.page.getByLabel(/address/i).fill(data.address)
    await this.page.getByLabel(/city/i).fill(data.city)
    await this.page.getByLabel(/country/i).fill(data.country)
    await this.page.getByLabel(/start date/i).fill(data.startDate)
    await this.page.getByLabel(/start time/i).fill(data.startTime)
    await this.page.getByLabel(/end date/i).fill(data.endDate)
    await this.page.getByLabel(/end time/i).fill(data.endTime)
    await this.page.getByRole('button', { name: /create|publish|save/i }).click()
    await expect(this.page.getByText(/created|success|published/i)).toBeVisible()
  }

  async openAttendeeListForActivity(title) {
    await this.page.goto('/manager/manageEvent')
    const row = this.page.locator('.el-table__row, .activity-card, article').filter({ hasText: title }).first()
    await expect(row).toBeVisible()
    await row.getByRole('button', { name: /attendee|list|view/i }).first().click()
    await expect(this.page).toHaveURL(/\/manager\/attendeeList/)
  }

  async confirmFirstRegisteredAttendee() {
    const registeredRow = this.page.locator('.el-table__row, article').filter({ hasText: /registered/i }).first()
    await expect(registeredRow).toBeVisible()
    await registeredRow.getByRole('button', { name: /confirm|confirmed|approve/i }).first().click()
    await expect(this.page.getByText(/confirmed/i)).toBeVisible()
  }
}


