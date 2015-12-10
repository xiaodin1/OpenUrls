import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sound.sampled.*;

public class OpenUrlsMain { 
	//CAOLIU社区
	private static String hostUrl = "http://cl.totu.me/";
       
    public static void main(String args[]){
    	if (args.length > 0) {
			String tmp = args[0];
			if(tmp.equalsIgnoreCase("-h") || tmp.equalsIgnoreCase("-help")) {
				System.out.println("调用方法：java OpenUrlsMain http://cl.totu.me\n可以修改草榴的地址前缀，这样就不用经常编译了");
				return;
			}
			else if(!tmp.startsWith("http")) {
				System.out.println("警告：前缀错误。应该是http开头的格式，如:http://cl.totu.me/");
				return;
			}
			hostUrl = tmp;
		}
    	
    	File root = new File("D:\\");
    	if(root.isDirectory()) {
    		for(File f : root.listFiles()) {
    			if(f.getName().endsWith(".html") || f.getName().endsWith(".htm")) {
    				System.out.println("发现文件：" + f.getName());
    				//创建001.html文件对应的文件夹D:\001
    				String dirPath = f.getName().replace(".html", "").replace(".htm", "");
    				File dir = new File(String.format("D:\\%s", dirPath));
    				if(!dir.exists()) {
    					dir.mkdirs();
    				}
    				//读取001.html的数据
    				StringBuilder sb = new StringBuilder();
    				File file = new File(String.format("D:\\%s", f.getName()));
    		        BufferedReader reader = null;
    		        try {
    		            reader = new BufferedReader(new FileReader(file));
    		            String tempString = null;
    		            while ((tempString = reader.readLine()) != null) {
    		            	sb.append(tempString+"\n");
    		            }
    		            reader.close();
    		        } catch(Exception e) {
    		        	e.printStackTrace();
    		        }
    				
    		        //解析001.html文件
    		        HTMLTool htmlTool = new HTMLTool(hostUrl);
    		        //HtmlParser支持直接传入D:\\001.htm，但它不能解析繁体中文，所以传入html代码
    				List<String> posts = htmlTool.parsePostNamesAndUrlsFromHtmlList(sb.toString());
//    				for (String string : strings) {
//						System.out.println(string);
//					}
    				assert(posts.size() / 2 == 0);	//数据必须成对出现
    				
    				//创建线程池
    				ExecutorService es = Executors.newCachedThreadPool();
    				final ThreadPoolExecutor executor = (ThreadPoolExecutor)es;
    				for(int i = 1; i < posts.size(); i+=2) {
    					//创建帖子文件夹
    					String folderName = posts.get(i - 1);
    					File subDir = new File("D:\\" + dirPath + "\\" + folderName);
    			    	if(!subDir.exists()) {
    			    		subDir.mkdirs();
    			    	}
    			    	//解析当前帖子的所有图片
    			    	String postUrl = posts.get(i);
    			    	String code = htmlTool.getHtmlCodeOfPost(postUrl);
    			    	List<String> images = htmlTool.parseImagesUrlFromPost(code);
    			    	//创建回调对象（处理失败）
    			    	IDownloaderCallback callback = new IDownloaderCallback() {
							
							@Override
							public void onException(Downloader d, Exception e) {
								System.out.println("线程出错：" + d.id + " " + e);
								if(d.tryTimes < Downloader.MAX_TRY_TIMES) {
									Downloader dd = new Downloader(d.id, d.raf, d.imgUrl, d.tryTimes + 1, d.callback);
									executor.execute(dd);
									System.out.println("线程重试：" + d.id + "第" + d.tryTimes + "次");
								} else {
									System.out.println("线程：" + d.id + " 达到最大重试限制，无法下载：" + d.imgUrl);
								}
							}
						};
    			    	for(int j = 0; j < images.size(); j++) {
    			    		//循环创建下载线程
    			    		String imgUrl = images.get(i);
    			    		int id = (i - 1) * 100 + j + 1;
    			    		RandomAccessFile raf;
							try {
								raf = new RandomAccessFile(subDir.getPath()+"\\"+ String.format("%3d.jpg",j + 1), "rw");
								Downloader d = new Downloader(id, raf, imgUrl, 1, callback);
	    			    		executor.execute(d);
							} catch (FileNotFoundException e1) {
								e1.printStackTrace();
							}
    			    	}
    			    	//写BT种子文件
    			    	File btTxt = new File(subDir.getAbsolutePath() + "\\" + folderName + ".txt");
    					try {
							BufferedWriter bw = new BufferedWriter(new FileWriter(btTxt));
							String bt = htmlTool.parseBTSeedUrlFromPost(code);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
    				}
    				 System.out.println("线程池中线程数目："+executor.getPoolSize()+"，队列中等待执行的任务数目："+
    			             executor.getQueue().size()+"，已执行玩别的任务数目："+executor.getCompletedTaskCount());
//    				break;
    			}
    		}
    	}
    	
    	//播放完成提示音
    	try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(new File("D:\\complete.wav"));
			AudioFormat aif = ais.getFormat();
			SourceDataLine sdl = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, aif);
            sdl = (SourceDataLine) AudioSystem.getLine(info);
            sdl.open(aif);
            sdl.start();
            int nByte = 0;
            byte[] buffer = new byte[1024 * 10];
            while (nByte != -1) {
                nByte = ais.read(buffer, 0, 1024 * 10);
                if (nByte >= 0) {
                    sdl.write(buffer, 0, nByte);
                }
            }
            sdl.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} 
    	System.out.println("执行完毕");
    } 
} 