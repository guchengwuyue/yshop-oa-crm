package co.yixiang.yshop.module.crm.controller.admin.crmachievement.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 按负责人 + 月份汇总金额
 */
@Data
public class MonthlyMoneySumDTO {

    private Long ownerUserId;

    private Integer month;

    private BigDecimal money;

}
