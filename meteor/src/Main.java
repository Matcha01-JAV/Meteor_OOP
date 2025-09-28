import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int meteorCount = 5;
        String input = JOptionPane.showInputDialog(null, "Amount:", "Meteor", JOptionPane.QUESTION_MESSAGE);
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

/* หน้าต่างหลัก */
class Mainframe extends JFrame {
    Mainframe() {
        setSize(config.PANEL_W, config.PANEL_H);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setTitle("Meteor Field");
    }
}
class Mypanel extends JPanel {

    // โหลดรูปด้วย Toolkit แบบที่ขอ
    Image bg = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "meteor" + File.separator + "src" + File.separator + "Meteorpic" + File.separator + "Background2.jpg");
    Image m1 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "meteor" + File.separator + "src" + File.separator + "Meteorpic" + File.separator + "Metorite1.png");
    Image m2 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "meteor" + File.separator + "src" + File.separator + "Meteorpic" + File.separator + "Metorite2.png");
    Image m3 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "meteor" + File.separator + "src" + File.separator + "Meteorpic" + File.separator + "Metorite3.png");
    Image m4 = Toolkit.getDefaultToolkit().createImage(System.getProperty("user.dir") + File.separator + "meteor" + File.separator + "src" + File.separator + "Meteorpic" + File.separator + "explode.png");

    final Random rnd = new Random();
    JLabel bgLabel;
    // Meteors
    Meteor[] meteors;
    // HUD
    final JLabel hud = new JLabel("Meteors: 0");
    // เธรดตรวจชน + สถานะการทำงาน
    Thread collisionThread;
    boolean running = true;

    // helper: สร้าง ImageIcon จาก Image พร้อม scale
    private static ImageIcon iconOf(Image img, int w, int h) {
        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    Mypanel(int meteorCount) {
        setLayout(null);
        setPreferredSize(new Dimension(config.PANEL_W, config.PANEL_H));

        // พื้นหลัง (JLabel) — แปลง bg (Image) เป็น ImageIcon ที่ขนาดเต็มจอ
        bgLabel = new JLabel(iconOf(bg, config.PANEL_W, config.PANEL_H));
        bgLabel.setBounds(0, 0, config.PANEL_W, config.PANEL_H);
        add(bgLabel); // ใส่ก่อน = ชั้นหลังสุด

        // HUD
        hud.setFont(new Font("Tahoma", Font.BOLD, 20));
        hud.setForeground(Color.YELLOW);
        hud.setBounds(10, 5, 300, 28);
        add(hud);
        setComponentZOrder(hud, 0); // ให้อยู่บน
        // สร้างอุกกาบาต
        meteors = new Meteor[meteorCount];

        for (int i = 0; i < meteorCount; i++) {
            int t = 1 + rnd.nextInt(3);
            Image pick;
            if (t == 1) {
                pick = m1;
            } else if (t == 2) {
                pick = m2;
            } else {
                pick = m3;
            }
            ImageIcon icon = iconOf(pick, config.METEORITE_SIZE, config.METEORITE_SIZE);

            int sx = rnd.nextInt(Math.max(1, config.PANEL_W - config.METEORITE_SIZE));
            int sy = rnd.nextInt(Math.max(1, config.PANEL_H - config.METEORITE_SIZE));

            double ang = rnd.nextDouble() * Math.PI * 2.0;
            double spd = config.MIN_SPEED + rnd.nextDouble() * 2.8;

            Meteor m = new Meteor(this, icon, sx, sy, Math.cos(ang) * spd, Math.sin(ang) * spd);
            meteors[i] = m;
            add(m);
            setComponentZOrder(m, 0);

            Thread th = new Thread(new MeteorMove(m, this), "meteor-" + i);
            th.setDaemon(true);
            th.start();
        }



        class FThread extends Thread {
            private final Mypanel panel;
            FThread(Mypanel panel) {
                this.panel = panel;
                setDaemon(true);
            }
            @Override
            public void run() {
                try {
                    while (true) {
                        final int alive = panel.getAliveCount();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                panel.hud.setText("Meteors: " + alive);
                                panel.repaint();
                            }
                        });
                        Thread.sleep(100);
                    }
                } catch (InterruptedException ignored) {}
            }
        }

        // เธรดตรวจชน
        FThread hudThread = new FThread(this);
        hudThread.start();
        collisionThread = new Collision(this);
        collisionThread.setDaemon(true);
        collisionThread.start();
    }

    private int getAliveCount() {
        int c = 0;
        for (Meteor m : meteors) {
            if (m.alive) {
                c++;
            }
        }
        return c;
    }
    void spawnExplosion(int x, int y) {
        final JLabel ex = new JLabel(iconOf(m4, config.METEORITE_SIZE, config.METEORITE_SIZE));
        ex.setBounds(x, y, config.METEORITE_SIZE, config.METEORITE_SIZE);
        // อัปเดต UI ตรงๆ (ไม่ผ่าน Runnable)
        add(ex);
        setComponentZOrder(ex, 0);
        ex.setVisible(true);
        revalidate();
        repaint();

        // Thread สำหรับลบ explosion
        Thread explosionThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(config.EXPLOSION_MS);
                } catch (InterruptedException ignored)
                {

                }

                remove(ex);
                revalidate();
                repaint();
            }
        };
        explosionThread.setDaemon(true);
        explosionThread.start();
    }

    // อุกกาบาตเป็น JLabel
    static  class Meteor extends JLabel {
        Mypanel panel;
        boolean alive = true;
        double x, y, dx, dy;

        Meteor(Mypanel p, ImageIcon icon, int startX, int startY, double sx, double sy) {
            super(icon);
            panel = p;
            x = startX;
            y = startY;
            dx = sx;
            dy = sy;
            setBounds(startX, startY, config.METEORITE_SIZE, config.METEORITE_SIZE);
            setVisible(true);
        }
        void die() {
            if (!alive)
            {
                return;
            }
            alive = false;
            setVisible(false);
        }

        double cx() {
            return x + getWidth() / 2.0;
        }

        double cy() {
            return y + getHeight() / 2.0;
        }
    }
    static class MeteorMove extends Thread {
        private final Meteor m;
        private final Mypanel p;

        MeteorMove(Meteor meteor, Mypanel panel) {
            this.m = meteor;
            this.p = panel;
            setDaemon(true);
        }
        @Override
        public void run() {
            try {
                while (true) {
                    m.x += m.dx;
                    m.y += m.dy;
                    boolean bounce = false;
                    if (m.x <= 0) {
                        m.x = 0;
                        m.dx = Math.abs(m.dx);
                        bounce = true;
                    } else if (m.x >= config.PANEL_W - m.getWidth()) {
                        m.x = config.PANEL_W - m.getWidth();
                        m.dx = -Math.abs(m.dx);
                        bounce = true;
                    }
                    if (m.y <= 0) {
                        m.y = 0;
                        m.dy = Math.abs(m.dy);
                        bounce = true;
                    } else if (m.y >= config.PANEL_H - m.getHeight()) {
                        m.y = config.PANEL_H - m.getHeight();
                        m.dy = -Math.abs(m.dy);
                        bounce = true;
                    }
                    if (bounce) {
                        double len = Math.hypot(m.dx, m.dy);
                        double sp = len * config.BOUNCE_ACCEL;
                        if (sp > config.MAX_SPEED)
                        {
                            sp = config.MAX_SPEED;
                        }
                        if (len != 0)
                        {
                            m.dx = (m.dx / len) * sp;
                            m.dy = (m.dy / len) * sp;
                        }
                    }


                    int fx = (int) Math.round(m.x);
                    int fy = (int) Math.round(m.y);
                    m.setLocation(fx, fy);
                    Thread.sleep(config.FPS_MS);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }
    static class Collision extends Thread {
        private final Mypanel panel;
        private final Random random = new Random();
        Collision(Mypanel panel) {
            this.panel = panel;
            setDaemon(true);
        }
        @Override
        public void run() {
            double collisionDistance = config.METEORITE_SIZE * config.COLLISION_FACTOR;
            double minDistanceSquared = collisionDistance * collisionDistance;
            try {
                while (true) {
                    for (int i = 0; i < panel.meteors.length; i++) {
                        Meteor meteorA = panel.meteors[i];
                        if (!meteorA.alive) continue;
                        double ax = meteorA.cx();
                        double ay = meteorA.cy();

                        for (int j = i + 1; j < panel.meteors.length; j++) {
                            Meteor meteorB = panel.meteors[j];
                            if (!meteorB.alive) continue;

                            double diffX = ax - meteorB.cx();
                            double diffY = ay - meteorB.cy();

                            if (diffX * diffX + diffY * diffY < minDistanceSquared) {
                                double midX = (ax + meteorB.cx()) / 2.0;
                                double midY = (ay + meteorB.cy()) / 2.0;

                                int explosionX = (int) (midX - config.METEORITE_SIZE / 2.0);
                                int explosionY = (int) (midY - config.METEORITE_SIZE / 2.0);

                                panel.spawnExplosion(explosionX, explosionY);

                                if (random.nextBoolean())
                                {
                                    meteorA.die();
                                }
                                else
                                {
                                    meteorB.die();
                                }
                            }
                        }
                    }
                    Thread.sleep(config.FPS_MS);
                }
            } catch (InterruptedException ignored) {}
        }
    }

}


