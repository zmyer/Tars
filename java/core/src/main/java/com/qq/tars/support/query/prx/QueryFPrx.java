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
// **********************************************************************
// This file was generated by a TARS parser!
// TARS version 1.0.1.
// **********************************************************************

package com.qq.tars.support.query.prx;

import com.qq.tars.common.support.Holder;
import com.qq.tars.protocol.annotation.Servant;
import com.qq.tars.protocol.tars.annotation.TarsCallback;
import com.qq.tars.protocol.tars.annotation.TarsContext;
import com.qq.tars.protocol.tars.annotation.TarsHolder;

/**
 * 获取对象endpoint的query接口
 */

// TODO: 17/5/22 by zmyer
@Servant
public interface QueryFPrx {
    /**
     * 根据id获取对象
     *
     * @param id 对象名称
     * @return 返回所有该对象的活动endpoint列表
     */
    public java.util.List<EndpointF> findObjectById(String id);

    /**
     * 根据id获取对象
     *
     * @param id 对象名称
     * @return 返回所有该对象的活动endpoint列表
     */
    public java.util.List<EndpointF> findObjectById(String id,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象
     *
     * @param id 对象名称
     * @return 返回所有该对象的活动endpoint列表
     */
    public void async_findObjectById(@TarsCallback QueryFPrxCallback callback, String id);

    /**
     * 根据id获取对象
     *
     * @param id 对象名称
     * @return 返回所有该对象的活动endpoint列表
     */
    public void async_findObjectById(@TarsCallback QueryFPrxCallback callback, String id,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取所有对象,包括活动和非活动对象
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectById4Any(String id, @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp);

    /**
     * 根据id获取所有对象,包括活动和非活动对象
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectById4Any(String id, @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取所有对象,包括活动和非活动对象
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectById4Any(@TarsCallback QueryFPrxCallback callback, String id);

    /**
     * 根据id获取所有对象,包括活动和非活动对象
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectById4Any(@TarsCallback QueryFPrxCallback callback, String id,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象所有endpoint列表,功能同findObjectByIdInSameGroup
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectById4All(String id, @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp);

    /**
     * 根据id获取对象所有endpoint列表,功能同findObjectByIdInSameGroup
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectById4All(String id, @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象所有endpoint列表,功能同findObjectByIdInSameGroup
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectById4All(@TarsCallback QueryFPrxCallback callback, String id);

    /**
     * 根据id获取对象所有endpoint列表,功能同findObjectByIdInSameGroup
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectById4All(@TarsCallback QueryFPrxCallback callback, String id,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameGroup(String id,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameGroup(String id,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameGroup(@TarsCallback QueryFPrxCallback callback,
        String id);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameGroup(@TarsCallback QueryFPrxCallback callback, String id,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象指定归属地的endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameStation(String id, String sStation,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp);

    /**
     * 根据id获取对象指定归属地的endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameStation(String id, String sStation,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象指定归属地的endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameStation(@TarsCallback QueryFPrxCallback callback,
        String id, String sStation);

    /**
     * 根据id获取对象指定归属地的endpoint列表
     *
     * @param id 对象名称
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameStation(@TarsCallback QueryFPrxCallback callback,
        String id, String sStation, @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param setId set全称,格式为setname.setarea.setgroup
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameSet(String id, String setId,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param setId set全称,格式为setname.setarea.setgroup
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public int findObjectByIdInSameSet(String id, String setId,
        @TarsHolder Holder<java.util.List<EndpointF>> activeEp,
        @TarsHolder Holder<java.util.List<EndpointF>> inactiveEp,
        @TarsContext java.util.Map<String, String> ctx);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param setId set全称,格式为setname.setarea.setgroup
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameSet(@TarsCallback QueryFPrxCallback callback, String id,
        String setId);

    /**
     * 根据id获取对象同组endpoint列表
     *
     * @param id 对象名称
     * @param setId set全称,格式为setname.setarea.setgroup
     * @param activeEp 存活endpoint列表
     * @param inactiveEp 非存活endpoint列表
     */
    public void async_findObjectByIdInSameSet(@TarsCallback QueryFPrxCallback callback, String id,
        String setId, @TarsContext java.util.Map<String, String> ctx);
}
