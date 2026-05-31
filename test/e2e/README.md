# E2E Test Plan

Playwright E2E tests live here because they validate the whole app: frontend, backend, database state, role permissions, messages, notifications, bookings, reviews, and email side effects.

## Setup

Copy `.env.e2e.example` to `.env.e2e` and fill the test accounts.

Run from `frontend/`:

```bash
npm install
npx playwright install
npm run e2e
```

For debugging:

```bash
npm run e2e:headed
npm run e2e:ui
```

## Required Accounts

- `ATTENDEE`: database role may be `PARENT`, UI must show `ATTENDEE`.
- `ORGANIZER`: creates activities and confirms registrations.
- `ADMIN`: smoke-tests elevated access and role display.
- Optional `ATTENDEE_2`: used for friend request and friend chat flows.

Registration and forgot-password are tested manually by request. Automated tests start from existing accounts.

## Review Rule

Event reviews must only be submitted after:

1. The attendee has a registration for the event.
2. The organizer/admin changes the registration status to `CONFIRMED`.
3. The event has started.

The E2E flow confirms the registration before trying to leave a review.
