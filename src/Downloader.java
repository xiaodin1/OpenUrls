import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;


public class Downloader extends Thread {
	/**
	 * 最大重试次数
	 */
	public static final int MAX_TRY_TIMES = 3;
	/**
	 * 线程ID（唯一标示）
	 */
	public int id;
	/**
	 * 线程最多可以尝试的次数（如果达到3次，就算了）
	 */
	public int tryTimes;
	/**
	 * 需要下载的图片链接地址
	 */
	public String imgUrl;
	/**
	 * 将下载到的字节输出到raf中  
	 */
    public RandomAccessFile raf; 
	/**
	 * 任务出现异常时的回调
	 */
	public IDownloaderCallback callback;
	
	public Downloader(int id, RandomAccessFile raf, String imgUrl, int tryTimes, IDownloaderCallback callback) {
		assert(raf != null && imgUrl != null && callback != null && tryTimes >= 1 && tryTimes <= MAX_TRY_TIMES);
		this.id = id;
		this.raf = raf;
		this.imgUrl = imgUrl;
		this.callback = callback;
		this.tryTimes = tryTimes;
	}

	@Override
	public void run() {
		super.run();	
		System.out.println("线程:" + this.id + " 第" + tryTimes + "次执行...");
		this.Download(this.imgUrl, this.raf);		
	}
	
	public void Download(String imgUrl, RandomAccessFile raf)
	{
		try {  
        	// 构造URL  
	        URL url = new URL(imgUrl);  
	        // 打开连接  
	        URLConnection con = url.openConnection();  
	        //设置请求超时为10s  
	        con.setConnectTimeout(10*1000); 
	        //设置UA
	        con.setRequestProperty("User-Agent", "Mozilla/4.0 "); 
	        // 输入流  
	        InputStream is = con.getInputStream();  
	      
	        // 1K的数据缓冲  
	        byte[] bs = new byte[1024 * 1];  
	        // 读取到的数据长度  
	        int len;  
	        // 输出到文件流  
	        // 开始读取  
	        while ((len = is.read(bs)) != -1) {  
	          this.raf.write(bs, 0, len);  
	        }  
	        // 完毕，关闭所有链接  
	        is.close();
	        raf.close();
	        System.out.println("线程:" + this.id + " 执行成功！！");
        } catch (Exception e) {
			if(callback != null) {
				callback.onException(this, e);	//出现异常，丢给主线程（如果是TimeOutException，重新添加）
			}
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
