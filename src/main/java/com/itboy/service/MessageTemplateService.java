package com.itboy.service;

import com.itboy.model.AjaxResult;
import com.itboy.model.Result;
import com.itboy.model.SysMessageTemplateModel;

import java.util.List;
import java.util.Map;

public interface MessageTemplateService {

    Result<SysMessageTemplateModel> list(SysMessageTemplateModel model);

    AjaxResult addMessageTemplate(SysMessageTemplateModel sysMessageTemplateModel);

    AjaxResult deleteMessageTemplate(Long id);

    AjaxResult testMessageTemplate(SysMessageTemplateModel model);

    boolean sendMessage(Long id, Map<String, Object> params);

    boolean sendMessage(SysMessageTemplateModel model, Map<String, Object> params);

    List<Map<String, String>> findMessageTemplateList();

}