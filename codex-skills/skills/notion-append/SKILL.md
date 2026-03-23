---
name: notion-append
description: Append content to a Notion page using direct API calls. Use when you need to add commands, notes, or documentation to Notion pages.
---

# Notion Append

Append formatted content to Notion pages via direct API calls.

## Workflow

1. Search for the target page by title
2. Extract the page ID from search results
3. Append content blocks (headings, code, lists) to the page

## Usage

When user asks to "write to notion", "add to notion page", or "update notion":
1. Ask for page title if not provided
2. Search: `curl -X POST https://api.notion.com/v1/search -H "Authorization: Bearer $NOTION_API_KEY" -H "Notion-Version: 2022-06-28" -H "Content-Type: application/json" -d '{"query":"PAGE_TITLE","page_size":5}'`
3. Append: `curl -X PATCH https://api.notion.com/v1/blocks/PAGE_ID/children -H "Authorization: Bearer $NOTION_API_KEY" -H "Notion-Version: 2022-06-28" -H "Content-Type: application/json" -d '{"children":[...]}'`

## Block Types

- heading_3: `{"type":"heading_3","heading_3":{"rich_text":[{"type":"text","text":{"content":"Title"}}]}}`
- code: `{"type":"code","code":{"rich_text":[{"type":"text","text":{"content":"code"}}],"language":"bash"}}`
- bulleted_list_item: `{"type":"bulleted_list_item","bulleted_list_item":{"rich_text":[{"type":"text","text":{"content":"item"}}]}}`

## API Key

Read from MCP config: `~/.kiro/settings/mcp.json` or `~/.codex/settings/mcp.json` under `mcpServers.notion.env.NOTION_API_KEY`
