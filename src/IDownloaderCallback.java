
/**
 * 负责Downloader线程类出现可捕获的异常时，进行对主线程的回调处理
 */
public interface IDownloaderCallback {
	/**
	 * Downloader出现异常时，在catch块中将对象抛回主线程以备二次添加任务
	 * @param d	发生异常的Downloader对象
	 * @param e	发生的异常对象
	 */
	public void onException(Downloader d, Exception e);
}
