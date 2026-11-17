package co.yixiang.yshop.module.crm.service.crmachievement;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import co.yixiang.yshop.framework.common.enums.CommonStatusEnum;
import co.yixiang.yshop.framework.common.enums.ShopCommonEnum;
import co.yixiang.yshop.framework.common.pojo.PageResult;
import co.yixiang.yshop.framework.common.util.object.BeanUtils;
import co.yixiang.yshop.framework.mybatis.core.query.LambdaQueryWrapperX;
import co.yixiang.yshop.framework.security.core.util.SecurityFrameworkUtils;
import co.yixiang.yshop.module.crm.controller.admin.crmachievement.vo.*;
import co.yixiang.yshop.module.crm.dal.dataobject.crmachievement.CrmAchievementDO;
import co.yixiang.yshop.module.crm.dal.mysql.crmachievement.CrmAchievementMapper;
import co.yixiang.yshop.module.crm.dal.mysql.crmcontract.CrmContractMapper;
import co.yixiang.yshop.module.crm.dal.mysql.crmcontractreceivables.CrmContractReceivablesMapper;
import co.yixiang.yshop.module.crm.enums.ContractStatusEnum;
import co.yixiang.yshop.module.crm.enums.FlowStepEnum;
import co.yixiang.yshop.module.system.api.user.AdminUserApi;
import co.yixiang.yshop.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import co.yixiang.yshop.module.system.controller.admin.user.vo.user.UserPageReqVO;
import co.yixiang.yshop.module.system.dal.dataobject.dept.DeptDO;
import co.yixiang.yshop.module.system.dal.dataobject.user.AdminUserDO;
import co.yixiang.yshop.module.system.dal.mysql.user.AdminUserMapper;
import co.yixiang.yshop.module.system.service.dept.DeptService;
import co.yixiang.yshop.module.system.service.user.AdminUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static co.yixiang.yshop.framework.common.exception.util.ServiceExceptionUtil.exception;
import static co.yixiang.yshop.module.crm.enums.ErrorCodeConstants.ACHIEVEMENT_NOT_EXISTS;

/**
 * 业绩目标 Service 实现类
 *
 * @author yshop
 */
@Service
@Validated
public class CrmAchievementServiceImpl implements CrmAchievementService {

    @Resource
    private CrmAchievementMapper achievementMapper;
    @Resource
    private DeptService deptService;
    @Resource
    private AdminUserService userService;
    @Resource
    private CrmContractMapper contractMapper;
    @Resource
    private CrmContractReceivablesMapper contractReceivablesMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private AdminUserService adminUserService;
    @Resource
    private AdminUserApi adminUserApi;


    @Override
    public void createAchievement(DeptAchieveSaveVO createReqVO) {

        if (createReqVO.getId() == 0) {
            //批量
            if (FlowStepEnum.TYPE_3.getValue().equals(createReqVO.getAchievementRespVO().getType())) {
                List<DeptDO> list = deptService.getDeptList(
                        new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
                list.forEach(dept -> {
                    createReqVO.setId(dept.getId());
                    createReqVO.setName(dept.getName());
                    saveAchievement(createReqVO);
                });
            } else {
                List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
                list.forEach(user -> {
                    createReqVO.setId(user.getId());
                    createReqVO.setName(user.getNickname());
                    saveAchievement(createReqVO);
                });
            }
        } else {
            //单个操作
            if (FlowStepEnum.TYPE_3.getValue().equals(createReqVO.getAchievementRespVO().getType())) {
                createReqVO.setName(createReqVO.getName());
            } else {
                createReqVO.setName(createReqVO.getNickname());
            }
            saveAchievement(createReqVO);
        }


    }

    /**
     * 添加或者更新
     *
     * @param createReqVO
     */
    private void saveAchievement(DeptAchieveSaveVO createReqVO) {
        // 插入
        CrmAchievementDO achievement = BeanUtils.toBean(createReqVO.getAchievementRespVO(), CrmAchievementDO.class);
        achievement.setTypeId(createReqVO.getId());
        achievement.setName(createReqVO.getName());
        LambdaQueryWrapper<CrmAchievementDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmAchievementDO::getType, achievement.getType())
                .eq(CrmAchievementDO::getTypeId, achievement.getTypeId())
                .eq(CrmAchievementDO::getConfig, achievement.getConfig())
                .eq(CrmAchievementDO::getYear, achievement.getYear());
        CrmAchievementDO achievementOld = achievementMapper.selectOne(wrapper);
        if (achievementOld == null) {
            achievementMapper.insert(achievement);
        } else {
            achievement.setId(achievementOld.getId());
            achievementMapper.updateById(achievement);
        }

        //更新年目标
        List<BigDecimal> bigDecimals = Arrays.asList(
                achievement.getJanuary(), achievement.getFebruary(), achievement.getMarch(), achievement.getApril(),
                achievement.getMay(), achievement.getJune(), achievement.getJuly(), achievement.getAugust(),
                achievement.getSeptember(), achievement.getOctober(), achievement.getNovember(), achievement.getDecember()
        );
        BigDecimal yearTarget = bigDecimals.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        CrmAchievementDO achievementDO = CrmAchievementDO.builder().yeartarget(yearTarget).id(achievement.getId()).build();
        achievementMapper.updateById(achievementDO);
    }

