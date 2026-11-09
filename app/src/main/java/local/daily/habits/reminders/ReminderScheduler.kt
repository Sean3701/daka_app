package local.daily.habits.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import local.daily.habits.MainActivity
import local.daily.habits.data.*
import local.daily.habits.domain.*
import java.time.*

class ReminderScheduler(private val context:Context) {
    suspend fun reschedule() {
        val snapshot=HabitRepository(AppDatabase.get(context)).snapshot.first()
        val alarm=context.getSystemService(AlarmManager::class.java)
        snapshot.habits.forEach { h->
            val intent=Intent(context,ReminderReceiver::class.java).setAction("habit:${h.id}").putExtra("habitId",h.id)
            val pending=PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarm.cancel(pending)
            if(h.reminder.isNotBlank() && h.archivedOn==null){
                val time=runCatching{LocalTime.parse(h.reminder)}.getOrNull()?:return@forEach
                val now=ZonedDateTime.now()
                val next=(0L..366L).asSequence().map{now.toLocalDate().plusDays(it)}.firstOrNull{day->day.atTime(time).atZone(now.zone)>now && ReminderPolicy.shouldNotify(h,day,snapshot.completions)}?:return@forEach
                val millis=next.atTime(time).atZone(now.zone).toInstant().toEpochMilli()
                if(Build.VERSION.SDK_INT<31 || alarm.canScheduleExactAlarms()) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pending)
                else alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pending)
            }
        }
    }
    suspend fun notify(title:String,text:String,id:Int) {
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val settings=SettingsStore(context).values.first()
        val manager=context.getSystemService(NotificationManager::class.java)
        val channelId="habit_${settings["sound"]!="false"}_${settings["vibration"]=="true"}"
        manager.createNotificationChannel(NotificationChannel(channelId,"打卡提醒",NotificationManager.IMPORTANCE_DEFAULT).apply{
            enableVibration(settings["vibration"]=="true");if(settings["sound"]=="false")setSound(null,null)
        })
        val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        manager.notify(id,NotificationCompat.Builder(context,channelId).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build())
    }
}
class ReminderReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        val result=goAsync()
        CoroutineScope(Dispatchers.IO).launch { try{
            val scheduler=ReminderScheduler(context)
            val s=HabitRepository(AppDatabase.get(context)).snapshot.first()
            s.habits.find{it.id==intent.getStringExtra("habitId")}?.let{h->if(ReminderPolicy.shouldNotify(h,LocalDate.now(),s.completions))scheduler.notify(h.name,"今天还有一件小事等你完成",h.id.hashCode())}
            scheduler.reschedule()
        }finally{result.finish()} }
    }
}
