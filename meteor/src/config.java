import java.io.File;

class config {
    static final int PANEL_W = 500;
    static final int PANEL_H = 750;
    static final int METEORITE_SIZE = 50;

    static final double MIN_SPEED = 2;     // px/tick
    static final double MAX_SPEED = 12.0;    // เพดานความเร็ว
    static final double BOUNCE_ACCEL = 1.12; // คูณความเร็วเมื่อชนขอบ
    static final int FPS_MS = 12;                 // ~60 FPS
     static final int EXPLOSION_MS = 250;          // ระเบิดแสดงกี่ ms
    static final double HIT_FACTOR = 0.90; // 0.8–1.0 ยิ่งต่ำยิ่งชนง่าย

}
