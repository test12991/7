/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2030 The XdagJ Developers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.xdag.net.message;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.xdag.config.Config;
import io.xdag.net.message.p2p.DisconnectMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageQueue {
    public static final ScheduledExecutorService timer = Executors.newScheduledThreadPool(4, new ThreadFactory() {
        private final AtomicInteger cnt = new AtomicInteger(0);

        public Thread newThread(Runnable r) {
            return new Thread(r, "msg-" + cnt.getAndIncrement());
        }
    });
    private final Config config;

    private final Queue<Message> queue = new ConcurrentLinkedQueue<>();
    private final Queue<Message> prioritized = new ConcurrentLinkedQueue<>();
    private ChannelHandlerContext ctx;
    private ScheduledFuture<?> timerTask;

    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public MessageQueue(Config config) {
        this.config = config;
    }

    public synchronized void activate(ChannelHandlerContext ctx) {
        this.ctx = ctx;
        timerTask = timer.scheduleAtFixedRate(
                () -> {
                    try {
                        nudgeQueue();
                    } catch (Throwable t) {
                        log.error("Exception in MessageQueue", t);
                    }
                },
                10, 10, TimeUnit.MILLISECONDS);
    }

    public synchronized void deactivate() {
        this.timerTask.cancel(false);
    }

    public boolean isIdle() {
        return size() == 0;
    }

    public void disconnect(ReasonCode code) {
        log.debug("Actively closing the connection: reason = {}", code);

        // avoid repeating close requests
        if (isClosed.compareAndSet(false, true)) {
            ctx.writeAndFlush(new DisconnectMessage(code)).addListener((ChannelFutureListener) future -> ctx.close());
        }
    }

    public boolean sendMessage(Message msg) {
        if (size() >= config.getNodeSpec().getNetMaxMessageQueueSize()) {
            log.debug("message queue is full, size:{}", size());
            disconnect(ReasonCode.MESSAGE_QUEUE_FULL);
            return false;
        }

        if (config.getNodeSpec().getNetPrioritizedMessages().contains(msg.getCode())) {
            prioritized.add(msg);
        } else {
            queue.add(msg);
        }
        return true;
    }

    public int size() {
        return queue.size() + prioritized.size();
    }

    private void nudgeQueue() {
        int n = Math.min(5, size());
        if (n == 0) {
            return;
        }
        // write out n messages
        for (int i = 0; i < n; i++) {
            Message msg = !prioritized.isEmpty() ? prioritized.poll() : queue.poll();

            log.trace("Wiring message: {}", msg);
            ctx.write(msg).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
        }
        ctx.flush();
    }
}
