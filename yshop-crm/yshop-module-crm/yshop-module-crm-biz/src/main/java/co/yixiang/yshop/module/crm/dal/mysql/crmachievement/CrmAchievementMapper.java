package co.yixiang.yshop.module.crm.dal.mysql.crmachievement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.module.crm.dal.dataobject.crmachievement.CrmAchievementDO;
import org.apache.ibatis.annotations.Mapper;
import co.yixiang.yshop.module.crm.controller.admin.crmachievement.vo.*;

/**
 * 业绩目标 Mapper
 *
 * @author yshop
 */
@Mapper
public interface CrmAchievementMapper extends BaseMapperX<CrmAchievementDO> {

    default PageResult<CrmAchievementDO> selectPage(CrmAchievementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmAchievementDO>()
                .likeIfPresent(CrmAchievementDO::getName, reqVO.getName())
                .eqIfPresent(CrmAchievementDO::getConfig, reqVO.getConfig())
                .eqIfPresent(CrmAchievementDO::getType, reqVO.getType())
                .eqIfPresent(CrmAchievementDO::getTypeId, reqVO.getTypeId())
                .eqIfPresent(CrmAchievementDO::getYear, reqVO.getYear())
                .eqIfPresent(CrmAchievementDO::getJanuary, reqVO.getJanuary())
                .eqIfPresent(CrmAchievementDO::getFebruary, reqVO.getFebruary())
                .eqIfPresent(CrmAchievementDO::getMarch, reqVO.getMarch())
                .eqIfPresent(CrmAchievementDO::getApril, reqVO.getApril())
                .eqIfPresent(CrmAchievementDO::getMay, reqVO.getMay())
                .eqIfPresent(CrmAchievementDO::getJune, reqVO.getJune())
                .eqIfPresent(CrmAchievementDO::getJuly, reqVO.getJuly())
                .eqIfPresent(CrmAchievementDO::getAugust, reqVO.getAugust())
                .eqIfPresent(CrmAchievementDO::getSeptember, reqVO.getSeptember())
                .eqIfPresent(CrmAchievementDO::getOctober, reqVO.getOctober())
                .eqIfPresent(CrmAchievementDO::getNovember, reqVO.getNovember())
                .eqIfPresent(CrmAchievementDO::getDecember, reqVO.getDecember())
                .eqIfPresent(CrmAchievementDO::getYeartarget, reqVO.getYeartarget())
                .betweenIfPresent(CrmAchievementDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CrmAchievementDO::getId));
    }

    /**
     * 按对象 ID 批量查询业绩目标
     */
    default List<CrmAchievementDO> selectListByTypeIds(Integer type, Collection<Long> typeIds,
                                                       Integer config, Integer year) {
        if (typeIds == null || typeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CrmAchievementDO>()
                .eq(CrmAchievementDO::getType, type)
                .in(CrmAchievementDO::getTypeId, typeIds)
                .eqIfPresent(CrmAchievementDO::getConfig, config)
                .eqIfPresent(CrmAchievementDO::getYear, year));
    }

}