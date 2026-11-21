package tannyjung.tanshugetrees_core.outside;

import tannyjung.tanshugetrees_core.Core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [MODS-CONFIG] 控制台日志限流 / Console log limiter.
 *
 * 症状: 服务器 console 被模组刷屏 (例如每读一次缺失的树数据、每 tick 一次失败的命令/方块实体读取,
 * 都打一整块 "Found an error reported from ..." 堆栈, 每秒几十上百行).
 *
 * 做法: 按 "类别 + 细节" 生成一个 key, 同一个 key 在窗口期内 (默认 30 秒) 只完整打一次,
 * 窗口内后续重复只累计计数, 下个窗口第一次打印时带上 "xN since last report".
 * 这样:
 *   - 首次出现的问题仍然完整可见 (不会为了安静而掩盖真 bug);
 *   - 同一个问题每秒报一万次也只占一行;
 *   - 不同的问题互不掩盖 (key 含细节).
 *
 * 另外提供 BoundedSet, 给"每个位置/每个 id 只提示一次"的场景用, 带容量上限防止长期跑图内存膨胀.
 */
public final class LogLimiter {

    private static final long WINDOW_MS = 30_000L;
    private static final int MAX_KEYS = 512;

    private static final class Entry {
        long window_start = System.currentTimeMillis();
        int suppressed = 0;
        boolean seen = false;
    }

    private static final Map<String, Entry> entries = new ConcurrentHashMap<>();

    private LogLimiter() {
    }

    /**
     * @return true = 本次应该真的打日志; false = 本窗口内已打过, 请静默.
     */
    public static boolean shouldLog(String key) {

        long now = System.currentTimeMillis();
        Entry entry = entries.get(key);

        if (entry == null) {

            if (entries.size() >= MAX_KEYS) {

                // 极多不同 key (例如把坐标拼进 key) 时防内存膨胀: 直接限流, 不再记录
                return false;

            }

            entry = new Entry();
            entries.put(key, entry);
            entry.seen = true;
            return true;

        }

        synchronized (entry) {

            if (now - entry.window_start < WINDOW_MS) {

                entry.suppressed++;
                return false;

            }

            entry.window_start = now;
            entry.suppressed = 0;
            return true;

        }

    }

    /** 本窗口内被静默掉的重复次数 (打印时附在行尾, 让人知道"其实刷了 N 次"). */
    public static int suppressedCount(String key) {

        Entry entry = entries.get(key);

        if (entry == null) {

            return 0;

        }

        synchronized (entry) {

            return entry.suppressed;

        }

    }

    /** 记录一条错误, 同一 key 每窗口只完整打一次. */
    public static void error(String key, String message, Throwable throwable) {

        int suppressed = suppressedCount(key);

        if (shouldLog(key) == false) {

            return;

        }

        String suffix = suppressed > 0 ? " (+" + suppressed + " more like this in the last " + (WINDOW_MS / 1000) + "s)" : "";

        if (throwable == null) {

            Core.logger.error(message + suffix);

        } else {

            Core.logger.error(message + suffix, throwable);

        }

    }

    public static void warn(String key, String message) {

        if (shouldLog(key) == false) {

            return;

        }

        int suppressed = suppressedCount(key);
        Core.logger.warn(message + (suppressed > 0 ? " (+" + suppressed + " more)" : ""));

    }

    /** 清空 (世界切换时调用, 让新世界的问题重新可见). */
    public static void clear() {

        entries.clear();

    }

    /**
     * 有容量上限的字符串集合: "每个位置/每个 id 只提示一次" 用.
     * 满了之后自动清空 (宁可重复提示一次, 也不要无限吃内存).
     */
    public static final class BoundedSet {

        private final int max_size;
        private final java.util.Set<String> set = java.util.concurrent.ConcurrentHashMap.newKeySet();

        public BoundedSet(int max_size) {

            this.max_size = Math.max(16, max_size);

        }

        /** @return true = 首次添加 (调用方应打印), false = 已存在或被容量清空后重新添加. */
        public boolean add(String value) {

            if (set.size() >= max_size) {

                set.clear();

            }

            return set.add(value);

        }

        public void clear() {

            set.clear();

        }

    }

}
