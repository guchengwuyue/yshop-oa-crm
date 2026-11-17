package co.yixiang.yshop.module.crm.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客户团队成员权限
 */
@Getter
@AllArgsConstructor
public enum CustomerMemberPermEnum {

    READ(0, "read"),
    EDIT(1, "edit");

    private final Integer value;
    /**
     * 前端 permission 字段取值
     */
    private final String permission;

}
