# AGENTS.md — Smart Aesthetic Diary Android App

## 1. Project Goal

Build an Android application that will eventually feel like a **beautiful interactive digital diary/planner**, based on the supplied reference image.

The long-term product experience is a daily planner containing:

- Date
- Today's Focus
- Top Priorities
- To-Do List
- Schedule
- Self Care
- Notes / Ideas
- Gratitude
- Mood Tracker
- End-of-Day Reflection
- Don't Forget
- Daily Reminder

The reference image is the **future frontend/design target**. The final frontend should reproduce that diary/stationery aesthetic very closely, but that is **not the main implementation task right now**.

---

# 2. CURRENT DEVELOPMENT PRIORITY: BACKEND / DATA / APP FOUNDATION

## This is the most important instruction in this document.

**Focus the current development work primarily on the backend, application architecture, data models, persistence, repository layer, synchronization foundation, authentication foundation, and business logic.**

The final production frontend will be designed and implemented later.

For this phase, do **not** spend significant development effort trying to perfectly reproduce the attached diary UI.

Instead, create a **very basic testing frontend** whose only purpose is to verify that the backend works correctly.

The testing frontend can be plain, ugly, simple Android/Compose UI with basic text fields, buttons, checkboxes, lists, and screens. Visual polish is explicitly **not a goal** for this phase.

### In other words

Think of the project as:

```text
CURRENT PHASE

Backend + architecture + data + persistence + auth + sync
                         ↓
              Very basic test frontend
                         ↓
              Verify everything works

LATER PHASE

                Fully designed diary UI
                         ↓
         Exact aesthetic reference implementation
```

The developer/agent must **not confuse the testing frontend with the final product frontend**.

---

# 3. Final Product Vision (for future reference)

The eventual main screen should closely resemble the supplied reference image.

The user should be able to interact directly with the diary page, for example:

- Tap the To-Do section and add a task directly there.
- Edit an existing task directly in the section.
- Check off tasks.
- Edit schedule entries directly.
- Write notes directly.
- Select a mood.
- Write gratitude and reflection entries.
- Edit today's focus and priorities.

The final UI should feel like **the actual paper diary has become an app**, rather than looking like a conventional productivity dashboard.

This design requirement is intentionally deferred until the backend foundation is stable.

---

# 4. Core Architecture Requirement

Use a clean separation between UI, application logic, data, persistence, and external services.

Recommended conceptual architecture:

```text
                TEST / FUTURE UI
                       ↓
                  ViewModel
                       ↓
              Use Cases / Services
                       ↓
              Repository Interfaces
                 ↙          ↘
        Local Data Store    Cloud/Drive
                 ↘          ↙
                 Data Models
```

The exact architecture may use MVVM, Clean Architecture, or a similarly sensible structure, but the following rule is mandatory:

> **UI components must not directly own persistence logic, database logic, or Google Drive API logic.**

The frontend must communicate through ViewModels/use cases/repositories.

---

# 5. Technology Direction

Preferred Android stack:

- Kotlin
- Jetpack Compose for the temporary testing frontend
- ViewModel
- Kotlin Coroutines
- Kotlin Serialization or another reliable serialization mechanism
- Room or another appropriate local persistence solution
- Repository pattern
- Google authentication
- Google Drive integration for user-owned cloud storage/synchronization

Use modern stable Android practices.

Do not add libraries without a clear reason.

Keep external dependencies minimal and well justified.

---

# 6. Data Model

The application revolves around a **daily planner entry**.

The model must be structured. Do not represent the entire diary as one giant string or unstructured JSON blob in application code.

Conceptually:

```kotlin
data class DailyPlanner(
    val date: LocalDate,
    val focus: String,
    val topPriorities: List<PriorityItem>,
    val todos: List<TodoItem>,
    val schedule: List<ScheduleItem>,
    val selfCare: List<SelfCareItem>,
    val notes: String,
    val dontForget: List<ReminderItem>,
    val gratitude: List<String>,
    val mood: Mood?,
    val reflection: Reflection
)
```

The exact implementation may differ, but each section must have a clearly defined data representation.

### Important

Each user-editable unit should have a stable identifier where appropriate.

Examples:

```text
DailyPlannerId / date
TodoItemId
PriorityItemId
ScheduleItemId
ReminderItemId
```

This is important for reliable updates, synchronization, deletion, ordering, and conflict handling later.

---

# 7. Required Backend Capabilities

The backend/application layer should support the following operations before the final UI is built.

