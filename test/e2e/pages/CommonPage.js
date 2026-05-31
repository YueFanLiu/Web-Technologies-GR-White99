import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class CommonPage {
  constructor(page) {
    this.page = page
  }

  async expectSignedInShell() {
    await expect(this.page.locator('body')).toBeVisible()
    await expect(this.page).not.toHaveURL(/\/login$/)
  }

  async warmAuthenticatedSession() {
    await this.page.goto('/index')
    await this.expectSignedInShell()
  }

  async openProfile() {
    await this.page.goto('/user/profile')
  }

  async expectRoleDisplayedAsAttendee() {
    await this.openProfile()
    await expect(this.page.getByText('ATTENDEE')).toBeVisible()
    await expect(this.page.getByText('PARENT')).toHaveCount(0)
  }

  async unreadBadgeShouldNotShowZero() {
    await expect(this.page.locator('.el-badge__content', { hasText: /^0$/ })).toHaveCount(0)
  }
}


