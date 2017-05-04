package com.dtstack.rdos.engine.execution.base;

import com.dtstack.rdos.commom.exception.RdosException;
import com.dtstack.rdos.engine.execution.base.enumeration.EngineType;
import com.dtstack.rdos.engine.execution.base.enumeration.RdosTaskStatus;
import com.dtstack.rdos.engine.execution.base.pojo.JobResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.*;

/**
 * 任务提交执行容器
 * 单独起线程执行
 * Date: 2017/2/21
 * Company: www.dtstack.com
 * @ahthor xuchao
 */

public class JobSubmitExecutor{

    private static final Logger logger = LoggerFactory.getLogger(JobSubmitExecutor.class);

    private static final String Engine_TYPES_KEY = "engineTypes";

    private static final String TYPE_NAME_KEY = "typeName";

    public static final String SLOTS_KEY = "slots";//可以并行提交job的线程数

    private int minPollSize = 10;

    private int maxPoolSize = minPollSize;

    private ExecutorService executor;

    private boolean hasInit = false;

    //为了获取job状态,FIXME 是否有更合适的方式?
    private Map<EngineType, IClient> clientMap = new HashMap<>();

    private List<Map<String, Object>> clientParamsList;
    
	private static String userDir = System.getProperty("user.dir");

    private static JobSubmitExecutor singleton = new JobSubmitExecutor();

    private JobSubmitExecutor(){}

    public void init(Map<String,Object> engineConf) throws Exception{
        if(!hasInit){
            Object slots = engineConf.get(SLOTS_KEY);
            if(slots!=null)this.maxPoolSize = (int) slots;
            clientParamsList = (List<Map<String, Object>>) engineConf.get(Engine_TYPES_KEY);
            executor = new ThreadPoolExecutor(minPollSize, maxPoolSize,
                    0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>());
            initJobClient(clientParamsList);
            hasInit = true;
        }
    }

    private void initJobClient(List<Map<String, Object>> clientParamsList) throws Exception{
        for(Map<String, Object> params : clientParamsList){
            String clientTypeStr = (String) params.get(TYPE_NAME_KEY);
            EngineType engineType = EngineType.getEngineType(clientTypeStr);
            loadComputerPlugin(clientTypeStr);

            IClient client = ClientFactory.getClient(clientTypeStr);
            Properties clusterProp = new Properties();
            clusterProp.putAll(params);
            client.init(clusterProp);
            clientMap.put(engineType, client);
        }
    }
    
    private void loadComputerPlugin(String pluginType) throws Exception{
    	String plugin = String.format("%s/plugin/%s", userDir,pluginType);
		File finput = new File(plugin);
		if(!finput.exists()){
			throw new Exception(String.format("%s direcotry not found",plugin));
		}
		ClientFactory.initPluginClass(pluginType, getClassLoad(finput));
    }
    
    
	private URLClassLoader getClassLoad(File dir) throws MalformedURLException, IOException{
		File[] files = dir.listFiles();
		URL[] urls = new URL[files.length];
		int index = 0;
	    if (files!=null&&files.length>0){
			for(File f:files){
				String jarName = f.getName();
				if(f.isFile()&&jarName.endsWith(".jar")){
					urls[index] = f.toURI().toURL();
					index = index+1;
				}
			}
	    }
    	return new URLClassLoader(urls,this.getClass().getClassLoader());
	}

    public static JobSubmitExecutor getInstance(){
        return singleton;
    }

    public void submitJob(JobClient jobClient) throws Exception{
        executor.submit(new JobSubmitProcessor(clientParamsList,jobClient));
    }

    public RdosTaskStatus getJobStatus(EngineType engineType, String jobId){
        IClient client = clientMap.get(engineType);
        try{
            return client.getJobStatus(jobId);
        }catch (Exception e){
            logger.error("", e);
            return RdosTaskStatus.FAILED;//FIXME 是否应该抛出异常或者提供新的状态
        }
    }

    public JobResult stopJob(EngineType engineType, String jobId){
        IClient client = clientMap.get(engineType);
        return client.cancelJob(jobId);
    }

    public void shutdown(){
        //FIXME 是否需要做同步等processor真正完成
        if(executor!=null)executor.shutdownNow();
    }

    class JobSubmitProcessor implements Runnable{

        private JobClient jobClient;

        private Map<EngineType, IClient> clusterClientMap = new HashMap<>();

        public JobSubmitProcessor(List<Map<String, Object>> clientParamsList,JobClient jobClient) throws Exception{
            this.jobClient = jobClient;
            for(Map<String, Object> clientParams : clientParamsList){
                String clientTypeStr = (String) clientParams.get(TYPE_NAME_KEY);
                if(clientTypeStr == null){
                    logger.error("node.yml of engineTypes setting error, typeName must not be null!!!");
                    throw new RdosException("node.yml of engineTypes setting error, typeName must not be null!!!");
                }

                IClient client = ClientFactory.getClient(clientTypeStr);
                if(client == null){
                    throw new RdosException("not support for client type " + clientTypeStr);
                }
                Properties clusterProp = new Properties();
                clusterProp.putAll(clientParams);
                client.init(clusterProp);

                EngineType engineType = EngineType.getEngineType(clientTypeStr);
                clusterClientMap.put(engineType, client);
            }
        }

        @Override
        public void run(){
            if(jobClient != null){

                IClient clusterClient = clusterClientMap.get(jobClient.getEngineType());
                JobResult jobResult = null;

                if(clusterClient == null){
                    jobResult = JobResult.createErrorResult("job setting client type " +
                            "(" + jobClient.getEngineType()  +") don't found.");
                    listenerJobStatus(jobClient, jobResult);
                    return;
                }
                try{
                	Thread.currentThread().setContextClassLoader(clusterClient.getClass().getClassLoader());
                    jobResult = clusterClient.submitJob(jobClient);
                    logger.info("submit job result is:{}.", jobResult);
                    String jobId = jobResult.getData(JobResult.JOB_ID_KEY);
                    jobClient.setEngineTaskId(jobId);
                }catch (Exception e){//捕获未处理异常,防止跳出执行线程
                    jobResult = JobResult.createErrorResult(e);
                    logger.error("get unexpected exception", e);
                }catch (Error e){
                    jobResult = JobResult.createErrorResult(e);
                    logger.error("get an error, please check program!!!!", e);
                }

                listenerJobStatus(jobClient, jobResult);
            }
        }

        private void listenerJobStatus(JobClient jobClient, JobResult jobResult){
            //FIXME 之后需要对本地异常信息做存储
            jobClient.setJobResult(jobResult);
            JobClient.getQueue().offer(jobClient);//添加触发读取任务状态消息
        }
    }

}