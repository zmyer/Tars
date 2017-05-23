/**
 * Tencent is pleased to support the open source community by making Tars available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.qq.tars.client.cluster;

import com.qq.tars.client.ServantProxyConfig;
import com.qq.tars.rpc.common.Url;
import java.util.concurrent.ConcurrentHashMap;

// TODO: 17/5/22 by zmyer
public class ServantnvokerAliveChecker {

    //服务调用对象存活映射表
    private static final ConcurrentHashMap<String, ServantInvokerAliveStat> cache =
        new ConcurrentHashMap<String, ServantInvokerAliveStat>();

    // TODO: 17/5/22 by zmyer
    public static ServantInvokerAliveStat get(Url url) {
        //url标识符
        String identity = url.toIdentityString();
        //获取服务调用对象存活信息
        ServantInvokerAliveStat stat = cache.get(identity);
        if (stat == null) {
            //如果不存在服务存活对象,则直接创建
            cache.putIfAbsent(identity, new ServantInvokerAliveStat(identity));
            //获取存活对象
            stat = cache.get(identity);
        }
        //返回存活对象
        return stat;
    }

    // TODO: 17/5/22 by zmyer
    public static boolean isAlive(Url url, ServantProxyConfig config, int ret) {
        //获取存活对象
        ServantInvokerAliveStat stat = get(url);
        //调用完毕流程
        stat.onCallFinished(ret, config);
        //返回是否存活
        return stat.isAlive();
    }
}
