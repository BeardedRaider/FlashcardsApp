package uws.ac.uk.flashcardsapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private val flashcards = mutableListOf<Flashcard>()
    private var currentCardIndex = 0
    private lateinit var questionText: TextView
    private lateinit var answerText: TextView
    private lateinit var frontCard: View
    private lateinit var backCard: View
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var cardCounterText: TextView
    private var isFlipped = false
    private val categories = listOf("Math", "Science", "History", "Literature", "General Knowledge")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialize views
        questionText = findViewById(R.id.question_text)
        answerText = findViewById(R.id.answer_text)
        frontCard = findViewById(R.id.front_card)
        backCard = findViewById(R.id.back_card)
        categoryFilterSpinner = findViewById(R.id.category_filter_spinner)
        cardCounterText = findViewById(R.id.card_counter)

        val flipButton = findViewById<Button>(R.id.flip_button)
        val nextCardButton = findViewById<Button>(R.id.next_card_button)

        sharedPreferences = getSharedPreferences("FlashcardsPrefs", Context.MODE_PRIVATE)
        loadFlashcards()
        setupCategoryFilter()
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
                clearFlashcards() // Call the correct function
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAddCardDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.category_spinner)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        val questionInput = dialogView.findViewById<EditText>(R.id.question_input)
        val answerInput = dialogView.findViewById<EditText>(R.id.answer_input)
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        saveButton.setOnClickListener {
            val question = questionInput.text.toString()
            val answer = answerInput.text.toString()
            val selectedCategory = categorySpinner.selectedItem.toString()

            if (question.isNotEmpty() && answer.isNotEmpty()) {
                val newFlashcard = Flashcard(question, answer, selectedCategory)
                flashcards.add(newFlashcard)
                saveFlashcards()
                updateUI()
                dialog.dismiss()
            }
        }

        dialog.show()
    }



    private fun clearFlashcards() {
        val options = arrayOf("Clear All Flashcards", "Clear Flashcards by Category", "Cancel")

        AlertDialog.Builder(this)
            .setTitle("Clear Flashcards")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmClearAllFlashcards()  // Clear all flashcards
                    1 -> showCategoryClearDialog()    // Clear by category
                    2 -> {} // Cancel - do nothing
                }
            }
            .show()
    }

    // Step 1: Confirm before clearing all flashcards
    private fun confirmClearAllFlashcards() {
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("This will permanently delete all flashcards. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                flashcards.clear()
                saveFlashcards()
                updateUI()
                Toast.makeText(this, "All flashcards deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Step 2: Show category selection for deletion
    private fun showCategoryClearDialog() {
        val availableCategories = flashcards.map { it.category }.distinct()

        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "No categories available to clear", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Category to Clear")
            .setItems(availableCategories.toTypedArray()) { _, index ->
                confirmClearCategory(availableCategories[index])
            }
            .show()
    }

    // Step 3: Confirm before deleting a category
    private fun confirmClearCategory(category: String) {
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("This will permanently delete all flashcards in '$category'. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                flashcards.removeAll { it.category == category }
                saveFlashcards()
                updateUI()
                Toast.makeText(this, "Deleted all flashcards in $category", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun flipCard() {
        if (isFlipped) {
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            frontCard.animate().rotationY(0f).setDuration(500).start()
        } else {
            frontCard.visibility = View.GONE
            backCard.visibility = View.VISIBLE
            frontCard.animate().rotationY(180f).setDuration(500).start()
        }
        isFlipped = !isFlipped
    }

    private fun showNextCard() {
        if (flashcards.isNotEmpty() && currentCardIndex < flashcards.size - 1) {
            frontCard.animate().rotationY(180f).setDuration(500).start()

            Handler(Looper.getMainLooper()).postDelayed({
                currentCardIndex++
                isFlipped = false
                updateUI()
                frontCard.animate().rotationY(0f).setDuration(500).start()
            }, 500)
        }
    }

    private fun setupCategoryFilter() {
        val filterCategories = listOf("Show All") + categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = adapter

        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = parent.getItemAtPosition(position).toString()
                filterFlashcardsByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun filterFlashcardsByCategory(category: String) {
        val filteredFlashcards = if (category == "Show All") {
            flashcards
        } else {
            flashcards.filter { it.category == category }
        }

        if (filteredFlashcards.isNotEmpty()) {
            currentCardIndex = 0
            updateUIWithFilteredCards(filteredFlashcards)
        } else {
            questionText.text = "No flashcards found for this category, please add some via the menu bar at the top of the screen."
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false
            cardCounterText.visibility = View.GONE
        }
    }

    private fun updateUIWithFilteredCards(filteredFlashcards: List<Flashcard>) {
        if (filteredFlashcards.isNotEmpty()) {
            val currentFlashcard = filteredFlashcards[currentCardIndex]
            questionText.text = "Q. ${currentFlashcard.question}"
            answerText.text = "A. ${currentFlashcard.answer}"
            cardCounterText.text = "Card ${currentCardIndex + 1}/${filteredFlashcards.size}"
            cardCounterText.visibility = View.VISIBLE
        } else {
            cardCounterText.visibility = View.GONE
        }
    }

    private fun updateUI() {
        if (flashcards.isNotEmpty()) {
            updateUIWithFilteredCards(flashcards)
        } else {
            questionText.text = "No flashcards available."
            answerText.text = ""
            cardCounterText.visibility = View.GONE
        }
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
