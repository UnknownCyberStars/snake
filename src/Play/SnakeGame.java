package Play;

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
 * 贪吃蛇 (Snake)
 *
 * 特性:
 *  - 经典有界棋盘: 蛇在固定窗口内移动, 窗口静止不跟随
 *  - 穿行模式: 蛇移动到窗口边缘时不会死亡, 而是从对侧边界出现
 *  - 只有蛇撞到自己才会死亡
 *
 * 操作: 方向键/WASD 转向, 空格 暂停, R 重新开始, ESC 退出
 */
public class SnakeGame extends JPanel {

    // ---------- 常量 ----------
    private static final int CELL = 20;             // 格子像素
    private static final int COLS = 40, ROWS = 30;  // 棋盘格子数
    private static final int W = COLS * CELL;       // 画布宽
    private static final int H = ROWS * CELL;       // 画布高
    private static final int TICK_START = 140;      // 初始步进毫秒
    private static final int TICK_MIN = 70;         // 最快步进毫秒
    private static final int FRAME_MS = 16;         // 渲染帧间隔(约60fps)

    private enum Dir { UP, DOWN, LEFT, RIGHT }
    private enum State { LOGIN, READY, RUNNING, PAUSED, GAME_OVER }

    // ---------- 游戏状态 ----------
    private final List<Point> body = new ArrayList<>();  // 蛇身(棋盘格坐标, 头在 index 0)
    private Dir dir = Dir.RIGHT;
    private Dir pendingDir = Dir.RIGHT;
    private Point food = new Point(5, 0);
    private State state = State.READY;
    private int score = 0;
    private final Random rnd = new Random();
    private final Timer logicTimer;                // 逻辑步进
    private final Timer renderTimer;               // 渲染(食物脉动动画等)

    // ---------- 按钮 ----------
    private JButton restartBtn;
    private JButton menuBtn;

    // ---------- 登录界面 ----------
    private LoginPanel loginPanel;          // 内嵌登录面板(替代原开始界面)
    private String currentUser;             // 当前登录用户名

