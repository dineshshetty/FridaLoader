package com.dns.fridaloader;

import android.graphics.Color;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import life.sabujak.roundedbutton.RoundedButton;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import com.muddzdev.styleabletoast.StyleableToast;

import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    String receivedData = "";
    int downloadIdOne;
    private static String dirPath;
    String request_endpoint = "https://api.github.com/repos/frida/frida/releases/latest";
    String URL1 ="";
   // String URL1 = "https://github.com/frida/frida/releases/download/12.6.6/frida-server-12.6.6-android-x86.xz";
    String frida_url_prefix = "https://github.com/frida/frida/releases/download/";

    Button performDownloadCancel,  performDownload ;
    RoundedButton reCheckStatusButton;
    ProgressBar downloadProgressBar;
    TextView textViewActionProgress, fridaStatusTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //handle super user permissions
        executeCommand("su 0 ls", true);
        dirPath = Util.getRootDirPath(getApplicationContext());
        performDownloadCancel = (Button) findViewById(R.id.button_cancel_frida);
        downloadProgressBar = (ProgressBar) findViewById(R.id.progressBarOne);
        textViewActionProgress = (TextView)findViewById(R.id.textViewActionProgress);
        fridaStatusTextView = (TextView)findViewById(R.id.fridaStatusTextView);
        performDownload = (Button) findViewById(R.id.button_download_frida);

        reCheckStatusButton = (RoundedButton) findViewById(R.id.reCheckFridaStatus);



        performDownload.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                performDownloadAndStartFrida();
            }
        });

        reCheckStatusButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                doFridaStuff();
            }
        });

        SystemClock.sleep(2000);
        doFridaStuff();

    }

    private void doFridaStuff() {

       // setupFridaURL();

        Boolean fridaStatus = checkFridaStatusCode();
        if(fridaStatus){
            //frida already running
            fridaAlreadyRunning();
        }else{
            //frida not currently running
            freshFridaLaunchOptions();
        }
    }


    public void setupFridaURL() {
        HttpURLConnectionGetRequest requestTypeOne = new HttpURLConnectionGetRequest();
        try {
            String archType = execSomeCommandAndGetResponse("getprop ro.product.cpu.abi");
            receivedData = requestTypeOne.execute(request_endpoint, archType).get().toString();
            System.out.println("receivedData = "+receivedData);
            if(!receivedData.contains("ERROR")){
                System.out.println("Frida URL found = "+frida_url_prefix.concat(receivedData));
                URL1 = frida_url_prefix.concat(receivedData);
            }else{
                System.out.println("Something Went Wrong while finding latest branch so fallback to older version");
                URL1 = "https://github.com/frida/frida/releases/download/12.6.6/frida-server-12.6.6-android-x86.xz";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void performDownloadAndStartFrida() {

        showFridaArchSelection();

        setupFridaURL();

        if (Status.RUNNING == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.pause(downloadIdOne);
            return;
        }

        performDownload.setEnabled(false);
        downloadProgressBar.setIndeterminate(true);
        downloadProgressBar.getIndeterminateDrawable().setColorFilter(
                Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN);

        if (Status.PAUSED == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.resume(downloadIdOne);
            return;
        }

        downloadIdOne = PRDownloader.download(URL1, dirPath, "frida-server-latest-xz.xz")
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {
                        reCheckStatusButton.setEnabled(false);
                        downloadProgressBar.setIndeterminate(false);
                        performDownload.setEnabled(true);
                        performDownload.setText("PAUSE PROCESS");
                        performDownloadCancel.setEnabled(true);
                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {
                        performDownload.setText("RESUME PROCESS");
                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        reCheckStatusButton.setEnabled(true);
                        performDownload.setText("START PROCESS");
                        performDownloadCancel.setEnabled(false);
                        downloadProgressBar.setProgress(0);
                        textViewActionProgress.setText("");
                        downloadIdOne = 0;
                        downloadProgressBar.setIndeterminate(false);
                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        downloadProgressBar.setProgress((int) progressPercent);
                        textViewActionProgress.setText(Util.getProgressDisplayLine(progress.currentBytes, progress.totalBytes));
                        downloadProgressBar.setIndeterminate(false);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        performDownloadCancel.setEnabled(false);
                        performDownload.setText("DOWNLOAD & RUN AGAIN");

                        System.out.println("Now uncompressing");
                        try {
                            FileInputStream fin = new FileInputStream("/storage/emulated/0/Android/data/com.dns.fridaloader/files/" + "frida-server-latest-xz.xz");
                            BufferedInputStream in = new BufferedInputStream(fin);
                            FileOutputStream out = new FileOutputStream("/storage/emulated/0/Android/data/com.dns.fridaloader/files/" + "frida-server-latest-decompressed");
                            XZInputStream xzIn = new XZInputStream(in);
                            final byte[] buffer = new byte[8192];
                            int n = 0;
                            while (-1 != (n = xzIn.read(buffer))) {
                                out.write(buffer, 0, n);
                            }
                            out.close();
                            xzIn.close();
                        }
                        catch(Exception e) {
                            Log.e("Decompress", "unzip", e);
                        }

                        executeCommand("su 0 cp /storage/emulated/0/Android/data/com.dns.fridaloader/files/frida-server-latest-decompressed /data/local/tmp/frida-server-latest", true);
                        SystemClock.sleep(2000);

                        executeCommand("su 0 chmod +x /data/local/tmp/frida-server-latest", true);
                        SystemClock.sleep(2000);
                        executeCommand("su 0 /data/local/tmp/frida-server-latest &", true);
                        SystemClock.sleep(2000);
                        fridaStatusTextView.setText("Running");
                        fridaStatusTextView.setTextColor(Color.parseColor("#ACF7C1"));

                        doFridaStuff();
                        reCheckStatusButton.setEnabled(true);
                    }

                    @Override
                    public void onError(Error error) {
                        performDownload.setText("Start");
                        Toast.makeText(getApplicationContext(), "Something Went Wrong" + " " + "1", Toast.LENGTH_SHORT).show();
                        textViewActionProgress.setText("");
                        downloadProgressBar.setProgress(0);
                        downloadIdOne = 0;
                        performDownloadCancel.setEnabled(false);
                        downloadProgressBar.setIndeterminate(false);
                        performDownload.setEnabled(true);
                        reCheckStatusButton.setEnabled(true);
                    }
                });

    }

    private void showFridaArchSelection() {
        //$$$$

//        CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this)
//                .setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT)
//                .setTitle("Select Current Architecture")
//                .setMessage("Frida Server (frida-server-latest) is current not running. How do you want to proceed?")
//                .addButton("Install & Run Latest Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
//                    performDownloadAndStartFrida();
//                    dialog.dismiss();
//                })
//                .addButton("Force Start Existing Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.DEFAULT, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
//
//                    String status = executeCommand("su 0 ls /data/local/tmp/frida-server-latest", false);
//                    if (!status.contains("frida-server-latest")) {
//                        System.out.println("No such file or directory");
//                        fridaStatusTextView.setText("Server Not Found");
//                        fridaStatusTextView.setTextColor(Color.parseColor("#A50104"));
//
//                        StyleableToast.makeText(MainActivity.this, "Server Not Found", Toast.LENGTH_LONG, R.style.red).show();
//                    }else{
//                        StyleableToast.makeText(MainActivity.this, "Server Found. Starting it now.", Toast.LENGTH_LONG, R.style.green).show();
//                        executeCommand("su 0 /data/local/tmp/frida-server-latest &", true);
//                        SystemClock.sleep(2000);
//                        fridaStatusTextView.setText("Running");
//                        fridaStatusTextView.setTextColor(Color.parseColor("#ACF7C1"));
//                        doFridaStuff();
//                    }
//                    dialog.dismiss();
//                })
//                .addButton("Continue", -1, -1, CFAlertDialog.CFAlertActionStyle.DEFAULT, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
//                    dialog.dismiss();
//                });
//        builder.show();
    }

    private boolean checkFridaStatusCode() {
        String status = executeCommand("su 0 ps -A", false);
        if (status.contains("frida-server-latest")) {
        return true;
        }else{
            status = executeCommand("su 0 ps", false);
            if (status.contains("frida-server-latest")) {
                return true;
            }else{
                return false;
            }
        }
    }

    private String execSomeCommandAndGetResponse(String command) {
        //example command = "su 0 ps -A"
        String response = executeCommand(command, false);
        return response;
    }

    private void fridaAlreadyRunning() {
            System.out.println("FRIDA IS RUNNING");
            fridaStatusTextView.setText("Running");
            fridaStatusTextView.setTextColor(Color.parseColor("#ACF7C1"));

            CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this)
                    .setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT)
                    .setTitle("Frida Status")
                    .setMessage("Frida Server (frida-server-latest) is already running. How do you want to proceed?")
                    .addButton("Download & Run Latest Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                        Toast.makeText(MainActivity.this, "Upgrade tapped", Toast.LENGTH_SHORT).show();
                        executeCommand("su 0 killall frida-server-latest &", false);
                        executeCommand("su 0 killall frida-server-latest &", false);
                        fridaStatusTextView.setText("Terminated");
                        performDownloadAndStartFrida();
                        SystemClock.sleep(2000);
                        doFridaStuff();
                        dialog.dismiss();
                    })
                    .addButton("KILL Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                Toast.makeText(MainActivity.this, "Kill Frida tapped", Toast.LENGTH_SHORT).show();
                executeCommand("su 0 killall frida-server-latest &", false);
                fridaStatusTextView.setText("Terminated");
                SystemClock.sleep(2000);
                doFridaStuff();
                dialog.dismiss();
            }).addButton("Continue", -1, -1, CFAlertDialog.CFAlertActionStyle.DEFAULT, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                        dialog.dismiss();
                    });
            builder.show();
    }

    private void freshFridaLaunchOptions() {
        fridaStatusTextView.setText("Not Running");
        fridaStatusTextView.setTextColor(Color.parseColor("#A50104"));

        CFAlertDialog.Builder builder = new CFAlertDialog.Builder(this)
                .setDialogStyle(CFAlertDialog.CFAlertStyle.ALERT)
                .setTitle("Frida Status")
                .setMessage("Frida Server (frida-server-latest) is current not running. How do you want to proceed?")
                .addButton("Install & Run Latest Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                    performDownloadAndStartFrida();
                    dialog.dismiss();
                })
                .addButton("Force Start Existing Frida Server", -1, -1, CFAlertDialog.CFAlertActionStyle.DEFAULT, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {

                    String status = executeCommand("su 0 ls /data/local/tmp/frida-server-latest", false);
                    if (!status.contains("frida-server-latest")) {
                        System.out.println("No such file or directory");
                        fridaStatusTextView.setText("Server Not Found");
                        fridaStatusTextView.setTextColor(Color.parseColor("#A50104"));

                        StyleableToast.makeText(MainActivity.this, "Server Not Found", Toast.LENGTH_LONG, R.style.red).show();
                    }else{
                        StyleableToast.makeText(MainActivity.this, "Server Found. Starting it now.", Toast.LENGTH_LONG, R.style.green).show();
                        executeCommand("su 0 /data/local/tmp/frida-server-latest &", true);
                        SystemClock.sleep(2000);
                        fridaStatusTextView.setText("Running");
                        fridaStatusTextView.setTextColor(Color.parseColor("#ACF7C1"));
                        doFridaStuff();
                    }
                    dialog.dismiss();
                })
                .addButton("Continue", -1, -1, CFAlertDialog.CFAlertActionStyle.DEFAULT, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                    dialog.dismiss();
                });
        builder.show();
    }

    private String executeCommand(String command, boolean standardOutExclude) {
        int readData;
        char[] buffer;
        buffer = new char[4096];
        StringBuilder outputData;
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec(command);
            if (standardOutExclude) {
                return "";
            }
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            outputData = new StringBuilder();
            while ((readData = reader.read(buffer)) > 0)
            {
                outputData.append(buffer, 0, readData);
            }
            reader.close();
            process.waitFor();
            return outputData.toString();
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
