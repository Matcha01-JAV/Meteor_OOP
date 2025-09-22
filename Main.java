import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Main {
    public static void main(String[] args) {
        // รับจำนวนอุกกาบาต
        int meteorCount = 5; // ค่าเริ่มต้น
        String input = JOptionPane.showInputDialog(
                null, "Amount:", "Meteor", JOptionPane.QUESTION_MESSAGE
        );
        try {
            if (input != null) {
                int n = Integer.parseInt(input.trim());
                if (n > 0) meteorCount = n;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid Use defualt = 5");
        }

        // สร้างเฟรม
        Mainframe mf = new Mainframe();
        Mypanel mp = new Mypanel(meteorCount);
        mf.add(mp);
        mf.setVisible(true);
    }
}

class Mainframe extends JFrame {
    Mainframe() {
        setSize(config.PANEL_W, config.PANEL_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle("Meteor Field");
    }
}

/** ค่าคงที่ของเกม */


class Mypanel extends JPanel {
    // โหลดรูป
    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"Meteorpic"+File.separator+"background.png");
    Image m1 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"Meteorpic"+File.separator+"Metorite1.png");
    Image m2 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"Meteorpic"+File.separator+"Metorite2.png");
    Image m3 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"Meteorpic"+File.separator+"Metorite3.png");

    // สถานะของอุกกาบาต
    double[] meteorX;     // ตำแหน่งแกน meteorX (ซ้ายบน)
    double[] meteorY;     // ตำแหน่งแกน meteorY (ซ้ายบน)
    double[] MoveX;    // ความเร็วแกน meteorX
    double[] MoveY;    // ความเร็วแกน meteorY
    boolean[] alive;// ยังอยู่หรือไม่
    int[] type;     // ใช้เลือกรูป 1..3

    private final Timer timer = new Timer(true);

    Mypanel(int meteorCount) {
        setPreferredSize(new Dimension(config.PANEL_W, config.PANEL_H));

        meteorX = new double[meteorCount];
        meteorY = new double[meteorCount];
        MoveX = new double[meteorCount];
        MoveY = new double[meteorCount];
        alive = new boolean[meteorCount];
        type = new int[meteorCount];

        Random rnd = new Random();

        // สุ่มค่าเริ่มต้น
        for (int i = 0; i < meteorCount; i++) {
            meteorX[i] = rnd.nextDouble() * (config.PANEL_W - config.METEORITE_SIZE);
            meteorY[i] = rnd.nextDouble() * (config.PANEL_H - config.METEORITE_SIZE);

            double angle = rnd.nextDouble() * Math.PI * 2.0;      // 0..2π
            double speed = config.MIN_SPEED + rnd.nextDouble() * 2.8; // ~1.2..4.0
            MoveX[i] = Math.cos(angle) * speed;
            MoveY[i] = Math.sin(angle) * speed;

            alive[i] = true;
            type[i] = 1 + rnd.nextInt(3); // 1..3
        }

        // อัปเดต ~60 FPS
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                updateMotion();
                SwingUtilities.invokeLater(() -> repaint());
            }
        }, 0, 16);
    }

    /** อัปเดตการเคลื่อนที่ + ชนขอบ + ชนกัน */
    private void updateMotion() {
        for (int i = 0; i < meteorX.length; i++) {
            if (!alive[i]) continue;

            // เคลื่อนที่แนวตรง
            meteorX[i] += MoveX[i];
            meteorY[i] += MoveY[i];

            boolean bounced = false;

            // ขอบซ้าย/ขวา: ล็อกพอดี + เด้งกลับ + เร่งความเร็ว
            if (meteorX[i] <= 0) {
                meteorX[i] = 0;
                MoveX[i] = Math.abs(MoveX[i]);
                bounced = true;
            } else if (meteorX[i] >= config.PANEL_W - config.METEORITE_SIZE) {
                meteorX[i] = config.PANEL_W - config.METEORITE_SIZE;
                MoveX[i] = -Math.abs(MoveX[i]);
                bounced = true;
            }

            // ขอบบน/ล่าง
            if (meteorY[i] <= 0) {
                meteorY[i] = 0;
                MoveY[i] = Math.abs(MoveY[i]);
                bounced = true;
            } else if (meteorY[i] >= config.PANEL_H - config.METEORITE_SIZE) {
                meteorY[i] = config.PANEL_H - config.METEORITE_SIZE;
                MoveY[i] = -Math.abs(MoveY[i]);
                bounced = true;
            }

            // เร่งเมื่อชนขอบ (คุมเพดาน)
            if (bounced) {
                double speed = Math.hypot(MoveX[i], MoveY[i]) * config.BOUNCE_ACCEL;
                if (speed > config.MAX_SPEED) speed = config.MAX_SPEED;
                double angle = Math.atan2(MoveY[i], MoveX[i]);
                MoveX[i] = Math.cos(angle) * speed;
                MoveY[i] = Math.sin(angle) * speed;
            }
        }

        // ตรวจชนกัน (วงกลม) แล้วหายทั้งคู่
        checkMeteorCollisions();
    }

    /** ตรวจชนกันแบบวงกลม (ใช้จุดศูนย์กลาง + ระยะชนปรับด้วย COLLISION_FACTOR) */
    private void checkMeteorCollisions() {
        final double hitDiameter = config.METEORITE_SIZE * config.COLLISION_FACTOR;
        final double minDistSq = hitDiameter * hitDiameter;

        for (int i = 0; i < meteorX.length; i++) {
            if (!alive[i]) continue;

            double cx1 = meteorX[i] + config.METEORITE_SIZE / 2.0;
            double cy1 = meteorY[i] + config.METEORITE_SIZE / 2.0;

            for (int j = i + 1; j < meteorX.length; j++) {
                if (!alive[j]) continue;

                double cx2 = meteorX[j] + config.METEORITE_SIZE / 2.0;
                double cy2 = meteorY[j] + config.METEORITE_SIZE / 2.0;

                double dx = cx1 - cx2;
                double dy = cy1 - cy2;
                double distSq = dx * dx + dy * dy;

                if (distSq < minDistSq) {
                    alive[i] = false;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // วาดพื้นหลังเต็มจอ
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);

        // วาดอุกกาบาตที่ยังมีชีวิต
        int aliveCount = 0;
        for (int i = 0; i < meteorX.length; i++) {
            if (!alive[i]) continue;

            Image sprite = switch (type[i]) {
                case 1 -> m1;
                case 2 -> m2;
                default -> m3;
            };
            g.drawImage(sprite,
                    (int) Math.round(meteorX[i]),
                    (int) Math.round(meteorY[i]),
                    config.METEORITE_SIZE, config.METEORITE_SIZE, this);

            aliveCount++;
        }

        // HUD
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Meteors: " + aliveCount, 10, 22);
    }
}
