import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 无界贪吃蛇 (Infinite Snake)
 *
 * 特性:
 *  - 蛇头永远位于游戏窗口正中央(相机跟随蛇头), 世界坐标无限大, 没有墙壁
 *  - 游戏窗口在物理屏幕上始终跟随蛇头移动
 *  - 窗口移出屏幕可用区域(已扣除 macOS 菜单栏/Dock)边缘时被拆分为两半:
 *    一半留在屏内, 另一半由影子窗口在对侧显示, 形成无界地图效果
 *  - 只有蛇撞到自己才会死亡
 *
 * 操作: 方向键/WASD 转向, 空格 暂停, R 重新开始, ESC 退出
 */
public class SnakeGame extends JPanel {

    // ---------- 常量 ----------
    private static final int CELL = 20;             // 格子像素
    private static final int COLS = 40, ROWS = 30;  // 窗口内可见格子数
    private static final int W = COLS * CELL;       // 窗口宽
    private static final int H = ROWS * CELL;       // 窗口高
    private static final int TICK_START = 140;      // 初始步进毫秒
    private static final int TICK_MIN = 70;         // 最快步进毫秒
    private static final int FOOD_RADIUS = 12;      // 食物刷新范围(格, 保证始终可见)
    private static final int FRAME_MS = 16;         // 渲染帧间隔(约60fps)

    private enum Dir { UP, DOWN, LEFT, RIGHT }
    private enum State { READY, RUNNING, PAUSED, GAME_OVER }

    // ---------- 游戏状态 ----------
    private final List<Point> body = new ArrayList<>();  // 蛇身(世界格坐标, 头在 index 0)
    private final List<Point> prev = new ArrayList<>();  // 上一逻辑步位置, 用于平滑插值
    private Dir dir = Dir.RIGHT;
    private Dir pendingDir = Dir.RIGHT;
    private Point food = new Point(5, 0);
    private State state = State.READY;
    private int score = 0;
    private long lastTickNanos = System.nanoTime();
    private final Random rnd = new Random();

