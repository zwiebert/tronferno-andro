package de.bertw.tronferno

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_wtimer.*

class WTimerActivity : AppCompatActivity() {
    val weekDays = WeekDays()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wtimer)

        val wtimerString = intent.getStringExtra("wtimer") ?: "empty"
        weekDays.setTimes(wtimerString)

        vetWtUpMon.setText(weekDays.wdays[0].up)
        vetWtUpTue.setText(weekDays.wdays[1].up)
        vetWtUpWed.setText(weekDays.wdays[2].up)
        vetWtUpThu.setText(weekDays.wdays[3].up)
        vetWtUpFri.setText(weekDays.wdays[4].up)
        vetWtUpSat.setText(weekDays.wdays[5].up)
        vetWtUpSun.setText(weekDays.wdays[6].up)

        vetWtDownMon.setText(weekDays.wdays[0].down)
        vetWtDownTue.setText(weekDays.wdays[1].down)
        vetWtDownWed.setText(weekDays.wdays[2].down)
        vetWtDownThu.setText(weekDays.wdays[3].down)
        vetWtDownFri.setText(weekDays.wdays[4].down)
        vetWtDownSat.setText(weekDays.wdays[5].down)
        vetWtDownSun.setText(weekDays.wdays[6].down)

        vcbWtTue.isChecked =weekDays.wdays[1].copy
        vcbWtWed.isChecked =weekDays.wdays[2].copy
        vcbWtThu.isChecked =weekDays.wdays[3].copy
        vcbWtFri.isChecked =weekDays.wdays[4].copy
        vcbWtSat.isChecked =weekDays.wdays[5].copy
        vcbWtSun.isChecked =weekDays.wdays[6].copy



    }

    fun viewsToWeekDays() {
        weekDays.wdays[0].up = vetWtUpMon.text.toString()
        weekDays.wdays[1].up = vetWtUpTue.text.toString()
        weekDays.wdays[2].up = vetWtUpWed.text.toString()
        weekDays.wdays[3].up = vetWtUpThu.text.toString()
        weekDays.wdays[4].up = vetWtUpFri.text.toString()
        weekDays.wdays[5].up = vetWtUpSat.text.toString()
        weekDays.wdays[6].up = vetWtUpSun.text.toString()

        weekDays.wdays[0].down = vetWtDownMon.text.toString()
        weekDays.wdays[1].down = vetWtDownTue.text.toString()
        weekDays.wdays[2].down = vetWtDownWed.text.toString()
        weekDays.wdays[3].down = vetWtDownThu.text.toString()
        weekDays.wdays[4].down = vetWtDownFri.text.toString()
        weekDays.wdays[5].down = vetWtDownSat.text.toString()
        weekDays.wdays[6].down = vetWtDownSun.text.toString()

        weekDays.wdays[1].copy = vcbWtTue.isChecked
        weekDays.wdays[2].copy = vcbWtWed.isChecked
        weekDays.wdays[3].copy = vcbWtThu.isChecked
        weekDays.wdays[4].copy = vcbWtFri.isChecked
        weekDays.wdays[5].copy = vcbWtSat.isChecked
        weekDays.wdays[6].copy = vcbWtSun.isChecked
    }

    fun onClick(view: View) {
        when(view.id) {
            vbtWtOk.id -> {
                viewsToWeekDays()
                val intent = Intent()
                intent.putExtra("message_return", weekDays.toString())
                setResult(Activity.RESULT_OK, intent)
                finish()
            }

            vbtWtCancel.id -> {
                finish()
            }
        }
    }

    class WeekDay(val name : String,
                  var up : String = "",
                  var down : String = "",
                  var copy : Boolean = false) {

        override fun toString(): String {
            if (copy) {
                return "+"
            }
            return (if (up.isBlank()) "-" else up)  + (if (down.isBlank()) "-" else down)
        }
    }
    class WeekDays {
        val wdays = arrayOf(WeekDay("Mon"), WeekDay("Tue"), WeekDay("Wed"), WeekDay("Thu"), WeekDay("Fri"), WeekDay("Sat"), WeekDay("Sun"))

        override fun toString() : String {
            var s = ""
            for (wd in wdays) {
                s += wd.toString()
            }
            return s
        }

        fun setTimes(wtimer : String)  {
            var t = 0
            var isUp = true

            for (i in 0 .. 6) {
                val wday = wdays[i]
                wday.up = ""
                wday.down = ""
                wday.copy = false
                var wdayDone = false
                val len = wtimer.length

                while(!wdayDone) {
                    if (t >= len) {
                        return
                    }
                    when (wtimer[t]) {
                        '-' -> { // timer off
                            ++t
                            if (!isUp) {
                                wdayDone = true
                            }
                            isUp = !isUp
                        }

                        '+' -> { // copy previous wday on+off timer
                            wday.copy = true
                            ++t
                            wdayDone = true
                        }

                        else -> {
                            if (t + 4 > len) {
                                return
                            }
                            val time = wtimer.substring(t, t + 4)
                            if (isUp) {
                                wday.up = time
                            } else {
                                wday.down = time
                            }
                            t += 4
                            if (!isUp) {
                                wdayDone = true
                            }
                            isUp = !isUp
                        }

                    }
                }
            }
        }
    }


}