    @Override
    public void updateAchievement(CrmAchievementSaveReqVO updateReqVO) {
        // 校验存在
        validateAchievementExists(updateReqVO.getId());
        // 更新
        CrmAchievementDO updateObj = BeanUtils.toBean(updateReqVO, CrmAchievementDO.class);
        achievementMapper.updateById(updateObj);
    }

    @Override
    public void deleteAchievement(Long id) {
        // 校验存在
        validateAchievementExists(id);
        // 删除
        achievementMapper.deleteById(id);
    }

    private void validateAchievementExists(Long id) {
        if (achievementMapper.selectById(id) == null) {
            throw exception(ACHIEVEMENT_NOT_EXISTS);
        }
    }

    @Override
    public CrmAchievementDO getAchievement(Long id) {
        return achievementMapper.selectById(id);
    }

    @Override
    public PageResult<UserAchieveRespVO> getAchievementPage(CrmAchievementPageReqVO pageReqVO) {
        UserPageReqVO userPageReqVO = new UserPageReqVO();
        userPageReqVO.setPageNo(pageReqVO.getPageNo());
        userPageReqVO.setPageSize(pageReqVO.getPageSize());
        PageResult<AdminUserDO> pageResult = userService.getUserPage(userPageReqVO);
        PageResult<UserAchieveRespVO> pageResult1 = BeanUtils.toBean(pageResult, UserAchieveRespVO.class);

        List<Long> typeIds = pageResult1.getList().stream().map(UserAchieveRespVO::getId).collect(Collectors.toList());
        Map<Long, CrmAchievementDO> achievementMap = toAchievementMap(
                achievementMapper.selectListByTypeIds(FlowStepEnum.TYPE_2.getValue(), typeIds,
                        pageReqVO.getConfig(), pageReqVO.getYear()));

        pageResult1.getList().forEach(v -> {
            CrmAchievementDO crmAchievementDO = achievementMap.getOrDefault(v.getId(), emptyAchievement());
            v.setAchievementRespVO(BeanUtils.toBean(crmAchievementDO, CrmAchievementRespVO.class));
        });

        return pageResult1;
    }

