package com.example.smd_assignment_i230796
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class   call_screen: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.call_screen)


        val cal_btn = findViewById<ImageView>(R.id.call_callic)

        cal_btn.setOnClickListener {
            finish()
            overridePendingTransition(0,0)
        }
    }

}