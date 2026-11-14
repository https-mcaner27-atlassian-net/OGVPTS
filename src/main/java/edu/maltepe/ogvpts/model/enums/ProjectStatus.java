package edu.maltepe.ogvpts.model.enums;

/**
 * Proje ve aşama durumu (projects.status, project_milestones.status).
 * Veritabanında küçük harfle saklanır (ör. baslamadi); dönüşümü ProjectStatusConverter yapar.
 */
public enum ProjectStatus {
    BASLAMADI, DEVAM_EDIYOR, TAMAMLANDI
}