## Daily planner

- Create planner for a date.
- Load planner for a date.
- Update planner content.
- Delete planner if required by the chosen data model.
- Detect whether a planner already exists.
- Navigate between dates at the data layer.

## Today's Focus

- Read focus.
- Update focus.
- Clear focus.

## Priorities

- Create/update/delete priorities.
- Preserve priority order.
- Support exactly three primary priorities for the initial product model unless there is a strong technical reason to make the model more flexible.

## Todo

- Add task.
- Edit task.
- Mark task complete/incomplete.
- Delete task.
- Reorder tasks.
- Preserve completion state.
- Preserve creation/update timestamps where useful.

## Schedule

- Store a time slot.
- Store/edit schedule text for that time slot.
- Clear a schedule entry.
- Support multiple entries if the model later requires them.

## Self Care

- Store checklist items.
- Update completion state.
- Add/remove customizable items in the future.

## Notes / Ideas

- Save multiline note content.
- Update note content without affecting other sections.

## Don't Forget

- Add reminder.
- Edit reminder.
- Complete/incomplete reminder.
- Delete reminder.
- Reorder reminders if needed.

## Gratitude

- Add/edit/remove gratitude entries.
- Preserve entry order.

## Mood

- Save one primary mood for a date.
- Replace/update mood.
- Clear mood.

## Reflection

Store separate fields for:

- What went well today?
- What can I improve?
- I'm proud of myself for...

Each field must be independently updateable.

---

# 8. Local Persistence

The application must be designed so that the diary is useful even without network access.

The local database/persistence layer should be the reliable source for normal app interaction.

The app should be able to:

- Open today's planner offline.
- Read previously saved data offline.
- Create/edit/delete tasks offline.
- Edit notes offline.
- Update schedule offline.
- Update mood offline.
- Update reflection offline.
- Queue changes for later synchronization where required.

Do not make normal diary editing depend on an immediate network request.

---

# 9. Repository Layer

Create repository interfaces that hide implementation details from the UI.

Example concept:

```kotlin
interface PlannerRepository {
    suspend fun getPlanner(date: LocalDate): DailyPlanner?
    suspend fun createPlanner(planner: DailyPlanner)
    suspend fun updatePlanner(planner: DailyPlanner)
    suspend fun deletePlanner(date: LocalDate)
}
```

More granular repositories may be used if appropriate, for example:

```text
PlannerRepository
TodoRepository
ScheduleRepository
SyncRepository
AuthRepository
```

The exact split is an implementation decision.

The key rule is:

> **External storage technology must remain replaceable behind interfaces.**

A future change from one persistence mechanism to another should not require rewriting the planner UI.

---

# 10. Google Authentication

The long-term application flow is:

```text
App launch
    ↓
Google Sign-In
    ↓
Authenticated user
    ↓
Drive setup / permission
    ↓
Planner
```

Google should be the primary login method.

For the current backend phase:

- Implement an authentication abstraction.
- Expose the current signed-in user through application state.
- Handle sign-in success.
- Handle sign-in cancellation/failure.
- Handle sign-out.
- Keep authentication code outside the UI components.

The testing frontend only needs basic controls to trigger/test these states.

Do not hard-code a user's Google account information.

---

# 11. Google Drive Integration

Google Drive is intended to provide user-owned cloud backup/synchronization for diary data.

The architecture should separate Drive operations from the rest of the application.

Conceptually:

```text
PlannerRepository
       ↓
Sync / Cloud Layer
       ↓
Google Drive Data Source
```

Do **not** call Google Drive APIs directly from:

- Composables
- Individual todo components
- Notes components
- Schedule components
- Other UI elements

### Drive responsibilities

The backend foundation should eventually support:

- Creating the app's dedicated storage area/files.
- Reading planner data.
- Writing planner data.
- Updating planner data.
- Handling missing cloud data.
- Handling authentication/permission failures.
- Synchronizing local and cloud changes.
- Avoiding accidental overwriting where possible.

Use the minimum required permissions for the intended functionality.

Do not request broad Drive permissions without a concrete requirement.

---

# 12. Synchronization Strategy

The exact sync algorithm can be implemented incrementally, but the data model should already support synchronization.

At minimum, consider storing:

```text
createdAt
updatedAt
localVersion / revision
syncState
lastSyncedAt
```

where appropriate.

Possible sync states:

```text
LOCAL_ONLY
SYNC_PENDING
SYNCED
SYNC_ERROR
```

