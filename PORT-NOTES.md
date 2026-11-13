# BBS AM (AfterMotion) - port to BBS FS 2.4

Original addon: KDR4, MIT license (see LICENSE). This is the same addon, adapted to BBS FS 2.4
(Minecraft 1.20.1). Do not install it together with the 2.7 build: both use the mod id `afterimage`.

## Build
1. Have the BBS FS 2.4 jar (Minecraft 1.20.1) in `libs/`, in the project folder, or in its parent folder -
   any file name. The build finds it by itself (and says so if it only finds a jar of another version).
   To point at a jar by hand, add `bbs_jar_path=C:/path/to/bbs.jar` to gradle.properties.
2. `gradlew build` -> `build/libs/bbs-am-2.4-1.0.0.jar`.

## What was changed (BBS 2.4 has no addon API from 2.7)
- Entrypoint: only the `bbs-addon` key exists in 2.4. The client half is attached from it, client-side only.
- Clip registered on `RegisterSettingsEvent` (2.4 sends it after the clip factory exists); editor panel via `UIClip.register`.
- Film tick / render / shutdown and the model pose are hooked with 3 small Mixins instead of `FilmEvents` / `FormPoseEvents`.
- `PoseCapture` works on `ModelGroup` / `BOBJBone` (no `RigBone` in 2.4).
- Entities are looked up by replay index (2.4); the clip's "target" is still the replay id, as before.
- The editor widgets helpers that 2.7 has in `UIClip` (`bind`, `toggle`, `trackpad`, ...) were added to the panel class.
- Language files are reloaded after registration (2.4 loads strings before the event).

## Known difference
2.7 colours model forms with a flat overlay (`Form.overlayColor`). 2.4 has no such feature,
so colours are applied as a tint (texture x colour). Colour modes work; the look is not identical.
