/**
 * 
 */
/**
 * @author Administrator
 *
 */
package com.anke.core;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.anke.util.LoggerUtil;
import com.anke.util.TimerManager;

public class CoreMain {

	/**
	 * @param args
	 * 
	 */
	
	private static final Logger logger = LoggerUtil.getInstance(CoreMain.class);
	private static int ExecuteSynchronization = 0;// 是否立即同步
	private static int synchronizationInterval = 24;// 同步间隔时间
	private static int synchronizationHour = 1;// 同步时间点-小时
	private static int synchronizationMinute = 0;// 同步时间点-分钟
	private static int synchronizationSecond = 0;// 同步时间点-秒

	private static String remoteVersionUrl = null;// 云端版本号URL
	private static String remoteDBUrl = null;// 云端telphone.db数据库URL
	private static String remotefrontendUrl = null;// 云端frontend.jar包URL
	private static String localFilePath = null;// 保存本地文件路径地址
	private static String DBTelphone = null;// 要更新的telphone.db的名称
	// private static String NewDBTelphone = null;// 先下载telphone.db的临时名称
	private static String frontendName = null;// IceCore.jar的包名
	// private static String NewIceCoreName = null;// 先下载IceCore.jar的临时包名
	private static String StopfrontendName = null;// 要执行关闭的程序包名

	private static int loacldb_version = 0;// 本地数据库版本号
	private static int loaclfrontend_version = 0;// 本地frontend_version版本号
	private static int remotedb_version = 0;// 远程数据库版本号
	private static int remotefrontend_version = 0;// 远程frontend_version版本号

	private static int DOWN_Finish = 0;// 下载完成
	private static int DOWN_DB_Finish = 0;// 下载SQLite数据库完成
	private static int DOWN_frontend_Finish = 0;// 下载IceCore完成
	private static int Update_Finish = 0;// 修改完成
	private static String nowdate = null;// 当前时间
	private static int RemoteVersionData = 0;// 读取远程数据是否成功
	private static int First_Up = 0;// 首次启动

	public static void main(String[] args) {

		try {

			// 初始化
			String filePath = new File("").getAbsolutePath() + "/Log/RepeatOpen.log";
			File lockFile = new File(filePath);

			FileOutputStream fos = new FileOutputStream(lockFile, true);

			FileChannel chanel = fos.getChannel();

			FileLock lock = chanel.tryLock();// 锁住RepeatOpen.log
			if (lock == null) {
				System.out.println("A previous instance is already running....");
				System.exit(1);
			}

			GetNowTime();
			//System.out.println(nowdate + " 前置机frontend程序和SQLite数据库同步服务启动成功！");
			logger.debug("前置机frontend程序和SQLite数据库同步服务启动成功！");
			
			getInterval();// 读取本地配置文件
			System.out.println(nowdate + " 保存本地文件路径地址：" + localFilePath);
			// 如果设置为不是立即执行，就执行一次
			if (ExecuteSynchronization == 0) {
				First_Up=1;
				init();
			}
			new TimerManager(new TimerTaskMain(), synchronizationInterval, ExecuteSynchronization, synchronizationHour,
					synchronizationMinute, synchronizationSecond);

		} catch (Exception e) {
			logger.debug("main() " + e.getCause(), e.getCause());
		}
	}

	// Timer方法
	static class TimerTaskMain extends TimerTask {
		@Override
		public void run() {
			//捕获所有的异常，保证定时任务能够继续执行
			try{

				//System.out.println("timer开始执行！");
				logger.debug("timer开始执行！");
				First_Up=0;
				init();
			}catch (Throwable e) {
                // donothing
            }
		}
	}

