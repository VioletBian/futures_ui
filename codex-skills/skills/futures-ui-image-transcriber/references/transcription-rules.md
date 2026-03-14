# Transcription Rules

Apply these rules whenever recreating code from the referenced source images:

1. Recreate the visible code files and folder layout as closely as possible.
2. Lock down package path, class name, fields, constructor signature, and visible method signatures from the image before writing the file.
3. Read the referenced images in filename order and map each image to a concrete class or tree node before editing.
4. Use OCR only as a quick locator pass for dense trees or code regions, then manually re-check each class. Never trust OCR alone for identifiers, helper methods, package placement, or control flow.
5. If OCR fails or the full image is too dense, crop or zoom the relevant region locally and re-read it manually before editing.
6. If the image class name or filename conflicts with the current workspace file, rename the file/class first and then sweep for stale references in code and docs.
7. Ignore IDE overlays such as inline parameter-name hints, blame metadata, usage counts, and review decorations.
8. If a fragment is unreadable, keep the surrounding code and insert a short inline comment marking the uncertainty.
9. Ignore missing third-party classes or libraries. The output does not need to compile.
10. Prefer the visible project tree when choosing module names, package paths, and file locations.
11. Prefer dedicated project-tree images and direct code images over any separately written analysis markdown unless the user explicitly confirms that markdown as reliable.
12. If a new code image contradicts an older transcription, update the transcription to match the image and re-check dependent classes instead of preserving guessed code.
13. Do not mark a class as reviewed unless there is a direct image for that class in the current source set. Mention missing source explicitly instead of guessing.
14. If the tree source makes a filename or path correction obvious, repair that structure even when the code body still lacks a fresh close-up image. Keep those fixes labeled as structural repairs.
15. For model or decompiled class views, do not invent routine getters, builders, or hidden tail members beyond the readable region. Add only clearly implied structural stubs and label the uncertainty.
16. If the user explicitly authorizes model-class getter/setter completion, supplement only bean-style accessors required by real call sites and supported by visible or directly implied fields.
17. If the user reports IDE index facts such as find-usage results, call hierarchy, or jump-to-definition from the complete internal source tree, treat those relation edges as highly trusted evidence.
18. If a relation edge is already confirmed by direct code or user-confirmed IDE index evidence, do not reconstruct a huge surrounding class unless the current task depends on its internals.
19. After a direct-image correction, search for stale old names and for callers that rely on members not visible in the corrected source; report unresolved dependency gaps explicitly.
20. Replace `com.gs` with `com.xx`.
21. Replace `goldmansachs` and `gs` with `xx` when they appear in identifiers, packages, or documentation that must be rewritten.
22. Replace `aviator` with `evetor`.
23. Skip Git author or blame comment blocks entirely.
24. Avoid wording that refers to the source as a photo or screenshot if the task forbids those terms.
25. If a class still has material ambiguity after a careful visual pass, ask the user for more images and name the exact class, method, or IDE lookup result needed.
26. When the task improves architectural or business understanding, write or update a concise markdown note in the workspace and mark unresolved ambiguities explicitly.
27. If the logic chain is clear enough, add a concise class/module flow diagram near the top of that markdown note, preferably in `mermaid`, using only confirmed or explicitly user-confirmed relation edges.

When a task includes a dedicated structure source, treat that structure source as authoritative for where files belong.
