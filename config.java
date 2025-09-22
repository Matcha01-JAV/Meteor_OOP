import java.io.File;

class config {
    static final int PANEL_W = 500;
    static final int PANEL_H = 750;
    static final int METEORITE_SIZE = 50;

    static final double MIN_SPEED = 1.2;     // px/tick
    static final double MAX_SPEED = 12.0;    // เพดานความเร็ว
    static final double BOUNCE_ACCEL = 1.12; // คูณความเร็วเมื่อชนขอบ

    // ช่วยให้ชนง่ายขึ้น (รูปมีขอบโปร่งใสได้)
    static final double COLLISION_FACTOR = 0.90; // 0.8–1.0 ยิ่งต่ำยิ่งชนง่าย

}