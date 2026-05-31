export const roles = {
  attendee: {
    email: process.env.E2E_ATTENDEE_EMAIL,
    password: process.env.E2E_ATTENDEE_PASSWORD,
    storageState: '../auth/attendee.json'
  },
  attendee2: {
    email: process.env.E2E_ATTENDEE_2_EMAIL,
    password: process.env.E2E_ATTENDEE_2_PASSWORD,
    storageState: '../auth/attendee2.json',
    optional: true
  },
  organizer: {
    email: process.env.E2E_ORGANIZER_EMAIL,
    password: process.env.E2E_ORGANIZER_PASSWORD,
    storageState: '../auth/organizer.json'
  },
  admin: {
    email: process.env.E2E_ADMIN_EMAIL,
    password: process.env.E2E_ADMIN_PASSWORD,
    storageState: '../auth/admin.json'
  }
}

export function requireAccount(roleName) {
  const account = roles[roleName]
  if (!account?.email || !account?.password) {
    throw new Error(`Missing E2E ${roleName} credentials in test/e2e/.env.e2e`)
  }
  return account
}

export function hasOptionalAccount(roleName) {
  const account = roles[roleName]
  return Boolean(account?.email && account?.password)
}


