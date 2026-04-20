package ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.Random;

/**
 * Fond type « réseau de particules » : points animés et segments entre voisins proches (style landing MediLink Care).
 */
public final class ParticleBackground {

    private static final double LINE_MAX_DIST = 118;
    private static final double SPEED = 52;

    private final Canvas canvas;
    private final int n;
    private final Random rnd = new Random();
    private double[] x;
    private double[] y;
    private double[] vx;
    private double[] vy;
    private double[] radius;
    private double[] hue;
    private double[] sat;
    private AnimationTimer timer;
    private long lastNs;

    public ParticleBackground(Canvas canvas, Region sizeSource, int particleCount) {
        this.canvas = canvas;
        this.n = Math.max(24, particleCount);
        canvas.widthProperty().bind(sizeSource.widthProperty());
        canvas.heightProperty().bind(sizeSource.heightProperty());
        canvas.setMouseTransparent(true);
        Runnable resize = this::initOrResetParticles;
        canvas.widthProperty().addListener((o, a, b) -> resize.run());
        canvas.heightProperty().addListener((o, a, b) -> resize.run());
    }

    private void initOrResetParticles() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 32 || h < 32) {
            return;
        }
        x = new double[n];
        y = new double[n];
        vx = new double[n];
        vy = new double[n];
        radius = new double[n];
        hue = new double[n];
        sat = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = rnd.nextDouble() * w;
            y[i] = rnd.nextDouble() * h;
            vx[i] = (rnd.nextDouble() - 0.5) * 2 * SPEED;
            vy[i] = (rnd.nextDouble() - 0.5) * 2 * SPEED;
            radius[i] = 0.7 + rnd.nextDouble() * 2.2;
            hue[i] = 155 + rnd.nextDouble() * 95;
            sat[i] = 0.42 + rnd.nextDouble() * 0.25;
        }
    }

    public void play() {
        if (timer != null) {
            timer.stop();
        }
        lastNs = 0;
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (x == null && canvas.getWidth() > 32) {
                    initOrResetParticles();
                }
                if (x == null) {
                    return;
                }
                if (lastNs == 0) {
                    lastNs = now;
                }
                double dt = Math.min(0.05, (now - lastNs) / 1_000_000_000.0);
                lastNs = now;
                tick(dt);
                render();
            }
        };
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void tick(double dt) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (x == null) {
            return;
        }
        for (int i = 0; i < n; i++) {
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            if (x[i] < 0) {
                x[i] = 0;
                vx[i] = Math.abs(vx[i]);
            } else if (x[i] > w) {
                x[i] = w;
                vx[i] = -Math.abs(vx[i]);
            }
            if (y[i] < 0) {
                y[i] = 0;
                vy[i] = Math.abs(vy[i]);
            } else if (y[i] > h) {
                y[i] = h;
                vy[i] = -Math.abs(vy[i]);
            }
        }
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w < 8 || h < 8 || x == null) {
            return;
        }
        g.setFill(Color.web("#050a14"));
        g.fillRect(0, 0, w, h);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dx = x[i] - x[j];
                double dy = y[i] - y[j];
                double d = Math.hypot(dx, dy);
                if (d < LINE_MAX_DIST && d > 0.5) {
                    double t = 1 - d / LINE_MAX_DIST;
                    double alpha = t * t * 0.42;
                    g.setStroke(Color.color(0.35, 0.82, 0.95, alpha));
                    g.setLineWidth(0.55);
                    g.strokeLine(x[i], y[i], x[j], y[j]);
                }
            }
        }

        for (int i = 0; i < n; i++) {
            g.setFill(Color.hsb(hue[i], sat[i], 1.0, 0.9));
            double r = radius[i];
            g.fillOval(x[i] - r, y[i] - r, r * 2, r * 2);
        }
    }
}
