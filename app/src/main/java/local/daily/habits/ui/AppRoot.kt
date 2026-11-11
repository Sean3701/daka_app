package local.daily.habits.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import local.daily.habits.data.*
import local.daily.habits.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class,ExperimentalFoundationApi::class)
@Composable fun AppRoot(repository: HabitRepository, settingsStore: SettingsStore) {
    val data by repository.snapshot.collectAsState(Snapshot())
    val settings by settingsStore.values.collectAsState(emptyMap())
    val scope=rememberCoroutineScope()
    val context=LocalContext.current
    var page by remember { mutableStateOf("今日") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var editor by remember { mutableStateOf<Habit?>(null) }
    var temporary by remember { mutableStateOf(false) }
    var temporaryName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTemp by remember { mutableStateOf<TemporaryTask?>(null) }
    var onlyDone by remember { mutableStateOf(false) }
    val dark=when(settings["theme"]){"深色"->true;"浅色"->false;else->isSystemInDarkTheme()}
    val colors=if(dark) darkColorScheme(primary=Color(0xFFC3B1FF),onPrimary=Color(0xFF292042),background=Color(0xFF30291D),surface=Color(0xFF3B3428),onSurface=Color(0xFFFFF3DF),secondaryContainer=Color(0xFF493663),tertiaryContainer=Color(0xFF736020))
        else lightColorScheme(primary=Color(0xFF6246D7),background=Color(0xFFFFF4D7),surface=Color(0xFFFFFDF7),onSurface=Color(0xFF29283B),secondaryContainer=Color(0xFFDCCCFF),tertiaryContainer=Color(0xFFFFE26B))
    fun act(block: suspend ()->Unit) { scope.launch { try { block() } catch(e:Exception){error=e.message?:"操作失败"} } }
    fun chooseDate() { DatePickerDialog(context,{_,y,m,d->date=LocalDate.of(y,m+1,d);page="今日"},date.year,date.monthValue-1,date.dayOfMonth).apply { datePicker.maxDate=System.currentTimeMillis() }.show() }
    MaterialTheme(colorScheme=colors) {
        Scaffold(containerColor=colors.background,topBar={
            TopAppBar(title={Text(if(page=="今日") "把今天，过得有点意思" else page,fontSize=21.sp,fontWeight=FontWeight.Bold)},colors=TopAppBarDefaults.topAppBarColors(containerColor=colors.background),actions={IconButton(onClick={page="设置"}){Icon(Icons.Outlined.Settings,"打开设置")}})
        },bottomBar={ NavigationBar(containerColor=colors.surface) {
            val labels=listOf("今日","习惯","月历","记录","设置")
            val icons=listOf(Icons.Outlined.Home,Icons.Outlined.CheckCircle,Icons.Outlined.DateRange,Icons.Outlined.List,Icons.Outlined.Settings)
            labels.forEachIndexed { i,label->if(label=="今日" || settings["show_$label"]!="false") NavigationBarItem(selected=page==label,onClick={page=label},icon={Icon(icons[i],label)},label={Text(label)}) }
        }}) { insets ->
            Column(Modifier.padding(insets).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                when(page) {
                    "今日" -> {
                        Row(verticalAlignment=Alignment.CenterVertically) { Text("DAILY CLUB",Modifier.background(colors.tertiaryContainer,RoundedCornerShape(6.dp)).padding(8.dp),fontWeight=FontWeight.Bold);Spacer(Modifier.weight(1f));TextButton(onClick={chooseDate()}){Text(date.toString()+" ▾")} }
                        var drag by remember { mutableFloatStateOf(0f) }
                        Row(Modifier.fillMaxWidth().pointerInput(date){detectHorizontalDragGestures(onDragStart={drag=0f},onDragEnd={if(drag>80)date=date.minusDays(1) else if(drag < -80 && date<LocalDate.now())date=date.plusDays(1)}){_,amount->drag+=amount}},horizontalArrangement=Arrangement.SpaceBetween) {
                            IconButton(onClick={date=date.minusDays(1)}){Icon(Icons.Outlined.ChevronLeft,"前一天")}
                            Text(if(date==LocalDate.now()) "今天" else date.dayOfWeek.toString(),Modifier.align(Alignment.CenterVertically))
                            IconButton(onClick={date=date.plusDays(1)},enabled=date<LocalDate.now()){Icon(Icons.Outlined.ChevronRight,"后一天")}
                        }
                        val flights=data.flights.filter { it.date==date.toString() }
                        SodaCard(colors.secondaryContainer) {
                            Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text("起飞",fontSize=26.sp,fontWeight=FontWeight.Bold);Text("随手记一次，留住今天",fontSize=12.sp)};Icon(Icons.Outlined.RocketLaunch,null,Modifier.size(42.dp)) }
                            Button(onClick={act{repository.addFlight(date,UUID.randomUUID().toString())}},modifier=Modifier.fillMaxWidth().testTag("flight-add")){Icon(Icons.Outlined.Add,null);Text(if(date<LocalDate.now())"补记起飞 · 加一次" else "起飞 · 记一次")}
                            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("${if(date==LocalDate.now())"今日" else "当日"}已记录 ${flights.size} 次",Modifier.weight(1f).testTag("flight-count"),fontSize=12.sp);TextButton(onClick={flights.maxByOrNull { it.recordedAt }?.let{act{repository.deleteFlight(it.id)}}},enabled=flights.isNotEmpty(),modifier=Modifier.testTag("flight-undo")){Text("撤销一次",fontSize=12.sp)} }
                            TextButton(onClick={page="起飞统计"}){Text("查看统计与明细 →")}
                        }
                        val stats=Statistics.flight(data.flights,LocalDate.now())
                        Metrics(listOf("本周次数" to "${stats.weekCount}","本月次数" to "${stats.monthCount}","连续记录" to "${stats.currentStreak} 天"))
                        Row(verticalAlignment=Alignment.CenterVertically) { Text("我的日常清单",Modifier.weight(1f),fontWeight=FontWeight.Bold,fontSize=19.sp);TextButton(onClick={temporary=true},enabled=date==LocalDate.now()){Text("＋ 临时任务")} }
                        val due=data.habits.filter { ScheduleEngine.isDue(it,date,data.completions) }
                        due.forEach { h-> val done=data.completions.find { it.taskId==h.id && it.date==date.toString() };TaskCard(h.symbol+"  "+h.name,if(h.frequency.endsWith("QUOTA"))ScheduleEngine.period(h,date,data.completions).let { "本周期 ${it.done} / ${it.target} 天" } else h.group,done!=null,done?.backfill==true){act{repository.setCompleted(h.id,date,done==null)}} }
                        data.temporary.filter { it.date==date.toString() }.forEach { t->val done=data.completions.find { it.taskId==t.id && it.date==t.date };Box(Modifier.combinedClickable(onClick={},onLongClick={deleteTemp=t})){TaskCard(t.name,"临时任务 · 长按删除",done!=null,done?.backfill==true){act{repository.setCompleted(t.id,date,done==null)}}} }
                        if(due.isEmpty() && data.temporary.none{it.date==date.toString()})Text("今天没有待办，去习惯库添加一个吧。")
                        if(date<LocalDate.now()) data.habits.filter { it.frequency.endsWith("QUOTA") && !ScheduleEngine.isDue(it,date,data.completions) && it.start<=date.toString() && (it.archivedOn==null || it.archivedOn>date.toString()) }.forEach { h->TextButton(onClick={act{repository.setCompleted(h.id,date,true)}}){Text("${h.name} · 周期已达标，仍要补记")} }
                    }
                    "习惯" -> {
                        Button(onClick={editor=Habit(UUID.randomUUID().toString(),"",LocalDate.now().toString())},modifier=Modifier.fillMaxWidth()){Text("＋ 创建习惯")}
                        data.habits.filter { it.archivedOn==null }.forEach { h->SodaCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(h.symbol+"  "+h.name,fontWeight=FontWeight.Bold);Text(h.group+" · "+frequencyLabel(ScheduleEngine.effective(h,LocalDate.now()).frequency),fontSize=12.sp); val stats=Statistics.habit(h,data.completions,LocalDate.now()); Text("累计 ${stats.completedDays} 天 · 连续 ${stats.currentStreak} ${stats.unit} · 最长 ${stats.longestStreak} ${stats.unit}",fontSize=12.sp)};TextButton(onClick={editor=h}){Text("编辑")}} } }
                        if(data.habits.none { it.archivedOn==null })Text("习惯从一件小事开始。")
                    }
                    "月历" -> {
                        MonthHeader(month,{month=month.minusMonths(1)},{month=month.plusMonths(1)})
                        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(onlyDone,{onlyDone=it});Text("仅已完成")}
                        MonthGrid(month,data,onlyDone){date=it;page="今日"}
                    }
                    "记录" -> {
                        val dates=(data.completions.map{it.date}+data.flights.map{it.date}).distinct().sortedDescending()
                        if(dates.isEmpty())Text("还没有记录，第一次打卡就从今天开始。")
                        dates.forEach { day->SodaCard { TextButton(onClick={date=LocalDate.parse(day);page="今日"}){Text(day+" →")};data.flights.filter{it.date==day}.forEach { e->RecordRow((if(e.backfill)"补·" else "")+"起飞",{act{repository.deleteFlight(e.id)}}) };data.completions.filter{it.date==day}.forEach { e->RecordRow((if(e.backfill)"补·" else "")+(data.habits.find{it.id==e.taskId}?.name?:data.temporary.find{it.id==e.taskId}?.name?:"历史任务"),{act{repository.setCompleted(e.taskId,LocalDate.parse(day),false)}}) } } }
                    }
                    "起飞统计" -> {
                        val stats=Statistics.flight(data.flights,LocalDate.now())
                        Metrics(listOf("累计次数" to "${stats.totalCount}","记录天数" to "${stats.activeDays}","最长连续" to "${stats.longestStreak} 天"))
                        Text("同一天可以记录多次；连续天数按日期计算。",fontSize=12.sp)
                        MonthHeader(month,{month=month.minusMonths(1)},{month=month.plusMonths(1)})
                        MonthGrid(month,data.copy(habits=emptyList(),completions=emptyList()),false){date=it;page="今日"}
                        TextButton(onClick={page="记录"}){Text("查看每一次记录 / 删除")}
                    }
                    "设置" -> SettingsScreen(settings,{key,value->act{settingsStore.set(key,value)}},repository,settingsStore,{error=it})
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        editor?.let { habit->HabitEditor(habit,{editor=null},{h->act{repository.saveHabit(h);editor=null}},{act{repository.archiveHabit(habit.id);editor=null}}) }
        if(temporary) AlertDialog(onDismissRequest={temporary=false},title={Text("今天的临时任务")},text={OutlinedTextField(temporaryName,{temporaryName=it},label={Text("任务名称")})},confirmButton={TextButton(onClick={act{repository.addTemporary(temporaryName);temporary=false;temporaryName=""}},enabled=temporaryName.isNotBlank()){Text("添加")}},dismissButton={TextButton(onClick={temporary=false}){Text("取消")}})
        deleteTemp?.let { t->AlertDialog(onDismissRequest={deleteTemp=null},title={Text("删除临时任务？")},text={Text("也会删除这项任务的打卡记录。")},confirmButton={TextButton(onClick={act{repository.deleteTemporary(t.id);deleteTemp=null}}){Text("删除")}},dismissButton={TextButton(onClick={deleteTemp=null}){Text("取消")}}) }
        error?.let { message->AlertDialog(onDismissRequest={error=null},title={Text("提示")},text={Text(message)},confirmButton={TextButton(onClick={error=null}){Text("知道了")}}) }
    }
}