	private static void init() {
		try {
			readLoaclVersionData();// 读取本地版本信息
			readRemoteVersionData();// 读取远程版本信息
			// System.out.println("本地数据库版本号：" + loacldb_version);
			// System.out.println("远程数据库版本号：" + remotedb_version);

			// 判断数据库文件和程序文件是否存在
			int telphoneDB = 1;
			int frontend = 1;
			File file = new File(localFilePath + "\\" + DBTelphone);
			File frontendfile = new File(localFilePath + "\\" + frontendName);
			if (file.exists()) {
				telphoneDB = 1;
			} else {
				telphoneDB = 0;
			}

			if (frontendfile.exists()) {
				frontend = 1;
			} else {
				frontend = 0;
			}

			// 如果远程数据库版本号大于本地数据库版本号，就开始下载新版本
			if (remotedb_version > loacldb_version || telphoneDB == 0) {
				if(telphoneDB == 0){

					logger.debug("本地没有数据库" + DBTelphone + "文件！开始下载...");
				}
				else{
					logger.debug("云端" + DBTelphone + "发现新版本！开始下载...");
				}
				downloadFile(remoteDBUrl, localFilePath + "\\" + "Newtelphone.db");

				// 下载完成
				if (DOWN_Finish == 1) {
					logger.debug("下载" + DBTelphone + "数据库成功！");
					DOWN_DB_Finish = 1;

				}
			}
			DOWN_Finish = 0;

			// 如果远程程序版本号大于本地程序版本号，就开始下载新版本
			if (remotefrontend_version > loaclfrontend_version || frontend == 0) {
				if(frontend == 0){
					logger.debug("本地没有程序包" + frontendName + "文件！开始下载...");
				}
				else{

					logger.debug("云端" + frontendName + "发现新版本！开始下载...");
				}
				downloadFile(remotefrontendUrl, localFilePath + "\\" + "Newfrontend.jar");

				// 下载完成
				if (DOWN_Finish == 1) {
					logger.debug("下载" + frontendName + "程序成功！");
					DOWN_frontend_Finish = 1;

				}
			}

			if (RemoteVersionData > 0) {

				// 如果数据库下载完成或者程序下载完成
				if (DOWN_DB_Finish == 1 || DOWN_frontend_Finish == 1) {

					StopProcess();// 停止程序--执行批处理程序
					Thread.sleep(2000);// 暂停2秒
					if (DOWN_DB_Finish == 1) {
						ReanmeTo(DBTelphone, "Oldtelphone.db");// 先修改原文件
						ReanmeTo("Newtelphone.db", DBTelphone);// 再修改新下载的文件
						ReanmeTo("Oldtelphone.db", "Oldtelphone.db");// 最后删除Oldtelphone.db

						writeVersionData("db_version", String.valueOf(remotedb_version));
						logger.debug("本地数据库版本号修改为：" + remotedb_version);
						DOWN_DB_Finish = 0;
					}
					if (DOWN_frontend_Finish == 1) {
						ReanmeTo(frontendName, "Oldfrontend.jar");// 先修改原文件

						ReanmeTo("Newfrontend.jar", frontendName);// 再修改新下载的文件
						ReanmeTo("Oldfrontend.jar", "Oldfrontend.jar");// 最后删除Oldfrontend.jar

						writeVersionData("frontend_version", String.valueOf(remotefrontend_version));
						logger.debug("本地" + frontendName + "版本号修改为：" + remotefrontend_version);

						DOWN_frontend_Finish = 0;
					}

					RuntimeProcess();// 打开批处理程序
					
					//GetNowTime();// 获取当前时间
					//System.out.println(nowdate + " 前置机frontend程序本地版本：【" + remotefrontend_version + "】和SQLite数据库的本地版本：【"
					//		+ remotedb_version + "】");
					logger.debug("前置机frontend程序本地版本：【" + remotefrontend_version + "】和SQLite数据库的本地版本：【" + remotedb_version + "】与云端版本相同！不需要更新！");
					
				} else if(DOWN_DB_Finish == 0 && DOWN_frontend_Finish == 0 && First_Up==1) {

					StopProcess();// 停止程序--执行批处理程序
					Thread.sleep(1000);// 暂停1秒
					RuntimeProcess();// 打开批处理程序
					
					//GetNowTime();// 获取当前时间
					//System.out.println(nowdate + " 前置机frontend程序本地版本：【" + loaclfrontend_version + "】和SQLite数据库的本地版本：【"
					//		+ loacldb_version + "】与云端版本相同！不需要更新！");
					logger.debug("前置机frontend程序本地版本：【" + loaclfrontend_version + "】和SQLite数据库的本地版本：【" + loacldb_version + "】与云端版本相同！不需要更新！");
				
				}else {
					//GetNowTime();// 获取当前时间
					//System.out.println(nowdate + " 前置机frontend程序本地版本：【" + loaclfrontend_version + "】和SQLite数据库的本地版本：【" + loacldb_version + "】与云端版本相同！不需要更新！");
					logger.debug("前置机frontend程序本地版本：【" + loaclfrontend_version + "】和SQLite数据库的本地版本：【" + loacldb_version + "】与云端版本相同！不需要更新！");
				}
			}
		} catch (Exception e) {
			logger.debug("run() " + e.getCause(), e.getCause());
		}
	}

