package com.population.resident.service;

import com.population.resident.common.ErrorCode;
import com.population.resident.domain.ResidentJudgeApplication;
import com.population.resident.domain.ResidentJudgeApplicationAttachment;
import com.population.resident.dto.JudgeApplicationAttachmentItem;
import com.population.resident.exception.BizException;
import com.population.resident.mapper.ResidentJudgeApplicationAttachmentMapper;
import com.population.resident.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResidentJudgeApplicationAttachmentService {

    private static final long MAX_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    private final ResidentJudgeApplicationAttachmentMapper residentJudgeApplicationAttachmentMapper;
    private final JudgeApplicationPermissionService permissionService;

    @Value("${app.storage.base-dir:${java.io.tmpdir}/resident-mgmt}")
    private String storageBaseDir;

    @Transactional(rollbackFor = Exception.class)
    public Long uploadAttachment(Long applicationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "附件不能为空");
        }
        if (file.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "附件大小不能超过20MB");
        }
        ResidentJudgeApplication application = permissionService.requireApplicationAndPermission(applicationId, true);
        CurrentUser currentUser = permissionService.requireCurrentUser();

        String originalName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unnamed";
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < originalName.length() - 1) {
            ext = "." + originalName.substring(dotIdx + 1);
        }
        String safeFileName = UUID.randomUUID() + ext;
        String dateDir = LocalDate.now().toString().replace("-", "");
        Path baseDir = Paths.get(storageBaseDir, "judge-applications", dateDir);
        try {
            Files.createDirectories(baseDir);
            Path target = baseDir.resolve(safeFileName).toAbsolutePath().normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            ResidentJudgeApplicationAttachment attachment = new ResidentJudgeApplicationAttachment();
            attachment.setApplicationId(application.getId());
            attachment.setOriginalName(originalName);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setStoragePath(target.toString());
            attachment.setUploaderId(currentUser.getUserId());
            residentJudgeApplicationAttachmentMapper.insert(attachment);
            return attachment.getId();
        } catch (IOException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR.getCode(), "附件保存失败");
        }
    }

    public List<JudgeApplicationAttachmentItem> listAttachments(Long applicationId) {
        ResidentJudgeApplication application = permissionService.requireApplicationAndPermission(applicationId, false);
        return residentJudgeApplicationAttachmentMapper.findByApplicationId(application.getId()).stream()
                .map(item -> JudgeApplicationAttachmentItem.builder()
                        .id(item.getId())
                        .originalName(item.getOriginalName())
                        .contentType(item.getContentType())
                        .fileSize(item.getFileSize())
                        .createdAt(item.getCreatedAt())
                        .build())
                .toList();
    }

    public ResidentJudgeApplicationAttachment attachmentDetail(Long attachmentId) {
        ResidentJudgeApplicationAttachment attachment = residentJudgeApplicationAttachmentMapper.findById(attachmentId);
        if (attachment == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        permissionService.requireApplicationAndPermission(attachment.getApplicationId(), false);
        return attachment;
    }

    public File downloadAttachment(Long attachmentId) {
        ResidentJudgeApplicationAttachment attachment = attachmentDetail(attachmentId);
        File file = new File(attachment.getStoragePath());
        if (!file.exists() || !file.isFile()) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "附件文件不存在");
        }
        return file;
    }
}