@Composable fun SodaCard(color: Color=MaterialTheme.colorScheme.surface,content: @Composable ColumnScope.()->Unit) { Surface(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=color,border=BorderStroke(1.dp,MaterialTheme.colorScheme.onSurface.copy(alpha=.65f)),shadowElevation=3.dp){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)} }
@Composable fun Metrics(items: List<Pair<String,String>>) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){items.forEach{(name,value)->Surface(Modifier.weight(1f),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)){Column(Modifier.padding(10.dp)){Text(name,fontSize=11.sp);Text(value,fontSize=20.sp,fontWeight=FontWeight.Bold)}}}} }
@Composable fun TaskCard(name:String,subtitle:String,done:Boolean,backfill:Boolean,onClick:()->Unit) { SodaCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text((if(backfill)"补·" else "")+name,fontWeight=FontWeight.Bold);Text(subtitle,fontSize=12.sp)};Checkbox(done,{onClick()})} } }
@Composable fun RecordRow(name:String,onDelete:()->Unit){Row(verticalAlignment=Alignment.CenterVertically){Text(name,Modifier.weight(1f));TextButton(onClick=onDelete){Text("删除")}}}
@Composable fun MonthHeader(month:YearMonth,previous:()->Unit,next:()->Unit){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){IconButton(onClick=previous){Icon(Icons.Outlined.ChevronLeft,"上个月")};Text("${month.year} 年 ${month.monthValue} 月",fontWeight=FontWeight.Bold);IconButton(onClick=next){Icon(Icons.Outlined.ChevronRight,"下个月")}}}
@Composable fun MonthGrid(month:YearMonth,data:Snapshot,onlyDone:Boolean,onDate:(LocalDate)->Unit){
    val offset=month.atDay(1).dayOfWeek.value-1
    Row(Modifier.fillMaxWidth()){listOf("一","二","三","四","五","六","日").forEach{Text(it,Modifier.weight(1f),fontSize=12.sp)}}
    (0 until (offset+month.lengthOfMonth()+6)/7).forEach{week->Row(Modifier.fillMaxWidth()){(0..6).forEach{col->val day=week*7+col-offset+1;Box(Modifier.weight(1f).heightIn(min=64.dp)){if(day in 1..month.lengthOfMonth()){val date=month.atDay(day);val completions=data.completions.filter{it.date==date.toString()};val flights=data.flights.filter{it.date==date.toString()};val total=completions.size+flights.size;if(!onlyDone || total>0) TextButton(onClick={onDate(date)},enabled=date<=LocalDate.now(),contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth()){Column(horizontalAlignment=Alignment.CenterHorizontally){Text("$day");if(total>0)Text((if(completions.any{it.backfill}||flights.any{it.backfill})"补·" else "")+"$total",fontSize=10.sp)}}}}}}}
}
fun frequencyLabel(value:String)=mapOf("DAILY" to "每天","WEEK_DAYS" to "每周指定日","MONTH_DAYS" to "每月指定日","WEEK_QUOTA" to "每周至少 N 天","MONTH_QUOTA" to "每月至少 N 天")[value]?:value
