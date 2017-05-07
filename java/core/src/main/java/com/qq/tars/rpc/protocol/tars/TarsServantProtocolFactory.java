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

package com.qq.tars.rpc.protocol.tars;

import com.qq.tars.net.protocol.ProtocolDecoder;
import com.qq.tars.net.protocol.ProtocolEncoder;
import com.qq.tars.net.protocol.ProtocolFactory;

// TODO: 17/4/15 by zmyer
public class TarsServantProtocolFactory implements ProtocolFactory {
    //编码器对象
    private TarsCodec codec = null;

    // TODO: 17/4/15 by zmyer
    public TarsServantProtocolFactory(TarsCodec codec) {
        this.codec = codec;
    }

    // TODO: 17/4/15 by zmyer
    public ProtocolEncoder getEncoder() {
        return this.codec;
    }

    // TODO: 17/4/15 by zmyer
    public ProtocolDecoder getDecoder() {
        return this.codec;
    }
}
