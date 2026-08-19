# InspirationWall implementation contract

- Package: `com.spatialapps.inspirationwall`.
- Default root: one Shared Space Volumetric `DefaultWindowContainer` named `InspirationWorkbench`.
- Physical plane detection and persistent anchors are Stage-only. Open one secondary Mixed `Stage` named `WallAnchorStage` after explicit consent; always provide a stable exit back to Shared Space.
- UI uses `PicoTheme` and SpatialUI/Compose primitives. Do not import Material or Material3 components.
- The root is intentionally opaque (`pico.spatial.windowcontainer.materialbackground=0`); keep the `// design-style: opaque-root` signal beside its root background.
- Persist walls, groups, cards, transforms, and anchor metadata with Room. Store images/doodles as private local files and persist only URIs/relative paths.
- Speech input must use on-device recognition when available; never retain audio. Keyboard entry is the required fallback.
- Never claim emulator evidence for physical anchors, plane detection, comfort, controller precision, performance, or 60fps.
- Preserve normalized wall coordinates and deterministic z ordering; filter deleted cards before rank normalization.
- Run unit tests, design-style verification, build, install, launch, screenshot, and logcat checks before handoff.
