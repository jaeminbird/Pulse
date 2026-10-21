# API Documentation - CI/CD Pipeline Dashboard

This document provides detailed information about all available API endpoints.

## Base URL

```
http://localhost:8080
```

## Table of Contents

- [Build Management](#build-management)
- [Repository Management](#repository-management)
- [Pipeline Management](#pipeline-management)
- [GitHub Actions Integration](#github-actions-integration)
- [Error Responses](#error-responses)

---

## Build Management

### Get All Builds

Retrieve all builds in the system.

**Endpoint:** `GET /api/builds`

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "repositoryId": 1,
    "repositoryName": "backend-api",
    "status": "SUCCESS",
    "commitSha": "a1b2c3d4e5f6...",
    "startedAt": "2025-10-25T10:30:00",
    "completedAt": "2025-10-25T10:45:00"
  },
  {
    "id": 2,
    "repositoryId": 1,
    "repositoryName": "backend-api",
    "status": "IN_PROGRESS",
    "commitSha": "b2c3d4e5f6g7...",
    "startedAt": "2025-10-25T11:00:00",
    "completedAt": null
  }
]
```

### Get Build by ID

Retrieve details of a specific build.

**Endpoint:** `GET /api/builds/{id}`

**Parameters:**
- `id` (path) - Build ID

**Response:** `200 OK`

```json
{
  "id": 1,
  "repositoryId": 1,
  "repositoryName": "backend-api",
  "status": "SUCCESS",
  "commitSha": "a1b2c3d4e5f6...",
  "startedAt": "2025-10-25T10:30:00",
  "completedAt": "2025-10-25T10:45:00"
}
```

**Error Responses:**
- `404 Not Found` - Build not found

### Get Builds by Repository

Retrieve all builds for a specific repository.

**Endpoint:** `GET /api/builds/repository/{repositoryId}`

**Parameters:**
- `repositoryId` (path) - Repository ID

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "repositoryId": 1,
    "repositoryName": "backend-api",
    "status": "SUCCESS",
    "commitSha": "a1b2c3d4e5f6...",
    "startedAt": "2025-10-25T10:30:00",
    "completedAt": "2025-10-25T10:45:00"
  }
]
```

### Get Builds by Status

Retrieve all builds with a specific status.

**Endpoint:** `GET /api/builds/status/{status}`

**Parameters:**
- `status` (path) - Build status (PENDING, IN_PROGRESS, SUCCESS, FAILED, CANCELLED)

**Example:**
```bash
curl http://localhost:8080/api/builds/status/SUCCESS
```

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "repositoryId": 1,
    "repositoryName": "backend-api",
    "status": "SUCCESS",
    "commitSha": "a1b2c3d4e5f6...",
    "startedAt": "2025-10-25T10:30:00",
    "completedAt": "2025-10-25T10:45:00"
  }
]
```

**Error Responses:**
- `400 Bad Request` - Invalid status value

### Sync Builds from GitHub

Trigger synchronization of workflow runs from GitHub Actions API to the database.

**Endpoint:** `POST /api/builds/sync`

**Request Body:**

```json
{
  "owner": "microsoft",
  "repo": "vscode",
  "repositoryId": 1
}
```

**Fields:**
- `owner` (required) - GitHub repository owner
- `repo` (required) - GitHub repository name
- `repositoryId` (required) - Local database repository ID

**Response:** `200 OK`

```json
{
  "success": true,
  "syncedCount": 15,
  "message": "Successfully synced 15 builds from GitHub",
  "repositoryName": "backend-api"
}
```

**Error Responses:**

- `400 Bad Request` - Invalid request or repository not found
```json
{
  "success": false,
  "syncedCount": 0,
  "message": "Repository not found with ID: 1",
  "repositoryName": null
}
```

- `401 Unauthorized` - Invalid GitHub token
```json
{
  "success": false,
  "syncedCount": 0,
  "message": "GitHub API error: Invalid authentication token",
  "repositoryName": null
}
```

- `404 Not Found` - Repository not found on GitHub
```json
{
  "success": false,
  "syncedCount": 0,
  "message": "GitHub API error: Repository not found",
  "repositoryName": null
}
```

**Example:**

```bash
curl -X POST http://localhost:8080/api/builds/sync \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "microsoft",
    "repo": "vscode",
    "repositoryId": 1
  }'
