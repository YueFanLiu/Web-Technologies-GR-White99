import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class ActivityPage {
  constructor(page) {
    this.page = page
  }

  async gotoList() {
    await this.page.goto('/product/mainEvent')
    await expect(this.page.getByRole('heading', { name: /activities|events/i })).toBeVisible()
  }

  async openActivityByTitle(title) {
    await this.page.getByText(title, { exact: false }).first().click()
    await expect(this.page).toHaveURL(/\/product\/eventDetails/)
  }

  async bookCurrentActivity(contact) {
    await this.page.getByRole('button', { name: /book now/i }).click()
    await expect(this.page).toHaveURL(/\/product\/bookActivity/)
    await this.page.getByLabel(/full name/i).fill(contact.fullName)
    await this.page.getByLabel(/^email$/i).fill(contact.email)
    await this.page.getByLabel(/phone number/i).fill(contact.phone)
    await this.page.getByRole('button', { name: /confirm booking/i }).first().click()
    await expect(this.page).toHaveURL(/\/product\/bookingConfirmation/)
    await expect(this.page.getByText(/booking/i)).toBeVisible()
  }

  async expectCannotContactOrganizerBeforeConfirmed() {
    await this.page.goto('/product/eventDetails' + new URL(this.page.url()).search)
    await expect(this.page.getByRole('button', { name: /contact organizer/i })).toHaveCount(0)
  }

  async submitReview(text) {
    await expect(this.page.getByText(/reviews/i)).toBeVisible()
    await this.page.getByPlaceholder(/write a review/i).fill(text)
    await this.page.getByRole('button', { name: /submit review/i }).click()
    await expect(this.page.getByText(text)).toBeVisible()
  }
}


