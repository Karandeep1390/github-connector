# GitHub Repository Activity Connector

A scalable, production-ready Spring Boot connector for fetching GitHub repository activity data. This connector demonstrates best practices for building maintainable, resilient integrations with external APIs.

## Features

- **Scalable Architecture**: Built with Spring WebFlux for reactive, non-blocking operations
- **Comprehensive Error Handling**: Graceful handling of rate limits, authentication errors, and network issues
- **Retry Logic**: Configurable retry mechanism with exponential backoff using Resilience4j
- **REST API Interface**: Clean RESTful endpoints for integration with other applications
- **Robust Pagination**: Handles GitHub API pagination for both repositories and commits
- **Structured Data Models**: Type-safe POJOs with proper JSON mapping
- **Production Ready**: Includes logging, metrics, health checks, and configuration management

## Architecture Overview

The connector follows a layered architecture pattern with reactive programming at its core:

```
├── Controller Layer (REST endpoints)
├── Service Layer (Business logic with reactive streams)
├── Configuration Layer (Properties and WebClient setup)
├── Model Layer (POJOs for GitHub data)
├── Exception Layer (Custom exceptions and global handler)
└── WebClient (Reactive HTTP client for external API calls)
```

### Design Decision: Reactive Programming with WebFlux

This connector is built using **Spring WebFlux** and reactive programming patterns (Mono/Flux) rather than traditional blocking approaches. Here's why this architectural choice was made:

#### Why Reactive Programming?

**1. Efficient External API Integration**
```java
// Multiple GitHub API calls handled concurrently
Flux<GitHubRepository> repositories = fetchAllRepositories(username)
    .flatMap(repo -> fetchRecentCommits(repo)  // Non-blocking concurrent calls
        .map(commits -> enrichRepository(repo, commits)));
```

The GitHub connector needs to make multiple API calls:
- Fetch user repositories (1 call)
- Fetch commits for each repository (N calls)
- Handle pagination for both repositories and commits

Reactive programming allows these calls to be processed concurrently without blocking threads.

**2. Superior Scalability**
- **Blocking approach**: 1000 concurrent requests = 1000 threads (~1GB memory)
- **Reactive approach**: 1000 concurrent requests = 10-20 threads (~20MB memory)

**3. Built-in Backpressure and Error Resilience**
```java
return webClient.get()
    .uri("/users/{username}/repos", username)
    .retrieve()
    .bodyToMono(List.class)
    .timeout(Duration.ofSeconds(30))           // Non-blocking timeout
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))) // Automatic retry
    .onErrorMap(this::mapWebClientException);   // Error transformation
```
#### Reactive Operators Used

**Pagination with `expand()`:**
```java
fetchRepositoriesPage(username, 1, pageSize)
    .expand(repos -> repos.size() < pageSize ? 
        Mono.empty() : 
        fetchRepositoriesPage(username, nextPage, pageSize))
```

**Concurrent Processing with `flatMap()`:**
```java
.flatMap(repo -> fetchRecentCommits(repo, commitLimit)
    .map(commits -> new GitHubRepository(repo, commits)))
```

**Error Handling with `onErrorResume()`:**
```java
.onErrorResume(WebClientResponseException.class, ex -> 
    ex.getStatusCode() == HttpStatus.CONFLICT ? 
        Mono.just(List.of()) :  // Empty repo
        Mono.error(mapWebClientException(ex)))
```

#### When Reactive Programming Shines

✅ **Perfect for this connector because:**
- Multiple external API calls per request
- I/O-intensive operations (network calls)
- Need for high concurrency support
- Built-in retry and error handling requirements
- Scalable architecture for production use

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- GitHub Personal Access Token (for API authentication)

## Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/Karandeep1390/github-connector.git
cd github-connector
```

### 2. Configure GitHub Access Token

Create a GitHub Personal Access Token:
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate a new token with `repo` scope (for private repos) or just `public_repo` for public repositories
3. Copy the token

**Option A: Environment Variable (Recommended)**
```bash
export GITHUB_TOKEN=your_github_token_here
```

**Option B: Application Properties**
```yaml
# src/main/resources/application.yml
github:
  token: your_github_token_here
```

### 3. Build the Application

```bash
mvn clean package
```

### 4. Run the Application

**As a Web Service:**
```bash
java -jar target/github-activity-connector-1.0.0.jar
```
## Local Development Setup

### 1. Run the Application Locally

**Start the Spring Boot Application:**
```bash
# Option 1: Using Maven
mvn spring-boot:run

