import core.Downloader;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
@Slf4j
public class Main {
    public static void main(String[] args) {

        //获取下载地址

        String url=null;

        if(args==null||args.length==0){
            while (true){
                System.out.println("请输入下载链接");
                Scanner scanner=new Scanner(System.in);
                url=scanner.nextLine();
                if(url!=null){
                    break;
                }
            }
        }else {
            url=args[0];
        }

        Downloader downloader = new Downloader();
        downloader.download(url);

    }
}
