package org.coherent.core;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.coherent.core.Command.Context;
import static org.coherent.core.Command.Context.*;
import org.coherent.core.Command.Parameter;
import static org.coherent.core.Command.Parameter.*;
import org.coherent.core.Command.Flow;
import static org.coherent.core.Command.Flow.*;
import org.coherent.core.Command.Body;
import static org.coherent.core.Command.Body.*;
import org.coherent.core.Command.Binding;
import static org.coherent.core.Command.Binding.*;
import org.coherent.core.Command.Dispatcher;
import static org.coherent.core.Command.Dispatcher.*;
import org.coherent.core.Command.Behavior;
import static org.coherent.core.Command.Behavior.*;

import static org.coherent.core.Command.Body.Notation.*;
import static org.coherent.core.Command.Flow.Notation.*;

import org.jparsec.core.Escaper;
import static org.jparsec.core.Escaper.*;
import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Message;
import static org.jparsec.core.Parser.Message.*;
import org.jparsec.core.Parser.Result;
import static org.jparsec.core.Parser.Result.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
import org.monadium.core.data.Either;
import static org.monadium.core.data.Either.*;
import org.monadium.core.data.Id;
import static org.monadium.core.data.Id.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

import static org.monadium.core.data.List.Notation.*;

import static org.monadium.core.Notation.*;

public abstract class Command<S, C, A> {
    public static abstract class Context<S, C> {
        public static final class Base<S, C, A> extends Context<S, C> {
            final Command<S, C, A> command;

            Base(S source, C context, Command<S, C, A> command) { super(source, context); this.command = command; }

            public interface Case<S, C, R> { <A> R caseBase(Command<S, C, A> command); }
            @Override public <R> R caseof(Base.Case<S, C, R> caseBase, Fork.Case<S, C, R> caseFork) { return caseBase.caseBase(command); }
        }
        public static final class Fork<S, C, T, D, P, A> extends Context<S, C> {
            final Context<T, D> parent;
            final P parameter;
            final String binding;
            final Command<S, C, A> command;

            Fork(S source, C context, Context<T, D> parent, P parameter, String binding, Command<S, C, A> command) { super(source, context); this.parent = parent; this.parameter = parameter; this.binding = binding; this.command = command; }

            public interface Case<S, C, R> { <T, D, P, A> R caseFork(Context<T, D> parent, P parameter, String binding, Command<S, C, A> command); }
            @Override public <R> R caseof(Base.Case<S, C, R> caseBase, Fork.Case<S, C, R> caseFork) { return caseFork.caseFork(parent, parameter, binding, command); }
        }

        final S source;
        final C context;

        Context(S source, C context) { this.source = source; this.context = context; }

        public interface Match<S, C, R> extends Base.Case<S, C, R>, Fork.Case<S, C, R> {}
        public final <R> R match(Match<S, C, R> match) { return caseof(match, match); }
        public abstract <R> R caseof(Base.Case<S, C, R> caseBase, Fork.Case<S, C, R> caseFork);

        public static <S, C, A> Context<S, C> base(S source, C context, Command<S, C, A> command) { return new Base<>(source, context, command); }
        public static <S, C, A> Context<S, C> base(Tuple<S, C> environment, Command<S, C, A> command) { return base(environment.first(), environment.second(), command); }
        public static <S, C, T, D, P, A> Context<S, C> fork(S source, C context, Context<T, D> parent, P parameter, String binding, Command<S, C, A> command) { return new Fork<>(source, context, parent, parameter, binding, command); }
        public static <S, C, T, D, P, A> Context<S, C> fork(Tuple<S, C> environment, Context<T, D> parent, P parameter, String binding, Command<S, C, A> command) { return fork(environment.first(), environment.second(), parent, parameter, binding, command); }

        public S source() { return source; }
        public C context() { return context; }

        public Tuple<S, C> environment() { return tuple(source, context); }
    }
    public static final class Parameter<S, C, A> {
        final String name;
        final String description;
        final Parser<Text, Context<S, C>, Bottom, A> parser;
        final BiFunction<Text, Context<S, C>, List<Text>> completer;

