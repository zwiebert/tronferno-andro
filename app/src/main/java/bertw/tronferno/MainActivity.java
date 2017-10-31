package bertw.tronferno;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class MainActivity extends AppCompatActivity {

    static String tcpHostname = "fernotron.fritz.box";   // FIXME: make configurable
    final static int tcpPort = 7777;

    TextView tvRec;
    private boolean use_wifi = true;
    private MessageHandler mMessageHandler = new MessageHandler(this);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        tvRec = (TextView) findViewById(R.id.textViewRec);

        if (use_wifi) {
            wifi_onCreate();
        }
    }

    ///////////// wifi //////////////////////
    private Socket mTcpSocket;
    private SocketAddress socketAddress;
    private Thread tcpWrite_Thread = null;
    private Thread tcpRead_Thread = null;
    private final BlockingQueue<String> q = new ArrayBlockingQueue<>(1000);


    private void start_tcpRead_Thread() {
        if (tcpRead_Thread == null) {
            tcpRead_Thread = new Thread() {
                byte[] buf = new byte[256];

                public void run() {
                    for (; mTcpSocket.isConnected(); ) {
                        try {
                            int len = mTcpSocket.getInputStream().read(buf);
                            if (len > 0) {
                                byte data[] = Arrays.copyOf(buf, len);
                                mMessageHandler.obtainMessage(MSG_DATA_RECEIVED, data).sendToTarget();
                            }
                        } catch (IOException e) {
                            // reconnect_tcpSocket();
                        }
                    }

                }

            };
        }

        tcpRead_Thread.start();
    }


    void tcpSocket_transmit(String s) {
        q.add(s);
        if (tcpWrite_Thread == null) {
            tcpWrite_Thread = new Thread() {
                //private Socket mTcpSocket;
                public void run() {
                    while (true) {
                        try {
                            final String data = q.take();

                            for (int i = 0, retry = 10; ; ++i) {
                                try {
                                    mTcpSocket.getOutputStream().write(data.getBytes());
                                    break;
                                } catch (IOException e) {
                                    if (i < retry) {
                                        reconnect_tcpSocket();
                                    } else {
                                        mMessageHandler.obtainMessage(0, e.toString().getBytes() + "\n").sendToTarget();
                                        break;
                                    }
                                }
                            }

                        } catch (InterruptedException e) {
                            // e.printStackTrace();
                        }
                    }
                }
            };
            tcpWrite_Thread.start();
        }
    }

    boolean reconnect_tcpSocket() {
        try {
            mTcpSocket.connect(socketAddress);
            return true;
        } catch (IOException e) {
        }
        return false;
    }

    boolean connect_tcpSocket() {
        try {
            mTcpSocket = new Socket();
            socketAddress = new InetSocketAddress(tcpHostname, tcpPort);
            mTcpSocket.connect(socketAddress);
            return mTcpSocket.isConnected();
        } catch (IOException e) {
            tvRec.setText("error: " + e.toString());
        } catch (NullPointerException e) {
            tvRec.setText("cannot connect to tcp server");
        }
        return false;
    }

    void start_tcp() {
        connect_tcpSocket();
        start_tcpRead_Thread();
    }

    void stop_tcp() {
        try {
            mTcpSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void wifi_onCreate() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (use_wifi) {
            start_tcp();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (use_wifi)
            stop_tcp();
    }

    final static int MSG_DATA_RECEIVED = 0;
    final static int MSG_CUAS_TIME_OUT = 3;
    final static int MSG_SEND_ENABLE = 4;

    private static class MessageHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MessageHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity ma = mActivity.get();
            switch (msg.what) {
                case MainActivity.MSG_DATA_RECEIVED:
                    try {
                        String s = new String((byte[]) msg.obj, "UTF-8");
                        ma.tvRec.append(s);
                        if (ma.progressDialog != null && ma.progressDialog.isShowing() && ma.cuasInProgress) {
                            if (s.contains("reply: cuas=ok\n")) {
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Success. Data has been received and stored.");
                                ma.cuasInProgress = false;
                            } else if (s.contains("reply: cuas=time-out\n")) {
                                ma.cuasInProgress = false;
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Time-Out. Please try again.");
                            }
                        }


                    } catch (Exception e) {
                        ma.tvRec.setText("error: " + e.toString());

                    }
                    break;

                case MainActivity.MSG_CUAS_TIME_OUT:
                    if (ma.progressDialog != null && ma.progressDialog.isShowing() && ma.cuasInProgress) {
                        ma.cuasInProgress = false;
                        ma.progressDialog.hide();
                        ma.showAlertDialog("Time-Out. Please try again.");
                    }
                    break;


                case MainActivity.MSG_SEND_ENABLE:
                    ma.enableSend(true, 0);
                    break;

            }
        }
    }

    private void transmit(String s) throws IOException {
        tvRec.append("transmit: " + s + "\n");
        if (use_wifi) tcpSocket_transmit(s);
    }


    public void onCheckedClick(View view) {

        try {
            boolean checked = ((CheckBox) view).isChecked();

            switch (view.getId()) {
                case R.id.checkBox_rtc_only:

                    findViewById(R.id.checkBox_daily_up).setEnabled(!checked);
                    findViewById(R.id.checkBox_daily_down).setEnabled(!checked);
                    findViewById(R.id.checkBox_weekly).setEnabled(!checked);
                    findViewById(R.id.checkBox_astro).setEnabled(!checked);
                    findViewById(R.id.editText_dailyUpTime).setEnabled(!checked && ((CheckBox) findViewById(R.id.checkBox_daily_up)).isChecked());
                    findViewById(R.id.editText_dailyDownTime).setEnabled(!checked && ((CheckBox) findViewById(R.id.checkBox_daily_down)).isChecked());
                    findViewById(R.id.editText_weeklyTimer).setEnabled(!checked && ((CheckBox) findViewById(R.id.checkBox_weekly)).isChecked());
                    findViewById(R.id.checkBox_random).setEnabled(!checked);
                    findViewById(R.id.checkBox_sun_auto).setEnabled(!checked);

                    break;

                case R.id.checkBox_daily_up:
                    findViewById(R.id.editText_dailyUpTime).setEnabled(checked);
                    break;

                case R.id.checkBox_daily_down:
                    findViewById(R.id.editText_dailyDownTime).setEnabled(checked);
                    break;

                case R.id.checkBox_weekly:
                    findViewById(R.id.editText_weeklyTimer).setEnabled(checked);
                    break;

                case R.id.checkBox_astro:
                    findViewById(R.id.editText_astroMinuteOffset).setEnabled(checked);
                    break;
            }


        } catch (Exception e) {
            tvRec.setText(e.toString());
        }
    }

    final String timeFormat = "%4d-%02d-%02dT%02d:%02d:%02d";

    private void fer_send_time() throws IOException {
        Calendar c = Calendar.getInstance();

        String sd = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        String st = new SimpleDateFormat("HH:mm:ss").format(c.getTime());


        String cmd = String.format("config rtc=%sT%s;", sd, st);
        tvRec.append(cmd + "\n");
        transmit(cmd);

    }

    int group = 3, memb = 1; // FIXME:
    int group_max = 4;  // FIXME : configure this from settings
    int[] memb_max = {0, 5, 5, 1, 1, 0, 0, 0};  // FIXME: configure this from settings
    static final String sendFmt = "send a=%d g=%d m=%d c=%s;";
    static final String timerFmt = "timer a=%d g=%d m=%d%s;";
    static final String configFmt = "config %s;";

    public void onClick(View view) {

        try {
            switch (view.getId()) {
                case R.id.button_stop:
                    transmit(String.format(sendFmt, 0, group, memb, "stop"));
                    break;
                case R.id.button_up:
                    transmit(String.format(sendFmt, 0, group, memb, "up"));
                    break;
                case R.id.button_down:
                    transmit(String.format(sendFmt, 0, group, memb, "down"));
                    break;
                case R.id.button_g:
                    group = (++group % (group_max + 1));
                    ((TextView) findViewById(R.id.textView_g)).setText(group == 0 ? "A" : String.valueOf(group));
                    if (memb > memb_max[group])
                        memb = 1;
                    ((TextView) findViewById(R.id.textView_e)).setText(group == 0 ? "" : (memb == 0 ? "A" : String.valueOf(memb)));
                    break;
                case R.id.button_e:
                    memb = (++memb % (memb_max[group] + 1));
                    ((TextView) findViewById(R.id.textView_e)).setText(group == 0 ? "" : (memb == 0 ? "A" : String.valueOf(memb)));

                    break;
                case R.id.button_sun_pos:
                    transmit(String.format(sendFmt, 0, group, memb, "sun-down"));
                    break;

                case R.id.button_timer:
                    String upTime = ((EditText) findViewById(R.id.editText_dailyUpTime)).getText().toString();
                    String downTime = ((EditText) findViewById(R.id.editText_dailyDownTime)).getText().toString();
                    String astroOffset = ((EditText) findViewById(R.id.editText_astroMinuteOffset)).getText().toString();

                    String timer = "";

                    if (((CheckBox) findViewById(R.id.checkBox_rtc_only)).isChecked()) {
                        timer += " rtc-only=1";
                    } else {
                        boolean dailyUpChecked = ((CheckBox) findViewById(R.id.checkBox_daily_up)).isChecked();
                        boolean dailyDownChecked = ((CheckBox) findViewById(R.id.checkBox_daily_down)).isChecked();
                        boolean weeklyChecked = ((CheckBox) findViewById(R.id.checkBox_weekly)).isChecked();

                        if (dailyUpChecked || dailyDownChecked) {
                            timer += " daily=";
                            timer += dailyUpChecked ? upTime.substring(0, 2) + upTime.substring(3, 5) : "-";
                            timer += dailyDownChecked ? downTime.substring(0, 2) + downTime.substring(3, 5) : "-";
                        }

                        if (((CheckBox) findViewById(R.id.checkBox_astro)).isChecked()) {
                            timer += " astro=";
                            timer += astroOffset;
                        }

                        if (weeklyChecked) {
                            String weeklyTimer = ((EditText) findViewById(R.id.editText_weeklyTimer)).getText().toString();

                            timer += " weekly=";
                            timer += weeklyTimer;
                        }

                    }


                    if (((CheckBox) findViewById(R.id.checkBox_sun_auto)).isChecked()) {
                        timer += " sun-auto=1";
                    }

                    if (((CheckBox) findViewById(R.id.checkBox_random)).isChecked()) {
                        timer += " random=1";
                    }


                    // timer = upTime.substring(0,2);


                    transmit(String.format(timerFmt, 0, group, memb, timer));
                    enableSend(false, 5);


                    break;


            }


        } catch (Exception e) {
            tvRec.setText(e.toString());
        }
    }

    AlertDialog alertDialog;

    void showAlertDialog(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                alertDialog.hide();
            }
        });

        alertDialog = builder.create();
        alertDialog.show();
    }

    void showProgressDialog(String msg, final int time_out) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(60);
        progressDialog.show();
        cuasInProgress = true;


        final Thread t = new Thread() {
            @Override
            public void run() {
                int jumpTime = 0;

                while (jumpTime < time_out && cuasInProgress) {
                    try {
                        sleep(1000);
                        jumpTime += 1;
                        progressDialog.setProgress(jumpTime);

                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                mMessageHandler.obtainMessage(MSG_CUAS_TIME_OUT, "timeout").sendToTarget();
            }
        };
        t.start();
    }

    void enableSend(boolean enable, final int timeout) {

        if (timeout > 0) {
            final Thread t = new Thread() {
                @Override
                public void run() {
                    int jumpTime = 0;

                    while (jumpTime < timeout) {
                        try {
                            sleep(1000);
                            jumpTime += 1;
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    mMessageHandler.obtainMessage(MSG_SEND_ENABLE, "timeout").sendToTarget();
                }
            };
            t.start();


        }

        findViewById(R.id.button_timer).setEnabled(enable);
        findViewById(R.id.button_up).setEnabled(enable);
        findViewById(R.id.button_stop).setEnabled(enable);
        findViewById(R.id.button_sun_pos).setEnabled(enable);
        findViewById(R.id.button_down).setEnabled(enable);
        mMenu.findItem(R.id.action_cuAutoSet).setEnabled(enable);
        mMenu.findItem(R.id.action_setFunc).setEnabled(enable);

    }

    ProgressDialog progressDialog;
    boolean cuasInProgress = false;

    public void onMenuClick(MenuItem mi) {

        try {
            switch (mi.getItemId()) {
                case R.id.action_cuAutoSet:
                    transmit(String.format(configFmt, "cu=auto"));
                    showProgressDialog("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...", 60);
                    break;

                case R.id.action_setFunc:
                    transmit(String.format(sendFmt, 0, group, memb, "set"));
                    showAlertDialog("You now have 60 seconds remaining to press STOP on the transmitter you want to add/remove. Beware: If you press STOP on the central unit, the device will be removed from it. To add it again, you would need the code. If you don't have the code, then you would have to press the physical set-button on the device");
                    break;
            }

        } catch (IOException e) {
            tvRec.setText(e.toString());
        }
    }


    Menu mMenu;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
