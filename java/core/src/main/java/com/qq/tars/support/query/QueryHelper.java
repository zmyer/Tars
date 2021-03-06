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

package com.qq.tars.support.query;

import com.qq.tars.client.Communicator;
import com.qq.tars.client.ServantProxyConfig;
import com.qq.tars.common.support.Holder;
import com.qq.tars.common.util.Constants;
import com.qq.tars.common.util.StringUtils;
import com.qq.tars.protocol.util.TarsHelper;
import com.qq.tars.support.query.prx.EndpointF;
import com.qq.tars.support.query.prx.QueryFPrx;
import java.util.ArrayList;
import java.util.List;

// TODO: 17/4/18 by zmyer
public final class QueryHelper {

    private final Communicator communicator;

    // TODO: 17/5/22 by zmyer
    public QueryHelper(Communicator communicator) {
        this.communicator = communicator;
    }

    // TODO: 17/5/22 by zmyer
    public List<EndpointF> findObjectById(String objName) {
        return getPrx().findObjectById(objName);
    }

    // TODO: 17/5/22 by zmyer
    private QueryFPrx getPrx() {
        return communicator.stringToProxy(QueryFPrx.class,
            communicator.getCommunicatorConfig().getLocator());
    }

    // TODO: 17/4/18 by zmyer
    public String getServerNodes(ServantProxyConfig config) {
        //创建查询代理对象
        QueryFPrx queryProxy = getPrx();
        String name = config.getSimpleObjectName();
        Holder<List<EndpointF>> activeEp = new Holder<List<EndpointF>>(new ArrayList<EndpointF>());
        Holder<List<EndpointF>> inactiveEp = new Holder<List<EndpointF>>(new ArrayList<EndpointF>());
        int ret;
        if (config.isEnableSet()) {
            ret = queryProxy.findObjectByIdInSameSet(name, config.getSetDivision(), activeEp, inactiveEp);
        } else {
            ret = queryProxy.findObjectByIdInSameGroup(name, activeEp, inactiveEp);
        }

        if (ret != TarsHelper.SERVERSUCCESS) {
            return null;
        }

        StringBuilder value = new StringBuilder();
        if (activeEp.value != null && !activeEp.value.isEmpty()) {
            for (EndpointF endpointF : activeEp.value) {
                if (value.length() > 0) {
                    value.append(":");
                }
                value.append(toFormatString(endpointF, true));
            }
        }
        if (value.length() < 1) {
            return null;
        }
        value.insert(0, Constants.TARS_AT);
        value.insert(0, name);
        return value.toString();
    }

    private String toFormatString(EndpointF endpointF, boolean active) {
        StringBuilder value = new StringBuilder();
        if (!(StringUtils.isEmpty(endpointF.host) || endpointF.port <= 0)) {
            value.append(endpointF.istcp == 0 ? "udp" : "tcp").append(" ");
            value.append("-h").append(" ").append(endpointF.host).append(" ");
            value.append("-p").append(" ").append(endpointF.port).append(" ");
            value.append("-t").append(" 3000 ");
            value.append("-a").append(" ").append(active ? "1" : "0").append(" ");
            value.append("-g").append(" ").append(endpointF.grid);
            if (endpointF.setId != null && endpointF.setId.length() > 0) {
                value.append(" ").append("-s").append(" ").append(endpointF.setId);
            }
        }
        return value.toString();
    }
}
