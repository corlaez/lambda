package com.jnape.palatable.lambda.monad.transformer.builtin;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.specialized.Lift;
import com.jnape.palatable.lambda.functions.specialized.Pure;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.builtin.Compose;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.transformer.MonadT;

import java.util.Objects;

import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;

/**
 * A {@link MonadT monad transformer} for {@link Lazy}. Note that {@link LazyT#flatMap(Fn1)} must force its value.
 *
 * @param <M> the outer {@link Monad}
 * @param <A> the carrier type
 */
public final class LazyT<M extends Monad<?, M>, A> implements MonadT<M, A, LazyT<M, ?>, LazyT<?, ?>> {

    private final Monad<Lazy<A>, M> mla;

    private LazyT(Monad<Lazy<A>, M> mla) {
        this.mla = mla;
    }

    /**
     * Recover the full structure of the embedded {@link Monad}.
     *
     * @param <MLA> the witnessed target type
     * @return the embedded {@link Monad}
     */
    public <MLA extends Monad<Lazy<A>, M>> MLA runLazyT() {
        return mla.coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B, N extends Monad<?, N>> LazyT<N, B> lift(Monad<B, N> mb) {
        return liftLazyT().apply(mb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, B> flatMap(Fn1<? super A, ? extends Monad<B, LazyT<M, ?>>> f) {
        return new LazyT<>(mla.flatMap(lazyA -> f.apply(lazyA.value()).<LazyT<M, B>>coerce().runLazyT()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, B> pure(B b) {
        return new LazyT<>(mla.pure(lazy(b)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, B> fmap(Fn1<? super A, ? extends B> fn) {
        return MonadT.super.<B>fmap(fn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, B> zip(Applicative<Fn1<? super A, ? extends B>, LazyT<M, ?>> appFn) {
        return MonadT.super.zip(appFn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> Lazy<LazyT<M, B>> lazyZip(
            Lazy<? extends Applicative<Fn1<? super A, ? extends B>, LazyT<M, ?>>> lazyAppFn) {
        return new Compose<>(mla)
                .lazyZip(lazyAppFn.fmap(lazyT -> new Compose<>(
                        lazyT.<LazyT<M, Fn1<? super A, ? extends B>>>coerce()
                                .<Monad<Lazy<Fn1<? super A, ? extends B>>, M>>runLazyT())))
                .fmap(compose -> lazyT(compose.getCompose()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, B> discardL(Applicative<B, LazyT<M, ?>> appB) {
        return MonadT.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> LazyT<M, A> discardR(Applicative<B, LazyT<M, ?>> appB) {
        return MonadT.super.discardR(appB).coerce();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LazyT<?, ?> && Objects.equals(mla, ((LazyT<?, ?>) other).mla);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mla);
    }

    @Override
    public String toString() {
        return "LazyT{mla=" + mla + '}';
    }

    /**
     * Static factory method for lifting a <code>{@link Monad}&lt;{@link Lazy}&lt;A&gt;, M&gt;</code> into a
     * {@link LazyT}.
     *
     * @param mla the {@link Monad}&lt;{@link Lazy}&lt;A&gt;, M&gt;
     * @param <M> the outer {@link Monad} unification parameter
     * @param <A> the carrier type
     * @return the new {@link LazyT}
     */
    public static <M extends Monad<?, M>, A> LazyT<M, A> lazyT(Monad<Lazy<A>, M> mla) {
        return new LazyT<>(mla);
    }

    /**
     * The canonical {@link Pure} instance for {@link LazyT}.
     *
     * @param pureM the argument {@link Monad} {@link Pure}
     * @param <M>   the argument {@link Monad} witness
     * @return the {@link Pure} instance
     */
    public static <M extends Monad<?, M>> Pure<LazyT<M, ?>> pureLazyT(Pure<M> pureM) {
        return new Pure<LazyT<M, ?>>() {
            @Override
            public <A> LazyT<M, A> checkedApply(A a) {
                return lazyT(pureM.<A, Monad<A, M>>apply(a).fmap(Lazy::lazy));
            }
        };
    }

    /**
     * {@link Lift} for {@link LazyT}.
     *
     * @return the {@link Monad} lifted into {@link LazyT}
     */
    public static Lift<LazyT<?, ?>> liftLazyT() {
        return new Lift<LazyT<?, ?>>() {
            @Override
            public <A, M extends Monad<?, M>> LazyT<M, A> checkedApply(Monad<A, M> ga) {
                return lazyT(ga.fmap(Lazy::lazy));
            }
        };
    }
}
