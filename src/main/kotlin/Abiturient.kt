package admission

class Abiturient(val snils: String, val egeSum: Int) {
    val directions: MutableMap<Int, Int> = mutableMapOf()
    var passedDirection: Int = 0;
    fun addDirection(direction: Int, priority: Int) {
        directions.put(priority,direction)
    }
    fun passDirection(direction: Int) {passedDirection = direction}
}