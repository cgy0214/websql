package com.itboy.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.itboy.dao.DbSourceRepository;
import com.itboy.dao.SysUserRepository;
import com.itboy.dao.TeamResourceRepository;
import com.itboy.dao.TeamSourceRepository;
import com.itboy.model.*;
import com.itboy.service.TeamSourceService;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TeamSourceServiceImpl implements TeamSourceService {


    @Resource
    private TeamResourceRepository teamResourceRepository;


    @Resource
    private TeamSourceRepository teamSourceRepository;


    @Resource
    private SysUserRepository sysUserRepository;

    @Resource
    private DbSourceRepository dbSourceRepository;

    /**
     * 修改团队所属资源
     *
     * @param teams       团队IDS
     * @param resourceIds 资源IDS
     * @param type        资源类型
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTeamResources(List<String> teams, List<Long> resourceIds, String type) {
        if (!teams.isEmpty()) {
            if (!resourceIds.isEmpty()) {
                teamResourceRepository.deleteResourceByUserId(resourceIds, type);
            }
            for (Long resourceId : resourceIds) {
                for (String id : teams) {
                    TeamResourceModel teamResourceModel = new TeamResourceModel();
                    teamResourceModel.setResourceId(resourceId);
                    teamResourceModel.setTeamId(Long.valueOf(id));
                    teamResourceModel.setResourceType(type);
                    teamResourceModel.setCreateTime(DateUtil.date());
                    teamResourceRepository.save(teamResourceModel);
                }
            }
        }
        return true;
    }


    @Override
    public List<Map<String, String>> queryTeamAllBySelect() {
        TeamSourceModel param = new TeamSourceModel();
        param.setState(0);
        List<TeamSourceModel> teamList = teamSourceRepository.findAll(Example.of(param));
        List<Map<String, String>> resultList = new ArrayList<>(teamList.size());
        for (TeamSourceModel teamSourceModel : teamList) {
            Map<String, String> item = new HashMap<>(4);
            item.put("code", teamSourceModel.getId().toString());
            item.put("value", teamSourceModel.getTeamName());
            item.put("select", "false");
            resultList.add(item);
        }
        return resultList;
    }


    @Override
    public List<TeamResourceModel> queryTeamResourceById(List<Long> ids, String type) {
        return teamResourceRepository.queryTeamResourceById(ids, type);
    }
    @Override
    public List<TeamResourceModel> queryTeamResourceByTeamId(List<Long> ids, String type) {
        return teamResourceRepository.queryTeamResourceByTeamId(ids, type);
    }

    @Override
    public AjaxResult deleteTeam(Long id) {
        //TODO 查询是否存在资源，存在不允许删除。
        teamSourceRepository.deleteById(id);
        return AjaxResult.success();
    }


    @Override
    public AjaxResult addTeamSource(TeamSourceModel teamSourceModel) {
        TeamSourceModel param = new TeamSourceModel();
        param.setTeamName(teamSourceModel.getTeamName());
        if (teamSourceRepository.count(Example.of(param)) > 0) {
            return AjaxResult.error("团队名称已经存在,换个名字!");
        }
        teamSourceModel.setCreateTime(DateUtil.date());
        teamSourceRepository.save(teamSourceModel);
        return AjaxResult.success("新增成功!");
    }

    @Override
    public List<TeamSourceModel> queryValidTeamList() {
        TeamSourceModel param = new TeamSourceModel();
        param.setState(0);
        return teamSourceRepository.findAll(Example.of(param));
    }

    @Override
    public List<TeamSourceModel> queryTeamByIds(List<Long> ids) {
        List<TeamSourceModel> list = teamSourceRepository.findAllById(ids);
        return list.stream().filter(s -> s.getState().equals(0)).collect(Collectors.toList());
    }

    @Override
    public Result<TeamSourceModel> selectTeamList(TeamSourceModel model) {
        Result<TeamSourceModel> result = new Result<>();
        Specification<TeamSourceModel> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<Predicate>(1);
            if (ObjectUtil.isNotEmpty(model.getTeamName())) {
                predicates.add(cb.like(root.get("teamName"), "%" + model.getTeamName() + "%"));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
        Page<TeamSourceModel> all = teamSourceRepository.findAll(spec, PageRequest.of(model.getPage() - 1, model.getLimit()));
        if (!all.getContent().isEmpty()) {
            List<SysUser> userList = sysUserRepository.findAllById(all.getContent().stream().map(TeamSourceModel::getUserId).filter(ObjectUtil::isNotNull).collect(Collectors.toSet()));
            Map<Long, String> userMap = userList.stream().collect(Collectors.toMap(SysUser::getUserId, SysUser::getName));
            all.getContent().parallelStream().forEach(s -> {
                s.setStateName(s.getState() == 0 ? "启用" : "禁用");
                s.setUserName(userMap.get(s.getUserId()) == null ? "" : userMap.get(s.getUserId()));
            });
        }
        result.setList(all.getContent());
        result.setCount((int) all.getTotalElements());
        return result;
    }

    @Override
    public Result<Map<String, Object>> queryTeamResourceList(Long id) {
        TeamResourceModel param = new TeamResourceModel();
        param.setTeamId(id);
        List<TeamResourceModel> list = teamResourceRepository.findAll(Example.of(param));
        //团队
        List<TeamSourceModel> teamSourceModels = queryTeamByIds(list.stream().map(TeamResourceModel::getTeamId).collect(Collectors.toList()));
        Map<Long, TeamSourceModel> teamMap = teamSourceModels.stream().collect(Collectors.toMap(TeamSourceModel::getId, s -> s));
        //人员
        List<SysUser> userList = sysUserRepository.findAllById(list.stream().filter(s -> "USER".equals(s.getResourceType())).map(TeamResourceModel::getResourceId).collect(Collectors.toList()));
        Map<Long, SysUser> userMap = userList.stream().collect(Collectors.toMap(SysUser::getUserId, s -> s));
        //数据源
        List<DataSourceModel> dataSourceModels = dbSourceRepository.findAllById(list.stream().filter(s -> "DATASOURCE".equals(s.getResourceType())).map(TeamResourceModel::getResourceId).collect(Collectors.toList()));
        Map<Long, DataSourceModel> dataMap = dataSourceModels.stream().collect(Collectors.toMap(DataSourceModel::getId, s -> s));

        //TODO SQL文本，作业任务
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (TeamResourceModel teamResourceModel : list) {
            TeamSourceModel teamSourceModel = teamMap.get(teamResourceModel.getTeamId());
            Map<String, Object> item = new HashMap<>();
            item.put("teamId", teamResourceModel.getTeamId());
            item.put("teamName", teamSourceModel == null ? "" : teamSourceModel.getTeamName());
            if ("USER".equals(teamResourceModel.getResourceType())) {
                SysUser sysUser = userMap.get(teamResourceModel.getResourceId());
                item.put("resourceId", sysUser == null ? "" : sysUser.getUserId());
                item.put("resourceName", sysUser == null ? "" : "登录账号:"+ sysUser.getUserName());
                item.put("resourceType", "登录用户");
            }
            if ("DATASOURCE".equals(teamResourceModel.getResourceType())) {
                DataSourceModel data = dataMap.get(teamResourceModel.getResourceId());
                item.put("resourceId", data == null ? "" : data.getId());
                item.put("resourceName", data == null ? "" : data.getDbName());
                item.put("resourceType", "数据源");
            }
            resultList.add(item);
        }
        Result<Map<String, Object>> result = new Result<>();
        result.setList(resultList);
        result.setCount(resultList.size());
        return result;
    }
}