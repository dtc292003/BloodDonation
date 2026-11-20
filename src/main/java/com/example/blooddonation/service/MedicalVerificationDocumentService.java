
package com.example.blooddonation.service;

import com.example.blooddonation.entity.MedicalUnit;
import com.example.blooddonation.entity.MedicalVerificationDocument;
import com.example.blooddonation.repository.MedicalVerificationDocumentRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicalVerificationDocumentService {

    private final MedicalVerificationDocumentRepository documentRepo;

    private static final long MAX_SIZE = 5L * 1024 * 1024;

    public void save(MedicalUnit unit, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vui lòng tải giấy tờ xác minh"
            );
        }

        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tài liệu không được vượt quá 5 MB"
            );
        }

        byte[] bytes;

        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Không đọc được tài liệu",
                    ex
            );
        }

        String contentType = detectContentType(bytes);

        if (contentType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Chỉ hỗ trợ PDF, JPG hoặc PNG"
            );
        }

        MedicalVerificationDocument document =
                new MedicalVerificationDocument();

        document.setMedicalUnit(unit);
        document.setContentType(contentType);
        document.setSizeBytes((long) bytes.length);
        document.setFileData(bytes);
        document.setUploadedAt(LocalDateTime.now());

        documentRepo.save(document);
    }

    private String detectContentType(byte[] b) {

        // PDF: %PDF-
        if (b.length >= 5 &&
                b[0] == '%' &&
                b[1] == 'P' &&
                b[2] == 'D' &&
                b[3] == 'F' &&
                b[4] == '-') {
            return "application/pdf";
        }

        // JPEG: FF D8 FF
        if (b.length >= 3 &&
                (b[0] & 0xFF) == 0xFF &&
                (b[1] & 0xFF) == 0xD8 &&
                (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // PNG signature
        int[] png = {
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };

        if (b.length >= png.length) {
            boolean valid = true;

            for (int i = 0; i < png.length; i++) {
                if ((b[i] & 0xFF) != png[i]) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                return "image/png";
            }
        }

        return null;
    }
}