        Parameter(String name, String description, Parser<Text, Context<S, C>, Bottom, A> parser, BiFunction<Text, Context<S, C>, List<Text>> completer) { this.name = name; this.description = description; this.parser = parser; this.completer = completer; }

        public static <S, C, A> Parameter<S, C, A> parameter(String name, String description, Parser<Text, Context<S, C>, Bottom, A> parser, BiFunction<Text, Context<S, C>, List<Text>> completer) { return new Parameter<>(name, description, parser, completer); }

        public String name() { return name; }
        public String description() { return description; }
        public Parser<Text, Context<S, C>, Bottom, A> parser() { return parser; }
        public BiFunction<Text, Context<S, C>, List<Text>> completer() { return completer; }

        public static <S, C, A, B> Parameter<S, C, B> specialize(Parameter<S, C, A> parameter, Function<A, B> f) { return parameter.map(f); }

        public static <S, C, A> Parser<Text, Context<S, C>, Bottom, A> parseParameter(Parameter<S, C, A> parameter) { return conclude(parameter.parser(), error("Could not parse command parameter " + escapeString(parameter.name()))); }
        public static <S, C, A> Parser<Text, Context<S, C>, Bottom, List<Text>> analyzeParameter(Parameter<S, C, A> parameter) {
            return $do(
            $(  getStream()                                                             , input ->
            $(  getUser()                                                               , context ->
            $(  simple(parameter.completer().apply(input, context))                     , completions ->
            $(  simple(completions.filter(completion -> input.isPrefixOf(completion)))  ))))
            );
        }

        public <B> Parameter<S, C, B> map(Function<A, B> f) { return parameter(name, description, parser.map(f), completer); }
    }
    public static abstract class Flow<T, A> {
        public static final class Value<T, A> extends Flow<T, A> {
            final A a;

            Value(A a) { this.a = a; }

            public interface Case<T, A, R> { R caseValue(A a); }
            @Override public <R> R caseof(Value.Case<T, A, R> caseValue, External.Case<T, A, R> caseExternal, FlatMap.Case<T, A, R> caseFlatMap) { return caseValue.caseValue(a); }

        }
        public static final class External<T, A> extends Flow<T, A> {
            final T t;

            External(T t) { this.t = t; }

            public interface Case<T, A, R> { R caseExternal(T t); }
            @Override public <R> R caseof(Value.Case<T, A, R> caseValue, External.Case<T, A, R> caseExternal, FlatMap.Case<T, A, R> caseFlatMap) { return caseExternal.caseExternal(t); }
        }
        public static final class FlatMap<T, X, A> extends Flow<T, A> {
            final Flow<T, X> fx;
            final Function<X, Flow<T, A>> f;

            FlatMap(Flow<T, X> fx, Function<X, Flow<T, A>> f) { this.fx = fx; this.f = f; }

            public interface Case<T, A, R> { <X> R caseFlatMap(Flow<T, X> fx, Function<X, Flow<T, A>> f); }
            @Override public <R> R caseof(Value.Case<T, A, R> caseValue, External.Case<T, A, R> caseExternal, FlatMap.Case<T, A, R> caseFlatMap) { return caseFlatMap.caseFlatMap(fx, f); }
        }

        Flow() {}

        public interface Match<T, A, R> extends Value.Case<T, A, R>, External.Case<T, A, R>, FlatMap.Case<T, A, R> {}
        public final <R> R match(Match<T, A, R> match) { return caseof(match, match, match); }
        public abstract <R> R caseof(Value.Case<T, A, R> caseEvaluate, External.Case<T, A, R> caseDefine, FlatMap.Case<T, A, R> caseFlatMap);

        public static <T, A> Flow<T, A> value(A a) { return new Value<>(a); }
        public static <T, A> Flow<T, A> external(T t) { return new External<>(t); }

