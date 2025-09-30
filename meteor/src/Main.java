import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        int meteorCount = 5;
        String input = JOptionPane.showInputDialog(null, "Amount:", "Meteor", JOptionPane.QUESTION_MESSAGE);
        try {
            if (input != null) {
                int n = Integer.parseInt(input.trim());
                if (n > 0)
                {
                    meteorCount = n;
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid! Use Default Amount = 5");
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

    Random rnd = new Random();
    JLabel bgLabel;
    // Meteors
    Meteor[] meteors;
    // HUD
    JLabel hud = new JLabel("Meteors: 0");
    // เธรดตรวจชน + สถานะการทำงาน
    Thread ThreadCheck;

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
            if (t == 1)
            {
                pick = m1;
            } else if (t == 2)
            {
                pick = m2;
            } else
            {
                pick = m3;
            }
            ImageIcon icon = iconOf(pick, config.METEORITE_SIZE, config.METEORITE_SIZE);


            int Startx = rnd.nextInt(Math.max(1, config.PANEL_W - 10 - config.METEORITE_SIZE));
            int Starty = rnd.nextInt(Math.max(1, config.PANEL_H - 30 - config.METEORITE_SIZE));

            double dx = rnd.nextDouble() * 2 - 1; // random -1.0..1.0
            double dy = rnd.nextDouble() * 2 - 1; // random -1.0..1.0

            // ป้องกันไม่ให้ dx,dy = 0 ทั้งคู่
            if (dx == 0 && dy == 0)
            {
                dx = 1;
            }

            double spd = config.MIN_SPEED + rnd.nextDouble() * 2.8;

            dx *= spd;
            dy *= spd;

            Meteor m = new Meteor(this, icon, Startx, Starty , dx, dy);
            meteors[i] = m;
            add(m);
            setComponentZOrder(m, 0);

            Thread th = new Thread(new MeteorMove(m, this), "meteor-" + i);
            th.setDaemon(true);
            th.start();

        }

        class hubThread extends Thread {
            private final Mypanel panel;
            hubThread(Mypanel panel) {
                this.panel = panel;
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
        hubThread hubThread = new hubThread(this);
        hubThread.start();
        ThreadCheck = new MeteorCheck(this);
        ThreadCheck.setDaemon(true);
        ThreadCheck.start();
    }
    int getAliveCount() {
        int c = 0;
        for (Meteor m : meteors)
        {
            if (m.alive)
            {
                c++;
            }
        }
        return c;
    }
    void spawnExplosion(int x, int y) {
        JLabel ex = new JLabel(iconOf(m4, config.METEORITE_SIZE, config.METEORITE_SIZE));
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
class Meteor extends JLabel {
        Mypanel panel;
        boolean alive = true;
        double x, y, Speedx, Speedy;

        Meteor(Mypanel p, ImageIcon icon, int MeteorX, int MeteorY, double speedx, double speedy) {
            super(icon);
            panel = p;
            x = MeteorX;
            y = MeteorY;
            Speedx = speedx;
            Speedy = speedy;
            setBounds(MeteorX, MeteorY, config.METEORITE_SIZE, config.METEORITE_SIZE);
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

        double cx() { return x + getWidth() / 2.0; }

        double cy() {
            return y + getHeight() / 2.0;
        }
    }
class MeteorMove extends Thread {
        Meteor m;
        Mypanel p;

        MeteorMove(Meteor meteor, Mypanel panel) {
            this.m = meteor;
            this.p = panel;
            setDaemon(true);
        }
        @Override
        public void run() {
            try {
                while (true) {
                    m.x += m.Speedx;
                    m.y += m.Speedy;
                    if (m.Speedx >= config.MAX_SPEED){
                        m.Speedx = config.MAX_SPEED;
                    }
                    if (m.Speedy >= config.MAX_SPEED){
                        m.Speedy = config.MAX_SPEED;
                    }
                    if (m.x <= 0) {
                        m.x = 0;
                        m.Speedx = Math.abs(m.Speedx)*config.BOUNCE_ACCEL;
                    } else if (m.x >= config.PANEL_W - 10 - m.getWidth()) {
                        m.x = config.PANEL_W - 10 - m.getWidth();
                        m.Speedx = -Math.abs(m.Speedx)*config.BOUNCE_ACCEL;
                    }
                    if (m.y <= 0) {
                        m.y = 0;
                        m.Speedy = Math.abs(m.Speedy)*config.BOUNCE_ACCEL;

                    } else if (m.y >= config.PANEL_H - 35 - m.getHeight()) {
                        m.y = config.PANEL_H - 35 - m.getHeight();
                        m.Speedy = -Math.abs(m.Speedy)*config.BOUNCE_ACCEL;
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
    class MeteorCheck extends Thread {
        Mypanel panel;
        Random random = new Random();
        MeteorCheck(Mypanel panel) {
            this.panel = panel;
            setDaemon(true);
        }
        @Override
        public void run() {
            double hitDiameter = config.METEORITE_SIZE * config.HIT_FACTOR;
            double minDistSq = hitDiameter * hitDiameter;

            try {
                while (true) {
                    Meteor[] arr = panel.meteors;

                    for (int i = 0; i < arr.length; i++)
                    {
                        Meteor a = arr[i];
                        if (a == null || !a.alive)
                        {
                            continue;
                        }
                        double ax = a.cx();
                        double ay = a.cy();
                        for (int j = i + 1; j < arr.length; j++)
                        {
                            Meteor b = arr[j];
                            if (b == null || !b.alive) continue;

                            double dx = ax - b.cx();
                            double dy = ay - b.cy();
                            double distSq = (dx * dx) + (dy * dy);

                            if (distSq < minDistSq) {

                                if (random.nextBoolean())
                                {
                                    double midX = a.cx();
                                    double midY = a.cy();
                                    int ex = (int)(midX - config.METEORITE_SIZE/2.0);
                                    int ey = (int)(midY - config.METEORITE_SIZE/2.0);
                                    panel.spawnExplosion(ex, ey);
                                    a.die();
                                } else
                                {
                                    double midX = b.cx();
                                    double midY = b.cy();
                                    int ex = (int)(midX - config.METEORITE_SIZE/2.0);
                                    int ey = (int)(midY - config.METEORITE_SIZE/2.0);
                                    panel.spawnExplosion(ex, ey);
                                    b.die();
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


