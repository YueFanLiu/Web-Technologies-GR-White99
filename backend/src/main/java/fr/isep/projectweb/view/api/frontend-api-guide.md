# Frontend API Guide

Base URL in local development:

```text
http://localhost:9192
```

Most request and response bodies are JSON. For protected JSON endpoints, add:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

Public read endpoints do not need login:

```text
GET /api/events/**
GET /api/locations/**
GET /api/posts/**
```

Write endpoints generally need login.

Image upload endpoints use `multipart/form-data` instead of JSON. Do not manually set the `Content-Type` header for `FormData`; the browser adds the boundary automatically.
Only JPEG and PNG files are accepted. The backend rejects other image formats.

## Frontend Fetch Pattern

Public GET example:

```js
const res = await fetch("http://localhost:9192/api/events");
const data = await res.json();
```

Protected GET example:

```js
const res = await fetch("http://localhost:9192/api/users/me", {
  headers: {
    "Authorization": `Bearer ${accessToken}`
  }
});

const data = await res.json();
```

POST/PUT example:

```js
const res = await fetch("http://localhost:9192/api/events", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${accessToken}`
  },
  body: JSON.stringify(requestBody)
});

const data = await res.json();
```

DELETE example:

```js
await fetch(`http://localhost:9192/api/events/${eventId}`, {
  method: "DELETE",
  headers: {
    "Authorization": `Bearer ${accessToken}`
  }
});
```

Important frontend rules:

- `GET /api/events/**`, `GET /api/locations/**`, and `GET /api/posts/**` can be called without token.
- Protected `GET` endpoints still need `Authorization`, for example `/api/users/me`, `/api/users/{id}`, `/api/users/search`, `/api/friends`, `/api/chats`, `/api/notifications`, `/api/registrations`, and `/api/auth/debug`.
- `DELETE` endpoints return `204 No Content`, so the frontend must not call `res.json()` for successful delete responses.
- Multipart image upload endpoints return JSON containing the Supabase public URL.
- Image delete endpoints currently delete the database image record only. The public file object in Supabase Storage is not removed by the backend.
- There is no `PUT` endpoint for event/location/post image records. To replace an image, delete the old image record and upload or add a new one.
- Spring Security `401/403` responses may not always use the custom JSON error shape. The helper at the end of this document handles that.
- Supabase Storage must allow uploads to the `images` bucket. If multipart image upload returns `new row violates row-level security policy`, run `src/main/resources/supabase-storage-policies.sql` in the Supabase SQL Editor, or configure `SUPABASE_SERVICE_ROLE_KEY` for the backend runtime.

Error response shape:

```json
{
  "timestamp": "2026-05-03T15:26:08.866953+02:00",
  "status": 400,
  "message": "Error message"
}
```

## Auth

### GET /api/auth/config

Public. Returns frontend auth configuration.

Response body:

```json
{
  "supabaseUrl": "https://project.supabase.co",
  "supabasePublishableKey": "sb_publishable_...",
  "configured": true
}
```

### GET /api/auth/debug

Protected. Useful only for debugging the current JWT.

Response body:

```json
{
  "subject": "user-uuid",
  "email": "alice@example.com",
  "role": "authenticated",
  "issuer": "https://project.supabase.co/auth/v1",
  "expiresAt": "2026-05-03T14:00:00Z",
  "claims": {}
}
```

### POST /api/auth/signup

Public.

Request body:

```json
{
  "email": "alice@example.com",
  "password": "password123",
  "fullName": "Alice Dupont",
  "role": "ORGANIZER"
}
```

Response body:

```json
{
  "success": true,
  "status": "success",
  "message": "Signup successful",
  "email": "alice@example.com",
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresIn": 3600,
  "user": {
    "id": "user-id",
    "email": "alice@example.com",
    "fullName": "Alice Dupont",
    "role": "ORGANIZER"
  }
}
```

### POST /api/auth/login

Public.

Request body:

```json
{
  "email": "alice@example.com",
  "password": "password123"
}
```

Response body:

```json
{
  "success": true,
  "status": "success",
  "message": "Login successful",
  "email": "alice@example.com",
  "accessToken": "jwt-token",
  "refreshToken": "refresh-token",
  "expiresIn": 3600,
  "user": {
    "id": "user-id",
    "email": "alice@example.com",
    "fullName": "Alice Dupont",
    "role": "ORGANIZER"
  }
}
```

    ### POST /api/auth/forgot-password

Public. Sends a Supabase password reset email. `redirectTo` is optional; when provided, Supabase uses it as the page opened from the reset email link.
This backend endpoint only starts the password recovery flow. It does not receive the new password and it does not update the password itself.

Request body:

```json
{
  "email": "alice@example.com",
  "redirectTo": "http://localhost:5173/reset-password"
}
```

Response body:

```json
{
  "success": true,
  "status": "PASSWORD_RESET_EMAIL_SENT",
  "message": "Password reset email sent",
  "email": "alice@example.com"
}
```

Frontend flow:

1. On the "forgot password" page, call the backend endpoint above:

```js
await apiPost("/api/auth/forgot-password", {
  email,
  redirectTo: `${window.location.origin}/reset-password`
});
```

2. Supabase sends the password reset email. The `redirectTo` URL must be allowed in Supabase Auth URL Configuration, for example `http://localhost:5173/reset-password` in local development.

3. The user clicks the email link. Supabase redirects the browser to `redirectTo` with the password recovery information in the URL. The frontend page at `/reset-password` must initialize the same Supabase client used by the app.

4. On the `/reset-password` page, listen for the Supabase password recovery auth event. When it fires, show the new-password form:

```js
const {
  data: { subscription }
} = supabase.auth.onAuthStateChange((event, session) => {
  if (event !== "PASSWORD_RECOVERY") return;

  // Enable/show the reset-password form.
  // Supabase has restored a recovery session from the email link.
});
```

5. When the user submits the new password, call `updateUser`. The frontend does not manually send the email token to the backend; Supabase uses the recovery session created from the email link:

```js
async function submitNewPassword(newPassword) {
  const { data: { session } } = await supabase.auth.getSession();

  if (!session) {
    throw new Error("Password recovery session is missing or expired");
  }

  const { error } = await supabase.auth.updateUser({
    password: newPassword
  });

  if (error) {
    throw error;
  }
}
```

Swagger verification:

- Open `POST /api/auth/forgot-password`.
- Use this request body:

```json
{
  "email": "alice@example.com",
  "redirectTo": "http://localhost:5173/reset-password"
}
```

- A successful Swagger response only proves that the backend accepted the request and Supabase accepted the recovery email request.
- To verify the full password change, open the email, click the reset link, land on the frontend `/reset-password` page, and confirm that `supabase.auth.updateUser({ password: newPassword })` succeeds.
- Swagger cannot verify the final password update with the current backend API because there is no backend endpoint that accepts `token + newPassword`. The final update is done by the Supabase JavaScript client using the recovery session from the email link.

## Current User

### GET /api/users/me

Protected.

Response body:

```json
{
  "id": "uuid",
  "email": "alice@example.com",
  "fullName": "Alice Dupont",
  "phone": "+33123456789",
  "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
  "role": "ORGANIZER",
  "createdAt": "2026-05-03T12:00:00",
  "updatedAt": "2026-05-03T12:00:00",
  "accessibilityPreferences": {
    "wheelchairAccessible": true,
    "elevatorNeeded": true,
    "accessibleRestroom": true,
    "quietEnvironment": false
  }
}
```

### PUT /api/users/me

Protected. Updates only editable profile fields. `email` and `role` are read-only and must not be sent by the frontend for profile editing.

Request body:

```json
{
  "fullName": "Alice Martin",
  "phone": "+33123456789",
  "accessibilityPreferences": {
    "wheelchairAccessible": true,
    "elevatorNeeded": true,
    "accessibleRestroom": true,
    "quietEnvironment": false
  }
}
```

Response body: `UserProfileResponse`.

### POST /api/users/me/avatar

Protected. Uploads the current user's avatar to Supabase Storage under `images/userAvatar/{userId}/...`, saves the returned public URL in `users.photo`, and returns `UserProfileResponse`.

Request body: `multipart/form-data`

```text
file=<image file>
```

Frontend example:

```js
const formData = new FormData();
formData.append("file", file);

const res = await fetch("http://localhost:9192/api/users/me/avatar", {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${accessToken}`
  },
  body: formData
});

const profile = await res.json();
```

Response body:

```json
{
  "id": "uuid",
  "email": "alice@example.com",
  "fullName": "Alice Dupont",
  "phone": "+33123456789",
  "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
  "role": "ORGANIZER",
  "createdAt": "2026-05-03T12:00:00",
  "updatedAt": "2026-05-03T12:00:00",
  "accessibilityPreferences": {
    "wheelchairAccessible": true,
    "elevatorNeeded": true,
    "accessibleRestroom": true,
    "quietEnvironment": false
  }
}
```

### GET /api/users/{id}

Protected.

Response body:

```json
{
  "id": "uuid",
  "fullName": "Alice Dupont",
  "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
  "role": "ORGANIZER"
}
```

### GET /api/users/search

Protected. Searches public user profiles by full name. The current authenticated user is excluded from the response.

Optional query params:

```text
keyword=alice
limit=20
```

Frontend example:

```js
const params = new URLSearchParams({
  keyword: "alice",
  limit: "20"
});

const users = await apiGet(`/api/users/search?${params}`, accessToken);
```

Response body:

```json
[
  {
    "id": "uuid",
    "fullName": "Alice Dupont",
    "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
    "role": "PARENT"
  }
]
```

## Friends

All friend endpoints are protected.

### GET /api/friends

Returns the current authenticated user's accepted friends.

Frontend example:

```js
const friends = await apiGet("/api/friends", accessToken);
```

Response body:

```json
[
  {
    "user": {
      "id": "uuid",
      "fullName": "Bob Martin",
      "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
      "role": "PARENT"
    },
    "friendsSince": "2026-05-20T13:00:00"
  }
]
```

### POST /api/friend-requests

Sends a friend request. The backend rejects requests to yourself and duplicate pending or accepted relationships.

Request body:

```json
{
  "addresseeId": "uuid"
}
```

Frontend example:

```js
const request = await apiPost("/api/friend-requests", {
  addresseeId: userId
}, accessToken);
```

Response body:

```json
{
  "id": "uuid",
  "requester": {
    "id": "uuid",
    "fullName": "Alice Dupont",
    "photo": null,
    "role": "PARENT"
  },
  "addressee": {
    "id": "uuid",
    "fullName": "Bob Martin",
    "photo": null,
    "role": "PARENT"
  },
  "status": "PENDING",
  "createdAt": "2026-05-20T13:00:00",
  "updatedAt": "2026-05-20T13:00:00"
}
```

### GET /api/friend-requests/incoming

Returns pending friend requests sent to the current authenticated user.

```js
const incoming = await apiGet("/api/friend-requests/incoming", accessToken);
```

Response body: `FriendRequestResponse[]`.

### GET /api/friend-requests/outgoing

Returns pending friend requests sent by the current authenticated user.

```js
const outgoing = await apiGet("/api/friend-requests/outgoing", accessToken);
```

Response body: `FriendRequestResponse[]`.

### POST /api/friend-requests/{requestId}/accept

Accepts an incoming pending friend request. Only the addressee can accept it.

```js
const accepted = await apiPost(`/api/friend-requests/${requestId}/accept`, {}, accessToken);
```

Response body: `FriendRequestResponse` with `status: "ACCEPTED"`.

### POST /api/friend-requests/{requestId}/reject

Rejects an incoming pending friend request. Only the addressee can reject it.

```js
const rejected = await apiPost(`/api/friend-requests/${requestId}/reject`, {}, accessToken);
```

Response body: `FriendRequestResponse` with `status: "REJECTED"`.

### DELETE /api/friend-requests/{requestId}

Cancels an outgoing pending friend request. Only the requester can cancel it.

```js
await apiDelete(`/api/friend-requests/${requestId}`, accessToken);
```

Response body: empty, status `204 No Content`.

### DELETE /api/friends/{friendUserId}

Removes an accepted friend relationship.

```js
await apiDelete(`/api/friends/${friendUserId}`, accessToken);
```

Response body: empty, status `204 No Content`.

## Chats

All chat endpoints are protected. The first version is REST-based direct chat. The frontend can poll `GET /api/chats/{conversationId}/messages` to refresh messages.

Regular direct chats can only be created between accepted friends. Event-based
direct chats can be created between an event organizer and a confirmed attendee
for that event. Message read and write access is limited to conversation
participants.

### GET /api/chats

Returns the current authenticated user's chat conversations.

```js
const chats = await apiGet("/api/chats", accessToken);
```

Response body:

```json
[
  {
    "id": "uuid",
    "type": "DIRECT",
    "event": null,
    "participants": [
      {
        "id": "uuid",
        "fullName": "Alice Dupont",
        "photo": null,
        "role": "PARENT"
      },
      {
        "id": "uuid",
        "fullName": "Bob Martin",
        "photo": null,
        "role": "PARENT"
      }
    ],
    "lastMessage": {
      "id": "uuid",
      "conversationId": "uuid",
      "sender": {
        "id": "uuid",
        "fullName": "Bob Martin",
        "photo": null,
        "role": "PARENT"
      },
      "content": "Hello",
      "messageType": "TEXT",
      "createdAt": "2026-05-20T13:00:00",
      "editedAt": null,
      "deletedAt": null
    },
    "unreadCount": 1,
    "createdAt": "2026-05-20T13:00:00",
    "updatedAt": "2026-05-20T13:01:00"
  }
]
```

### POST /api/chats/direct

Gets or creates a direct chat with a friend.

Request body:

```json
{
  "userId": "uuid"
}
```

Frontend example:

```js
const chat = await apiPost("/api/chats/direct", {
  userId: friendUserId
}, accessToken);
```

Response body: `ChatConversationResponse`.

### POST /api/chats/events/{eventId}/direct

Gets or creates an event-based direct chat between the event organizer and one
confirmed attendee. This does not require the users to be friends.

Rules:

- The event organizer can create/open a chat with a confirmed attendee.
- A confirmed attendee can create/open a chat with the event organizer.
- Attendee-to-attendee event chats are not allowed.
- Users unrelated to the event receive `403 Forbidden`.

Request body:

```json
{
  "userId": "uuid"
}
```

Frontend examples:

```js
// Organizer opens a chat with one attendee.
const chat = await apiPost(`/api/chats/events/${eventId}/direct`, {
  userId: attendeeUserId
}, accessToken);

// Attendee opens a chat with the organizer.
const chat = await apiPost(`/api/chats/events/${eventId}/direct`, {
  userId: organizerUserId
}, accessToken);
```

Response body: `ChatConversationResponse`.

For event-based chats, `ChatConversationResponse.event` is an
`EventSummaryResponse`:

```json
{
  "id": "uuid",
  "type": "DIRECT",
  "event": {
    "id": "uuid",
    "title": "Music Night",
    "category": "concert",
    "startTime": "2026-05-10T18:00:00",
    "endTime": "2026-05-10T20:00:00",
    "status": "PUBLISHED"
  },
  "participants": [],
  "lastMessage": null,
  "unreadCount": 0,
  "createdAt": "2026-05-20T13:00:00",
  "updatedAt": "2026-05-20T13:00:00"
}
```

### GET /api/chats/{conversationId}/messages

Returns messages for one conversation, newest first. Use `before` for pagination.

Optional query params:

```text
before=2026-05-20T13:00:00
limit=30
```

Frontend example:

```js
const params = new URLSearchParams({ limit: "30" });
const messages = await apiGet(`/api/chats/${conversationId}/messages?${params}`, accessToken);
```

Response body:

```json
[
  {
    "id": "uuid",
    "conversationId": "uuid",
    "sender": {
      "id": "uuid",
      "fullName": "Alice Dupont",
      "photo": null,
      "role": "PARENT"
    },
    "content": "Hello",
    "messageType": "TEXT",
    "createdAt": "2026-05-20T13:00:00",
    "editedAt": null,
    "deletedAt": null
  }
]
```

### POST /api/chats/{conversationId}/messages

Sends a message. `messageType` is optional and defaults to `TEXT`. Allowed values are `TEXT`, `IMAGE`, and `SYSTEM`.

Request body:

```json
{
  "content": "Hello",
  "messageType": "TEXT"
}
```

Frontend example:

```js
const message = await apiPost(`/api/chats/${conversationId}/messages`, {
  content: text,
  messageType: "TEXT"
}, accessToken);
```

Response body: `ChatMessageResponse`.

### POST /api/chats/{conversationId}/read

Marks the conversation as read. If `messageId` is omitted, the latest message in the conversation is used.

Request body:

```json
{
  "messageId": "uuid"
}
```

Frontend example:

```js
const chat = await apiPost(`/api/chats/${conversationId}/read`, {
  messageId: latestVisibleMessageId
}, accessToken);
```

Response body: `ChatConversationResponse`.

## Notifications

All notification endpoints are protected. Notifications are created by backend business events such as friend requests, chat messages, event registrations, reviews, event updates/cancellations, and automatic event reminders.

The first frontend version can use polling:

- Call `GET /api/notifications/unread-count` every 30-60 seconds while the user is logged in.
- Call `GET /api/notifications?status=unread&limit=20` when opening the notification dropdown.
- Call `POST /api/notifications/{id}/read` when the user opens or clicks one notification.
- Call `POST /api/notifications/read-all` for a "mark all as read" action.

Notification `type` values:

```text
FRIEND_REQUEST_RECEIVED
FRIEND_REQUEST_ACCEPTED
FRIEND_REQUEST_REJECTED
CHAT_MESSAGE_RECEIVED
EVENT_REGISTRATION_CREATED
EVENT_REGISTRATION_STATUS_CHANGED
POST_REVIEW_CREATED
EVENT_REVIEW_CREATED
EVENT_UPDATED
EVENT_CANCELLED
EVENT_STARTS_IN_1_DAY
EVENT_STARTS_IN_2_HOURS
```

Common `targetType` values:

```text
FRIEND_REQUEST
USER
CHAT_CONVERSATION
EVENT
REGISTRATION
POST
```

Use `targetType` and `targetId` to navigate after a click:

- `FRIEND_REQUEST` -> friend request page or incoming requests panel.
- `USER` -> user profile or friends page.
- `CHAT_CONVERSATION` -> chat page with `targetId` as conversation id.
- `EVENT` -> event detail page with `targetId` as event id.
- `REGISTRATION` -> registration/booking detail if the frontend has that page.
- `POST` -> post detail page with `targetId` as post id.

### GET /api/notifications

Returns current user's notifications, newest first.

Optional query params:

```text
status=all
status=unread
status=read
type=CHAT_MESSAGE_RECEIVED
before=2026-05-21T10:00:00
limit=20
```

`status` defaults to `all`. `limit` defaults to `20` and is capped at `100`. Use `before` with the last loaded notification's `createdAt` for pagination.

Frontend example:

```js
const params = new URLSearchParams({
  status: "unread",
  limit: "20"
});

const notifications = await apiGet(`/api/notifications?${params}`, accessToken);
```

Response body:

```json
[
  {
    "id": "uuid",
    "type": "CHAT_MESSAGE_RECEIVED",
    "title": "New message",
    "body": "Alice Dupont sent you a message",
    "actor": {
      "id": "uuid",
      "fullName": "Alice Dupont",
      "photo": "https://project.supabase.co/storage/v1/object/public/images/userAvatar/user-id/avatar.jpg",
      "role": "PARENT"
    },
    "targetType": "CHAT_CONVERSATION",
    "targetId": "conversation-uuid",
    "sourceType": "CHAT_MESSAGE",
    "sourceId": "message-uuid",
    "payload": {
      "conversationId": "conversation-uuid",
      "messageId": "message-uuid"
    },
    "readAt": null,
    "archivedAt": null,
    "createdAt": "2026-05-21T10:00:00"
  }
]
```

Event reminder example:

```json
{
  "id": "uuid",
  "type": "EVENT_STARTS_IN_2_HOURS",
  "title": "Event starts soon",
  "body": "Music Night starts in 2 hours",
  "actor": null,
  "targetType": "EVENT",
  "targetId": "event-uuid",
  "sourceType": "EVENT",
  "sourceId": "event-uuid",
  "payload": {
    "eventId": "event-uuid",
    "eventTitle": "Music Night",
    "startTime": "2026-05-21T18:00:00",
    "reminder": "2_HOURS"
  },
  "readAt": null,
  "archivedAt": null,
  "createdAt": "2026-05-21T16:00:00"
}
```

### GET /api/notifications/unread-count

Returns current user's unread notification count.

Frontend example:

```js
const { count } = await apiGet("/api/notifications/unread-count", accessToken);
```

Response body:

```json
{
  "count": 7
}
```

### POST /api/notifications/{id}/read

Marks one notification as read. Only the notification recipient can read it.

Frontend example:

```js
const notification = await apiPost(`/api/notifications/${notificationId}/read`, {}, accessToken);
```

Response body: `NotificationResponse` with `readAt` set.

### POST /api/notifications/read-all

Marks all current user's notifications as read. The request body is optional.

Request body to mark everything:

```json
{}
```

Request body to mark only one type:

```json
{
  "type": "CHAT_MESSAGE_RECEIVED"
}
```

Frontend example:

```js
const { count } = await apiPost("/api/notifications/read-all", {}, accessToken);
```

Response body:

```json
{
  "count": 0
}
```

### DELETE /api/notifications/{id}

Archives one notification. It is hidden from future `GET /api/notifications` responses.

Frontend example:

```js
await apiDelete(`/api/notifications/${notificationId}`, accessToken);
```

Response body: empty, status `204 No Content`.

## Events

### GET /api/events

Public.

Returns the main activity page events using the precomputed recommendation
score stored on each event. Keyword still filters matching events, but keyword
relevance is not part of the precomputed score.

Optional query params:

```text
keyword=music
category=concert
status=PUBLISHED
locationId=uuid
upcomingOnly=true
limit=20
```

Frontend example:

```js
const params = new URLSearchParams({
  keyword: "music",
  upcomingOnly: "true",
  limit: "20"
});

const res = await fetch(`http://localhost:9192/api/events?${params}`);
const events = await res.json();
```

Response body:

```json
[
  {
    "id": "uuid",
    "title": "Music Night",
    "description": "Live music event",
    "category": "concert",
    "startTime": "2026-05-10T18:00:00",
    "endTime": "2026-05-10T20:00:00",
    "capacity": 100,
    "price": 10.0,
    "isVirtual": false,
    "status": "PUBLISHED",
    "organizer": {
      "id": "uuid",
      "fullName": "Alice Dupont",
      "role": "ORGANIZER"
    },
    "location": {
      "id": "uuid",
      "name": "ISEP",
      "address": "10 Rue de Vanves",
      "city": "Paris",
      "country": "France",
      "latitude": 48.84,
      "longitude": 2.29
    },
    "imageUrls": [],
    "coverImageUrl": "https://example.com/image.jpg",
    "averageRating": 4.5,
    "reviewCount": 3,
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  }
]
```

### GET /api/events/{id}

Public. Returns one `EventResponse`.

### GET /api/events/search

Public. Returns `EventResponse[]`.

Searches events by keyword and optional filters. `locationId` is the preferred
location filter. `location` is also accepted as an alias when the value is a
location UUID. `date` uses `YYYY-MM-DD` and returns events whose time range
overlaps that day. `activityType` matches the event `category`.

Empty filter params are ignored. The values `all`, `any`, `default`, and `none`
are also ignored for `locationId`, `location`, `date`, `activityType`, and
`accessibilityOptions`. When no effective filter or keyword is provided, this
endpoint returns the same recommendation-ranked default event list as
`GET /api/events`.

Optional query params:

```text
keyword=music
locationId=uuid
location=uuid
date=2026-05-10
activityType=concert
accessibilityOptions=wheelchairAccessible
accessibilityOptions=hasElevator
accessibilityOptions=accessibleToilet
accessibilityOptions=quietEnvironment
accessibilityOptions=stepFreeAccess
```

`accessibilityOptions` can be sent multiple times or as a comma-separated list:

```text
/api/events/search?activityType=concert&locationId=uuid&date=2026-05-10&accessibilityOptions=wheelchairAccessible,stepFreeAccess
```

Default filter example:

```text
/api/events/search?locationId=all&date=all&activityType=all&accessibilityOptions=all
```

### GET /api/events/organizer/{organizerId}

Public. Returns `EventResponse[]`.

### GET /api/events/location/{locationId}

Public. Returns `EventResponse[]`.

### POST /api/events/{eventId}/save

Protected. Saves an event for the current authenticated user. This is a
bookmark/favorite action and is separate from event registrations.

Frontend example:

```js
const saved = await apiPost(`/api/events/${eventId}/save`, {}, accessToken);
```

Response body:

```json
{
  "id": "uuid",
  "event": {
    "id": "uuid",
    "title": "Music Night",
    "category": "concert",
    "startTime": "2026-05-10T18:00:00",
    "endTime": "2026-05-10T20:00:00",
    "status": "PUBLISHED"
  },
  "createdAt": "2026-05-03T12:00:00"
}
```

### GET /api/events/{eventId}/save

Protected. Returns whether the current authenticated user has saved the event.

Response body:

```json
{
  "eventId": "uuid",
  "saved": true
}
```

### DELETE /api/events/{eventId}/save

Protected. Removes the current authenticated user's saved event record.

Response body: empty, status `204 No Content`.

### GET /api/users/me/saved-events

Protected. Returns the current authenticated user's saved events ordered by save
time descending.

Response body: `EventSaveResponse[]`.

### POST /api/events

Protected. Current user must have role `ORGANIZER` or `ADMIN`.

Request body:

```json
{
  "title": "Music Night",
  "description": "Live music event",
  "category": "concert",
  "startTime": "2026-05-10T18:00:00",
  "endTime": "2026-05-10T20:00:00",
  "capacity": 100,
  "price": 10.0,
  "isVirtual": false,
  "status": "PUBLISHED",
  "locationId": "uuid"
}
```

Response body: `EventResponse`.

### PUT /api/events/{id}

Protected. Current user must be an admin or the event organizer.

Request body: same as `EventRequest`.

Response body: `EventResponse`.

### DELETE /api/events/{id}

Protected. Current user must be an admin or the event organizer.

Response body: empty, status `204 No Content`.

## Event Images

### GET /api/events/{eventId}/images

Public.

Response body:

```json
[
  {
    "id": "uuid",
    "imageUrl": "https://example.com/event.jpg",
    "createdAt": "2026-05-03T12:00:00"
  }
]
```

### POST /api/events/{eventId}/images JSON

Protected. Adds an existing image URL without uploading a file.

Request body:

```json
{
  "imageUrl": "https://example.com/event.jpg"
}
```

Response body: `ImageResponse`.

### POST /api/events/{eventId}/images multipart

Protected. With `multipart/form-data`, uploads the file to Supabase Storage under `images/eventImages/{eventId}/...`, saves the public URL in `event_images.image_url`, and returns `ImageResponse`.

Request body: `multipart/form-data`

```text
file=<image file>
```

Frontend example:

```js
const formData = new FormData();
formData.append("file", file);

const res = await fetch(`http://localhost:9192/api/events/${eventId}/images`, {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${accessToken}`
  },
  body: formData
});

const image = await res.json();
```

Response body:

```json
{
  "id": "uuid",
  "imageUrl": "https://project.supabase.co/storage/v1/object/public/images/eventImages/event-id/image.jpg",
  "createdAt": "2026-05-03T12:00:00"
}
```

### DELETE /api/events/{eventId}/images/{imageId}

Protected. Deletes the image database record. Response body empty, status `204 No Content`.

## Event Reviews

### GET /api/events/{eventId}/reviews

Public.

Response body:

```json
[
  {
    "id": "uuid",
    "rating": 5,
    "comment": "Great event",
    "user": {
      "id": "uuid",
      "fullName": "Alice Dupont",
      "role": "USER"
    },
    "createdAt": "2026-05-03T12:00:00"
  }
]
```

### POST /api/events/{eventId}/reviews

Protected. Current user must have a `CONFIRMED` registration for the event, and
the event must already be ended.

Request body:

```json
{
  "rating": 5,
  "comment": "Great event"
}
```

Response body: `ReviewResponse`.

### PUT /api/events/{eventId}/reviews/{reviewId}

Protected. Current user must be the review author, an admin, or the organizer
of the reviewed event.

Request body: same as `ReviewRequest`.

Response body: `ReviewResponse`.

### DELETE /api/events/{eventId}/reviews/{reviewId}

Protected. Current user must be the review author, an admin, or the organizer
of the reviewed event. Response body empty, status `204 No Content`.

## Locations

### GET /api/locations

Public.

Response body:

```json
[
  {
    "id": "uuid",
    "name": "ISEP",
    "description": "Engineering school",
    "address": "10 Rue de Vanves",
    "city": "Paris",
    "country": "France",
    "latitude": 48.84,
    "longitude": 2.29,
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  }
]
```

### GET /api/locations/{id}

Public. Returns one `LocationResponse`.

### GET /api/locations/search?keyword=paris

Public. Returns `LocationResponse[]`.

The response shape is unchanged, but the order now uses the location recommendation score first. The score currently considers keyword relevance, linked event/post activity, accessibility flags, image count, and profile completeness. If two locations have the same score, they are ordered by name, then by id.

### POST /api/locations

Protected.

Request body:

```json
{
  "name": "ISEP",
  "description": "Engineering school",
  "address": "10 Rue de Vanves",
  "city": "Paris",
  "country": "France",
  "latitude": 48.84,
  "longitude": 2.29
}
```

Response body: `LocationResponse`.

### PUT /api/locations/{id}

Protected.

Request body: same as `LocationRequest`.

Response body: `LocationResponse`.

### DELETE /api/locations/{id}

Protected. Response body empty, status `204 No Content`.

## Location Images

### GET /api/locations/{locationId}/images

Public. Returns `ImageResponse[]`.

### POST /api/locations/{locationId}/images JSON

Protected. Adds an existing image URL without uploading a file.

Request body:

```json
{
  "imageUrl": "https://example.com/location.jpg"
}
```

Response body: `ImageResponse`.

### POST /api/locations/{locationId}/images multipart

Protected. With `multipart/form-data`, uploads the file to Supabase Storage under `images/locationImages/{locationId}/...`, saves the public URL in `location_images.image_url`, and returns `ImageResponse`.

Request body: `multipart/form-data`

```text
file=<image file>
```

Frontend example:

```js
const formData = new FormData();
formData.append("file", file);

const res = await fetch(`http://localhost:9192/api/locations/${locationId}/images`, {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${accessToken}`
  },
  body: formData
});

const image = await res.json();
```

Response body:

```json
{
  "id": "uuid",
  "imageUrl": "https://project.supabase.co/storage/v1/object/public/images/locationImages/location-id/image.jpg",
  "createdAt": "2026-05-03T12:00:00"
}
```

### DELETE /api/locations/{locationId}/images/{imageId}

Protected. Deletes the image database record. Response body empty, status `204 No Content`.

## Location Accessibility

### GET /api/locations/{locationId}/accessibility

Public.

Response body:

```json
{
  "id": "uuid",
  "location": {
    "id": "uuid",
    "name": "ISEP",
    "description": "Engineering school",
    "address": "10 Rue de Vanves",
    "city": "Paris",
    "country": "France",
    "latitude": 48.84,
    "longitude": 2.29,
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  },
  "wheelchairAccessible": true,
  "hasElevator": true,
  "accessibleToilet": true,
  "quietEnvironment": false,
  "stepFreeAccess": true,
  "notes": "Main entrance is step free",
  "createdAt": "2026-05-03T12:00:00",
  "updatedAt": "2026-05-03T12:00:00"
}
```

### POST /api/locations/{locationId}/accessibility

Protected.

Request body:

```json
{
  "wheelchairAccessible": true,
  "hasElevator": true,
  "accessibleToilet": true,
  "quietEnvironment": false,
  "stepFreeAccess": true,
  "notes": "Main entrance is step free"
}
```

Response body: `LocationAccessibilityResponse`.

### PUT /api/locations/{locationId}/accessibility

Protected.

Request body: same as `LocationAccessibilityRequest`.

Response body: `LocationAccessibilityResponse`.

### DELETE /api/locations/{locationId}/accessibility

Protected. Response body empty, status `204 No Content`.

## Posts

### GET /api/posts

Public.

Returns the main community feed using the precomputed recommendation score
stored on each post. Keyword still filters matching posts and related context,
but keyword relevance is not part of the precomputed score.

When `keyword` is provided, this endpoint searches post title/content plus
related `location.name`, `location.city`, `event.title`, and `event.category`.
For example, `keyword=paris` can return posts whose own text does not contain
`paris` if their related location is in Paris.

Optional query params:

```text
keyword=hello
status=PUBLISHED
locationId=uuid
eventId=uuid
limit=20
```

Response body:

```json
[
  {
    "id": "uuid",
    "title": "My post",
    "content": "Post content",
    "status": "PUBLISHED",
    "user": {
      "id": "uuid",
      "fullName": "Alice Dupont",
      "role": "USER"
    },
    "location": {
      "id": "uuid",
      "name": "ISEP",
      "description": "Engineering school",
      "address": "10 Rue de Vanves",
      "city": "Paris",
      "country": "France",
      "latitude": 48.84,
      "longitude": 2.29,
      "createdAt": "2026-05-03T12:00:00",
      "updatedAt": "2026-05-03T12:00:00"
    },
    "event": {
      "id": "uuid",
      "title": "Music Night",
      "category": "concert",
      "startTime": "2026-05-10T18:00:00",
      "endTime": "2026-05-10T20:00:00",
      "status": "PUBLISHED"
    },
    "createdAt": "2026-05-03T12:00:00",
    "updatedAt": "2026-05-03T12:00:00"
  }
]
```

### GET /api/posts/{id}

Public. Returns one `PostResponse`.

### GET /api/posts/search?keyword=hello

Public. Legacy keyword search. Returns `PostResponse[]`.

This endpoint searches only post `title` and `content`, then returns matches by
creation time. It does not search related location or event fields. For
recommendation ranking and location/event keyword matching, use
`GET /api/posts?keyword=hello`.

### GET /api/posts/user/{userId}

Public. Returns `PostResponse[]`.

### GET /api/posts/location/{locationId}

Public. Returns `PostResponse[]`.

### GET /api/posts/event/{eventId}

Public. Returns `PostResponse[]`.

### POST /api/posts

Protected.

Request body:

```json
{
  "locationId": "uuid",
  "eventId": "uuid",
  "title": "My post",
  "content": "Post content",
  "status": "PUBLISHED"
}
```

`locationId` and `eventId` can be `null`, but at least one should normally be provided by the frontend for meaningful display.

Response body: `PostResponse`.

### PUT /api/posts/{id}

Protected. Current user must be the post author, an admin, or the organizer of
the related event when the post is linked to an event.

Request body: same as `PostRequest`.

Response body: `PostResponse`.

### DELETE /api/posts/{id}

Protected. Current user must be the post author, an admin, or the organizer of
the related event when the post is linked to an event. Response body empty,
status `204 No Content`.

## Post Images

### GET /api/posts/{postId}/images

Public. Returns `ImageResponse[]`.

### POST /api/posts/{postId}/images JSON

Protected. Adds an existing image URL without uploading a file.

Request body:

```json
{
  "imageUrl": "https://example.com/post.jpg"
}
```

Response body: `ImageResponse`.

### POST /api/posts/{postId}/images multipart

Protected. With `multipart/form-data`, uploads the file to Supabase Storage under `images/postImages/{postId}/...`, saves the public URL in `post_images.image_url`, and returns `ImageResponse`.

Request body: `multipart/form-data`

```text
file=<image file>
```

Frontend example:

```js
const formData = new FormData();
formData.append("file", file);

const res = await fetch(`http://localhost:9192/api/posts/${postId}/images`, {
  method: "POST",
  headers: {
    "Authorization": `Bearer ${accessToken}`
  },
  body: formData
});

const image = await res.json();
```

Response body:

```json
{
  "id": "uuid",
  "imageUrl": "https://project.supabase.co/storage/v1/object/public/images/postImages/post-id/image.jpg",
  "createdAt": "2026-05-03T12:00:00"
}
```

### DELETE /api/posts/{postId}/images/{imageId}

Protected. Deletes the image database record. Response body empty, status `204 No Content`.

## Post Reviews

### GET /api/posts/{postId}/reviews

Public. Returns `ReviewResponse[]`.

### POST /api/posts/{postId}/reviews

Protected.

Request body:

```json
{
  "rating": 5,
  "comment": "Helpful post"
}
```

Response body: `ReviewResponse`.

### PUT /api/posts/{postId}/reviews/{reviewId}

Protected. Current user must be the review author, the post author, an admin,
or the organizer of the related event when the post is linked to an event.

Request body: same as `ReviewRequest`.

Response body: `ReviewResponse`.

### DELETE /api/posts/{postId}/reviews/{reviewId}

Protected. Current user must be the review author, the post author, an admin,
or the organizer of the related event when the post is linked to an event.
Response body empty, status `204 No Content`.

## Registrations

### POST /api/registrations

Protected.

Request body:

```json
{
  "eventId": "uuid",
  "status": "REGISTERED"
}
```

Response body:

```json
{
  "id": "uuid",
  "event": {
    "id": "uuid",
    "title": "Music Night",
    "category": "concert",
    "startTime": "2026-05-10T18:00:00",
    "endTime": "2026-05-10T20:00:00",
    "status": "PUBLISHED"
  },
  "user": {
    "id": "uuid",
    "fullName": "Alice Dupont",
    "role": "USER"
  },
  "status": "REGISTERED",
  "registeredAt": "2026-05-03T12:00:00"
}
```

### GET /api/registrations

Protected. Admin only. Returns `RegistrationResponse[]`.

### GET /api/registrations/{id}

Protected. Current user must own the registration, be an admin, or organize the
registration's event. Returns one `RegistrationResponse`.

### GET /api/registrations/event/{eventId}

Protected. Admins and the event organizer only. Returns
`RegistrationResponse[]`.

### GET /api/registrations/user/{userId}

Protected. Current user can view their own registrations. Admins can view any
user's registrations. Returns `RegistrationResponse[]`.

### PUT /api/registrations/{id}

Protected. Admins and the event organizer only. The registration `eventId`
cannot be changed.

Request body: same as `RegistrationRequest`.

Response body: `RegistrationResponse`.

### DELETE /api/registrations/{id}

Protected. Current user must own the registration, be an admin, or organize the
registration's event. Response body empty, status `204 No Content`.

## Common Frontend Helpers

Use this helper for all JSON API calls in this document.

```js
const API_BASE = "http://localhost:9192";

async function readResponseBody(res) {
  if (res.status === 204) return null;

  const text = await res.text();
  if (!text) return null;

  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

async function apiRequest(path, { method = "GET", body, token } = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...(token ? { "Authorization": `Bearer ${token}` } : {})
    },
    body: body !== undefined ? JSON.stringify(body) : undefined
  });

  const data = await readResponseBody(res);
  if (!res.ok) {
    throw {
      status: res.status,
      message: data?.message || res.statusText || "Request failed",
      body: data
    };
  }

  return data;
}

function apiGet(path, token) {
  return apiRequest(path, { token });
}

function apiPost(path, body, token) {
  return apiRequest(path, { method: "POST", body, token });
}

function apiPut(path, body, token) {
  return apiRequest(path, { method: "PUT", body, token });
}

function apiDelete(path, token) {
  return apiRequest(path, { method: "DELETE", token });
}

async function uploadImage(path, file, token) {
  const formData = new FormData();
  formData.append("file", file);

  const res = await fetch(`${API_BASE}${path}`, {
    method: "POST",
    headers: {
      ...(token ? { "Authorization": `Bearer ${token}` } : {})
    },
    body: formData
  });

  const data = await readResponseBody(res);
  if (!res.ok) {
    throw {
      status: res.status,
      message: data?.message || res.statusText || "Upload failed",
      body: data
    };
  }

  return data;
}
```

Helper applicability:

- Use `apiGet(path)` for public GET endpoints such as `/api/events`, `/api/locations`, `/api/posts`.
- Use `apiGet(path, accessToken)` for protected GET endpoints such as `/api/users/me`, `/api/users/{id}`, `/api/users/search`, `/api/friends`, `/api/chats`, `/api/notifications`, `/api/registrations`, `/api/auth/debug`.
- Use `apiPost(path, body, accessToken)` for protected create/action endpoints such as `/api/friend-requests`, `/api/friend-requests/{id}/accept`, `/api/chats/direct`, and `/api/chats/{id}/messages`.
- Use `apiPut(path, body, accessToken)` for protected update endpoints.
- Use `apiDelete(path, accessToken)` for protected delete endpoints; it returns `null` on `204 No Content`.
- Use `uploadImage(path, file, accessToken)` for multipart upload endpoints such as `/api/users/me/avatar`, `/api/events/{eventId}/images`, `/api/locations/{locationId}/images`, and `/api/posts/{postId}/images`.

Endpoints that should not use these helpers:

- Static pages such as `/login.html`, `/signup.html`, `/me.html`; open them in the browser as pages.
- Swagger UI `/swagger-ui.html`; open it in the browser.
- Raw files or downloads if added later; these helpers assume JSON or empty response.
