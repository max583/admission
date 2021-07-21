package admission

import org.jsoup.Jsoup
import java.time.LocalDateTime

const val checkingSnils = "179-225-859 11"
// Контрольные цифры приема.
val KCP = mapOf("11.03.01Радиотехника (Интеллектуальные радиотехнические системы)" to 60,
                "11.03.01Радиотехника (Системы компьютерного зрения)" to 40,
                "11.03.02Инфокоммуникационные технологии и системы связи" to 70,
                "11.03.03Конструирование и технология электронных средств" to 50,
                "11.05.01Радиоэлектронные системы и комплексы" to 30,
                "11.03.04Электроника и наноэлектроника (Оптоэлектроника и фотоника)" to 69,
                "11.03.04Электроника и наноэлектроника (Электронные приборы и устройства)" to 46,
                "11.03.04Электроника и наноэлектроника (Экстремальная электроника)" to 46,
                "11.03.04Электроника и наноэлектроника (Терагерцовая электроника)" to 69,
                "28.03.01Нанотехнологии и микросистемная техника" to 70,
                "01.03.02Прикладная математика и информатика" to 58,
                "09.03.01Информатика и вычислительная техника (Искусственный интеллект)" to 93,
                "09.03.01Информатика и вычислительная техника (Компьютерное моделирование и проектирование)" to 47,
                "09.03.02Информационные системы и технологии" to 100,
                "09.03.04Программная инженерия" to 40,
                "10.05.01Компьютерная безопасность" to 60,
                "27.03.03Системный анализ и управление" to 14,
                "27.03.04Управление в технических системах (Компьютерные интеллектуальные технологии управления в технический системах)" to 40,
                "13.03.02Электроэнергетика и электротехника" to 135,
                "15.03.06Мехатроника и робототехника" to 0,
                "27.03.04Управление в технических системах (Автоматика и робототехнические системы)" to 85,
                "12.03.01Приборостроение" to 137,
                "12.03.04Биотехнические системы и технологии" to 58,
                "20.03.01Техносферная безопасность" to 28,
                "27.03.02Управление качеством" to 17,
                "27.03.05Инноватика" to 10,
                "42.03.01Реклама и связи с общественностью" to 20,
                "45.03.02Лингвистика" to 25)

