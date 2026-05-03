# Frontend API Guide

Base URL in local development:

```text
http://localhost:9192
```

All request and response bodies are JSON. For protected endpoints, add:

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
- Protected `GET` endpoints still need `Authorization`, for example `/api/users/me`, `/api/users/{id}`, `/api/registrations`, and `/api/auth/debug`.
- `DELETE` endpoints return `204 No Content`, so the frontend must not call `res.json()` for successful delete responses.
- Spring Security `401/403` responses may not always use the custom JSON error shape. The helper at the end of this document handles that.

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
  "role": "ORGANIZER",
  "createdAt": "2026-05-03T12:00:00",
  "updatedAt": "2026-05-03T12:00:00"
}
```

### PUT /api/users/me

Protected.

Request body:

```json
{
  "fullName": "Alice Martin",
  "phone": "+33123456789"
}
```

Response body: `UserProfileResponse`.

### GET /api/users/{id}

Protected.

Response body:

```json
{
  "id": "uuid",
  "fullName": "Alice Dupont",
  "role": "ORGANIZER"
}
```

## Events

### GET /api/events

Public.

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

### GET /api/events/search?keyword=music

Public. Returns `EventResponse[]`.

### GET /api/events/organizer/{organizerId}

Public. Returns `EventResponse[]`.

### GET /api/events/location/{locationId}

Public. Returns `EventResponse[]`.

### POST /api/events

Protected. Current user must have role `ORGANIZER`.

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

Protected. Current user must be the event organizer.

Request body: same as `EventRequest`.

Response body: `EventResponse`.

### DELETE /api/events/{id}

Protected. Current user must be the event organizer.

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

### POST /api/events/{eventId}/images

Protected.

Request body:

```json
{
  "imageUrl": "https://example.com/event.jpg"
}
```

Response body: `ImageResponse`.

### DELETE /api/events/{eventId}/images/{imageId}

Protected. Response body empty, status `204 No Content`.

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

Protected.

Request body:

```json
{
  "rating": 5,
  "comment": "Great event"
}
```

Response body: `ReviewResponse`.

### PUT /api/events/{eventId}/reviews/{reviewId}

Protected.

Request body: same as `ReviewRequest`.

Response body: `ReviewResponse`.

### DELETE /api/events/{eventId}/reviews/{reviewId}

Protected. Response body empty, status `204 No Content`.

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

### POST /api/locations/{locationId}/images

Protected.

Request body:

```json
{
  "imageUrl": "https://example.com/location.jpg"
}
```

Response body: `ImageResponse`.

### DELETE /api/locations/{locationId}/images/{imageId}

Protected. Response body empty, status `204 No Content`.

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

Public. Returns `PostResponse[]`.

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

Protected.

Request body: same as `PostRequest`.

Response body: `PostResponse`.

### DELETE /api/posts/{id}

Protected. Response body empty, status `204 No Content`.

## Post Images

### GET /api/posts/{postId}/images

Public. Returns `ImageResponse[]`.

### POST /api/posts/{postId}/images

Protected.

Request body:

```json
{
  "imageUrl": "https://example.com/post.jpg"
}
```

Response body: `ImageResponse`.

### DELETE /api/posts/{postId}/images/{imageId}

Protected. Response body empty, status `204 No Content`.

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

Protected.

Request body: same as `ReviewRequest`.

Response body: `ReviewResponse`.

### DELETE /api/posts/{postId}/reviews/{reviewId}

Protected. Response body empty, status `204 No Content`.

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

Protected. Returns `RegistrationResponse[]`.

### GET /api/registrations/{id}

Protected. Returns one `RegistrationResponse`.

### GET /api/registrations/event/{eventId}

Protected. Returns `RegistrationResponse[]`.

### GET /api/registrations/user/{userId}

Protected. Returns `RegistrationResponse[]`.

### PUT /api/registrations/{id}

Protected.

Request body: same as `RegistrationRequest`.

Response body: `RegistrationResponse`.

### DELETE /api/registrations/{id}

Protected. Response body empty, status `204 No Content`.

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
```

Helper applicability:

- Use `apiGet(path)` for public GET endpoints such as `/api/events`, `/api/locations`, `/api/posts`.
- Use `apiGet(path, accessToken)` for protected GET endpoints such as `/api/users/me`, `/api/users/{id}`, `/api/registrations`, `/api/auth/debug`.
- Use `apiPost(path, body, accessToken)` for protected create endpoints.
- Use `apiPut(path, body, accessToken)` for protected update endpoints.
- Use `apiDelete(path, accessToken)` for protected delete endpoints; it returns `null` on `204 No Content`.

Endpoints that should not use these helpers:

- Static pages such as `/login.html`, `/signup.html`, `/me.html`; open them in the browser as pages.
- Swagger UI `/swagger-ui.html`; open it in the browser.
- Raw files or downloads if added later; these helpers assume JSON or empty response.
