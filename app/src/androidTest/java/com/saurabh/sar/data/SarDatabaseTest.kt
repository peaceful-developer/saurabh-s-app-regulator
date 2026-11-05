package com.saurabh.sar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.modes.ModeBundle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Room round trip on the phone: ./gradlew connectedDebugAndroidTest (DECISIONS #24). */
@RunWith(AndroidJUnit4::class)
class SarDatabaseTest {
    private lateinit var db: SarDatabase
    private lateinit var repo: SarRepository

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), SarDatabase::class.java).build()
        repo = SarRepository(db)
    }

    @After fun tearDown() = db.close()

    @Test fun modeBundleRoundTrip() = runTest {
        val id = repo.saveMode(ModeBundle(
            Mode(name = "Office Work", type = ModeType.SCHEDULED, message = "Focus."),
            listOf(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 540, endMinute = 1080)),
            listOf(AppRule(packageName = "ig", type = RuleType.LIMIT, allowanceMin = 15, capMin = 10, breakMin = 5)),
        ))
        assertThat(repo.mode(id)!!.name).isEqualTo("Office Work")
        assertThat(repo.schedulesByMode()[id]!!.single().endMinute).isEqualTo(1080)
        assertThat(repo.rulesFor(id).single().capMin).isEqualTo(10)
    }

    @Test fun runAndUsageRoundTripAndRetention() = runTest {
        val modeId = repo.saveMode(ModeBundle(Mode(name = "Gym", type = ModeType.MANUAL, message = "Lift."), emptyList(), emptyList()))
        val run = repo.insertRun(Run(modeId = modeId, startedAt = 1_000, locked = false), "Gym")
        repo.saveUsage(listOf(AppUsage(run.id, "yt", usedMs = 60_000)))
        assertThat(repo.activeRunNow()!!.id).isEqualTo(run.id)
        repo.saveRun(run.copy(endedAt = 2_000, endReason = EndReason.STOPPED), "Gym")
        assertThat(repo.activeRunNow()).isNull()
        assertThat(repo.usageFor(run.id).single().usedMs).isEqualTo(60_000)
        repo.deleteHistoryBefore(3_000)
        assertThat(repo.usageFor(run.id)).isEmpty()
    }

    @Test fun deletingAModeKeepsHistory() = runTest {
        val modeId = repo.saveMode(ModeBundle(Mode(name = "X", type = ModeType.MANUAL, message = "m"), emptyList(),
            listOf(AppRule(packageName = "a", type = RuleType.BLOCK))))
        val run = repo.insertRun(Run(modeId = modeId, startedAt = 1, endedAt = 2, locked = false, endReason = EndReason.STOPPED), "X")
        repo.deleteMode(modeId)
        assertThat(repo.rulesFor(modeId)).isEmpty()
        assertThat(repo.lastRunStarts()[modeId]).isEqualTo(run.startedAt)
    }
}
