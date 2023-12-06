package com.genersoft.iot.vmp.service.impl;

import com.genersoft.iot.vmp.common.BatchLimit;
import com.genersoft.iot.vmp.common.CommonGbChannel;
import com.genersoft.iot.vmp.conf.exception.ControllerException;
import com.genersoft.iot.vmp.gb28181.bean.*;
import com.genersoft.iot.vmp.gb28181.event.EventPublisher;
import com.genersoft.iot.vmp.gb28181.event.subscribe.catalog.CatalogEvent;
import com.genersoft.iot.vmp.service.IPlatformChannelService;
import com.genersoft.iot.vmp.storager.dao.*;
import com.genersoft.iot.vmp.vmanager.bean.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author lin
 */
@Service
public class PlatformChannelServiceImpl implements IPlatformChannelService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformChannelServiceImpl.class);

    @Autowired
    private PlatformChannelMapper platformChannelMapper;

    @Autowired
    private CommonChannelMapper commonGbChannelMapper;

    @Autowired
    TransactionDefinition transactionDefinition;

    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;

    @Autowired
    private SubscribeHolder subscribeHolder;


    @Autowired
    private DeviceChannelMapper deviceChannelMapper;

    @Autowired
    private PlatformCatalogMapper catalogManager;

    @Autowired
    private ParentPlatformMapper platformMapper;

    @Autowired
    EventPublisher eventPublisher;

    @Override
    @Transactional
    public int addChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds) {
        assert platform != null;
        // 检查通道Id数据是否都是在数据库中存在的数据
        List<Integer> commonGbChannelIdsForSave = commonGbChannelMapper.getChannelIdsByIds(commonGbChannelIds);
        if (commonGbChannelIdsForSave.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "有效待关联通道Id为空");
        }
        // 去除已经关联的部分通道
        List<Integer> commonGbChannelIdsInDb = platformChannelMapper.findChannelsInDb(platform.getId(),
                commonGbChannelIdsForSave);
        if (!commonGbChannelIdsInDb.isEmpty()) {
            commonGbChannelIdsForSave.removeAll(commonGbChannelIdsInDb);
        }
        if (commonGbChannelIdsForSave.isEmpty()) {
            throw new ControllerException(ErrorCode.ERROR100.getCode(), "有效待关联通道Id为空");
        }
        int allCount = 0;
        if (commonGbChannelIdsForSave.size() > BatchLimit.count) {
            for (int i = 0; i < commonGbChannelIdsForSave.size(); i += BatchLimit.count) {
                int toIndex = i + BatchLimit.count;
                if (i + BatchLimit.count > commonGbChannelIdsForSave.size()) {
                    toIndex = commonGbChannelIdsForSave.size();
                }
                int count = platformChannelMapper.addChannels(platform.getId(), commonGbChannelIdsForSave.subList(i, toIndex));
                allCount += count;
                logger.info("[关联通道]国标通道 平台：{}, 共需关联通道数:{}, 已关联：{}", platform.getServerGBId(), commonGbChannelIdsForSave.size(), allCount);
            }
        }else {
            allCount = platformChannelMapper.addChannels(platform.getId(), commonGbChannelIdsForSave);
            logger.info("[关联通道]国标通道 平台：{}, 关联通道数:{}", platform.getServerGBId(), commonGbChannelIdsForSave.size());
        }
        SubscribeInfo catalogSubscribe = subscribeHolder.getCatalogSubscribe(platform.getServerGBId());
        if (catalogSubscribe != null) {
            List<CommonGbChannel> channelList = commonGbChannelMapper.queryInIdList(commonGbChannelIdsForSave);
            if (channelList != null) {
                eventPublisher.catalogEventPublish(platform.getServerGBId(), channelList, CatalogEvent.ADD);
            }
        }
        return allCount;
    }

    @Override
    public int removeChannelForGB(ParentPlatform platform, List<Integer> commonGbChannelIds) {
        return 0;
    }
}
