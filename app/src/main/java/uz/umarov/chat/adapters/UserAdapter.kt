package uz.umarov.chat.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import uz.umarov.chat.ChatActivity
import uz.umarov.chat.R
import uz.umarov.chat.models.User

class UserAdapter(private val context: Context, private var userList: ArrayList<User>) :
    RecyclerView.Adapter<UserAdapter.UserVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVh {
        val binding = LayoutInflater.from(context).inflate(R.layout.user_layout, parent, false)
        return UserVh(binding)
    }

    override fun onBindViewHolder(holder: UserVh, position: Int) {
        val currentUser = userList[position]
        holder.textName.text = currentUser.name
        holder.msgNumber.text = currentUser.newMessageCount.toString()

        holder.itemView.setOnClickListener {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("name", currentUser.name)
            intent.putExtra("uid", currentUser.uid)
            context.startActivity(intent)
        }

        if (currentUser.name == "Umarov (Owner)") {
            holder.deleteUser.visibility = View.GONE
            holder.ownerLogo.visibility = View.VISIBLE
        } else {
            holder.deleteUser.visibility = View.VISIBLE
            holder.ownerLogo.visibility = View.GONE
            holder.deleteUser.setOnClickListener {
                val auth = FirebaseAuth.getInstance()
                val dbRef = FirebaseDatabase.getInstance().reference

                val currentUserId = auth.currentUser?.uid
                val selectedUserId = currentUser.uid

                if (currentUserId != null && currentUserId != selectedUserId) {
                    showConfirmationDialog(selectedUserId!!, dbRef)
                }
            }
        }
    }

    override fun getItemCount(): Int = userList.size

    private fun showConfirmationDialog(selectedUserId: String, dbRef: DatabaseReference) {
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setMessage("Are you sure you want to delete this user?")
            .setCancelable(false)
            .setPositiveButton("Delete") { dialog, _ ->
                dbRef.child("user").child(selectedUserId).removeValue()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val alert = dialogBuilder.create()
        alert.show()
    }

    class UserVh(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName = itemView.findViewById<TextView>(R.id.user_name)!!
        val msgNumber = itemView.findViewById<TextView>(R.id.numbermsg)!!
        val deleteUser = itemView.findViewById<View>(R.id.deleteUser)!!
        val ownerLogo = itemView.findViewById<View>(R.id.ownerLogo)!!
    }

    fun filterList(filteredList: List<User>) {
        userList = ArrayList(filteredList)
        notifyDataSetChanged()
    }
}
