# crashreportlib
Android App 应用崩溃日志收集上传组件

# 使用方法
1、gradle配置

	compile 'com.ideacode:crashreportlib:1.0.0'

2、全局配置

在自定义的Application中，onCreate方法添加  
Constant.IP：日志收集服务器ip，自行定义  
Constant.PORT：日志手机服务器端口，自行定义
	
	CrashHandler.getInstance().init(this, Constant.IP, Constant.PORT);
	
	
3、在代码中 try{}catch(Exception e){} ，在catch块内增加错误收集方法
	CrashFileManager.getInstance().saveCrashInfo(e);
	
	try{
	
	}catch (Exception e) {
            e.printStackTrace();
            CrashFileManager.getInstance().saveCrashInfo(e);
    }
