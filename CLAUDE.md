# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test

```bash
# Compile + run all tests
mvn clean test

# Package executable JAR
mvn clean package

# Run a specific test class
mvn test -Dtest=CodeReviewPromptBuilderTest

# The project requires JDK 17, configured at:
#   C:\DevelopTool\JDK17\bin\java
```

## Architecture

This is a GitLab MR AI code review bot. It receives GitLab webhook events, fetches MR diffs via GitLab API, sends them to an LLM for review, and posts the review as a MR comment.

### Request flow

```
GitLab Webhook → GitLabWebhookController (token validation, event routing)
                    → reviewTaskExecutor.execute(...)  ← async, returns 200 immediately
                    → ReviewService.handleMergeRequestEvent()
                         → GitLabApiService.getMergeRequestChanges()  (OkHttp → GitLab REST API)
                         → CodeReviewAgent.review()                    (ReAct Agent with LLM)
                         → GitLabApiService.createMergeRequestNote()  (post review comment)
```

### Key design decisions

- **Controller is thin**: validates token via `X-Gitlab-Token` header, routes by `X-Gitlab-Event` header, submits to thread pool, returns immediately. No business logic.
- **Async via explicit Executor**: Controller injects `@Qualifier("reviewTaskExecutor") Executor` and calls `reviewTaskExecutor.execute()`. NOT `@Async` — the async behavior is visible at the call site.
- **OkHttp, not RestTemplate**: `GitLabApiServiceImpl` uses OkHttp 4.12.0 for all GitLab API calls, serializing/deserializing with Jackson `ObjectMapper`. The client bean is configured in `OkHttpClientConfig` with 10s connect / 60s read timeouts.
- **ReAct Agent with tools**: `CodeReviewAgent` wraps Spring AI Alibaba's `ReactAgent`. The AI can call `fetchFullFileContent` (a `@Tool` method in `ReviewAgentTools`) to get full source context beyond the diff. Reasoning traces are collected via interceptors (`ReviewReasoningInterceptor` → `ReasoningTraceCollector`).
- **ThreadPoolExecutor**: defined in `ExecutorConfig`, bean name `reviewTaskExecutor`. core=2, max=4, queue=100, `CallerRunsPolicy` rejection. Thread prefix `review-`. Backpressure: when saturated, Controller thread executes the task directly rather than dropping it.
- **Diff truncation**: `CodeReviewPromptBuilder` caps diff content at `code-review.max-diff-chars` (default 150000). If exceeded, truncation is noted in the prompt so the LLM knows the diff is incomplete.

### Package layout

| Package | Role |
|---------|------|
| `com.maa.config` | `@ConfigurationProperties` (`gitlab`, `code-review`) + `@Configuration` beans (OkHttp, Executor) |
| `com.maa.controller` | REST controllers (GitLab webhook endpoint) |
| `com.maa.service` | Service interfaces (`GitLabApiService`, `ReviewService`) |
| `com.maa.service.serviceImpl` | Service implementations (`GitLabApiServiceImpl`) |
| `com.maa.agent` | `CodeReviewAgent` (ReAct agent), `ReviewReasoningInterceptor`, `ReasoningTraceCollector` |
| `com.maa.agent.review` | `CodeReviewPromptBuilder` (diff→prompt assembly + review note formatting) |
| `com.maa.agent.tools` | `ReviewAgentTools` — `@Tool` methods the AI agent can invoke |
| `com.maa.common.dto` | `ResultMsg<T>` unified API response |
| `com.maa.common.dto.gitlab` | GitLab API DTOs (`MergeRequestChangesResponse`, `FileChange`, etc.) |
| `com.maa.common.exception` | `BusinessException` + `GlobalExceptionHandler` |

## Configuration

### Required properties (application.yml)

```yaml
gitlab:
  base-url: http://...          # GitLab instance
  private-token: ${private-token}      # GitLab access token (env var preferred)
  webhook-secret: ${webhook-secret}    # webhook secret token

spring.ai.openai:
  base-url: https://api.deepseek.com   # LLM endpoint (OpenAI-compatible)
  api-key: ${DEEPSEEK_API_KEY}         # LLM API key
  chat.options.model: deepseek-chat
  chat.options.temperature: 0.3

code-review:
  enabled: true                # toggle AI review on/off
  max-diff-chars: 150000       # max diff characters sent to LLM
```

### Alternative LLM providers (uncomment in config)

```yaml
# 通义千问:
#   spring.ai.openai.base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
#   spring.ai.openai.chat.options.model: qwen-plus
# 智谱 GLM:
#   spring.ai.openai.base-url: https://open.bigmodel.cn/api/paas/v4
#   spring.ai.openai.chat.options.model: glm-4-flash
```

### Logging

```yaml
logging.level:
  com.maa.agent: DEBUG    # 🤔思考 / 🔧工具调用 / 📝输出 traces
  com.maa.service: INFO   # business orchestration logs
```

## GitLab Webhook Setup

Endpoint: `POST /api/gitlab/webhook/MR-aiCodeReview`

Required headers:
- `X-Gitlab-Token`: webhook secret (validated against `gitlab.webhook-secret`)
- `X-Gitlab-Event`: must be `"Merge Request Hook"` for MR review

Only MR actions `open`, `update`, `reopen` trigger a diff fetch and review. Other event types/actions are logged and skipped.

## Secrets

Keep credentials out of committed files. `application.yml` uses environment variable placeholders (`${PRIVATE_TOKEN}`, `${DEEPSEEK_API_KEY}`). The untracked `application-dev.yml` contains real values for local development and must never be committed.
