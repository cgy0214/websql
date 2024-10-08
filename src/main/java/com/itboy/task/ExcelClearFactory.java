package com.itboy.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.cron.task.Task;
import com.itboy.dao.SysExportLogRepository;
import com.itboy.model.SysExportModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @ClassName : ExcelClearFactory
 * @Description : 历史文件删除
 * @Author 超 boy_0214@sina.com
 * @Date: 2024/10/08 17:12
 */
@Slf4j
@Component
public class ExcelClearFactory implements Task {


    @Resource
    private SysExportLogRepository sysExportLogRepository;


    public void run() {
        execute();
        ScheduleUtils.addTask(20000L, "0 0 1 * * ?", this);
    }

    @Override
    public void execute() {
        try {
            List<SysExportModel> list = sysExportLogRepository.findAll();
            for (SysExportModel model : list) {
                if(ObjectUtil.isNotEmpty(model.getFiles())){
                    FileUtil.del(model.getFiles());
                    sysExportLogRepository.delete(model);
                }
            }
        } catch (Exception e) {
            log.error("Excel Clear error!");
        }
    }


}
