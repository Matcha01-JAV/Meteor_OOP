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
            JOptionPane.showMessageDialog(null, "ค่าไม่ถูกต้อง จะใช้ค่าเริ่มต้น = 5");
        }

        Mainframe mf = new Mainframe();
        Mypanel mp = new Mypanel(meteorCount);
        mf.add(mp);
        mf.setVisible(true);
    }
}
// สร้างเฟรม
class Mainframe extends JFrame {
    Mainframe() {
        setSize(config.PANEL_W, config.PANEL_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setTitle("Meteor Field");
    }
}


class Mypanel extends JPanel {
    // รูปภาพ
    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"meteor"+ File.separator+"src"+ File.separator+"Meteorpic"+ File.separator+"Background2.jpg");
    Image m1 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"meteor"+ File.separator+"src"+ File.separator+"Meteorpic"+ File.separator+"Metorite1.png");
    Image m2 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"meteor"+ File.separator+"src"+ File.separator+"Meteorpic"+ File.separator+"Metorite2.png");
    Image m3 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"meteor"+ File.separator+"src"+ File.separator+"Meteorpic"+ File.separator+"Metorite3.png");
    Image m4 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir")+ File.separator+"meteor"+ File.separator+"src"+ File.separator+"Meteorpic"+ File.separator+"explode.png");

    // สถานะอุกกาบาต
    double[] meteorX;
    double[] meteorY;
    double[] MoveX;
    double[] MoveY;
    boolean[] alive;
    int[] type;
    double[] epsX;     // ระเบิด (x)
    double[] epsY;     // ระเบิด (y)
    int[] epsstay;     // เวลาของระเบิด

    Timer timer = new Timer(true);
    Random rnd = new Random();

    Mypanel(int meteorCount) {
        setPreferredSize(new Dimension(config.PANEL_W, config.PANEL_H));

        meteorX = new double[meteorCount];
        meteorY = new double[meteorCount];
        MoveX   = new double[meteorCount];
        MoveY   = new double[meteorCount];
        alive   = new boolean[meteorCount];
        type    = new int[meteorCount];

        // [ADD] อาร์เรย์เอฟเฟกต์ระเบิด
        epsX   = new double[meteorCount];
        epsY   = new double[meteorCount];
        epsstay = new int[meteorCount];

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

            epsstay[i] = 0; // ยังไม่มีระเบิด
        }

        // ~60 FPS
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                updateMotion();
                updateBooms(); // ลดอายุระเบิด
                SwingUtilities.invokeLater(() -> repaint());
            }
        }, 0, 16);
    }

    //เวลาเอฟเฟกต์ระเบิด
    private void updateBooms() {
        for (int i = 0; i < epsstay.length; i++) {
            if (epsstay[i] > 0)
            {
                epsstay[i]--;
            }
        }
    }

    // อัปเดตการเคลื่อนที่ + ชนขอบ + ตรวจชนกัน
    private void updateMotion() {
        for (int i = 0; i < meteorX.length; i++) {
            if (!alive[i]) continue;

            // เคลื่อนที่แนวตรง
            meteorX[i] += MoveX[i];
            meteorY[i] += MoveY[i];

            boolean bounced = false;

            // ขอบซ้าย/ขวา
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

            // เร่งนิดหน่อยตอนชนขอบ (คุมเพดาน)
            if (bounced) {
                double speed = Math.hypot(MoveX[i], MoveY[i]) * config.BOUNCE_ACCEL;
                if (speed > config.MAX_SPEED) speed = config.MAX_SPEED;
                double angle = Math.atan2(MoveY[i], MoveX[i]);
                MoveX[i] = Math.cos(angle) * speed;
                MoveY[i] = Math.sin(angle) * speed;
            }
        }

        // ตรวจชนกันและสร้างระเบิด (ไม่เปลี่ยนทิศลูกที่รอด)
        checkMeteorCollisions();
    }

    // ตรวจชนกันแบบวงกลม + ทำระเบิด
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
                    // จุดชน
                    double midX = (cx1 + cx2) / 2.0;
                    double midY = (cy1 + cy2) / 2.0;

                    // เก็บเอฟเฟกต์ไว้ในช่อง i (หรือ j ก็ได้)
                    epsX[i] = midX;
                    epsY[i] = midY;
                    epsstay[i] = 15; // ~0.25 วินาทีที่ 60FPS

                    // ให้หายไป 1 ลูกแบบสุ่ม
                    if (rnd.nextBoolean()) {
                        alive[j] = false;
                    } else {
                        alive[i] = false;
                    }

                    // หมายเหตุ: ไม่แตะ moveX/moveY ของลูกที่รอด
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // พื้นหลังเต็มจอ
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);

        // วาดอุกกาบาตที่ยังมีชีวิต
        int aliveCount = 0;
        for (int i = 0; i < meteorX.length; i++) {
            if (!alive[i]) continue;

            Image sprite;
            if (type[i] == 1)      sprite = m1;
            else if (type[i] == 2) sprite = m2;
            else                   sprite = m3;

            g.drawImage(sprite,
                    (int)Math.round(meteorX[i]),
                    (int)Math.round(meteorY[i]),
                    config.METEORITE_SIZE, config.METEORITE_SIZE, this);

            aliveCount++;
        }

        // วาดเอฟเฟกต์ระเบิด
        for (int i = 0; i < epsstay.length; i++) {
            if (epsstay[i] > 0) {
                g.drawImage(m4,
                        (int)(epsX[i] - config.METEORITE_SIZE / 2.0),
                        (int)(epsY[i] - config.METEORITE_SIZE / 2.0),
                        config.METEORITE_SIZE, config.METEORITE_SIZE, this);
            }
        }

        // HUD
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Tahoma", Font.BOLD, 20));
        g.drawString("Meteors: " + aliveCount, 10, 22);
    }
}
