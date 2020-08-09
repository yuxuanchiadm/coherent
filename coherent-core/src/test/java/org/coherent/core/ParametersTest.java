package org.coherent.core;

import org.coherent.core.Command;
import static org.coherent.core.Command.*;
import org.coherent.core.Command.Completion;
import static org.coherent.core.Command.Completion.*;
import org.coherent.core.Command.Parameter;
import static org.coherent.core.Command.Parameter.*;
import org.coherent.core.Command.Flow;
import static org.coherent.core.Command.Flow.*;
import org.coherent.core.Command.Body;
import static org.coherent.core.Command.Body.*;
import org.coherent.core.Command.Dispatcher;
import static org.coherent.core.Command.Dispatcher.*;
import org.coherent.core.Command.Action;
import static org.coherent.core.Command.Action.*;
import org.coherent.core.Command.Behavior;
import static org.coherent.core.Command.Behavior.*;
import org.coherent.core.Parameters;
import static org.coherent.core.Parameters.*;

import static org.coherent.core.Command.Body.Notation.*;
import static org.coherent.core.Command.Flow.Notation.*;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Location;
import static org.jparsec.core.Parser.Location.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;

import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
import org.monadium.core.data.Either;
import static org.monadium.core.data.Either.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

import static org.monadium.core.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public final class ParametersTest {
	@Test public void testUnion() {
		Command<Unit, Unit, Either<String, Boolean>> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Either<String, Boolean>>> testBody() {
					return define(parameterUnion("test", "Test union parameter",
						specialize(parameterString("test", "Test string parameter"), Either::left),
						specialize(parameterBoolean("test", "Test boolean parameter"), Either::right)
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
		Command<Unit, Unit, Tuple<String, Boolean>> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Tuple<String, Boolean>>> testBody() {
					return define(parameterNested("test", "Test nested parameter", new Object() {
						public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Boolean>>> nestedBody() {
							return $do(
							$(  define(parameterString("test", "Test string parameter"))	, p1 ->
							$(  define(parameterBoolean("test", "Test boolean parameter"))	, p2 ->
							$(  evaluate($do(
								$(  p1						, v1 ->
								$(  p2						, v2 ->
								$(  value(tuple(v1, v2))	)))
								))															)))
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
		Command<Unit, Unit, Either<Tuple<String, Character>, Tuple<String, Boolean>>> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Either<Tuple<String, Character>, Tuple<String, Boolean>>>> testBody() {
					return define(parameterUnion("test", "Test union parameter",
						specialize(parameterNested("test", "Test nested parameter", new Object() {
							public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Character>>> nestedBody() {
								return $do(
								$(  define(parameterString("test", "Test string parameter"))		, p1 ->
								$(  define(parameterCharacter("test", "Test character parameter"))  , p2 ->
								$(  evaluate($do(
									$(  p1						, v1 ->
									$(  p2						, v2 ->
									$(  value(tuple(v1, v2))	)))
									))																)))
								);
							}
						}::nestedBody), Either::left),
						specialize(parameterNested("test", "Test nested parameter", new Object() {
							public <N> Body<Unit, Unit, N, Flow<N, Tuple<String, Boolean>>> nestedBody() {
								return $do(
								$(  define(parameterString("test", "Test string parameter"))	, p1 ->
								$(  define(parameterBoolean("test", "Test boolean parameter"))	, p2 ->
								$(  evaluate($do(
									$(  p1						, v1 ->
									$(  p2						, v2 ->
									$(  value(tuple(v1, v2))	)))
									))															)))
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
	@Test public void testOption() {
		Command<Unit, Unit, Integer> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Integer>> testBody() {
					return define(parameterOption("test", "Test option parameter", tuple("foo", 1), tuple("bar", 2)));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Integer> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result1 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(1, result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result2 = runCommand(rootCommand, text("test bar"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(2, result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result3 = runCommand(rootCommand, text("test baz"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("foo"), location().advanceString("test ")), completion(text("bar"), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test f"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("foo"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test b"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("bar"), location().advanceString("test "))), result6.coerceResult());
	}
	@Test public void testLiteral() {
		Command<Unit, Unit, String> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, String>> testBody() {
					return define(parameterLiteral("test", "Test literal parameter", "foo", "bar"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, String> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result1 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals("foo", result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result2 = runCommand(rootCommand, text("test bar"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals("bar", result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result3 = runCommand(rootCommand, text("test baz"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("foo"), location().advanceString("test ")), completion(text("bar"), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test f"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("foo"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test b"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("bar"), location().advanceString("test "))), result6.coerceResult());
	}
	@Test public void testPhrase() {
		Command<Unit, Unit, String> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, String>> testBody() {
					return define(parameterPhrase("test", "Test phrase parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, String> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result1 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals("foo", result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result2 = runCommand(rootCommand, text("test bar"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals("bar", result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result3 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result3.isSuccess());
		assertEquals(list(), result3.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(), result4.coerceResult());
	}
	@Test public void testBoolean() {
		Command<Unit, Unit, Boolean> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Boolean>> testBody() {
					return define(parameterBoolean("test", "Test boolean parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Boolean> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Boolean>> result1 = runCommand(rootCommand, text("test false"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(false, result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Boolean>> result2 = runCommand(rootCommand, text("test true"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(true, result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Boolean>> result3 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test ")), completion(text("true"), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test f"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("false"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test t"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(completion(text("true"), location().advanceString("test "))), result6.coerceResult());
	}
	@Test public void testInteger() {
		Command<Unit, Unit, Integer> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Integer>> testBody() {
					return define(parameterInteger("test", "Test integer parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Integer> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result1 = runCommand(rootCommand, text("test 0"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(0, result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result2 = runCommand(rootCommand, text("test 1"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(1, result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Integer>> result3 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(), result4.coerceResult());
	}
	@Test public void testFloat() {
		Command<Unit, Unit, Float> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Float>> testBody() {
					return define(parameterFloat("test", "Test float parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Float> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Float>> result1 = runCommand(rootCommand, text("test 0.0"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals(0.0F, result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Float>> result2 = runCommand(rootCommand, text("test 1.5"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals(1.5F, result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Float>> result3 = runCommand(rootCommand, text("test NaN"), unit(), unit());
		assertTrue(result3.isSuccess());
		assertTrue(result3.coerceResult().isHandled());
		assertEquals(Float.NaN, result3.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Float>> result4 = runCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result4.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(), result5.coerceResult());
	}
	@Test public void testCharacter() {
		Command<Unit, Unit, Character> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Character>> testBody() {
					return define(parameterCharacter("test", "Test character parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Character> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<Character>> result1 = runCommand(rootCommand, text("test 'a'"), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals('a', result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Character>> result2 = runCommand(rootCommand, text("test 'b'"), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals('b', result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<Character>> result3 = runCommand(rootCommand, text("test 'c"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("'"), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test '"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("'"), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test 'a"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(), result6.coerceResult());
	}
	@Test public void testString() {
		Command<Unit, Unit, String> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, String>> testBody() {
					return define(parameterString("test", "Test string parameter"));
				}
			}::testBody,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, String> rootCommand = root(testCommand);

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result1 = runCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result1.isSuccess());
		assertTrue(result1.coerceResult().isHandled());
		assertEquals("foo", result1.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result2 = runCommand(rootCommand, text("test \"bar\""), unit(), unit());
		assertTrue(result2.isSuccess());
		assertTrue(result2.coerceResult().isHandled());
		assertEquals("bar", result2.coerceResult().coerceHandled().get());

		Result<Text, Context<Unit, Unit>, Bottom, Action<String>> result3 = runCommand(rootCommand, text("test \"baz"), unit(), unit());
		assertTrue(result3.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result4 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result4.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result5 = completeCommand(rootCommand, text("test \""), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(completion(text("\""), location().advanceString("test "))), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Completion>> result6 = completeCommand(rootCommand, text("test \"foo"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(), result6.coerceResult());
	}
}