	// 获取现在时间
	private static void GetNowTime() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// 设置日期格式
			nowdate = df.format(new Date());
		} catch (Exception e) {
			logger.debug("GetNowTime() " + e.getCause(), e.getCause());
		}
	}

	// 修改包名
	private static void ReanmeTo(String oldName, String newName) throws IOException {

		File file = new File(localFilePath + "\\" + oldName);// 指定目录文件
		// String filename = file.getAbsolutePath();
		// if (filename.indexOf(".") >= 0) {
		// filename = filename.substring(0, filename.lastIndexOf("\\"));
		// }

		// 如果新文件存在，就覆盖原来的。否则修改名称
		File newfile = new File(localFilePath + "\\" + newName);
		if (newfile.exists()) {
			if (oldName == newName && (newName == "Oldtelphone.db" || newName == "Oldfrontend.jar")) {
				newfile.delete();
				Update_Finish = 1;
			}
		} else {
			if (file.exists()) {
				file.renameTo(newfile);
				Update_Finish = 1;
			}
		}

	}

	// 获取本程序启动进程PID
	private static int getPid() {
		// 获取进程的PID
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		String name = runtime.getName(); // format: "pid@hostname"
		try {

			return Integer.parseInt(name.substring(0, name.indexOf('@')));
		} catch (Exception e) {
			return -1;
		}
	}

	// 执行批处理,打开程序
	public static void RuntimeProcess() {
		// 执行批处理文件
		// String strcmd = "cmd /c start " + localFilePath + "\\" +
		// StartIceCore;
		String strcmd = "cmd /c start java -jar " + frontendName + "";

		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			ps = rt.exec(strcmd, null, new File(localFilePath));
			// ps.waitFor();

			//GetNowTime();// 获取当前时间
			//System.out.println(nowdate + " 打开互联急救前置机程序！");
			logger.debug("打开互联急救前置机程序！");

			// br.close();
			// inputStr.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("执行批处理，打开启动" + frontendName + "出错【RuntimeProcess】", e1.getCause());
		} catch (Exception e) {
			logger.error("执行批处理，打开启动" + frontendName + "出错【RuntimeProcess】", e.getCause());
		}
		// ps.destroy();
	}

	// 执行批处理,关闭程序
	public static void StopProcess() throws InterruptedException {
		// 执行批处理文件
		// String strcmd = "cmd /c start " + localFilePath + "\\" + StopIceCore
		// +"";
		String strcmd = "cmd /c java -jar " + StopfrontendName;
		Runtime rt = Runtime.getRuntime();
		Process pss = null;
		try {
			pss = rt.exec(strcmd, null, new File(localFilePath));
			pss.waitFor();

			//GetNowTime();// 获取当前时间
			//System.out.println(nowdate + " 关闭互联急救前置机程序！");
			logger.debug("关闭互联急救前置机程序！");
			

		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("执行批处理，关闭程序出错【StopProcess】", e1.getCause());
		} catch (Exception e) {
			logger.error("执行批处理，关闭程序出错【StopProcess】", e.getCause());
		}
	}

	// 测试--关闭批处理
	public static void KillProcess(int pid) throws InterruptedException {
		// 执行批处理文件
		String strcmd = "taskkill /pid " + pid + " /F";
		// String strcmd = "taskkill /f /t /im cmd.exe";
		// String strcmd = "cmd /k start "+localFilePath +"\\stop.bat";
		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			ps = rt.exec(strcmd);
			// ps = rt.exec(strcmd,null,new File(localFilePath));
			ps.waitFor();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	// 获取本地配置文件
	public static void getInterval() {
		Properties p = new Properties();
		try {
			// 获取绝对路径
			String filePath = new File("").getAbsolutePath() + "/config.properties";
			InputStream fis = new FileInputStream(filePath);
			p.load(fis);
			// ExecuteSynchronization =
			// Integer.parseInt(p.getProperty("ExecuteSynchronization"));//
			// 是否立即同步
			synchronizationInterval = Integer.parseInt(p.getProperty("synchronizationInterval"));// 同步间隔
			synchronizationHour = Integer.parseInt(p.getProperty("synchronizationHour"));// 同步时间点--小时--当不为立即同步启作用
			synchronizationMinute = Integer.parseInt(p.getProperty("synchronizationMinute"));// 同步时间点--分钟--当不为立即同步启作用
			synchronizationSecond = Integer.parseInt(p.getProperty("synchronizationSecond"));// 同步时间点--秒--当不为立即同步启作用
			remoteVersionUrl = p.getProperty("VersionUrl");
			remoteDBUrl = p.getProperty("DBUrl");
			remotefrontendUrl = p.getProperty("frontendUrl");
			localFilePath = new String(p.getProperty("localFilePath").getBytes("ISO-8859-1"), "UTF-8");
			DBTelphone = p.getProperty("DBTelphone");
			// NewDBTelphone = p.getProperty("NewDBTelphone");
			frontendName = new String(p.getProperty("frontendName").getBytes("ISO-8859-1"), "UTF-8");
			// NewIceCoreName = new
			// String(p.getProperty("NewIceCoreName").getBytes("ISO-8859-1"),
			// "UTF-8");
			StopfrontendName = new String(p.getProperty("StopfrontendName").getBytes("ISO-8859-1"), "UTF-8");
			fis.close();
		} catch (FileNotFoundException e) {
			logger.error("获取本地配置文件：config.properties出错", e.getCause());
			// logger.debug(e);
		} catch (IOException e) {
			logger.error("获取本地配置文件：config.properties出错", e.getCause());
			// logger.debug(e);
		} catch (Exception e) {
			logger.error("获取本地配置文件：config.properties出错", e.getCause());
		}
	}

	/**
	 * 下载远程文件并保存到本地
	 * 
	 * @param remoteFilePath
	 *            远程文件路径
	 * @param localFilePath
	 *            本地文件路径
	 */
	public static void downloadFile(String remoteFilePath, String localFilePath) {
		URL urlfile = null;
		HttpURLConnection httpUrl = null;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		File f = new File(localFilePath);
		try {
			urlfile = new URL(remoteFilePath);
			httpUrl = (HttpURLConnection) urlfile.openConnection();
			httpUrl.connect();
			// int length = httpUrl.getContentLength();
			bis = new BufferedInputStream(httpUrl.getInputStream());
			bos = new BufferedOutputStream(new FileOutputStream(f));

			// int count = 0;
			int len;
			byte[] b = new byte[1024];
			while ((len = bis.read(b)) != -1) {
				// int numread = bis.read(b);
				// progress = (int) (((float) count / length) * 100);

				bos.write(b, 0, len);
			}
			if (len <= 0) {
				// 下载完成
				DOWN_Finish = 1;
			}
			bos.flush();
			bis.close();
			httpUrl.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("下载程序出错【downloadFile】", e.getCause());
		} finally {
			try {
				bis.close();
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 读取本地版本信息
	public static void readLoaclVersionData() {

		Properties prop = new Properties();
		try {
			// 获取绝对路径
			String filePath = new File("").getAbsolutePath() + "/version.txt";
			InputStream fis = new FileInputStream(filePath);
			prop.load(fis);
			loacldb_version = Integer.parseInt(prop.getProperty("db_version"));// 本地数据库版本
			loaclfrontend_version = Integer.parseInt(prop.getProperty("frontend_version"));//
			// 一定要在修改值之前关闭fis
			fis.close();
		} catch (IOException e) {
			logger.error("读取本地version.txt配置文件失败！", e.getCause());
		}
	}

	// 读取远程版本信息
	public static void readRemoteVersionData() {

		StringBuilder strBuilder = new StringBuilder();
		try {
			// File file = new File(remoteVersionUrl);
			URL urlfile = new URL(remoteVersionUrl);
			if (urlfile != null) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(urlfile.openStream(), "UTF-8"));
				String line = null;
				while ((line = reader.readLine()) != null) {
					strBuilder.append(line + "\n");
					if (line.indexOf("db_version=") >= 0) {
						remotedb_version = Integer
								.parseInt(line.toString().substring(line.toString().indexOf('=') + 1));// 查找数据库版本号
					}
					if (line.indexOf("frontend_version=") >= 0) {
						remotefrontend_version = Integer
								.parseInt(line.toString().substring(line.toString().indexOf('=') + 1));// 查找程序版本号
					}
				}
				reader.close();

				RemoteVersionData = 1;// 读取成功
				// System.out.println(strBuilder.toString());
			}
		} catch (IOException e) {
			RemoteVersionData = 0;
			logger.error("读取远程version.txt配置文件失败！请检查网络！", e.getCause());
		}
	}

	/**
	 * 修改或添加键值对 如果key存在，修改, 反之，添加。
	 * 
	 * @param filePath
	 *            文件路径，即文件所在包的路径，例如：java/util/config.properties
	 * @param key
	 *            键
	 * @param value
	 *            键对应的值
	 */
	public static void writeVersionData(String key, String value) {

		Properties prop = new Properties();
		// int SQLiteDBVersion = 0;
		try {
			// 获取绝对路径
			String filePath = new File("").getAbsolutePath() + "/version.txt";
			InputStream fis = new FileInputStream(filePath);
			prop.load(fis);
			loacldb_version = Integer.parseInt(prop.getProperty("db_version"));// 数据库版本
			loaclfrontend_version = Integer.parseInt(prop.getProperty("frontend_version"));//
			// 一定要在修改值之前关闭fis
			fis.close();
			OutputStream fos = new FileOutputStream(filePath);
			// key = "db_version";
			if (key == "frontend_version") {
				prop.setProperty("db_version", String.valueOf(loacldb_version));
				prop.setProperty(key, value);
			}
			if (key == "db_version") {
				prop.setProperty(key, value);
				prop.setProperty("frontend_version", String.valueOf(loaclfrontend_version));
			}
			// 保存，并加入注释
			prop.store(fos, "Update '" + key + "' value");
			fos.close();
		} catch (IOException e) {
			logger.error("写入version配置文件失败", e.getCause());
		}
	}
}