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

package com.qq.tars.server.core;

import com.qq.tars.net.core.Processor;
import com.qq.tars.net.core.Request;
import com.qq.tars.net.core.Response;
import com.qq.tars.net.core.Session;
import com.qq.tars.rpc.protocol.tars.TarsServantRequest;

// TODO: 17/4/15 by zmyer
public class ServantProcessor extends Processor {
    //创建处理器对象
    private TarsServantProcessor processor = new TarsServantProcessor();

    // TODO: 17/4/15 by zmyer
    @Override
    public Response process(Request request, Session session) {
        Response response;

        if (request instanceof TarsServantRequest) {
            //开始处理请求对象
            response = processor.process(request, session);
        } else {
            throw new IllegalArgumentException("unknown request type.");
        }
        //返回应答对象
        return response;
    }
}
