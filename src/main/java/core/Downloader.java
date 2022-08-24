package core;

import constant.Constant;
import lombok.extern.slf4j.Slf4j;
import util.FileUtils;
import util.HttpUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.concurrent.*;


@Slf4j
public class Downloader {
    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    public ThreadPoolExecutor threadPoolExecutor=new ThreadPoolExecutor(Constant.THREAD_NUM,Constant.THREAD_NUM,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(5));
    public CountDownLatch countDownLatch=new CountDownLatch(Constant.THREAD_NUM);
    public void download(String url){
        //获取文件名
        String fileName= HttpUtils.getHttpFileName(url);
        //获得下载路径
        String path= Constant.PATH+fileName;

        //获取本地文件的大小
        long localFileLength = FileUtils.getFileContentLength(path);
        DownloadInfoThread downloadInfoThread=null;
        HttpURLConnection httpURLConnection=null;

        //获取链接对象
        try {
            httpURLConnection = HttpUtils.getHttpURLConnection(url);
            int contentLength = httpURLConnection.getContentLength();
            if(contentLength<=localFileLength){
                log.info("{}文件已经下载过了",path);
                return;
            }
            //创建获取下载信息的任务对象
            downloadInfoThread = new DownloadInfoThread(contentLength);
            //将任务交给线程池,newScheduledThreadPool

            scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread,100,200, TimeUnit.MILLISECONDS);

            ArrayList<Future> list=new ArrayList<Future>();
            spilt(url,list);
//            for (Future future :
//                    list) {
//                future.get();
//
//            }
            /**
             * //等待countDownLatch计数器置0则说明所有任务结束
             * */
            countDownLatch.await();
            //合并文件
            if(merge(path)){
                clearTemp(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            log.info("下载完成!!!");
            scheduledExecutorService.shutdown();
            threadPoolExecutor.shutdown();
        }

        /*try {
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            FileOutputStream fileOutputStream = new FileOutputStream(path);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            int len=-1;
            byte[] bytes = new byte[Constant.BYTE_SIZE];
            while ((len=bufferedInputStream.read(bytes))!=-1){
                downloadInfoThread.downSize.add(len);
                bufferedOutputStream.write(bytes,0,len);
            }
        } catch (IOException e) {
            log.error("{}下载失败",path);
            e.printStackTrace();
        }finally {
            if(httpURLConnection!=null){
                httpURLConnection.disconnect();
                log.info("\n{}下载成功",path);
            }

        }*/
    }
    public void spilt(String url, ArrayList<Future> futureList){

        //获取下载文件大小

        long contentLength=HttpUtils.getHttpFileContentLength(url);

        //切分为五块
        long size=contentLength/Constant.THREAD_NUM;

        //根据线程数量计算分块个数

        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            //下载文件的起始位置
            long startPos=i*size;

            long endPos;

            if(i==Constant.THREAD_NUM-1){
                //下载最后一块,只需要将剩余的部分下载完毕即可
                endPos=0;
            }else{
                endPos=startPos+size;

            }
            //如果不是第一块,起始位置要+1
            if(startPos!=0){
                startPos++;
            }
            //创建任务
            DownloadTask downloadTask = new DownloadTask(url, startPos, endPos, i,countDownLatch);
            //将子任务提交到线程池中
            Future<Boolean> future = threadPoolExecutor.submit(downloadTask);
            futureList.add(future);


        }

    }
    /**
     * 合并临时文件
     * */
    public boolean merge(String fileName) {
        log.info("开始合并文件{}", fileName);
        byte[] buffer = new byte[Constant.BYTE_SIZE];
        int len = -1;
        try (RandomAccessFile accessFile = new RandomAccessFile(fileName, "rw")) {

            for (int i = 0; i < Constant.THREAD_NUM; i++) {
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(fileName + ".temp" + i))) {
                    while ((len = bufferedInputStream.read(buffer)) != -1) {
                        accessFile.write(buffer, 0, len);
                    }
                }
            }
            log.info("合并完毕");
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 删除临时文件
     * */
    public boolean clearTemp(String fileName){

        for (int i = 0; i < Constant.THREAD_NUM; i++) {
            final File file = new File(fileName + ".temp" + i);
            file.delete();
        }
        return true;
    }

}
