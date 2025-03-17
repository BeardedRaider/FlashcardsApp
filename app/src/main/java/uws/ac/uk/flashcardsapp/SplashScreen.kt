package uws.ac.uk.flashcardsapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * This activity is shown when the app is launched. It displays a full-screen splash
 * screen with the app's logo for a few seconds, then navigates to the main activity.
 */
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Hide status and navigation bars for full-screen experience
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Delay for 3 seconds, then navigate to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Create an intent to navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            // Start the activity
            startActivity(intent)
            // Close SplashScreen activity
            finish()
        }, 3000) // Adjust delay as needed
    }
}