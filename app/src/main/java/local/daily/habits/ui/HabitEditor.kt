package local.daily.habits.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import local.daily.habits.domain.Habit
import java.time.LocalDate
import java.time.LocalTime

@Composable fun HabitEditor(original: Habit,onDismiss:()->Unit,onSave:(Habit)->Unit,onDelete:()->Unit) {
    var name by remember { mutableStateOf(original.name) }
    var symbol by remember { mutableStateOf(original.symbol) }
    var group by remember { mutableStateOf(original.group) }
    var frequency by remember { mutableStateOf(original.frequency) }
    var days by remember { mutableStateOf(original.days.joinToString(",")) }
    var target by remember { mutableStateOf(original.target.toString()) }
    var start by remember { mutableStateOf(original.start) }
    var reminder by remember { mutableStateOf(original.reminder) }
    var color by remember { mutableStateOf(original.color) }
    var error by remember { mutableStateOf("") }
    var delete by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest=onDismiss,title={Row{Text("编辑习惯",Modifier.weight(1f));if(original.name.isNotEmpty())TextButton(onClick={delete=true}){Text("删除")}}},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        OutlinedTextField(name,{name=it},label={Text("名称")},singleLine=true)
        OutlinedTextField(symbol,{symbol=it.take(8)},label={Text("图标或短文字")})
        OutlinedTextField(group,{group=it},label={Text("分组")})
        Text("颜色")
        Row{listOf(0xFF6246D7,0xFF218A6B,0xFFD36C38,0xFFC54C83).forEach{c->RadioButton(color==c,{color=c},colors=RadioButtonDefaults.colors(selectedColor=androidx.compose.ui.graphics.Color(c),unselectedColor=androidx.compose.ui.graphics.Color(c)))}}
        listOf("DAILY","WEEK_DAYS","MONTH_DAYS","WEEK_QUOTA","MONTH_QUOTA").forEach { f->Row{RadioButton(frequency==f,{frequency=f});TextButton(onClick={frequency=f}){Text(frequencyLabel(f))}} }
        if(frequency.endsWith("DAYS"))OutlinedTextField(days,{days=it},label={Text(if(frequency=="WEEK_DAYS")"星期 1—7，用逗号分隔" else "日期 1—31，用逗号分隔")})
        if(frequency.endsWith("QUOTA"))OutlinedTextField(target,{target=it},label={Text("至少完成 N 天")})
        OutlinedTextField(start,{start=it},label={Text("起始日期 YYYY-MM-DD")})
        OutlinedTextField(reminder,{reminder=it},label={Text("提醒 HH:mm，留空关闭")})
        if(original.name.isNotEmpty())Text("周期习惯的频率和目标修改，下个原周期生效。",style=MaterialTheme.typography.bodySmall)
        if(error.isNotEmpty())Text(error,color=MaterialTheme.colorScheme.error)
    }},confirmButton={TextButton(onClick={try{
        require(name.isNotBlank()){ "请输入名称" };LocalDate.parse(start)
        if(reminder.isNotBlank())LocalTime.parse(reminder)
        val selected=if(frequency.endsWith("DAYS")) days.split(',', '，').map{it.trim().toInt()}.distinct() else emptyList()
        require(!frequency.endsWith("DAYS") || selected.isNotEmpty())
        require(selected.all{it in 1..if(frequency=="WEEK_DAYS")7 else 31}){"日期超出范围"}
        val n=if(frequency.endsWith("QUOTA"))target.toInt() else 1
        require(n in 1..if(frequency=="WEEK_QUOTA")7 else 31){"目标天数超出范围"}
        onSave(original.copy(name=name.trim(),symbol=symbol,group=group.ifBlank{"默认"},frequency=frequency,days=selected,target=n,start=start,reminder=reminder,color=color))
    }catch(e:Exception){error=e.message?:"请检查输入格式"}}){Text("保存")}},dismissButton={TextButton(onClick=onDismiss){Text("取消")}})
    if(delete)AlertDialog(onDismissRequest={delete=false},title={Text("删除此习惯？")},text={Text("停止今后的任务和提醒，历史记录仍会保留。")},confirmButton={TextButton(onClick=onDelete){Text("删除")}},dismissButton={TextButton(onClick={delete=false}){Text("取消")}})
}
