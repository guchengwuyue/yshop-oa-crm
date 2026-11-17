package co.yixiang.yshop.module.crm.dal.mysql.crmcontractreceivables;

import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.framework.mybatis.core.mapper.BaseMapperX;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.framework.security.core.util.SecurityFrameworkUtils;
import co.yixiang.yshop.module.crm.controller.admin.crmachievement.vo.MonthlyMoneySumDTO;
import co.yixiang.yshop.module.crm.controller.admin.crmanalysis.vo.ContractVO;
import co.yixiang.yshop.module.crm.controller.admin.crmcontractreceivables.vo.CrmContractReceivablesPageReqVO;
import co.yixiang.yshop.module.crm.controller.admin.crmcontractreceivables.vo.CrmContractReceivablesRespVO;
import co.yixiang.yshop.module.crm.dal.dataobject.crmcontract.CrmContractDO;
import co.yixiang.yshop.module.crm.dal.dataobject.crmcontractreceivables.CrmContractReceivablesDO;
import co.yixiang.yshop.module.crm.dal.dataobject.crmcustomer.CrmCustomerDO;
import co.yixiang.yshop.module.system.dal.dataobject.user.AdminUserDO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 回款 Mapper
 *
 * @author yshop
 */
@Mapper
public interface CrmContractReceivablesMapper extends BaseMapperX<CrmContractReceivablesDO> {

    default PageResult<CrmContractReceivablesRespVO> selectPage2(CrmContractReceivablesPageReqVO reqVO, List<Long> ids) {

        Long loginAdminId = SecurityFrameworkUtils.getLoginUserId();
        return selectJoinPage(reqVO, CrmContractReceivablesRespVO.class, new MPJLambdaWrapper<CrmContractReceivablesDO>()
                .selectAll(CrmContractReceivablesDO.class)
                .selectAs(CrmCustomerDO::getName,CrmContractReceivablesRespVO::getCustomerName)
                .selectAs(CrmContractDO::getName, CrmContractReceivablesRespVO::getContractName)
                .selectAs(CrmContractDO::getNumber, CrmContractReceivablesRespVO::getContractNumber)
                .selectAs(AdminUserDO::getNickname, CrmContractReceivablesRespVO::getCreatorName)
                .leftJoin(CrmCustomerDO.class,CrmCustomerDO::getId,CrmContractReceivablesDO::getCustomerId)
                .leftJoin(CrmContractDO.class, CrmContractDO::getId, CrmContractReceivablesDO::getContractId)
                .leftJoin(AdminUserDO.class, AdminUserDO::getId, CrmContractReceivablesDO::getCreator)
                .likeIfExists(CrmCustomerDO::getName, reqVO.getCustomerName())
                .eqIfExists(CrmContractReceivablesDO::getCustomerId,reqVO.getCustomerId())
                .eqIfExists(CrmContractDO::getName, reqVO.getContractName())
                .eqIfExists(CrmContractReceivablesDO::getCheckStatus, reqVO.getCheckStatus())
                .likeIfExists(CrmContractReceivablesDO::getNumber, reqVO.getNumber())
                .eqIfExists(CrmContractReceivablesDO::getContractId, reqVO.getContractId())
                .in(!ids.isEmpty(), CrmContractReceivablesDO::getOwnerUserId,ids)
                .likeIfExists(AdminUserDO::getNickname, reqVO.getCreatorName())
                .orderByDesc(CrmContractReceivablesDO::getId)
                .apply(reqVO.getIsCheck(), "FIND_IN_SET ('" + loginAdminId + "',t.flow_admin_id)")
        );


    }

    default PageResult<CrmContractReceivablesDO> selectPage(CrmContractReceivablesPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmContractReceivablesDO>()
                .eqIfPresent(CrmContractReceivablesDO::getNumber, reqVO.getNumber())
                .eqIfPresent(CrmContractReceivablesDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(CrmContractReceivablesDO::getContractId, reqVO.getContractId())
                .betweenIfPresent(CrmContractReceivablesDO::getReturnTime, reqVO.getReturnTime())
                .eqIfPresent(CrmContractReceivablesDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(CrmContractReceivablesDO::getMoney, reqVO.getMoney())
                .eqIfPresent(CrmContractReceivablesDO::getCheckStatus, reqVO.getCheckStatus())
                .eqIfPresent(CrmContractReceivablesDO::getFlowId, reqVO.getFlowId())
                .eqIfPresent(CrmContractReceivablesDO::getStepId, reqVO.getStepId())
                .eqIfPresent(CrmContractReceivablesDO::getCheckAdminId, reqVO.getCheckAdminId())
                .eqIfPresent(CrmContractReceivablesDO::getFlowAdminId, reqVO.getFlowAdminId())
                .eqIfPresent(CrmContractReceivablesDO::getRemark, reqVO.getRemark())
                .eqIfPresent(CrmContractReceivablesDO::getOwnerUserId, reqVO.getOwnerUserId())
                .eqIfPresent(CrmContractReceivablesDO::getOrderAdminId, reqVO.getOrderAdminId())
                .eqIfPresent(CrmContractReceivablesDO::getPayStatus, reqVO.getPayStatus())
                .eqIfPresent(CrmContractReceivablesDO::getPayType, reqVO.getPayType())
                .betweenIfPresent(CrmContractReceivablesDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(CrmContractReceivablesDO::getId));
    }

    @Select("select any_value(t.id) as id,COUNT(t.id) AS count,SUM(t.money) AS money,t1.nickname AS nickname  from yshop_crm_contract_receivables  t " +
            "LEFT JOIN system_users t1 ON (t1.id = t.order_admin_id) " +
            "where t.deleted=0 GROUP BY t.order_admin_id ORDER BY any_value(money) DESC limit 10")
    List<ContractVO> selectReceivablesTop();

    /**
     * 按负责人 + 月份汇总回款金额（按创建时间，兼容原业绩明细逻辑）
     */
    @Select("""
            <script>
            SELECT owner_user_id AS ownerUserId, MONTH(create_time) AS `month`, IFNULL(SUM(money), 0) AS money
            FROM yshop_crm_contract_receivables
            WHERE deleted = 0
              AND check_status = #{checkStatus}
              AND create_time BETWEEN #{startTime} AND #{endTime}
              AND owner_user_id IN
              <foreach collection="userIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            GROUP BY owner_user_id, MONTH(create_time)
            </script>
            """)
    List<MonthlyMoneySumDTO> selectMonthlyMoneySumByCreateTime(@Param("userIds") Collection<Long> userIds,
                                                               @Param("checkStatus") Integer checkStatus,
                                                               @Param("startTime") Date startTime,
                                                               @Param("endTime") Date endTime);

    /**
     * 汇总回款金额（按回款日期）
     */
    @Select("""
            <script>
            SELECT IFNULL(SUM(money), 0)
            FROM yshop_crm_contract_receivables
            WHERE deleted = 0
              AND check_status = #{checkStatus}
              AND return_time BETWEEN #{startTime} AND #{endTime}
              AND owner_user_id IN
              <foreach collection="userIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    BigDecimal selectMoneySumByReturnTime(@Param("userIds") Collection<Long> userIds,
                                          @Param("checkStatus") Integer checkStatus,
                                          @Param("startTime") Date startTime,
                                          @Param("endTime") Date endTime);

}