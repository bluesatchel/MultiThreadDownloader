package core;

import constant.Constant;
import lombok.extern.slf4j.Slf4j;
import util.HttpUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * @see java.util.concurrent.Callable
 * 实现Callable接口允许线程返回值
 * */
@Slf4j
public class DownloadTask implements Callable<Boolean> {

    private String url;

    private long startPos;

    private long endPos;
    //标识当前是第几部分
    private int part;
    private CountDownLatch countDownLatch;

    public DownloadTask(String url, long startPos, long endPos, int part, CountDownLatch countDownLatch) {
        this.url = url;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.countDownLatch = countDownLatch;
    }

    public Boolean call() throws Exception {
        //获取文件名
        String httpFileName= HttpUtils.getHttpFileName(url);

        //分块的文件名
        String originalName=httpFileName+".temp";
        originalName=Constant.PATH+originalName;
        httpFileName=httpFileName+".temp"+part;

        httpFileName= Constant.PATH+httpFileName;
        //获取分块下载的连接


        /**
         * 在这里实现断点续传功能
         *
         * 首先校验temp文件是否存在,如果存在则获取temp文件的大小,然后传递给getHttpURLConnection获取到对应的下载字节范围,然后利用RandomAccessFile类进行续写
         * */

        File file = new File(httpFileName);
        if(file.exists()){

            long length = file.length();
            //只有第0块可以直接用length传入
            HttpURLConnection httpURLConnection = null;
            if(part==0){
                httpURLConnection=HttpUtils.getHttpURLConnection(url, length, endPos);

            }else{
                httpURLConnection=HttpUtils.getHttpURLConnection(url, startPos+length, endPos);
            }
            try {
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);

                RandomAccessFile accessFile=new RandomAccessFile(httpFileName,"rw");
                accessFile.seek(accessFile.length());

                byte[] buffer=new byte[Constant.BYTE_SIZE];
                int len=-1;
                while ((len=bufferedInputStream.read(buffer))!=-1){
                    //1秒内下载数据之和
                    //downSize是一个原子类
                    DownloadInfoThread.downSize.add(len);
                    accessFile.write(buffer,0,len);
                }
                accessFile.close();
                return true;

            } catch (FileNotFoundException exception) {
                log.error("下载文件不存在{}",url);
                return false;
            }catch (Exception e){
                log.error("下载出现异常");
                return false;
            }finally {
                httpURLConnection.disconnect();
                //线程执行完成一次减1
                countDownLatch.countDown();
            }
        }else{
            HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);
            try {
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                RandomAccessFile accessFile=new RandomAccessFile(httpFileName,"rw");

                byte[] buffer=new byte[Constant.BYTE_SIZE];
                int len=-1;
                while ((len=bufferedInputStream.read(buffer))!=-1){
                    //1秒内下载数据之和
                    //downSize是一个原子类
                    DownloadInfoThread.downSize.add(len);
                    accessFile.write(buffer,0,len);
                }
                accessFile.close();
                return true;

            } catch (FileNotFoundException exception) {
                log.error("下载文件不存在{}",url);
                return false;
            }catch (Exception e){
                log.error("下载出现异常");
                return false;
            }finally {
                httpURLConnection.disconnect();
                //线程执行完成一次减1
                countDownLatch.countDown();
            }




        }


    }
}
