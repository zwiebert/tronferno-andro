package bertw.tronferno

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class ExpertActivity : AppCompatActivity() {

    override fun onBackPressed() {
        //super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expert)
    }
}
