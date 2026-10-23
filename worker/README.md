# Banners worker

A Cloudflare Worker behind @pixelGramCI_bot. pixelGram sends the bot a JPEG with `/banner` in the caption, or `/remove`.
The worker checks the file and commits `Banners/<userId>.jpg` and `Banners/index.json` in one commit marked `[skip ci]`.
The build workflow also ignores `Banners/**`, so banner commits never start a build.

## Setup

1. Create a fine-grained GitHub token with **Contents: Read and write** for this repository only.
2. Install Wrangler and deploy from this folder:
   ```
   npm install -g wrangler
   wrangler login
   wrangler deploy
   wrangler secret put BOT_TOKEN
   wrangler secret put GITHUB_TOKEN
   wrangler secret put WEBHOOK_SECRET
   ```
   `WEBHOOK_SECRET` is any random string you make up.
3. Point the bot at the worker:
   ```
   curl "https://api.telegram.org/bot<BOT_TOKEN>/setWebhook" \
     -d url=https://pixelgram-banners.<your-subdomain>.workers.dev \
     -d secret_token=<WEBHOOK_SECRET> \
     -d allowed_updates='["message"]'
   ```

To block someone, add their user ID to `BANNED` in `wrangler.toml` (comma separated) and deploy again.
Clients read banners from `raw.githubusercontent.com`, so the repository has to be public.
