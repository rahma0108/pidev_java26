package services;

import models.AnalyseDonIA;

import java.io.IOException;

/**
 * Analyse d’un don par un modèle de langage (Claude, Gemini, etc.).
 */
public interface DonAnalyseLLM {

    AnalyseDonIA analyserDon(String titre, String categorie, String description, int quantite) throws IOException;
}
