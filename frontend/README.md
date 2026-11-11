# The Buzz Frontend

Vue 3 + TypeScript + Vite web frontend for The Buzz

## Requirements
- Node versions `^20.19.0` or `>=22.12.0`

## Setup
- npm install

## Running locally
- npm run dev

## Builiding and Deploying
- npm run build
- npm run deploy

## Running tests
- npm run test:unit

## Routes
`/` | Redirects to `/messages` 
`/messages` | Main feed
`/messages/new` | Create a new idea
`/messages/:id` | Message detail and replies
`/pa` | All users
`/p1/:id` | User profile

## Deployment
- https://team-04.dokku.cse.lehigh.edu