# Option 2: Using compiled JAR
java -jar target/github-activity-connector-1.0.0.jar
```

The application will start on `http://localhost:8080`

### 2. Verify Application is Running

Check the health endpoint:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

## API Endpoints and Usage

The connector provides two REST endpoints for fetching GitHub repository activity:

### 1. POST /api/v1/github/repository-activity

**Description:** Fetch repository activity with custom parameters via request body

**Request Body:**
```json
{
  "username": "octocat",
  "pageSize": 30,
  "commitLimit": 20
}
```

**Example curl commands:**

**Basic request:**
```bash
curl -X POST http://localhost:8080/api/v1/github/repository-activity \
  -H "Content-Type: application/json" \
  -d '{"username": "Karandeep1390"}'
```

**With custom parameters:**
```bash
curl -X POST http://localhost:8080/api/v1/github/repository-activity \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Karandeep1390", 
    "pageSize": 50, 
    "commitLimit": 10
  }'
```

### 2. GET /api/v1/github/repository-activity/{username}

**Description:** Fetch repository activity with query parameters

**Parameters:**
- `username` (path parameter, required): GitHub username
- `pageSize` (query parameter, optional): Number of repositories per page (default: 30)
- `commitLimit` (query parameter, optional): Number of recent commits per repository (default: 20)

**Example curl commands:**

**Basic request:**
```bash
curl "http://localhost:8080/api/v1/github/repository-activity/Karandeep1390"
```

**With query parameters:**
```bash
curl "http://localhost:8080/api/v1/github/repository-activity/Karandeep1390?pageSize=25&commitLimit=5"
```

### Response Format