A simple deterministic strategy is acceptable for V1. Do not build an extremely complex conflict-resolution system unless required.

However, the architecture must not assume that local data and cloud data will always be identical.

---

# 13. Error Handling

Backend code must handle expected failures explicitly.

Examples:

- No internet connection.
- Google sign-in failure.
- Expired authentication/session.
- Drive permission denial.
- Drive API failure.
- Malformed cloud data.
- Database failure.
- Duplicate planner creation.
- Missing planner for a requested date.
- Sync conflict.

Do not silently swallow exceptions.

Expose meaningful application-level error states to the ViewModel.

The temporary frontend can show these as simple text messages, dialogs, or debug panels.

---

# 14. Basic Testing Frontend — CURRENT PHASE ONLY

Create a **very basic testing frontend**.

This frontend exists only to prove that the backend works.

It does **not** need to look anything like the attached diary reference.

A simple Compose screen is enough.

Example temporary layout:

```text
---------------------------------
 Backend Test Planner

 Date: [ 2026-09-13 ]

 Focus:
 [__________________________]

 Todo:
 [Task text______________] [Add]
 □ Task 1              [Edit]
 □ Task 2              [Delete]

 Schedule:
 08:00 [_______________]
 09:00 [_______________]

 Notes:
 [__________________________]
 [__________________________]

 Mood: [Happy] [Neutral] [Sad]

 [SAVE]
 [LOAD]
 [SYNC]

 Status:
 Saved locally
 Synced
---------------------------------
```

The exact appearance is irrelevant.

### The testing frontend MUST be useful for testing:

- Create planner.
- Load planner.
- Edit focus.
- Add todo.
- Edit todo.
- Complete todo.
- Delete todo.
- Edit schedule.
- Edit notes.
- Update mood.
- Edit reflection.
- Save locally.
- Load locally.
- Sign in/out.
- Trigger cloud sync.
- Display sync errors/state.

The testing UI can use plain buttons and fields.

---

# 15. Do Not Spend Time Polishing the Testing Frontend

During the backend phase, do NOT spend significant time on:

- Decorative flowers.
- Fancy typography.
- Exact spacing from the reference.
- Animations.
- Complex responsive layouts.
- Custom illustrations.
- Paper textures.
- Beautiful cards.
- Final navigation design.
- Theme customization.

The test frontend is disposable.

It may be completely replaced later.

---

# 16. Future Production Frontend Requirement

Once the backend is stable, the production frontend will be designed separately and can be completely replaced without changing the backend contracts.

The eventual frontend should be based closely on the supplied reference image.

It should have independent components such as:

```text
DailyPlannerScreen
├── HeaderSection
├── DateSection
├── TodaysFocusSection
├── TopPrioritiesSection
├── TodoListSection
├── ScheduleSection
├── SelfCareSection
├── NotesIdeasSection
├── GratitudeSection
├── MoodTrackerSection
├── DontForgetSection
├── ReflectionSection
└── DailyReminderSection
```

Each section will later be designed independently so the final visual layout can be changed without changing the underlying storage model.

### Critical future rule

The final UI must allow direct editing inside the page.

For example:

```text
TO-DO LIST

□ Buy groceries
□ Complete assignment
□ Go to gym

+ Add task
```

The user should be able to tap directly inside the To-Do section and add/edit tasks without being forced into a separate Todo application screen.

The same principle applies to schedule, notes, priorities, gratitude, and reflection.

---

# 17. Backend API Contract Must Be UI-Agnostic

Do not design backend data around the current temporary UI.

Bad approach:

```text
Backend stores whatever happens to be visible on the test screen.
```

Good approach:

```text
Backend stores the actual planner domain model.

Temporary test UI  ───────┐
                          ├── ViewModel → Repository → Data
Future diary UI ──────────┘
```

This is extremely important because the final frontend will be redesigned later.

---

# 18. Suggested Project Structure

A structure similar to this is recommended:

```text
app/
  src/main/java/.../

    data/
      local/
        database/
        dao/
        entities/
      remote/
        drive/
        auth/
      repository/

    domain/
      model/
      repository/
      usecase/

    presentation/
      testplanner/
        TestPlannerScreen.kt
        TestPlannerViewModel.kt

    core/
      auth/
      sync/
      error/
      util/

    MainActivity.kt
```

The naming and exact directory structure may differ, but responsibilities should remain separated.

---

# 19. Testing Requirements

Backend work must include meaningful automated tests.

At minimum, test:

### Unit tests

