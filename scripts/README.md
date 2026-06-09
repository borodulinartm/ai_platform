# Agent Manager

Unified PowerShell manager for three AI agents: **Hermes Desktop**, **JiuwenSwarm**, and **OpenClaw**.

## Quick Start

Double-click `Agent-Manager.bat` or run in terminal:

```
.\Agent-Manager.ps1
```

## Per-Agent Scripts

Each agent can also be run standalone:

| Agent | Script | Actions |
|-------|--------|---------|
| Hermes Desktop | `.\HermesDesktop.ps1` | install, uninstall, status, update |
| JiuwenSwarm | `.\JiuwenSwarm.ps1` | install, uninstall, status, update |
| OpenClaw | `.\OpenClaw.ps1` | install, uninstall, status, update |

Examples:

```
.\HermesDesktop.ps1 status
.\JiuwenSwarm.ps1 install
.\OpenClaw.ps1 uninstall
```

## Configuration

All configuration is in `config.json` — a single shared file for credentials, providers, models, and per-agent defaults.

### `config.json` structure

**`providers`** — API endpoint definitions:

- `base_url` — API endpoint URL
- `api_key` — API key for authentication
- `provider_type` — `custom` for OpenAI-compatible
- `description` — Human-readable label

**`models[]`** — Available models:

- `id` — Model slug used in requests
- `name` — Display name
- `provider` — References a key in `providers`
- `context_length` — Max context window in tokens
- `max_output_tokens` — Max output tokens
- `supports_vision` — Whether model accepts images
- `supports_tools` — Whether model supports function calling
- `type` — Omit for chat models, `"embedding"` for embedding models

**`agents`** — Per-agent defaults:

- `provider` — Which provider to use
- `model` — Default model ID for chat
- `embed_model` — (JiuwenSwarm only) Embedding model ID
- `version` — (OpenClaw only) Pin a specific version

### Example

```json
{
  "providers": {
    "MyProvider": {
      "base_url": "https://api.example.com/v2",
      "api_key": "your-api-key-here",
      "provider_type": "custom",
      "description": "My OpenAI-compatible endpoint"
    }
  },
  "models": [
    {
      "id": "my-model",
      "name": "My Model",
      "provider": "MyProvider",
      "context_length": 128000,
      "max_output_tokens": 4096,
      "supports_vision": false,
      "supports_tools": true
    },
    {
      "id": "my-embed-model",
      "name": "My Embedding Model",
      "provider": "MyProvider",
      "context_length": 8192,
      "max_output_tokens": 0,
      "supports_vision": false,
      "supports_tools": false,
      "type": "embedding"
    }
  ],
  "agents": {
    "Hermes": { "provider": "MyProvider", "model": "my-model" },
    "JiuwenSwarm": { "provider": "MyProvider", "model": "my-model", "embed_model": "my-embed-model" },
    "OpenClaw": { "provider": "MyProvider", "model": "my-model", "version": "2026.3.13" }
  }
}
```

Chat models without `type` are injected into OpenClaw's provider config.
Models with `"type": "embedding"` are used by JiuwenSwarm's `EmbedModel`.

Logs are written to `~\agent-logs\`.

## Notes

- OpenClaw gateway runs in a separate cmd window (required by the Node.js process)
- Hermes Desktop installs silently with pre-seeded config (no first-run wizard)
- JiuwenSwarm uses `uv tool install` with port-based process management