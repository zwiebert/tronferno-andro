package bertw.tronferno;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {


    String tcpHostname;
    int tcpPort;
    static int msgid = 1;
    final static int MSG_DATA_RECEIVED = 0;
    final static int MSG_CUAS_TIME_OUT = 3;
    final static int MSG_SEND_ENABLE = 4;
    final static int MSG_LINE_RECEIVED = 5;
    final static String def_dailyUp = "07:30";
    final static String def_dailyDown = "19:30";
    final static String def_weekly =  "0700-++++0900-+";
    final static String def_astro = "0";

    CheckBox view_checkBox_daily_up, view_checkBox_daily_down, view_checkBox_weekly, view_checkBox_astro, view_checkBox_random,
    view_checkBox_sun_auto, view_checkBox_rtc_only;
    TextView view_textView_log, view_textView_g, view_textView_e;
    EditText view_editText_dailyUpTime, view_editText_dailyDownTime, view_editText_weeklyTimer, view_editText_astroMinuteOffset;

    private boolean use_wifi = true;
    private MessageHandler mMessageHandler = new MessageHandler(this);

      private void LoadPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        tcpHostname = sharedPreferences.getString("tcpHostName", "fernotron.fritz.box");
        if (tcpHostname.contains(":")) {
            int pos = tcpHostname.indexOf(':');
            tcpPort = Integer.parseInt(tcpHostname.substring(pos+1));
            tcpHostname = tcpHostname.substring(0, pos);
        } else {
            tcpPort = 7777;
        }

        String sgam = sharedPreferences.getString("groupsAndMembers", "77777777");
        int sgam_length_1 = Math.min(7, sgam.length() - 1);
        group_max = Math.min(7, Integer.parseInt(sgam.substring(0,1)));

        for (int i=1; i <= sgam_length_1; ++i) {
            memb_max[i] = Math.min(7, Integer.parseInt(sgam.substring(i, i+1)));
        }
        for (int i=sgam_length_1+1; i <= 7; ++i) {
            memb_max[i] = 0;
        }

    }


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

        LoadPreferences();

        view_checkBox_daily_up = (CheckBox) findViewById(R.id.checkBox_daily_up);
        view_checkBox_daily_down = (CheckBox) findViewById(R.id.checkBox_daily_down);
        view_checkBox_weekly = (CheckBox) findViewById(R.id.checkBox_weekly);
        view_checkBox_astro = (CheckBox) findViewById(R.id.checkBox_astro);
        view_checkBox_random = (CheckBox) findViewById(R.id.checkBox_random);
        view_checkBox_sun_auto = (CheckBox) findViewById(R.id.checkBox_sun_auto);
        view_checkBox_rtc_only = (CheckBox) findViewById(R.id.checkBox_rtc_only);

        view_editText_dailyUpTime = (EditText) findViewById(R.id.editText_dailyUpTime);
        view_editText_dailyDownTime = (EditText) findViewById(R.id.editText_dailyDownTime);
        view_editText_weeklyTimer = (EditText) findViewById(R.id.editText_weeklyTimer);
        view_editText_astroMinuteOffset = (EditText) findViewById(R.id.editText_astroMinuteOffset);

        view_textView_log = (TextView) findViewById(R.id.textView_log);
        view_textView_g = (TextView) findViewById(R.id.textView_g);
        view_textView_e = (TextView) findViewById(R.id.textView_e);

        view_checkBox_daily_up.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_daily_down.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_weekly.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_astro.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_random.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_sun_auto.setOnCheckedChangeListener(onCheckedChanged);
        view_checkBox_rtc_only.setOnCheckedChangeListener(onCheckedChanged);

        view_editText_dailyUpTime.setText("");
        view_editText_dailyDownTime.setText("");
        view_editText_weeklyTimer.setText("");
        view_editText_astroMinuteOffset.setText("");

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
                    try {
                        BufferedReader br = new BufferedReader(new InputStreamReader(mTcpSocket.getInputStream()));

                        if (false) {
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
                            } else {
                                   while (mTcpSocket.isConnected()) {
                                       try {
                                           String line = br.readLine();

                                           mMessageHandler.obtainMessage(MSG_LINE_RECEIVED, line).sendToTarget();

                                       } catch (IOException e) {
                                           // reconnect_tcpSocket();
                                       }
                                   }

                            }

                        } catch(IOException e){

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
            view_textView_log.setText("error: " + e.toString());
        } catch (NullPointerException e) {
            view_textView_log.setText("cannot connect to tcp server");
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
                        ma.view_textView_log.append(s);
                        if (s.contains("rs=data")) {
                            ma.parse_received_data(s);
                        }
                        if (ma.progressDialog != null && ma.progressDialog.isShowing() && ma.cuasInProgress) {
                            if (s.contains(":cuas=ok:")) {
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Success. Data has been received and stored.");
                                ma.cuasInProgress = false;
                            } else if (s.contains(":cuas=time-out:")) {
                                ma.cuasInProgress = false;
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Time-Out. Please try again.");
                            }
                        }


                    } catch (Exception e) {
                        ma.view_textView_log.setText("error: " + e.toString());

                    }
                    break;

                case MainActivity.MSG_LINE_RECEIVED:
                    try {
                        String s = (String)msg.obj;
                        ma.view_textView_log.append(s + "\n");
                        if (s.contains("rs=data")) {
                            ma.parse_received_data(s);
                        }
                        if (ma.progressDialog != null && ma.progressDialog.isShowing() && ma.cuasInProgress) {
                            if (s.contains(":cuas=ok:")) {
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Success. Data has been received and stored.");
                                ma.cuasInProgress = false;
                            } else if (s.contains(":cuas=time-out:")) {
                                ma.cuasInProgress = false;
                                ma.progressDialog.hide();
                                ma.showAlertDialog("Time-Out. Please try again.");
                            }
                        }


                    } catch (Exception e) {
                        ma.view_textView_log.setText("error: " + e.toString());

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
        view_textView_log.append("transmit: " + s + "\n");
        if (use_wifi) tcpSocket_transmit(s);
    }

    CompoundButton.OnCheckedChangeListener onCheckedChanged = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton button, boolean isChecked) {
            switch (button.getId()) {

                case R.id.checkBox_rtc_only:

                    (view_checkBox_daily_up).setEnabled(!isChecked);
                    (view_checkBox_daily_down).setEnabled(!isChecked);
                    (view_checkBox_weekly).setEnabled(!isChecked);
                    (view_checkBox_astro).setEnabled(!isChecked);
                    (view_editText_dailyUpTime).setEnabled(!isChecked && ((CheckBox) (view_checkBox_daily_up)).isChecked());
                    (view_editText_dailyDownTime).setEnabled(!isChecked && ((CheckBox) (view_checkBox_daily_down)).isChecked());
                    (view_editText_weeklyTimer).setEnabled(!isChecked && ((CheckBox) (view_checkBox_weekly)).isChecked());
                    (view_checkBox_random).setEnabled(!isChecked);
                    (view_checkBox_sun_auto).setEnabled(!isChecked);

                    break;

                case R.id.checkBox_daily_up:
                    (view_editText_dailyUpTime).setEnabled(isChecked);
                    if (!isChecked) view_editText_dailyUpTime.setText("");
                    break;

                case R.id.checkBox_daily_down:
                    (view_editText_dailyDownTime).setEnabled(isChecked);
                    if (!isChecked) view_editText_dailyDownTime.setText("");
                    break;

                case R.id.checkBox_weekly:
                    (view_editText_weeklyTimer).setEnabled(isChecked);
                    if (!isChecked) view_editText_weeklyTimer.setText("");
                    break;

                case R.id.checkBox_astro:
                    (view_editText_astroMinuteOffset).setEnabled(isChecked);
                    if (!isChecked) view_editText_astroMinuteOffset.setText("");
                    break;
            }
        }
    };

    public void onCheckedClick(View view) {
        boolean isChecked = ((CheckBox) view).isChecked();


        if (isChecked) switch (view.getId()) {
            case R.id.checkBox_daily_up:
                (view_editText_dailyUpTime).setText(def_dailyUp);
                break;

            case R.id.checkBox_daily_down:
                (view_editText_dailyDownTime).setText(def_dailyDown);
                break;

            case R.id.checkBox_weekly:
                (view_editText_weeklyTimer).setText(def_weekly);
                break;

            case R.id.checkBox_astro:
                (view_editText_astroMinuteOffset).setText(def_astro);
                break;
        }

    }

    final String timeFormat = "%4d-%02d-%02dT%02d:%02d:%02d";

    private void fer_send_time() throws IOException {
        Calendar c = Calendar.getInstance();

        String sd = new SimpleDateFormat("yyyy-MM-dd").format(c.getTime());
        String st = new SimpleDateFormat("HH:mm:ss").format(c.getTime());


        String cmd = String.format("config rtc=%sT%s;", sd, st);
        view_textView_log.append(cmd + "\n");
        transmit(cmd);

    }

    int group = 3, memb = 1; // FIXME:
    int group_max = 4;  // FIXME : configure this from settings
    int[] memb_max = {0, 5, 5, 1, 1, 0, 0, 0};  // FIXME: configure this from settings
    static final String sendFmt   = "send mid=%d a=%d g=%d m=%d c=%s;";
    static final String timerFmt  = "timer mid=%d a=%d g=%d m=%d%s;";
    static final String configFmt = "config mid=%d %s;";

    boolean wait_for_saved_timer = false;

    void get_saved_timer(int g, int m) throws java.io.IOException {
        transmit(String.format("timer mid=%d g=%d m=%d rs=2;", getMsgid(), g, m));
        wait_for_saved_timer = true;
    }

    void parse_received_data(String s) {
        try {
            s = s.substring(s.indexOf((":rs=data: ")));

       //     tvRec.append(String.format("###%s###\n", s));

            Short g = 0, m = 0, sun_auto = 0, random = 0, astro = 0;
            String daily = "", weekly = "";
            boolean hasAstro = false;

            if (s.startsWith(":rs=data: none")) {

            } else if (s.startsWith(":rs=data: timer ")) {
                int scIdx = s.indexOf(';');


                if (scIdx > 16) {
                    s = s.substring(16, scIdx);
                } else {
                    s = s.substring(16);
                }

                Pattern p = Pattern.compile("\\s+");
                String arr[] = p.split(s);


                for (String i : arr) {
                    if (i.contains("=")) {
                        int idxES = i.indexOf('=');
                        String key = i.substring(0, idxES);
                        String val = i.substring(idxES + 1);
                //        tvRec.append(String.format("##%s##%s\n", key, val));

                        if (key.equals("g")) {
                            g = Short.parseShort(val);

                        } else if (key.equals("m")) {
                            m = Short.parseShort(val);

                        } else if (key.equals("sun-auto")) {
                            sun_auto = Short.parseShort(val);

                        } else if (key.equals("random")) {
                            random = Short.parseShort(val);

                        } else if (key.equals("astro")) {
                            hasAstro = true;
                            astro = Short.parseShort(val);

                        } else if (key.equals("daily")) {
                            daily = val;

                        } else if (key.equals("weekly")) {
                            weekly = val;

                        }

                    }
                }


            }

            ((CheckBox) (view_checkBox_sun_auto)).setChecked(sun_auto == 1);
            ((CheckBox) (view_checkBox_random)).setChecked(random == 1);
            ((CheckBox) (view_checkBox_weekly)).setChecked(!weekly.isEmpty());
            ((CheckBox) (view_checkBox_astro)).setChecked(hasAstro);
            ((EditText) (view_editText_weeklyTimer)).setText(weekly);
            ((CheckBox) (view_checkBox_daily_up)).setChecked(!(daily.isEmpty() || daily.startsWith("-")));
            ((CheckBox) (view_checkBox_daily_down)).setChecked(!(daily.isEmpty() || daily.endsWith("-")));

            ((EditText) (view_editText_astroMinuteOffset)).setText(hasAstro ? astro.toString() : "");

            String dailyUp = "", dailyDown = "";

            if (!daily.startsWith("-")) {
                dailyUp = daily.substring(0,2) + ":" + daily.substring(2,4);
                daily = daily.substring(4);
            } else {
                daily = daily.substring(1);
            }

            if (!daily.startsWith("-")) {
                dailyDown = daily.substring(0,2) + ":" + daily.substring(2,4);
             }

            ((EditText) (view_editText_dailyUpTime)).setText(dailyUp);
            ((EditText) (view_editText_dailyDownTime)).setText(dailyDown);

        } catch (Exception e) {
            ;
        }

    }

    int getMsgid() { return ++msgid; }

    public void onClick(View view) {

        try {
            switch (view.getId()) {
                case R.id.button_stop:
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "stop"));
                    break;
                case R.id.button_up:
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "up"));
                    break;
                case R.id.button_down:
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "down"));
                    break;
                case R.id.button_g:
                    group = (++group % (group_max + 1));
                    ((TextView) (view_textView_g)).setText(group == 0 ? "A" : String.valueOf(group));
                    if (memb > memb_max[group])
                        memb = 1;
                    ((TextView) (view_textView_e)).setText(group == 0 ? "" : (memb == 0 ? "A" : String.valueOf(memb)));
                    get_saved_timer(group, memb);
                    break;
                case R.id.button_e:
                    memb = (++memb % (memb_max[group] + 1));
                    ((TextView) (view_textView_e)).setText(group == 0 ? "" : (memb == 0 ? "A" : String.valueOf(memb)));
                    get_saved_timer(group, memb);
                    break;
                case R.id.button_sun_pos:
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "sun-down"));
                    break;

                case R.id.button_timer:
                    String upTime = ((EditText) (view_editText_dailyUpTime)).getText().toString();
                    String downTime = ((EditText) (view_editText_dailyDownTime)).getText().toString();
                    String astroOffset = ((EditText) (view_editText_astroMinuteOffset)).getText().toString();
                    boolean rtc_only;

                    String timer = "";

                    if (rtc_only = ((CheckBox) (view_checkBox_rtc_only)).isChecked()) {
                        timer += " rtc-only=1";
                    } else {
                        boolean dailyUpChecked = ((CheckBox) (view_checkBox_daily_up)).isChecked();
                        boolean dailyDownChecked = ((CheckBox) (view_checkBox_daily_down)).isChecked();
                        boolean weeklyChecked = ((CheckBox) (view_checkBox_weekly)).isChecked();

                        if (dailyUpChecked || dailyDownChecked) {
                            timer += " daily=";
                            timer += dailyUpChecked ? upTime.substring(0, 2) + upTime.substring(3, 5) : "-";
                            timer += dailyDownChecked ? downTime.substring(0, 2) + downTime.substring(3, 5) : "-";
                        }

                        if (((CheckBox) (view_checkBox_astro)).isChecked()) {
                            timer += " astro=";
                            timer += astroOffset;
                        }

                        if (weeklyChecked) {
                            String weeklyTimer = ((EditText) (view_editText_weeklyTimer)).getText().toString();

                            timer += " weekly=";
                            timer += weeklyTimer;
                        }

                    }


                    if (((CheckBox) (view_checkBox_sun_auto)).isChecked()) {
                        timer += " sun-auto=1";
                    }

                    if (((CheckBox) (view_checkBox_random)).isChecked()) {
                        timer += " random=1";
                    }


                    // timer = upTime.substring(0,2);


                    transmit(String.format(timerFmt, getMsgid(), 0, group, memb, timer));
                    if (!rtc_only ) {
                        enableSend(false, 5);
                    }


                    break;


            }


        } catch (Exception e) {
            view_textView_log.setText(e.toString());
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
                    transmit(String.format(configFmt, getMsgid(), "cu=auto"));
                    showProgressDialog("Press the Stop-Button on your Fernotron Central Unit in the next 60 seconds...", 60);
                    break;

                case R.id.action_setFunc:
                    transmit(String.format(sendFmt, getMsgid(), 0, group, memb, "set"));
                    showAlertDialog("You now have 60 seconds remaining to press STOP on the transmitter you want to add/remove. Beware: If you press STOP on the central unit, the device will be removed from it. To add it again, you would need the code. If you don't have the code, then you would have to press the physical set-button on the device");
                    break;
            }

        } catch (IOException e) {
            view_textView_log.setText(e.toString());
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