        public static <A> A evalFlow(Flow<Bottom, A> flow) {
            return flow.match(new Flow.Match<>(){
                @Override public A
                caseValue(A a) {
                    return a;
                }
                @Override public A
                caseExternal(Bottom t) {
                    return absurd(t);
                }
                @Override public <X> A
                caseFlatMap(Flow<Bottom, X> fx, Function<X, Flow<Bottom, A>> f) {
                    return evalFlow(f.apply(evalFlow(fx)));
                }
            });
        }

        public final <B> Flow<T, B> map(Function<A, B> f) { return flatMap(a -> value(f.apply(a))); }
        public final <B> Flow<T, B> applyMap(Flow<T, Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
        public final <B> Flow<T, B> flatMap(Function<A, Flow<T, B>> f) { return new FlatMap<>(this, f); }

        public static <T, A> Flow<T, A> pure(A a) { return value(a); }
        public static <T, A, B> Flow<T, B> replace(Flow<T, A> fa, B b) { return fa.map(a -> b); }
        public static <T, A> Flow<T, Unit> discard(Flow<T, A> fa) { return fa.map(a -> unit()); }

        public static final class Notation {
            Notation() {}

            public static <T, A, B> Flow<T, B> $(Flow<T, A> fa, Function<A, Flow<T, B>> f) { return fa.flatMap(f); }
            public static <T, A, B> Flow<T, B> $(Flow<T, A> fa, Supplier<Flow<T, B>> fb) { return fa.flatMap(a -> fb.get()); }
        }
    }
    public static abstract class Body<S, C, T, A> {
        public static final class Evaluate<S, C, T, A> extends Body<S, C, T, A> {
            final A a;

            Evaluate(A a) { this.a = a; }

            public interface Case<S, C, T, A, R> { R caseEvaluate(A a); }
            @Override public <R> R caseof(Evaluate.Case<S, C, T, A, R> caseEvaluate, Define.Case<S, C, T, A, R> caseDefine, FlatMap.Case<S, C, T, A, R> caseFlatMap) { return caseEvaluate.caseEvaluate(a); }
        }
        public static final class Define<S, C, T, X> extends Body<S, C, T, Flow<T, X>> {
            final Parameter<S, C, X> parameter;

            Define(Parameter<S, C, X> parameter) { this.parameter = parameter; }

            public interface Case<S, C, T, A, R> { <X> R caseDefine(Parameter<S, C, X> parameter, Id<Flow<T, X>, A> id); }
            @Override public <R> R caseof(Evaluate.Case<S, C, T, Flow<T, X>, R> caseEvaluate, Define.Case<S, C, T, Flow<T, X>, R> caseDefine, FlatMap.Case<S, C, T, Flow<T, X>, R> caseFlatMap) { return caseDefine.caseDefine(parameter, refl()); }
        }
        public static final class FlatMap<S, C, T, X, A> extends Body<S, C, T, A> {
            final Body<S, C, T, X> fx;
            final Function<X, Body<S, C, T, A>> f;

            FlatMap(Body<S, C, T, X> fx, Function<X, Body<S, C, T, A>> f) { this.fx = fx; this.f = f; }

            public interface Case<S, C, T, A, R> { <X> R caseFlatMap(Body<S, C, T, X> fx, Function<X, Body<S, C, T, A>> f); }
            @Override public <R> R caseof(Evaluate.Case<S, C, T, A, R> caseEvaluate, Define.Case<S, C, T, A, R> caseDefine, FlatMap.Case<S, C, T, A, R> caseFlatMap) { return caseFlatMap.caseFlatMap(fx, f); }
        }

        Body() {}

        public interface Match<S, C, T, A, R> extends Evaluate.Case<S, C, T, A, R>, Define.Case<S, C, T, A, R>, FlatMap.Case<S, C, T, A, R> {}
        public final <R> R match(Match<S, C, T, A, R> match) { return caseof(match, match, match); }
        public abstract <R> R caseof(Evaluate.Case<S, C, T, A, R> caseEvaluate, Define.Case<S, C, T, A, R> caseDefine, FlatMap.Case<S, C, T, A, R> caseFlatMap);

