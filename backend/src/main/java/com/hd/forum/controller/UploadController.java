package com.hd.forum.controller;

import com.hd.forum.common.Result;
import com.hd.forum.entity.SysFile;
import com.hd.forum.mapper.SysFileMapper;
import com.hd.forum.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 文件上传接口（当前主要用于图片上传）。
 *
 * 功能：
 * - 校验上传文件类型必须为 image/*
 * - 生成随机文件名写入到本地 uploads 目录
 * - 将文件元信息写入 sys_file 表，便于后续引用计数与清理
 *
 * 注意：
 * - 图片压缩与 WebP 生成属于“演示/增强体验”功能，生产环境建议使用专门的图片处理服务
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {
    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_IMAGE_EDGE = 8000;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final int RESIZE_EDGE = 1920;
    private static final Map<String, String> EXTENSION_FORMAT_MAP = Map.of(
            ".jpg", "jpg",
            ".jpeg", "jpg",
            ".png", "png",
            ".gif", "gif",
            ".webp", "webp"
    );
    private static final Map<String, String> CONTENT_TYPE_FORMAT_MAP = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp"
    );

    private final SysFileMapper sysFileMapper;

    @Value("${upload.path:D:/Code/Java/forum/uploads}")
    private String uploadPath;

    @Value("${upload.base-url:/uploads}")
    private String baseUrl;

    @Operation(summary = "上传图片")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        Long currentUserId = SecurityUtils.getUserId();
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        long fileSize = file.getSize();
        if (fileSize <= 0) {
            return Result.error("上传文件不能为空");
        }
        if (fileSize > MAX_IMAGE_SIZE) {
            return Result.error("图片大小不能超过10MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        String requestedFormat = resolveRequestedFormat(ext, contentType);
        if (requestedFormat == null) {
            return Result.error("仅支持 JPG、PNG、GIF、WEBP 格式图片");
        }

        try {
            String actualFormat = detectActualFormat(file);
            if (actualFormat == null) {
                return Result.error("图片格式无效或文件已损坏");
            }
            if (!requestedFormat.equals(actualFormat)) {
                return Result.error("文件扩展名、类型与图片实际格式不一致");
            }

            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return Result.error("图片内容无效，无法解析");
            }

            if (isEmptyImage(image)) {
                return Result.error("图片内容为空或全透明，请上传有效图片");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            long pixels = (long) width * height;
            if (width <= 0 || height <= 0) {
                return Result.error("图片尺寸无效");
            }
            if (width > MAX_IMAGE_EDGE || height > MAX_IMAGE_EDGE || pixels > MAX_IMAGE_PIXELS) {
                return Result.error("图片分辨率过大，请压缩后再上传");
            }

            if (ext.isBlank()) {
                ext = "." + actualFormat;
            }
            String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;

            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path targetPath = uploadDir.resolve(newFilename);
            List<Path> createdFiles = new ArrayList<>();

            try {
                if ("gif".equals(actualFormat)) {
                    file.transferTo(targetPath);
                    createdFiles.add(targetPath);
                } else {
                    if (width > RESIZE_EDGE || height > RESIZE_EDGE) {
                        image = resizeImage(image, RESIZE_EDGE, RESIZE_EDGE, "png".equals(actualFormat) || "webp".equals(actualFormat));
                    }

                    if (!ImageIO.write(image, actualFormat, targetPath.toFile())) {
                        throw new IOException("未找到可用的图片写入器: " + actualFormat);
                    }
                    createdFiles.add(targetPath);

                    if (!"webp".equals(actualFormat) && hasImageWriter("webp")) {
                        String webpFilename = newFilename.substring(0, newFilename.lastIndexOf(".")) + ".webp";
                        Path webpPath = uploadDir.resolve(webpFilename);
                        if (!ImageIO.write(image, "webp", webpPath.toFile())) {
                            throw new IOException("未找到可用的 WebP 写入器");
                        }
                        createdFiles.add(webpPath);
                    }
                }

                SysFile sysFile = new SysFile();
                sysFile.setFilename(newFilename);
                sysFile.setOriginalName(originalFilename);
                sysFile.setType(contentType);
                sysFile.setSize(fileSize);
                sysFile.setUserId(currentUserId);
                sysFile.setRefCount(0);
                sysFileMapper.insert(sysFile);
            } catch (Exception ex) {
                cleanupCreatedFiles(createdFiles);
                throw ex;
            }

            String imageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(baseUrl)
                    .path("/")
                    .path(newFilename)
                    .toUriString();
            return Result.success(imageUrl);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败");
        }
    }

    private boolean isEmptyImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) return true;

        // 采样检测：为了性能，不检查所有像素，而是采样检查
        int firstRgb = image.getRGB(0, 0);
        int sampleCount = 0;
        int sameCount = 0;

        // 检查四个角和中心
        int[] checkX = {0, width - 1, 0, width - 1, width / 2};
        int[] checkY = {0, 0, height - 1, height - 1, height / 2};

        for (int i = 0; i < checkX.length; i++) {
            sampleCount++;
            if (image.getRGB(checkX[i], checkY[i]) == firstRgb) {
                sameCount++;
            }
        }

        // 如果采样点全部相同，再进行更密集的随机采样
        if (sameCount == sampleCount) {
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < 50; i++) {
                int x = random.nextInt(width);
                int y = random.nextInt(height);
                sampleCount++;
                if (image.getRGB(x, y) == firstRgb) {
                    sameCount++;
                } else {
                    return false; // 只要有一个像素不同，就不是空图
                }
            }
        }

        return sameCount == sampleCount;
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private String resolveRequestedFormat(String ext, String contentType) {
        String extFormat = EXTENSION_FORMAT_MAP.get(ext);
        String contentTypeFormat = CONTENT_TYPE_FORMAT_MAP.get(contentType);
        if (extFormat != null && contentTypeFormat != null && !extFormat.equals(contentTypeFormat)) {
            return null;
        }
        return Optional.ofNullable(extFormat).orElse(contentTypeFormat);
    }

    private String detectActualFormat(MultipartFile file) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageInputStream == null) {
                return null;
            }
            var readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                return normalizeFormat(reader.getFormatName());
            } finally {
                reader.dispose();
            }
        }
    }

    private String normalizeFormat(String format) {
        if (format == null) {
            return null;
        }
        String normalized = format.trim().toLowerCase(Locale.ROOT);
        if ("jpeg".equals(normalized)) {
            return "jpg";
        }
        return normalized;
    }

    private boolean hasImageWriter(String format) {
        return ImageIO.getImageWritersByFormatName(format).hasNext();
    }

    private void cleanupCreatedFiles(List<Path> createdFiles) {
        for (Path path : createdFiles) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException cleanupEx) {
                log.warn("清理上传残留文件失败: {}", path, cleanupEx);
            }
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth, int maxHeight, boolean keepAlpha) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        int type = keepAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, type);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        graphics2D.dispose();

        return resizedImage;
    }
}
