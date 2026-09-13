package com.dialy.app.data

import com.dialy.app.core.TestDispatcherProvider
import com.dialy.app.core.auth.AuthState
import com.dialy.app.core.auth.AuthUser
import com.dialy.app.core.sync.SyncResult
import com.dialy.app.core.sync.SyncState
import com.dialy.app.data.remote.auth.AuthRepositoryImpl
import com.dialy.app.data.remote.drive.DriveDataSource
import com.dialy.app.data.remote.drive.RemoteDriveFile
import com.dialy.app.data.repository.PlannerRepositoryImpl
import com.dialy.app.data.repository.SyncRepositoryImpl
import com.dialy.app.domain.model.DailyPlanner
import com.dialy.app.domain.model.Mood
import com.dialy.app.domain.model.MoodType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncRepositoryTest {

    private lateinit var fakePlannerDao: FakeDailyPlannerDao
    private lateinit var fakeTodoDao: FakeTodoDao
    private lateinit var fakePriorityDao: FakePriorityDao
    private lateinit var fakeScheduleDao: FakeScheduleDao
    private lateinit var fakeSelfCareDao: FakeSelfCareDao
    private lateinit var fakeReminderDao: FakeReminderDao
    private lateinit var fakeGratitudeDao: FakeGratitudeDao

    private lateinit var plannerRepository: PlannerRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var fakeDriveDataSource: FakeDriveDataSource
    private lateinit var syncRepository: SyncRepositoryImpl

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setUp() {
        fakePlannerDao = FakeDailyPlannerDao()
        fakeTodoDao = FakeTodoDao()
        fakePriorityDao = FakePriorityDao()
        fakeScheduleDao = FakeScheduleDao()
        fakeSelfCareDao = FakeSelfCareDao()
        fakeReminderDao = FakeReminderDao()
        fakeGratitudeDao = FakeGratitudeDao()

        val dispatchers = TestDispatcherProvider()

        plannerRepository = PlannerRepositoryImpl(
            plannerDao = fakePlannerDao,
            todoDao = fakeTodoDao,
            priorityDao = fakePriorityDao,
            scheduleDao = fakeScheduleDao,
            selfCareDao = fakeSelfCareDao,
            reminderDao = fakeReminderDao,
            gratitudeDao = fakeGratitudeDao,
            dispatchers = dispatchers
        )

        authRepository = AuthRepositoryImpl()
        fakeDriveDataSource = FakeDriveDataSource()

        syncRepository = SyncRepositoryImpl(
            plannerRepository = plannerRepository,
            plannerDao = fakePlannerDao,
            driveDataSource = fakeDriveDataSource,
            authRepository = authRepository,
            dispatchers = dispatchers
        )
    }

    @Test
    fun `syncPlanner returns NotAuthenticated when user is not logged in`() = runTest {
        val result = syncRepository.syncPlanner("2026-09-13")
        assertTrue(result is SyncResult.NotAuthenticated)
    }

    @Test
    fun `syncPlanner uploads local planner to Drive when no remote file exists`() = runTest {
        // Authenticate
        authRepository.signIn(AuthUser(id = "user1", email = "test@example.com", displayName = "Test User"))

        val date = "2026-09-13"
        val planner = DailyPlanner.createDefault(date).copy(
            focus = "Local only focus",
            updatedAt = 1000L
        )
        plannerRepository.savePlanner(planner)

        val result = syncRepository.syncPlanner(date)
        assertTrue(result is SyncResult.Success)

        val remoteContent = fakeDriveDataSource.downloadFile("planner_$date.json").getOrNull()
        assertNotNull(remoteContent)
        assertTrue(remoteContent!!.contains("Local only focus"))
    }

    @Test
    fun `syncPlanner downloads and updates local when remote is newer`() = runTest {
        authRepository.signIn(AuthUser(id = "user1", email = "test@example.com", displayName = "Test User"))

        val date = "2026-09-13"
        val localPlanner = DailyPlanner.createDefault(date).copy(
            focus = "Old local focus",
            updatedAt = 1000L
        )
        val savedLocal = plannerRepository.savePlanner(localPlanner)

        val newerRemotePlanner = DailyPlanner.createDefault(date).copy(
            focus = "Newer remote focus",
            mood = Mood(type = MoodType.VERY_HAPPY),
            updatedAt = savedLocal.updatedAt + 10_000L
        )
        fakeDriveDataSource.uploadFile("planner_$date.json", json.encodeToString(newerRemotePlanner))

        val result = syncRepository.syncPlanner(date)
        assertTrue(result is SyncResult.Success)

        val currentLocal = plannerRepository.getPlanner(date)
        assertEquals("Newer remote focus", currentLocal?.focus)
        assertEquals(MoodType.VERY_HAPPY, currentLocal?.mood?.type)
    }

    @Test
    fun `backupToCloud and restoreFromCloud restore complete diary state`() = runTest {
        authRepository.signIn(AuthUser(id = "user1", email = "test@example.com", displayName = "Test User"))

        val p1 = DailyPlanner.createDefault("2026-09-13").copy(focus = "Day 1 focus")
        val p2 = DailyPlanner.createDefault("2026-09-14").copy(focus = "Day 2 focus")
        plannerRepository.savePlanner(p1)
        plannerRepository.savePlanner(p2)

        val backupResult = syncRepository.backupToCloud()
        assertTrue(backupResult is SyncResult.Success)

        // Clear local
        fakePlannerDao.deletePlanner("2026-09-13")
        fakePlannerDao.deletePlanner("2026-09-14")

        val restoreResult = syncRepository.restoreFromCloud()
        assertTrue(restoreResult is SyncResult.Success)

        val restored1 = plannerRepository.getPlanner("2026-09-13")
        val restored2 = plannerRepository.getPlanner("2026-09-14")
        assertEquals("Day 1 focus", restored1?.focus)
        assertEquals("Day 2 focus", restored2?.focus)
    }

    @Test
    fun `purgeOldLocalData uploads un-synced old planners to Drive and deletes them locally`() = runTest {
        authRepository.signIn(AuthUser(id = "user1", email = "test@example.com", displayName = "Test User"))

        val today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))
        val oldDate = com.dialy.app.core.util.DateUtils.toIsoString(today.minusDays(10))
        val recentDate = com.dialy.app.core.util.DateUtils.toIsoString(today.minusDays(2))

        val oldPlanner = DailyPlanner.createDefault(oldDate).copy(focus = "Old journal 10 days ago")
        val recentPlanner = DailyPlanner.createDefault(recentDate).copy(focus = "Recent journal 2 days ago")

        plannerRepository.savePlanner(oldPlanner)
        plannerRepository.savePlanner(recentPlanner)

        val purgeResult = syncRepository.purgeOldLocalData(retentionDays = 7)
        assertTrue(purgeResult.isSuccess)
        assertEquals(1, purgeResult.getOrNull())

        // Old planner purged from local DB
        assertNull(fakePlannerDao.getPlannerByDateOnce(oldDate))

        // Recent planner retained locally in DB
        assertNotNull(fakePlannerDao.getPlannerByDateOnce(recentDate))

        // Old planner safely archived in Google Drive
        val driveFile = fakeDriveDataSource.downloadFile("planner_$oldDate.json").getOrNull()
        assertNotNull(driveFile)
        assertTrue(driveFile!!.contains("Old journal 10 days ago"))
    }
}

// =========================================================================
// Fake Drive Data Source
// =========================================================================

class FakeDriveDataSource : DriveDataSource {
    private val files = mutableMapOf<String, String>()

    override suspend fun uploadFile(fileName: String, content: String): Result<String> {
        files[fileName] = content
        return Result.success("file_id_$fileName")
    }

    override suspend fun downloadFile(fileName: String): Result<String?> {
        return Result.success(files[fileName])
    }

    override suspend fun listFiles(): Result<List<RemoteDriveFile>> {
        return Result.success(files.map {
            RemoteDriveFile(id = it.key, name = it.key, modifiedTime = System.currentTimeMillis(), content = it.value)
        })
    }

    override suspend fun deleteFile(fileName: String): Result<Unit> {
        files.remove(fileName)
        return Result.success(Unit)
    }
}