        public static <S, C, T, A> Body<S, C, T, A> evaluate(A a) { return new Evaluate<>(a); }
        public static <S, C, T, X> Body<S, C, T, Flow<T, X>> define(Parameter<S, C, X> parameter) { return new Define<>(parameter); }
        public static <S, C, T> Body<S, C, T, Flow<T, Unit>> empty() { return evaluate(value(unit())); }

        public static <S, C, T, A> Parser<Text, Context<S, C>, Bottom, A> parseBody(Body<S, C, T, A> body) {
            return body.match(new Body.Match<>() {
                @Override public Parser<Text, Context<S, C>, Bottom, A>
                caseEvaluate(A a) {
                    return simple(a);
                }
                @Override public <X> Parser<Text, Context<S, C>, Bottom, A>
                caseDefine(Parameter<S, C, X> parameter, Id<Flow<T, X>, A> id) {
                    return $do(
                    $(  recur(() -> parseParameter(parameter))      , x ->
                    $(  choice(suppress(eof()), skipMany(space()))  , () ->
                    $(  simple(id.coerce(value(x)))                 )))
                    );
                }
                @Override public <X> Parser<Text, Context<S, C>, Bottom, A>
                caseFlatMap(Body<S, C, T, X> fx, Function<X, Body<S, C, T, A>> f) {
                    return $do(
                    $(  recur(() -> parseBody(fx))           , x ->
                    $(  recur(() -> parseBody(f.apply(x)))   ))
                    );
                }
            });
        }
        public static <S, C, T, A> Parser<Text, Context<S, C>, Bottom, Either<List<Text>, A>> analyzeBody(Body<S, C, T, A> body) {
            return body.match(new Body.Match<>() {
                @Override public Parser<Text, Context<S, C>, Bottom, Either<List<Text>, A>>
                caseEvaluate(A a) {
                    return simple(right(a));
                }
                @Override public <X> Parser<Text, Context<S, C>, Bottom, Either<List<Text>, A>>
                caseDefine(Parameter<S, C, X> parameter, Id<Flow<T, X>, A> id) {
                    return choice($do(
                    $(  attempt(recur(() -> parseParameter(parameter))) , x ->
                    $(  choice($do(
                        $(  suppress(eof())     , () ->
                        $(  simple(left(nil())) ))
                        ), $do(
                        $(  skipMany(space())                   , () ->
                        $(  simple(right(id.coerce(value(x))))  ))
                        ))                                              ))
                    ), $do(
                    $(  recur(() -> analyzeParameter(parameter))    , completions ->
                    $(  simple(left(completions))                   ))
                    ));
                }
                @Override public <X> Parser<Text, Context<S, C>, Bottom, Either<List<Text>, A>>
                caseFlatMap(Body<S, C, T, X> fx, Function<X, Body<S, C, T, A>> f) {
                    return $do(
                    $(  recur(() -> analyzeBody(fx))                    , result ->
                    $(  result.caseof(
                            completions -> simple(left(completions)),
                            x -> recur(() -> analyzeBody(f.apply(x)))
                        )                                               ))
                    );
                }
            });
        }

