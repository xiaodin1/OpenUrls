import java.io.BufferedReader; 
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader; 
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection; 
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL; 
import java.net.URLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.awt.Toolkit;

public class OpenUrlsMain { 
	//草榴社区
	private static String hostUrl = "http://cl.totu.me/";
	//XIAAV论坛
//	private static String hostUrl = "http://xav3.info/";
//	private static String requestUrl = "http://xav3.info/forum.php";
	
	private static int BREAK_COUNT = 1001;
    
	//获取HTML代码（UA设置错误可能导致服务器返回403）
	public static String getHTML(String pageURL, String encoding) { 
        StringBuilder pageHTML = new StringBuilder(); 
        try { 
            URL url = new URL(pageURL); 
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 "); 
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding));
            String line = null; 
            while ((line = br.readLine()) != null) { 
                pageHTML.append(line); 
                pageHTML.append("\r\n"); 
            } 
            connection.disconnect(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        } 
        return pageHTML.toString(); 
    } 
	
	/**
	 * 下载网络图片
	 * @param urlString
	 * @param filename
	 * @param savePath
	 * @throws Exception
	 */
	 public static void download(String urlString, String filename,File sf) throws Exception {  
	        // 构造URL  
	        URL url = new URL(urlString);  
	        // 打开连接  
	        URLConnection con = url.openConnection();  
	        //设置请求超时为5s  
	        con.setConnectTimeout(15*1000); 
	        //设置UA
	        con.setRequestProperty("User-Agent", "Mozilla/4.0 "); 
	        // 输入流  
	        InputStream is = con.getInputStream();  
	      
	        // 1K的数据缓冲  
	        byte[] bs = new byte[1024];  
	        // 读取到的数据长度  
	        int len;  
	        // 输出的文件流  
//	       File sf=new File(savePath);  
//	       if(!sf.exists()){  
//	           sf.mkdirs();  
//	       }  
	       OutputStream os = new FileOutputStream(sf.getPath()+"\\"+filename);  
	        // 开始读取  
	        while ((len = is.read(bs)) != -1) {  
	          os.write(bs, 0, len);  
	        }  
	        // 完毕，关闭所有链接  
	        os.close();  
	        is.close();  
	    }   
	
    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static void readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        try {
//            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
            	if(line > BREAK_COUNT)
            	{
            		break;
            	}
                // 显示行号
//                System.out.println("line " + line + ": " + tempString);
//                line++;
//            	System.out.println(tempString);
            	
            	//草榴社区解析逻辑
            	if(tempString.contains("<h3><a href=") && tempString.contains("</a></h3>"))
            	{
            		String DirName = tempString.substring(tempString.indexOf("\">") + 2, tempString.indexOf("</a></h3>")).replace("/","-").replace("?", "").replace("&nbsp;", "");
                    System.out.println("line " + line + ": " + DirName);
                    line++;
            		int subUrlBegin = tempString.indexOf("htm_data");
            		int subUrlEnd = tempString.indexOf(".html");
            		if(subUrlEnd == -1)	//没有.html的不是
            		{
            			continue;
            		}
            		String url = hostUrl + tempString.substring(subUrlBegin, subUrlEnd + 5);	//5: .html
            		try{
            			//v1.0：直接用默认浏览器打开网页，速度慢、占资源多，而且隐蔽性差、资源筛选比较费神
//            			java.awt.Desktop.getDesktop().browse(new URI(url));  
//            			Thread.sleep(1200);
            			
            			
            			
            			OpenUrlsMain.parseCaoLiuPostInfo(fileName, DirName, url);
            		}
            		catch(Exception ex)
            		{
            			
            		}
            	}
            	//XIAAV解析逻辑
//            	if(tempString.contains("<a href=") && tempString.contains("onclick=\"atarget(this)\""))
//            	{
//                    System.out.println("line " + line + ": " + tempString);
//                    line++;
//            		int subUrlBegin = tempString.indexOf("thread");
//            		int subUrlEnd = tempString.indexOf(".html");
//            		if(subUrlEnd == -1)	//没有.html的不是
//            		{
//            			continue;
//            		}
//            		String url = hostUrl + tempString.substring(subUrlBegin, subUrlEnd + 5);	//5: .html
//            		try{
//            			java.awt.Desktop.getDesktop().browse(new URI(url));  
//            			Thread.sleep(2000);
//            		}
//            		catch(Exception ex)
//            		{
//            			
//            		}
//            	}
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        Toolkit kit = Toolkit.getDefaultToolkit();
        kit.beep();
    }
    
    /**
	 * 草榴贴解析说明：
	 * 
	 * 1，HTML代码中，第一次出现的<div class=\"tpc_content do_not_catch>...到/div>是楼主，我们只取这里面的内容
	 * 2，从楼主的HTML代码中，第一个<img src=' ... 到>&nbsp;是第一张预览图（不能用&nbsp不带>，容易把空格给算成
	 * &nbsp，不准确），通常来讲，预览图在HTML代码中都是连续的。如果&nbsp;后面跟的下一个标签不是<img src而且别
	 * 的，说明是楼主的其他资源广告，这之后的图片就不需要了
	 * 3，http://www.rmdown.com/link.php是种子文件的网络地址，这个页面是需要在浏览器中打开的，确定想下载时候手动开启即可
	 */
    public static void parseCaoLiuPostInfo(String folderName, String subDirName, String url)
    {
    	//http://cl.totu.me/htm_data/15/1512/1744793.html 3个
    	//http://cl.totu.me/htm_data/15/1512/1744977.html 1个
    	
    	//创建本地文件夹
    	File dir = new File(folderName.replace(".html", ""));
    	if(!dir.exists()) {
    		dir.mkdir();
    	}
    	
    	File subDir = new File(dir.getAbsolutePath() + "\\" + subDirName);
    	if(!subDir.exists()) {
    		subDir.mkdir();
    	}
    	
    	//传入帖子地址，得到html代码字符串
    	String html = OpenUrlsMain.getHTML(url, "GBK");	
    	
    	//获取楼主信息
    	String louzhuTagStart = "<div class=\"tpc_content do_not_catch";	//只有第一个是楼主，后面的是其他人的回帖
    	String louzhuTagEnd = "/div";	//只有louzhuTagStart后面的第一个/div是楼主信息结束tag，其他不是
    	
    	String yulantuTagStart = "<img src='";	//预览图标签开始
    	String yulantuTagEnd = ">&nbsp;";		//预览图标签结束（注意，下一个必须是yulantuTagStart开头，否则后面的图就不是本帖内容，就不要了
    	
    	int louzhuStartIdx = html.indexOf(louzhuTagStart);
    	int louzhuEndIdx = html.indexOf(louzhuTagEnd, louzhuStartIdx) - 1;
    	String info = html.substring(louzhuStartIdx, louzhuEndIdx);	//得到楼主内容的HTML代码
//    	System.out.println(info + "\n");
    	
    	//从楼主信息中，遍历连续预览图
    	int imageStartIdx = info.indexOf(yulantuTagStart);	//<img src='
    	int imageEndIdx = -1; 
    	int imageWhiteSpacePlaceHolderIdx = -1;	//&nbsp;出现的位置
    	int idx = 0;
    	
    	int cpuNums = Runtime.getRuntime().availableProcessors();  //获取当前系统的CPU 数目
    	ExecutorService executorService =Executors.newFixedThreadPool(cpuNums * 5);	//4核CPU的话，每核心开5个线程（测试）
    	
    	StringBuilder sb = new StringBuilder();
    	
    	while(imageStartIdx != -1) {
    		imageEndIdx = info.indexOf("g'",imageStartIdx) + 1;	//http://... .jpg(png?jpeg?无所谓，只要最后是g就可以）
    		if(imageEndIdx > 0) {
    			String imgUrl = info.substring(imageStartIdx + yulantuTagStart.length(), imageEndIdx);	//预览图地址
    			//TODO:添加下载预览图策略
    			try {
//					OpenUrlsMain.download(imgUrl, subDirName + String.format("%3d.jpg",idx), subDir);
    				idx++;
    				executorService.execute(new Downloader(imgUrl, new RandomAccessFile(subDir.getPath()+"\\"+ subDirName + String.format("%3d.jpg",idx), "rw")));
				} catch (Exception e) {
					e.printStackTrace();
				}
    			if(imgUrl.length() < 100) {	//TODO：匹配条件暂时还有点问题，会取出一些“本站担保【官方直营】【澳门金沙娱乐场】【1258.com”这样的数据，暂时略去超长的imgUrl
//    				System.out.println(imgUrl);
    				sb.append(imgUrl + "\r\n");
    			}
		    	imageWhiteSpacePlaceHolderIdx = info.indexOf(yulantuTagEnd, imageEndIdx);
		    	imageStartIdx = info.indexOf(yulantuTagStart, imageEndIdx);

		    	/**
		    	 * 说明：>&nbsp;之后的下一个标签如果不是<img src='，index会远大于15，离得很远，所以就不要了。
		    	 * 之所以不选2这样紧挨着的，是因为有些楼主排版会加上</br>这样的换行符
		    	 */
		    	if(Math.abs(imageStartIdx - (imageWhiteSpacePlaceHolderIdx + yulantuTagEnd.length())) > 15) {	
		    		break;
		    	}
    		} else {
    			break;	//没找到.jpg，错误：中断循环
    		}
    	}
    	
    	//拿BT种子下载地址
    	int btSeedStartIdx = info.indexOf("http://www.rmdown.com");	//仅此一份
    	int btSeedEndIdx = info.indexOf("</a>", btSeedStartIdx);
    	String btUrl = info.substring(btSeedStartIdx, btSeedEndIdx);
    	System.out.println();
    	System.out.println(btUrl);
    	System.out.println("\n\n\n");
    	try {
			File btTxt = new File(subDir.getAbsolutePath() + "\\" + subDirName + ".txt");
			BufferedWriter bw = new BufferedWriter(new FileWriter(btTxt));
			bw.write(sb.toString());
			bw.write(btUrl);
			bw.write("\r\n");
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public static void main(String args[]){
    	if (args.length > 0) {
			String tmp = args[0];
			if(tmp.equalsIgnoreCase("-h")) {
				System.out.println("调用方法：java OpenUrlsMain http://cl.totu.me\n可以修改草榴的地址前缀，这样就不用经常编译了");
				return;
			}
			else if(!tmp.startsWith("http")) {
				System.out.println("警告：前缀错误。应该是http开头的格式，如:http://cl.totu.me/");
				return;
			}
			hostUrl = tmp;
		}
    	
    	File file = new File("D:\\");
    	if(file.isDirectory()) {
    		for(File f : file.listFiles()) {
    			if(f.getName().endsWith(".html") || f.getName().endsWith(".htm")) {
    				System.out.println(f.getName());
    				OpenUrlsMain.readFileByLines(String.format("D:\\%s", f.getName()));
//    				f.delete();
    				break;
    			}
    		}
    	}
    	System.out.println("执行完毕");
    } 
} 