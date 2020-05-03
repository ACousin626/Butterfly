package com.simonalong.buffterfly.sample;

import com.simonalong.butterfly.sequence.ButterflyConfig;
import com.simonalong.butterfly.sequence.ButterflyIdGenerator;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author shizi
 * @since 2020/5/3 6:33 PM
 */
public class BaseTest {

    public static ButterflyConfig config;

    @SuppressWarnings("all")
    public ExecutorService executorService = Executors.newFixedThreadPool(1000, new ThreadFactory() {
        private AtomicInteger count = new AtomicInteger(0);

        @Override
        @SuppressWarnings("all")
        public Thread newThread(Runnable r) {
            return new Thread(r, "test_" + count.getAndIncrement());
        }
    });


    public void show(Object obj) {
        if (null == obj) {
            show("obj is null");
        } else {
            System.out.println(obj.toString());
        }
    }

    /**
     * 基本测试
     */
    public void baseRun() {
        ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
        generator.addNamespaces("test1", "test2");

        // 测试test1
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test1")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test1")));

        // 测试test2
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
        show(ButterflyIdGenerator.parseUid(generator.getUUid("test2")));
    }

    /**
     * 持续的低qps的压测，则这个QPS还是比较低，完全满足业务需求
     */
    @SneakyThrows
    public void lowPressRun() {
        ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
        generator.addNamespaces("biz0");
        int count = 10;
        int callNum = 10;
        int concurrentNum = 100;

        for (int i = 0; i < count; i++) {
            generateFun(generator, "biz0", callNum, concurrentNum);
            Thread.sleep(1000);
        }
    }

    /**
     * 低qps一段时间后到高QPS，可以支撑更高，但是一旦持续的高并发，则后面会慢慢降下来
     */
    @SneakyThrows
    public void lowToHighPressRun() {
        ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
        generator.addNamespaces("biz0");
        int count = 10;
        int callNum = 10;
        int concurrentNum = 100;

        for (int i = 0; i < count; i++) {
            generateFun(generator, "biz0", callNum + i * i * 100, concurrentNum + i * 10 * i);
            Thread.sleep(1000);
        }
    }

    /**
     * 持续的高QPS，则只能达到最高的理论值（51.2w/s）
     */
    @SneakyThrows
    public void highPressRun() {
        ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
        generator.addNamespaces("biz0");
        int count = 10;
        int callNum = 1000;
        int concurrentNum = 10000;

        for (int i = 0; i < count; i++) {
            generateFun(generator, "biz0", callNum, concurrentNum);
            Thread.sleep(1000);
        }
    }

    /**
     * 多业务的压测：低qps一段时间后到高QPS，可以支撑更高
     */
    @SneakyThrows
    public void lowToHighMultiBizPressRun() {
        ButterflyIdGenerator generator = ButterflyIdGenerator.getInstance(config);
        generator.addNamespaces("biz0", "biz1");
        int count = 10;
        int callNum = 10;
        int concurrentNum = 100;

        for (int i = 0; i < count; i++) {
            generateFun(generator, "biz0", callNum + i * i * 100, concurrentNum + i * 10 * i);
            generateFun(generator, "biz1", callNum + i * i * 100, concurrentNum + i * 10 * i);
            Thread.sleep(1000);
        }
    }


    /**
     * 每次调用的统计，统计其中的QPS
     *
     * @param generator     id生成器
     * @param biz           业务名
     * @param callNum       调用多少次
     * @param concurrentNum 每次的并发量
     */
    @SneakyThrows
    public void generateFun(ButterflyIdGenerator generator, String biz, Integer callNum, Integer concurrentNum) {
        long start = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(callNum * concurrentNum);
        for (int index = 0; index < concurrentNum; index++) {
            executorService.execute(() -> {
                for (int j = 0; j < callNum; j++) {
                    generator.getUUid(biz);
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        show("biz=" + biz + ", qps = " + (callNum * concurrentNum) / (duration * 10.0) + "单位（w/s）");
    }
}
