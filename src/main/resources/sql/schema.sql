-- Base : medilink (créer la base si besoin : CREATE DATABASE medilink CHARACTER SET utf8mb4;)
--
-- Table `disponibilite` : modèle minimal aligné avec le code Java (pas de medecin_id en base).

CREATE TABLE IF NOT EXISTS `user` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `full_name` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL,
    `roles` VARCHAR(255) NOT NULL COMMENT 'ex: PATIENT ou MEDECIN ou MEDECIN,ADMIN',
    `preferred_time` TIME NULL,
    `max_days_ahead` INT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `disponibilite` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `date` DATE NOT NULL,
    `heure_debut` TIME NOT NULL,
    `heure_fin` TIME NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_dispo_date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `rendez_vous` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `disponibilite_id` INT NOT NULL,
    `date_heure` DATETIME NOT NULL,
    `statut` VARCHAR(32) NOT NULL,
    `motif` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL,
    `patient_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rdv_disponibilite` (`disponibilite_id`),
    KEY `idx_rdv_patient` (`patient_id`),
    CONSTRAINT `fk_rdv_dispo` FOREIGN KEY (`disponibilite_id`) REFERENCES `disponibilite` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_rdv_patient` FOREIGN KEY (`patient_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `avis` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `rendez_vous_id` INT NOT NULL,
    `patient_id` INT NOT NULL,
    `note` INT NOT NULL,
    `commentaire` VARCHAR(2000) NULL,
    `created_at` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_avis_rdv` (`rendez_vous_id`),
    KEY `idx_avis_patient` (`patient_id`),
    CONSTRAINT `fk_avis_rdv` FOREIGN KEY (`rendez_vous_id`) REFERENCES `rendez_vous` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_avis_patient` FOREIGN KEY (`patient_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
