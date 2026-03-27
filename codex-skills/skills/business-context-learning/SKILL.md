---
name: business-context-learning
description: Learn, maintain, and refine ongoing business or project context from user explanations across multiple turns. Use when Codex needs to absorb domain background, account models, ownership boundaries, mapping rules, operational constraints, or change motivations before analysis, review, or implementation, especially in long-running enterprise systems where facts arrive incrementally and must be separated into confirmed facts, inferences, and open questions.
---

# Business Context Learning

Capture business context as a stable working model that later code changes can trust. Keep business facts, technical evidence, and open questions explicitly separated so the project does not drift into implementation based on guessed assumptions.

## Workflow

1. Restate the user's latest explanation into three buckets: `已确认事实`, `当前推断`, `待确认问题`.
2. Normalize domain terms immediately. For each important field or concept, write down its current meaning, owner, and audience.
3. Write mapping relations in cardinality form such as `1:1`, `1:N`, `N:1`, or `A + B + C -> D`.
4. Ask only the highest-value clarification questions first. Prioritize boundaries that change aggregation scope, source-of-truth ownership, alert semantics, or uniqueness constraints.
5. Record why the business wants the change, not just which fields exist. The business driver often decides the correct aggregation layer.
6. Separate long-lived project context from task-specific implementation notes. Put stable business facts in a dedicated project markdown and keep task mechanics in task documents.
7. After every meaningful new batch of information, update the canonical project business-context markdown instead of letting the understanding live only in chat.
8. Before code work starts, reread the current fact ledger and surface any unresolved business boundary that could invalidate the implementation.

## What To Capture

Always try to capture these dimensions when the user is explaining a business system:

- Field meaning: what each identifier or enum represents.
- Ownership: which team or system controls each field.
- Visibility: which identifiers are visible to customers and which are internal.
- Mapping direction: how low-level and high-level accounts relate.
- Aggregation boundary: what constitutes one business bucket for money, risk, or alerts.
- Calculation boundary: which numbers can be summed and which require a different computation model.
- Operational reality: why the business actually prefers the new behavior.
- Future variants: nearby scenarios that are not in scope yet but may reuse the same model.

## Question Heuristics

Prefer targeted questions that reduce architectural ambiguity. Good clarification themes include:

- Is a field globally fixed or customer-configurable?
- What is the true aggregation boundary?
- Which team owns the source of truth?
- Which system computes derived values such as margin or limit usage?
- Which old behaviors must remain valid?
- What does the real payload look like in the new scenario?
- Is a new field additive metadata, or does it imply a new business level?

Avoid broad prompts that force the user to re-explain everything. Ask the smallest question that can settle an important boundary.

## Documentation Rules

- Keep a canonical business-context markdown in the workspace root when the user wants project facts persisted.
- Use stable sections such as `文档目的`, `已确认事实`, `当前业务模型`, `当前最重要的业务边界`, and `待补事实`.
- Prefer concise sentences over long narratives so later implementation work can scan quickly.
- If a statement comes from the user, record it as business confirmation.
- If a statement comes from code reading, mark it as code evidence rather than business confirmation.
- Never quietly upgrade an inference into a confirmed fact.

## Handoff Standard

When pausing or handing off to implementation work, leave behind:

1. A short summary of the current business model.
2. The highest-risk unresolved questions.
3. The exact project markdown that now holds the stable facts.
