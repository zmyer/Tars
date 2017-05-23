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
import com.qq.tars.client.util.ClientLogger;
import com.qq.tars.common.util.Constants;
import java.math.BigDecimal;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: 17/5/22 by zmyer
public class ServantInvokerAliveStat {
    //标识符
    private final String identity;
    //是否调用成功
    private AtomicBoolean lastCallSucess = new AtomicBoolean();
    //超时时间
    private long timeout_startTime = 0;
    //失败频率
    private long frequenceFailInvoke = 0;
    //失败频率开始时间
    private long frequenceFailInvoke_startTime = 0;
    //超时次数
    private long timeoutCount = 0;
    //失败次数
    private long failedCount = 0;
    //成功次数
    private long succCount = 0;
    //连接是否超时断开
    private boolean netConnectTimeout = false;
    //服务是否存活
    private boolean alive = true;
    //最后一次重试时间
    private long lastRetryTime = 0;

    // TODO: 17/5/22 by zmyer
    ServantInvokerAliveStat(String identity) {
        this.identity = identity;
    }

    // TODO: 17/5/22 by zmyer
    public boolean isAlive() {
        return alive;
    }

    // TODO: 17/5/22 by zmyer
    public synchronized void onCallFinished(int ret, ServantProxyConfig config) {
        if (ret == Constants.INVOKE_STATUS_SUCC) {
            //清空失败频率
            frequenceFailInvoke = 0;
            //清空失败频率开始时间
            frequenceFailInvoke_startTime = 0;
            //设置调用成功标记
            lastCallSucess.set(true);
            netConnectTimeout = false;
            //递增成功次数
            succCount++;
        } else if (ret == Constants.INVOKE_STATUS_TIMEOUT) {
            if (!lastCallSucess.get()) {
                //递增失败次数
                frequenceFailInvoke++;
            } else {
                //设置调用失败标记
                lastCallSucess.set(false);
                //设置调用失败次数
                frequenceFailInvoke = 1;
                //记录失败调用的时间戳
                frequenceFailInvoke_startTime = System.currentTimeMillis();
            }
            //设置超时连接标记
            netConnectTimeout = false;
            //递增超时次数
            timeoutCount++;
        } else if (ret == Constants.INVOKE_STATUS_EXEC) {
            if (!lastCallSucess.get()) {
                //递增失败调用次数
                frequenceFailInvoke++;
            } else {
                //失败调用
                lastCallSucess.set(false);
                //设置失败调用次数
                frequenceFailInvoke = 1;
                //记录失败调用时间戳
                frequenceFailInvoke_startTime = System.currentTimeMillis();
            }
            //设置连接超时标记
            netConnectTimeout = false;
            //递增失败调用次数
            failedCount++;
        } else if (ret == Constants.INVOKE_STATUS_NETCONNECTTIMEOUT) {
            netConnectTimeout = true;
        }

        //如果已经超时,则重置相关参数
        if ((timeout_startTime + config.getCheckInterval()) < System.currentTimeMillis()) {
            timeoutCount = 0;
            failedCount = 0;
            succCount = 0;
            timeout_startTime = System.currentTimeMillis();
        }

        //如果依旧存活
        if (alive) {
            //总的调用次数
            long totalCount = timeoutCount + failedCount + succCount;
            if (timeoutCount >= config.getMinTimeoutInvoke()) {
                double radio = div(timeoutCount, totalCount, 2);
                if (radio > config.getFrequenceFailRadio()) {
                    //设置失活标记
                    alive = false;
                    ClientLogger.getLogger().info(identity + "|alive=false|radio=" + radio + "|" + toString());
                }
            }

            if (alive) {
                if (frequenceFailInvoke >= config.getFrequenceFailInvoke()
                    && (frequenceFailInvoke_startTime + 5000) > System.currentTimeMillis()) {
                    //设置失活标记
                    alive = false;
                    ClientLogger.getLogger().info(identity + "|alive=false|frequenceFailInvoke=" + frequenceFailInvoke + "|" + toString());
                }
            }

            if (alive) {
                if (netConnectTimeout) {
                    //设置失活标记
                    alive = false;
                    ClientLogger.getLogger().info(identity + "|alive=false|netConnectTimeout" + "|" + toString());
                }
            }
        } else {
            if (ret == Constants.INVOKE_STATUS_SUCC) {
                alive = true;
            }
        }
    }

    // TODO: 17/5/22 by zmyer
    public long getLastRetryTime() {
        return lastRetryTime;
    }

    // TODO: 17/5/22 by zmyer
    public void setLastRetryTime(long lastRetryTime) {
        this.lastRetryTime = lastRetryTime;
    }

    // TODO: 17/5/22 by zmyer
    public String toString() {
        return "lastCallSucc:" + lastCallSucess.get() + "|" +
            "timeoutCount:" + timeoutCount + "|" +
            "failedCount:" + failedCount + "|" +
            "succCount:" + succCount + "|" +
            "available:" + alive + "|" +
            "netConnectTimeout:" + netConnectTimeout + "|" +
            "timeout_startTime:" + new Date(timeout_startTime) + "|" +
            "frequenceFailInvoke:" + frequenceFailInvoke + "|" +
            "frequenceFailInvoke_startTime:" + new Date(frequenceFailInvoke_startTime) + "|" +
            "lastRetryTime:" + new Date(lastRetryTime);
    }

    // TODO: 17/5/22 by zmyer
    private double div(double v1, double v2, int scale) {
        if (scale < 0) {
            throw new IllegalArgumentException("The scale must be a positive integer or zero");
        }
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.divide(b2, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