```

### Create Build

Create a new build manually.

**Endpoint:** `POST /api/builds?repositoryId={repositoryId}`

**Parameters:**
- `repositoryId` (query) - Repository ID

**Request Body:**

```json
{
  "status": "PENDING",
  "commitSha": "a1b2c3d4e5f6...",
  "startedAt": "2025-10-25T10:30:00"
}
```

**Response:** `201 Created`

```json
{
  "id": 3,
  "repositoryId": 1,
  "repositoryName": "backend-api",
  "status": "PENDING",
  "commitSha": "a1b2c3d4e5f6...",
  "startedAt": "2025-10-25T10:30:00",
  "completedAt": null
}
```

**Error Responses:**
- `400 Bad Request` - Invalid request or repository not found

### Update Build

Update an existing build.

**Endpoint:** `PUT /api/builds/{id}`

**Parameters:**
- `id` (path) - Build ID

**Request Body:**

```json
{
  "status": "SUCCESS",
  "completedAt": "2025-10-25T10:45:00"
}
```

**Response:** `200 OK`

```json
{
  "id": 1,
  "repositoryId": 1,
  "repositoryName": "backend-api",
  "status": "SUCCESS",
  "commitSha": "a1b2c3d4e5f6...",
  "startedAt": "2025-10-25T10:30:00",
  "completedAt": "2025-10-25T10:45:00"
}
```

**Error Responses:**
- `404 Not Found` - Build not found

### Delete Build

Delete a build.

**Endpoint:** `DELETE /api/builds/{id}`

**Parameters:**
- `id` (path) - Build ID

**Response:** `204 No Content`

**Error Responses:**
- `404 Not Found` - Build not found

---

## Repository Management

### Get All Repositories

**Endpoint:** `GET /api/repositories`

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "backend-api",
    "githubUrl": "https://github.com/example/backend-api",
    "createdAt": "2025-10-25T09:00:00"
  }
]
```

### Create Repository

**Endpoint:** `POST /api/repositories`

**Request Body:**

```json
{
  "name": "my-app",
  "githubUrl": "https://github.com/myorg/my-app"
}
```

**Response:** `201 Created`

```json
{
  "id": 1,
  "name": "my-app",
  "githubUrl": "https://github.com/myorg/my-app",
  "createdAt": "2025-10-25T10:00:00"
}
```

`githubUrl` may also be `owner/repo` shorthand, and `name` is optional (defaults to `owner/repo`). Workflow runs are synced right away. Returns `400` if the repository is already monitored or doesn't exist on GitHub.

### Get Dashboard Summary

**Endpoint:** `GET /api/repositories/summary`

Returns every repository with its latest build, its last 10 builds (newest first), and the success rate across the finished ones.

```json
[
  {
    "repository": { "id": 1, "name": "angular/angular-cli", "githubUrl": "https://github.com/angular/angular-cli", "createdAt": "2026-10-21T20:59:55" },
    "latestBuild": { "id": 12, "status": "IN_PROGRESS", "workflowName": "CI", "branch": "main", "commitSha": "3db6585...", "commitMessage": "build: update dependencies", "authorName": "Angular Robot", "htmlUrl": "https://github.com/angular/angular-cli/actions/runs/39674381174", "startedAt": "2026-10-21T20:54:44", "completedAt": null },
    "recentBuilds": [ "..." ],
    "successRate": 89
  }
]
```

All timestamps are UTC.

### Sync One Repository

**Endpoint:** `POST /api/repositories/{id}/sync`

**Response:** `200 OK` with a `SyncResponse` (`syncedCount` = builds created or updated).

### Delete Repository

**Endpoint:** `DELETE /api/repositories/{id}`

Stops monitoring the repository and deletes its builds. **Response:** `204 No Content`

---

## Pipeline Management

### Get All Pipelines

**Endpoint:** `GET /api/pipelines`

### Get Pipeline by ID

