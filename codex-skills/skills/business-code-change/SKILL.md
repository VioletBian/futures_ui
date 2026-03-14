---
name: business-code-change
description: Implement business-code changes driven by a concrete task in an existing workspace. Use when Codex needs to change real backend or middle-tier logic under task constraints such as "工作业务代码因任务需求改动", especially for locking the minimal file set, making incremental code edits, preserving compatibility, adding Chinese comments on substantial updated blocks, and explicitly calling out missing facts or residual risks.
---

# Business Code Change

Implement the smallest defensible business-code change set for a task that already names the scope, constraints, and target behavior. Prioritize real code edits over broad redesign, and keep reasoning tied to concrete files and call paths.

## Workflow

1. Read the task statement and the explicitly referenced files first.
2. Lock the smallest necessary file set before editing anything.
3. Trace the real runtime entry points and data flow in the current workspace instead of assuming generic layers.
4. Change model, loader, matcher, or generator code only where the task requires.
5. Preserve backward compatibility unless the task explicitly says to break it.
6. Add a Chinese comment ahead of each substantial updated code block to explain purpose.
7. If a critical fact is missing, stop guessing and name the exact class, method, or find-usage the user should retrieve from the full source tree.

## Editing Rules

- Prefer additive or narrowly refactored changes over deleting existing code.
- Keep unrelated modules out of scope even if they are part of the broader system.
- Treat reconstructed or partial source files as evidence-based stubs: add only the minimal structural completion needed for the current task.
- When a model is incomplete, add the smallest accessor or helper set required by real callers and note that it is a structural completion.
- Reuse one shared helper path for duplicated business semantics so threshold, polling, and snapshot paths do not drift.

## Output

Finish with three parts:

1. What changed.
2. Why the implementation is shaped this way.
3. Residual risks, including any facts that still need confirmation from the full source repository.
