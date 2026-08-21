package com.example.todoapp

import android.app.Application
import com.example.todoapp.data.TaskRepository
import com.example.todoapp.data.TodoDatabase
import com.example.todoapp.mascot.MascotAnnouncementScheduler
import com.example.todoapp.mascot.MascotAppearancePreferences
import com.example.todoapp.notification.ReminderDeliveryStore
import com.example.todoapp.notification.ReminderNotifier
import com.example.todoapp.notification.ReminderScheduler
import com.example.todoapp.notification.createReminderNotificationChannel

class TodoApplication : Application() {
    private val database by lazy { TodoDatabase.getInstance(this) }

    private val reminderDeliveryStore by lazy { ReminderDeliveryStore(this) }

    val reminderNotifier: ReminderNotifier by lazy {
        ReminderNotifier(
            context = this,
            taskDao = database.taskDao(),
            deliveryStore = reminderDeliveryStore,
        )
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepository(
            taskDao = database.taskDao(),
            reminderScheduler = ReminderScheduler(this),
            reminderDeliveryStore = reminderDeliveryStore,
        )
    }

    override fun onCreate() {
        super.onCreate()
        createReminderNotificationChannel(this)
        MascotAnnouncementScheduler.update(
            context = this,
            frequency = MascotAppearancePreferences(this).read().announcementFrequency,
        )
    }
}