fun main() {

    // загружаем направления
    println(LocalDateTime.now())
    println("Загружаем направления http://etu.ru")

    val doc = Jsoup.connect("http://etu.ru/ru/abiturientam/priyom-na-1-y-kurs/podavshie-zayavlenie/").timeout(60*1000).get()   // <1>

    val list = doc.select("div#content.col-sm-9").select("table")[0].select("tbody").select("tr").select("tr")

    val directionList: MutableMap<Int, Direction> = mutableMapOf()

    var i = 0
    list.select("tr").forEach {
        if (it.select("td")[2].select("a").attr("href") != "") {
            directionList[++i] = Direction(
                it.select("td")[0].text(),
                it.select("td")[1].text(),
                "http://etu.ru/"+ it.select("td")[2].select("a").attr("href"),
                KCP[it.select("td")[0].text()+ it.select("td")[1].text()] ?: 0
            )
        }
    }

    directionList.forEach {
        println(it.key.toString()+": "+it.value.code+" "+it.value.name+" "+it.value.url+" "+it.value.limit)
    }

    println("Получено "+directionList.size+" направлений с непустыми списками. Загружаем абитуриентов")

    // загружаем абитуриентов
    val abiturientList: MutableMap<String,Abiturient> = mutableMapOf()

    for (direction in directionList) {
        println("Загружаем абитуриентов направления "+direction.value.code+" "+direction.value.name)
        val docAbit = Jsoup.connect(direction.value.url).timeout(60*1000).get()
        val directionName = docAbit.select("title").text()
        println("Проверяем название направления: $directionName")
        val listAbit = docAbit.select("div#content.container").select("table")[0].select("tbody").select("tr")
        //print(listAbit)

        var abiturientSum = 0
        listAbit.select("tr").forEach {
            val snils = it.select("td")[1].text()
            //if (snils == "158-637-894 28") println("Нашел 158-637-894 28!!!!")
            val priority = it.select("td")[2].text().toInt()
            val egeSumText = it.select("td")[4].text()
            val egeSum = if (egeSumText == "-") 300 else egeSumText.toInt()
            val consent = it.select("td")[10].text() != "Нет"

            //println("SNILS= "+snils+" priority="+priority+" egeSum="+egeSum)
            if (abiturientList.containsKey(snils)) {
                abiturientList[snils]?.addDirection(direction.key,priority, consent)
            }
            else
            {
                val abiturientNew = Abiturient(snils,egeSum)
                abiturientNew.addDirection(direction.key,priority, consent)
                abiturientList[snils] = abiturientNew
            }
            ++abiturientSum
        }
        println("Обработано абитуриентов по направлению: $abiturientSum")
        //Thread.sleep(5_000)
    }

    println("Всего абитуриентов найдено: "+abiturientList.size)

    println("Сортируем абитуриентов")

    val abiturientSortedList = abiturientList.toList()
        .sortedBy { (_, value) -> -value.egeSum }
        .toMap().toMutableMap()

    println("Всего абитуриентов отсортировано: "+abiturientSortedList.size)

    val otsev = 0
    println("Выкидываем $otsev% абитуриентов")

    repeat (otsev*(abiturientSortedList.size-abiturientSortedList.keys.indexOf(checkingSnils))/100){
        abiturientSortedList.remove(abiturientSortedList.keys.elementAt(0))
    }

    println("Осталось абитуриентов: "+abiturientSortedList.size)

    println("Распределяем абитуриентов")

   // var j = 0
    val consentConsider = false // Учитывать согласие?
    for (abiturient in abiturientSortedList){
        val sortedDirections = abiturient.value.directions.toList().sortedBy { (key, _) -> key }.toMap()
        //if (abiturient.key == "158-637-894 28") println("Abit=" + abiturient.key+" egeSum="+abiturient.value.egeSum)
        for (direction in sortedDirections.filter { (_, value) -> value.consent||!consentConsider }) {
            //if (abiturient.key == "158-637-894 28") println("priority=" + direction.key + " direction="+direction.value.direction+" name="+directionList[direction.value.direction]?.code+" "+directionList[direction.value.direction]?.name)
            if (directionList[direction.value.direction]?.isAvailable() == true) {
                directionList[direction.value.direction]?.addAbiturient(abiturient.value.egeSum,abiturient.value.snils)
                abiturient.value.passDirection(direction.value.direction)
                break
            }
        }
        //if (abiturient.key == "158-637-894 28") println("passDirection=" + abiturient.value.passedDirection)
        //if (j++ > 1000) exitProcess(0)
    }

    println("Проходные баллы по направлениям")
    directionList.forEach {
        println(it.value.code+" "+it.value.name+" "+it.value.students.size+" "+it.value.passEge)
    }


    val checkingAitDirection = abiturientList[checkingSnils]?.passedDirection ?: 0
    if (checkingAitDirection == 0) {
        println("Текущее состояние для " + checkingSnils+" (EGE: "+abiturientList[checkingSnils]?.egeSum+"): не проходит")
    }
    else {
        println("Текущее состояние для "+checkingSnils+" (EGE: "+abiturientList[checkingSnils]?.egeSum+"): проходит на "+directionList[checkingAitDirection]?.code+" "+directionList[checkingAitDirection]?.name)
    }

    if (1==0) {
        println("Распределение")

        for (direction in directionList) {
            println(direction.value.code + " " + direction.value.name + " " + direction.value.limit)
            direction.value.students.forEach {
                println(it + " " + abiturientList[it]?.egeSum)
            }
        }
    }

    println(LocalDateTime.now())

    //println(abiturientList["158-637-894 28"])
    //abiturientList["158-637-894 28"]?.directions?.forEach {
    //    println(directionList[it.value.direction]?.code + " " + directionList[it.value.direction]?.name)
    //}
}