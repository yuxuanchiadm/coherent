package org.coherent.core;

import org.coherent.core.Command;
import static org.coherent.core.Command.*;
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
import org.coherent.core.Command.Action;
import static org.coherent.core.Command.Action.*;
import org.coherent.core.Command.Behavior;
import static org.coherent.core.Command.Behavior.*;

import static org.coherent.core.Command.Body.Notation.*;
import static org.coherent.core.Command.Flow.Notation.*;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Read;
import static org.jparsec.core.parser.Read.*;

import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
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
		Command<Unit, Unit, Unit> testCommand = node(
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

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result4 = completeCommand(rootCommand, text(""), unit(), unit());
		assertTrue(result4.isSuccess());
		assertEquals(list(text("test")), result4.coerceResult());
		assertEquals(1, result4.getEnvironment().location().line());
		assertEquals(1, result4.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result5 = completeCommand(rootCommand, text("te"), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(text("test")), result5.coerceResult());
		assertEquals(1, result5.getEnvironment().location().line());
		assertEquals(1, result5.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result6 = completeCommand(rootCommand, text("test"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(), result6.coerceResult());
		assertEquals(1, result6.getEnvironment().location().line());
		assertEquals(5, result6.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result7 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(), result7.coerceResult());
		assertEquals(1, result7.getEnvironment().location().line());
		assertEquals(6, result7.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result8 = completeCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(), result8.coerceResult());
		assertEquals(1, result8.getEnvironment().location().line());
		assertEquals(6, result8.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result9 = completeCommand(rootCommand, text("test foo "), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());
		assertEquals(1, result9.getEnvironment().location().line());
		assertEquals(6, result9.getEnvironment().location().column());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result10 = completeCommand(rootCommand, text("test foo bar"), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(), result10.coerceResult());
		assertEquals(1, result10.getEnvironment().location().line());
		assertEquals(6, result10.getEnvironment().location().column());
	}
	@Test public void testParameter() {
		Command<Unit, Unit, Tuple<String, Integer>> testCommand = node(
			"test",
			"Test command",
			new Object() {
				public <T> Body<Unit, Unit, T, Flow<T, Tuple<String, Integer>>> testBody() {
				return $do(
					$(  define(parameter("test1", "Test string parameter", readString(), (input, context) -> singleton(text("\"\""))))  , p1 ->
					$(  define(parameter("test2", "Test integer parameter", readInteger(), (input, context) -> singleton(text("0"))))   , p2 ->
					$(  evaluate($do(
						$(  p1						, v1 ->
						$(  p2						, v2 ->
						$(  value(tuple(v1, v2))	)))
						))																											  )))
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

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result7 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(text("\"\"")), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result8 = completeCommand(rootCommand, text("test \"foo"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result9 = completeCommand(rootCommand, text("test \"foo\""), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result10 = completeCommand(rootCommand, text("test \"foo\" "), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(text("0")), result10.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result11 = completeCommand(rootCommand, text("test \"foo\" 12450"), unit(), unit());
		assertTrue(result11.isSuccess());
		assertEquals(list(), result11.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result12 = completeCommand(rootCommand, text("test\"foo\" 12450"), unit(), unit());
		assertTrue(result12.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result13 = completeCommand(rootCommand, text("test \"foo\"12450"), unit(), unit());
		assertTrue(result13.isFail());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result14 = completeCommand(rootCommand, text("test \"foo\" 12450 bar"), unit(), unit());
		assertTrue(result14.isSuccess());
		assertEquals(list(), result14.coerceResult());
	}
	@Test public void testDispatcher() {
		Command<Unit, Unit, Unit> helpCommand = node(
			"help",
			"Help command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> debugCommand = node(
			"debug",
			"Register command",
			Body::empty,
			dispatcher(),
			handler((context, parameter) -> handled(() -> parameter))
		);
		Command<Unit, Unit, Unit> testCommand = node(
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

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result5 = completeCommand(rootCommand, text("test "), unit(), unit());
		assertTrue(result5.isSuccess());
		assertEquals(list(text("help"), text("debug")), result5.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result6 = completeCommand(rootCommand, text("test he"), unit(), unit());
		assertTrue(result6.isSuccess());
		assertEquals(list(text("help")), result6.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result7 = completeCommand(rootCommand, text("test help"), unit(), unit());
		assertTrue(result7.isSuccess());
		assertEquals(list(), result7.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result8 = completeCommand(rootCommand, text("test de"), unit(), unit());
		assertTrue(result8.isSuccess());
		assertEquals(list(text("debug")), result8.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result9 = completeCommand(rootCommand, text("test debug"), unit(), unit());
		assertTrue(result9.isSuccess());
		assertEquals(list(), result9.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result10 = completeCommand(rootCommand, text("test foo"), unit(), unit());
		assertTrue(result10.isSuccess());
		assertEquals(list(), result10.coerceResult());

		Result<Text, Context<Unit, Unit>, Bottom, List<Text>> result11 = completeCommand(rootCommand, text("testhelp"), unit(), unit());
		assertTrue(result11.isFail());
	}
	@Test public void testRejected() {
		Command<Unit, Unit, Unit> testCommand = node(
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
}
