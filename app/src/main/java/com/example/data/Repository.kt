package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class Repository(
    private val memberDao: MemberDao,
    private val messageDao: MessageDao,
    private val storyDao: StoryDao
) {
    val allMembers: Flow<List<MemberEntity>> = memberDao.getAllMembers()
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()
    val allStories: Flow<List<StoryEntity>> = storyDao.getAllStories()

    suspend fun getMe(): MemberEntity? = memberDao.getMe()

    suspend fun joinMe(name: String, imageUrl: String) = withContext(Dispatchers.IO) {
        // Delete previous me if exists
        memberDao.deleteMe()
        val me = MemberEntity(
            id = "me_user_id",
            name = name,
            imageUrl = imageUrl,
            statusText = "फचे गैंग में नया विद्रोही! 🔥",
            isMe = true,
            online = true,
            joinedTimestamp = System.currentTimeMillis()
        )
        memberDao.insertMember(me)
    }

    suspend fun insertMessage(message: MessageEntity) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
    }

    suspend fun insertStory(story: StoryEntity) = withContext(Dispatchers.IO) {
        storyDao.insertStory(story)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        messageDao.deleteAllMessages()
        storyDao.deleteAllStories()
    }

    // Connects to the real internet to fetch active people
    suspend fun fetchOnlineInternetUsers(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClients.randomUserService.getRandomUsers(12)
            val results = response.results
            if (results.isNotEmpty()) {
                val memberEntities = results.mapIndexed { index, result ->
                    // Map fetched images to cool local Hindi gang names for hilarious authentic regional styling
                    val localName = LOCAL_HINDI_NAMES.getOrElse(index) { "गैंगस्टर ${result.name.first}" }
                    val statusText = LOCAL_STATUS_TEXTS.getOrElse(index) { "फचे गैंग सद्स्य ✌️" }
                    MemberEntity(
                        id = result.email,
                        name = localName,
                        imageUrl = result.picture.large,
                        statusText = statusText,
                        isMe = false,
                        online = true,
                        joinedTimestamp = System.currentTimeMillis() - (index * 60000) // Staggered joining
                    )
                }
                memberDao.insertMembers(memberEntities)

                // Generate some initial messages
                val count = memberEntities.size
                if (count > 0) {
                    val welcomeMsg = MessageEntity(
                        text = "अरे भाइयों राम-राम! मैं ${memberEntities[0].name} अभी अभी फचे गैंग में ऑनलाइन आ गया हूँ! फचे गैंग ज़िंदाबाद! 🚩✊",
                        senderId = memberEntities[0].id,
                        senderName = memberEntities[0].name,
                        senderImageUrl = memberEntities[0].imageUrl,
                        timestamp = System.currentTimeMillis() - 500000,
                        isMe = false
                    )
                    messageDao.insertMessage(welcomeMsg)
                }
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            Log.e("Repository", "Error fetching online internet users: ${e.message}", e)
            // Fallback: Populate mock users but with real online image links so it still looks beautiful offline
            val fallbacks = mutableListOf<MemberEntity>()
            for (i in 0 until 10) {
                val id = "fallback_user_$i"
                val name = LOCAL_HINDI_NAMES[i % LOCAL_HINDI_NAMES.size]
                val status = LOCAL_STATUS_TEXTS[i % LOCAL_STATUS_TEXTS.size]
                // Alternate between male and female portraits via Picsum of high-fidelity
                val sex = if (i % 2 == 0) "men" else "women"
                val picIndex = (i + 15) % 99
                val picUrl = "https://randomuser.me/api/portraits/$sex/$picIndex.jpg"
                fallbacks.add(
                    MemberEntity(
                        id = id,
                        name = name,
                        imageUrl = picUrl,
                        statusText = status,
                        isMe = false,
                        online = true,
                        joinedTimestamp = System.currentTimeMillis() - (i * 120000)
                    )
                )
            }
            memberDao.insertMembers(fallbacks)
            return@withContext false
        }
    }

    companion object {
        val LOCAL_HINDI_NAMES = listOf(
            "रामनिवास ठेकेदार 🚜",
            "बबली बदमाश 🔫",
            "पवन फौजी 🇮🇳",
            "दीपक सरपंच 👑",
            "प्रियंका पटवारी 📝",
            "कविता डाकू 🦅",
            "संजय बॉक्सर 🥊",
            "राजेश पहलवान 💪",
            "सोनू शिकारी 🏹",
            "सुमन चौधरी 🌾",
            "नवीन जासूस 🕵️",
            "सीमा गुजरी 🥛",
            "अनिल डॉक्टर 💉",
            "मनीष ठेका 🍾",
            "अमित पहलवान 🏋️"
        )

        val LOCAL_STATUS_TEXTS = listOf(
            "फचे गैंग का चेला, अकेला ही मेला! 😈",
            "फचे गैंग की जय हो, भाई लोग! 🤝",
            "काम ऐसा करो कि नाम हो जाए! 😎",
            "अरे लाडले, के हाल चाल हैं? 🍻",
            "जीमने जा रहा हूँ, कोई चलेगा? 🍲",
            "सरपंच साहब की बैठक में हूँ। 🏛️",
            "जय बाबा की, फचे गैंग ज़िंदाबाद! 🚩",
            "बदमाशी छोड़ दी, पर फचे गैंग नहीं! 😉",
            "सिर्फ फचे गैंग के लिए सेवा चौबीस घंटे! ☎️"
        )
    }
}
