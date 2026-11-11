# Phase 3 Backend API Spec

## Overview
All responses are in this format:
```json
{ "status": "ok/error", "message": null, "data": ... }
```
All routes require the user to be logged in via Google OAuth2 session cookies.


## Auth
- `GET /auth/google/callback` — handles Google login, sets cookies, redirects to `/`
- `POST /auth/logout` — logs out user, clears session from Memcachier


## Messages
- `GET /messages` — get all valid messages. Returns likeCount, replyCount, and userLiked (no voter info exposed)
- `GET /messages/:id` — get one message
- `POST /messages` — create a message `{ subject, details }`
- `PUT /messages/:id` — update your own message `{ subject, details }`
- `DELETE /messages/:id` — delete your own message


## Replies
- `GET /messages/:id/replies` — get all valid replies for a message. Returns likeCount and userLiked (no voter info exposed)
- `POST /messages/:id/replies` — create a reply `{ subject, details }`


## Likes
- `POST /messages/:id/like` — like a message, returns `{ likeCount, userLiked: true }`
- `DELETE /messages/:id/like` — unlike a message, returns `{ likeCount, userLiked: false }`
- `POST /replies/:id/like` — like a reply, returns `{ likeCount, userLiked: true }`
- `DELETE /replies/:id/like` — unlike a reply, returns `{ likeCount, userLiked: false }`


## User Profiles (New additions for Phase 3)
Profile fields (gender_identity, sexual_orientation, note) are stored directly in tblPerson.
- `GET /users/:id` — get a user profile
  - if you are the owner: returns `{ id, name, email, note, gender_identity, sexual_orientation }`
  - if you are not the owner: returns `{ id, name, email, note }` only
- `PUT /users/:id` — update your own profile `{ name, note, gender_identity, sexual_orientation }`
  - email cannot be changed
  - only the owner can call this

## Notes
- Session is stored in Memcachier (not in-memory HashMap anymore)
- Session key is `auth.gId` cookie value
- Invalidated users (`valid = false` in tblPerson) are rejected at login
- Voter identity is never included in any response
- GI and SO are never returned to anyone other than the profile owner
- Only profile owners can call `PUT /users/:id`