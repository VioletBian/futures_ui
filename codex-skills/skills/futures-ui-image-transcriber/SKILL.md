---
name: futures-ui-image-transcriber
description: Transcribe code and directory structures from referenced source images into repository files with near 1:1 fidelity. Use when Codex needs to recreate module trees, source files, or configuration files from IDE/editor captures or photographed screens, especially when the task requires preserving visible folder structure, inserting comments for unreadable fragments, and applying workspace-specific replacement rules such as package or vendor renames.
---

# Futures Ui Image Transcriber

## Workflow

Follow this workflow:

1. Read [`references/transcription-rules.md`](./references/transcription-rules.md) and extract every mandatory replacement or omission rule before writing code.
2. Establish source authority before touching code. Use this order unless the user overrides it:
   `directory_structure` or other dedicated tree images > per-class close-up code images > broader code images > existing workspace files > separately written markdown.
3. Read the referenced images in filename order and keep a small source ledger while working: image filename -> inferred class or tree node -> target file path -> confidence. Do not edit a class until that mapping is explicit.
4. Do a quick OCR pass first only as a locator, then re-check class-by-class manually. When the image is dense, crop or zoom the relevant region locally before transcribing.
5. Inspect the referenced images with vision first. Use [`scripts/ocr_images.swift`](./scripts/ocr_images.swift) only to narrow down tree nodes or dense regions; never accept OCR output as the final source for method names, fields, package paths, or control flow.
6. If the full image is too dense or OCR fails, crop or zoom the relevant region locally and re-read it before editing. Prefer manual zoomed inspection over repeated OCR retries.
7. Before editing a class, explicitly lock down from the image: package line, target path, class name, fields, constructor signature, visible method signatures, and any helper methods that appear in the file. Fix path/package mismatches first.
8. If the image class name or filename conflicts with the current workspace file, rename the file and class to match the image before touching method bodies. After that, search the workspace for stale references and update documentation or callers that still use the old name.
9. Ignore IDE-only overlays while transcribing. Do not copy parameter-name hints such as `queryParams:` or `consumer:`, blame metadata, usage counts, tabs, or review annotations into source files.
10. Rebuild the visible directory tree before filling file contents. If the tree image conflicts with existing code placement, the tree image wins and dependent imports or references should be revisited in the same pass.
11. Transcribe code into the target workspace with the visible naming, indentation, member order, and helper-method presence preserved as closely as possible.
12. Immediately perform a second visual pass on each transcribed class. Re-check package, filename, field list, constructor parameters, return types, helper methods, and any previously missing functions. Do not move on while obvious mismatches remain.
13. When later close-up images arrive for a class, re-audit adjacent or dependent classes as well. Package placement drift, renamed methods, missing helpers, wrong filenames, and stale docs often propagate beyond the newly photographed file.
14. When any token is unreadable, keep only the confirmed structure and insert a short inline comment marking the uncertain fragment. Do not silently invent method bodies, helper methods, imports, or branches just to make the file look complete.
15. For large multi-image model or prototype classes, first lock a field ledger: visible section heading -> field names in order -> readable type tokens -> confidence. Treat the field declaration block as the anchor before editing getters, builders, equals/hashCode, or toString.
16. When a later image batch mainly covers the tail half of a large class, default to additive repair. If an edit unexpectedly deletes, reorders, or renames earlier confirmed fields or section headers, stop and reconcile against the earlier front-half images and the local diff before saving.
17. After patching the tail of a large model or prototype class, run a consistency sweep across `fields -> getters/setters -> builder setters -> equals/hashCode -> toString`. Only then adjust validation helpers or tests that depend on those members.
18. For decompiled or model-definition views, do not assume hidden getters, builders, or generated members beyond the readable region. Add only directly implied structural stubs when needed, and label the uncertainty inline.
19. Exception for model classes: if the user explicitly allows it, bean-style getters/setters may be supplemented from real workspace call sites when the backing field is visible or the member existence is directly proven by those call sites. Mark such additions as structural repair rather than direct-image confirmation.
20. The user may have access to a complete internal source tree with IDE index features such as find-usage, call hierarchy, or jump-to-definition, even when you do not. Treat specific index results that the user reports back, or images of those indexed call sites, as highly trusted evidence for relationship edges and source-of-truth confirmation.
21. When a user provides find-usage or call-hierarchy evidence, incorporate it explicitly into the source ledger as `user-confirmed index relation`, and use it to replace guesses about upstream/downstream callers.
22. If a relation edge is already confirmed by direct code, tree structure, or user-confirmed IDE index evidence, do not expand scope into reconstructing a huge surrounding class unless the current task depends on that class's internal logic.
23. After correcting a class from direct images, run a quick workspace search for stale old names and for callers that depend on members not visible in the corrected source. Report those dependency gaps explicitly instead of pretending they are confirmed.
24. Only claim a class is corrected when there is direct image support for that class. If a requested class has no corresponding image in the current batch, say so explicitly and do not pretend the class was re-verified.
25. If a prior pass created placeholder files or wrong filenames and the tree image now makes the real path obvious, fix the structure in the same pass. For code bodies without direct close-up support, limit the change to structural repair or directly implied call-site fixes.
26. If the remaining ambiguity is material, ask the user for more images and name the exact class, method, screen region, or IDE lookup result needed. Prefer a precise reshoot or find-usage request over speculative completion.
27. When the reconstruction materially improves understanding of code flow, architecture, or business behavior, write or update a concise markdown note in the workspace that records the current understanding and unresolved questions.
28. If the logic chain is already clear enough to explain end-to-end, add a compact class-or-module flow diagram near the top of that markdown note. Prefer `mermaid` and keep it limited to confirmed or explicitly user-confirmed relation edges.
29. Validate the output against the visible structure and the replacement rules, then report which classes are confirmed, which were only structurally repaired, and which still need better images.

