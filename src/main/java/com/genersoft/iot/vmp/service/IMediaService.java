package com.genersoft.iot.vmp.service;

import com.genersoft.iot.vmp.common.StreamInfo;
import com.genersoft.iot.vmp.gb28181.bean.GbStream;
import com.genersoft.iot.vmp.gb28181.bean.PlatformGbStream;

/**
 * 媒体信息业务
 */
public interface IMediaService {

    /**
     * 根据应用名和流ID获取播放地址, 通过zlm接口检查是否存在
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStreamWithCheck(String app, String stream);

    /**
     * 根据应用名和流ID获取播放地址, 只是地址拼接
     * @param app
     * @param stream
     * @return
     */
    StreamInfo getStreamInfoByAppAndStream(String app, String stream);

}