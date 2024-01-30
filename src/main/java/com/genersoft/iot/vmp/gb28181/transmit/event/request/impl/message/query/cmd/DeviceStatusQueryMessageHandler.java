package com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.cmd;

import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.gb28181.bean.Device;
import com.genersoft.iot.vmp.gb28181.bean.ParentPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.cmd.impl.SIPCommanderFroPlatform;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.SIPRequestProcessorParent;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.IMessageHandler;
import com.genersoft.iot.vmp.gb28181.transmit.event.request.impl.message.query.QueryMessageHandler;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import gov.nist.javax.sip.message.SIPRequest;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.header.FromHeader;
import javax.sip.message.Response;
import java.text.ParseException;

import static com.genersoft.iot.vmp.gb28181.utils.XmlUtil.getText;

@Component
public class DeviceStatusQueryMessageHandler extends SIPRequestProcessorParent implements InitializingBean, IMessageHandler {

    private final Logger logger = LoggerFactory.getLogger(DeviceStatusQueryMessageHandler.class);
    private final String cmdType = "DeviceStatus";

    @Autowired
    private QueryMessageHandler queryMessageHandler;

    @Autowired
    private SIPCommanderFroPlatform cmderFroPlatform;

    @Autowired
    private IPlatformChannelService platformChannelService;

    @Override
    public void afterPropertiesSet() throws Exception {
        queryMessageHandler.addHandler(cmdType, this);
    }

    @Override
    public void handForDevice(RequestEvent evt, Device device, Element element) {

    }

    @Override
    public void handForPlatform(RequestEvent evt, ParentPlatform parentPlatform, Element rootElement) {

        logger.info("接收到DeviceStatus查询消息");
        FromHeader fromHeader = (FromHeader) evt.getRequest().getHeader(FromHeader.NAME);
        // 回复200 OK
        try {
            responseAck((SIPRequest) evt.getRequest(), Response.OK);
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 国标级联 DeviceStatus查询回复200OK: {}", e.getMessage());
        }
        String sn = rootElement.element("SN").getText();
        String channelId = getText(rootElement, "DeviceID");
        CommonGbChannel commonGbChannel = platformChannelService.queryChannelByPlatformIdAndChannelDeviceId(parentPlatform.getId(), channelId);
        if (commonGbChannel ==null){
            logger.error("[平台没有该通道的使用权限]:platformId"+parentPlatform.getServerGBId()+"  deviceID:"+channelId);
            return;
        }
        try {
            cmderFroPlatform.deviceStatusResponse(parentPlatform,channelId, sn, fromHeader.getTag(),commonGbChannel.getCommonGbStatus());
        } catch (SipException | InvalidArgumentException | ParseException e) {
            logger.error("[命令发送失败] 国标级联 DeviceStatus查询回复: {}", e.getMessage());
        }
    }
}
