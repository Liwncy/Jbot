package x.ovo.jbot.plugin.mmorpg.util;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.*;

/**
 * Vert.x MySQL 数据库工具类（单例模式）
 */
public class MySQLClient {

    private static MySQLClient instance;
    private final Pool pool;
    private final Vertx vertx;

    // 私有构造方法
    private MySQLClient(Vertx vertx, JsonObject config) {
        this.vertx = vertx;

        // 解析配置
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setHost(config.getString("host", "localhost"))
                .setPort(config.getInteger("port", 3306))
                .setDatabase(config.getString("database", "mmorpg"))
                .setUser(config.getString("username", "root"))
                .setPassword(config.getString("password", "liwncy@123"))
                .setCharset(config.getString("charset", "utf8mb4"));
//                .setConnectTimeout(config.getInteger("connectTimeout", 5000));

        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(config.getInteger("maxPoolSize", 5))
                .setIdleTimeout(config.getInteger("idleTimeout", 1800));

        // 创建连接池
        this.pool = Pool.pool(vertx, connectOptions, poolOptions);
    }

    /**
     * 初始化工具类（需先调用）
     * @param vertx Vertx实例
     * @param config 数据库配置
     */
    public static synchronized void init(Vertx vertx, JsonObject config) {
        if (instance == null) {
            instance = new MySQLClient(vertx, config);
        }
    }

    /**
     * 获取工具类实例
     */
    public static MySQLClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MySQLClient not initialized!");
        }
        return instance;
    }

    /**
     * 获取连接池实例
     */
    public Pool getPool() {
        return pool;
    }

    /**
     * 执行查询（无参数）
     */
    public Future<RowSet<Row>> query(String sql) {
        return pool.query(sql).execute();
    }

    /**
     * 执行带参数的查询
     */
    public Future<RowSet<Row>> queryWithParams(String sql, Tuple params) {
        return pool.preparedQuery(sql).execute(params);
    }

    /**
     * 执行更新操作
     */
    public Future<RowSet<Row>> update(String sql, Tuple params) {
        return queryWithParams(sql, params);
    }

    /**
     * 开启事务
     */
    public Future<Transaction> beginTransaction() {
        return pool.getConnection()
                .compose(conn -> conn.begin().map(transaction -> {
                    transaction.completion().onComplete(v -> conn.close());
                    return transaction;
                }));
    }

    /**
     * 提交事务
     */
    public Future<Void> commit(Transaction transaction) {
        return transaction.commit();
    }

    /**
     * 回滚事务
     */
    public Future<Void> rollback(Transaction transaction) {
        return transaction.rollback();
    }

    /**
     * 关闭连接池
     */
    public Future<Void> close() {
        return pool.close();
    }
}