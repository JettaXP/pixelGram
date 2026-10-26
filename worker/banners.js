const MAX_BYTES = 600 * 1024;
const MAX_SIDE = 4000;
const COOLDOWN_MS = 60 * 1000;
const RETRIES = 4;
const BADGE_COOLDOWN_MS = 5 * 1000;
const recent = new Map();

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return new Response("pixelGram banners");
    if (request.headers.get("X-Telegram-Bot-Api-Secret-Token") !== env.WEBHOOK_SECRET) {
      return new Response("forbidden", { status: 403 });
    }
    const update = await request.json();
    const message = update.message;
    if (!message || message.chat.type !== "private" || !message.from) return new Response("ok");
    try {
      await handle(message, env);
    } catch (error) {
      console.log(error.stack || String(error));
      await reply(env, message.chat.id, "Something went wrong, try again later.");
    }
    return new Response("ok");
  },
};

async function handle(message, env) {
  const userId = String(message.from.id);
  const command = (message.caption || message.text || "").trim().split(/\s+/)[0];
  if (command === "/badge") return badge(message, env, userId);
  if (command !== "/banner" && command !== "/remove") return;
  if ((env.BANNED || "").split(",").map((id) => id.trim()).includes(userId)) {
    return reply(env, message.chat.id, "Banners are disabled for this account.");
  }
  if (command === "/remove") {
    await commit(env, userId, null);
    return reply(env, message.chat.id, "Banner removed.");
  }
  const last = recent.get(userId) || 0;
  if (Date.now() - last < COOLDOWN_MS) return reply(env, message.chat.id, "Too fast, wait a minute.");
  recent.set(userId, Date.now());
  const file = message.document || (message.photo && message.photo[message.photo.length - 1]);
  if (!file) return reply(env, message.chat.id, "Send an image with /banner in the caption.");
  if (file.file_size > MAX_BYTES) return reply(env, message.chat.id, "The banner is too big.");
  const bytes = await telegramFile(env, file.file_id);
  const size = jpegSize(bytes);
  if (!size || size.width > MAX_SIDE || size.height > MAX_SIDE) return reply(env, message.chat.id, "The banner must be a JPEG image.");
  await commit(env, userId, bytes);
  return reply(env, message.chat.id, "Banner updated.");
}

async function badge(message, env, userId) {
  if (!(env.BADGE_OWNERS || "").split(",").map((id) => id.trim()).includes(userId)) return;
  const last = recent.get(`badge${userId}`) || 0;
  if (Date.now() - last < BADGE_COOLDOWN_MS) return reply(env, message.chat.id, "Too fast, wait a few seconds.");
  recent.set(`badge${userId}`, Date.now());
  const match = (message.text || "").trim().match(/^\/badge(?:\s+(\d{1,20})\s+(\S{1,16})(?:\s+([\s\S]*))?)?$/u);
  if (!match) return reply(env, message.chat.id, "Send /badge <custom emoji id or 0> <emoji> <text>, or /badge to reset.");
  const row = match[1] ? [userId, match[1], match[2], clean(match[3] || "")].join("\t") : null;
  await commitText(env, "Banners/badges.tsv", (text) => {
    const rows = text.split("\n").filter((line) => line && line.split("\t")[0] !== userId);
    if (row) rows.push(row);
    return rows.length ? rows.join("\n") + "\n" : "";
  }, `${row ? "Badge" : "Reset badge"} ${userId} [skip ci]`);
  return reply(env, message.chat.id, row ? "Badge updated." : "Badge reset.");
}

function clean(text) {
  return Array.from(text.replace(/[\p{Cc}\p{Cf}]/gu, (c) => (c === "\u200d" || c === "\ufe0f" ? c : " ")).trim()).slice(0, 64).join("");
}

async function commitText(env, path, change, message) {
  for (let attempt = 0; attempt < RETRIES; attempt++) {
    const ref = await github(env, `git/ref/heads/${env.BRANCH}`);
    const head = await github(env, `git/commits/${ref.object.sha}`);
    const response = await fetch(`https://api.github.com/repos/${env.REPO}/contents/${path}?ref=${ref.object.sha}`, {
      headers: headers(env, "application/vnd.github.raw"),
    });
    if (!response.ok && response.status !== 404) throw new Error(`${path}: ${response.status}`);
    const content = change(response.ok ? await response.text() : "");
    const newTree = await github(env, "git/trees", "POST", {
      base_tree: head.tree.sha,
      tree: [{ path, mode: "100644", type: "blob", content }],
    });
    const created = await github(env, "git/commits", "POST", { message, tree: newTree.sha, parents: [ref.object.sha] });
    const moved = await github(env, `git/refs/heads/${env.BRANCH}`, "PATCH", { sha: created.sha }, true);
    if (moved) return;
  }
  throw new Error("branch kept moving");
}