    @Override
    public List<UserAchieveDetailRespVO> getAchievementDetailCount(CrmAchievementPageReqVO pageReqVO) {
        Long adminId = SecurityFrameworkUtils.getLoginUserId();
        List<Long> ids = new ArrayList<>();
        ids.add(adminId);
        ids.addAll(adminUserApi.getUserListBySubordinateIds(adminId));
        List<AdminUserDO> adminUserDOList = userService.getUserList(ids);
        List<UserAchieveDetailRespVO> userAchieveRespVOS = BeanUtils.toBean(adminUserDOList, UserAchieveDetailRespVO.class);

        Map<Long, CrmAchievementDO> achievementMap = toAchievementMap(
                achievementMapper.selectListByTypeIds(FlowStepEnum.TYPE_2.getValue(), ids,
                        pageReqVO.getConfig(), pageReqVO.getYear()));

        Map<Long, Map<Integer, BigDecimal>> monthlyMoneyMap = loadMonthlyMoneyMap(ids, pageReqVO.getYear(), pageReqVO.getConfig());

        userAchieveRespVOS.forEach(v -> {
            if (adminId.equals(v.getId())) {
                v.setNickname(v.getNickname() + "(本人)");
            } else {
                v.setNickname(v.getNickname() + "(下属)");
            }
            CrmAchievementDO crmAchievementDO = achievementMap.getOrDefault(v.getId(), emptyAchievement());
            Map<Integer, BigDecimal> userMonthlyMoney = monthlyMoneyMap.getOrDefault(v.getId(), Collections.emptyMap());
            CrmAchievementDetailRespVO achievementDetailRespVO = CrmAchievementDetailRespVO.builder()
                    .january(buildMonthMap(userMonthlyMoney.get(1), crmAchievementDO.getJanuary()))
                    .february(buildMonthMap(userMonthlyMoney.get(2), crmAchievementDO.getFebruary()))
                    .march(buildMonthMap(userMonthlyMoney.get(3), crmAchievementDO.getMarch()))
                    .april(buildMonthMap(userMonthlyMoney.get(4), crmAchievementDO.getApril()))
                    .may(buildMonthMap(userMonthlyMoney.get(5), crmAchievementDO.getMay()))
                    .june(buildMonthMap(userMonthlyMoney.get(6), crmAchievementDO.getJune()))
                    .july(buildMonthMap(userMonthlyMoney.get(7), crmAchievementDO.getJuly()))
                    .august(buildMonthMap(userMonthlyMoney.get(8), crmAchievementDO.getAugust()))
                    .september(buildMonthMap(userMonthlyMoney.get(9), crmAchievementDO.getSeptember()))
                    .october(buildMonthMap(userMonthlyMoney.get(10), crmAchievementDO.getOctober()))
                    .november(buildMonthMap(userMonthlyMoney.get(11), crmAchievementDO.getNovember()))
                    .december(buildMonthMap(userMonthlyMoney.get(12), crmAchievementDO.getDecember()))
                    .build();
            v.setAchievementRespVO(achievementDetailRespVO);
        });

        return userAchieveRespVOS;
    }

    /**
     * 一次查出全年各用户按月完成金额，替代 N×12 次 selectList
     */
    private Map<Long, Map<Integer, BigDecimal>> loadMonthlyMoneyMap(List<Long> userIds, Integer year, Integer config) {
        if (userIds == null || userIds.isEmpty() || year == null) {
            return Collections.emptyMap();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, Calendar.JANUARY, 1);
        Date startTime = DateUtil.beginOfYear(calendar.getTime());
        Date endTime = DateUtil.endOfYear(calendar.getTime());

        List<MonthlyMoneySumDTO> monthlySums;
        if (ShopCommonEnum.ACH_1.getValue().equals(config)) {
            monthlySums = contractMapper.selectMonthlyMoneySum(userIds,
                    ContractStatusEnum.STATUS_2.getValue(), startTime, endTime);
        } else {
            // 明细接口原逻辑：回款按 create_time + IS_FINISH_1 过滤
            monthlySums = contractReceivablesMapper.selectMonthlyMoneySumByCreateTime(userIds,
                    ShopCommonEnum.IS_FINISH_1.getValue(), startTime, endTime);
        }
        return toUserMonthMoneyMap(monthlySums);
    }

