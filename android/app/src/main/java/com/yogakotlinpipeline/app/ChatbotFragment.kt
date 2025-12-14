package com.yogakotlinpipeline.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yogakotlinpipeline.app.utils.ChatbotService
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var backButton: ImageButton

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messageAdapter: ChatMessageAdapter
    private val chatbotService = ChatbotService.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_chatbot, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        backButton = view.findViewById(R.id.backButton)

        // Setup RecyclerView
        messageAdapter = ChatMessageAdapter(messages)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Add welcome message
        messages.add(
            ChatMessage(
                "🧘 Welcome to Yoga Assistant! Ask me anything about yoga poses, modifications, or exercise routines.",
                isUser = false
            )
        )
        messageAdapter.notifyItemInserted(messages.size - 1)

        // Setup click listeners
        sendButton.setOnClickListener { sendMessage() }
        backButton.setOnClickListener { requireActivity().onBackPressed() }

        messageInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_ENTER &&
                event.action == android.view.KeyEvent.ACTION_DOWN
            ) {
                sendMessage()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show()
            return
        }

        // Add user message
        messages.add(ChatMessage(messageText, isUser = true))
        messageAdapter.notifyItemInserted(messages.size - 1)
        messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
        messageInput.text.clear()

        // Show loading
        sendButton.isEnabled = false
        loadingIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = chatbotService.sendMessage(messageText)
                if (response != null) {
                    messages.add(ChatMessage(response, isUser = false))
                    messageAdapter.notifyItemInserted(messages.size - 1)
                    messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                } else {
                    messages.add(
                        ChatMessage(
                            "Sorry, I couldn't process your request. Please try again.",
                            isUser = false
                        )
                    )
                    messageAdapter.notifyItemInserted(messages.size - 1)
                }
            } catch (e: Exception) {
                messages.add(ChatMessage("Error: ${e.message}", isUser = false))
                messageAdapter.notifyItemInserted(messages.size - 1)
            } finally {
                sendButton.isEnabled = true
                loadingIndicator.visibility = View.GONE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        chatbotService.clearConversationHistory()
    }
}

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatMessageAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    override fun getItemCount() = messages.size

    override fun getItemViewType(position: Int) =
        if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) R.layout.item_message_user else R.layout.item_message_bot
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(message: ChatMessage) {
            itemView.findViewById<TextView>(R.id.messageText).text = message.text
        }
    }

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }
}