    // ---------- 窗口/屏幕 ----------
    private final JFrame frame;
    private final Rectangle screen;                // 主显示器边界
    private final ShadowView[] shadows = new ShadowView[3];  // 溢出内容影子窗口(左右/上下/角落)
    private int lastWinX = Integer.MIN_VALUE, lastWinY = Integer.MIN_VALUE;
    private int lastWinH;
    private int mainOffY = 0;      // 主窗口内容补偿偏移(macOS 顶部钳制区), 平时为 0
    private final Timer logicTimer;                // 逻辑步进
    private final Timer renderTimer;               // 渲染 + 窗口跟随

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 可用屏幕区域 = 物理边界扣除系统占用区(macOS 菜单栏/Dock 等)。
            // 若用物理边界做回绕, 拆分点会落在被系统遮挡的边缘, 蛇头会在菜单栏/Dock里"消失"
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle bounds = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            Rectangle screen = new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                    Math.max(1, bounds.width - insets.left - insets.right),
                    Math.max(1, bounds.height - insets.top - insets.bottom));
            JFrame frame = new JFrame("无界贪吃蛇 — ESC 退出");
            frame.setUndecorated(true);            // 无边框: 窗口位置即内容位置, 精确居中
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            SnakeGame game = new SnakeGame(frame, screen);
            frame.setContentPane(game);
            frame.pack();
            frame.setVisible(true);
            game.updateWindows();                  // 初始定位: 蛇在屏幕中央, 窗口完全可见
            game.start();
        });
    }

    private SnakeGame(JFrame frame, Rectangle screen) {
        this.frame = frame;
        this.screen = screen;
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
        addMouseListener(new MouseAdapter() {      // 窗口到处跑, 点一下可重新获得键盘焦点
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
        resetGame();
        logicTimer = new Timer(TICK_START, e -> step());
        renderTimer = new Timer(FRAME_MS, e -> {
            repaint();
            updateWindows();
        });
        for (int i = 0; i < shadows.length; i++) shadows[i] = new ShadowView();
    }

    private void start() {
        logicTimer.start();
        renderTimer.start();
        requestFocusInWindow();
    }

    // ================= 逻辑 =================

    /** 逻辑步进: 蛇移动一格 */
    private void step() {
        if (state != State.RUNNING) return;
        lastTickNanos = System.nanoTime();         // 只在真正移动时重置, 否则暂停/结束时插值会抖动

        if (!isOpposite(pendingDir, dir)) dir = pendingDir;   // 应用缓存方向, 禁止180°掉头
        Point head = body.get(0);
        Point nh = move(head, dir);

        // 自撞检测: 若不生长, 尾格本步会移开, 允许进入
        int safeLen = nh.equals(food) ? body.size() : body.size() - 1;
        for (int i = 0; i < safeLen; i++) {
            if (body.get(i).equals(nh)) {
                state = State.GAME_OVER;
                return;
            }
        }

        prev.clear();
        prev.addAll(body);                         // 记录移动前位置(完整保留, 勿删尾!)
        body.add(0, nh);
        if (nh.equals(food)) {
            prev.add(0, prev.get(0));              // 生长: 新头从旧头位置插值而来
            score++;
            logicTimer.setDelay(Math.max(TICK_MIN, TICK_START - score * 2));  // 越吃越快
            spawnFood();
        } else {
            body.remove(body.size() - 1);          // 未吃到: 尾巴跟上
            // prev 不删: prev[i] 是"段 i 移动前的位置", 尾段由 A2 平滑滑向 A1
        }
    }

    /** 以当前视野位置为原点重启, 窗口不会跳变 */
    private void restart(Dir startDir) {
        Point cam = new Point(body.get(0));
        int ox = 0, oy = 0;
        switch (startDir) {
            case UP: oy = 1; break;
            case DOWN: oy = -1; break;
            case LEFT: ox = 1; break;
            case RIGHT: ox = -1; break;
        }
        body.clear();
        body.add(cam);
        body.add(new Point(cam.x + ox, cam.y + oy));
        body.add(new Point(cam.x + ox * 2, cam.y + oy * 2));
        prev.clear();
        prev.addAll(body);
        dir = startDir;
        pendingDir = startDir;
        score = 0;
        state = State.RUNNING;
        lastTickNanos = System.nanoTime();
        if (logicTimer != null) logicTimer.setDelay(TICK_START);
        spawnFood();
    }

    /** 初始重置: 世界原点 (0,0) 开始 */
    private void resetGame() {
        body.clear();
        body.add(new Point(0, 0));
        body.add(new Point(-1, 0));
        body.add(new Point(-2, 0));
        prev.clear();
        prev.addAll(body);
        dir = Dir.RIGHT;
        pendingDir = Dir.RIGHT;
        score = 0;
        state = State.READY;
        lastTickNanos = System.nanoTime();
        spawnFood();
    }

    /** 在蛇头附近随机生成食物(保证不出现在蛇身上) */
    private void spawnFood() {
        Point head = body.get(0);
        for (int tries = 0; tries < 5000; tries++) {
            Point p = new Point(head.x + rnd.nextInt(FOOD_RADIUS * 2 + 1) - FOOD_RADIUS,
                                head.y + rnd.nextInt(FOOD_RADIUS * 2 + 1) - FOOD_RADIUS);
            if (!onBody(p)) { food = p; return; }
        }
        Point p = new Point(head);
        while (onBody(p)) p = move(p, dir);        // 兜底: 沿当前方向找空位
        food = p;
    }

    private boolean onBody(Point p) {
        for (Point s : body) if (s.equals(p)) return true;
        return false;
    }

    private void onKey(int code) {
        if (code == KeyEvent.VK_ESCAPE) { System.exit(0); return; }
        if (code == KeyEvent.VK_R) { restart(Dir.RIGHT); return; }
        if (code == KeyEvent.VK_SPACE) {
            if (state == State.RUNNING) state = State.PAUSED;
            else if (state == State.PAUSED) state = State.RUNNING;
            return;
        }
        Dir d = dirOf(code);
        if (d == null) return;
        if (state == State.READY || state == State.GAME_OVER) {
            restart(d);
        } else {
            if (!isOpposite(d, dir)) pendingDir = d;
        }
    }

    private static Dir dirOf(int code) {
        switch (code) {
            case KeyEvent.VK_UP: case KeyEvent.VK_W: return Dir.UP;
            case KeyEvent.VK_DOWN: case KeyEvent.VK_S: return Dir.DOWN;
            case KeyEvent.VK_LEFT: case KeyEvent.VK_A: return Dir.LEFT;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: return Dir.RIGHT;
            default: return null;
        }
    }

    private static Point move(Point p, Dir d) {
        switch (d) {
            case UP: return new Point(p.x, p.y - 1);
            case DOWN: return new Point(p.x, p.y + 1);
            case LEFT: return new Point(p.x - 1, p.y);
            default: return new Point(p.x + 1, p.y);
        }
    }

    private static boolean isOpposite(Dir a, Dir b) {
        return (a == Dir.UP && b == Dir.DOWN) || (a == Dir.DOWN && b == Dir.UP)
            || (a == Dir.LEFT && b == Dir.RIGHT) || (a == Dir.RIGHT && b == Dir.LEFT);
    }

    // ================= 窗口跟随 + 屏幕边缘拆分 =================

    /** 每帧更新主窗口位置并计算影子窗口: 主窗口可移出屏幕, 溢出部分由影子窗口在对侧显示 */
    private void updateWindows() {
        long hx = screen.x + mod((long) headRenderX() + screen.width / 2L - screen.x, screen.width);
        long hy = screen.y + mod((long) headRenderY() + screen.height / 2L - screen.y, screen.height);
        int wx = (int) (hx - W / 2L);
        int wyIntended = (int) (hy - H / 2L);
        if (W >= screen.width) wx = (int) clamp(wx, screen.x + screen.width - W, (long) screen.x);
        if (H >= screen.height) wyIntended = (int) clamp(wyIntended, screen.y + screen.height - H, (long) screen.y);
        // macOS 窗口不能部分悬在屏幕上方(y∈(-H,菜单栏)会被强制钳回菜单栏下):
        // 顶部 → 窗口贴菜单栏底并收缩; 底部 → 窗口底边贴可用区域下界(Dock 顶)并收缩,
        // 永不滑入 Dock 遮挡区; 左/右无系统限制, 保持滑动拆分
        int wy = wyIntended;
        int winH = H;
        if (screen.y > 0 && wy < screen.y) {
            wy = screen.y;
            winH = H - (screen.y - wyIntended);
        } else if (wyIntended + H > screen.y + screen.height) {
            winH = screen.y + screen.height - wyIntended;
        }
        mainOffY = wyIntended - wy;
        if (wx != lastWinX || wy != lastWinY || winH != lastWinH) {
            frame.setBounds(wx, wy, W, winH);
            lastWinX = wx;
            lastWinY = wy;
            lastWinH = winH;
        }
        updateShadows(wx, wyIntended);
    }

    /**
     * 计算主窗口溢出屏幕的条带并分配给影子窗口:
     *  - 左/右溢出 → 影子窗口在屏幕另一侧显示对应内容条
     *  - 上/下溢出 → 同理
     *  - 角落溢出 → 第三个影子窗口显示角落块
     * offX/offY 为影子窗口渲染时相对视口的平移量(面板坐标 = 视口坐标 + 偏移)
     */
    private void updateShadows(int wx, int wy) {
        boolean xL = wx < screen.x;
        boolean xR = W < screen.width && wx + W > screen.x + screen.width;
        boolean yT = wy < screen.y;
        boolean yB = H < screen.height && wy + H > screen.y + screen.height;
        int n = 0;
        if (xL || xR) {
            int px = xL ? screen.x + screen.width + (wx - screen.x) : screen.x;
            int pw = xL ? screen.x - wx : wx + W - screen.x - screen.width;
            int py = Math.max(wy, screen.y);
            int ph = Math.min(wy + H, screen.y + screen.height) - py;
            int offX = xL ? 0 : wx - screen.x - screen.width;
            n = placeShadow(n, px, py, pw, ph, offX, mainOffY);
        }
        if (yT || yB) {
            int px = Math.max(wx, screen.x);
            int pw = Math.min(wx + W, screen.x + screen.width) - px;
            int py = yT ? screen.y + screen.height + (wy - screen.y) : screen.y;
            int ph = yT ? screen.y - wy : wy + H - screen.y - screen.height;
            int offY = yT ? 0 : wy - screen.y - screen.height;
            n = placeShadow(n, px, py, pw, ph, 0, offY);
        }
        if ((xL || xR) && (yT || yB)) {
            int px = xL ? screen.x + screen.width + (wx - screen.x) : screen.x;
            int pw = xL ? screen.x - wx : wx + W - screen.x - screen.width;
            int py = yT ? screen.y + screen.height + (wy - screen.y) : screen.y;
            int ph = yT ? screen.y - wy : wy + H - screen.y - screen.height;
            int offX = xL ? 0 : wx - screen.x - screen.width;
            int offY = yT ? 0 : wy - screen.y - screen.height;
            n = placeShadow(n, px, py, pw, ph, offX, offY);
        }
        for (; n < shadows.length; n++) shadows[n].setVisible(false);
    }

    /** 配置一个影子窗口的位置/尺寸/偏移, 返回下一个可用下标 */
    private int placeShadow(int idx, int px, int py, int pw, int ph, int offX, int offY) {
        if (pw > 0 && ph > 0) {
            shadows[idx].setOffset(offX, offY);
            shadows[idx].place(px, py, pw, ph);
            shadows[idx].setVisible(true);
            return idx + 1;
        }
        return idx;
    }

    /** 影子窗口: 无边框子窗口, 与主窗口同相机渲染偏移后的视口内容 */
    private class ShadowView {
        private final JFrame win = new JFrame();
        private final JPanel view = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                paintWorld(g2, alpha(), offX, offY);
                drawHud(g2, offX, offY);
                g2.dispose();
            }
        };
        private int offX, offY;

        ShadowView() {
            win.setUndecorated(true);
            win.setResizable(false);
            win.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            win.setFocusableWindowState(false);   // 永不抢键盘焦点
            win.setAlwaysOnTop(true);             // 保证在对侧屏幕可见
            win.setContentPane(view);
            view.setBackground(new Color(0x13, 0x17, 0x22));
            win.setBounds(0, 0, 1, 1);
            win.setVisible(false);
        }

        void setOffset(int ox, int oy) { offX = ox; offY = oy; }

        /** 设置窗口位置与尺寸(屏幕绝对坐标)并请求重绘 */
        void place(int x, int y, int w, int h) {
            win.setBounds(x, y, w, h);
            win.repaint();
        }

        void setVisible(boolean v) { win.setVisible(v); }
    }

    /** 正数取模(结果 ∈ [0, m)) */
    private static long mod(long a, long m) {
        long r = a % m;
        return r < 0 ? r + m : r;
    }

    private static long clamp(long v, long lo, long hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ================= 渲染 =================

    private double alpha() {
        long elapsed = System.nanoTime() - lastTickNanos;
        double a = elapsed / (logicTimer.getDelay() * 1_000_000.0);
        return Math.max(0.0, Math.min(1.0, a));
    }

    /** 蛇头插值后的世界像素坐标(相机位置) */
    private double headRenderX() {
        return (prev.get(0).x + (body.get(0).x - prev.get(0).x) * alpha()) * CELL;
    }

    private double headRenderY() {
        return (prev.get(0).y + (body.get(0).y - prev.get(0).y) * alpha()) * CELL;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        paintWorld(g2, alpha(), 0, mainOffY);
        drawHud(g2, 0, mainOffY);
        drawOverlay(g2, mainOffY);
        g2.dispose();
    }

    /** 渲染世界内容(棋盘/食物/蛇/窗口边框), offX/offY 为影子窗口的视口平移偏移 */
    private void paintWorld(Graphics2D g2, double a, int offX, int offY) {
        double camX = prev.get(0).x + (body.get(0).x - prev.get(0).x) * a;  // 相机所在格(浮点)
        double camY = prev.get(0).y + (body.get(0).y - prev.get(0).y) * a;
        drawCheckerboard(g2, camX, camY, offX, offY);
        drawFood(g2, camX, camY, offX, offY);
        drawSnake(g2, camX, camY, a, offX, offY);
        // 窗口边框: 拆分时边框随内容绕出, 能直观看到"窗口被切成两半"
        g2.setColor(new Color(0x2a, 0x35, 0x4a, 120));
        g2.drawRect(offX + 1, offY + 1, W - 2, H - 2);
    }

    /** 棋盘格背景(对齐世界网格, 移动时可见世界在滚动) */
    private void drawCheckerboard(Graphics2D g2, double camX, double camY, int offX, int offY) {
        int x0 = (int) Math.floor(camX - COLS / 2.0) - 1;
        int y0 = (int) Math.floor(camY - ROWS / 2.0) - 1;
        for (int cy = y0; cy <= y0 + ROWS + 2; cy++) {
            for (int cx = x0; cx <= x0 + COLS + 2; cx++) {
                g2.setColor(((cx + cy) & 1) == 0 ? new Color(0x16, 0x1b, 0x27) : new Color(0x13, 0x17, 0x22));
                g2.fillRect((int) Math.round((cx - camX) * CELL + W / 2.0) + offX - 1,
                            (int) Math.round((cy - camY) * CELL + H / 2.0) + offY - 1, CELL + 2, CELL + 2);
            }
        }
    }

    private void drawFood(Graphics2D g2, double camX, double camY, int offX, int offY) {
        double fx = (food.x - camX) * CELL + W / 2.0 + offX;
        double fy = (food.y - camY) * CELL + H / 2.0 + offY;
        double pulse = 0.8 + 0.2 * Math.sin(System.nanoTime() / 3.0e8);
        int r = (int) Math.round(CELL * 0.42 * pulse);
        g2.setColor(new Color(0xff, 0x4d, 0x4d));
        g2.fillOval((int) fx - r, (int) fy - r, r * 2, r * 2);
        g2.setColor(new Color(0xff, 0xd0, 0xd0));
        g2.fillOval((int) fx - r / 3, (int) fy - r / 3, r * 2 / 3, r * 2 / 3);
    }

    private void drawSnake(Graphics2D g2, double camX, double camY, double a, int offX, int offY) {
        int n = body.size();
        for (int i = n - 1; i >= 0; i--) {          // 先画尾巴, 头在最上层
            Point bp = body.get(i), pp = prev.get(i);
            double sx = (pp.x + (bp.x - pp.x) * a - camX) * CELL + W / 2.0 + offX;
            double sy = (pp.y + (bp.y - pp.y) * a - camY) * CELL + H / 2.0 + offY;
            int ins = Math.max(2, CELL / 5);
            int sz = CELL - ins * 2;
            int rx = (int) sx - CELL / 2 + ins;
            int ry = (int) sy - CELL / 2 + ins;

            if (i == 0) {
                // 蛇头: 亮绿色 + 眼睛
                g2.setColor(new Color(0x6e, 0xf2, 0x9a));
                g2.fillRoundRect(rx, ry, sz, sz, 12, 12);
                g2.setColor(new Color(0x2a, 0x8f, 0x4e));
                g2.drawRoundRect(rx, ry, sz, sz, 12, 12);
                drawEyes(g2, sx, sy, bp, pp);
            } else {
                // 蛇身: 由尾到头的绿色渐变
                double t = (double) i / n;
                g2.setColor(new Color(40 + (int) (t * 20), 170 + (int) (t * 55), 60 + (int) (t * 20)));
                g2.fillRoundRect(rx, ry, sz, sz, 10, 10);
                g2.setColor(new Color(0x0e, 0x5a, 0x33));
                g2.drawRoundRect(rx, ry, sz, sz, 10, 10);
            }
        }
    }

    /** 蛇头眼睛, 朝向移动方向 */
    private void drawEyes(Graphics2D g2, double sx, double sy, Point bp, Point pp) {
        int dx = bp.x - pp.x, dy = bp.y - pp.y;
        if (dx == 0 && dy == 0) {                  // tick 边界插值系数为0时用当前方向兜底
            switch (dir) {
                case UP: dy = -1; break;
                case DOWN: dy = 1; break;
                case LEFT: dx = -1; break;
                case RIGHT: dx = 1; break;
            }
        }
        double len = Math.hypot(dx, dy);
        double nx = len > 0 ? dx / len : 0, ny = len > 0 ? dy / len : 0;
        double px = -ny, py = nx;                  // 垂直方向
        double off = CELL * 0.28, side = CELL * 0.16;
        for (int s = -1; s <= 1; s += 2) {
            double ex = sx + nx * off + px * side * s;
            double ey = sy + ny * off + py * side * s;
            g2.setColor(Color.WHITE);
            g2.fillOval((int) (ex - 4), (int) (ey - 4), 8, 8);
            g2.setColor(new Color(0x1a, 0x1a, 0x2a));
            g2.fillOval((int) (ex + nx * 2 - 2), (int) (ey + ny * 2 - 2), 4, 4);
        }
    }

    private void drawHud(Graphics2D g2, int offX, int offY) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        String scoreStr = "得分 " + score + "    长度 " + body.size();
        String worldStr = "世界坐标 (" + body.get(0).x + ", " + body.get(0).y + ")";
        drawText(g2, scoreStr, 12 + offX, 24 + offY, Color.WHITE);
        drawText(g2, worldStr, W - 12 - g2.getFontMetrics().stringWidth(worldStr) + offX, 24 + offY,
                 new Color(0x9a, 0xa5, 0xb5));
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String help = "方向键/WASD 转向 · 空格 暂停 · R 重开 · ESC 退出";
        drawText(g2, help, W / 2 - g2.getFontMetrics().stringWidth(help) / 2 + offX, H - 14 + offY,
                 new Color(0x8a, 0x94, 0xa3));
    }

    private void drawText(Graphics2D g2, String s, int x, int y, Color c) {
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(s, x + 1, y + 1);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    private void drawOverlay(Graphics2D g2, int offY) {
        String msg = null, sub = null;
        if (state == State.READY) {
            msg = "无界贪吃蛇";
            sub = "按任意方向键开始";
        } else if (state == State.PAUSED) {
            msg = "已暂停";
            sub = "按空格继续";
        } else if (state == State.GAME_OVER) {
            msg = "游戏结束 · 得分 " + score;
            sub = "按 R 或方向键重新开始";
        }
        if (msg == null) return;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, W, H);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        drawText(g2, msg, W / 2 - g2.getFontMetrics().stringWidth(msg) / 2, H / 2 - 8 + offY, Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        drawText(g2, sub, W / 2 - g2.getFontMetrics().stringWidth(sub) / 2, H / 2 + 28 + offY,
                 new Color(0xcf, 0xd8, 0xe3));
    }
}
