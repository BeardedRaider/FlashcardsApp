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

/**
 * This is the main activity of the app. It displays a list of flashcards and allows the user to
 * add new flashcards, flip the cards and show the next card.
 */
class MainActivity : AppCompatActivity() {

    // SharedPreferences to store the list of flashcards
    private lateinit var sharedPreferences: SharedPreferences

    // List of flashcards
    private val flashcards = mutableListOf<Flashcard>()

    // Index of the current flashcard
    private var currentCardIndex = 0

    // TextView for the question
    private lateinit var questionText: TextView

    // TextView for the answer
    private lateinit var answerText: TextView

    // View for the front of the card
    private lateinit var frontCard: View

    // View for the back of the card
    private lateinit var backCard: View

    // Spinner to filter the flashcards by category
    private lateinit var categoryFilterSpinner: Spinner

    // Whether the card is currently flipped
    private var isFlipped = false

    // List of categories
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

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("FlashcardsPrefs", Context.MODE_PRIVATE)

        // Load the list of flashcards from SharedPreferences
        loadFlashcards()

        // Set up the category filter spinner
        setupCategoryFilter()

        // Update the UI to show the first flashcard
        updateUI()

        // Set up the flip button and next card button
        findViewById<Button>(R.id.flip_button).setOnClickListener { flipCard() }
        findViewById<Button>(R.id.next_card_button).setOnClickListener { showNextCard() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Show the add card dialog
            R.id.action_add_card -> {
                showAddCardDialog()
                true
            }
            // Clear the list of flashcards
            R.id.action_clear_flashcards -> {
                clearFlashcards()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Shows a dialog to add a new flashcard. The dialog contains an EditText for the question,
     * an EditText for the answer, a Spinner for the category and a Button to save the flashcard.
     */
    private fun showAddCardDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_card, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.category_spinner)

        // Create an ArrayAdapter for the categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        // Get the EditText for the question and answer
        val questionInput = dialogView.findViewById<EditText>(R.id.question_input)
        val answerInput = dialogView.findViewById<EditText>(R.id.answer_input)

        // Get the Button to save the flashcard
        val saveButton = dialogView.findViewById<Button>(R.id.save_button)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Set the Button to save the flashcard
        saveButton.setOnClickListener {
            // Get the text from the EditText
            val question = questionInput.text.toString()
            val answer = answerInput.text.toString()
            val selectedCategory = categorySpinner.selectedItem.toString()

            // Check if the question and answer are not empty
            if (question.isNotEmpty() && answer.isNotEmpty()) {
                // Create a new flashcard
                val newFlashcard = Flashcard(question, answer, selectedCategory)
                // Add the flashcard to the list
                flashcards.add(newFlashcard)
                // Save the list of flashcards to SharedPreferences
                saveFlashcards()
                // Update the UI to show the first flashcard
                updateUI()
                // Dismiss the dialog
                dialog.dismiss()
            }
        }

        // Show the dialog
        dialog.show()
    }

