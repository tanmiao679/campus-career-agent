# DeepSeek 真实冒烟测试记录

测试日期：2026-09-08

运行方式：无 Web 端口的 Spring Boot 测试上下文，真实调用 DeepSeek `deepseek-chat`。

安全约束：密钥仅通过进程环境变量注入；不写入代码、配置、测试输出或本文件。

## 结果

| 场景 | HTTP/执行状态 | 工具轨迹 | 可用性判断 |
| --- | --- | --- | --- |
| 分析 Java AI 实习岗位要求 | 通过 | `analyze_job_description` | 回答非空，正确选择 JD 分析工具 |
| 基于同一会话生成每周 5 天学习计划 | 通过 | `generate_study_plan` | 回答非空，正确复用会话并选择计划工具 |
| 基于同一会话生成 5 道面试题 | 通过 | `generate_interview_questions` | 回答非空，正确复用会话并选择面试题工具 |
| 请求与求职无关的长诗 | 通过 | 无 | 回答非空且没有误调用求职工具 |
| 强制以 `daysPerWeek=0` 调用计划工具 | 通过 | 无重复轨迹 | 返回非空限制说明，未出现工具循环 |

测试汇总：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。五个场景在一次测试内串行完成，总耗时约 33 秒。

## 环境说明

当前 Codex Windows 宿主不允许 Java NIO 创建本机回环管道，因此内置 Tomcat 和 Reactor Netty 无法在该宿主运行。真实验证只在测试范围内把 HTTP 请求工厂替换为阻塞式实现；生产代码和默认运行配置没有因此改变。

普通 `mvn test` 不会运行真实模型测试。只有显式设置 `SPRING_AI_DEEPSEEK_API_KEY` 时，`RealDeepSeekSmokeTest` 才会启用。
