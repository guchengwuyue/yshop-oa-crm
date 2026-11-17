package co.yixiang.yshop.module.crm.dal.mysql.crmcustomer;

import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.module.crm.dal.dataobject.crmcustomer.CrmCustomerMemberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 客户团队成员 Mapper
 *
 * @author yshop
 */
@Mapper
public interface CrmCustomerMemberMapper extends BaseMapperX<CrmCustomerMemberDO> {

    default List<CrmCustomerMemberDO> selectListByCustomerIdsAndUserId(Collection<Long> customerIds, Long userId) {
        if (customerIds == null || customerIds.isEmpty() || userId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<CrmCustomerMemberDO>()
                .in(CrmCustomerMemberDO::getCustomerId, customerIds)
                .eq(CrmCustomerMemberDO::getUserId, userId));
    }

    default void deleteByCustomerId(Long customerId) {
        delete(CrmCustomerMemberDO::getCustomerId, customerId);
    }

}