## Reconstruction Rules

- Prefer the image-derived folder and package structure over any local guesswork.
- Prefer user-provided project-tree images and direct code images over any previously generated markdown analysis.
- If a code image contradicts an earlier transcription, the code image wins. Remove or simplify the guessed code instead of trying to preserve it.
- Do not trust a class just because one method now matches. Re-check sibling methods, helper methods, and dependent imports when a class has already been transcribed once.
- Do not infer that a named class has been covered just because it was mentioned in the user message. Confirm that a direct source image for that class exists in the referenced batch.
- If a class name in the image disagrees with the current file name, the image wins. Rename the file and then sweep for stale references.
- For large model or prototype classes split across many images, treat the field declaration block as the anchor source. Reconcile later methods and builder sections back to that field list before trusting a tail-only patch.
- If a tail-image repair causes unexpected deletions, reorderings, or section drift in earlier confirmed fields, assume regression until the diff is reconciled against the earlier images.
- For these large classes, explicitly cross-check `fields -> getters/setters -> builder setters -> equals/hashCode -> toString -> validation helpers -> referenced tests/JSON`.
- If a model or decompiled class is only partially visible, keep the confirmed visible portion and mark the missing lower region. Do not backfill routine getters or builder logic unless they are directly implied by visible code and clearly labeled as structural repair.
- If the user explicitly authorizes model-class getter/setter completion, supplement only bean-style accessors that are required by real call sites and supported by visible or directly implied fields.
- If the user reports IDE index facts such as callers, implementations, or find-usage results from the full source tree, treat those relation edges as solid evidence even if you still need images for local code text.
- Once a relation edge is solidly confirmed, keep scope tight. Do not reconstruct a giant unrelated class just for completeness unless the current task depends on it.
- When writing an understanding markdown and the chain is sufficiently stable, place a concise flow diagram near the top so the class/module relations can be scanned before reading prose.
- When OCR fails on a dense image, switch to cropped manual inspection instead of reusing a weak transcription.
- Distinguish three states in status reporting: `confirmed by direct image`, `structurally repaired from tree/local consistency`, and `still missing solid source`.
- Keep generated code ASCII unless the target file already uses non-ASCII.
- Preserve incomplete snippets when possible; annotate uncertainty inline instead of deleting the fragment.
- Skip author-attribution comment blocks when the source shows Git or editor blame metadata.
- Skip IDE parameter hints and inline type hints when the source shows them.
- Avoid writing prose that refers to the source as a photo or screenshot if the user has prohibited that wording.

## OCR Helper

Compile the helper when needed:

```bash
swiftc -framework Vision scripts/ocr_images.swift -o /tmp/ocr-images
```

Run it on a single file at a time for best stability:

```bash
/tmp/ocr-images /absolute/path/to/image.jpg
```

Use OCR output to narrow down class names, package names, and long member lists. Re-check the source image before accepting any ambiguous token.

## Output Standard

- Create the visible directory structure first when the task spans multiple modules.
- Keep file names and package declarations aligned with the rewritten namespace rules from the reference file.
- Favor correctness of core code over compilability.
- End with a short note listing unresolved unreadable fragments, if any.

## Resources

- [`references/transcription-rules.md`](./references/transcription-rules.md): task-specific replacements and omissions.
- [`scripts/ocr_images.swift`](./scripts/ocr_images.swift): local OCR helper for extracting dense code or folder trees.
