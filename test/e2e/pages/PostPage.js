import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class PostPage {
  constructor(page) {
    this.page = page
  }

  async createPost(data, relatedEventTitle = '') {
    await this.page.goto('/post/createPost')
    if (relatedEventTitle) {
      await this.page.getByPlaceholder(/search an event/i).fill(relatedEventTitle)
      await this.page.getByText(relatedEventTitle, { exact: false }).first().click()
    }
    await this.page.getByLabel(/post title/i).fill(data.title)
    await this.page.getByLabel(/content|experience/i).fill(data.content)
    await this.page.getByRole('button', { name: /publish post/i }).click()
    await expect(this.page).toHaveURL(/\/post\/mainPost/)
  }

  async openPostDetails(title) {
    await this.page.goto('/post/mainPost')
    const card = this.page.locator('article').filter({ hasText: title }).first()
    await expect(card).toBeVisible()
    await card.getByRole('button', { name: /view/i }).click()
    await expect(this.page).toHaveURL(/\/post\/postDetails/)
    await expect(this.page.getByRole('heading', { name: title })).toBeVisible()
  }

  async expectRelatedEventAndOpen(eventTitle) {
    const escapedTitle = eventTitle.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    await expect(this.page.getByRole('button', { name: new RegExp(escapedTitle, 'i') })).toBeVisible()
    await this.page.getByRole('button', { name: new RegExp(escapedTitle, 'i') }).click()
    await expect(this.page).toHaveURL(/\/product\/eventDetails/)
    await expect(this.page.getByText(eventTitle, { exact: false })).toBeVisible()
  }

  async commentOnPost(comment) {
    await this.page.getByPlaceholder(/write a comment/i).fill(comment)
    await this.page.getByRole('button', { name: /submit comment/i }).click()
    await expect(this.page.getByText(comment)).toBeVisible()
  }
}


