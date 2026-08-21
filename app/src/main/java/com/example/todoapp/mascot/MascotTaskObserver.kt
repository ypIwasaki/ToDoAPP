package com.example.todoapp.mascot

import com.example.todoapp.data.TaskEntity
import com.example.todoapp.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal class MascotTaskObserver(
    private val repository: TaskRepository,
    private val scope: CoroutineScope,
    private val onEvent: (MascotTaskEvent) -> Unit,
) {
    private var observationJob: Job? = null
    private var previous: List<TaskEntity>? = null

    fun start() {
        if (observationJob != null) return
        observationJob = scope.launch {
            repository.observeTasks().collectLatest { items ->
                val current = items.map { it.task }
                val event = MascotTaskEventPlanner.nextEvent(
                    previous = previous,
                    current = current,
                    now = System.currentTimeMillis(),
                )
                previous = current
                event?.let(onEvent)
            }
        }
    }

    fun release() {
        observationJob?.cancel()
        observationJob = null
        previous = null
    }
}
