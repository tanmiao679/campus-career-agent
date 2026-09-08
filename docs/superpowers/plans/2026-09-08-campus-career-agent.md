# Campus Career Agent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a real, interview-ready Java career Agent that uses DeepSeek through Spring AI to select and combine three deterministic career tools with bounded memory, tool-call limits, validation, traceability, tests, and honest open-source attribution.

**Architecture:** A Spring MVC API delegates to `CareerAgentService`, which calls a small `CareerAgentGateway` abstraction. The production gateway wraps Spring AI `ChatClient`, attaches three `@Tool` beans and an in-memory `MessageWindowChatMemory`, while deterministic tool logic remains independently testable without model calls. `ToolTraceRecorder` receives a request ID through Spring AI `ToolContext` and records safe execution summaries for the response.

**Tech Stack:** Java 21, Spring Boot 4.0.7, Spring AI 2.0.1, `spring-ai-starter-model-deepseek`, Maven, Jakarta Validation, JUnit 5, AssertJ, Mockito, MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-08-campus-career-agent-design.md`

## Global Constraints

- Repository name is exactly `campus-career-agent`.
- Base package is exactly `com.tanmiao.careeragent`.
- Use Java 21, Spring Boot 4.0.7, and Spring AI 2.0.1.
- Read the DeepSeek key only from `DEEPSEEK_API_KEY`; never commit a real key.
- First release has exactly three business tools and no RAG, MCP, database, Docker, crawler, multi-agent system, or separate frontend.
- Tool logic must be testable without a real model call.
- Use `MessageWindowChatMemory` with a 12-message window.
- Limit each tool to 2 calls and all tools together to 5 calls in one user turn.
- API input message limit is 8,000 characters.
- Trace input is summarized and truncated; do not log full resumes, phone numbers, email addresses, API keys, or full JD text.
- Every completed task ends with passing focused tests and one meaningful commit.
- Do not create or push the public GitHub repository until local tests and the real DeepSeek smoke check pass and the user approves the final repository contents.

---

## File Structure

```text
campus-career-agent/
├── .env.example
├── .gitignore
├── LICENSE
├── NOTICE
├── README.md
├── pom.xml
├── docs/
│   ├── architecture.md
│   ├── interview-notes.md
│   └── superpowers/
│       ├── plans/2026-09-08-campus-career-agent.md
│       └── specs/2026-09-08-campus-career-agent-design.md
├── src/main/java/com/tanmiao/careeragent/
│   ├── CampusCareerAgentApplication.java
│   ├── agent/
│   │   ├── CareerAgentGateway.java
│   │   ├── CareerAgentService.java
│   │   └── SpringAiCareerAgentGateway.java
│   ├── api/
│   │   ├── AgentController.java
│   │   └── dto/
│   │       ├── AgentRequest.java
│   │       ├── AgentResponse.java
│   │       └── ApiError.java
│   ├── config/
│   │   └── AiConfig.java
│   ├── error/
│   │   ├── AgentExecutionException.java
│   │   └── GlobalExceptionHandler.java
│   ├── tool/
│   │   ├── InterviewTools.java
│   │   ├── JobDescriptionTools.java
│   │   ├── StudyPlanTools.java
│   │   └── model/
│   │       ├── InterviewQuestionSet.java
│   │       ├── JobAnalysis.java
│   │       ├── StudyPhase.java
│   │       └── StudyPlan.java
│   └── trace/
│       ├── ToolTrace.java
│       ├── ToolTraceRecorder.java
│       └── ToolTraceStatus.java
├── src/main/resources/application.yml
├── src/test/java/com/tanmiao/careeragent/
│   ├── CampusCareerAgentApplicationTest.java
│   ├── agent/CareerAgentServiceTest.java
│   ├── api/AgentControllerTest.java
│   ├── tool/InterviewToolsTest.java
│   ├── tool/JobDescriptionToolsTest.java
│   ├── tool/StudyPlanToolsTest.java
│   └── trace/ToolTraceRecorderTest.java
└── src/test/resources/application.yml
```

## Task 1: Bootstrap a Buildable, Secret-Safe Project

**Files:**
- Create: `campus-career-agent/pom.xml`
- Create: `campus-career-agent/.gitignore`
- Create: `campus-career-agent/.env.example`
- Create: `campus-career-agent/src/main/resources/application.yml`
- Create: `campus-career-agent/src/test/resources/application.yml`
- Create: `campus-career-agent/src/main/java/com/tanmiao/careeragent/CampusCareerAgentApplication.java`
- Create: `campus-career-agent/src/test/java/com/tanmiao/careeragent/CampusCareerAgentApplicationTest.java`

**Interfaces:**
- Produces: a Maven project that compiles without an API key when tests disable the chat model.

- [ ] **Step 1: Create the project directory and initialize local Git**

Run:

```powershell
New-Item -ItemType Directory -Path campus-career-agent
git -C campus-career-agent init
```

Expected: an empty local Git repository; no GitHub remote.

- [ ] **Step 2: Write the failing context test**

```java
package com.tanmiao.careeragent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusCareerAgentApplicationTest {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 3: Add the Maven build and application entry point**

`pom.xml` must use Java 21, Spring Boot 4.0.7, Spring AI BOM 2.0.1, and these dependencies:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-model-deepseek</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

`CampusCareerAgentApplication.java`:

```java
package com.tanmiao.careeragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusCareerAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusCareerAgentApplication.class, args);
    }
}
```

- [ ] **Step 4: Add safe configuration**

`application.yml`:

```yaml
spring:
  application:
    name: campus-career-agent
  ai:
    model:
      chat: deepseek
    deepseek:
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        model: ${DEEPSEEK_MODEL:deepseek-chat}
        temperature: 0.2
    retry:
      max-attempts: 2
      backoff:
        initial-interval: 500ms
        multiplier: 2
        max-interval: 2s
    tools:
      throw-exception-on-error: false
      limits:
        max-calls-per-tool-default: 2
        max-total-tool-calls: 5
        on-limit-exceeded: RETURN_ERROR_RESPONSE
server:
  port: ${SERVER_PORT:8080}
```

`src/test/resources/application.yml` disables real chat auto-configuration and supplies a non-secret placeholder so context tests never require the user's key:

```yaml
spring:
  ai:
    model:
      chat: none
    deepseek:
      api-key: test-key-never-sent
```

`.env.example` contains only `DEEPSEEK_API_KEY=replace-with-your-own-key` and `DEEPSEEK_MODEL=deepseek-chat`. `.gitignore` must include `.env`, `*.log`, `target/`, `.idea/`, `*.iml`, and `.vscode/`.

- [ ] **Step 5: Run the first test**

Run: `mvn test -Dtest=CampusCareerAgentApplicationTest`

Expected: `BUILD SUCCESS`, with no real DeepSeek request.

- [ ] **Step 6: Verify secret safety and commit**

Run:

```powershell
git status --short
git grep -n -E "sk-[A-Za-z0-9]|DEEPSEEK_API_KEY: [^$]" -- . ':!*.md'
git add .
git commit -m "chore: bootstrap Spring AI career agent"
```

Expected: the secret scan prints nothing; commit succeeds.

## Task 2: Add Safe Tool Execution Tracing

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/trace/ToolTraceStatus.java`
- Create: `src/main/java/com/tanmiao/careeragent/trace/ToolTrace.java`
- Create: `src/main/java/com/tanmiao/careeragent/trace/ToolTraceRecorder.java`
- Test: `src/test/java/com/tanmiao/careeragent/trace/ToolTraceRecorderTest.java`

**Interfaces:**
- Produces: `<T> T record(String requestId, String toolName, String inputSummary, Supplier<T> action)`, `List<ToolTrace> findByRequestId(String requestId)`, and `List<ToolTrace> drainByRequestId(String requestId)`.

- [ ] **Step 1: Write failing success, failure, and redaction tests**

```java
@Test
void recordsSuccessfulExecution() {
    var recorder = new ToolTraceRecorder();
    String result = recorder.record("req-1", "analyze_job_description", "Java JD", () -> "ok");
    assertThat(result).isEqualTo("ok");
    assertThat(recorder.findByRequestId("req-1")).singleElement()
        .satisfies(trace -> assertThat(trace.status()).isEqualTo(ToolTraceStatus.SUCCESS));
}

@Test
void recordsFailureAndRethrows() {
    var recorder = new ToolTraceRecorder();
    assertThatThrownBy(() -> recorder.record("req-2", "study_plan", "input",
        () -> { throw new IllegalStateException("boom"); }))
        .isInstanceOf(IllegalStateException.class);
    assertThat(recorder.findByRequestId("req-2").getFirst().status())
        .isEqualTo(ToolTraceStatus.FAILED);
}

@Test
void truncatesAndRedactsTraceInput() {
    var recorder = new ToolTraceRecorder();
    recorder.record("req-3", "tool", "邮箱 test@example.com 电话 13800138000 " + "x".repeat(300), () -> "ok");
    String summary = recorder.findByRequestId("req-3").getFirst().inputSummary();
    assertThat(summary).doesNotContain("test@example.com", "13800138000");
    assertThat(summary.length()).isLessThanOrEqualTo(200);
}

@Test
void drainsCompletedRequestToAvoidUnboundedRetention() {
    var recorder = new ToolTraceRecorder();
    recorder.record("req-4", "tool", "input", () -> "ok");
    assertThat(recorder.drainByRequestId("req-4")).hasSize(1);
    assertThat(recorder.findByRequestId("req-4")).isEmpty();
}
```

- [ ] **Step 2: Run the test and confirm it fails**

Run: `mvn test -Dtest=ToolTraceRecorderTest`

Expected: compilation fails because tracing types do not exist.

- [ ] **Step 3: Implement immutable trace records and recorder**

Use:

```java
public enum ToolTraceStatus { SUCCESS, FAILED }

public record ToolTrace(
    String toolName,
    ToolTraceStatus status,
    String inputSummary,
    long durationMs,
    String errorMessage
) {}
```

`ToolTraceRecorder` uses `ConcurrentHashMap<String, CopyOnWriteArrayList<ToolTrace>>`, `System.nanoTime()` for duration, replaces email and mainland mobile patterns with `[REDACTED]`, truncates input to 200 characters, and returns immutable list copies. `drainByRequestId` removes the completed request entry before returning its immutable copy.

- [ ] **Step 4: Run focused tests and commit**

Run: `mvn test -Dtest=ToolTraceRecorderTest`

Expected: all three tests pass.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/trace src/test/java/com/tanmiao/careeragent/trace
git commit -m "feat: record safe tool execution traces"
```

## Task 3: Implement Deterministic JD Analysis Tool

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/tool/model/JobAnalysis.java`
- Create: `src/main/java/com/tanmiao/careeragent/tool/JobDescriptionTools.java`
- Test: `src/test/java/com/tanmiao/careeragent/tool/JobDescriptionToolsTest.java`

**Interfaces:**
- Produces: `JobAnalysis analyzeJobDescription(String jobTitle, String jobDescription, ToolContext toolContext)`.
- Consumes: `ToolTraceRecorder.record(...)`; `toolContext.getContext().get("requestId")`.

- [ ] **Step 1: Write failing behavior tests**

```java
@Test
void extractsKnownSkillsWithoutDuplicates() {
    var tools = new JobDescriptionTools(new ToolTraceRecorder());
    var result = tools.analyzeJobDescription(
        "AI应用开发实习生",
        "熟悉 Java、Spring Boot、Spring AI、RAG、Redis，了解 Java 工程化",
        new ToolContext(Map.of("requestId", "req-1")));
    assertThat(result.skills()).containsExactly("Java", "Spring Boot", "Spring AI", "RAG", "Redis");
}

@Test
void rejectsBlankAndOversizedDescriptions() {
    var tools = new JobDescriptionTools(new ToolTraceRecorder());
    var context = new ToolContext(Map.of("requestId", "req-2"));
    assertThatThrownBy(() -> tools.analyzeJobDescription("AI实习生", " ", context))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tools.analyzeJobDescription("AI实习生", "x".repeat(8001), context))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Implement the minimal skill catalogue and parser**

`JobAnalysis`:

```java
public record JobAnalysis(
    String jobTitle,
    List<String> skills,
    List<String> educationRequirements,
    List<String> responsibilityKeywords
) {}
```

The catalogue contains only job-relevant aliases for Java, Spring Boot, Spring AI, RAG, PostgreSQL, pgvector, Redis, Docker, Git, REST API, WebSocket, Tool Calling, MCP, and Prompt Engineering. Match case-insensitively while preserving catalogue order; do not invent unmatched skills.

Annotate the method:

```java
@Tool(name = "analyze_job_description",
      description = "Analyze a software or AI job description and return explicit skills, education requirements, and responsibility keywords. Use this before creating a study plan or interview questions from a JD.")
```

- [ ] **Step 3: Run tests and commit**

Run: `mvn test -Dtest=JobDescriptionToolsTest`

Expected: parser and validation tests pass without an API call.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/tool src/test/java/com/tanmiao/careeragent/tool/JobDescriptionToolsTest.java
git commit -m "feat: add deterministic JD analysis tool"
```

## Task 4: Implement Study Plan Tool

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/tool/model/StudyPhase.java`
- Create: `src/main/java/com/tanmiao/careeragent/tool/model/StudyPlan.java`
- Create: `src/main/java/com/tanmiao/careeragent/tool/StudyPlanTools.java`
- Test: `src/test/java/com/tanmiao/careeragent/tool/StudyPlanToolsTest.java`

**Interfaces:**
- Produces: `StudyPlan generateStudyPlan(String targetRole, List<String> missingSkills, int daysPerWeek, ToolContext toolContext)`.

- [ ] **Step 1: Write failing prioritization and validation tests**

```java
@Test
void prioritizesCoreAgentSkillsAndBuildsThreePhases() {
    var tools = new StudyPlanTools(new ToolTraceRecorder());
    var result = tools.generateStudyPlan(
        "Java AI应用开发",
        List.of("MCP", "Spring AI", "Tool Calling", "RAG"),
        5,
        new ToolContext(Map.of("requestId", "req-1")));
    assertThat(result.phases()).hasSize(3);
    assertThat(result.prioritizedSkills()).startsWith("Spring AI", "Tool Calling", "RAG");
}

@Test
void requiresOneToSevenStudyDays() {
    var tools = new StudyPlanTools(new ToolTraceRecorder());
    var context = new ToolContext(Map.of("requestId", "req-2"));
    assertThatThrownBy(() -> tools.generateStudyPlan("Java", List.of("RAG"), 0, context))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Implement a deterministic three-phase plan**

Use records:

```java
public record StudyPhase(String name, List<String> skills, List<String> deliverables) {}
public record StudyPlan(String targetRole, int daysPerWeek,
                        List<String> prioritizedSkills, List<StudyPhase> phases) {}
```

Priority order is Java/Spring foundations, Spring AI/Tool Calling, RAG/vector search, reliability/testing, then optional MCP. The three phase names are `基础补齐`, `项目实现`, and `面试验证`; every phase has at least one concrete deliverable.

- [ ] **Step 3: Run tests and commit**

Run: `mvn test -Dtest=StudyPlanToolsTest`

Expected: all tests pass without an API call.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/tool src/test/java/com/tanmiao/careeragent/tool/StudyPlanToolsTest.java
git commit -m "feat: add targeted study plan tool"
```

## Task 5: Implement Interview Question Tool

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/tool/model/InterviewQuestionSet.java`
- Create: `src/main/java/com/tanmiao/careeragent/tool/InterviewTools.java`
- Test: `src/test/java/com/tanmiao/careeragent/tool/InterviewToolsTest.java`

**Interfaces:**
- Produces: `InterviewQuestionSet generateInterviewQuestions(String targetRole, List<String> skills, int count, ToolContext toolContext)`.

- [ ] **Step 1: Write failing count and relevance tests**

```java
@Test
void returnsRequestedNumberWithinLimit() {
    var tools = new InterviewTools(new ToolTraceRecorder());
    var result = tools.generateInterviewQuestions(
        "Java Agent 实习生", List.of("Spring AI", "RAG", "Redis"), 5,
        new ToolContext(Map.of("requestId", "req-1")));
    assertThat(result.questions()).hasSize(5);
    assertThat(result.questions()).anyMatch(q -> q.contains("Spring AI") || q.contains("RAG"));
}

@Test
void rejectsQuestionCountsOutsideOneToTen() {
    var tools = new InterviewTools(new ToolTraceRecorder());
    var context = new ToolContext(Map.of("requestId", "req-2"));
    assertThatThrownBy(() -> tools.generateInterviewQuestions("Java", List.of("Java"), 11, context))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: Implement a curated question bank**

`InterviewQuestionSet` is `record InterviewQuestionSet(String targetRole, List<String> skills, List<String> questions)`. Maintain at least two questions for each of Java, Spring Boot, Spring AI, Tool Calling, RAG, Redis, PostgreSQL, WebSocket, and Docker. Select skill-matched questions first, remove duplicates, cap count at 10, and use general Java backend questions only to fill a shortfall.

- [ ] **Step 3: Run tests and commit**

Run: `mvn test -Dtest=InterviewToolsTest`

Expected: count, validation, relevance, and deduplication tests pass.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/tool src/test/java/com/tanmiao/careeragent/tool/InterviewToolsTest.java
git commit -m "feat: add interview question tool"
```

## Task 6: Integrate DeepSeek, Memory, Tool Limits, and Agent Service

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/agent/CareerAgentGateway.java`
- Create: `src/main/java/com/tanmiao/careeragent/agent/SpringAiCareerAgentGateway.java`
- Create: `src/main/java/com/tanmiao/careeragent/agent/CareerAgentService.java`
- Create: `src/main/java/com/tanmiao/careeragent/config/AiConfig.java`
- Create: `src/main/java/com/tanmiao/careeragent/api/dto/AgentResponse.java`
- Create: `src/main/java/com/tanmiao/careeragent/error/AgentExecutionException.java`
- Test: `src/test/java/com/tanmiao/careeragent/agent/CareerAgentServiceTest.java`

**Interfaces:**
- `CareerAgentGateway.execute(String conversationId, String requestId, String message) -> String`
- `CareerAgentService.chat(String conversationId, String message) -> AgentResponse`
- `AgentResponse(String requestId, String conversationId, String answer, List<ToolTrace> toolTraces)`

- [ ] **Step 1: Write failing service tests with a fake gateway**

```java
@Test
void generatesIdsAndReturnsTraces() {
    var traces = new ToolTraceRecorder();
    CareerAgentGateway gateway = (conversationId, requestId, message) -> {
        traces.record(requestId, "analyze_job_description", "summary", () -> "ok");
        return "分析完成";
    };
    var service = new CareerAgentService(gateway, traces);
    var response = service.chat(null, "分析这份 Java JD");
    assertThat(response.requestId()).isNotBlank();
    assertThat(response.conversationId()).isNotBlank();
    assertThat(response.answer()).isEqualTo("分析完成");
    assertThat(response.toolTraces()).singleElement();
}

@Test
void wrapsProviderFailureWithoutLeakingSecrets() {
    CareerAgentGateway gateway = (conversationId, requestId, message) -> {
        throw new RuntimeException("Authorization: Bearer sk-secret");
    };
    var service = new CareerAgentService(gateway, new ToolTraceRecorder());
    assertThatThrownBy(() -> service.chat("c1", "hello"))
        .isInstanceOf(AgentExecutionException.class)
        .hasMessage("AI 服务暂时不可用，请稍后重试");
}
```

- [ ] **Step 2: Configure bounded memory and ChatClient**

`AiConfig` creates:

```java
@Bean
ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder().maxMessages(12).build();
}

@Bean
ChatClient careerChatClient(ChatModel chatModel, ChatMemory memory) {
    return ChatClient.builder(chatModel)
        .defaultSystem("""
            你是校园求职助手。只处理求职、岗位分析、学习规划和面试准备。
            涉及岗位要求时优先调用岗位分析工具；需要计划或题目时调用对应工具。
            不虚构用户经历，不声称用户掌握未提供的技能。工具失败时说明限制。
            """)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
        .build();
}
```

`SpringAiCareerAgentGateway.execute` calls:

```java
return chatClient.prompt()
    .user(message)
    .tools(jobDescriptionTools, studyPlanTools, interviewTools)
    .toolContext(Map.of("requestId", requestId))
    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
    .call()
    .content();
```

Tool limits remain configured in `application.yml`: 2 calls per tool and 5 total calls, with `RETURN_ERROR_RESPONSE` behavior.

- [ ] **Step 3: Implement service orchestration**

Declare `CareerAgentGateway` as `@FunctionalInterface`. Generate UUID strings for missing conversation and request IDs, reject blank or over-8,000-character messages before calling the gateway, return the immutable result of `traces.drainByRequestId(requestId)`, and wrap provider exceptions in the fixed safe `AgentExecutionException` message. On provider failure, drain and discard that request's traces before throwing so failed requests do not accumulate in memory.

- [ ] **Step 4: Run tests and commit**

Run: `mvn test -Dtest=CareerAgentServiceTest`

Expected: ID, trace, validation, and provider-failure tests pass without an API call.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/agent src/main/java/com/tanmiao/careeragent/config src/main/java/com/tanmiao/careeragent/api/dto/AgentResponse.java src/main/java/com/tanmiao/careeragent/error/AgentExecutionException.java src/test/java/com/tanmiao/careeragent/agent
git commit -m "feat: orchestrate bounded career agent conversations"
```

## Task 7: Expose a Validated REST API

**Files:**
- Create: `src/main/java/com/tanmiao/careeragent/api/dto/AgentRequest.java`
- Create: `src/main/java/com/tanmiao/careeragent/api/dto/ApiError.java`
- Create: `src/main/java/com/tanmiao/careeragent/api/AgentController.java`
- Create: `src/main/java/com/tanmiao/careeragent/error/GlobalExceptionHandler.java`
- Test: `src/test/java/com/tanmiao/careeragent/api/AgentControllerTest.java`

**Interfaces:**
- `POST /api/v1/agent/chat`
- Request: `AgentRequest(String conversationId, String message)`
- Success: `AgentResponse`
- Validation failure: HTTP 400 `ApiError(String code, String message)`
- AI provider failure: HTTP 503 with code `AI_SERVICE_UNAVAILABLE`

- [ ] **Step 1: Write failing MockMvc tests**

```java
@Test
void returnsAgentResponse() throws Exception {
    given(service.chat(null, "分析 Java Agent JD"))
        .willReturn(new AgentResponse("r1", "c1", "完成", List.of()));
    mvc.perform(post("/api/v1/agent/chat")
            .contentType(APPLICATION_JSON)
            .content("{\"message\":\"分析 Java Agent JD\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.requestId").value("r1"));
}

@Test
void rejectsBlankMessage() throws Exception {
    mvc.perform(post("/api/v1/agent/chat")
            .contentType(APPLICATION_JSON)
            .content("{\"message\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
}
```

- [ ] **Step 2: Implement request validation and exception mapping**

Use:

```java
public record AgentRequest(
    String conversationId,
    @NotBlank @Size(max = 8000) String message
) {}
```

`AgentController` accepts `@Valid @RequestBody AgentRequest` and delegates to `CareerAgentService`. `GlobalExceptionHandler` maps `MethodArgumentNotValidException` and `IllegalArgumentException` to 400, and `AgentExecutionException` to 503. Do not return exception class names, stack traces, provider bodies, or credentials.

- [ ] **Step 3: Run API and full tests, then commit**

Run:

```powershell
mvn test -Dtest=AgentControllerTest
mvn test
```

Expected: focused and full suites pass without a real model call.

Commit:

```powershell
git add src/main/java/com/tanmiao/careeragent/api src/main/java/com/tanmiao/careeragent/error/GlobalExceptionHandler.java src/test/java/com/tanmiao/careeragent/api
git commit -m "feat: expose validated career agent API"
```

## Task 8: Perform Minimal Real DeepSeek Smoke Verification

**Files:**
- Create: `docs/smoke-test-results.md`
- Modify only if a verified defect is found: production or test files from Tasks 1–7

**Interfaces:**
- Verifies the production `POST /api/v1/agent/chat` behavior against DeepSeek.

- [ ] **Step 1: Set the API key only in the current terminal**

Run locally without printing the value:

```powershell
$env:DEEPSEEK_API_KEY = Read-Host "请输入 DeepSeek API Key"
```

Expected: the value exists only in that process environment and is not written to a file.

- [ ] **Step 2: Start the application**

Run: `mvn spring-boot:run`

Expected: application starts on port 8080 without logging the key.

- [ ] **Step 3: Execute exactly five smoke scenarios**

Use one request for each scenario:

1. `请分析：招聘 Java AI 应用开发实习生，要求 Spring Boot、Spring AI、RAG、Redis。`
2. `根据刚才的岗位要求，为只会 Java 和 Spring Boot 的学生制定每周学习 5 天的计划。`
3. `根据刚才的岗位生成 5 道面试题。`
4. `帮我写一首与求职无关的长诗。`
5. `严格按参数调用学习计划工具：目标岗位 Java AI 应用开发，缺口技能 Spring AI，daysPerWeek 必须为 0；如果工具拒绝该参数，请解释失败原因并停止继续调用。`

Record only request intent, HTTP status, called tool names, whether the answer is usable, and observed token metadata when available. Do not record the API key or full private resume content.

- [ ] **Step 4: Apply only evidence-driven fixes**

If a scenario fails, first record the exact failure category: startup/configuration, model did not request a tool, invalid tool arguments, tool exception, loop limit, or API response mapping. Make the smallest fix that addresses that category and add a regression test before rerunning only the failed scenario.

- [ ] **Step 5: Run the complete verification and commit**

Run:

```powershell
mvn test
mvn clean package
git status --short
git grep -n -E "sk-[A-Za-z0-9]|api-key:[[:space:]]+[^$]" -- . ':!*.md'
```

Expected: tests and package succeed, secret scan prints nothing, and only `docs/smoke-test-results.md` or intentional fixes are uncommitted.

Commit:

```powershell
git add docs/smoke-test-results.md
git add src/main src/test
git commit -m "test: verify DeepSeek tool calling flows"
```

## Task 9: Make the Repository Recruiter-Ready

**Files:**
- Create: `LICENSE`
- Create: `NOTICE`
- Create: `README.md`
- Create: `docs/architecture.md`
- Create: `docs/interview-notes.md`
- Copy: approved spec and plan into `docs/superpowers/`

**Interfaces:**
- Produces: a repository understandable in five seconds and reproducible without secret leakage.

- [ ] **Step 1: Add honest attribution**

Use Apache License 2.0. `NOTICE` must name the two reference sources and state that the career-domain tools, validation, bounded execution, trace response, tests, and documentation are this repository’s implementation. Do not claim affiliation with Spring or Alibaba.

- [ ] **Step 2: Write README in recruiter-first order**

README sections must appear in this order:

1. One-sentence project value
2. Screenshot or short demo GIF
3. Implemented features only
4. Architecture diagram
5. Tool-calling sequence
6. Tech stack
7. Quick start with environment variables
8. Five example requests
9. Test command and latest verified result
10. Design trade-offs and intentionally omitted features
11. Open-source attribution
12. Roadmap clearly marked as not implemented

- [ ] **Step 3: Write interview notes**

`docs/interview-notes.md` answers in the user’s own words:

- Why use Tool Calling instead of one large prompt?
- Why are the three tools deterministic?
- How does Spring AI pass `ToolContext` without exposing it to the model?
- How do per-tool and total-call limits stop runaway loops?
- What does `MessageWindowChatMemory` retain and omit?
- Why is Redis excluded from version 1?
- How are secrets and personal data protected?
- What was changed relative to the reference examples?

- [ ] **Step 4: Final repository audit**

Run:

```powershell
mvn clean test
git log --oneline --decorate -10
git status --short
git grep -n -E "sk-[A-Za-z0-9]|DEEPSEEK_API_KEY=.*[^}]$" -- . ':!*.md' ':!.env.example'
```

Expected: build passes; history contains meaningful incremental commits; working tree is clean; no real key is found.

- [ ] **Step 5: Commit documentation**

```powershell
git add README.md LICENSE NOTICE docs .env.example
git commit -m "docs: prepare recruiter-ready project showcase"
```

Expected: local repository is complete but still has no GitHub remote.

## Task 10: User Review and Public GitHub Publishing

**Files:**
- No code changes unless requested during review.

**Interfaces:**
- Produces: an approved public repository under the user’s GitHub account.

- [ ] **Step 1: Review the exact publish set with the user**

Show the user the README, screenshots, Git history summary, smoke-test evidence, attribution, and secret-scan result. Confirm that the user can explain the three tools and one failure-handling path.

- [ ] **Step 2: Obtain explicit approval before creating or pushing a public repository**

Approval must name `campus-career-agent` and confirm public visibility. Do not infer approval from earlier permission to work locally.

- [ ] **Step 3: Create the GitHub repository without auto-generated files**

Use the GitHub website if CLI authentication is unavailable. Repository name: `campus-career-agent`; description: `A Spring AI career assistant demonstrating bounded Tool Calling, conversation memory, validation, and safe execution tracing.`

- [ ] **Step 4: Connect and push**

```powershell
git remote add origin https://github.com/tanmiao679/campus-career-agent.git
git branch -M main
git push -u origin main
```

Expected: GitHub shows the same commits and README as the reviewed local repository.

- [ ] **Step 5: Verify the public result**

Open the public repository and confirm README rendering, license detection, no secret files, visible commit history, and copyable startup commands. Pin the repository only after this verification.
