# Access4All Missing Features Expansion Plan

## Summary
This implementation adds five missing feature groups to Access4All: multi-tier tickets, calendar export links, attendee analytics, admin user management, and platform usage reports. Calendar integration uses ICS download plus Google and Outlook links. Ticket sales are calculated from booking price snapshots, without real payment processing.

## Feature Scope
- Multi-tier ticketing: events can have VIP, Standard, Early Bird, or custom ticket tiers.
- Calendar integration: confirmed registrations can be exported as ICS and opened in Google Calendar or Outlook.
- Attendee analytics: organizers and admins can view registration counts, remaining capacity, ticket revenue, and feedback metrics.
- User management: admins can list, search, update role/profile fields, and deactivate/reactivate users.
- Reporting tools: admins can view platform usage statistics and top events.

## Implementation Order
1. Add database migration for ticket tiers, registration price snapshots, user status, and indexes.
2. Add backend ticket tier entity, repository, DTOs, service, and controller.
3. Update registration creation/update logic to validate ticket tiers, capacity, quantity, and price snapshots.
4. Add calendar ICS endpoint and frontend buttons.
5. Add event analytics endpoint and display it on the attendee list page.
6. Add admin user management APIs and frontend page.
7. Add admin platform report APIs and frontend page.
8. Run backend/frontend build validation and update E2E coverage later.

## Public API Additions
- `GET /api/events/{eventId}/ticket-tiers`
- `POST /api/events/{eventId}/ticket-tiers`
- `PUT /api/events/{eventId}/ticket-tiers/{tierId}`
- `DELETE /api/events/{eventId}/ticket-tiers/{tierId}`
- `POST /api/registrations` with `ticketTierId` and `quantity`
- `GET /api/registrations/{id}/calendar.ics`
- `GET /api/events/{eventId}/analytics`
- `GET /api/admin/users`
- `PUT /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/status`
- `GET /api/admin/reports/platform-usage`

## Assumptions
- Ticket sales means calculated revenue from registrations, not real payment integration.
- Calendar integration means downloadable/importable calendar data and provider links, not OAuth-based direct calendar write.
- User deletion is avoided. User management uses soft status changes because hard deletion can break registrations, posts, chats, notifications, and Supabase Auth links.
- Existing Ruoyi `/system/user` frontend code is not reused because the backend does not implement those endpoints.
