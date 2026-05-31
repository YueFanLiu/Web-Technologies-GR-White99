import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class FriendsPage {
  constructor(page) {
    this.page = page
  }

  async expectBlankSearchWarning() {
    await this.page.goto('/friends')
    await this.page.getByRole('button', { name: /search/i }).click()
    await expect(this.page.getByText('Please enter a keyword')).toBeVisible()
  }

  async searchUser(keyword) {
    await this.page.goto('/friends')
    await this.page.getByPlaceholder(/search/i).fill(keyword)
    await this.page.getByRole('button', { name: /search/i }).click()
    await expect(this.page.getByText(keyword, { exact: false })).toBeVisible()
  }

  async sendFriendRequest(keyword) {
    await this.searchUser(keyword)
    await this.page.getByRole('button', { name: /add|request/i }).first().click()
    await expect(this.page.getByText(/sent|pending|request/i)).toBeVisible()
  }

  async acceptFirstRequest() {
    await this.page.goto('/friends')
    await this.page.getByRole('button', { name: /accept/i }).first().click()
    await expect(this.page.getByText(/accepted|friend/i)).toBeVisible()
  }
}