        public final <B> Body<S, C, T, B> map(Function<A, B> f) { return flatMap(a -> evaluate(f.apply(a))); }
        public final <B> Body<S, C, T, B> applyMap(Body<S, C, T, Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
        public final <B> Body<S, C, T, B> flatMap(Function<A, Body<S, C, T, B>> f) { return new FlatMap<>(this, f); }

        public static <S, C, T, A> Body<S, C, T, A> pure(A a) { return evaluate(a); }
        public static <S, C, T, A, B> Body<S, C, T, B> replace(Body<S, C, T, A> fa, B b) { return fa.map(a -> b); }
        public static <S, C, T, A> Body<S, C, T, Unit> discard(Body<S, C, T, A> fa) { return fa.map(a -> unit()); }

        public static final class Notation {
            Notation() {}

            public static <S, C, T, A, B> Body<S, C, T, B> $(Body<S, C, T, A> fa, Function<A, Body<S, C, T, B>> f) { return fa.flatMap(f); }
            public static <S, C, T, A, B> Body<S, C, T, B> $(Body<S, C, T, A> fa, Supplier<Body<S, C, T, B>> fb) { return fa.flatMap(a -> fb.get()); }
        }
    }
    public static abstract class Binding<S, C, P, A> {
        public static final class Entry<S, C, T, D, P, A> extends Binding<S, C, P, A> {
            final BiFunction<Context<S, C>, P, Tuple<T, D>> delegator;
            final Command<T, D, A> command;

            Entry(String binding, BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command) { super(binding); this.delegator = delegator; this.command = command; }

            public interface Case<S, C, P, A, R> { <T, D> R caseEntry(BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command); }
            @Override public <R> R caseof(Entry.Case<S, C, P, A, R> caseEntry) { return caseEntry.caseEntry(delegator, command); }

            @Override public <B> Binding<S, C, P, B> map(Function<A, B> f) { return entry(binding, delegator, command.map(f)); }
        }

        final String binding;

        Binding(String binding) { this.binding = binding; }

        public interface Match<S, C, P, A, R> extends Entry.Case<S, C, P, A, R> {}
        public final <R> R match(Match<S, C, P, A, R> match) { return caseof(match); }
        public abstract <R> R caseof(Entry.Case<S, C, P, A, R> caseEntry);

        public static <S, C, T, D, P, A> Binding<S, C, P, A> entry(String binding, BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command) { return new Entry<>(binding, delegator, command); }
        public static <S, C, P, A> Binding<S, C, P, A> entry(String binding, Command<S, C, A> command) { return entry(binding, (context, parameter) -> context.environment(), command); }
        public static <S, C, T, D, P, A> Binding<S, C, P, A> entry(BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command) { return entry(command.name(), delegator, command); }
        public static <S, C, P, A> Binding<S, C, P, A> entry(Command<S, C, A> command) { return entry(command.name(), (context, parameter) -> context.environment(), command); }

        public final String binding() { return binding; }

        public static <S, C, P, A> Parser<Text, Context<S, C>, Bottom, Action<A>> parseBinding(Binding<S, C, P, A> binding, P parameter) {
            return binding.match(new Binding.Match<>() {
                @Override public <T, D> Parser<Text, Context<S, C>, Bottom, Action<A>>
                caseEntry(BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command) {
                    return localUser(recur(() -> parseCommand(command)), context -> fork(delegator.apply(context, parameter), context, parameter, binding.binding(), command));
                }
            });
        }
        public static <S, C, P, A> Parser<Text, Context<S, C>, Bottom, List<Text>> analyzeBinding(Binding<S, C, P, A> binding, P parameter) {
            return binding.match(new Binding.Match<>() {
                @Override public <T, D> Parser<Text, Context<S, C>, Bottom, List<Text>>
                caseEntry(BiFunction<Context<S, C>, P, Tuple<T, D>> delegator, Command<T, D, A> command) {
                    return localUser(recur(() -> analyzeCommand(command)), context -> fork(delegator.apply(context, parameter), context, parameter, binding.binding(), command));
                }
            });
        }

        public abstract <B> Binding<S, C, P, B> map(Function<A, B> f);
    }
    public static final class Dispatcher<S, C, P, A> {
        final List<Binding<S, C, P, A>> bindings;

        Dispatcher(List<Binding<S, C, P, A>> bindings) { this.bindings = bindings; }

        public static <S, C, P, A> Dispatcher<S, C, P, A> dispatcher(List<Binding<S, C, P, A>> bindings) { return new Dispatcher<>(bindings); }
        @SafeVarargs public static <S, C, P, A> Dispatcher<S, C, P, A> dispatcher(Binding<S, C, P, A>... bindings) { return dispatcher(list(bindings)); }