- Planner creation.
- Planner loading.
- Planner updates.
- Todo add/edit/complete/delete.
- Priority operations.
- Schedule updates.
- Notes updates.
- Mood updates.
- Reflection updates.
- Repository behavior.
- Serialization/deserialization.
- Sync-state transitions.

### Error tests

- Missing planner.
- Database failure.
- Invalid cloud data.
- Drive failure.
- Authentication failure.
- Offline behavior.

### Integration-level tests where practical

- Local database → repository → ViewModel.
- Mock/fake cloud source → sync layer → repository.

Use fake/mock data sources rather than real Google accounts inside automated tests.

---

# 20. Development Sequence

Follow this order unless there is a strong reason to change it.

## Phase A — Project foundation

1. Android project setup.
2. Kotlin/Compose configuration.
3. Dependency setup.
4. Basic package structure.
5. Environment/configuration handling.

## Phase B — Domain models

1. DailyPlanner.
2. TodoItem.
3. PriorityItem.
4. ScheduleItem.
5. SelfCareItem.
6. ReminderItem.
7. Reflection.
8. Mood.
9. IDs/timestamps/sync metadata.

## Phase C — Local persistence

1. Database/entities.
2. DAOs.
3. Mappers.
4. Repository implementation.
5. Unit tests.

## Phase D — Application/business logic

1. ViewModel state.
2. Use cases.
3. Validation.
4. Error handling.
5. Date/planner management.

## Phase E — Basic testing frontend

Build only enough UI to exercise all backend operations.

## Phase F — Google authentication

Implement auth abstraction and integration.

## Phase G — Google Drive

1. Drive data source.
2. Storage format.
3. Sync state.
4. Upload/download/update.
5. Error handling.
6. Sync tests.

## Phase H — End-to-end testing

Verify:

```text
UI → ViewModel → Repository → Local DB
                           ↘ Sync → Google Drive
```

## Phase I — Production frontend

**Only after the backend foundation is working reliably.**

At this stage, replace the temporary frontend with the custom diary UI based on the supplied reference image.

---

# 21. Production Frontend Handoff Requirement

When the frontend redesign starts, the backend should already expose clean state and operations so the new UI can be built without changing database structure unnecessarily.

The future UI designer/developer should be able to take something like:

```kotlin
TodoListState(
    items = items,
    onAdd = ...,
    onToggle = ...,
    onEdit = ...,
    onDelete = ...,
    onReorder = ...
)
```

and create any visual implementation they want.

For example, the final UI could be:

```text
reference-style diary
```

or a completely different UI later, while the same backend continues to work.

---

# 22. What NOT to Do During the Current Backend Phase

Do not:

- Spend time reproducing the final image pixel-by-pixel yet.
- Build the production diary UI now.
- Use the reference JPEG as the application background.
- Hard-code planner data into Compose.
- Put database calls inside Composables.
- Put Drive API calls inside Composables.
- Put authentication code inside individual UI sections.
- Create one giant `DailyPlannerActivity` containing everything.
- Store the entire planner as one unstructured text blob.
- Tie backend models to visual layout coordinates.
- Assume network access is always available.
- Build unnecessary smart-AI features before the core data flow works.
- Over-engineer synchronization before basic local persistence is reliable.

---

# 23. Definition of Done — Backend Phase

The backend phase is successful when:

- A user can authenticate through the application auth layer.
- A planner can be created and loaded by date.
- All major planner sections have structured models.
- Planner data can be saved locally.
- Planner data can be loaded locally.
- Individual sections/items can be updated independently.
- Todo items can be added/edited/completed/deleted/reordered.
- Schedule entries can be edited independently.
- Notes can be edited independently.
- Mood can be updated independently.
- Reflection fields can be updated independently.
- Repository interfaces hide storage implementation details.
- Drive integration is isolated behind a cloud/sync layer.
- Offline edits do not require immediate cloud connectivity.
- Sync state and expected failures are handled.
- Automated tests cover the important domain and repository behavior.
- A very basic testing frontend can exercise the backend successfully.
- The final frontend can later be replaced without redesigning the backend.

---

# 24. Final Guiding Principle

**Build the brain first. Design the face later.**

The current task is to make the application data, persistence, authentication, synchronization, and business logic reliable and easy to use from any frontend.

The attached diary image is the **future UI specification**, not the current implementation priority.

The temporary frontend should be intentionally simple and disposable.

Once the backend is stable, the production frontend can be designed from the reference image and rebuilt section-by-section without changing the backend foundation.