async function telegramFile(env, fileId) {
  const info = await (await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getFile?file_id=${encodeURIComponent(fileId)}`)).json();
  if (!info.ok) throw new Error(`getFile: ${info.description}`);
  const response = await fetch(`https://api.telegram.org/file/bot${env.BOT_TOKEN}/${info.result.file_path}`);
  if (!response.ok) throw new Error(`download: ${response.status}`);
  return new Uint8Array(await response.arrayBuffer());
}

function jpegSize(bytes) {
  if (bytes[0] !== 0xff || bytes[1] !== 0xd8) return null;
  let offset = 2;
  while (offset + 9 < bytes.length) {
    if (bytes[offset] !== 0xff) return null;
    const marker = bytes[offset + 1];
    const length = (bytes[offset + 2] << 8) | bytes[offset + 3];
    if (marker >= 0xc0 && marker <= 0xc3) {
      return { height: (bytes[offset + 5] << 8) | bytes[offset + 6], width: (bytes[offset + 7] << 8) | bytes[offset + 8] };
    }
    offset += 2 + length;
  }
  return null;
}

async function commit(env, userId, bytes) {
  for (let attempt = 0; attempt < RETRIES; attempt++) {
    const ref = await github(env, `git/ref/heads/${env.BRANCH}`);
    const head = await github(env, `git/commits/${ref.object.sha}`);
    const index = await readIndex(env, ref.object.sha);
    const tree = [];
    if (bytes) {
      const blob = await github(env, "git/blobs", "POST", { content: base64(bytes), encoding: "base64" });
      tree.push({ path: `Banners/${userId}.jpg`, mode: "100644", type: "blob", sha: blob.sha });
      index[userId] = blob.sha.slice(0, 12);
    } else {
      if (!(userId in index)) return;
      tree.push({ path: `Banners/${userId}.jpg`, mode: "100644", type: "blob", sha: null });
      delete index[userId];
    }
    tree.push({ path: "Banners/index.json", mode: "100644", type: "blob", content: JSON.stringify(index, null, 1) + "\n" });
    const newTree = await github(env, "git/trees", "POST", { base_tree: head.tree.sha, tree });
    const created = await github(env, "git/commits", "POST", {
      message: `${bytes ? "Banner" : "Remove banner"} ${userId} [skip ci]`,
      tree: newTree.sha,
      parents: [ref.object.sha],
    });
    const moved = await github(env, `git/refs/heads/${env.BRANCH}`, "PATCH", { sha: created.sha }, true);
    if (moved) return;
  }
  throw new Error("branch kept moving");
}

async function readIndex(env, commitSha) {
  const response = await fetch(`https://api.github.com/repos/${env.REPO}/contents/Banners/index.json?ref=${commitSha}`, {
    headers: headers(env, "application/vnd.github.raw"),
  });
  if (response.status === 404) return {};
  if (!response.ok) throw new Error(`index: ${response.status}`);
  return JSON.parse(await response.text());
}

async function github(env, path, method = "GET", body, soft = false) {
  const response = await fetch(`https://api.github.com/repos/${env.REPO}/${path}`, {
    method,
    headers: headers(env, "application/vnd.github+json"),
    body: body ? JSON.stringify(body) : undefined,
  });
  if (soft && response.status === 422) return null;
  if (!response.ok) throw new Error(`${method} ${path}: ${response.status} ${await response.text()}`);
  return response.json();
}

function headers(env, accept) {
  return {
    Authorization: `Bearer ${env.GITHUB_TOKEN}`,
    Accept: accept,
    "User-Agent": "pixelgram-banners",
    "Content-Type": "application/json",
  };
}

function base64(bytes) {
  let binary = "";
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

async function reply(env, chatId, text) {
  const response = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/sendMessage`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ chat_id: chatId, text }),
  });
  if (!response.ok) console.log(`sendMessage failed: ${response.status} ${await response.text()}`);
}
