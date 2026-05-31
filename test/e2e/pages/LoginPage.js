import { expect } from '../../../frontend/node_modules/@playwright/test/index.mjs'

export class LoginPage {
  constructor(page) {
    this.page = page
  }

  async goto() {
    await this.page.goto('/login')
  }

  async login(email, password) {
    await this.goto()
    await this.page.getByPlaceholder(/email/i).fill(email)
    await this.page.getByPlaceholder(/password/i).fill(password)
    await this.page.getByRole('button', { name: /log\s*in/i }).click()
    await expect(this.page).not.toHaveURL(/\/login$/)
  }
}