    /**
     * Shows a dialog to clear flashcards. The dialog contains three options:
     *  - Clear All Flashcards: This will delete all flashcards.
     *  - Clear Flashcards by Category: This will show a list of categories and allow the user to select one to clear.
     *  - Cancel: This will do nothing and dismiss the dialog.
     */
    private fun clearFlashcards() {
        val options = arrayOf(
            "Clear All Flashcards", // Delete all flashcards
            "Clear Flashcards by Category", // Delete flashcards by category
            "Cancel" // Do nothing and dismiss the dialog
        )

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


    /**
     * Flips the current card to show either the front (question) or the back (answer).
     * The card is flipped by animating the rotationY property from 0 to 180 degrees.
     * The isFlipped flag is used to keep track of whether the card is currently showing the front or back.
     */
    private fun flipCard() {
        if (isFlipped) {
            // Flip back to the front
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            frontCard.animate().rotationY(0f).setDuration(500).start()
        } else {
            // Flip to the back
            frontCard.visibility = View.GONE
            backCard.visibility = View.VISIBLE
            frontCard.animate().rotationY(180f).setDuration(500).start()
        }
        isFlipped = !isFlipped
    }

    /**
     * Shows the next flashcard in the list, by animating the flip from the current card to the next one.
     * If there are no more flashcards to show, it disables the "Next Card" button.
     */
    private fun showNextCard() {
        if (flashcards.isNotEmpty()) {
            if (currentCardIndex < flashcards.size - 1) {
                // Animate the flip from the current card to the next one
                frontCard.animate().rotationY(180f).setDuration(500).start()

                // After the animation, move to the next card and update the UI
                Handler(Looper.getMainLooper()).postDelayed({
                    currentCardIndex++ // Move to the next card
                    isFlipped = false
                    updateUI()

                    // Animate back to normal
                    frontCard.animate().rotationY(0f).setDuration(500).start()
                }, 500)
            } else {
                // If there are no more flashcards to show, disable the "Next Card" button
                Log.d("FlashcardApp", "No more flashcards to show.")
                findViewById<Button>(R.id.next_card_button).isEnabled = false
            }
        }
    }


    /**
     * Sets up the category filter spinner to display all categories (starting with "Show All") and
     * sets a listener to filter the flashcards by the selected category.
     */
    private fun setupCategoryFilter() {
        // Create a list of categories for the filter, starting with "Show All"
        val filterCategories = listOf("Show All") + categories

        // Create an ArrayAdapter to display the categories in a spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterCategories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Set the adapter to the category filter spinner
        categoryFilterSpinner.adapter = adapter

        // Set a listener for item selection on the spinner
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Get the selected category and filter flashcards accordingly
                val selectedCategory = parent.getItemAtPosition(position).toString()
                filterFlashcardsByCategory(selectedCategory)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Update UI if no category is selected
                updateUI()
            }
        }
    }

    /**
     * Filters the list of flashcards by a given category.
     * If the category is "Show All", it returns the original list of flashcards.
     * Otherwise, it returns a list of flashcards that match the given category.
     *
     * @param category The category to filter the flashcards by
     */
    private fun filterFlashcardsByCategory(category: String) {
        val filteredFlashcards = if (category == "Show All") {
            flashcards
        } else {
            flashcards.filter { it.category == category }
        }

        val cardCounterText = findViewById<TextView>(R.id.card_counter)

        if (filteredFlashcards.isNotEmpty()) {
            currentCardIndex = 0
            updateUIWithFilteredCards(filteredFlashcards)
        } else {
            questionText.text = "No flashcards found for this category."
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false

            // ✅ Hide the counter when no flashcards exist
            cardCounterText.visibility = View.GONE
        }
    }


    /**
     * Updates the UI to reflect the current state of the filtered flashcards.
     * If there are filtered flashcards available, it displays the current flashcard's question and answer.
     * If no filtered flashcards are available, it shows a message indicating that no flashcards were found.
     * It also updates the visibility of the card views and the card counter text.
     */
    private fun updateUIWithFilteredCards(filteredFlashcards: List<Flashcard>) {
        val cardCounterText = findViewById<TextView>(R.id.card_counter)

        if (filteredFlashcards.isNotEmpty()) {
            val currentFlashcard = filteredFlashcards[currentCardIndex]
            questionText.text = "Q. ${currentFlashcard.question}"
            answerText.text = "A. ${currentFlashcard.answer}"
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false
            findViewById<Button>(R.id.next_card_button).isEnabled = filteredFlashcards.size > 1

            // ✅ Show counter
            cardCounterText.text = "Card ${currentCardIndex + 1}/${filteredFlashcards.size}"
            cardCounterText.visibility = View.VISIBLE
        } else {
            questionText.text = "No flashcards found."
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false

            // ✅ Hide counter when no cards exist
            cardCounterText.text = ""  // Optional: Clear text
            cardCounterText.visibility = View.GONE
        }
    }




    /**
     * Updates the UI to reflect the current state of the flashcards.
     * If there are flashcards available, it displays the current flashcard's question and answer.
     * If no flashcards are available, it shows a message indicating that no flashcards are available.
     * It also updates the visibility of the card views and the card counter text.
     */
    private fun updateUI() {
        val cardCounterText = findViewById<TextView>(R.id.card_counter)

        if (flashcards.isNotEmpty()) {
            val currentFlashcard = flashcards[currentCardIndex]
            questionText.text = "Q. ${currentFlashcard.question}"
            answerText.text = "A. ${currentFlashcard.answer}"
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false
            findViewById<Button>(R.id.next_card_button).isEnabled = flashcards.size > 1

            // ✅ Show counter
            cardCounterText.text = "Card ${currentCardIndex + 1}/${flashcards.size}"
            cardCounterText.visibility = View.VISIBLE
        } else {
            questionText.text = "No flashcards available."
            answerText.text = ""
            frontCard.visibility = View.VISIBLE
            backCard.visibility = View.GONE
            isFlipped = false

            // ✅ Hide counter when no cards
            cardCounterText.visibility = View.GONE
        }
    }



    /**
     * Saves the current list of flashcards to SharedPreferences.
     * This function serializes the list of flashcards to a JSON string and stores it in SharedPreferences.
     */
    private fun saveFlashcards() {
        // Serialize the list of flashcards to a JSON string
        val json = Gson().toJson(flashcards)
        // Store the JSON string in SharedPreferences
        sharedPreferences.edit().putString("flashcards_list", json).apply()
    }

    /**
     * Loads the list of flashcards from SharedPreferences.
     * This function deserializes the JSON string stored in SharedPreferences to a list of flashcards.
     */
    private fun loadFlashcards() {
        // Get the JSON string from SharedPreferences
        val json = sharedPreferences.getString("flashcards_list", null)
        // Deserialize the JSON string to a list of flashcards
        val type = object : TypeToken<MutableList<Flashcard>>() {}.type
        // Clear the list and add the deserialized flashcards
        flashcards.clear()
        flashcards.addAll(Gson().fromJson(json, type) ?: mutableListOf())
    }
}

