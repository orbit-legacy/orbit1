/*
 Copyright (C) 2015 - 2019 Electronic Arts Inc.  All rights reserved.
 This file is part of the Orbit Project <https://www.orbit.cloud>.
 See license in LICENSE.
 */

package cloud.orbit.benchmark

import cloud.orbit.core.actor.AbstractActor
import cloud.orbit.core.actor.ActorWithStringKey
import cloud.orbit.core.actor.createProxy
import cloud.orbit.runtime.stage.Stage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OperationsPerInvocation
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

private const val REQUESTS_PER_BATCH = 500

interface BasicBenchmarkActor : ActorWithStringKey {
    fun echo(string: String): Deferred<String>
}

class BasicBenchmarkActorImpl : BasicBenchmarkActor, AbstractActor() {
    override fun echo(string: String): Deferred<String> {
        return CompletableDeferred(string)
    }
}

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
open class ActorBenchmarks {
    var stage: Stage? = null

    @Setup
    fun setup() {
        stage = Stage()
        runBlocking {
            stage!!.start().await()
        }
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    fun echoThroughputBenchmark() {
        batchIteration()
    }

    @Benchmark
    @Threads(8)
    @OperationsPerInvocation(REQUESTS_PER_BATCH)
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    fun echoBenchmark() {
        batchIteration()
    }

    private fun batchIteration() {
        val myList = ArrayList<Deferred<String>>(REQUESTS_PER_BATCH)
        repeat(REQUESTS_PER_BATCH) {
            val actor = stage!!.actorProxyFactory
                .createProxy<BasicBenchmarkActor>(
                    ThreadLocalRandom.current().nextInt(1000)
                        .toString()
                )
            myList.add(actor.echo("Joe"))
        }
        runBlocking {
            myList.awaitAll()
        }
    }

    @TearDown
    fun teardown() {
        runBlocking {
            stage!!.stop().await()
        }
    }
}