/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.util.promise;

import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import org.osgi.util.function.Function;
import org.osgi.util.function.Predicate;

/**
 * Promise implementation.
 * 
 * <p>
 * This class is not used directly by clients. Clients should use
 * {@link Deferred} to create a resolvable {@link Promise}.
 * 
 * @param <T> The result type associated with the Promise.
 * 
 * @ThreadSafe
 * @author $Id: d8b44a36f3eb797316b213118192fac213fa0c59 $
 */
final class PromiseImpl<T> implements Promise<T> {
	/**
	 * A ConcurrentLinkedQueue to hold the callbacks for this Promise, so no
	 * additional synchronization is required to write to or read from the
	 * queue.
	 */
	private final ConcurrentLinkedQueue<Runnable>	callbacks;
	/**
	 * A CountDownLatch to manage the resolved state of this Promise.
	 * 
	 * <p>
	 * This object is used as the synchronizing object to provide a critical
	 * section in {@link #resolve(Object, Throwable)} so that only a single
	 * thread can write the resolved state variables and open the latch.
	 * 
	 * <p>
	 * The resolved state variables, {@link #value} and {@link #fail}, must only
	 * be written when the latch is closed (getCount() != 0) and must only be
	 * read when the latch is open (getCount() == 0). The latch state must
	 * always be checked before writing or reading since the resolved state
	 * variables' memory consistency is guarded by the latch.
	 */
	private final CountDownLatch					resolved;
	/**
	 * The value of this Promise if successfully resolved.
	 * 
	 * @GuardedBy("resolved")
	 * @see #resolved
	 */
	private T										value;
	/**
	 * The failure of this Promise if resolved with a failure or {@code null} if
	 * successfully resolved.
	 * 
	 * @GuardedBy("resolved")
	 * @see #resolved
	 */
	private Throwable								fail;

	/**
	 * Initialize this Promise.
	 */
	PromiseImpl() {
		callbacks = new ConcurrentLinkedQueue<Runnable>();
		resolved = new CountDownLatch(1);
	}

	/**
	 * Initialize and resolve this Promise.
	 * 
	 * @param v The value of this resolved Promise.
	 * @param f The failure of this resolved Promise.
	 */
	PromiseImpl(T v, Throwable f) {
		value = v;
		fail = f;
		callbacks = new ConcurrentLinkedQueue<Runnable>();
		resolved = new CountDownLatch(0);
	}

	/**
	 * Resolve this Promise.
	 * 
	 * @param v The value of this Promise.
	 * @param f The failure of this Promise.
	 */
	void resolve(T v, Throwable f) {
		// critical section: only one resolver at a time
		synchronized (resolved) {
			if (resolved.getCount() == 0) {
				throw new IllegalStateException("Already resolved");
			}
			/*
			 * The resolved state variables must be set before opening the
			 * latch. This safely publishes them to be read by other threads
			 * that must verify the latch is open before reading.
			 */
			value = v;
			fail = f;
			resolved.countDown();
		}
		notifyCallbacks(); // call any registered callbacks
	}

