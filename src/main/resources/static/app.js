(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const examples = {
    '岗位分析': '请分析这个校招岗位：Java 后端开发，要求掌握 Java、Spring Boot、MySQL、Redis，了解计算机网络与操作系统。请拆解核心技能和准备重点。',
    '学习计划': '我是一名准备 Java 后端校招的大学生，掌握 Java 基础，但 Spring Boot、MySQL 和 Redis 比较薄弱。请制定一个 4 周的学习计划，每天可以投入 2 小时。',
    '面试题': '请为 Java 后端校招准备一组面试题，覆盖 Java 集合、并发、Spring Boot、MySQL 和 Redis，并给出回答要点。'
  };
  let conversationId = null;
  let busy = false;
  let turns = 0;
  const traceButtons = [];
  function element(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = String(text);
    return node;
  }
  function addMessage(role, text) {
    $('welcome').hidden = true;
    const row = element('article', 'message ' + role);
    row.append(element('div', 'message-label', role === 'user' ? 'YOU / 你' : 'CAREER AGENT'));
    row.append(element('div', 'bubble', text));
    $('messages').append(row);
    scrollChat();
    return row;
  }
  function scrollChat() { $('messages').scrollTop = $('messages').scrollHeight; }
  function setBusy(value) {
    busy = value;
    $('send').disabled = value;
    $('new-chat').disabled = value;
    document.querySelectorAll('[data-example]').forEach(button => { button.disabled = value; });
    $('send').textContent = value ? '处理中…' : '发送 ↗';
    $('state').textContent = value ? 'Agent 正在处理' : '等待提问';
    $('chat-form').setAttribute('aria-busy', String(value));
  }
  function field(dl, label, value, className) {
    dl.append(element('dt', '', label), element('dd', className || '', value ?? '—'));
  }
  function showTrace(data, selected) {
    traceButtons.forEach(button => button.setAttribute('aria-pressed', String(button === selected)));
    $('conversation-id').textContent = data.conversationId;
    $('request-id').textContent = data.requestId;
    $('trace-count').textContent = data.toolTraces.length;
    $('traces').replaceChildren();
    if (!data.toolTraces.length) {
      $('traces').append(element('p', 'empty', '本次回答没有工具调用。Agent 可能直接基于对话上下文作答。'));
    }
    data.toolTraces.forEach(trace => {
      const card = element('article', 'tool' + (trace.status === 'FAILED' ? ' failed' : ''));
      const head = element('div', 'tool-head');
      head.append(element('h4', '', trace.toolName), element('span', 'badge', trace.status));
      const dl = element('dl');
      field(dl, 'inputSummary', trace.inputSummary);
      field(dl, 'durationMs', String(trace.durationMs) + ' ms');
      field(dl, 'errorMessage', trace.errorMessage || '—', trace.errorMessage ? 'tool-error' : '');
      card.append(head, dl);
      $('traces').append(card);
    });
  }
  function validateResponse(data) {
    if (!data || typeof data.answer !== 'string' || typeof data.conversationId !== 'string' || !data.conversationId.trim() || typeof data.requestId !== 'string' || !data.requestId.trim() || !Array.isArray(data.toolTraces)) return false;
    return data.toolTraces.every(t => t && typeof t.toolName === 'string' && ['SUCCESS', 'FAILED'].includes(t.status) && typeof t.inputSummary === 'string' && Number.isFinite(t.durationMs) && t.durationMs >= 0 && (t.errorMessage == null || typeof t.errorMessage === 'string'));
  }
  async function submit(event) {
    event.preventDefault();
    if (busy) return;
    const message = $('message').value.trim();
    if (!message || message.length > 8000) return;
    $('feedback').hidden = true;
    addMessage('user', message);
    $('message').value = '';
    updateCount();
    const pending = addMessage('pending', '正在分析问题，等待 Agent 返回回答与执行记录…');
    setBusy(true);
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 120000);
    try {
      if (location.protocol === 'file:') throw new Error('请将这三个文件放入 Spring Boot 的 static 目录，启动项目后通过 http://localhost:8080/ 访问。');
      const response = await fetch('/api/v1/agent/chat', {
        method: 'POST', headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
        body: JSON.stringify({ conversationId, message }), signal: controller.signal
      });
      let data;
      try { data = await response.json(); }
      catch { throw new Error('服务返回了非 JSON 内容（HTTP ' + response.status + '），请检查后端服务与接口地址。'); }
      if (!response.ok) throw new Error((data.message || '请求失败，请检查后端服务。') + '（HTTP ' + response.status + (data.code ? ' · ' + data.code : '') + '）');
      if (!validateResponse(data)) throw new Error('响应字段与 AgentResponse 不匹配，请检查后端版本。');
      conversationId = data.conversationId;
      turns += 1;
      $('turn-count').textContent = turns + ' 次提问';
      pending.remove();
      const answer = addMessage('assistant', data.answer);
      const button = element('button', 'trace-link', '查看执行记录 · ' + data.toolTraces.length + ' 次工具调用 ↗');
      button.type = 'button';
      button.addEventListener('click', () => showTrace(data, button));
      traceButtons.push(button);
      answer.append(button);
      showTrace(data, button);
      scrollChat();
    } catch (error) {
      pending.remove();
      const reason = error.name === 'AbortError' ? '等待超过 120 秒。请求结果未确认，请检查后端日志后再决定是否重试。' : error instanceof TypeError ? '无法连接后端，请确认 Spring Boot 已启动，并通过服务地址访问此页面。' : error.message;
      addMessage('error', reason);
      $('feedback').textContent = '本次请求未完成；右侧保留上一次成功回答的记录。';
      $('feedback').hidden = false;
      if (!$('message').value) { $('message').value = message; updateCount(); }
    } finally {
      clearTimeout(timeout);
      setBusy(false);
      $('message').focus();
    }
  }
  function updateCount() { $('char-count').textContent = $('message').value.length + ' / 8000'; }
  $('chat-form').addEventListener('submit', submit);
  $('message').addEventListener('input', updateCount);
  $('message').addEventListener('keydown', event => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
      event.preventDefault();
      if (!busy) $('chat-form').requestSubmit();
    }
  });
  document.querySelectorAll('[data-example]').forEach(button => button.addEventListener('click', () => {
    $('message').value = examples[button.dataset.example];
    updateCount();
    $('message').focus();
  }));
  $('new-chat').addEventListener('click', () => {
    if (busy) return;
    conversationId = null;
    turns = 0;
    traceButtons.length = 0;
    document.querySelectorAll('.message').forEach(node => node.remove());
    $('welcome').hidden = false;
    $('message').value = '';
    updateCount();
    $('turn-count').textContent = '0 次提问';
    $('conversation-id').textContent = '尚未创建';
    $('request-id').textContent = '等待请求';
    $('trace-count').textContent = '—';
    $('traces').replaceChildren(element('p', 'empty', '发送问题后，这里展示服务端返回的工具名称、状态、输入摘要与耗时。'));
    $('feedback').hidden = true;
    $('message').focus();
  });
})();
