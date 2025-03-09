package uws.ac.uk.flashcardsapp

//testing to see if push works
import android.content.Context
import androidx.appcompat.widget.Toolbar
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.view.LayoutInflater
import android.widget.EditText

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val flashcards = mutableListOf<Flashcard>()
    private var currentCardIndex = 0
    private lateinit var questionText: TextView
    private lateinit var answerText: TextView
    private lateinit var frontCard: View
    private lateinit var backCard: View
    private var isFlipped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar)) // Set up the App Bar

        // Initialize views
        questionText = findViewById(R.id.question_text)
        answerText = findViewById(R.id.answer_text)
        frontCard = findViewById(R.id.front_card)
        backCard = findViewById(R.id.back_card)

        val flipButton = findViewById<Button>(R.id.flip_button)
        val nextCardButton = findViewById<Button>(R.id.next_card_button)

        sharedPreferences = getSharedPreferences("FlashcardsPrefs", Context.MODE_PRIVATE)
        loadFlashcards() // Load saved flashcards at startup
        updateUI()

        flipButton.setOnClickListener { flipCard() }
        nextCardButton.setOnClickListener { showNextCard() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_card -> {
                showAddCardDialog()
                true
            }
            R.id.action_clear_flashcards -> {
                clearFlashcards()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddCardDialog() {
        // Inflate the dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_card, null)
        val questionInput = dialogView.findViewById<EditText>(R.id.dialog_question_input)
        val answerInput = dialogView.findViewById<EditText>(R.id.dialog_answer_input)

        // Create the AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Add Flashcard")
            .setView(dialogView)  // Use the inflated layout for the dialog
            .setPositiveButton("Save") { _, _ ->
                val question = questionInput.text.toString()
                val answer = answerInput.text.toString()
                if (question.isNotEmpty() && answer.isNotEmpty()) {
                    // Save the new flashcard
                    flashcards.add(Flashcard(question, answer))
                    saveFlashcards()
                    updateUI()  // Update the UI with the new card
                }
            }
            .setNegativeButton("Cancel", null)  // Close dialog if cancel is pressed
            .show()
    }


    private fun flipCard() {
        if (isFlipped) {
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            frontCard.animate().rotationY(0f).setDuration(500).start()  // Flip back
        } else {
            frontCard.visibility = View.GONE
            backCard.visibility = View.VISIBLE
            frontCard.animate().rotationY(180f).setDuration(500).start()  // Flip to back
        }
        isFlipped = !isFlipped
    }

    private fun showNextCard() {
        if (flashcards.isNotEmpty() && currentCardIndex < flashcards.size - 1) {
            // Animate the card flip out
            frontCard.animate().rotationY(180f).setDuration(500).start()

            // Delay to allow animation to complete
            Handler(Looper.getMainLooper()).postDelayed({
                currentCardIndex++
                isFlipped = false // Reset flip state to show the question side
                updateUI()

                // Animate flip back to front
                frontCard.animate().rotationY(0f).setDuration(500).start()
            }, 500)
        } else {
            Log.d("FlashcardApp", "No more flashcards to show.")
            findViewById<Button>(R.id.next_card_button).isEnabled = false
        }
    }

    private fun updateUI() {
        if (flashcards.isNotEmpty() && currentCardIndex < flashcards.size) {
            val currentFlashcard = flashcards[currentCardIndex]
            questionText.text = currentFlashcard.question
            answerText.text = currentFlashcard.answer

            if (isFlipped) {
                frontCard.visibility = View.GONE
                backCard.visibility = View.VISIBLE
            } else {
                frontCard.visibility = View.VISIBLE
                backCard.visibility = View.GONE
            }

            findViewById<Button>(R.id.next_card_button).isEnabled = currentCardIndex < flashcards.size - 1
        } else {
            questionText.text = ""
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false
        }
    }

    private fun clearFlashcards() {
        // Clear SharedPreferences data
        sharedPreferences.edit().clear().apply()

        // Clear the in-memory list of flashcards
        flashcards.clear()

        // Reset the current card index to 0
        currentCardIndex = 0

        // Update the UI to reflect the cleared data
        updateUI()

        // Save the cleared data to SharedPreferences
        saveFlashcards()
    }

    private fun saveFlashcards() {
        val json = Gson().toJson(flashcards)
        sharedPreferences.edit().putString("flashcards_list", json).apply()
    }

    private fun loadFlashcards() {
        val json = sharedPreferences.getString("flashcards_list", null)
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type
        flashcards.clear()
        flashcards.addAll(Gson().fromJson(json, type) ?: mutableListOf())
    }
}

