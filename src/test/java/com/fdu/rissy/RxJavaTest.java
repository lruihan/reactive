package com.fdu.rissy;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class RxJavaTest {

    private static final List<String> LETTERS = Arrays.asList("A", "B", "C", "D", "E");

    @Rule
    public final ImmediateSchedulersRule schedulers = new ImmediateSchedulersRule();

    @Rule
    public final TestSchedulerRule testSchedulerRule = new TestSchedulerRule();

    @Test
    public void testInSameThread() {
        List<String> results = new ArrayList<>();
        Observable<String> observable = Observable.fromIterable(LETTERS).zipWith(Observable.range(1, Integer.MAX_VALUE),
                (string, index) -> index + "-" + string);
        observable.subscribe(results::add);

        assertThat(results, notNullValue());
        assertThat(results, hasSize(5));
        assertThat(results, hasItems("1-A", "2-B", "3-C", "4-D", "5-E"));
    }

    @Test
    public void testUsingTestObserver() {
        TestObserver<String> observer = new TestObserver<>();

        Observable<String> observable = Observable.fromIterable(LETTERS)
                                                    .zipWith(
                                                            Observable.range(1, Integer.MAX_VALUE),
                                                            ((string, index) -> index + "-" + string)
                                                    );
        observable.subscribe(observer);

        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(5);
        assertThat(observer.values(), hasItem("4-D"));
    }

    @Test
    public void testException() {
        TestObserver<String> observer = new TestObserver<>();
        Exception exception = new RuntimeException("oops!");

        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> index + "-" + string)
                .concatWith(Observable.error(exception));

        observable.subscribe(observer);

        observer.assertError(exception);
        observer.assertNotComplete();
    }

    @Test
    public void testComputationSchedulerAwait() {
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> index + "-" + string);
        observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);

        await().timeout(2, TimeUnit.SECONDS)
                .until(observer::valueCount, equalTo(5));

        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem("4-D"));
    }

    @Test
    public void testUsingBlockingCall() {
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> index + "-" + string);
        Iterable<String> results = observable
                .subscribeOn(Schedulers.computation())
                .blockingIterable();

        assertThat(results, notNullValue());
        assertThat(results, iterableWithSize(5));
        assertThat(results, hasItem("1-A"));
    }


    @Test
    public void testComputationScheduler() {
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> index + "-" + string);
        observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);
        observer.awaitTerminalEvent(2, TimeUnit.SECONDS);

        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem("4-D"));
    }

    @Test
    public void testRxJavaPluginsWithImmediateScheduler() {
        RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.range(1, Integer.MAX_VALUE),
                        (string, index) -> index + "-" + string);
        try {
            observable.subscribeOn(Schedulers.computation())
                    .subscribe(observer);
            observer.assertComplete();
            observer.assertNoErrors();
            observer.assertValueCount(5);

            assertThat(observer.values(), hasItem("4-D"));
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void testUsingImmediateSchedulersRule() {
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(LETTERS)
                                            .zipWith(Observable.range(1, Integer.MAX_VALUE),
                                                    (string, index) -> index + "-" + string);
        observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);

        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(5);
        assertThat(observer.values(), hasItem("3-C"));
    }

    @Test
    public void testUsingTestScheduler() {
        TestScheduler scheduler = new TestScheduler();
        TestObserver<String> observer = new TestObserver<>();

        Observable<Long> tick = Observable.interval(1, TimeUnit.SECONDS, scheduler);
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(tick, (string, index) -> index + "-" + string);
        observable.subscribeOn(scheduler)
                .subscribe(observer);

        observer.assertNoValues();
        observer.assertNotComplete();

        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        observer.assertValues("0-A");

        scheduler.advanceTimeTo(5, TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(5);
    }

    @Test
    public void testUsingTestSchedulerRule() {
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable.fromIterable(LETTERS)
                .zipWith(Observable.interval(1, TimeUnit.SECONDS),
                        (string, index) -> index + "-" + string);
        observable.subscribeOn(Schedulers.computation())
                .subscribe(observer);

        observer.assertNoValues();
        observer.assertNotComplete();

        testSchedulerRule.getTestScheduler().advanceTimeBy(1, TimeUnit.SECONDS);

        observer.assertNoErrors();
        observer.assertValueCount(1);
        observer.assertValues("0-A");

        testSchedulerRule.getTestScheduler().advanceTimeTo(5, TimeUnit.SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(5);
    }

    private static class ImmediateSchedulersRule implements TestRule {

        @Override
        public Statement apply(Statement statement, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
                    RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
                    RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
                    try {
                        statement.evaluate();
                    } finally {
                        RxJavaPlugins.reset();
                    }
                }
            };
        }
    }

    private static class TestSchedulerRule implements TestRule {
        private final TestScheduler testScheduler = new TestScheduler();

        public TestScheduler getTestScheduler() {
            return testScheduler;
        }

        @Override
        public Statement apply(Statement statement, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
                    RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
                    RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> testScheduler);

                    try {
                        statement.evaluate();
                    } finally {
                        RxJavaPlugins.reset();
                    }
                }
            };
        }
    }
}
