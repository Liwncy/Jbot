package x.ovo.jbot.impl.message;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hutool.core.thread.BlockPolicy;
import org.dromara.hutool.core.thread.ExecutorBuilder;
import org.dromara.hutool.core.thread.ThreadUtil;
import x.ovo.jbot.core.Context;
import x.ovo.jbot.core.command.Command;
import x.ovo.jbot.core.event.ExceptionEvent;
import x.ovo.jbot.core.event.MessageEvent;
import x.ovo.jbot.core.message.MessageManager;
import x.ovo.jbot.core.message.entity.Message;
import x.ovo.jbot.core.message.entity.SentMessage;
import x.ovo.jbot.core.message.entity.TextMessage;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认消息管理器
 *
 * @author ovo created on 2025/02/23.
 */
@Slf4j
public class DefaultMessageManager implements MessageManager {

    private static final ReentrantLock SEND_LOCK = new ReentrantLock();
    private static final ReentrantLock RECEIVE_LOCK = new ReentrantLock();
    private static final Condition SEND_SINGLE = SEND_LOCK.newCondition();
    private static final Condition RECEIVE_SINGLE = RECEIVE_LOCK.newCondition();
    private static final ConcurrentLinkedQueue<Message> SEND_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Message> RECEIVE_QUEUE = new ConcurrentLinkedQueue<>();

    private ExecutorService executor;
    private boolean flag = true;

    @Override
    public Future<SentMessage> send(Message message) {
        // todo 如果消息发送间隔太短，可能会发送失败
        return Context.get().getAdapter().getMessageService().send(message)
                .onSuccess(m -> message.onSend(true).accept(log))
                .onFailure(t -> message.onSend(false).accept(log));
    }

    @Override
    public Future<Void> onInit() throws Exception {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        this.executor = ExecutorBuilder.of()
                .setCorePoolSize(cpuNum)
                .setMaxPoolSize(cpuNum * 10)
                .setThreadFactory(ThreadUtil.newNamedThreadFactory("consumer-", true))
                .setWorkQueue(new LinkedBlockingQueue<>(50))
                .setHandler(new BlockPolicy())
                .build();
        log.info("消息管理器初始化完成");
        this.handle();
        return MessageManager.super.onInit();
    }

    @Override
    public Future<Void> onDestroy() throws Exception {
        return Future.future(promise -> {
            this.flag = false;
            this.executor.shutdown();
            promise.complete();
        });
    }

    @Override
    public void addReceive(Message message) {
        RECEIVE_LOCK.lock();
        try {
            if (RECEIVE_QUEUE.size() <= 1000) {
                RECEIVE_QUEUE.add(message);
                RECEIVE_SINGLE.signalAll();
                log.debug("消息队列添加成功，当前队列长度：{}", RECEIVE_QUEUE.size());
            }
        } finally {
            RECEIVE_LOCK.unlock();
        }
    }

    @Override
    public void addAllReceive(Collection<Message> messages) {
        RECEIVE_LOCK.lock();
        try {
            if (RECEIVE_QUEUE.size() < 1000) {
                RECEIVE_QUEUE.addAll(messages);
                RECEIVE_SINGLE.signalAll();
            }
        } finally {
            RECEIVE_LOCK.unlock();
        }
    }

    @Override
    public void handle() {
        ThreadUtil.newThread(() -> {
            log.info("启动消息处理线程");
            while (this.flag) {
                try {
                    this.loop();
                } catch (Exception e) {
                    log.error("消息处理异常：{}", e.getMessage());
                    ExceptionEvent.of(e).publish();
                }
            }
        }, "message-consumer").start();
    }

    private void loop() {
        // 队列为空时，等待
        if (RECEIVE_QUEUE.isEmpty()) {
            RECEIVE_LOCK.lock();
            try {
                while (RECEIVE_QUEUE.isEmpty()) RECEIVE_SINGLE.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("消息队列等待异常：{}", e.getMessage());
            } finally {
                RECEIVE_LOCK.unlock();
            }
        }
        var message = RECEIVE_QUEUE.poll();
        if (Objects.isNull(message)) return;
        if (Instant.now().toEpochMilli() / 1000 - message.getCreateTime() > 60) return;
        log.info(message.formatString());
        this.executor.execute(() -> {
            // 如果是指令，则执行指令
            if (message instanceof TextMessage && Command.isCommand(message.getContent())) {
                Command.of(message).execute();
                return;
            }
            // 发送事件
            MessageEvent.of(message).publish();
        });
    }

}
