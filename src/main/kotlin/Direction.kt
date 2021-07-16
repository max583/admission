package admission

class Direction (val code: String,val name: String,val url: String, val limit: Int) {
    var counter: Int = 0
    var passEge: Int = 300
    var students = arrayListOf<String>()
    fun isAvailable():Boolean { return limit > counter}
    fun addAbiturient(ege: Int, snils: String) {
        counter++
        passEge = ege
        students.add(snils)
    }
}