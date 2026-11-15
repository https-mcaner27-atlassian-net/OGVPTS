package edu.maltepe.ogvpts.model.enums;

/**
 * Görev durumu (tasks.status).
 * Veritabanında küçük harfle saklanır (ör. atandi); dönüşümü TaskStatusConverter yapar.
 */
public enum TaskStatus {
    ATANDI, TESLIM_EDILDI, DUZELTME_ISTENDI, ONAYLANDI
}