        public List<Binding<S, C, P, A>> bindings() { return bindings; }

        public static <S, C, P, A> Parser<Text, Context<S, C>, Bottom, Binding<S, C, P, A>> parseBindings(Dispatcher<S, C, P, A> dispatcher) {
            return conclude(dispatcher.bindings().foldl(
                (parser, binding) -> choice(replace(string(binding.binding()), binding), parser),
                ignore()
            ), error("Could not parse command binding"));
        }
        public static <S, C, P, A> Parser<Text, Context<S, C>, Bottom, Action<A>> parseDispatcher(Dispatcher<S, C, P, A> dispatcher, P parameter) {
            return $do(
            $(  recur(() -> parseBindings(dispatcher))          , binding ->
            $(  choice(suppress(eof()), skipMany(space()))      , ()  ->
            $(  recur(() -> parseBinding(binding, parameter))   )))
            );
        }
        public static <S, C, P, A> Parser<Text, Context<S, C>, Bottom, List<Text>> analyzeDispatcher(Dispatcher<S, C, P, A> dispatcher, P parameter) {
            return choice($do(
            $(  attempt(recur(() -> parseBindings(dispatcher)))             , binding ->
            $(  choice($do(
                $(  suppress(eof()) , () ->
                $(  simple(nil())   ))
                ), $do(
                $(  skipMany(space())                               , () ->
                $(  recur(() -> analyzeBinding(binding, parameter)) ))
                ))                                                          ))
            ), $do(
            $(  getStream()                                                                         , input ->
            $(  simple($do(
                $(  dispatcher.bindings()                                           , binding ->
                $(  singleton(text(binding.binding()))                              , completion ->
                $(  input.isPrefixOf(completion) ? singleton(completion) : nil()    )))
                ))                                                                                  ))
            ));
        }

        public <B> Dispatcher<S, C, P, B> map(Function<A, B> f) { return dispatcher(bindings.map(binding -> binding.map(f))); }
    }
    public static abstract class Action<A> {
        public static final class Handled<A> extends Action<A> {
            final Supplier<A> action;

            Handled(Supplier<A> action) { this.action = action; }

            public interface Case<A, R> { R caseHandled(Supplier<A> action); }
            @Override public <R> R caseof(Handled.Case<A, R> caseHandled, Rejected.Case<A, R> caseRejected) { return caseHandled.caseHandled(action); }

            @Override public boolean isHandled() { return true; }
            @Override public boolean isRejected() { return false; }
            @Override public Supplier<A> coerceHandled() throws Undefined { return action; }
            @Override public Runnable coerceRejected() throws Undefined { return undefined(); }

            @Override public <B> Action<B> map(Function<A, B> f) { return handled(() -> f.apply(action.get())); }
        }
        public static final class Rejected<A> extends Action<A> {
            final Runnable action;

            Rejected(Runnable action) { this.action = action; }

            public interface Case<A, R> { R caseRejected(Runnable action); }
            @Override public <R> R caseof(Handled.Case<A, R> caseHandled, Rejected.Case<A, R> caseRejected) { return caseRejected.caseRejected(action); }

            @Override public boolean isHandled() { return false; }
            @Override public boolean isRejected() { return true; }
            @Override public Supplier<A> coerceHandled() throws Undefined { return undefined(); }
            @Override public Runnable coerceRejected() throws Undefined { return action; }

            @Override public <B> Action<B> map(Function<A, B> f) { return rejected(action); }
        }

        Action() {}

        public interface Match<A, R> extends Handled.Case<A, R>, Rejected.Case<A, R> {}
        public final <R> R match(Match<A, R> match) { return caseof(match, match); }
        public abstract <R> R caseof(Handled.Case<A, R> caseHandled, Rejected.Case<A, R> caseRejected);

        public static <A> Action<A> handled(Supplier<A> action) { return new Handled<>(action); }
        public static <A> Action<A> rejected(Runnable action) { return new Rejected<>(action); }

