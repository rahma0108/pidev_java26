package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/**
 * Fond plein écran identique à l’ancienne couche de base des particules (#050a14),
 * sans points ni segments animés (la racine {@code root-care} reste transparente dans le CSS).
 */
public final class ParticleBackground {

    private static final Color FOND = Color.web("#050a14");

    private final Canvas canvas;

    @SuppressWarnings("unused")
    public ParticleBackground(Canvas canvas, Region sizeSource, int particleCount) {
        this.canvas = canvas;
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        Runnable peindre = this::peindreFond;
        canvas.widthProperty().addListener((o, a, b) -> peindre.run());
        canvas.heightProperty().addListener((o, a, b) -> peindre.run());
    }

    private void peindreFond() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 8 || h < 8) {
            return;
        }
        var g = canvas.getGraphicsContext2D();
        g.setFill(FOND);
        g.fillRect(0, 0, w, h);
    }

    public void play() {
        peindreFond();
    }

    public void stop() {
        // rien à arrêter
    }
}