```json
{
  "username": "Karandeep1390",
  "fetchedAt": "2025-08-17T16:29:22.768656",
  "totalRepositories": 12,
  "repositories": [
    {
      "full_name": "Karandeep1390/Anaya",
      "html_url": "https://github.com/Karandeep1390/Anaya",
      "clone_url": "https://github.com/Karandeep1390/Anaya.git",
      "created_at": "2025-06-06T04:15:34",
      "updated_at": "2025-06-18T09:42:17",
      "pushed_at": "2025-06-18T09:42:14",
      "stargazers_count": 0,
      "forks_count": 0,
      "default_branch": "main",
      "id": 997164005,
      "name": "Anaya",
      "description": "Loan Re-engagement Agentic Ai Chatbot",
      "size": 215,
      "language": "Python",
      "recentCommits": [
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/b6b9db5116018c9060757cdeaa8a1a1a0591089d",
          "sha": "b6b9db5116018c9060757cdeaa8a1a1a0591089d",
          "commit": {
            "message": "added",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:42:11"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:42:11"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/c9797a7d551a640b117f505eaebc6c596eb3a776",
          "sha": "c9797a7d551a640b117f505eaebc6c596eb3a776",
          "commit": {
            "message": "added",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:30:39"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:30:39"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/f3ae1f336991f7103a95d1692f6982078c20b532",
          "sha": "f3ae1f336991f7103a95d1692f6982078c20b532",
          "commit": {
            "message": "added",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:26:29"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T09:26:29"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/9d1bf331ccf4dbbd985d4ac7e1fc7b4f0ffcce16",
          "sha": "9d1bf331ccf4dbbd985d4ac7e1fc7b4f0ffcce16",
          "commit": {
            "message": "added",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T08:56:01"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T08:56:01"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/35fce5f7b9f3c4f365269fc6712b59f14d7f31bb",
          "sha": "35fce5f7b9f3c4f365269fc6712b59f14d7f31bb",
          "commit": {
            "message": "added",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T08:30:45"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-18T08:30:45"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/4872673d80224d94a57117621db6df2a204113e6",
          "sha": "4872673d80224d94a57117621db6df2a204113e6",
          "commit": {
            "message": "Answer only for personal loan",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-13T05:43:20"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-13T05:44:02"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/df12e39c456b80f121179e972570e538fa9f7626",
          "sha": "df12e39c456b80f121179e972570e538fa9f7626",
          "commit": {
            "message": "Added Dev Container Folder",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-11T08:36:44"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-11T08:36:44"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/Anaya/commit/52fbb4949c108e13ea1e22a3e1c37f26ac593854",
          "sha": "52fbb4949c108e13ea1e22a3e1c37f26ac593854",
          "commit": {
            "message": "Developed a streamlit application with Open AI SDK",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-06T04:16:44"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-06-06T04:16:44"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        }
      ]
    },
    {
      "full_name": "Karandeep1390/PLPAVector",
      "html_url": "https://github.com/Karandeep1390/PLPAVector",
      "clone_url": "https://github.com/Karandeep1390/PLPAVector.git",
      "created_at": "2025-01-31T10:36:57",
      "updated_at": "2025-02-08T13:24:56",
      "pushed_at": "2025-02-08T13:24:53",
      "stargazers_count": 0,
      "forks_count": 0,
      "default_branch": "main",
      "id": 925154444,
      "name": "PLPAVector",
      "description": null,
      "size": 4082,
      "language": "Python",
      "recentCommits": [
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/08482f8f1cc34430dac23622f16e6c95123c6202",
          "sha": "08482f8f1cc34430dac23622f16e6c95123c6202",
          "commit": {
            "message": "Changing prompt and vector creating strategy",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-08T13:24:45"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-08T13:24:45"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/e67e195ce3326549ea0e7acb0d1bb4a644b1ff9f",
          "sha": "e67e195ce3326549ea0e7acb0d1bb4a644b1ff9f",
          "commit": {
            "message": "Making some changes",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-02T06:23:25"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-02T06:23:25"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/c42064322e8097e5470a015624111f36ba17fe22",
          "sha": "c42064322e8097e5470a015624111f36ba17fe22",
          "commit": {
            "message": "Making some changes",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-02T06:19:51"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-02T06:19:51"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/0ea259d0e83d3985721cbf9796612eed9c4d2444",
          "sha": "0ea259d0e83d3985721cbf9796612eed9c4d2444",
          "commit": {
            "message": "Making some changes",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:17:50"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:17:50"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/97c65cc684f820856909cf5926c5e3baa64c9609",
          "sha": "97c65cc684f820856909cf5926c5e3baa64c9609",
          "commit": {
            "message": "Making some changes",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:17:16"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:17:16"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/721c69f94f77f84a2feddb3a7642afd25e2b849a",
          "sha": "721c69f94f77f84a2feddb3a7642afd25e2b849a",
          "commit": {
            "message": "Making some changes",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:14:12"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T10:14:12"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/7580a7d810d0ee9026e7aeed426b4bbc00a99ef7",
          "sha": "7580a7d810d0ee9026e7aeed426b4bbc00a99ef7",
          "commit": {
            "message": "Changing prompt",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:46:23"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:46:23"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/af119ea7a8daf15de8c3a6cbe7e1d3f16976e930",
          "sha": "af119ea7a8daf15de8c3a6cbe7e1d3f16976e930",
          "commit": {
            "message": "reverting a change",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:36:25"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:36:25"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/a16a65092c5cc9cc0bcbf364f5f8b4421b3067b8",
          "sha": "a16a65092c5cc9cc0bcbf364f5f8b4421b3067b8",
          "commit": {
            "message": "reverting a change",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:32:06"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:32:06"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        },
        {
          "html_url": "https://github.com/Karandeep1390/PLPAVector/commit/af7ebdb2676441356ba799f6ad082581f299ca3f",
          "sha": "af7ebdb2676441356ba799f6ad082581f299ca3f",
          "commit": {
            "message": "reverting a change",
            "author": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:30:42"
            },
            "committer": {
              "name": "Karandeep Singh",
              "email": "karandeepsingh1390@gmail.com",
              "date": "2025-02-01T09:30:42"
            }
          },
          "author": {
            "name": null,
            "email": null,
            "date": null
          },
          "committer": {
            "name": null,
            "email": null,
            "date": null
          }
        }
      ]
    }
  ],
  "rateLimitInfo": {
    "remaining": 5000,
    "limit": 5000,
    "resetTime": "2025-08-17T17:29:22.768665"
  }
}
```

## Configuration

The connector supports extensive configuration through `application.yml`:

```yaml
github:
  api-url: https://api.github.com
  token: ${GITHUB_TOKEN}
  default-page-size: 30
  max-retries: 3
  retry-delay-ms: 1000
  timeout-seconds: 30

resilience4j:
  retry:
    instances:
      github-api:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2

logging:
  level:
    com.connector.github: INFO
```

## Error Handling

The connector handles various error scenarios gracefully:

- **Rate Limiting**: Automatic retry with exponential backoff
- **Authentication Failures**: Clear error messages for token issues
- **Network Errors**: Timeout handling and connection retry
- **User Not Found**: Proper 404 handling
- **Empty Repositories**: Graceful handling of repos with no commits
- **Validation Errors**: Input validation with detailed error messages

### Error Response Format

```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "GitHub API rate limit exceeded. Please try again later.",
  "timestamp": "2025-01-15T10:30:00",
  "status": 429
}
```