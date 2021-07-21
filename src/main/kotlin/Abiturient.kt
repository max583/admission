@file:Suppress("SpellCheckingInspection")

package admission

class Abiturient(val snils: String, val egeSum: Int ) {
    val directions: MutableMap<Int, AbiturientDirection> = mutableMapOf()
    var passedDirection: Int = 0
    fun addDirection(direction: Int, priority: Int, consent: Boolean) {
        directions[priority] = AbiturientDirection(direction,consent)
    }
    fun passDirection(direction: Int) {passedDirection = direction}
}