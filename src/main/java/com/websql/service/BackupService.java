package com.websql.service;

import com.websql.model.AjaxResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface BackupService {

    ResponseEntity dataBackups();

    AjaxResult uploadBackups(MultipartFile file);
}
