package com.example.todoapp.mascot

import com.example.todoapp.data.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotTaskEventPlannerTest {
    @Test
    fun initialOverviewChoosesEarliestActiveDeadline() {
        val event = MascotTaskEventPlanner.overview(
            tasks = listOf(
                task(1, "あと", deadline = 30_000),
                task(2, "さき", deadline = 20_000),
            ),
            now = 10_000,
        )

        assertEquals(2L, event.taskId)
        assertEquals(MascotTaskEventKind.OVERVIEW, event.kind)
        assertTrue(event.message.contains("さき"))
    }

    @Test
    fun completedTaskCreatesCelebrationWithRemainingCount() {
        val before = listOf(task(1, "完了する"), task(2, "残る"))
        val after = listOf(before[0].copy(isCompleted = true, updatedAt = 20), before[1])

        val event = requireNotNull(MascotTaskEventPlanner.nextEvent(before, after, now = 30))

        assertEquals(MascotTaskEventKind.COMPLETED, event.kind)
        assertEquals(MascotExpression.HAPPY, event.expression)
        assertTrue(event.message.contains("残り1件"))
    }

    @Test
    fun overdueTaskHasPriorityInOverview() {
        val event = MascotTaskEventPlanner.overview(
            tasks = listOf(task(1, "期限切れ", deadline = 5_000)),
            now = 10_000,
        )

        assertEquals(MascotTaskEventKind.OVERDUE, event.kind)
        assertEquals(MascotExpression.WORRIED, event.expression)
    }

    private fun task(
        id: Long,
        title: String,
        deadline: Long? = null,
    ) = TaskEntity(
        id = id,
        title = title,
        deadlineEpochMillis = deadline,
        createdAt = 10,
        updatedAt = 10,
    )
}
