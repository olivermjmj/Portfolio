(() => {
  "use strict";
  const root = document.getElementById("portfolio-chat");
  if (!root) return;
  const get = (id) => document.getElementById(`pc-${id}`);
  const panel = get("panel"), launcher = get("launcher"), input = get("input");
  const messages = get("messages"), status = get("status"), send = get("send");
  const storageKey = "portfolio-chat:v1";
  let history = [], pending = false, activeController = null, generation = 0;
  let endpoint = null, portfolio = null;
  try {
    portfolio = new URL(root.dataset.portfolioBase);
    if (root.dataset.apiBase) {
      const base = new URL(root.dataset.apiBase);
      if (base.protocol === "https:" || (base.protocol === "http:" && ["localhost", "127.0.0.1"].includes(base.hostname) && ["localhost", "127.0.0.1"].includes(location.hostname))) {
        if (!base.username && !base.password && !base.search && !base.hash) endpoint = `${base.href.replace(/\/+$/, "")}/api/chat`;
      }
    }
  } catch { /* Show unavailable state without making requests. */ }
  function safeUrl(value) {
    try {
      const url = new URL(value);
      return portfolio && url.protocol === "https:" && url.origin === portfolio.origin &&
        !url.username && !url.password && !url.pathname.includes("%") && url.pathname.startsWith(portfolio.pathname) ? url.href : null;
    } catch { return null; }
  }
  function normalizeSources(sources) {
    return Array.isArray(sources) ? sources.slice(0, 4).filter(s => s && typeof s.id === "string" && /^S[1-4]$/.test(s.id) && typeof s.title === "string")
      .map(s => ({ id: s.id, title: s.title.slice(0, 160), url: safeUrl(s.url) })) : [];
  }
  function renderMessage(message) {
    const article = document.createElement("div");
    article.className = "pc-message"; article.dataset.role = message.role;
    const speaker = document.createElement("span"); speaker.className = "pc-speaker";
    speaker.textContent = message.role === "user" ? "DIG" : "AI-ASSISTENT";
    const text = document.createElement("p"); text.textContent = message.content;
    article.append(speaker, text);
    const sources = normalizeSources(message.sources);
    if (sources.length) {
      const list = document.createElement("ul"); list.className = "pc-sources"; list.setAttribute("aria-label", "Kilder");
      for (const source of sources) {
        const item = document.createElement("li"), label = `[${source.id}] ${source.title}`;
        if (source.url) { const link = document.createElement("a"); link.href = source.url; link.textContent = label; item.append(link); }
        else item.textContent = label;
        list.append(item);
      }
      article.append(list);
    }
    messages.append(article); messages.scrollTop = messages.scrollHeight;
    return article;
  }
  function save() { try { sessionStorage.setItem(storageKey, JSON.stringify(history.slice(-30))); } catch { /* Private browsing/storage full: keep in memory. */ } }
  try {
    const restored = JSON.parse(sessionStorage.getItem(storageKey) || "[]");
    if (Array.isArray(restored) && restored.length <= 30 && restored.length % 2 === 0 && restored.every((m, i) =>
      m && m.role === (i % 2 === 0 ? "user" : "assistant") && typeof m.content === "string" && m.content.length > 0 &&
      Array.from(m.content).length <= (m.role === "user" ? 250 : 4000))) {
      history = restored.map(m => ({ role: m.role, content: m.content, sources: normalizeSources(m.sources) }));
      history.forEach(renderMessage);
    }
  } catch { /* Ignore corrupt or inaccessible storage. */ }
  function availability() {
    input.disabled = !endpoint || pending; send.disabled = !endpoint || pending;
    if (!endpoint) status.textContent = "Chatten er ikke tilsluttet endnu. Du kan stadig læse alle projekterne på siden.";
  }
  function toggle(open) {
    panel.hidden = !open; launcher.setAttribute("aria-expanded", String(open));
    if (open) { availability(); (input.disabled ? get("close") : input).focus(); messages.scrollTop = messages.scrollHeight; }
    else launcher.focus();
  }
  launcher.addEventListener("click", () => toggle(panel.hidden));
  get("close").addEventListener("click", () => toggle(false));
  root.addEventListener("keydown", event => { if (event.key === "Escape" && !panel.hidden) { event.preventDefault(); toggle(false); } });
  input.addEventListener("input", () => { get("counter").textContent = `${Array.from(input.value).length} / 250`; });
  input.addEventListener("keydown", event => {
    if (event.key === "Enter" && !event.shiftKey && !event.isComposing) { event.preventDefault(); get("form").requestSubmit(); }
  });
  get("reset").addEventListener("click", () => {
    generation++; activeController?.abort(); activeController = null; pending = false;
    history = []; save(); messages.replaceChildren(); status.textContent = ""; input.value = "";
    messages.setAttribute("aria-busy", "false"); get("counter").textContent = "0 / 250"; availability(); if (!input.disabled) input.focus();
  });
  get("form").addEventListener("submit", async event => {
    event.preventDefault();
    const question = input.value.trim();
    if (!endpoint || pending || !question || Array.from(question).length > 250) return;
    const thisGeneration = generation;
    const controller = new AbortController(); activeController = controller;
    const timer = setTimeout(() => controller.abort(), 120000);
    pending = true; availability(); status.textContent = "Finder kilder og skriver et svar…";
    messages.setAttribute("aria-busy", "true");
    const user = { role: "user", content: question };
    const userElement = renderMessage(user);
    try {
      const response = await fetch(endpoint, {
        method: "POST", headers: { "Content-Type": "application/json" }, credentials: "omit", signal: controller.signal,
        body: JSON.stringify({ message: question, history: history.slice(-6).map(({ role, content }) => ({ role, content })) })
      });
      const data = await response.json();
      if (generation !== thisGeneration) return;
      if (!response.ok) throw new Error(typeof data.message === "string" ? data.message : "Chatten er midlertidigt utilgængelig.");
      if (typeof data.answer !== "string" || !data.answer.trim() || Array.from(data.answer).length > 4000) throw new Error("Chatten returnerede et ugyldigt svar.");
      const assistant = { role: "assistant", content: data.answer, sources: normalizeSources(data.sources) };
      history.push(user, assistant); history = history.slice(-30); save(); renderMessage(assistant);
      input.value = ""; get("counter").textContent = "0 / 250"; status.textContent = "";
    } catch (error) {
      if (generation !== thisGeneration) return;
      userElement.remove();
      status.textContent = error.name === "AbortError" ? "Svaret tog for lang tid. Dit spørgsmål er bevaret; prøv igen senere." :
        (error instanceof TypeError ? "Kunne ikke kontakte chatten. Prøv igen senere." : error.message);
    } finally {
      clearTimeout(timer);
      if (generation === thisGeneration) {
        pending = false; activeController = null; messages.setAttribute("aria-busy", "false"); availability();
        if (!panel.hidden) input.focus();
      }
    }
  });
  availability();
})();
