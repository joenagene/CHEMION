
package game;

import java.awt.*;

public class HealthEffects {

    private static final double LOW_HP = 25.0;
    private static final double CRITICAL_HP = 10.0;

    private static final int HEAL_DURATION = 500;

    private float vignetteOpacity = 0f;
    private long healStart = -1;

    // Triggered when a medkit is successfully used
    public void triggerHeal() {
        healStart = System.currentTimeMillis();
    }

    // Update the low-health 
    public void update(double hp, double maxHp) {

        float targetOpacity = 0f;

        if (maxHp > 0 && hp > 0) {

            double percent = (hp / maxHp) * 100.0;

            if (percent <= LOW_HP) {

                double severity =
                    (LOW_HP - percent) / LOW_HP;

                boolean critical = percent <= CRITICAL_HP;

                double pulseSpeed = critical ? 500.0 : 750.0;

                double pulse = (
                    Math.sin(
                        System.currentTimeMillis()
                        * 2.0 * Math.PI / pulseSpeed
                    ) + 1.0
                ) / 2.0;

                targetOpacity = (float) (
                    0.15 + severity * 0.25 + pulse * 0.10
                );
            }
        }

        vignetteOpacity +=
            (targetOpacity - vignetteOpacity) * 0.10f;

        if (vignetteOpacity < 0.005f) {
            vignetteOpacity = 0f;
        }
    }

    //  red glow around the screen edges
    public void drawVignette(Graphics2D g, int width, int height) {

        if (vignetteOpacity <= 0f) return;
        if (width <= 0 || height <= 0) return;

        Graphics2D effect = (Graphics2D) g.create();

        int alpha = Math.min(
            255, (int) (vignetteOpacity * 255)
        );

        Color red = new Color(180, 0, 15, alpha);
        Color transparent = new Color(180, 0, 15, 0);

        float edge = Math.min(width, height) * 0.16f;

        // Left edge
        effect.setPaint(new GradientPaint(
            0, 0, red,
            edge, 0, transparent
        ));

        effect.fillRect(0, 0, (int) Math.ceil(edge), height);

        // Right edge
        effect.setPaint(new GradientPaint(
            width, 0, red,
            width - edge, 0, transparent
        ));

        effect.fillRect(
            width - (int) Math.ceil(edge),
            0, (int) Math.ceil(edge), height
        );

        // Top edge
        effect.setPaint(new GradientPaint(
            0, 0, red,
            0, edge, transparent
        ));

        effect.fillRect(0, 0, width, (int) Math.ceil(edge));

        // Bottom edge
        effect.setPaint(new GradientPaint(
            0, height, red,
            0, height - edge, transparent
        ));

        effect.fillRect(
            0, height - (int) Math.ceil(edge),
            width, (int) Math.ceil(edge)
        );

        effect.dispose();
    }

    // green healing glow
    public void drawHealEffect(
            Graphics2D g, int x, int y, int radius) {

        if (healStart < 0 || radius <= 0) return;

        long elapsed =
            System.currentTimeMillis() - healStart;

        if (elapsed >= HEAL_DURATION) {
            healStart = -1;
            return;
        }

        float progress =
            elapsed / (float) HEAL_DURATION;

        int alpha = (int) (100 * (1f - progress));

        Graphics2D effect = (Graphics2D) g.create();

        RadialGradientPaint glow = new RadialGradientPaint(
            x, y, radius,
            new float[]{0f, 1f},
            new Color[]{
                new Color(50, 255, 100, alpha),
                new Color(50, 255, 100, 0)
            }
        );

        effect.setPaint(glow);

        effect.fillOval(
            x - radius,
            y - radius,
            radius * 2,
            radius * 2
        );

        effect.dispose();
    }

    public void reset() {
        vignetteOpacity = 0f;
        healStart = -1;
    }
}
