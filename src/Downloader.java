import java.io.FileOutputStream;
import java.io.InputStream;  
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;

public class Downloader extends Thread {

    // 将下载到的字节输出到raf中  
    private RandomAccessFile raf;  
    private String url;
    
    public Downloader(String url, RandomAccessFile raf) {
    	System.out.println("创建线程下载：" + url);
    	this.url = url;
        this.raf = raf;  
    }  
    
	public void run() {  
        try {  
        	// 构造URL  
	        URL url = new URL(this.url);  
	        // 打开连接  
	        URLConnection con = url.openConnection();  
	        //设置请求超时为5s  
	        con.setConnectTimeout(5*1000); 
	        //设置UA
	        con.setRequestProperty("User-Agent", "Mozilla/4.0 "); 
	        // 输入流  
	        InputStream is = con.getInputStream();  
	      
	        // 5K的数据缓冲  
	        byte[] bs = new byte[1024 * 5];  
	        // 读取到的数据长度  
	        int len;  
	        // 输出的文件流  
	        // 开始读取  
	        while ((len = is.read(bs)) != -1) {  
	          this.raf.write(bs, 0, len);  
	        }  
	        // 完毕，关闭所有链接  
	        is.close();
	        raf.close();
	        System.out.println("下载完毕：" +  this.url);
        } catch (Exception ex) {  
            ex.printStackTrace();  
        }  
        finally {  
            try {  
                if (raf != null) {  
                    raf.close();  
                }  
            } catch (Exception ex) {  
                ex.printStackTrace();  
            }  
        }  
    }  
}