	/**
	 * Call any registered callbacks if this Promise is resolved.
	 */
	private void notifyCallbacks() {
		if (resolved.getCount() != 0) {
			return; // return if not resolved
		}

		/*
		 * Note: multiple threads can be in this method removing callbacks from
		 * the queue and calling them, so the order in which callbacks are
		 * called cannot be specified.
		 */
		for (Runnable callback = callbacks.poll(); callback != null; callback = callbacks.poll()) {
			try {
				callback.run();
			} catch (Throwable t) {
				Logger.logCallbackException(t);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isDone() {
		return resolved.getCount() == 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public T getValue() throws InvocationTargetException, InterruptedException {
		resolved.await();
		if (fail == null) {
			return value;
		}
		throw new InvocationTargetException(fail);
	}

	/**
	 * {@inheritDoc}
	 */
	public Throwable getFailure() throws InterruptedException {
		resolved.await();
		return fail;
	}

	/**
	 * {@inheritDoc}
	 */
	public Promise<T> onResolve(Runnable callback) {
		callbacks.offer(callback);
		notifyCallbacks(); // call any registered callbacks
		return this;
	}

	/**
	 * {@inheritDoc}
	 */
	public <R> Promise<R> then(Success<? super T, ? extends R> success, Failure failure) {
		PromiseImpl<R> chained = new PromiseImpl<R>();
		onResolve(new Then<R>(chained, success, failure));
		return chained;
	}

	/**
	 * {@inheritDoc}
	 */
	public <R> Promise<R> then(Success<? super T, ? extends R> success) {
		return then(success, null);
	}

	/**
	 * A callback used to chain promises for the {@link #then(Success, Failure)}
	 * method.
	 * 
	 * @Immutable
	 */
	private final class Then<R> implements Runnable {
		private final PromiseImpl<R>			chained;
		private final Success<T, ? extends R>	success;
		private final Failure					failure;

		@SuppressWarnings("unchecked")
		Then(PromiseImpl<R> chained, Success<? super T, ? extends R> success, Failure failure) {
			this.chained = chained;
			this.success = (Success<T, ? extends R>) success;
			this.failure = failure;
		}

		public void run() {
			Throwable f;
			final boolean interrupted = Thread.interrupted();
			try {
				f = getFailure();
			} catch (Throwable e) {
				f = e; // propagate new exception
			} finally {
				if (interrupted) { // restore interrupt status
					Thread.currentThread().interrupt();
				}
			}
			if (f != null) {
				if (failure != null) {
					try {
						failure.fail(PromiseImpl.this);
					} catch (Throwable e) {
						f = e; // propagate new exception
					}
				}
				// fail chained
				chained.resolve(null, f);
				return;
			}
			Promise<? extends R> returned = null;
			if (success != null) {
				try {
					returned = success.call(PromiseImpl.this);
				} catch (Throwable e) {
					chained.resolve(null, e);
					return;
				}
			}
			if (returned == null) {
				// resolve chained with null value
				chained.resolve(null, null);
			} else {
				// resolve chained when returned promise is resolved
				returned.onResolve(new Chain<R>(chained, returned));
			}
		}
	}

	/**
	 * A callback used to resolve the chained Promise when the Promise promise
	 * is resolved.
	 * 
	 * @Immutable
	 */
	private final static class Chain<R> implements Runnable {
		private final PromiseImpl<R>		chained;
		private final Promise<? extends R>	promise;
		private final Throwable				failure;

		Chain(PromiseImpl<R> chained, Promise<? extends R> promise) {
			this.chained = chained;
			this.promise = promise;
			this.failure = null;
		}

		Chain(PromiseImpl<R> chained, Promise<? extends R> promise, Throwable failure) {
			this.chained = chained;
			this.promise = promise;
			this.failure = failure;
		}

		public void run() {
			R value = null;
			Throwable f;
			final boolean interrupted = Thread.interrupted();
			try {
				f = promise.getFailure();
				if (f == null) {
					value = promise.getValue();
				} else if (failure != null) {
					f = failure;
				}
			} catch (Throwable e) {
				f = e; // propagate new exception
			} finally {
				if (interrupted) { // restore interrupt status
					Thread.currentThread().interrupt();
				}
			}
			chained.resolve(value, f);
		}
	}

	/**
	 * Resolve this Promise with the specified Promise.
	 * 
	 * <p>
	 * If the specified Promise is successfully resolved, this Promise is
	 * resolved with the value of the specified Promise. If the specified
	 * Promise is resolved with a failure, this Promise is resolved with the
	 * failure of the specified Promise.
	 * 
	 * @param with A Promise whose value or failure will be used to resolve this
	 *        Promise. Must not be {@code null}.
	 * @return A Promise that is resolved only when this Promise is resolved by
	 *         the specified Promise. The returned Promise will be successfully
	 *         resolved, with the value {@code null}, if this Promise was
	 *         resolved by the specified Promise. The returned Promise will be
	 *         resolved with a failure of {@link IllegalStateException} if this
	 *         Promise was already resolved when the specified Promise was
	 *         resolved.
	 */
	Promise<Void> resolveWith(Promise<? extends T> with) {
		PromiseImpl<Void> chained = new PromiseImpl<Void>();
		ResolveWith resolveWith = new ResolveWith(chained);
		with.then(resolveWith, resolveWith);
		return chained;
	}

	/**
	 * A callback used to resolve this Promise with another Promise for the
	 * {@link PromiseImpl#resolveWith(Promise)} method.
	 * 
	 * @Immutable
	 */
	private final class ResolveWith implements Success<T, Void>, Failure {
		private final PromiseImpl<Void>	chained;

		ResolveWith(PromiseImpl<Void> chained) {
			this.chained = chained;
		}

		public Promise<Void> call(Promise<T> with) throws Exception {
			try {
				resolve(with.getValue(), null);
			} catch (Throwable e) {
				chained.resolve(null, e);
				return null;
			}
			chained.resolve(null, null);
			return null;
		}

		public void fail(Promise<?> with) throws Exception {
			try {
				resolve(null, with.getFailure());
			} catch (Throwable e) {
				chained.resolve(null, e);
				return;
			}
			chained.resolve(null, null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Promise<T> filter(Predicate<? super T> predicate) {
		return then(new Filter<T>(predicate));
	}

	/**
	 * A callback used by the {@link PromiseImpl#filter(Predicate)} method.
	 * 
	 * @Immutable
	 */
	private static final class Filter<T> implements Success<T, T> {
		private final Predicate<? super T>	predicate;

		Filter(Predicate<? super T> predicate) {
			this.predicate = requireNonNull(predicate);
		}

		public Promise<T> call(Promise<T> resolved) throws Exception {
			if (predicate.test(resolved.getValue())) {
				return resolved;
			}
			throw new NoSuchElementException();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public <R> Promise<R> map(Function<? super T, ? extends R> mapper) {
		return then(new Map<T, R>(mapper));
	}

	/**
	 * A callback used by the {@link PromiseImpl#map(Function)} method.
	 * 
	 * @Immutable
	 */
	private static final class Map<T, R> implements Success<T, R> {
		private final Function<? super T, ? extends R>	mapper;

		Map(Function<? super T, ? extends R> mapper) {
			this.mapper = requireNonNull(mapper);
		}

		public Promise<R> call(Promise<T> resolved) throws Exception {
			return new PromiseImpl<R>(mapper.apply(resolved.getValue()), null);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public <R> Promise<R> flatMap(Function<? super T, Promise<? extends R>> mapper) {
		return then(new FlatMap<T, R>(mapper));
	}

	/**
	 * A callback used by the {@link PromiseImpl#flatMap(Function)} method.
	 * 
	 * @Immutable
	 */
	private static final class FlatMap<T, R> implements Success<T, R> {
		private final Function<? super T, Promise<? extends R>>	mapper;

		FlatMap(Function<? super T, Promise<? extends R>> mapper) {
			this.mapper = requireNonNull(mapper);
		}

		@SuppressWarnings("unchecked")
		public Promise<R> call(Promise<T> resolved) throws Exception {
			return (Promise<R>) mapper.apply(resolved.getValue());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Promise<T> recover(Function<Promise<?>, ? extends T> recovery) {
		PromiseImpl<T> chained = new PromiseImpl<T>();
		Recover<T> recover = new Recover<T>(chained, recovery);
		then(recover, recover);
		return chained;
	}

	/**
	 * A callback used by the {@link PromiseImpl#recover(Function)} method.
	 * 
	 * @Immutable
	 */
	private static final class Recover<T> implements Success<T, Void>, Failure {
		private final PromiseImpl<T>					chained;
		private final Function<Promise<?>, ? extends T>	recovery;

		Recover(PromiseImpl<T> chained, Function<Promise<?>, ? extends T> recovery) {
			this.chained = chained;
			this.recovery = requireNonNull(recovery);
		}

		public Promise<Void> call(Promise<T> resolved) throws Exception {
			T value;
			try {
				value = resolved.getValue();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return null;
			}
			chained.resolve(value, null);
			return null;
		}

		public void fail(Promise<?> resolved) throws Exception {
			T recovered;
			Throwable failure;
			try {
				recovered = recovery.apply(resolved);
				failure = resolved.getFailure();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return;
			}
			if (recovered == null) {
				chained.resolve(null, failure);
			} else {
				chained.resolve(recovered, null);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Promise<T> recoverWith(Function<Promise<?>, Promise<? extends T>> recovery) {
		PromiseImpl<T> chained = new PromiseImpl<T>();
		RecoverWith<T> recoverWith = new RecoverWith<T>(chained, recovery);
		then(recoverWith, recoverWith);
		return chained;
	}

	/**
	 * A callback used by the {@link PromiseImpl#recoverWith(Function)} method.
	 * 
	 * @Immutable
	 */
	private static final class RecoverWith<T> implements Success<T, Void>, Failure {
		private final PromiseImpl<T>								chained;
		private final Function<Promise<?>, Promise<? extends T>>	recovery;

		RecoverWith(PromiseImpl<T> chained, Function<Promise<?>, Promise<? extends T>> recovery) {
			this.chained = chained;
			this.recovery = requireNonNull(recovery);
		}

		public Promise<Void> call(Promise<T> resolved) throws Exception {
			T value;
			try {
				value = resolved.getValue();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return null;
			}
			chained.resolve(value, null);
			return null;
		}

		public void fail(Promise<?> resolved) throws Exception {
			Promise<? extends T> recovered;
			Throwable failure;
			try {
				recovered = recovery.apply(resolved);
				failure = resolved.getFailure();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return;
			}
			if (recovered == null) {
				chained.resolve(null, failure);
			} else {
				recovered.onResolve(new Chain<T>(chained, recovered));
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Promise<T> fallbackTo(Promise<? extends T> fallback) {
		PromiseImpl<T> chained = new PromiseImpl<T>();
		FallbackTo<T> fallbackTo = new FallbackTo<T>(chained, fallback);
		then(fallbackTo, fallbackTo);
		return chained;
	}

	/**
	 * A callback used by the {@link PromiseImpl#fallbackTo(Promise)} method.
	 * 
	 * @Immutable
	 */
	private static final class FallbackTo<T> implements Success<T, Void>, Failure {
		private final PromiseImpl<T>		chained;
		private final Promise<? extends T>	fallback;

		FallbackTo(PromiseImpl<T> chained, Promise<? extends T> fallback) {
			this.chained = chained;
			this.fallback = requireNonNull(fallback);
		}

		public Promise<Void> call(Promise<T> resolved) throws Exception {
			T value;
			try {
				value = resolved.getValue();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return null;
			}
			chained.resolve(value, null);
			return null;
		}

		public void fail(Promise<?> resolved) throws Exception {
			Throwable failure;
			try {
				failure = resolved.getFailure();
			} catch (Throwable e) {
				chained.resolve(null, e);
				return;
			}
			fallback.onResolve(new Chain<T>(chained, fallback, failure));
		}
	}

	static <V> V requireNonNull(V value) {
		if (value != null) {
			return value;
		}
		throw new NullPointerException();
	}

	/**
	 * Use the lazy initialization holder class idiom to delay creating a Logger
	 * until we actually need it.
	 */
	private static final class Logger {
		private final static java.util.logging.Logger	LOGGER;
		static {
			LOGGER = java.util.logging.Logger.getLogger(PromiseImpl.class.getName());
		}

		static void logCallbackException(Throwable t) {
			LOGGER.log(java.util.logging.Level.WARNING, "Exception from Promise callback", t);
		}
	}
}