		public abstract boolean isHandled();
		public abstract boolean isRejected();
		public abstract Supplier<A> coerceHandled() throws Undefined;
		public abstract Runnable coerceRejected() throws Undefined;

        public abstract <B> Action<B> map(Function<A, B> f);
    }
    public static abstract class Behavior<S, C, P, A> {
        public static final class Handler<S, C, P, A> extends Behavior<S, C, P, A> {
            BiFunction<Context<S, C>, P, Action<A>> handler;

            Handler(BiFunction<Context<S, C>, P, Action<A>> handler) { this.handler = handler; }

            public interface Case<S, C, P, A, R> { R caseHandler(BiFunction<Context<S, C>, P, Action<A>> handler); }
            @Override public <R> R caseof(Handler.Case<S, C, P, A, R> caseHandler, Stub.Case<S, C, P, A, R> caseStub) { return caseHandler.caseHandler(handler); }

            @Override public <B> Behavior<S, C, P, B> map(Function<A, B> f) { return handler((context, parameter) -> handler.apply(context, parameter).map(f)); }
        }
        public static final class Stub<S, C, P, A> extends Behavior<S, C, P, A> {
            Stub() {}

            public interface Case<S, C, P, A, R> { R caseStub(); }
            @Override public <R> R caseof(Handler.Case<S, C, P, A, R> caseHandler, Stub.Case<S, C, P, A, R> caseStub) { return caseStub.caseStub(); }

            @Override public <B> Behavior<S, C, P, B> map(Function<A, B> f) { return stub(); }
        }

        Behavior() {}

        public interface Match<S, C, P, A, R> extends Handler.Case<S, C, P, A, R>, Stub.Case<S, C, P, A, R> {}
        public final <R> R match(Match<S, C, P, A, R> match) { return caseof(match, match); }
        public abstract <R> R caseof(Handler.Case<S, C, P, A, R> caseHandler, Stub.Case<S, C, P, A, R> caseStub);

        public static <S, C, P, A> Behavior<S, C, P, A> handler(BiFunction<Context<S, C>, P, Action<A>> handler) { return new Handler<>(handler); }
        public static <S, C, P, A> Behavior<S, C, P, A> stub() { return new Stub<>(); }

        public abstract <B> Behavior<S, C, P, B> map(Function<A, B> f);
    }
    public interface Definition<S, C, A> { <T> Body<S, C, T, Flow<T, A>> body(); }
    public static final class Node<S, C, P, A> extends Command<S, C, A> {
        final Definition<S, C, P> definition;
        final Dispatcher<S, C, P, A> dispatcher;
        final Behavior<S, C, P, A> behavior;

        Node(String name, String description, Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher, Behavior<S, C, P, A> behavior) { super(name, description); this.definition = definition; this.dispatcher = dispatcher; this.behavior = behavior; }

        public interface Case<S, C, A, R> { <P> R caseNode(Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher, Behavior<S, C, P, A> behavior); }
        @Override public <R> R caseof(Node.Case<S, C, A, R> caseNode) { return caseNode.caseNode(definition, dispatcher, behavior); }

        @Override public <B> Command<S, C, B> map(Function<A, B> f) { return node(name, description, definition, dispatcher.map(f), behavior.map(f)); }
    }

    final String name;
    final String description;

    Command(String name, String description) { this.name = name; this.description = description; }

    public interface Match<S, C, A, R> extends Node.Case<S, C, A, R> {}
    public final <R> R match(Match<S, C, A, R> match) { return caseof(match); }
    public abstract <R> R caseof(Node.Case<S, C, A, R> caseEntry);