**Endpoint:** `GET /api/pipelines/{id}`

### Create Pipeline

**Endpoint:** `POST /api/pipelines`

**Request Body:**

```json
{
  "name": "my-app-pipeline",
  "description": "Main application build pipeline",
  "repository": "https://github.com/example/my-app",
  "branch": "main",
  "status": "PENDING"
}
```

---

## GitHub Actions Integration

### Get Latest Build Status

**Endpoint:** `GET /api/github/status/{owner}/{repo}`

**Example:**
```bash
curl http://localhost:8080/api/github/status/microsoft/vscode
```

**Response:** `200 OK`

```json
{
  "runId": 1234567890,
  "repositoryName": "vscode",
  "branch": "main",
  "commitSha": "abc123...",
  "status": "completed",
  "conclusion": "success",
  "runNumber": 1234,
  "startedAt": "2025-10-25T10:30:00",
  "updatedAt": "2025-10-25T10:45:00",
  "htmlUrl": "https://github.com/microsoft/vscode/actions/runs/1234567890",
  "commitMessage": "Fix build issue",
  "authorName": "John Doe"
}
```

For more details, see [GITHUB_INTEGRATION.md](GITHUB_INTEGRATION.md).

---

## Error Responses

All error responses follow a consistent format:

### Error Response Structure

```json
{
  "timestamp": "2025-10-25T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Repository not found with ID: 999",
  "path": "/api/builds/sync",
  "details": []
}
```

**Fields:**
- `timestamp` - When the error occurred
- `status` - HTTP status code
- `error` - HTTP status text
- `message` - Detailed error message
- `path` - Request path that caused the error
- `details` - Additional error details (for validation errors)

### Common HTTP Status Codes

| Status Code | Description |
|------------|-------------|
| 200 | Success |
| 201 | Created |
| 204 | No Content |
| 400 | Bad Request - Invalid input |
| 401 | Unauthorized - Invalid credentials |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Server Error - Server error |

### Validation Error Example

```json
{
  "timestamp": "2025-10-25T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "path": "/api/builds/sync",
  "details": [
    "owner: must not be blank",
    "repositoryId: must not be null"
  ]
}
```

---

## Build Status Values

| Status | Description |
|--------|-------------|
| PENDING | Build is queued |
| IN_PROGRESS | Build is currently running |
| SUCCESS | Build completed successfully |
| FAILED | Build failed |
| CANCELLED | Build was cancelled |

---

## Testing the API

### Using curl

```bash
# Get all builds
curl http://localhost:8080/api/builds

# Get build by ID
curl http://localhost:8080/api/builds/1

# Get builds by status
curl http://localhost:8080/api/builds/status/SUCCESS

# Sync builds from GitHub
curl -X POST http://localhost:8080/api/builds/sync \
  -H "Content-Type: application/json" \
  -d '{
    "owner": "microsoft",
    "repo": "vscode",
    "repositoryId": 1
  }'

# Create a build
curl -X POST "http://localhost:8080/api/builds?repositoryId=1" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PENDING",
    "commitSha": "abc123def456",
    "startedAt": "2025-10-25T10:30:00"
  }'
```

### Using Postman

1. Import the endpoints from this documentation
2. Set base URL to `http://localhost:8080`
3. For POST requests, set `Content-Type: application/json` header
4. For GitHub integration, ensure `GITHUB_TOKEN` environment variable is set

### Using HTTPie

```bash
# Get all builds
http GET :8080/api/builds

# Sync builds
http POST :8080/api/builds/sync \
  owner=microsoft \
  repo=vscode \
  repositoryId:=1
```

---

## Rate Limiting

Currently, there are no rate limits on the local API endpoints. However, GitHub API has rate limits:

- Authenticated requests: 5,000 per hour
- Unauthenticated requests: 60 per hour

See [GITHUB_INTEGRATION.md](GITHUB_INTEGRATION.md) for more details on GitHub API limits.

---

## Authentication

Currently, the API does not require authentication. In production, consider implementing:

- JWT-based authentication
- API key authentication
- OAuth 2.0

---

## Support

For questions or issues, please refer to the main [README.md](README.md) or contact the development team.
