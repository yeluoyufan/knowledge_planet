package com.hd.forum.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hd.forum.entity.SysFile;
import com.hd.forum.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传文件清理定时任务。
 *
 * 背景：
 * - 上传图片会在 sys_file 表中记录元信息并维护 ref_count 引用计数
 * - 当 ref_count 长期为 0 时，说明该文件未被任何帖子引用，属于“孤儿文件”
 *
 * 该任务会定期清理孤儿文件，避免磁盘被长期占用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanupTask {

    private final SysFileMapper sysFileMapper;

    @Value("${upload.path:D:/Code/Java/forum/uploads}")
    private String uploadPath;

    /**
     * 每天凌晨 3 点清理无引用的图片
     * 只清理上传超过 24 小时且引用计数为 0 的文件
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupUnusedFiles() {
        log.info("开始清理无引用文件...");
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        
        List<SysFile> unusedFiles = sysFileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getRefCount, 0)
                .lt(SysFile::getCreateTime, threshold));
        
        for (SysFile file : unusedFiles) {
            try {
                // 删除原始文件
                Path path = Paths.get(uploadPath, file.getFilename());
                Files.deleteIfExists(path);
                
                // 删除 WebP 版本 (如果有)
                String filename = file.getFilename();
                if (filename.contains(".")) {
                    String webpName = filename.substring(0, filename.lastIndexOf(".")) + ".webp";
                    Files.deleteIfExists(Paths.get(uploadPath, webpName));
                }
                
                // 从数据库删除记录
                sysFileMapper.deleteById(file.getId());
                log.info("已清理文件: {}", file.getFilename());
            } catch (Exception e) {
                log.error("清理文件失败: {}", file.getFilename(), e);
            }
        }
        
        log.info("无引用文件清理完成，共处理 {} 个文件", unusedFiles.size());
    }
}