    public static <S, C, P, A> Command<S, C, A> node(String name, String description, Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher, Behavior<S, C, P, A> behavior) { return new Node<>(name, description, definition, dispatcher, behavior); }
    public static <S, C, P, A> Command<S, C, A> node(String name, String description, Definition<S, C, P> definition, Behavior<S, C, P, A> behavior) { return node(name, description, definition, dispatcher(), behavior); }
    public static <S, C, P, A> Command<S, C, A> node(String name, String description, Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher) { return node(name, description, definition, dispatcher, stub()); }
    public static <S, C, A> Command<S, C, A> node(String name, String description, Dispatcher<S, C, Unit, A> dispatcher, Behavior<S, C, Unit, A> behavior) { return node(name, description, Body::empty, dispatcher, behavior); }
    public static <S, C, A> Command<S, C, A> node(String name, String description, Dispatcher<S, C, Unit, A> dispatcher) { return node(name, description, Body::empty, dispatcher, stub()); }
    public static <S, C, A> Command<S, C, A> node(String name, String description) { return node(name, description, Body::empty, dispatcher(), stub()); }
    public static <S, C, A> Command<S, C, A> root(Command<S, C, A> command, String binding) {
        return node(
            "root",
            "Root command",
            Body::empty,
            dispatcher(entry(
                binding,
                command
            )),
            stub()
        );
    }
    public static <S, C, A> Command<S, C, A> root(Command<S, C, A> command) { return root(command, command.name()); }

    public final String name() { return name; }
    public final String description() { return description; }

    public static <S, C, A> Parser<Text, Context<S, C>, Bottom, Action<A>> parseCommand(Command<S, C, A> command) {
        return command.match(new Command.Match<>() {
            @Override public <P> Parser<Text, Context<S, C>, Bottom, Action<A>>
                caseNode(Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher, Behavior<S, C, P, A> behavior) {
                return $do(
                $(  recur(() -> parseBody($do(
                    $(  definition.<Bottom> body()  , flow ->
                    $(  evaluate(evalFlow(flow))    ))
                    )))                                                                 , parameter ->
                $(  behavior.caseof(
                        handler -> choice($do(
                        $(  suppress(eof())                             , () ->
                        $(  getUser()                                   , context ->
                        $(  simple(handler.apply(context, parameter))   )))
                        ), recur(() -> parseDispatcher(dispatcher, parameter))),
                        () -> recur(() -> parseDispatcher(dispatcher, parameter))
                    )                                                                   ))
                );
            }
        });
    }
    public static <S, C, A> Parser<Text, Context<S, C>, Bottom, List<Text>> analyzeCommand(Command<S, C, A> command) {
        return command.match(new Command.Match<>() {
            @Override public <P> Parser<Text, Context<S, C>, Bottom, List<Text>>
                caseNode(Definition<S, C, P> definition, Dispatcher<S, C, P, A> dispatcher, Behavior<S, C, P, A> behavior) {
                return $do(
                $(  recur(() -> analyzeBody($do(
                    $(  definition.<Bottom> body()  , flow ->
                    $(  evaluate(evalFlow(flow))    ))
                    )))                                                                     , result ->
                $(  result.caseof(
                        completions -> simple(completions),
                        parameter -> recur(() -> analyzeDispatcher(dispatcher, parameter))
                    )                                                                       ))
                );
            }
        });
    }

    public static <S, C, A> Result<Text, Context<S, C>, Bottom, Action<A>> runCommand(Command<S, C, A> command, Text input, Context<S, C> context) { return runParser(parseCommand(command), input, context); }
    public static <S, C, A> Result<Text, Context<S, C>, Bottom, Action<A>> runCommand(Command<S, C, A> command, Text input, S source, C context) { return runCommand(command, input, base(source, context, command)); }
    public static <S, C, A> Result<Text, Context<S, C>, Bottom, List<Text>> completeCommand(Command<S, C, A> command, Text input, Context<S, C> context) { return runParser(analyzeCommand(command), input, context); }
    public static <S, C, A> Result<Text, Context<S, C>, Bottom, List<Text>> completeCommand(Command<S, C, A> command, Text input, S source, C context) { return completeCommand(command, input, base(source, context, command)); }

    public abstract <B> Command<S, C, B> map(Function<A, B> f);
}
