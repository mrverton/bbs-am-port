# BBS AM (AfterMotion) — port for BBS FS 2.4

Fading afterimages (ghost copies) behind characters in [BBS FS](https://github.com/Wemppy4/bbs-fs) films,
set up with a single **Afterimage** clip on the timeline.

This repository is a **port of the BBS AM addon from BBS FS 2.7 to BBS FS 2.4** (Minecraft 1.20.1, Fabric).
The original addon was written by **KDR4** (MIT). The ghost logic, the clip and all of its settings are the
original code; what was ported is everything that connects the addon to BBS, because BBS FS 2.4 has no addon
API like 2.7 has (details below).



> **Status.** Ported and checked by compiling against the BBS FS 2.4 sources (see *Verification*). Please test
> on a copy of your project and report problems in Issues.

## Which version do I need?

| BBS FS | Use |
|--------|-----|
| **2.4** (1.20.1) | **this port** (`bbs-am-2.4-*.jar`) |
| 2.7 (1.20.1) | the original BBS AM by KDR4 |

Both use the mod id `afterimage` — **never install both**.

## Requirements
- Minecraft 1.20.1, Fabric Loader, Fabric API
- BBS FS 2.4

## Install
Put `bbs-am-2.4-1.0.0.jar` into `mods`, next to BBS FS 2.4 and Fabric API.

## Quick start
1. Open a film in BBS and go to the clip timeline.
2. *Add clip* → **Afterimage**.
3. Put the clip on a layer above the camera clip and stretch it over the part you want.
4. Click the clip: all settings are on the right.

Afterimages are off by default and only appear while the clip is active. To get them on every replay without a
clip, turn on *Afterimages everywhere* in the settings menu. Clip settings: target replay (or all), spawning
(always / only while moving, interval, distance), lifetime (hold, fade, start visibility), colour (original, solid,
gradient, two alternating, rainbow), motion (drift direction and speed). Full description: [`DOCS.md`](DOCS.md).
The addon switches itself off after three internal errors instead of crashing the game.

## Build from source
1. Put the **BBS FS 2.4** jar (Minecraft 1.20.1) into `libs/`, into the project folder, or into its parent
   folder — any file name. The build finds it itself: it takes a jar that contains BBS's main class and whose
   `fabric.mod.json` says version 2.4, and tells you if it only finds a jar of another version.
   To point at a jar by hand add `bbs_jar_path=C:/path/to/bbs.jar` to `gradle.properties`.
2. Run `gradlew build` (or `build.bat` on Windows).
3. The mod is `build/libs/bbs-am-2.4-1.0.0.jar` (`build.bat` also copies it to `READY_MOD/`).

The first build downloads Minecraft and the mappings, which takes a while.

## How the port was made

**1. Pinning the exact source.** BBS FS has no release tags, so the 2.4 source was taken from the git history of
the `1.20.1` branch: the commit that bumped the version to 2.4 (`cff7d87a`) and the last commit before the 2.5
bump (`70274c5c`). Everything below was checked against both.

**2. The finding.** The addon is written against a formal addon API (`BBSAddonMod`, `@Subscribe`,
`RegisterCameraClipsEvent`, `RegisterClipPanelsEvent`, `BBSClientReadyEvent`, `FilmEvents`, `FormPoseEvents`,
`BBSApi`). That API does not exist in 2.4 (nor in 2.5) — it appears later, before 2.7. So a plain recompile
against the 2.4 jar is impossible, and every place where the addon touched BBS had to be re-done on top of what
2.4 really has. BBS 2.4 does have an older, smaller event bus (`mchorse.bbs_mod.events`) with five events, and
that covers part of the job.

**3. Mapping, 2.7 mechanism → 2.4 replacement**

| 2.7 | 2.4 |
|-----|-----|
| `bbs-addon` + `bbs-client-addon` entrypoints | only the `bbs-addon` key is read; the client half is attached from it, on the client only |
| `RegisterCameraClipsEvent` | handler of `RegisterSettingsEvent` → `BBSMod.getFactoryCameraClips().register(...)` |
| `RegisterClipPanelsEvent` | `UIClip.register(Class, factory)` (public static) |
| `registerAddon(modId, class)` | `provider.register(new InternalAssetsSourcePack(modId, "assets/<id>/assets", class))` |
| `RegisterL10nEvent` / `RegisterClientSettingsEvent` | same events exist in 2.4 (`events.register`) |
| `FilmEvents.TICK_AFTER / RENDER_AFTER / SHUTDOWN` | Mixins: `BaseFilmController.updateEntities` (RETURN), `render` (RETURN), `shutdown` (HEAD), and `WorldFilmController.shutdown` (HEAD) |
| `FormPoseEvents.MODEL_POSE` | Mixin: `ModelFormRenderer.applyPhysicsOnce` (HEAD) |
| `RigBone` | `ModelGroup` (`current`, `orient`, `offset`) and `BOBJBone` (`transform`, `orient`, `offset`) |
| `Form.overlayColor` | multiplicative tint (see *Differences*) |
| `UIClip.bind / toggle / trackpad / textbox / color` | the same helpers written into the panel class, refreshed in `fillData()` |
| `controller.getEntities()` keyed by replay **id** | `controller.entities`, an `IntObjectMap` keyed by replay **index** |

## Porting peculiarities (the things that are easy to get wrong)
- **The 2.4 event bus swallows exceptions.** A failure inside an addon handler is silent, so the clip
  registration prints its stack trace itself.
- **Only one entrypoint key.** 2.4 reads `bbs-addon` only. Putting client-only types into that class would break
  dedicated servers, so the client half lives in a separate class that is attached only when the environment is
  the client.
- **Timing of the clip registration.** The camera clip factory is built inside `BBSMod.onInitialize`, *after*
  `RegisterSourcePacksEvent` but *before* `RegisterSettingsEvent`, so the second one is the right moment.
- **Languages load before the event.** 2.4 calls `l10n.reload()` before it sends `RegisterL10nEvent`, so the
  addon calls `reload()` itself after registering its files; otherwise the texts would show as raw keys.
- **Subclasses decide where a hook goes.** `WorldFilmController` calls `super` for update and render, so hooking
  the base class covers it — but its `shutdown()` does **not** call `super`, so it needs its own hook.
  `FilmEditorController.updateEntities` calls `super` with an adjusted tick, so the base hook sees the right tick.
- **The pose hook point.** In 2.4 `renderModel` runs IK, then physics, then constraints, and `applyPhysicsOnce`
  is called once per render — its start is exactly "after animation and IK, before physics", which is what the
  2.7 pose event provided.
- **Entities by index, targets by id.** The film's entities are looked up by replay index in 2.4; the clip still
  stores its target as the replay id (so the saved clip data keeps the same fields), and the actor picker, which
  works by index in 2.4, converts between the two.
- **Field initializers and `registerUI()`.** In 2.4 `registerUI()` runs inside the `UIClip` constructor, so a field
  of the panel class with an initializer would be reset to `null` right after. The list of widget refreshers is
  therefore declared without an initializer and created on first use.

## Differences from the 2.7 version
- **Model colour.** 2.7 paints model ghosts with a flat overlay (`Form.overlayColor`). 2.4 has no such feature,
  so the colour is applied as a tint (texture × colour). All colour modes work, but the look is not identical.
- **Target replay ids.** In 2.4 a replay's id is the list size at the time it was created, so ids can repeat after
  deleting replays. If that happens, one clip target may match two replays.

## Verification
- Every BBS class and member the addon uses was checked against the 2.4 source at both commits.
- The addon sources were compiled with `javac` against the BBS 2.4 sources, with small stubs standing in for the
  Minecraft, Fabric, Mixin and JOML classes. Result: no errors; the same check reports the error it should on a
  deliberately broken file.
- This is **not** a full Gradle/Loom build and **not** an in-game test of every feature.

## Credits and licence
- BBS AM (AfterMotion): **KDR4**, MIT.
- BBS FS: [Wemppy4](https://github.com/Wemppy4/bbs-fs); BBS mod: McHorse.
- This port keeps the MIT licence of the original, see [`LICENSE`](LICENSE).

## Reporting a problem
Attach `logs/latest.log` and the versions of BBS FS, Fabric Loader and this addon.


# BBS AM (AfterMotion) — порт для BBS FS 2.4

Затухающие послеобразы (копии-призраки) за персонажами в фильмах [BBS FS](https://github.com/Wemppy4/bbs-fs),
настраиваются одним клипом **«Послеобраз»** на таймлайне.

Это **порт аддона BBS AM с BBS FS 2.7 на BBS FS 2.4** (Minecraft 1.20.1, Fabric). Оригинальный аддон написал
**KDR4** (MIT). Логика призраков, сам клип и все его настройки — оригинальный код; портировано всё, что связывает
аддон с BBS, потому что в BBS FS 2.4 нет системы аддонов, как в 2.7 (подробности ниже).

[English version](README.md)

> **Статус.** Порт проверен компиляцией против исходников BBS FS 2.4 (см. «Проверка»). Пробуйте на копии проекта
> и сообщайте о проблемах в Issues.

## Какая версия мне нужна

| BBS FS | Что ставить |
|--------|-------------|
| **2.4** (1.20.1) | **этот порт** (`bbs-am-2.4-*.jar`) |
| 2.7 (1.20.1) | оригинальный BBS AM от KDR4 |

У обеих версий один id мода `afterimage` — **не ставь их вместе**.

## Требования
- Minecraft 1.20.1, Fabric Loader, Fabric API
- BBS FS 2.4

## Установка
Положи `bbs-am-2.4-1.0.0.jar` в `mods` рядом с BBS FS 2.4 и Fabric API.

## Быстрый старт
1. Открой фильм в BBS и перейди на таймлайн клипов.
2. «Добавить клип» → **Послеобраз**.
3. Поставь клип на слой выше клипа камеры и растяни по нужному участку.
4. Нажми на клип — все настройки справа.

По умолчанию послеобразы выключены и появляются только пока клип активен. Чтобы они были у всех реплеев без клипа,
включи «Послеобразы везде» в настройках. Настройки клипа: цель (реплей или все), появление (всегда / только в
движении, интервал, дистанция), время жизни (удержание, затухание, начальная видимость), цвет (оригинал, один цвет,
градиент, два по очереди, радуга), снос (направление и скорость). Полное описание: [`DOCS.md`](DOCS.md).
После трёх внутренних ошибок аддон выключается сам, а не роняет игру.

## Сборка из исходников
1. Положи jar **BBS FS 2.4** (Minecraft 1.20.1) в `libs/`, в папку проекта или в её родительскую папку — имя файла
   любое. Сборка находит его сама: берёт jar, в котором есть главный класс BBS и в `fabric.mod.json` версия 2.4, а если
   нашла только jar другой версии — сообщает об этом. Указать jar вручную можно строкой
   `bbs_jar_path=C:/путь/к/bbs.jar` в `gradle.properties`.
2. Запусти `gradlew build` (или `build.bat` в Windows).
3. Мод: `build/libs/bbs-am-2.4-1.0.0.jar` (`build.bat` ещё копирует его в `READY_MOD/`).

Первая сборка скачивает Minecraft и маппинги, это занимает время.

## Как сделан порт

**1. Фиксация исходников.** У BBS FS нет тегов релизов, поэтому исходники 2.4 взяты из истории ветки `1.20.1`:
коммит, поднявший версию до 2.4 (`cff7d87a`), и последний коммит перед версией 2.5 (`70274c5c`). Всё ниже сверено
с обоими.

**2. Главная находка.** Аддон написан под формальный API аддонов (`BBSAddonMod`, `@Subscribe`,
`RegisterCameraClipsEvent`, `RegisterClipPanelsEvent`, `BBSClientReadyEvent`, `FilmEvents`, `FormPoseEvents`,
`BBSApi`). В 2.4 (и в 2.5) этого API нет — он появился позже, до 2.7. Поэтому простая перекомпиляция против jar 2.4
невозможна, и каждое место, где аддон касался BBS, пришлось переделать на то, что в 2.4 реально есть. В 2.4 есть
старая небольшая шина событий (`mchorse.bbs_mod.events`) с пятью событиями — она закрывает часть задачи.

**3. Соответствие: механизм 2.7 → замена в 2.4**

| 2.7 | 2.4 |
|-----|-----|
| точки входа `bbs-addon` + `bbs-client-addon` | читается только ключ `bbs-addon`; клиентская половина подключается из него, только на клиенте |
| `RegisterCameraClipsEvent` | обработчик `RegisterSettingsEvent` → `BBSMod.getFactoryCameraClips().register(...)` |
| `RegisterClipPanelsEvent` | `UIClip.register(Class, factory)` (публичный статический) |
| `registerAddon(modId, class)` | `provider.register(new InternalAssetsSourcePack(modId, "assets/<id>/assets", class))` |
| `RegisterL10nEvent` / `RegisterClientSettingsEvent` | те же события есть в 2.4 (`events.register`) |
| `FilmEvents.TICK_AFTER / RENDER_AFTER / SHUTDOWN` | Mixin: `BaseFilmController.updateEntities` (RETURN), `render` (RETURN), `shutdown` (HEAD) и `WorldFilmController.shutdown` (HEAD) |
| `FormPoseEvents.MODEL_POSE` | Mixin: `ModelFormRenderer.applyPhysicsOnce` (HEAD) |
| `RigBone` | `ModelGroup` (`current`, `orient`, `offset`) и `BOBJBone` (`transform`, `orient`, `offset`) |
| `Form.overlayColor` | мультипликативная тонировка (см. «Отличия») |
| `UIClip.bind / toggle / trackpad / textbox / color` | те же хелперы дописаны в класс панели, обновляются в `fillData()` |
| `controller.getEntities()` по **id** реплея | `controller.entities` — `IntObjectMap` по **индексу** реплея |

## Особенности переноса (где легко ошибиться)
- **Шина событий 2.4 глотает исключения.** Сбой в обработчике аддона бесшумный, поэтому регистрация клипа сама
  печатает стектрейс.
- **Один ключ точки входа.** 2.4 читает только `bbs-addon`. Клиентские типы в этом классе сломали бы выделенные
  серверы, поэтому клиентская половина вынесена в отдельный класс и подключается только в клиентском окружении.
- **Момент регистрации клипа.** Фабрика клипов камеры строится внутри `BBSMod.onInitialize` *после*
  `RegisterSourcePacksEvent`, но *до* `RegisterSettingsEvent` — поэтому правильный момент именно второе событие.
- **Языки грузятся до события.** 2.4 вызывает `l10n.reload()` до отправки `RegisterL10nEvent`, поэтому аддон сам
  вызывает `reload()` после регистрации своих файлов; иначе тексты показывались бы сырыми ключами.
- **Куда ставить хук, решают подклассы.** `WorldFilmController` вызывает `super` в update и render, поэтому хука на
  базовом классе хватает — но его `shutdown()` `super` **не** вызывает, и ему нужен свой хук.
  `FilmEditorController.updateEntities` вызывает `super` со скорректированным тиком, поэтому базовый хук видит верный тик.
- **Точка хука позы.** В 2.4 `renderModel` применяет IK, затем физику, затем констрейнты, а `applyPhysicsOnce`
  вызывается раз за рендер — её начало ровно «после анимации и IK, до физики», как и давало событие позы в 2.7.
- **Сущности по индексу, цель по id.** Сущности фильма в 2.4 ищутся по индексу реплея; клип по-прежнему хранит цель
  как id реплея (поля сохранённого клипа те же), а выбор актёра, работающий в 2.4 по индексу, переводит одно в другое.
- **Инициализаторы полей и `registerUI()`.** В 2.4 `registerUI()` выполняется внутри конструктора `UIClip`, поэтому
  поле класса панели с инициализатором тут же обнулилось бы. Список обновителей виджетов объявлен без
  инициализатора и создаётся при первом использовании.

## Отличия от версии для 2.7
- **Цвет моделей.** 2.7 красит призраков-моделей плоской накладкой (`Form.overlayColor`). В 2.4 такой возможности
  нет, поэтому цвет применяется как тонировка (текстура × цвет). Все режимы цвета работают, но вид не идентичен.
- **Id целевых реплеев.** В 2.4 id реплея — это размер списка на момент создания, поэтому после удаления реплеев id
  могут повторяться. Тогда цель одного клипа может совпасть с двумя реплеями.

## Проверка
- Каждый класс и член BBS, которые использует аддон, сверены с исходниками 2.4 на обоих коммитах.
- Исходники аддона скомпилированы `javac` против исходников BBS 2.4, а классы Minecraft, Fabric, Mixin и JOML
  заменены небольшими заглушками. Результат: ошибок нет; та же проверка находит ожидаемую ошибку в намеренно
  сломанном файле.
- Это **не** полноценная сборка Gradle/Loom и **не** тест всех функций в игре.

## Авторство и лицензия
- BBS AM (AfterMotion): **KDR4**, MIT.
- BBS FS: [Wemppy4](https://github.com/Wemppy4/bbs-fs); мод BBS: McHorse.
- Порт сохраняет лицензию MIT оригинала, см. [`LICENSE`](LICENSE).

## Сообщить о проблеме
Приложи `logs/latest.log` и версии BBS FS, Fabric Loader и этого аддона.

