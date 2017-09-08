package com.fdu.rissy;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

public class HelloWorld {

    public static void main(String[] args) {

        Flowable.just("Hello World").subscribe(System.out::println);

        Flowable.fromCallable(() -> {
            Thread.sleep(1000);
            return "Done";
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.single())
                .subscribe(System.out::println, Throwable::printStackTrace);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Flowable.range(1, 10)
                .observeOn(Schedulers.computation())
                .map(v -> v * v)
                .blockingSubscribe(System.out::println);

        Flowable.range(1, 10)
                .flatMap(v ->
                    Flowable.just(v)
                        .subscribeOn(Schedulers.computation())
                        .map(w -> w * w))
                .blockingSubscribe(System.out::println);
          Flowable.range(1, 10)
                  .parallel()
                  .runOn(Schedulers.computation())
                  .map(v -> v * v)
                  .sequential()
                  .blockingSubscribe(System.out::println);
    }
}
