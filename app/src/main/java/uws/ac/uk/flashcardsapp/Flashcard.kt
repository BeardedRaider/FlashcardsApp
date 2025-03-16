package uws.ac.uk.flashcardsapp

data class Flashcard(
    /**
     * Data class to represent a flashcard
     * @property question The question that appears on the front of the flashcard
     * @property answer The answer that appears on the back of the flashcard
     * @property category The category or topic of the flashcard
     */
    val question: String,
    val answer: String,
    val category: String
)