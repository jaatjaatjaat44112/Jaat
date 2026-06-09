package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = Repository(
        database.memberDao(),
        database.messageDao(),
        database.storyDao()
    )

    val me: StateFlow<MemberEntity?> = flow {
        emit(repository.getMe())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val members: StateFlow<List<MemberEntity>> = repository.allMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val messages: StateFlow<List<MessageEntity>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stories: StateFlow<List<StoryEntity>> = repository.allStories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFetchingUsers = MutableStateFlow(false)
    val isFetchingUsers: StateFlow<Boolean> = _isFetchingUsers.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage: StateFlow<Boolean> = _isSendingMessage.asStateFlow()

    private val _activeStoryView = MutableStateFlow<List<StoryEntity>?>(null)
    val activeStoryView: StateFlow<List<StoryEntity>?> = _activeStoryView.asStateFlow()

    private val _typingMember = MutableStateFlow<String?>(null)
    val typingMember: StateFlow<String?> = _typingMember.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if Me exists. If so, fetch members automatically to get online list
            val currentMe = repository.getMe()
            if (currentMe != null) {
                syncOnlineMembers()
            }
        }
    }

    fun syncOnlineMembers() {
        viewModelScope.launch {
            _isFetchingUsers.value = true
            try {
                repository.fetchOnlineInternetUsers()
                generateDynamicStories()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error fetching online users", e)
            } finally {
                _isFetchingUsers.value = false
            }
        }
    }

    private suspend fun generateDynamicStories() {
        val activeMembers = members.value.filter { !it.isMe }
        if (activeMembers.isNotEmpty()) {
            val storiesList = listOf(
                "आज तो मौसम कतई ज़हर हो रहा है! ⛈️ चौधरी साहब के आम के बाग में कौन-कौन चलेगा पार्टी करने?",
                "बदमाश बनने की उम्र में फचे गैंग संभाल रहे हैं! जय बाबा की! 🚩😎",
                "आज की ताज़ा खबर: विकास की भैंस ने 15 लीटर दूध दिया है, सबको दावत है! 🥛😋",
                "पटवारी साहब ने आज गाँव का नक्शा ही बदल दिया, जय हो प्रियंका जी की! 📝👑",
                "ठेकेदार साहब कह रहे हैं कि इस बार सड़क फचे गैंग की मरज़ी से ही बनेगी! 🚜💪",
                "लोहा गरम है, हथौड़ा मार दो! फचे गैंग का डंका पूरे इलाके में बजता है! 🥊🦁",
                "चाय पी लो यारों, पवन फौजी की तरफ से कड़क अदरक वाली चाय! ☕🔥",
                "बबली भाई ने आज नया बुलेट लिया है! गाँव की गलियों में पटाखा बज रहा है! 🏍️💨"
            )
            for (i in 0 until minOf(5, activeMembers.size)) {
                val member = activeMembers[i]
                val randomText = storiesList[i % storiesList.size]
                repository.insertStory(
                    StoryEntity(
                        id = UUID.randomUUID().toString(),
                        memberId = member.id,
                        memberName = member.name,
                        memberImageUrl = member.imageUrl,
                        text = randomText,
                        timestamp = System.currentTimeMillis() - (i * 3600000)
                    )
                )
            }
        }
    }

    fun joinGang(name: String, avatarGender: String) {
        viewModelScope.launch {
            _isFetchingUsers.value = true
            val randomNum = (10..99).random()
            val genderPath = if (avatarGender.lowercase() == "female") "women" else "men"
            val imageUrl = "https://randomuser.me/api/portraits/$genderPath/$randomNum.jpg"
            
            repository.joinMe(name, imageUrl)
            // Reload me flow
            val freshMe = repository.getMe()
            (me as MutableStateFlow).value = freshMe

            // Sync other gang members over internet
            repository.fetchOnlineInternetUsers()
            generateDynamicStories()
            _isFetchingUsers.value = false
        }
    }

    fun postMyStory(text: String) {
        viewModelScope.launch {
            val currentMe = me.value ?: return@launch
            val myStory = StoryEntity(
                id = UUID.randomUUID().toString(),
                memberId = currentMe.id,
                memberName = currentMe.name,
                memberImageUrl = currentMe.imageUrl,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            repository.insertStory(myStory)
        }
    }

    fun showStory(storyList: List<StoryEntity>) {
        _activeStoryView.value = storyList
    }

    fun dismissStory() {
        _activeStoryView.value = null
    }

    fun addManualMeMessage(text: String) {
        viewModelScope.launch {
            val currentMe = me.value ?: return@launch
            val msg = MessageEntity(
                text = text,
                senderId = currentMe.id,
                senderName = currentMe.name,
                senderImageUrl = currentMe.imageUrl,
                timestamp = System.currentTimeMillis(),
                isMe = true
            )
            repository.insertMessage(msg)

            // Trigger simulated dynamic responses from other members of ফচে गैंग
            triggerGangReplies(text)
        }
    }

    private fun triggerGangReplies(userMessage: String) {
        viewModelScope.launch {
            _isSendingMessage.value = true
            
            // Choose a random member to simulate as currently "Typing..."
            val activeMembers = members.value.filter { !it.isMe }
            if (activeMembers.isEmpty()) {
                _isSendingMessage.value = false
                return@launch
            }
            
            val chosenTypist = activeMembers.random()
            _typingMember.value = chosenTypist.name
            
            // Artificial typing delay to feel like a real messaging app working with internet
            delay(2500)
            
            val geminiKey = BuildConfig.GEMINI_API_KEY
            val isKeyPlaceholder = geminiKey == "MY_GEMINI_API_KEY" || geminiKey.trim().isEmpty()

            if (!isKeyPlaceholder) {
                try {
                    val replyResult = fetchGeminiGangReply(userMessage, activeMembers)
                    if (replyResult != null && replyResult.isNotEmpty()) {
                        for (reply in replyResult) {
                            val matchedMember = activeMembers.find { it.name.contains(reply.senderName) || reply.senderName.contains(it.name) }
                                ?: activeMembers.random()
                            
                            val simulatedMessage = MessageEntity(
                                text = reply.text,
                                senderId = matchedMember.id,
                                senderName = matchedMember.name,
                                senderImageUrl = matchedMember.imageUrl,
                                timestamp = System.currentTimeMillis(),
                                isMe = false
                            )
                            repository.insertMessage(simulatedMessage)
                            delay(1000) // Delay between staggered replies
                        }
                    } else {
                        triggerLocalRuleReply(userMessage, activeMembers)
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Gemini failed, falling back to local simulation", e)
                    triggerLocalRuleReply(userMessage, activeMembers)
                }
            } else {
                triggerLocalRuleReply(userMessage, activeMembers)
            }
            
            _typingMember.value = null
            _isSendingMessage.value = false
        }
    }

    // Direct REST API integration following gemini-api directives
    private suspend fun fetchGeminiGangReply(userText: String, candidateMembers: List<MemberEntity>): List<SimulatedReply>? = withContext(Dispatchers.IO) {
        val geminiKey = BuildConfig.GEMINI_API_KEY
        val namesJoin = candidateMembers.joinToString(", ") { it.name }
        
        val systemMessageText = """
            You are simulating a WhatsApp-style group chat in Hindi named "फचे गैंग" (Phache Gang), which is a funny, warm, close agricultural-urban village community of loyal local friends.
            The user just sent a message. You must pick 1 or 2 distinct members from this group list: [$namesJoin] and write their conversational replies.
            Make the Hindi messages sound extremely local, full of friendly dehat/regional accent and charm (e.g. using 'लाडले', 'अरे भाई', 'मौज कर दी', 'के खाके मानोगे', 'जय बाबा की').
            Your response must be JSON only. Do not wrap in markdown tags like ```json or anything. 
            JSON Schema:
            [
              {
                "senderName": "Name of picked member",
                "text": "Funny, friendly Hindi response text"
              }
            ]
        """.trimIndent()

        val promptText = "The user says: \"$userText\""

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = promptText)))
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.8f
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemMessageText)))
        )

        try {
            val response = NetworkClients.geminiService.generateContent(geminiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!jsonText.isNullOrEmpty()) {
                val moshi = Moshi.Builder().build()
                val type = Types.newParameterizedType(List::class.java, SimulatedReply::class.java)
                val jsonAdapter = moshi.adapter<List<SimulatedReply>>(type)
                
                // Sanitise markup just in case
                val cleanJson = jsonText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()
                return@withContext jsonAdapter.fromJson(cleanJson)
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "REST Gemini Call Error", e)
        }
        return@withContext null
    }

    private suspend fun triggerLocalRuleReply(userText: String, candidateMembers: List<MemberEntity>) {
        val pickedMember = candidateMembers.random()
        val textLower = userText.lowercase()
        
        val replyText = when {
            textLower.contains("hello") || textLower.contains("hi") || textLower.contains("राम") -> {
                listOf(
                    "राम-राम लाडले! फचे गैंग में तेरा दिल से स्वागत है! 🚩🙏",
                    "अरे भाई राम-राम! क्या हाल-चाल हैं तुम्हारे? काफी दिनों में दर्शन दिए!"
                ).random()
            }
            textLower.contains("का") || textLower.contains("कौन") || textLower.contains("kya") -> {
                listOf(
                    "अरे भाई, सवाल पूछना छोड़ो, शाम को ठेके पे आओ, सारा सच बता देंगे! 🍾😉",
                    "सवाल कतई तगड़ा है भाई, पंचायती बैठक में सरपंच साहब ही बताएंगे इसका जवाब।",
                    "लाडले, फचे गैंग में नियम एक ही है: सवाल मत करो, सीधा हुक्म मान लो! 💪👑"
                ).random()
            }
            textLower.contains("पार्टी") || textLower.contains("खाना") || textLower.contains("party") -> {
                listOf(
                    "पार्टी? अरे यार पनीर टिक्का और कढ़ी-कचौड़ी का सीन बैठाओ आज फिर! 🥘🤤",
                    "रामनिवास भाई के फार्म हाउस पे आज शाम भट्टी चालू है, सब लोग आ जाओ भाई!",
                    "अरे भाई पवन फौजी की तरफ से स्पेशल दावत है आज, कोई पीछे नहीं हटेगा! 🍗🍻"
                ).random()
            }
            textLower.contains("बदमाश") || textLower.contains("गैंग") || textLower.contains("gang") -> {
                listOf(
                    "बदमाशी तो हमने छोड़ दी है लाडले, पर फचे गैंग का खौफ आज भी ज़िंदा है! 🔫🦁",
                    "पूरे इलाके में फचे गैंग का दबदबा है! जो बीच में आएगा, वो सीधे सरपंच साहब से मिलेगा!",
                    "फचे गैंग एक सोच है, जो सबके दिलों में राज करती है! 👑🔥"
                ).random()
            }
            else -> {
                listOf(
                    "अरे लाडले, तेरी बात सुनके मन कतई गार्डन-गार्डन हो गया! मौज कर दी! 😂👌",
                    "बिल्कुल सही बात बोली भाई ने! फचे गैंग का नाम ऐसे ही बुलंद रहेगा!",
                    "अरे भाइयों ज़रा सुनो, इसकी बात में दम तो है! तुम सबका क्या ख्याल है? 🤔🚜",
                    "कमाल की बात है! भाई शाम का खाना विकास भाई की हवेली पे ही खाएंगे सब लोग! 🌾🏰",
                    "जय महाकाल! गैंग के सब चीते फुल एक्टिव रहो, कुछ बड़ा धमाका होने वाला है! 🚩💥"
                ).random()
            }
        }

        val msg = MessageEntity(
            text = replyText,
            senderId = pickedMember.id,
            senderName = pickedMember.name,
            senderImageUrl = pickedMember.imageUrl,
            timestamp = System.currentTimeMillis(),
            isMe = false
        )
        repository.insertMessage(msg)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
            // Add initial greeting after clear
            val activeMembers = members.value.filter { !it.isMe }
            if (activeMembers.isNotEmpty()) {
                val member = activeMembers.random()
                repository.insertMessage(
                    MessageEntity(
                        text = "अरे लाडलों! चैट इतिहास खाली कर दिया गया है। फचे गैंग का नया अध्याय शुरू! 📖🚩",
                        senderId = member.id,
                        senderName = member.name,
                        senderImageUrl = member.imageUrl,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class SimulatedReply(
    val senderName: String,
    val text: String
)

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
