package edu.auk.admissions.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** One uploaded file of an application. The file itself is stored on disk, never in a public folder. */
@Entity
public class Document {

    /** 15 MB, to allow for scanned documents (FR-03). */
    public static final long MAX_SIZE_BYTES = 15L * 1024 * 1024;
    public static final String PDF_CONTENT_TYPE = "application/pdf";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType type;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    private long sizeBytes;

    @Column(nullable = false)
    private String storagePath;

    protected Document() {
    }

    public Document(DocumentType type, String fileName, String contentType, long sizeBytes, String storagePath) {
        this.type = type;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    /** FR-03: a PDF of at most 15 MB. */
    public boolean isValid() {
        return PDF_CONTENT_TYPE.equals(contentType) && sizeBytes > 0 && sizeBytes <= MAX_SIZE_BYTES;
    }

    void attachTo(Application application) {
        this.application = application;
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public DocumentType getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStoragePath() {
        return storagePath;
    }
}
