package com.itboy.service;


import com.itboy.model.AjaxResult;
import com.itboy.model.Result;
import com.itboy.model.TeamResourceModel;
import com.itboy.model.TeamSourceModel;

import java.util.List;
import java.util.Map;

/**
 * @ClassName : TeamSourceService
 * @Description : 团队管理
 * @Author : 超 boy_0214@sina.com
 * @Date: 2024/03/23 20:01
 */
public interface TeamSourceService {

    Boolean updateTeamResources(List<String> teams, List<Long> resourceIds, String type);


    List<Map<String, String>> queryTeamAllBySelect();


    List<TeamResourceModel> queryTeamResourceById(List<Long> resourceIds, String type);

    List<TeamResourceModel> queryTeamResourceByTeamId(List<Long> teamIds, String type);

    AjaxResult deleteTeam(Long id);


    AjaxResult addTeamSource(TeamSourceModel teamSourceModel);

    List<TeamSourceModel> queryValidTeamList();

    List<TeamSourceModel> queryTeamByIds(List<Long> collect);


    Result<TeamSourceModel> selectTeamList(TeamSourceModel teamSourceModel);

    Result<Map<String, Object>> queryTeamResourceList(Long id);

}
