package com.example.todoapp.mascot

import com.example.todoapp.data.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class MascotExpression(val frameIndex: Int) {
    NORMAL(0),
    HAPPY(1),
    CELEBRATE(2),
    WORRIED(4),
}

internal enum class MascotTaskEventKind {
    OVERVIEW,
    ADDED,
    COMPLETED,
    ALL_COMPLETED,
    OVERDUE,
    REMINDER,
}

internal data class MascotTaskEvent(
    val key: String,
    val kind: MascotTaskEventKind,
    val message: String,
    val expression: MascotExpression,
    val taskId: Long? = null,
    val taskTitle: String? = null,
)

internal object MascotTaskEventPlanner {
    fun nextEvent(
        previous: List<TaskEntity>?,
        current: List<TaskEntity>,
        now: Long,
    ): MascotTaskEvent? {
        if (previous == null) return overview(current, now)

        val previousById = previous.associateBy(TaskEntity::id)
        val completed = current.firstOrNull { task ->
            task.isCompleted && previousById[task.id]?.isCompleted == false
        }
        if (completed != null) {
            val remaining = current.count { !it.isCompleted }
            return if (remaining == 0) {
                MascotTaskEvent(
                    key = "all-completed:${completed.id}:${completed.updatedAt}",
                    kind = MascotTaskEventKind.ALL_COMPLETED,
                    message = "全部完了！ おつかれさまでした！",
                    expression = MascotExpression.CELEBRATE,
                    taskId = completed.id,
                    taskTitle = completed.title,
                )
            } else {
                MascotTaskEvent(
                    key = "completed:${completed.id}:${completed.updatedAt}",
                    kind = MascotTaskEventKind.COMPLETED,
                    message = "「${completed.title}」を完了にしました。残り${remaining}件です！",
                    expression = MascotExpression.HAPPY,
                    taskId = completed.id,
                    taskTitle = completed.title,
                )
            }
        }

        val previousIds = previousById.keys
        val added = current.filter { it.id !in previousIds }.maxByOrNull(TaskEntity::createdAt)
        if (added != null) {
            return MascotTaskEvent(
                key = "added:${added.id}:${added.createdAt}",
                kind = MascotTaskEventKind.ADDED,
                message = "「${added.title}」を登録しました！",
                expression = MascotExpression.HAPPY,
                taskId = added.id,
                taskTitle = added.title,
            )
        }

        val newlyOverdue = current
            .filter { !it.isCompleted && it.deadlineEpochMillis != null }
            .filter { task ->
                val deadline = requireNotNull(task.deadlineEpochMillis)
                deadline <= now && (previousById[task.id]?.deadlineEpochMillis ?: Long.MAX_VALUE) > now
            }
            .minByOrNull { requireNotNull(it.deadlineEpochMillis) }
        return newlyOverdue?.let { overdueEvent(it) }
    }

    fun overview(tasks: List<TaskEntity>, now: Long): MascotTaskEvent {
        val active = tasks.filterNot(TaskEntity::isCompleted)
        if (active.isEmpty()) {
            return MascotTaskEvent(
                key = "overview:empty:${tasks.size}",
                kind = MascotTaskEventKind.OVERVIEW,
                message = if (tasks.isEmpty()) {
                    "まだタスクがありません。新しい予定を登録してみませんか？"
                } else {
                    "未完了のタスクはありません。ゆっくり休んでくださいね！"
                },
                expression = MascotExpression.HAPPY,
            )
        }

        val overdue = active
            .filter { (it.deadlineEpochMillis ?: Long.MAX_VALUE) <= now }
            .minByOrNull { requireNotNull(it.deadlineEpochMillis) }
        if (overdue != null) return overdueEvent(overdue)

        val next = active
            .filter { it.deadlineEpochMillis != null }
            .minByOrNull { requireNotNull(it.deadlineEpochMillis) }
        return if (next != null) {
            val deadline = requireNotNull(next.deadlineEpochMillis)
            MascotTaskEvent(
                key = "overview:next:${next.id}:$deadline",
                kind = MascotTaskEventKind.OVERVIEW,
                message = "次は「${next.title}」です。期限は${formatDeadline(deadline)}です。",
                expression = MascotExpression.NORMAL,
                taskId = next.id,
                taskTitle = next.title,
            )
        } else {
            MascotTaskEvent(
                key = "overview:active:${active.size}",
                kind = MascotTaskEventKind.OVERVIEW,
                message = "未完了のタスクが${active.size}件あります。",
                expression = MascotExpression.NORMAL,
                taskId = active.first().id,
                taskTitle = active.first().title,
            )
        }
    }

    fun reminder(task: TaskEntity): MascotTaskEvent = MascotTaskEvent(
        key = "reminder:${task.id}:${task.deadlineEpochMillis}:${task.reminderOffsetMinutes}",
        kind = MascotTaskEventKind.REMINDER,
        message = if (task.reminderOffsetMinutes == 0) {
            "「${task.title}」の期限です！"
        } else {
            "「${task.title}」の期限が近づいています！"
        },
        expression = MascotExpression.WORRIED,
        taskId = task.id,
        taskTitle = task.title,
    )

    private fun overdueEvent(task: TaskEntity): MascotTaskEvent = MascotTaskEvent(
        key = "overdue:${task.id}:${task.deadlineEpochMillis}",
        kind = MascotTaskEventKind.OVERDUE,
        message = "「${task.title}」の期限を過ぎています。確認しましょう！",
        expression = MascotExpression.WORRIED,
        taskId = task.id,
        taskTitle = task.title,
    )

    private fun formatDeadline(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("M月d日 H:mm", Locale.JAPAN))
}
