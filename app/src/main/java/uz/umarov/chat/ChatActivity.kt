    package uz.umarov.chat

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import uz.umarov.chat.adapters.MessageAdapter
import uz.umarov.chat.databinding.ActivityChatBinding
import uz.umarov.chat.models.Message
import uz.umarov.chat.models.User

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    private lateinit var userList: ArrayList<User>
    private var receiverRoom: String? = null
    private var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userList = ArrayList()

        binding.progressBar.visibility = View.VISIBLE
        binding.chatRv.visibility = View.INVISIBLE
        binding.messageBox.visibility = View.INVISIBLE
        binding.btnSend.visibility = View.INVISIBLE
        binding.cardView.visibility = View.INVISIBLE
        binding.linearLayout.visibility = View.INVISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            binding.progressBar.visibility = View.GONE
            binding.chatRv.visibility = View.VISIBLE
            binding.messageBox.visibility = View.VISIBLE
            binding.btnSend.visibility = View.VISIBLE
            binding.cardView.visibility = View.VISIBLE
            binding.linearLayout.visibility = View.VISIBLE
            checkChatEmptyState()
        }, 1000)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")

        binding.toolbar.title = name.toString()
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseDatabase.getInstance().reference

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)
        binding.chatRv.adapter = messageAdapter

        val messagesQuery = mDbRef.child("chats").child(senderRoom!!).child("messages")
        messagesQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messageList.clear()
                for (postSnapshot in snapshot.children) {
                    val message = postSnapshot.getValue(Message::class.java)
                    messageList.add(message!!)
                }
                messageAdapter.notifyDataSetChanged()
                checkChatEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
            }
        })

        binding.btnSend.setOnClickListener {
            val message = binding.messageBox.text.toString()
            val messageObject = Message(message, senderUid)
            if (message.isNotBlank()) {

                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                    }
                binding.messageBox.setText("")
            }else Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
        }

        if (binding.toolbar.title.toString() == "Umarov (Owner)"){
            binding.clearChat.visibility = View.GONE
        }

        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Clear Chat")
            .setMessage("Are you sure you want to clear the chat?")
            .setPositiveButton("Clear") { dialog, _ ->
                clearChat()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        binding.clearChat.setOnClickListener {
            dialogBuilder.show()
        }
    }

    private fun clearChat() {
        messageList.clear()
        messageAdapter.notifyDataSetChanged()
        mDbRef.child("chats").child(senderRoom!!).child("messages").removeValue()
        mDbRef.child("chats").child(receiverRoom!!).child("messages").removeValue()
        checkChatEmptyState()
    }

    private fun checkChatEmptyState() {
        if (messageList.isEmpty()) {
            binding.chatRv.visibility = View.GONE
            binding.emptyText.visibility = View.VISIBLE
        } else {
            binding.chatRv.visibility = View.VISIBLE
            binding.emptyText.visibility = View.GONE
        }
    }
}
