import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class MessagesPage {
  constructor(page) {
    this.page = page
  }

  async openMessages() {
    await this.page.goto('/messages')
    await expect(this.page.getByRole('heading', { name: /messages/i })).toBeVisible()
  }

  async openFirstConversation() {
    await this.openMessages()
    await this.page.locator('.conversation-item, article, .el-card').first().click()
    await expect(this.page).toHaveURL(/\/messages\//)
  }

  async sendMessage(text) {
    await this.page.getByPlaceholder(/type|message/i).fill(text)
    await this.page.getByRole('button', { name: /send/i }).click()
    await expect(this.page.getByText(text)).toBeVisible()
  }

  async expectNoZeroBadge() {
    await expect(this.page.locator('.el-badge__content', { hasText: /^0$/ })).toHaveCount(0)
  }
}


