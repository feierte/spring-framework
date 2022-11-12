package org.springframework.demo;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

/**
 * @author Jie Zhao
 * @date 2020/12/26 9:42
 */
public class Main {

	private static int i = 1;
	private static BlockingQueue<String> blockingQueue1 = new ArrayBlockingQueue<>(1); // 用于线程A与线程B之间协同
	private static BlockingQueue<String> blockingQueue2 = new ArrayBlockingQueue<>(1); // 用于线程B与线程C之间协同
	private static BlockingQueue<String> blockingQueue3 = new ArrayBlockingQueue<>(1); // 用于线程C与线程D之间协同

	// 控制线程打印次数，这里是打印102次
	private static CountDownLatch countDownLatch = new CountDownLatch(102);


	public static void main(String[] args) throws Exception {

		Thread threadA = new Thread(new Runnable() {
					public void run() {
						while (blockingQueue1.isEmpty()) {
							System.out.print("a");
							blockingQueue1.offer("ok");
						}
					}
				}, "线程A");
		Thread threadB = new Thread(new Runnable() {
					public void run() {
						while (!blockingQueue1.isEmpty()) {
							System.out.print("l");
							String str = blockingQueue1.poll();
							blockingQueue2.offer(str);

						}
					}
				}, "线程B");
		Thread threadC = new Thread(new Runnable() {
					public void run() {
						while (!blockingQueue2.isEmpty()) {
							System.out.print("i");
							String str = blockingQueue2.poll();
							blockingQueue3.offer(str);
						}
					}
				}, "线程C");
		Thread threadD = new Thread(new Runnable() {
					public void run() {
						while (!blockingQueue3.isEmpty()) {
							System.out.print(i++);
							countDownLatch.countDown();
						}
					}
				}, "线程D");

		threadA.start();
		threadB.start();
		threadC.start();
		threadD.start();

		countDownLatch.await();
	}
}
