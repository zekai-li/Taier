/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.engine.mapper;

import com.dtstack.engine.domain.EngineJobCache;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * company: www.dtstack.com
 * author: toutian
 * create: 2020/02/12
 */
public interface EngineJobCacheDao {

    int insert(@Param("jobId")String jobId, @Param("engineType") String engineType,
               @Param("computeType") Integer computeType, @Param("stage") int stage,
               @Param("jobInfo")String jobInfo, @Param("nodeAddress") String nodeAddress,
               @Param("jobName") String jobName, @Param("jobPriority") Long jobPriority, @Param("jobResource") String jobResource);

    int delete(@Param("jobId")String jobId);

    EngineJobCache getOne(@Param("jobId")String jobId);

    int updateStage(@Param("jobId") String jobId, @Param("stage") Integer stage,@Param("nodeAddress") String nodeAddress, @Param("jobPriority") Long jobPriority, @Param("waitReason") String waitReason);

    int updateStageBatch(@Param("jobIds") List<String> jobIds, @Param("stage") Integer stage,@Param("nodeAddress") String nodeAddress);

    List<EngineJobCache> listByStage(@Param("startId") Long id, @Param("nodeAddress") String nodeAddress, @Param("stage") Integer stage, @Param("jobResource") String jobResource);

    List<EngineJobCache> getByJobIds(@Param("jobIds") List<String> jobIds);

    List<String> listNames(@Param("jobName") String jobName);

    int countByStage(@Param("jobResource") String jobResource, @Param("stages") List<Integer> stages, @Param("nodeAddress") String nodeAddress);

    Long minPriorityByStage(@Param("jobResource") String jobResource, @Param("stages") List<Integer> stages, @Param("nodeAddress") String nodeAddress);

    List<String> getAllNodeAddress();

    Integer updateNodeAddressFailover(@Param("nodeAddress") String nodeAddress, @Param("jobIds") List<String> ids, @Param("stage") Integer stage);

    List<EngineJobCache> listByFailover(@Param("startId") Long id, @Param("nodeAddress") String nodeAddress, @Param("stage") Integer stage);

    List<String> getJobResources();

    List<Map<String,Object>> groupByJobResource(@Param("nodeAddress") String nodeAddress);

    Long countByJobResource(@Param("jobResource") String jobResource, @Param("stage") Integer stage, @Param("nodeAddress") String nodeAddress);

    List<EngineJobCache> listByJobResource(@Param("jobResource") String jobResource, @Param("stage") Integer stage, @Param("nodeAddress") String nodeAddress, @Param("start") Integer start, @Param("pageSize") Integer pageSize);

    Integer deleteByJobIds(@Param("jobIds") List<String> jobIds);

    Integer updateJobInfo(@Param("jobInfo") String jobInfo, @Param("jobId") String jobId);
}