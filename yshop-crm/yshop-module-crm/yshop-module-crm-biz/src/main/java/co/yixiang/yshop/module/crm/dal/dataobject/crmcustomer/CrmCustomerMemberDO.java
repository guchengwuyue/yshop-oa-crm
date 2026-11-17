package co.yixiang.yshop.module.crm.dal.dataobject.crmcustomer;

import co.yixiang.yshop.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 客户团队成员 DO
 *
 * @author yshop
 */
@TableName("yshop_crm_customer_member")
@KeySequence("yshop_crm_customer_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmCustomerMemberDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 客户 ID
     */
    private Long customerId;
    /**
     * 成员用户 ID
     */
    private Long userId;
    /**
     * 权限：0-只读 1-读写
     */
    private Integer perm;

}