    // ---------- 时间统计 ----------
    private long gameStartTime;          // 游戏开始（或恢复）时的系统时间
    private long totalPausedTime = 0;    // 累计暂停时间（毫秒）
    private long pauseStartTime = 0;     // 进入暂停时的系统时间
    private long finalElapsedSeconds = 0; // 游戏结束时保存的总秒数

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("贪吃蛇 — ESC 退出");
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            SnakeGame game = new SnakeGame();
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.start();
        });
    }

    private SnakeGame() {
        setPreferredSize(new Dimension(W, H));
        setBackground(new Color(0x13, 0x17, 0x22));
        setFocusable(true);
        setLayout(null);
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { onKey(e.getKeyCode()); }
        });
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
        createButtons();
        logicTimer = new Timer(TICK_START, e -> step());
        renderTimer = new Timer(FRAME_MS, e -> repaint());
        resetGame();
        // 初始进入登录界面(替代原 READY 开始界面)
        loginPanel = new LoginPanel(this::onLoginSuccess);
        loginPanel.setBounds((W - 420) / 2, (H - 450) / 2, 420, 450);
        loginPanel.setVisible(true);
        add(loginPanel);
        state = State.LOGIN;
    }

    private void start() {
        logicTimer.start();
        renderTimer.start();
        requestFocusInWindow();
        SwingUtilities.invokeLater(loginPanel::focusField);
    }

    // ================= 登录 =================

    /** 登录成功: 隐藏登录面板, 进入 READY 等待开始 */
    private void onLoginSuccess(String name) {
        currentUser = name;
        loginPanel.setVisible(false);
        resetGame();
        requestFocusInWindow();
    }

    /** 返回主菜单(登录界面) */
    private void backToLogin() {
        hideButtons();
        state = State.LOGIN;
        loginPanel.resetFields();
        loginPanel.setVisible(true);
        loginPanel.focusField();
    }

    // ================= 逻辑 =================

    /** 逻辑步进: 蛇移动一格 */
    private void step() {
        if (state != State.RUNNING) return;
        if (!isOpposite(pendingDir, dir)) dir = pendingDir;   // 应用缓存方向, 禁止180°掉头
        Point head = body.get(0);
        Point nh = wrap(move(head, dir));        // 越界后从对侧边界穿行回来

        // 自撞检测: 若不生长, 尾格本步会移开, 允许进入
        int safeLen = nh.equals(food) ? body.size() : body.size() - 1;
        for (int i = 0; i < safeLen; i++) {
            if (body.get(i).equals(nh)) {
                finalElapsedSeconds = getElapsedSeconds();  // 保存最终时间
                state = State.GAME_OVER;
                showButtons();
                saveScoreToDatabase();  // 预留数据库接口
                return;
            }
        }

        body.add(0, nh);
        if (nh.equals(food)) {
            score++;
            logicTimer.setDelay(Math.max(TICK_MIN, TICK_START - score * 2));  // 越吃越快
            spawnFood();
        } else {
            body.remove(body.size() - 1);          // 未吃到: 尾巴跟上
        }
    }

    /** 以棋盘中央为原点重新开局 */
    private void restart(Dir startDir) {
        int cx = COLS / 2, cy = ROWS / 2;
        int ox = 0, oy = 0;
        switch (startDir) {
            case UP: oy = 1; break;
            case DOWN: oy = -1; break;
            case LEFT: ox = 1; break;
            case RIGHT: ox = -1; break;
        }
        body.clear();
        body.add(new Point(cx, cy));
        body.add(new Point(cx + ox, cy + oy));
        body.add(new Point(cx + ox * 2, cy + oy * 2));
        dir = startDir;
        pendingDir = startDir;
        score = 0;
        state = State.RUNNING;
        logicTimer.setDelay(TICK_START);
        spawnFood();
        gameStartTime = System.currentTimeMillis();//时间初始化
        totalPausedTime = 0;
        pauseStartTime = 0;
        finalElapsedSeconds = 0;
        hideButtons();              //隐藏按钮
        requestFocusInWindow();     //重新获得键盘焦点
    }

    /** 初始重置: 棋盘中央, 朝右, 等待开始 */
    private void resetGame() {
        body.clear();
        int cx = COLS / 2, cy = ROWS / 2;
        body.add(new Point(cx, cy));
        body.add(new Point(cx - 1, cy));
        body.add(new Point(cx - 2, cy));
        dir = Dir.RIGHT;
        pendingDir = Dir.RIGHT;
        score = 0;
        state = State.READY;
        spawnFood();
        //时间初始化
        gameStartTime = System.currentTimeMillis();
        totalPausedTime = 0;
        pauseStartTime = 0;
        finalElapsedSeconds = 0;
        hideButtons();
    }

    /** 在棋盘上随机生成食物(保证不出现在蛇身上) */
    private void spawnFood() {
        for (int tries = 0; tries < 5000; tries++) {
            Point p = new Point(rnd.nextInt(COLS), rnd.nextInt(ROWS));
            if (!onBody(p)) { food = p; return; }
        }
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                Point p = new Point(x, y);
                if (!onBody(p)) { food = p; return; }
            }
        }
    }

    private boolean onBody(Point p) {
        for (Point s : body) if (s.equals(p)) return true;
        return false;
    }

    private void onKey(int code) {
        if (code == KeyEvent.VK_ESCAPE) { System.exit(0); return; }
        if (state == State.LOGIN) return;   // 登录界面按键交给登录面板处理
        if (code == KeyEvent.VK_R) { restart(Dir.RIGHT); return; }
        if (code == KeyEvent.VK_SPACE) {
            if (state == State.RUNNING) {
                state = State.PAUSED;
                pauseStartTime = System.currentTimeMillis();  //记录暂停开始
            } else if (state == State.PAUSED) {
                state = State.RUNNING;
                totalPausedTime += System.currentTimeMillis() - pauseStartTime;  //累加暂停时长
            }
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

    /** 把越界坐标绕回棋盘对侧 (穿行模式) */
    private static Point wrap(Point p) {
        int x = p.x % COLS;
        if (x < 0) x += COLS;
        int y = p.y % ROWS;
        if (y < 0) y += ROWS;
        return new Point(x, y);
    }

    private static boolean isOpposite(Dir a, Dir b) {
        return (a == Dir.UP && b == Dir.DOWN) || (a == Dir.DOWN && b == Dir.UP)
            || (a == Dir.LEFT && b == Dir.RIGHT) || (a == Dir.RIGHT && b == Dir.LEFT);
    }

    private long getElapsedSeconds() {
        if (state == State.GAME_OVER) {
            return finalElapsedSeconds;
        }
        long current = System.currentTimeMillis();
        long elapsed = current - gameStartTime - totalPausedTime;
        return elapsed / 1000;
    }
    private void saveScoreToDatabase() {
        // =============================================
        //预留接口：等数据库完成后在此处接入
        // 需要的参数：score（得分），finalElapsedSeconds（用时秒数）
        // =============================================

        // 目前先打印到控制台，方便测试
        System.out.println("📝 准备保存到数据库:");
        System.out.println("   得分: " + score);
        System.out.println("   用时: " + finalElapsedSeconds + " 秒");
        System.out.println("   时间: " + String.format("%02d:%02d",
                finalElapsedSeconds / 60, finalElapsedSeconds % 60));

        // =============================================
        // 以后替换为：
        // DatabaseHelper.saveRecord(score, finalElapsedSeconds);
        // =============================================
    }

    // ================= 渲染 =================
    /** 创建并配置两个按钮 */
    private void createButtons() {
        // 重新开始按钮
        restartBtn = new JButton("重新开始");
        restartBtn.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        restartBtn.setForeground(Color.WHITE);
        restartBtn.setBackground(new Color(60, 80, 100));
        // ... 样式设置 ...
        restartBtn.setVisible(false);
        restartBtn.addActionListener(e -> {
            restart(Dir.RIGHT);
            hideButtons();
        });

        // 返回主菜单按钮
        menuBtn = new JButton("返回主菜单");
        // ... 样式设置 .../
        menuBtn.setVisible(false);
        menuBtn.addActionListener(e -> backToLogin());   // 返回主菜单(登录界面)

        // 放置位置
        int btnW = 110, btnH = 40;
        int gap = 20;
        int totalW = btnW * 2 + gap;
        int startX = (W - totalW) / 2;
        int y = H / 2 + 70;

        restartBtn.setBounds(startX, y, btnW, btnH);
        menuBtn.setBounds(startX + btnW + gap, y, btnW, btnH);

        add(restartBtn);
        add(menuBtn);
    }

    private void showButtons() {
        SwingUtilities.invokeLater(() -> {
            restartBtn.setVisible(true);
            menuBtn.setVisible(true);
            restartBtn.setFocusable(false);
            menuBtn.setFocusable(false);
        });
    }

    private void hideButtons() {
        SwingUtilities.invokeLater(() -> {
            restartBtn.setVisible(false);
            menuBtn.setVisible(false);
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (state == State.LOGIN) {      // 登录界面: 仅绘制棋盘背景, 登录面板浮于其上
            drawCheckerboard(g2);
            g2.setColor(new Color(0x2a, 0x35, 0x4a));
            g2.drawRect(0, 0, W - 1, H - 1);
            g2.dispose();
            return;
        }
        drawCheckerboard(g2);
        drawFood(g2);
        drawSnake(g2);
        g2.setColor(new Color(0x2a, 0x35, 0x4a));
        g2.drawRect(0, 0, W - 1, H - 1);
        drawHud(g2);
        drawOverlay(g2);
        g2.dispose();
    }

    /** 棋盘格背景 */
    private void drawCheckerboard(Graphics2D g2) {
        for (int cy = 0; cy < ROWS; cy++) {
            for (int cx = 0; cx < COLS; cx++) {
                g2.setColor(((cx + cy) & 1) == 0 ? new Color(0x16, 0x1b, 0x27) : new Color(0x13, 0x17, 0x22));
                g2.fillRect(cx * CELL, cy * CELL, CELL, CELL);
            }
        }
    }

    private void drawFood(Graphics2D g2) {
        double fx = food.x * CELL + CELL / 2.0;
        double fy = food.y * CELL + CELL / 2.0;
        double pulse = 0.8 + 0.2 * Math.sin(System.nanoTime() / 3.0e8);
        int r = (int) Math.round(CELL * 0.42 * pulse);
        g2.setColor(new Color(0xff, 0x4d, 0x4d));
        g2.fillOval((int) fx - r, (int) fy - r, r * 2, r * 2);
        g2.setColor(new Color(0xff, 0xd0, 0xd0));
        g2.fillOval((int) fx - r / 3, (int) fy - r / 3, r * 2 / 3, r * 2 / 3);
    }

    private void drawSnake(Graphics2D g2) {
        int n = body.size();
        for (int i = n - 1; i >= 0; i--) {          // 先画尾巴, 头在最上层
            Point bp = body.get(i);
            int rx = bp.x * CELL, ry = bp.y * CELL;
            int ins = Math.max(2, CELL / 5);
            int sz = CELL - ins * 2;

            if (i == 0) {
                // 蛇头: 亮绿色 + 眼睛
                g2.setColor(new Color(0x6e, 0xf2, 0x9a));
                g2.fillRoundRect(rx + ins, ry + ins, sz, sz, 12, 12);
                g2.setColor(new Color(0x2a, 0x8f, 0x4e));
                g2.drawRoundRect(rx + ins, ry + ins, sz, sz, 12, 12);
                drawEyes(g2, rx, ry);
            } else {
                // 蛇身: 由尾到头的绿色渐变
                double t = (double) i / n;
                g2.setColor(new Color(40 + (int) (t * 20), 170 + (int) (t * 55), 60 + (int) (t * 20)));
                g2.fillRoundRect(rx + ins, ry + ins, sz, sz, 10, 10);
                g2.setColor(new Color(0x0e, 0x5a, 0x33));
                g2.drawRoundRect(rx + ins, ry + ins, sz, sz, 10, 10);
            }
        }
    }

    /** 蛇头眼睛, 朝向移动方向 */
    private void drawEyes(Graphics2D g2, int rx, int ry) {
        double dx = 0, dy = 0;
        switch (dir) {
            case UP: dy = -1; break;
            case DOWN: dy = 1; break;
            case LEFT: dx = -1; break;
            case RIGHT: dx = 1; break;
        }
        double sx = rx + CELL / 2.0, sy = ry + CELL / 2.0;
        double nx = dx, ny = dy;
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

    private void drawHud(Graphics2D g2) {
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        String scoreStr = "得分 " + score + "    长度 " + body.size();
        drawText(g2, scoreStr, 12, 24, Color.WHITE);
        //时间显示
        long elapsed = getElapsedSeconds();
        String timeStr = String.format("%02d:%02d", elapsed / 60, elapsed % 60);
        FontMetrics fm = g2.getFontMetrics();
        int timeWidth = fm.stringWidth("时间 " + timeStr);
        drawText(g2, "时间 " + timeStr, W - timeWidth - 12, 24, Color.WHITE);
        //帮助信息
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        String help = "方向键/WASD 转向 · 空格 暂停 · R 重开 · ESC 退出";
        drawText(g2, help, W / 2 - g2.getFontMetrics().stringWidth(help) / 2, H - 14,
                 new Color(0x8a, 0x94, 0xa3));
    }

    private void drawText(Graphics2D g2, String s, int x, int y, Color c) {
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(s, x + 1, y + 1);
        g2.setColor(c);
        g2.drawString(s, x, y);
    }

    private void drawOverlay(Graphics2D g2) {
        String msg = null, sub = null;
        if (state == State.READY) {
            msg = "贪吃蛇";
            sub = "按任意方向键开始 — 穿过边界会从对面出现";
        } else if (state == State.PAUSED) {
            msg = "已暂停";
            sub = "按空格继续";
        } else if (state == State.GAME_OVER) {
            long totalSec = finalElapsedSeconds;
            String timeStr = String.format("%02d:%02d", totalSec / 60, totalSec % 60);
            msg = "游戏结束 · 得分 " + score;
            sub = "总用时 " + timeStr + " · 玩家 " + (currentUser == null ? "" : currentUser)
                    + "  ·  点击下方按钮继续";
        }
        if (msg == null) return;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, W, H);
        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        drawText(g2, msg, W / 2 - g2.getFontMetrics().stringWidth(msg) / 2, H / 2 - 8, Color.WHITE);
        g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        drawText(g2, sub, W / 2 - g2.getFontMetrics().stringWidth(sub) / 2, H / 2 + 28,
                 new Color(0xcf, 0xd8, 0xe3));
    }
}
