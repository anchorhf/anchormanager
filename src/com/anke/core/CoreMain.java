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
	private static int ExecuteSynchronization = 0;// �Ƿ�����ͬ��
	private static int synchronizationInterval = 24;// ͬ�����ʱ��
	private static int synchronizationHour = 1;// ͬ��ʱ���-Сʱ
	private static int synchronizationMinute = 0;// ͬ��ʱ���-����
	private static int synchronizationSecond = 0;// ͬ��ʱ���-��

	private static String remoteVersionUrl = null;// �ƶ˰汾��URL
	private static String remoteDBUrl = null;// �ƶ�telphone.db���ݿ�URL
	private static String remotefrontendUrl = null;// �ƶ�frontend.jar��URL
	private static String localFilePath = null;// ���汾���ļ�·����ַ
	private static String DBTelphone = null;// Ҫ���µ�telphone.db������
	// private static String NewDBTelphone = null;// ������telphone.db����ʱ����
	private static String frontendName = null;// IceCore.jar�İ���
	// private static String NewIceCoreName = null;// ������IceCore.jar����ʱ����
	private static String StopfrontendName = null;// Ҫִ�йرյĳ������

	private static int loacldb_version = 0;// �������ݿ�汾��
	private static int loaclfrontend_version = 0;// ����frontend_version�汾��
	private static int remotedb_version = 0;// Զ�����ݿ�汾��
	private static int remotefrontend_version = 0;// Զ��frontend_version�汾��

	private static int DOWN_Finish = 0;// �������
	private static int DOWN_DB_Finish = 0;// ����SQLite���ݿ����
	private static int DOWN_frontend_Finish = 0;// ����IceCore���
	private static int Update_Finish = 0;// �޸����
	private static String nowdate = null;// ��ǰʱ��
	private static int RemoteVersionData = 0;// ��ȡԶ�������Ƿ�ɹ�
	private static int First_Up = 0;// �״�����

	public static void main(String[] args) {

		try {

			// ��ʼ��
			String filePath = new File("").getAbsolutePath() + "/Log/RepeatOpen.log";
			File lockFile = new File(filePath);

			FileOutputStream fos = new FileOutputStream(lockFile, true);

			FileChannel chanel = fos.getChannel();

			FileLock lock = chanel.tryLock();// ��סRepeatOpen.log
			if (lock == null) {
				System.out.println("A previous instance is already running....");
				System.exit(1);
			}

			GetNowTime();
			//System.out.println(nowdate + " ǰ�û�frontend�����SQLite���ݿ�ͬ�����������ɹ���");
			logger.debug("ǰ�û�frontend�����SQLite���ݿ�ͬ�����������ɹ���");
			
			getInterval();// ��ȡ���������ļ�
			System.out.println(nowdate + " ���汾���ļ�·����ַ��" + localFilePath);
			// �������Ϊ��������ִ�У���ִ��һ��
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

	// Timer����
	static class TimerTaskMain extends TimerTask {
		@Override
		public void run() {
			//�������е��쳣����֤��ʱ�����ܹ�����ִ��
			try{

				//System.out.println("timer��ʼִ�У�");
				logger.debug("timer��ʼִ�У�");
				First_Up=0;
				init();
			}catch (Throwable e) {
                // donothing
            }
		}
	}

	private static void init() {
		try {
			readLoaclVersionData();// ��ȡ���ذ汾��Ϣ
			readRemoteVersionData();// ��ȡԶ�̰汾��Ϣ
			// System.out.println("�������ݿ�汾�ţ�" + loacldb_version);
			// System.out.println("Զ�����ݿ�汾�ţ�" + remotedb_version);

			// �ж����ݿ��ļ��ͳ����ļ��Ƿ����
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

			// ���Զ�����ݿ�汾�Ŵ��ڱ������ݿ�汾�ţ��Ϳ�ʼ�����°汾
			if (remotedb_version > loacldb_version || telphoneDB == 0) {
				if(telphoneDB == 0){

					logger.debug("����û�����ݿ�" + DBTelphone + "�ļ�����ʼ����...");
				}
				else{
					logger.debug("�ƶ�" + DBTelphone + "�����°汾����ʼ����...");
				}
				downloadFile(remoteDBUrl, localFilePath + "\\" + "Newtelphone.db");

				// �������
				if (DOWN_Finish == 1) {
					logger.debug("����" + DBTelphone + "���ݿ�ɹ���");
					DOWN_DB_Finish = 1;

				}
			}
			DOWN_Finish = 0;

			// ���Զ�̳���汾�Ŵ��ڱ��س���汾�ţ��Ϳ�ʼ�����°汾
			if (remotefrontend_version > loaclfrontend_version || frontend == 0) {
				if(frontend == 0){
					logger.debug("����û�г����" + frontendName + "�ļ�����ʼ����...");
				}
				else{

					logger.debug("�ƶ�" + frontendName + "�����°汾����ʼ����...");
				}
				downloadFile(remotefrontendUrl, localFilePath + "\\" + "Newfrontend.jar");

				// �������
				if (DOWN_Finish == 1) {
					logger.debug("����" + frontendName + "����ɹ���");
					DOWN_frontend_Finish = 1;

				}
			}

			if (RemoteVersionData > 0) {

				// ������ݿ�������ɻ��߳����������
				if (DOWN_DB_Finish == 1 || DOWN_frontend_Finish == 1) {

					StopProcess();// ֹͣ����--ִ�����������
					Thread.sleep(2000);// ��ͣ2��
					if (DOWN_DB_Finish == 1) {
						ReanmeTo(DBTelphone, "Oldtelphone.db");// ���޸�ԭ�ļ�
						ReanmeTo("Newtelphone.db", DBTelphone);// ���޸������ص��ļ�
						ReanmeTo("Oldtelphone.db", "Oldtelphone.db");// ���ɾ��Oldtelphone.db

						writeVersionData("db_version", String.valueOf(remotedb_version));
						logger.debug("�������ݿ�汾���޸�Ϊ��" + remotedb_version);
						DOWN_DB_Finish = 0;
					}
					if (DOWN_frontend_Finish == 1) {
						ReanmeTo(frontendName, "Oldfrontend.jar");// ���޸�ԭ�ļ�

						ReanmeTo("Newfrontend.jar", frontendName);// ���޸������ص��ļ�
						ReanmeTo("Oldfrontend.jar", "Oldfrontend.jar");// ���ɾ��Oldfrontend.jar

						writeVersionData("frontend_version", String.valueOf(remotefrontend_version));
						logger.debug("����" + frontendName + "�汾���޸�Ϊ��" + remotefrontend_version);

						DOWN_frontend_Finish = 0;
					}

					RuntimeProcess();// �����������
					
					//GetNowTime();// ��ȡ��ǰʱ��
					//System.out.println(nowdate + " ǰ�û�frontend���򱾵ذ汾����" + remotefrontend_version + "����SQLite���ݿ�ı��ذ汾����"
					//		+ remotedb_version + "��");
					logger.debug("ǰ�û�frontend���򱾵ذ汾����" + remotefrontend_version + "����SQLite���ݿ�ı��ذ汾����" + remotedb_version + "�����ƶ˰汾��ͬ������Ҫ���£�");
					
				} else if(DOWN_DB_Finish == 0 && DOWN_frontend_Finish == 0 && First_Up==1) {

					StopProcess();// ֹͣ����--ִ�����������
					Thread.sleep(1000);// ��ͣ1��
					RuntimeProcess();// �����������
					
					//GetNowTime();// ��ȡ��ǰʱ��
					//System.out.println(nowdate + " ǰ�û�frontend���򱾵ذ汾����" + loaclfrontend_version + "����SQLite���ݿ�ı��ذ汾����"
					//		+ loacldb_version + "�����ƶ˰汾��ͬ������Ҫ���£�");
					logger.debug("ǰ�û�frontend���򱾵ذ汾����" + loaclfrontend_version + "����SQLite���ݿ�ı��ذ汾����" + loacldb_version + "�����ƶ˰汾��ͬ������Ҫ���£�");
				
				}else {
					//GetNowTime();// ��ȡ��ǰʱ��
					//System.out.println(nowdate + " ǰ�û�frontend���򱾵ذ汾����" + loaclfrontend_version + "����SQLite���ݿ�ı��ذ汾����" + loacldb_version + "�����ƶ˰汾��ͬ������Ҫ���£�");
					logger.debug("ǰ�û�frontend���򱾵ذ汾����" + loaclfrontend_version + "����SQLite���ݿ�ı��ذ汾����" + loacldb_version + "�����ƶ˰汾��ͬ������Ҫ���£�");
				}
			}
		} catch (Exception e) {
			logger.debug("run() " + e.getCause(), e.getCause());
		}
	}

	// ��ȡ����ʱ��
	private static void GetNowTime() {
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");// �������ڸ�ʽ
			nowdate = df.format(new Date());
		} catch (Exception e) {
			logger.debug("GetNowTime() " + e.getCause(), e.getCause());
		}
	}

	// �޸İ���
	private static void ReanmeTo(String oldName, String newName) throws IOException {

		File file = new File(localFilePath + "\\" + oldName);// ָ��Ŀ¼�ļ�
		// String filename = file.getAbsolutePath();
		// if (filename.indexOf(".") >= 0) {
		// filename = filename.substring(0, filename.lastIndexOf("\\"));
		// }

		// ������ļ����ڣ��͸���ԭ���ġ������޸�����
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

	// ��ȡ��������������PID
	private static int getPid() {
		// ��ȡ���̵�PID
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		String name = runtime.getName(); // format: "pid@hostname"
		try {

			return Integer.parseInt(name.substring(0, name.indexOf('@')));
		} catch (Exception e) {
			return -1;
		}
	}

	// ִ��������,�򿪳���
	public static void RuntimeProcess() {
		// ִ���������ļ�
		// String strcmd = "cmd /c start " + localFilePath + "\\" +
		// StartIceCore;
		String strcmd = "cmd /c start java -jar " + frontendName + "";

		Runtime rt = Runtime.getRuntime();
		Process ps = null;
		try {
			ps = rt.exec(strcmd, null, new File(localFilePath));
			// ps.waitFor();

			//GetNowTime();// ��ȡ��ǰʱ��
			//System.out.println(nowdate + " �򿪻�������ǰ�û�����");
			logger.debug("�򿪻�������ǰ�û�����");

			// br.close();
			// inputStr.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("ִ��������������" + frontendName + "����RuntimeProcess��", e1.getCause());
		} catch (Exception e) {
			logger.error("ִ��������������" + frontendName + "����RuntimeProcess��", e.getCause());
		}
		// ps.destroy();
	}

	// ִ��������,�رճ���
	public static void StopProcess() throws InterruptedException {
		// ִ���������ļ�
		// String strcmd = "cmd /c start " + localFilePath + "\\" + StopIceCore
		// +"";
		String strcmd = "cmd /c java -jar " + StopfrontendName;
		Runtime rt = Runtime.getRuntime();
		Process pss = null;
		try {
			pss = rt.exec(strcmd, null, new File(localFilePath));
			pss.waitFor();

			//GetNowTime();// ��ȡ��ǰʱ��
			//System.out.println(nowdate + " �رջ�������ǰ�û�����");
			logger.debug("�رջ�������ǰ�û�����");
			

		} catch (IOException e1) {
			e1.printStackTrace();
			logger.error("ִ���������رճ������StopProcess��", e1.getCause());
		} catch (Exception e) {
			logger.error("ִ���������رճ������StopProcess��", e.getCause());
		}
	}

	// ����--�ر�������
	public static void KillProcess(int pid) throws InterruptedException {
		// ִ���������ļ�
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

	// ��ȡ���������ļ�
	public static void getInterval() {
		Properties p = new Properties();
		try {
			// ��ȡ����·��
			String filePath = new File("").getAbsolutePath() + "/config.properties";
			InputStream fis = new FileInputStream(filePath);
			p.load(fis);
			// ExecuteSynchronization =
			// Integer.parseInt(p.getProperty("ExecuteSynchronization"));//
			// �Ƿ�����ͬ��
			synchronizationInterval = Integer.parseInt(p.getProperty("synchronizationInterval"));// ͬ�����
			synchronizationHour = Integer.parseInt(p.getProperty("synchronizationHour"));// ͬ��ʱ���--Сʱ--����Ϊ����ͬ��������
			synchronizationMinute = Integer.parseInt(p.getProperty("synchronizationMinute"));// ͬ��ʱ���--����--����Ϊ����ͬ��������
			synchronizationSecond = Integer.parseInt(p.getProperty("synchronizationSecond"));// ͬ��ʱ���--��--����Ϊ����ͬ��������
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
			logger.error("��ȡ���������ļ���config.properties����", e.getCause());
			// logger.debug(e);
		} catch (IOException e) {
			logger.error("��ȡ���������ļ���config.properties����", e.getCause());
			// logger.debug(e);
		} catch (Exception e) {
			logger.error("��ȡ���������ļ���config.properties����", e.getCause());
		}
	}

	/**
	 * ����Զ���ļ������浽����
	 * 
	 * @param remoteFilePath
	 *            Զ���ļ�·��
	 * @param localFilePath
	 *            �����ļ�·��
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
				// �������
				DOWN_Finish = 1;
			}
			bos.flush();
			bis.close();
			httpUrl.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("���س������downloadFile��", e.getCause());
		} finally {
			try {
				bis.close();
				bos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// ��ȡ���ذ汾��Ϣ
	public static void readLoaclVersionData() {

		Properties prop = new Properties();
		try {
			// ��ȡ����·��
			String filePath = new File("").getAbsolutePath() + "/version.txt";
			InputStream fis = new FileInputStream(filePath);
			prop.load(fis);
			loacldb_version = Integer.parseInt(prop.getProperty("db_version"));// �������ݿ�汾
			loaclfrontend_version = Integer.parseInt(prop.getProperty("frontend_version"));//
			// һ��Ҫ���޸�ֵ֮ǰ�ر�fis
			fis.close();
		} catch (IOException e) {
			logger.error("��ȡ����version.txt�����ļ�ʧ�ܣ�", e.getCause());
		}
	}

	// ��ȡԶ�̰汾��Ϣ
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
								.parseInt(line.toString().substring(line.toString().indexOf('=') + 1));// �������ݿ�汾��
					}
					if (line.indexOf("frontend_version=") >= 0) {
						remotefrontend_version = Integer
								.parseInt(line.toString().substring(line.toString().indexOf('=') + 1));// ���ҳ���汾��
					}
				}
				reader.close();

				RemoteVersionData = 1;// ��ȡ�ɹ�
				// System.out.println(strBuilder.toString());
			}
		} catch (IOException e) {
			RemoteVersionData = 0;
			logger.error("��ȡԶ��version.txt�����ļ�ʧ�ܣ��������磡", e.getCause());
		}
	}

	/**
	 * �޸Ļ���Ӽ�ֵ�� ���key���ڣ��޸�, ��֮����ӡ�
	 * 
	 * @param filePath
	 *            �ļ�·�������ļ����ڰ���·�������磺java/util/config.properties
	 * @param key
	 *            ��
	 * @param value
	 *            ����Ӧ��ֵ
	 */
	public static void writeVersionData(String key, String value) {

		Properties prop = new Properties();
		// int SQLiteDBVersion = 0;
		try {
			// ��ȡ����·��
			String filePath = new File("").getAbsolutePath() + "/version.txt";
			InputStream fis = new FileInputStream(filePath);
			prop.load(fis);
			loacldb_version = Integer.parseInt(prop.getProperty("db_version"));// ���ݿ�汾
			loaclfrontend_version = Integer.parseInt(prop.getProperty("frontend_version"));//
			// һ��Ҫ���޸�ֵ֮ǰ�ر�fis
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
			// ���棬������ע��
			prop.store(fos, "Update '" + key + "' value");
			fos.close();
		} catch (IOException e) {
			logger.error("д��version�����ļ�ʧ��", e.getCause());
		}
	}
}