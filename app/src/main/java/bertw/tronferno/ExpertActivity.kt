package bertw.tronferno

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.NavUtils

class ExpertActivity : AppCompatActivity() {

    override fun onBackPressed() {
        NavUtils.navigateUpFromSameTask(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expert)
    }
}
