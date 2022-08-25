package util;

import lombok.extern.slf4j.Slf4j;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j

public class HttpUtils {
    /**
     * 获取文件大小
    * */
    public static long getHttpFileContentLength(String url) {
        HttpURLConnection httpURLConnection= null;
        int contentLength=0;
        try {
            httpURLConnection = getHttpURLConnection(url);
            contentLength = httpURLConnection.getContentLength();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            httpURLConnection.disconnect();
        }
        return contentLength;

    }


    /**
    * @param startPos 下载文件起始位置
    * @param endPos 下载文件结束位置
    * */
    public static HttpURLConnection getHttpURLConnection(String url,long startPos,long endPos) throws Exception{
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        log.info("下载的区间是:{}--{}",startPos,endPos);
      if(endPos!=0){
          httpURLConnection.setRequestProperty("RANGE","bytes="+startPos+"-"+endPos);

      }else{
          httpURLConnection.setRequestProperty("RANGE","bytes="+startPos+"-");
      }
      return httpURLConnection;


    }

    /**
     * 获取HttpURLConnection连接对象
     * */
    public static HttpURLConnection getHttpURLConnection(String url) throws Exception{

        URL url1=new URL(url);

        HttpURLConnection HttpUrlConnection = (HttpURLConnection)url1.openConnection();
        //设置Header头
        HttpUrlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.0.0 Safari/537.36");

        return HttpUrlConnection;
    }
    public static String getHttpFileName(String url){
        //获取到最后一个/的index值
        int index=url.lastIndexOf("/");
        //最后一个/之后的就是文件名
        return url.substring(index+1);
    }
}
