package edu.auk.admissions.service;

import edu.auk.admissions.domain.BusinessRuleException;
import edu.auk.admissions.domain.Document;
import edu.auk.admissions.domain.DocumentType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Checks uploaded files on the server (FR-03, NFR-06) and keeps them in a private folder
 * outside the web root, so a document is never publicly reachable (NFR-01).
 */
@Service
public class DocumentStorage {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final Path root;

    public DocumentStorage(@Value("${app.storage.dir}") String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create document folder " + root, e);
        }
    }

    /** Throws with a message that says how to fix the file. */
    public void validate(MultipartFile file, DocumentType type) {
        String label = label(type);
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Upload the " + label + ".");
        }
        if (file.getSize() > Document.MAX_SIZE_BYTES) {
            throw new BusinessRuleException("The " + label + " is larger than 15 MB.");
        }
        if (!startsWithPdfMagic(file)) {
            throw new BusinessRuleException("The " + label + " must be a PDF file.");
        }
    }

    public Document store(MultipartFile file, DocumentType type) {
        validate(file, type);
        String storedName = UUID.randomUUID() + ".pdf";
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the " + label(type) + ".", e);
        }
        return new Document(type, cleanFileName(file.getOriginalFilename(), storedName),
                Document.PDF_CONTENT_TYPE, file.getSize(), storedName);
    }

    /** Used by the demo data seeder, which has the PDF in memory. */
    public Document store(DocumentType type, String fileName, byte[] pdf) {
        String storedName = UUID.randomUUID() + ".pdf";
        try {
            Files.write(root.resolve(storedName), pdf);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the " + label(type) + ".", e);
        }
        return new Document(type, fileName, Document.PDF_CONTENT_TYPE, pdf.length, storedName);
    }

    /** Keeps only the last path segment of what the browser sent, e.g. "C:\\scans\\essay.pdf" becomes "essay.pdf". */
    private static String cleanFileName(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String name = original.substring(Math.max(original.lastIndexOf('/'), original.lastIndexOf('\\')) + 1);
        return name.isBlank() ? fallback : name;
    }

    public Path pathOf(Document document) {
        Path path = root.resolve(document.getStoragePath()).normalize();
        if (!path.startsWith(root)) {
            throw new NotFoundException("Document not found.");
        }
        return path;
    }

    public void deleteQuietly(String storagePath) {
        try {
            Files.deleteIfExists(root.resolve(storagePath).normalize());
        } catch (IOException ignored) {
            // A leftover file is harmless; it is never served without a matching Document row.
        }
    }

    private static boolean startsWithPdfMagic(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_MAGIC.length);
            return Arrays.equals(head, PDF_MAGIC);
        } catch (IOException e) {
            return false;
        }
    }

    public static String label(DocumentType type) {
        return switch (type) {
            case TRANSCRIPT -> "academic transcript";
            case PROOF_OF_IDENTITY -> "proof of identity";
            case MOTIVATION_ESSAY -> "motivation essay";
        };
    }
}