    private Map<Long, Map<Integer, BigDecimal>> toUserMonthMoneyMap(List<MonthlyMoneySumDTO> monthlySums) {
        if (monthlySums == null || monthlySums.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Map<Integer, BigDecimal>> result = new HashMap<>();
        for (MonthlyMoneySumDTO dto : monthlySums) {
            if (dto.getOwnerUserId() == null || dto.getMonth() == null) {
                continue;
            }
            BigDecimal money = dto.getMoney() == null ? BigDecimal.ZERO : dto.getMoney();
            result.computeIfAbsent(dto.getOwnerUserId(), k -> new HashMap<>())
                    .put(dto.getMonth(), money);
        }
        return result;
    }

    private Map<String, String> buildMonthMap(BigDecimal finishMoney, BigDecimal target) {
        BigDecimal finish = finishMoney == null ? BigDecimal.ZERO : finishMoney;
        BigDecimal targetMoney = target == null ? BigDecimal.ZERO : target;
        String rate = "0.00%";
        if (targetMoney.compareTo(BigDecimal.ZERO) != 0) {
            rate = new DecimalFormat("0.00%").format(finish.divide(targetMoney, 2, RoundingMode.HALF_UP));
        }
        Map<String, String> map = new HashMap<>();
        map.put("finish", finish.toString());
        map.put("target", targetMoney.toString());
        map.put("rate", rate);
        return map;
    }

    @Override
    public List<DeptAchieveRespVO> getDeptAchieve(Integer type, Integer year) {
        List<DeptDO> list = deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        List<DeptAchieveRespVO> achieveRespVOS = BeanUtils.toBean(list, DeptAchieveRespVO.class);

        List<Long> typeIds = achieveRespVOS.stream().map(DeptAchieveRespVO::getId).collect(Collectors.toList());
        Map<Long, CrmAchievementDO> achievementMap = toAchievementMap(
                achievementMapper.selectListByTypeIds(FlowStepEnum.TYPE_3.getValue(), typeIds, type, year));

        achieveRespVOS.forEach(vo -> {
            CrmAchievementDO crmAchievementDO = achievementMap.getOrDefault(vo.getId(), emptyAchievement());
            vo.setAchievementRespVO(BeanUtils.toBean(crmAchievementDO, CrmAchievementRespVO.class));
        });
        return achieveRespVOS;
    }

    @Override
    public AchieveCountRespVO getCount(Integer type, Integer year, Integer month) {
        List<Long> userIds = new ArrayList<>();
        Long typeId = SecurityFrameworkUtils.getLoginUserId();
        if (FlowStepEnum.TYPE_2.getValue().equals(type)) {
            userIds.add(typeId);
        } else {
            //部门
            AdminUserDO adminUserDO = userMapper.selectById(typeId);
            typeId = adminUserDO.getDeptId();
            Set<Long> deptIds = adminUserService.getDeptCondition(adminUserDO.getDeptId());
            List<AdminUserDO> adminUserDOS = userMapper.selectList(new LambdaQueryWrapperX<AdminUserDO>()
                    .in(AdminUserDO::getDeptId, deptIds));
            userIds = adminUserDOS.stream().map(AdminUserDO::getId).collect(Collectors.toList());
        }
        Date starTime;
        Date endTime;
        Calendar calendar = Calendar.getInstance();

        if (month == 0) {
            //全年
            calendar.set(year, Calendar.JANUARY, 1);
            starTime = DateUtil.beginOfYear(calendar.getTime());
            endTime = DateUtil.endOfYear(calendar.getTime());
        } else {
            //某个月
            calendar.set(year, month - 1, 1);
            starTime = DateUtil.beginOfMonth(calendar.getTime());
            endTime = DateUtil.endOfMonth(calendar.getTime());
        }

        //合同目标 / 回款目标
        CrmAchievementDO crmAchievementDO = achievementMapper.selectOne(new LambdaQueryWrapperX<CrmAchievementDO>()
                .eq(CrmAchievementDO::getType, type)
                .eq(CrmAchievementDO::getTypeId, typeId)
                .eqIfPresent(CrmAchievementDO::getConfig, ShopCommonEnum.ACH_1.getValue())
                .eqIfPresent(CrmAchievementDO::getYear, year));
        CrmAchievementDO crmAchievementDO2 = achievementMapper.selectOne(new LambdaQueryWrapperX<CrmAchievementDO>()
                .eq(CrmAchievementDO::getType, type)
                .eq(CrmAchievementDO::getTypeId, typeId)
                .eqIfPresent(CrmAchievementDO::getConfig, ShopCommonEnum.ACH_2.getValue())
                .eqIfPresent(CrmAchievementDO::getYear, year));

        BigDecimal contractSuccessMoney = sumContractMoney(userIds, starTime, endTime);
        BigDecimal receivablesSuccessMoney = sumReceivablesMoney(userIds, starTime, endTime);

        BigDecimal contractMoney = resolveMonthTarget(crmAchievementDO, month);
        BigDecimal receivablesMoney = resolveMonthTarget(crmAchievementDO2, month);
        String contractPer = "0";
        String receivablesPer = "0";
        if (contractMoney.compareTo(BigDecimal.ZERO) > 0) {
            contractPer = NumberUtil.round(NumberUtil.div(contractSuccessMoney, contractMoney), 2)
                    .multiply(new BigDecimal("100")).toString();
        }
        if (receivablesMoney.compareTo(BigDecimal.ZERO) > 0) {
            receivablesPer = NumberUtil.round(NumberUtil.div(receivablesSuccessMoney, receivablesMoney), 2)
                    .multiply(new BigDecimal("100")).toString();
        }

        return AchieveCountRespVO.builder()
                .contractMoney(contractMoney)
                .contractSuccessMoney(contractSuccessMoney)
                .receivablesMoney(receivablesMoney)
                .receivablesSuccessMoney(receivablesSuccessMoney)
                .contractPer(contractPer)
                .receivablesPer(receivablesPer)
                .build();
    }

    private BigDecimal sumContractMoney(List<Long> userIds, Date startTime, Date endTime) {
        if (userIds == null || userIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = contractMapper.selectMoneySum(userIds,
                ContractStatusEnum.STATUS_2.getValue(), startTime, endTime);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private BigDecimal sumReceivablesMoney(List<Long> userIds, Date startTime, Date endTime) {
        if (userIds == null || userIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 统计接口原逻辑：回款按 return_time + 审核通过
        BigDecimal sum = contractReceivablesMapper.selectMoneySumByReturnTime(userIds,
                ContractStatusEnum.STATUS_2.getValue(), startTime, endTime);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private BigDecimal resolveMonthTarget(CrmAchievementDO achievement, Integer month) {
        if (achievement == null || month == null) {
            return BigDecimal.ZERO;
        }
        return switch (month) {
            case 0 -> defaultZero(achievement.getYeartarget());
            case 1 -> defaultZero(achievement.getJanuary());
            case 2 -> defaultZero(achievement.getFebruary());
            case 3 -> defaultZero(achievement.getMarch());
            case 4 -> defaultZero(achievement.getApril());
            case 5 -> defaultZero(achievement.getMay());
            case 6 -> defaultZero(achievement.getJune());
            case 7 -> defaultZero(achievement.getJuly());
            case 8 -> defaultZero(achievement.getAugust());
            case 9 -> defaultZero(achievement.getSeptember());
            case 10 -> defaultZero(achievement.getOctober());
            case 11 -> defaultZero(achievement.getNovember());
            case 12 -> defaultZero(achievement.getDecember());
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<Long, CrmAchievementDO> toAchievementMap(List<CrmAchievementDO> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyMap();
        }
        return list.stream().collect(Collectors.toMap(CrmAchievementDO::getTypeId, Function.identity(), (a, b) -> a));
    }

    private CrmAchievementDO emptyAchievement() {
        return CrmAchievementDO.builder()
                .january(BigDecimal.ZERO).february(BigDecimal.ZERO).march(BigDecimal.ZERO)
                .april(BigDecimal.ZERO).may(BigDecimal.ZERO).june(BigDecimal.ZERO)
                .july(BigDecimal.ZERO).august(BigDecimal.ZERO).september(BigDecimal.ZERO)
                .october(BigDecimal.ZERO).november(BigDecimal.ZERO).december(BigDecimal.ZERO)
                .yeartarget(BigDecimal.ZERO)
                .build();
    }
}
