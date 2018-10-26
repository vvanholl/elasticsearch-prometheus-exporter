/*
 * Copyright [2016] [Vincent VAN HOLLEBEKE]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.elasticsearch.action.admin.indices.stats;

import org.elasticsearch.action.support.DefaultShardOperationFailedException;

import java.util.List;

/**
 * Utility methods.
 */
public class PackageAccessHelper {

    /**
     * Shortcut to IndicesStatsResponse constructor which has package access restriction.
     *
     * @param shards            The shards stats.
     * @param totalShards       The total shards this request ran against.
     * @param successfulShards  The successful shards this request was executed on.
     * @param failedShards      The failed shards this request was executed on.
     * @param shardFailures     The list of shard failures exception.
     * @return new instance of IndicesStatsResponse.
     */
    public static IndicesStatsResponse createIndicesStatsResponse(ShardStats[] shards, int totalShards,
                                                                  int successfulShards, int failedShards,
                                                                  List<DefaultShardOperationFailedException> shardFailures) {
        return new IndicesStatsResponse(shards, totalShards, successfulShards, failedShards, shardFailures);
    }
}
