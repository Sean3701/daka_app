package local.daily.habits.domain
import java.time.LocalDate
object ReminderPolicy {
    fun shouldNotify(habit:Habit,date:LocalDate,completions:List<Completion>):Boolean =
        habit.reminder.isNotBlank() && (habit.archivedOn==null || date.toString()<habit.archivedOn) &&
        completions.none{it.taskId==habit.id && it.date==date.toString()} && ScheduleEngine.isDue(habit,date,completions)
}
