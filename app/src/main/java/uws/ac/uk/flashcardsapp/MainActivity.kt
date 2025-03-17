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

        // Get references to the buttons and set up their click listeners
        val flipButton = findViewById<Button>(R.id.flip_button)
        flipButton.setOnClickListener { flipCard() }
        val nextCardButton = findViewById<Button>(R.id.next_card_button)
        nextCardButton.setOnClickListener { showNextCard() }

        // Load flashcards from shared preferences
        sharedPreferences = getSharedPreferences("FlashcardsPrefs", Context.MODE_PRIVATE)
        loadFlashcards()

        // Set up the category filter spinner
        setupCategoryFilter()

        // Update the UI to show the current flashcard
        updateUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu for the toolbar
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item clicks
        return when (item.itemId) {
            R.id.action_add_card -> {
                // Show the add card dialog when the "Add Card" menu item is clicked
                showAddCardDialog()
                true
            }
            R.id.action_clear_flashcards -> {
                // Call the clearFlashcards function when the "Clear Flashcards" menu item is clicked
                clearFlashcards()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    /**
     * Displays a dialog to add a new flashcard with a question, answer, and category.
     */
    private fun showAddCardDialog() {
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)

        // Find the category spinner in the dialog layout
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.category_spinner)

        // Create an adapter for the category spinner with the list of categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Find the question and answer input fields in the dialog layout
        val questionInput = dialogView.findViewById<EditText>(R.id.question_input)
        val answerInput = dialogView.findViewById<EditText>(R.id.answer_input)

        // Find the save button in the dialog layout
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)

        // Create an AlertDialog with the custom view
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set a click listener on the save button
        saveButton.setOnClickListener {
            // Get the entered question and answer text
            val question = questionInput.text.toString()
            val answer = answerInput.text.toString()
            // Get the selected category from the spinner
            val selectedCategory = categorySpinner.selectedItem.toString()

            // Check that both the question and answer are not empty
            if (question.isNotEmpty() && answer.isNotEmpty()) {
                // Create a new flashcard with the entered data
                val newFlashcard = Flashcard(question, answer, selectedCategory)
                // Add the new flashcard to the list
                flashcards.add(newFlashcard)
                // Save the flashcards to persistent storage
                saveFlashcards()
                // Update the UI to reflect the new flashcard
                updateUI()
                // Dismiss the dialog
                dialog.dismiss()
            }
        }

        // Show the dialog
        dialog.show()
    }


    /**
     * Displays a dialog to clear flashcards with options to clear all or by category.
     */
    private fun clearFlashcards() {
        // Options for clearing flashcards
        val options = arrayOf("Clear All Flashcards", "Clear Flashcards by Category", "Cancel")

        // Show an AlertDialog with the options
        AlertDialog.Builder(this)
            .setTitle("Clear Flashcards")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmClearAllFlashcards()  // Option to clear all flashcards
                    1 -> showCategoryClearDialog()    // Option to clear by category
                    2 -> {} // Cancel - do nothing
                }
            }
            .show()
    }

    /**
     * Shows a confirmation dialog before clearing all flashcards.
     */
    private fun confirmClearAllFlashcards() {
        // Show a confirmation dialog for clearing all flashcards
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("This will permanently delete all flashcards. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                // Clear all flashcards and update UI
                flashcards.clear()
                saveFlashcards()
                updateUI()
                Toast.makeText(this, "All flashcards deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Displays a dialog to select a category for clearing flashcards.
     */
    private fun showCategoryClearDialog() {
        // Get distinct categories from flashcards
        val availableCategories = flashcards.map { it.category }.distinct()

        // Check if there are any categories available
        if (availableCategories.isEmpty()) {
            Toast.makeText(this, "No categories available to clear", Toast.LENGTH_SHORT).show()
            return
        }

        // Show an AlertDialog to select a category
        AlertDialog.Builder(this)
            .setTitle("Select Category to Clear")
            .setItems(availableCategories.toTypedArray()) { _, index ->
                // Confirm before clearing the selected category
                confirmClearCategory(availableCategories[index])
            }
            .show()
    }

    /**
     * Shows a confirmation dialog before clearing flashcards of a specific category.
     */
    private fun confirmClearCategory(category: String) {
        // Show a confirmation dialog for clearing flashcards in the selected category
        AlertDialog.Builder(this)
            .setTitle("Warning")
            .setMessage("This will permanently delete all flashcards in '$category'. Are you sure?")
            .setPositiveButton("Clear") { _, _ ->
                // Remove flashcards of the selected category and update UI
                flashcards.removeAll { it.category == category }
                saveFlashcards()
                updateUI()
                Toast.makeText(this, "Deleted all flashcards in $category", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    /**
     * Flips the flashcard from front to back or back to front.
     */
    private fun flipCard() {
        if (isFlipped) {
            // Show front of card again
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            // Animate rotation of front card
            frontCard.animate().rotationY(0f).setDuration(500).start()
        } else {
            // Show back of card
            frontCard.visibility = View.GONE
            backCard.visibility = View.VISIBLE
            // Animate rotation of front card
            frontCard.animate().rotationY(180f).setDuration(500).start()
        }
        // Toggle isFlipped flag
        isFlipped = !isFlipped
    }

    /**
     * Shows the next flashcard in the list.
     */
    private fun showNextCard() {
        if (flashcards.isNotEmpty() && currentCardIndex < flashcards.size - 1) {
            // Animate rotation of front card
            frontCard.animate().rotationY(180f).setDuration(500).start()

            // After animation is complete, show the next card
            Handler(Looper.getMainLooper()).postDelayed({
                currentCardIndex++
                isFlipped = false
                updateUI()
                // Animate rotation of front card back to 0 degrees
                frontCard.animate().rotationY(0f).setDuration(500).start()
            }, 500)
        }
    }

    /**
     * Sets up the category filter spinner.
     */
    private fun setupCategoryFilter() {
        // Create a list of categories for the spinner, with an extra option to show all flashcards
        val filterCategories = listOf("Show All") + categories
        // Create an adapter for the spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Set the adapter on the spinner
        categoryFilterSpinner.adapter = adapter

        // Set the onItemSelectedListener for the spinner
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected category from the spinner
                val selectedCategory = parent.getItemAtPosition(position).toString()
                // Filter the flashcards by category
                filterFlashcardsByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /**
     * Filters the flashcards by category.
     */
    private fun filterFlashcardsByCategory(category: String) {
        // If the selected category is "Show All", show all flashcards
        // otherwise, only show flashcards with the selected category
        val filteredFlashcards = if (category == "Show All") {
            flashcards
        } else {
            flashcards.filter { it.category == category }
        }

        if (filteredFlashcards.isNotEmpty()) {
            // Show the first flashcard in the filtered list
            currentCardIndex = 0
            updateUIWithFilteredCards(filteredFlashcards)
        } else {
            // Show a message if no flashcards are found for the selected category
            questionText.text = "No flashcards found for this category, please add some via the menu bar at the top of the screen."
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false
            cardCounterText.visibility = View.GONE
        }
    }
    /**
     * Updates the UI to show the current filtered flashcard, or nothing if there are no filtered flashcards.
     */
    private fun updateUIWithFilteredCards(filteredFlashcards: List<Flashcard>) {
        if (filteredFlashcards.isNotEmpty()) {
            // Show the current flashcard
            val currentFlashcard = filteredFlashcards[currentCardIndex]
            questionText.text = "Q. ${currentFlashcard.question}"
            answerText.text = "A. ${currentFlashcard.answer}"
            // Update the card counter to show the number of cards
            cardCounterText.text = "Card ${currentCardIndex + 1}/${filteredFlashcards.size}"
            cardCounterText.visibility = View.VISIBLE
        } else {
            // Hide the card counter if there are no filtered flashcards
            cardCounterText.visibility = View.GONE
        }
    }

    /**
     * Updates the UI to show the current flashcard, or nothing if there are no flashcards.
     */
    private fun updateUI() {
        if (flashcards.isNotEmpty()) {
            // Show the current flashcard
            updateUIWithFilteredCards(flashcards)
        } else {
            questionText.text = "No flashcards available."
            answerText.text = ""
            // Hide the card counter if there are no flashcards
            cardCounterText.visibility = View.GONE
        }
    }

    /**
     * Saves the flashcards to SharedPreferences.
     */
    private fun saveFlashcards() {
        // Convert the flashcards list to a JSON string
        val json = Gson().toJson(flashcards)
        // Save the JSON string to SharedPreferences
        sharedPreferences.edit().putString("flashcards_list", json).apply()
    }

    /**
     * Loads the flashcards from SharedPreferences.
     */
    private fun loadFlashcards() {
        // Get the JSON string from SharedPreferences
        val json = sharedPreferences.getString("flashcards_list", null)
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type
        // Clear the flashcards list
        flashcards.clear()
        // Convert the JSON string back to a list of flashcards and add to the list
        flashcards.addAll(Gson().fromJson(json, type) ?: mutableListOf())
    }
}
