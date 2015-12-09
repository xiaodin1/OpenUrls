import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.Tag;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeList;


public class HTMLTool {
	
	private static final String ENCODE = "gb2312";
	private String prefix;
	
	/**
	 * 生成Html工具
	 * @param webUrlPrefix 网站前缀。例如：http://cl.totu.me/。后面的链接解析函数只能解出后半段，如htm_data/2/1111/30611.html
	 */
	public HTMLTool(String webUrlPrefix)
	{
		assert(webUrlPrefix != null && webUrlPrefix.startsWith("http") && webUrlPrefix.endsWith("/"));
		this.prefix = webUrlPrefix;
	}
	
	/**
	 * 获取HTML代码（UA设置错误可能导致服务器返回403或者下载代码中途断线）
	 * @param pageURL 链接地址
	 * @param encoding 编码格式（如gb2312等）
	 * @return html源代码
	 */
 	public String getHtmlCodeOfPost(String pageURL) { 
        StringBuilder pageHTML = new StringBuilder(); 
        try { 
            URL url = new URL(pageURL); 
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 "); 
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), ENCODE));
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
	 * 从CAOLIU版块html代码中，把全部帖子的名称和链接地址解析出来（采用名称+Url成对的返回方式）
	 * @param htmlCode 版块贴子列表的html代码
	 * @return 名称+Url成对的结果值。例如：名称1、Url1、名称2、Url2... 这种
	 */
	public List<String> parsePostNamesAndUrlsFromHtmlList(String htmlCode)
	{
		List<String> results = new ArrayList<String>();
		try {
			Parser parser = new Parser(htmlCode);
			parser.setEncoding(ENCODE); 
			TagNameFilter filter = new TagNameFilter("h3");  
			NodeList nodeList = parser.parse(filter);
			if(nodeList != null) {
				for (int i = 0; i < nodeList.size(); i++) {
					//e.g. <h3><a href="htm_data/2/1111/30611.html">Re:新年聚会姐妹们一 ..</a></h3>
					Node node = nodeList.elementAt(i);				//<h3>
					Tag tag = (Tag)node.getFirstChild();			//<a>
					
					String urlSuffix = tag.getAttribute("href");	//htm_data/2/1111/30611.html
					String postName = node.toPlainTextString();		//Re:新年聚会姐妹们一 ..  
					postName = postName.replace("/", "-").replace("\\", "-").replace(":", "-")
							.replace("*", "-").replace("?", "-").replace("\"", "-").replace("<", "-")
							.replace(">", "-").replace("|", "-");	//去掉不能做Windows文件名的字符/\:*?"<>|
					
					if(urlSuffix.startsWith("htm_data")) {			//略去read.php?tid=1315这样的论坛公告
						
						results.add(postName);	
						results.add(prefix + urlSuffix);			//http://cl.totu.me/htm_data/2/1111/30611.html
					}
				}
			}
        }
        catch( Exception e ) {     
            e.printStackTrace();
        }
		return results;
	}
	
	/**
	 * 从楼主的帖子内容中解析出预览图地址
	 * @param htmlCode 帖子地址
	 * @return List<String> 预览图集合
	 */
	public List<String> parseImagesUrlFromPost(String htmlCode)
	{
		List<String> results = new ArrayList<String>();
		try {
			Parser parser = new Parser(htmlCode);
	        parser.setEncoding(ENCODE);
	        //<div class="tpc_content do_not_catch">	各个楼层的标签类名（含楼主）
	        NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter() {
	        	//取tagName是div的，包含class属性的，且class值不为空，等于tpc_content do_not_catch的节点
	            public boolean accept(Node node) {
	                return ((node instanceof Tag)
	                        && !((Tag)node).isEndTag()
	                        && ((Tag)node).getTagName().equals("DIV")	//这块不能写小写的div，否则无法匹配到（囧）
	                        && ((Tag)node).getAttribute("class") != null
	                        && ((Tag)node).getAttribute("class").equals("tpc_content do_not_catch"));
	            }
	        });
	        if(nodeList == null || nodeList.size() == 0) {
	        	System.out.println();
	        	return null;
	        }
	        Node master = nodeList.elementAt(0);	//只要楼主
	        /**
	         * 从楼主信息中取<img src='http://t2.imgchili.net/74202/74202373_1rct00786pl.jpg'
	         * 中间可能有<br>节点，但只要没有<a href='..>节点，就继续取图，否则中断（后面不是本帖图片）
	         */
	        Boolean isBegin = false;	//是否开始取图了（开始后，遇到第一个<a href='..>将中断取图）
	        NodeList nodes = master.getChildren();
	        for(int i = 0; i < nodes.size(); i++) {
	        	Node node = nodes.elementAt(i);
	        	if(node instanceof Tag) {	//略过TextNode（这种无法cast成Tag类型，抛异常）
	        		Tag tag = (Tag)node;
	        		//<img src='http://t2.imgchili.net/74202/74202373_1rct00786pl.jpg'>
	        		if(tag.getTagName().equalsIgnoreCase("IMG") && tag.getAttribute("src") != null) {
		        		isBegin = true;	//取到第一张图后，连续取图开始
		        		results.add(tag.getAttribute("src"));
		        	}
		        	if(isBegin) {
		        		//开始连续取图后，遇到第一个超链接元素，表示后面是其他影片的预览图了，就停止取图
		        		if(tag.getTagName().equalsIgnoreCase("A") && tag.getAttribute("href") != null) {
			        		break;
			        	}
		        	}
	        	}
	        }
        }
        catch( Exception e ) {     
            e.printStackTrace();
        }
		return results;
	}
	
	/**
	 * 从楼主的帖子内容中解析出BT种子页面地址
	 * @param htmlCode htmlCode 楼主贴html代码
	 * @return BT种子页面
	 */
	public String parseBTSeedUrlFromPost(String htmlCode)
	{
		try {
			Parser parser = new Parser(htmlCode);
	        parser.setEncoding(ENCODE);
	        //http://www.rmdown.com/link.php?hash=xxx
	        NodeList nodeList = parser.extractAllNodesThatMatch(new NodeFilter() {
	        	//取tagName是div的，包含class属性的，且class值不为空，等于tpc_content do_not_catch的节点
	            public boolean accept(Node node) {
	                return ((node instanceof TextNode)	
	                        && ((TextNode)node).getText().contains("http://www.rmdown.com"));
	            }
	        });
	        if(nodeList != null && nodeList.size() > 0) {
	        	return ((TextNode)nodeList.elementAt(0)).getText();
	        }
        }
        catch( Exception e ) {     
            e.printStackTrace();
        }
		return null;
	}
}
