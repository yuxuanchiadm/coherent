package org.coherent.core;

import org.coherent.core.Command;
import static org.coherent.core.Command.*;
import org.coherent.core.Command.Context;
import static org.coherent.core.Command.Context.*;
import org.coherent.core.Command.Completion;
import static org.coherent.core.Command.Completion.*;
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
import org.coherent.core.Command.Action;
import static org.coherent.core.Command.Action.*;
import org.coherent.core.Command.Behavior;
import static org.coherent.core.Command.Behavior.*;

import static org.coherent.core.Command.Body.Notation.*;
import static org.coherent.core.Command.Flow.Notation.*;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Location;
import static org.jparsec.core.Parser.Location.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Read;
import static org.jparsec.core.parser.Read.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
import org.monadium.core.data.Either;
import static org.monadium.core.data.Either.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

import static org.monadium.core.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {
	@Test public void testBasic() {
		Command<Unit, Unit, Unit> testCommand = command(
			"test",
			"Test command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result1 = runCommand(rootCommand, text(""), unit(), unit());
		assertTrue(result1.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result2 = runCommand(rootCommand, text("test"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(unit(), result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result3 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text(""), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("test"), location())), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("te"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("test"), location())), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result8 = completeCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result9 = completeCommand(rootCommand, text("test foo "), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result10 = completeCommand(rootCommand, text("test foo bar"), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(), result10.coerceResult());
	}
	@Test public void testParameter() {
		Command<Unit, Unit, Tuple<String, Integer>> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Tuple<String, Integer>>> testBody() {
					return $do(
					$(	define(parameter("test", "Test string parameter", readString(), completer(text("\""))))		, p1 ->
					$(	define(parameter("test", "Test integer parameter", readInteger(), completer(text("0"))))	, p2 ->
					$(	evaluate($do(
						$(	p1						, v1 ->
						$(	p2						, v2 ->
						$(	value(tuple(v1, v2))	)))
						))																							)))
					);
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Tuple<String, Integer>> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result1 = runCommand(rootCommand, text("test \"\""), unit(), unit());
		assertTrue(result1.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result2 = runCommand(rootCommand, text("test \"\" 0"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(tuple("", 0), result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result3 = runCommand(rootCommand, text("test \"foo\" 12450"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(tuple("foo", 12450), result3.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result4 = runCommand(rootCommand, text("test\"foo\" 12450"), unit(), unit());
		assertTrue(result4.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result5 = runCommand(rootCommand, text("test \"foo\"12450"), unit(), unit());
		assertTrue(result5.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Integer>>> result6 = runCommand(rootCommand, text("test \"foo\" 12450 bar"), unit(), unit());
		assertTrue(result6.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result8 = completeCommand(rootCommand, text("test \"foo"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result9 = completeCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result10 = completeCommand(rootCommand, text("test \"foo\" "), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(completion(text("0"), location().advanceString("test \"foo\" "))), result10.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result11 = completeCommand(rootCommand, text("test \"foo\" 12450"), unit(), unit());
		assertTrue(result11.isSuccess());
		assertEquals(list(), result11.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result12 = completeCommand(rootCommand, text("test\"foo\" 12450"), unit(), unit());
		assertTrue(result12.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result13 = completeCommand(rootCommand, text("test \"foo\"12450"), unit(), unit());
		assertTrue(result13.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result14 = completeCommand(rootCommand, text("test \"foo\" 12450 bar"), unit(), unit());
		assertTrue(result14.isSuccess());
		assertEquals(list(), result14.coerceResult());
	}
	@Test public void testUnion() {
		Command<Unit, Unit, Either<String, Boolean>> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Either<String, Boolean>>> testBody() {
					return define(union("test", "Test union parameter",
						specialize(parameter("test", "Test string parameter", readString(), completer(text("\""))), Either::left),
						specialize(parameter("test", "Test boolean parameter", readBoolean(), completer(text("false"), text("true"))), Either::right)
					));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Either<String, Boolean>> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<String, Boolean>>> result1 = runCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(left("foo"), result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<String, Boolean>>> result2 = runCommand(rootCommand, text("test \"bar\""), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(left("bar"), result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<String, Boolean>>> result3 = runCommand(rootCommand, text("test false"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(right(false), result3.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<String, Boolean>>> result4 = runCommand(rootCommand, text("test true"), unit(), unit());
		assertTrue(result4.isSuccess());
		assertTrue(result4.coerceResult().isHandled());
		assertEquals(right(true), result4.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test ")), completion(text("false"), location().advanceString("test ")), completion(text("true"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test \""), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test f"), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test "))), result7.coerceResult());
	}
	@Test public void testNested() {
		Command<Unit, Unit, Tuple<String, Boolean>> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Tuple<String, Boolean>>> testBody() {
					return define(nested("test", "Test nested parameter", new Object() {
						public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Boolean>>> nestedBody() {
							return $do(
							$(	define(parameter("test", "Test string parameter", readString(), completer(text("\""))))						, p1 ->
							$(	define(parameter("test", "Test boolean parameter", readBoolean(), completer(text("false"), text("true"))))	, p2 ->
							$(	evaluate($do(
								$(	p1						, v1 ->
								$(	p2						, v2 ->
								$(	value(tuple(v1, v2))	)))
								))																											)))
							);
						}
					}::nestedBody));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Tuple<String, Boolean>> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Boolean>>> result1 = runCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result1.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Boolean>>> result2 = runCommand(rootCommand, text("test false"), unit(), unit());
		assertTrue(result2.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Boolean>>> result3 = runCommand(rootCommand, text("test \"foo\" false"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(tuple("foo", false), result3.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Tuple<String, Boolean>>> result4 = runCommand(rootCommand, text("test false \"foo\""), unit(), unit());
		assertTrue(result4.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test \"foo\" "), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test \"foo\" ")), completion(text("true"), location().advanceString("test \"foo\" "))), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test \"foo\" f"), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test \"foo\" "))), result7.coerceResult());
	}
	@Test public void testUnionNested() {
		Command<Unit, Unit, Either<Tuple<String, Character>, Tuple<String, Boolean>>> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Either<Tuple<String, Character>, Tuple<String, Boolean>>>> testBody() {
					return define(union("test", "Test union parameter",
						specialize(nested("test", "Test nested parameter", new Object() {
							public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Character>>> nestedBody() {
								return $do(
								$(	define(parameter("test", "Test string parameter", readString(), completer(text("\""))))			, p1 ->
								$(	define(parameter("test", "Test character parameter", readCharacter(), completer(text("'"))))	, p2 ->
								$(	evaluate($do(
									$(	p1						, v1 ->
									$(	p2						, v2 ->
									$(	value(tuple(v1, v2))	)))
									))																								)))
								);
							}
						}::nestedBody), Either::left),
						specialize(nested("test", "Test nested parameter", new Object() {
							public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Boolean>>> nestedBody() {
								return $do(
								$(	define(parameter("test", "Test string parameter", readString(), completer(text("\""))))						, p1 ->
								$(	define(parameter("test", "Test boolean parameter", readBoolean(), completer(text("false"), text("true"))))	, p2 ->
								$(	evaluate($do(
									$(	p1						, v1 ->
									$(	p2						, v2 ->
									$(	value(tuple(v1, v2))	)))
									))																											)))
								);
							}
						}::nestedBody), Either::right)
					));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Either<Tuple<String, Character>, Tuple<String, Boolean>>> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<Tuple<String, Character>, Tuple<String, Boolean>>>> result1 = runCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result1.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<Tuple<String, Character>, Tuple<String, Boolean>>>> result2 = runCommand(rootCommand, text("test \"foo\" 'a'"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(left(tuple("foo", 'a')), result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Either<Tuple<String, Character>, Tuple<String, Boolean>>>> result3 = runCommand(rootCommand, text("test \"foo\" false"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(right(tuple("foo", false)), result3.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test \""), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test \"foo\" "), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("'"), location().advanceString("test \"foo\" ")), completion(text("false"), location().advanceString("test \"foo\" ")), completion(text("true"), location().advanceString("test \"foo\" "))), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test \"foo\" '"), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(completion(text("'"), location().advanceString("test \"foo\" "))), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result8 = completeCommand(rootCommand, text("test \"foo\" f"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test \"foo\" "))), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result9 = completeCommand(rootCommand, text("test \"foo\" t"), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(completion(text("true"), location().advanceString("test \"foo\" "))), result9.coerceResult());
	}
	@Test public void testDispatcher() {
		Command<Unit, Unit, Unit> helpCommand = command(
			"help",
			"Help command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> debugCommand = command(
			"debug",
			"Register command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> testCommand = command(
			"test",
			"Test command",
			Body::empty,
			dispatcher(entry(
				"help",
				(context, parameter) -> context.environment(),
				helpCommand
			), entry(
				"debug",
				(context, parameter) -> context.environment(),
				debugCommand
			)),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result1 = runCommand(rootCommand, text("test help"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(unit(), result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result2 = runCommand(rootCommand, text("test debug"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(unit(), result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result3 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result4 = runCommand(rootCommand, text("testhelp"), unit(), unit());
		assertTrue(result4.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("help"), location().advanceString("test ")), completion(text("debug"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test he"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("help"), location().advanceString("test "))), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result7 = completeCommand(rootCommand, text("test help"), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result8 = completeCommand(rootCommand, text("test de"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(completion(text("debug"), location().advanceString("test "))), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result9 = completeCommand(rootCommand, text("test debug"), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result10 = completeCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(), result10.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result11 = completeCommand(rootCommand, text("testhelp"), unit(), unit());
		assertTrue(result11.isFail());
	}
	@Test public void testRejected() {
		Command<Unit, Unit, Unit> testCommand = command(
			"test",
			"Test command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> rejected(() -> {}))
		);
		Command<Unit, Unit, Unit> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Unit>> result1 = runCommand(rootCommand, text("test"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isRejected());
	}
	@Test public void testSuggest() {
		Command<Unit, Unit, Integer> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Integer>> testBody() {
					return define(suggest(
						parameter("test", "Test integer parameter", readInteger(), completer(text("0"))),
						completer(text("12450"))
					));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Integer> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result1 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result1.isSuccess());
		assertEquals(list(completion(text("0"), location().advanceString("test ")), completion(text("12450"), location().advanceString("test "))), result1.coerceResult());
	}
	@Test public void testExtend() {
		Command<Unit, Unit, Integer> testCommand = command(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Integer>> testBody() {
					return define(extend(
						parameter("test", "Test integer parameter", readInteger(), completer(text("0"))),
						i -> $do(
						$(	character('!')	, () ->
						$(	simple(i)		))
						)
					));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Integer> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result1 = runCommand(rootCommand, text("test 0"), unit(), unit());
		assertTrue(result1.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result2 = runCommand(rootCommand, text("test 0!"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(0, result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result3 = runCommand(rootCommand, text("test 12450!"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(12450, result3.coerceResult().coerceHandled().get());
	}
}
