package uz.umarov.chat.models

data class User(
    var name: String? = null,
    var email: String? = null,
    var uid: String? = null,
    val newMessageCount: Int = 0

)