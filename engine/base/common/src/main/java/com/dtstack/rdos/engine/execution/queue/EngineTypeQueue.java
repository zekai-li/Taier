package com.dtstack.rdos.engine.execution.queue;

import com.dtstack.rdos.common.util.MathUtil;
import com.dtstack.rdos.engine.execution.base.JobClient;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Reason:
 * Date: 2018/1/15
 * Company: www.dtstack.com
 * @author xuchao
 */

public class EngineTypeQueue {

    private static final Logger LOG = LoggerFactory.getLogger(EngineTypeQueue.class);

    private static final String DEFAULT_GROUP_NAME = "default";

    /**
     * TODO 修改成外部可配置
     */
    private static final int MAX_QUEUE_LENGTH = 5;

    private String engineType;

    private Map<String, GroupExeQueue> groupExeQueueMap = Maps.newConcurrentMap();

    /**按时间排序*/
    /**
     * TODO 修改为线程安全的SkipList
     */
    private Set<GroupExeQueue> groupExeQueueSet;

    private Map<String, Integer> groupMaxPriority = Maps.newHashMap();

    public EngineTypeQueue(String engineType) {
        this.engineType = engineType;
        groupExeQueueSet = Sets.newTreeSet((gq1, gq2) -> MathUtil.getIntegerVal(gq1.getMaxTime() - gq2.getMaxTime()));
        GroupExeQueue defaultQueue = new GroupExeQueue(DEFAULT_GROUP_NAME);
        groupExeQueueMap.put(defaultQueue.getGroupName(), defaultQueue);
    }

    public void add(JobClient jobClient) {
        String groupName = jobClient.getGroupName();
        groupName = groupName == null ? DEFAULT_GROUP_NAME : groupName;
        GroupExeQueue exeQueue = groupExeQueueMap.get(groupName);
        if (exeQueue == null) {
            exeQueue = new GroupExeQueue(groupName);
            groupExeQueueMap.put(groupName, exeQueue);
        }

        exeQueue.addJobClient(jobClient);
        //重新更新下队列的排序
        groupExeQueueSet.add(exeQueue);
        groupMaxPriority.put(groupName, exeQueue.getMaxPriority());
    }

    public boolean remove(String groupName, String taskId) {

        groupName = groupName == null ? DEFAULT_GROUP_NAME : groupName;
        GroupExeQueue exeQueue = groupExeQueueMap.get(groupName);
        if (exeQueue == null) {
            return false;
        }

        boolean result = exeQueue.remove(taskId);
        groupMaxPriority.put(groupName, exeQueue.getMaxPriority());
        return result;
    }

    public boolean checkLocalPriorityIsMax(String groupName, String localAddress,
                                           ClusterQueueZKInfo.EngineTypeQueueZKInfo zkInfo) {

        Integer localPriority = groupMaxPriority.get(groupName);
        if (localPriority == null) {
            //不可能发生的
            LOG.error("it is not impossible. groupMaxPriority don't have info of :{}", groupName);
            return true;
        }

        boolean result = true;
        for(Map.Entry<String, ClusterQueueZKInfo.GroupQueueZkInfo> zkInfoEntry : zkInfo.getGroupQueueZkInfoMap().entrySet()){
            String address = zkInfoEntry.getKey();
            ClusterQueueZKInfo.GroupQueueZkInfo groupQueueZkInfo = zkInfoEntry.getValue();

            if (localAddress.equals(address)) {
                continue;
            }

            Map<String, Integer> remoteQueueInfo = groupQueueZkInfo.getPriorityInfo();
            Integer priority = remoteQueueInfo.getOrDefault(groupName, 0);
            if (priority > localPriority) {
                result = false;
                break;
            }
        }

        return result;
    }

    public boolean checkCanAddToWaitQueue(String groupName) {

        groupName = groupName == null ? DEFAULT_GROUP_NAME : groupName;
        GroupExeQueue exeQueue = groupExeQueueMap.get(groupName);

        if (exeQueue == null) {
            return true;
        }

        if (exeQueue.size() >= MAX_QUEUE_LENGTH) {
            return false;
        }

        return true;
    }

    public Set<GroupExeQueue> getGroupExeQueueSet() {
        return groupExeQueueSet;
    }

    public Map<String, GroupExeQueue> getGroupExeQueueMap() {
        return groupExeQueueMap;
    }

    public GroupExeQueue remove(String groupName) {
        GroupExeQueue groupExeQueue = groupExeQueueMap.remove(groupName);
        groupExeQueueSet.remove(groupExeQueue);
        groupMaxPriority.remove(groupName);
        return groupExeQueue;
    }

    public Map<String, Integer> getZkGroupPriorityInfo() {
        return groupMaxPriority;
    }

    public String getEngineType() {
        return engineType;
    }

    @Override
    public String toString() {
        return "EngineTypeQueue{" +
                "engineType='" + engineType + '\'' +
                ", groupExeQueueMap=" + groupExeQueueMap +
                ", groupExeQueueSet=" + groupExeQueueSet +
                ", groupMaxPriority=" + groupMaxPriority +
                '}';
    }
}
