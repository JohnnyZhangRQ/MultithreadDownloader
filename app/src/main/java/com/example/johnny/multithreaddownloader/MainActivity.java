package com.example.johnny.multithreaddownloader;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements DownloadLinkDialog.DownloadLinkListener{
    private TextView tvFilename,tvSpeed,tvPercent,tvNoDownload;
    private LinearLayout llDownloadProgress;
    private ProgressBar pbDownloadProgress;
    private double beginTime; //单位秒
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DownloadLinkDialog dialog = new DownloadLinkDialog();
                dialog.show(getFragmentManager(),"下载链接");
            }
        });
    }

    @Override
    public void onDownloadLinkComplete(String downloadLink) {
        if (TextUtils.isEmpty(downloadLink.trim())){
            Toast.makeText(this,"输入内容为空",Toast.LENGTH_SHORT).show();
        }else {
            doDownload(downloadLink);
        }
    }

    public void initView(){
        llDownloadProgress = (LinearLayout) findViewById(R.id.ll_download_progress);
        tvFilename = (TextView) findViewById(R.id.tv_filename);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        tvPercent = (TextView) findViewById(R.id.tv_percent);
        tvNoDownload = (TextView) findViewById(R.id.tv_no_download);
        pbDownloadProgress = (ProgressBar) findViewById(R.id.progress_bar);
    }

    /**
     * 使用Handler更新UI界面信息
     */
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!msg.getData().getBoolean("isSucceed")){
                Toast.makeText(MainActivity.this,"文件获取失败,请确认下载链接是否正确",Toast.LENGTH_SHORT).show();
            }else {
                llDownloadProgress.setVisibility(View.VISIBLE);
                tvNoDownload.setVisibility(View.GONE);
                pbDownloadProgress.setProgress(msg.getData().getInt("size"));

                double currentTime=(new Date()).getTime()/1000.0;

                float temp = (float) pbDownloadProgress.getProgress()
                        / (float) pbDownloadProgress.getMax();

                double speed = msg.getData().getInt("size")/1000.0/(currentTime - beginTime);
                BigDecimal bd =  new BigDecimal(speed);

                int progress = (int) (temp * 100);
                if (progress == 100) {
                    Toast.makeText(MainActivity.this, "下载完成！", Toast.LENGTH_LONG).show();
                }
                tvPercent.setText("下载进度：" + progress + " %");
                tvSpeed.setText("下载速度："+bd.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue()+"kb/s");
            }
        }
    };

    private void doDownload(String string) {
        // 获取SD卡路径
        String path = Environment.getExternalStorageDirectory()
                + "/myDownload/";
        File file = new File(path);
        // 如果SD卡目录不存在创建
        if (!file.exists()) {
            file.mkdir();
        }
        // 设置progressBar初始化
        pbDownloadProgress.setProgress(0);

        String downloadUrl = string;
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/')+1);
        tvFilename.setText("文件名："+fileName);
        int threadNum = 4;
        String filepath = path + fileName;
        Log.e(TAG, "download file  path:" + filepath);
        DownloadTask task = new DownloadTask(downloadUrl, threadNum, filepath);
        task.start();
    }

    class DownloadTask extends Thread {
        private String downloadUrl;// 下载链接地址
        private int threadNum;// 开启的线程数
        private String filePath;// 保存文件路径地址
        private int blockSize;// 每一个线程的下载量

        public DownloadTask(String downloadUrl, int threadNum, String filePtah) {
            this.downloadUrl = downloadUrl;
            this.threadNum = threadNum;
            this.filePath = filePtah;
        }

        @Override
        public void run() {
            FileDownloadThread[] threads = new FileDownloadThread[threadNum];
            try {
                URL url = new URL(downloadUrl);
                Log.d(TAG, "download file http path:" + downloadUrl);
                URLConnection conn = url.openConnection();
                // 读取下载文件总大小
                int fileSize = conn.getContentLength();
                if (fileSize <= 0) {
                    //Toast.makeText(MainActivity.this,"获取文件失败",Toast.LENGTH_SHORT).show();
                    Log.e(TAG,"文件获取失败");
                    Message msg = new Message();
                    msg.getData().putBoolean("isSucceed",false);
                    mHandler.sendMessage(msg);
                }else {
                    beginTime =(new Date()).getTime()/1000.0;
                    // 设置ProgressBar最大的长度为文件Size
                    pbDownloadProgress.setMax(fileSize);

                    // 计算每条线程下载的数据长度
                    blockSize = (fileSize % threadNum) == 0 ? fileSize / threadNum
                            : fileSize / threadNum + 1;

                    Log.e(TAG, "fileSize:" + fileSize + "  blockSize:");

                    File file = new File(filePath);
                    for (int i = 0; i < threads.length; i++) {
                        // 启动线程，分别下载每个线程需要下载的部分
                        threads[i] = new FileDownloadThread(url, file, blockSize,
                                (i + 1));
                        threads[i].setName("Thread:" + i);
                        threads[i].start();
                    }

                    boolean isFinished = false;
                    int downloadedAllSize = 0;
                    while (!isFinished) {
                        isFinished = true;
                        // 当前所有线程下载总量
                        downloadedAllSize = 0;
                        for (int i = 0; i < threads.length; i++) {
                            downloadedAllSize += threads[i].getDownloadLength();
                            if (!threads[i].isCompleted()) {
                                isFinished = false;
                            }
                        }
                        // 通知handler去更新视图组件
                        Message msg = new Message();
                        msg.getData().putInt("size", downloadedAllSize);
                        msg.getData().putBoolean("isSucceed",true);
                        mHandler.sendMessage(msg);
                        Log.e(TAG, "current downloadSize:" + downloadedAllSize);
                        Thread.sleep(1000);// 休息1秒后再读取下载进度
                    }
                    Log.e(TAG, " all of downloadSize:" + downloadedAllSize);
                }
            } catch (MalformedURLException e) {
                Message msg = new Message();
                msg.getData().putBoolean("isSucceed",false);
                mHandler.sendMessage(msg);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
